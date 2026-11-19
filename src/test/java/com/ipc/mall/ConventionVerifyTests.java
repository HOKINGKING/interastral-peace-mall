package com.ipc.mall;

import com.ipc.mall.entity.Address;
import com.ipc.mall.entity.Cart;
import com.ipc.mall.entity.Feedback;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Order;
import com.ipc.mall.entity.OrderItem;
import com.ipc.mall.entity.SysOperLog;
import com.ipc.mall.mapper.AddressMapper;
import com.ipc.mall.mapper.CartMapper;
import com.ipc.mall.mapper.FeedbackMapper;
import com.ipc.mall.mapper.ItemMapper;
import com.ipc.mall.mapper.OrderItemMapper;
import com.ipc.mall.mapper.OrderMapper;
import com.ipc.mall.mapper.SysOperLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 实体层与 Mapper 映射的实测。
 * <p>
 * 依赖已执行 db/schema.sql + db/data.sql 的数据库；库没建好时下面的断言会失败。
 * 注意：{@link #实体层约定验证()} 会插入并逻辑删除一条 SKU（spec=测试-SKU）。
 */
@SpringBootTest
class ConventionVerifyTests {

    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private SysOperLogMapper sysOperLogMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 实体层约定验证() {
        // ---------- 1) 查询：create_time 映射 ----------
        Item it = itemMapper.selectById(1L);
        assertNotNull(it, "查不到 id=1 的 SKU，请先执行 db/schema.sql 与 db/data.sql");
        assertNotNull(it.getCreateTime(), "createTime 未映射，检查列名 create_time");
        assertEquals(0, it.getDeleted(), "deleted 应为 0");
        System.out.println("[1] 查询 OK: spec=" + it.getSpec()
                + " price=" + it.getPrice() + " stock=" + it.getStock()
                + " createTime=" + it.getCreateTime() + " version=" + it.getVersion());

        // ---------- 2) 插入：create_time / update_time / deleted 自动填充 ----------
        Item n = new Item();
        n.setProductId(1L);
        n.setSpec("测试-SKU");
        n.setPrice(new BigDecimal("9.90"));
        n.setStock(1);
        assertEquals(1, itemMapper.insert(n));

        Item saved = itemMapper.selectById(n.getId());
        assertNotNull(saved.getCreateTime(), "insert 未填充 createTime");
        assertNotNull(saved.getUpdateTime(), "insert 未填充 updateTime");
        assertEquals(0, saved.getDeleted(), "insert 未填充 deleted=0");
        System.out.println("[2] 插入 OK: id=" + saved.getId()
                + " createTime=" + saved.getCreateTime() + " deleted=" + saved.getDeleted());

        // ---------- 3) 乐观锁 ----------
        Integer v0 = saved.getVersion();
        Item first = new Item();
        first.setId(saved.getId());
        first.setStock(2);
        first.setVersion(v0);
        assertEquals(1, itemMapper.updateById(first), "首次更新应成功");

        Item stale = new Item();
        stale.setId(saved.getId());
        stale.setStock(3);
        stale.setVersion(v0);
        assertEquals(0, itemMapper.updateById(stale), "旧版本更新必须失败，乐观锁未生效");
        System.out.println("[3] 乐观锁 OK: version " + v0 + " -> "
                + itemMapper.selectById(saved.getId()).getVersion());

        // ---------- 4) 逻辑删除 ----------
        assertEquals(1, itemMapper.deleteById(saved.getId()));
        assertNull(itemMapper.selectById(saved.getId()), "逻辑删除后仍能查到，配置未生效");
        System.out.println("[4] 逻辑删除 OK: 普通查询已查不到该 SKU");

        // 物理清理这次测试插入的行，避免演示库积累 deleted=1 的脏数据
        jdbcTemplate.update("DELETE FROM item WHERE id = ?", saved.getId());
    }

    @Test
    void 剩余六个实体映射验证() {
        // cart：继承 BaseEntity，deleted=0 过滤生效
        List<Cart> carts = cartMapper.selectList(null);
        assertFalse(carts.isEmpty(), "cart 查询为空");
        Cart cart = carts.get(0);
        assertNotNull(cart.getAccountId());
        assertNotNull(cart.getCreateTime());
        System.out.println("[cart] " + carts.size() + " 行，首行 itemId=" + cart.getItemId()
                + " quantity=" + cart.getQuantity() + " deleted=" + cart.getDeleted());

        // CartMapper 的原生查询必须能查到含已删除的记录（PRD §7.5）
        assertNotNull(cartMapper.selectIncludingDeleted(cart.getAccountId(), cart.getItemId()),
                "selectIncludingDeleted 查不到记录，加购复活逻辑会失效");

        // address
        List<Address> addresses = addressMapper.selectList(null);
        assertFalse(addresses.isEmpty(), "address 查询为空");
        System.out.println("[address] " + addresses.size() + " 行，首行 receiver="
                + addresses.get(0).getReceiver() + " default=" + addresses.get(0).getIsDefault());

        // orders：状态是 String，时间线字段能映射
        List<Order> orders = orderMapper.selectList(null);
        assertFalse(orders.isEmpty(), "orders 查询为空");
        Order order = orders.get(0);
        assertNotNull(order.getOrderNo());
        assertNotNull(order.getStatus());
        assertNotNull(order.getCreateTime());
        System.out.println("[orders] " + orders.size() + " 行，首行 orderNo="
                + order.getOrderNo() + " status=" + order.getStatus()
                + " total=" + order.getTotalAmount());

        // order_item：无 update_time / deleted，不应被追加 deleted 条件
        List<OrderItem> items = orderItemMapper.selectList(null);
        assertFalse(items.isEmpty(), "order_item 查询为空");
        OrderItem oi = items.get(0);
        assertNotNull(oi.getProductName());
        assertNotNull(oi.getCreateTime());
        System.out.println("[order_item] " + items.size() + " 行，首行 "
                + oi.getProductName() + " × " + oi.getQuantity()
                + " 小计=" + oi.lineAmount());

        // feedback
        List<Feedback> feedbacks = feedbackMapper.selectList(null);
        assertFalse(feedbacks.isEmpty(), "feedback 查询为空");
        Feedback fb = feedbacks.get(0);
        assertNotNull(fb.getType());
        assertNotNull(fb.getStatus());
        System.out.println("[feedback] " + feedbacks.size() + " 行，首行 type=" + fb.getType()
                + " status=" + fb.getStatus() + " complaint=" + fb.isComplaint());

        // sys_oper_log
        List<SysOperLog> logs = sysOperLogMapper.selectList(null);
        assertFalse(logs.isEmpty(), "sys_oper_log 查询为空");
        SysOperLog log = logs.get(0);
        assertNotNull(log.getModule());
        assertNotNull(log.getCreateTime());
        System.out.println("[sys_oper_log] " + logs.size() + " 行，首行 "
                + log.getModule() + "/" + log.getAction() + " cost=" + log.getCostMs() + "ms");
    }
}
