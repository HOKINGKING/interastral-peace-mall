package com.ipc.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ipc.mall.common.constant.OrderStatus;
import com.ipc.mall.common.constant.ProductStatus;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.OrderSubmitForm;
import com.ipc.mall.entity.Address;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.OrderItem;
import com.ipc.mall.entity.PaymentRecord;
import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.AddressMapper;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.OrderItemMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.mapper.PaymentRecordMapper;
import com.ipc.mall.mapper.ProductMapper;
import com.ipc.mall.service.AddressService;
import com.ipc.mall.service.CartService;
import com.ipc.mall.service.OrderService;
import com.ipc.mall.vo.AddressVO;
import com.ipc.mall.vo.CartItemVO;
import com.ipc.mall.vo.CheckoutVO;
import com.ipc.mall.vo.OrderDetailVO;
import com.ipc.mall.vo.OrderItemVO;
import com.ipc.mall.vo.OrderSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 交易链路实现（PRD §5.11–§5.14 / §7.6 / §8）。
 * <p>
 * 三条必须守住的规则：
 * <ol>
 *     <li><b>扣库存靠条件更新的影响行数</b>（带 version + stock &gt;= qty），0 行即 2002，不自动重试；</li>
 *     <li><b>订单 / 明细 / 扣库存 / 清购物车在同一事务</b>，任一步失败整单回滚；</li>
 *     <li><b>单价以数据库为准</b>，前端（确认页）传的价格一律忽略。</li>
 * </ol>
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final BigDecimal FREIGHT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int ORDER_NO_RETRY = 3;
    private static final int QTY_MIN = 1;
    private static final int QTY_MAX = 99;
    private static final int REMARK_MAX = 200;
    private static final String ADDRESS_INVALID = "收货地址不存在或不属于当前用户";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ItemMapper itemMapper;
    private final ProductMapper productMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final CartService cartService;
    private final AddressService addressService;
    private final AddressMapper addressMapper;

    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            ItemMapper itemMapper,
                            ProductMapper productMapper,
                            PaymentRecordMapper paymentRecordMapper,
                            CartService cartService,
                            AddressService addressService,
                            AddressMapper addressMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.itemMapper = itemMapper;
        this.productMapper = productMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.cartService = cartService;
        this.addressService = addressService;
        this.addressMapper = addressMapper;
    }

    // ------------------------------------------------------------------ 确认页

    @Override
    public CheckoutVO checkout(Long accountId, String cartIds, Long itemId, Integer quantity) {
        CheckoutVO vo = new CheckoutVO();
        List<CheckoutLine> lines = resolveLines(accountId, parseCartIds(cartIds), itemId, quantity);
        fillItems(vo, lines);

        List<AddressVO> addresses = addressService.list(accountId);
        vo.setAddresses(addresses);
        Long defaultAddressId = addresses.stream()
                .filter(AddressVO::isDefaultFlag)
                .map(AddressVO::getId)
                .findFirst()
                .orElse(addresses.isEmpty() ? null : addresses.get(0).getId());
        vo.setDefaultAddressId(defaultAddressId);

        // 页面只做提示，真正的拦截在 submit；这里把失败原因显示成一条警告
        try {
            checkLines(lines);
        } catch (BizException e) {
            vo.setBlocked(e.getMessage());
        }

        vo.setCartIds(StringUtils.hasText(cartIds) ? cartIds : null);
        vo.setItemId(itemId);
        vo.setQuantity(quantity);
        return vo;
    }

    // ------------------------------------------------------------------ 提交订单

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long accountId, OrderSubmitForm form) {
        List<Long> cartIds = parseCartIds(form.getCartIds());
        List<CheckoutLine> lines = resolveLines(accountId, cartIds, form.getItemId(), form.getQuantity());
        checkLines(lines);

        Address address = form.getAddressId() == null ? null : addressMapper.selectById(form.getAddressId());
        if (address == null || !Objects.equals(address.getAccountId(), accountId)) {
            throw new BizException(ResultCode.NO_PERMISSION.getCode(), ADDRESS_INVALID);
        }
        String remark = normalizeRemark(form.getRemark());

        BigDecimal goodsAmount = lines.stream()
                .map(line -> line.price().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setAccountId(accountId);
        order.setTotalAmount(goodsAmount.add(FREIGHT));
        order.setFreightAmount(FREIGHT);
        order.setStatus(OrderStatus.PENDING_PAYMENT.name());
        order.setReceiverName(address.getReceiver());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverDetail(address.getDetail());
        order.setRemark(remark);
        insertOrder(order);

        for (CheckoutLine line : lines) {
            orderItemMapper.insert(toOrderItem(order.getId(), line));
        }

        // 逐条条件扣库存：任何一条被抢空就整单回滚（明细与订单一起消失）
        for (CheckoutLine line : lines) {
            int version = line.sku().getVersion() == null ? 0 : line.sku().getVersion();
            int rows = itemMapper.deductStock(line.itemId(), line.quantity(), version);
            if (rows == 0) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH.getCode(),
                        line.displayName() + "库存不足，请重新下单");
            }
        }

        // 购物车来源：下单成功后把已结算的行逻辑删除；立即购买不动购物车
        if (!cartIds.isEmpty()) {
            for (CheckoutLine line : lines) {
                if (line.cartId() != null) {
                    cartService.remove(accountId, line.cartId());
                }
            }
        }
        log.info("下单成功: orderId={} no={} account={} 金额={}", order.getId(), order.getOrderNo(),
                accountId, order.getTotalAmount());
        return order.getId();
    }

    // ------------------------------------------------------------------ 支付 / 取消 / 收货

    @Override
    public Order get(Long accountId, Long orderId) {
        return findOwned(accountId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long accountId, Long orderId) {
        Order order = findOwned(accountId, orderId);
        // 幂等：重复点支付（已是 PAID）直接当成功，不重写 pay_time、不重复加销量
        if (OrderStatus.PAID.name().equals(order.getStatus())) {
            return;
        }
        if (!OrderStatus.of(order.getStatus()).canTransitTo(OrderStatus.PAID)) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.markPaid(orderId, accountId, now, now);
        if (rows == 0) {
            // 并发：可能另一请求刚支付成功 → 仍然按幂等处理
            Order latest = orderMapper.selectById(orderId);
            if (latest != null && OrderStatus.PAID.name().equals(latest.getStatus())) {
                return;
            }
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        // 销量只在状态确实流转成功时累加，且按 product_id 分组（PRD §7.6）
        increaseSales(orderId);
        // 支付成功才写流水，与状态流转同一事务：要么「已支付 + 有流水」，要么都不发生
        writePayment(order, now);
        log.info("订单支付成功: orderId={} account={}", orderId, accountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long accountId, Long orderId) {
        Order order = findOwned(accountId, orderId);
        if (!OrderStatus.of(order.getStatus()).canTransitTo(OrderStatus.CANCELLED)) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        // 只有条件更新成功（rows == 1）才回补库存，避免与超时关单并发时重复补货
        int rows = orderMapper.markCancelled(orderId, accountId, now, now);
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        for (OrderItem item : itemsOf(orderId)) {
            itemMapper.restoreStock(item.getItemId(), item.getQuantity());
        }
        log.info("订单已取消并回补库存: orderId={} account={}", orderId, accountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long accountId, Long orderId) {
        Order order = findOwned(accountId, orderId);
        if (!OrderStatus.of(order.getStatus()).canTransitTo(OrderStatus.COMPLETED)) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.markCompleted(orderId, accountId, now, now);
        if (rows == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        log.info("订单确认收货: orderId={} account={}", orderId, accountId);
    }

    // ------------------------------------------------------------------ 订单管理（列表 / 详情 / 删除）

    @Override
    public IPage<OrderSummaryVO> pageOrders(Long accountId, String status, int pageNo, int size) {
        Page<Order> page = new Page<>(Math.max(1, pageNo), Math.max(1, size));
        String statusFilter = normalizeStatus(status);
        IPage<Order> raw = orderMapper.selectPage(page, new LambdaQueryWrapper<Order>()
                .eq(Order::getAccountId, accountId)
                .eq(statusFilter != null, Order::getStatus, statusFilter)
                .orderByDesc(Order::getCreateTime)
                .orderByDesc(Order::getId));

        // 一页只查一次明细，再按 orderId 分组，避免每张卡片一次查询
        List<Long> orderIds = raw.getRecords().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (OrderItem item : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .in(OrderItem::getOrderId, orderIds))) {
                itemsByOrder.computeIfAbsent(item.getOrderId(), key -> new ArrayList<>()).add(item);
            }
        }

        Page<OrderSummaryVO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream()
                .map(order -> OrderSummaryVO.of(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList());
        return result;
    }

    @Override
    public OrderDetailVO detail(Long accountId, Long orderId) {
        Order order = findOwned(accountId, orderId);
        OrderDetailVO detail = OrderDetailVO.of(order, itemsOf(orderId));
        // 支付流水是独立凭证，按 order_id 查（唯一索引保证最多一条）
        PaymentRecord payment = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .last("LIMIT 1"));
        detail.applyPayment(payment);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long accountId, Long orderId) {
        Order order = findOwned(accountId, orderId);
        if (!OrderStatus.of(order.getStatus()).isFinal()) {
            // 只有 CANCELLED / COMPLETED 可以删（PRD §8.2 T7）
            throw new BizException(ResultCode.ORDER_STATUS_INVALID);
        }
        // 逻辑删除，不回补库存（取消时已经补过，删除只是隐藏）
        orderMapper.deleteById(orderId);
        log.info("订单已删除: orderId={} account={}", orderId, accountId);
    }

    // ------------------------------------------------------------------ 明细解析与校验

    /** 两个入口归一到同一份「待下单明细」，价格一律取数据库里的 item.price */
    private List<CheckoutLine> resolveLines(Long accountId, List<Long> cartIds, Long itemId, Integer quantity) {
        if (!cartIds.isEmpty()) {
            List<CartItemVO> carts = cartService.listByIds(accountId, cartIds);
            if (carts.isEmpty()) {
                return List.of();
            }
            Map<Long, Item> skuMap = loadItems(carts.stream().map(CartItemVO::getItemId).toList());
            Map<Long, Product> productMap = loadProducts(skuMap.values());
            List<CheckoutLine> lines = new ArrayList<>(carts.size());
            for (CartItemVO cart : carts) {
                Item sku = skuMap.get(cart.getItemId());
                Product product = sku == null ? null : productMap.get(sku.getProductId());
                lines.add(new CheckoutLine(cart.getCartId(), cart.getItemId(), sku, product, quantityOf(cart)));
            }
            return lines;
        }

        if (itemId == null) {
            return List.of();
        }
        Item sku = itemMapper.selectById(itemId);
        Product product = sku == null ? null : productMapper.selectById(sku.getProductId());
        int qty = quantity == null ? 1 : quantity;
        return List.of(new CheckoutLine(null, itemId, sku, product, qty));
    }

    /** 提交时的硬校验（PRD §5.11 第 1、3 条），任一失败即中止 */
    private void checkLines(List<CheckoutLine> lines) {
        if (lines.isEmpty()) {
            throw new BizException(ResultCode.CART_EMPTY);
        }
        for (CheckoutLine line : lines) {
            if (line.quantity() < QTY_MIN || line.quantity() > QTY_MAX) {
                throw new BizException(ResultCode.FAIL.getCode(), "购买数量需在 1–99 之间");
            }
            if (!line.onSale()) {
                throw new BizException(ResultCode.PRODUCT_OFF_SALE.getCode(),
                        line.displayName() + "已下架或不存在");
            }
            if (!line.stockEnough()) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH.getCode(),
                        line.displayName() + "库存不足，仅剩 " + line.stock() + " 件");
            }
        }
    }

    private void fillItems(CheckoutVO vo, List<CheckoutLine> lines) {
        BigDecimal goodsAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (CheckoutLine line : lines) {
            OrderItemVO item = new OrderItemVO();
            item.setItemId(line.itemId());
            item.setProductId(line.product() == null ? null : line.product().getId());
            item.setProductName(line.product() == null ? "已失效商品" : line.product().getName());
            item.setSpec(line.sku() == null ? null : line.sku().getSpec());
            item.setPrice(OrderItemVO.scale(line.price()));
            item.setPriceText(OrderItemVO.text(line.price()));
            item.setQuantity(line.quantity());
            item.setStock(line.stock());
            item.setAvailable(line.onSale());
            item.setStockEnough(line.stockEnough());
            item.setImage(imageOf(line));
            item.computeSubtotal();
            goodsAmount = goodsAmount.add(item.getSubtotal());
            totalQuantity += line.quantity();
            vo.getItems().add(item);
        }
        goodsAmount = goodsAmount.setScale(2, RoundingMode.HALF_UP);
        vo.setGoodsAmount(goodsAmount);
        vo.setFreightAmount(FREIGHT);
        vo.setTotalAmount(goodsAmount.add(FREIGHT));
        vo.setGoodsAmountText(goodsAmount.toPlainString());
        vo.setFreightAmountText(FREIGHT.toPlainString());
        vo.setTotalAmountText(vo.getTotalAmount().toPlainString());
        vo.setTotalQuantity(totalQuantity);
    }

    // ------------------------------------------------------------------ 落库辅助

    /** 订单归属校验：不存在 3001，别人的订单 3003（不是 3004，3004 是地址） */
    private Order findOwned(Long accountId, Long orderId) {
        Order order = orderId == null ? null : orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(order.getAccountId(), accountId)) {
            throw new BizException(ResultCode.ORDER_NOT_OWNED);
        }
        return order;
    }

    private List<OrderItem> itemsOf(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
    }

    /**
     * 写支付流水（PRD §5.12 / §7.3 payment_record）。
     * <p>
     * 一单一成功流水，流水号固定为 {@code PAY + orderNo}：
     * 只在 markPaid 影响行数为 1 时调用，与「待支付 → 已支付」一一对应；
     * 表上的 {@code order_id} / {@code transaction_no} 唯一索引是并发兜底。
     */
    private void writePayment(Order order, LocalDateTime payTime) {
        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setAccountId(order.getAccountId());
        payment.setAmount(OrderItemVO.scale(order.getTotalAmount()));
        payment.setPayMethod(PaymentRecord.METHOD_MOCK);
        payment.setStatus(PaymentRecord.STATUS_SUCCESS);
        payment.setTransactionNo("PAY" + order.getOrderNo());
        payment.setPayTime(payTime);
        paymentRecordMapper.insert(payment);
    }

    /** 按 product_id 分组累加销量：同一订单里同一 SPU 的多条明细合并成一次更新 */
    private void increaseSales(Long orderId) {
        Map<Long, Integer> quantityByProduct = new HashMap<>();
        for (OrderItem item : itemsOf(orderId)) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            quantityByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        quantityByProduct.forEach(productMapper::increaseSales);
    }

    private void insertOrder(Order order) {
        for (int attempt = 1; ; attempt++) {
            order.setOrderNo(nextOrderNo());
            try {
                orderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                // order_no 唯一索引兜底：撞了就换一个号重试，最多 3 次
                if (attempt >= ORDER_NO_RETRY) {
                    log.error("订单号连续冲突，放弃下单: accountId={}", order.getAccountId());
                    throw new BizException(ResultCode.ERROR.getCode(), "下单失败，请稍后重试");
                }
                log.warn("订单号冲突，重试第 {} 次: {}", attempt, order.getOrderNo());
            }
        }
    }

    private OrderItem toOrderItem(Long orderId, CheckoutLine line) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setItemId(line.itemId());
        item.setProductId(line.product() == null ? null : line.product().getId());
        item.setProductName(line.product() == null ? "已失效商品" : line.product().getName());
        item.setSpec(line.sku() == null ? "" : line.sku().getSpec());
        item.setPrice(OrderItemVO.scale(line.price()));
        item.setQuantity(line.quantity());
        item.setImage(imageOf(line));
        return item;
    }

    private String nextOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_FORMAT)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String normalizeRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String value = remark.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > REMARK_MAX) {
            throw new BizException(ResultCode.FAIL.getCode(), "订单备注最多 200 字");
        }
        return value;
    }

    private Map<Long, Item> loadItems(List<Long> itemIds) {
        Map<Long, Item> map = new HashMap<>();
        List<Long> ids = itemIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return map;
        }
        for (Item sku : itemMapper.selectBatchIds(ids)) {
            map.put(sku.getId(), sku);
        }
        return map;
    }

    private Map<Long, Product> loadProducts(Collection<Item> skus) {
        Map<Long, Product> map = new HashMap<>();
        List<Long> ids = skus.stream().map(Item::getProductId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return map;
        }
        for (Product product : productMapper.selectBatchIds(ids)) {
            map.put(product.getId(), product);
        }
        return map;
    }

    private int quantityOf(CartItemVO cart) {
        return cart.getQuantity() == null ? 0 : cart.getQuantity();
    }

    private String imageOf(CheckoutLine line) {
        if (line.sku() != null && StringUtils.hasText(line.sku().getImage())) {
            return line.sku().getImage();
        }
        return line.product() == null ? null : line.product().getMainImage();
    }

    /** 状态 Tab 参数：只接受合法枚举名，空/非法一律按「全部」处理 */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String value = status.trim();
        for (OrderStatus candidate : OrderStatus.values()) {
            if (candidate.name().equals(value)) {
                return value;
            }
        }
        log.warn("忽略非法的订单状态筛选: {}", value);
        return null;
    }

    /** "1,2,3" → [1,2,3]；非法片段直接忽略，前后台都不会因此 500 */    private List<Long> parseCartIds(String cartIds) {
        if (!StringUtils.hasText(cartIds)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : cartIds.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
                log.warn("忽略非法的 cartId: {}", value);
            }
        }
        return ids;
    }

    // ------------------------------------------------------------------ 内部模型

    /** 一条待下单明细：保留 SKU 实体是为了拿 version 做乐观锁扣库存 */
    private record CheckoutLine(Long cartId, Long itemId, Item sku, Product product, int quantity) {

        boolean onSale() {
            return sku != null && product != null && ProductStatus.isOnSale(product.getStatus());
        }

        int stock() {
            return sku == null || sku.getStock() == null ? 0 : sku.getStock();
        }

        boolean stockEnough() {
            return onSale() && quantity <= stock();
        }

        BigDecimal price() {
            return sku == null || sku.getPrice() == null ? BigDecimal.ZERO : sku.getPrice();
        }

        String displayName() {
            return product == null ? "该商品" : "「" + product.getName() + "」";
        }
    }
}
