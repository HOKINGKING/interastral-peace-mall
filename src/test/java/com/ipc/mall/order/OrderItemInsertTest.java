package com.ipc.mall.order;

import com.ipc.mall.entity.OrderItem;
import com.ipc.mall.mapper.OrderItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * order_item 落库的回归测试。
 * <p>
 * 这张表<b>不继承 BaseEntity</b>（没有 update_time / deleted），以前字段填充器会直接取
 * {@code deleted} 导致 {@code There is no getter for property named 'deleted'}，
 * 下单插明细直接 500。这里用真实 Mapper 插一条再物理清理，确保填充器跳过缺失字段。
 */
@SpringBootTest
class OrderItemInsertTest {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 插入订单明细只填充createTime() {
        OrderItem item = new OrderItem();
        item.setOrderId(0L);
        item.setItemId(1L);
        item.setProductId(1L);
        item.setProductName("测试明细");
        item.setSpec("测试规格");
        item.setPrice(new BigDecimal("9.90"));
        item.setQuantity(1);

        assertEquals(1, orderItemMapper.insert(item), "order_item 应能正常插入");
        assertNotNull(item.getCreateTime(), "create_time 应由 MyMetaObjectHandler 填充");

        OrderItem saved = orderItemMapper.selectById(item.getId());
        assertNotNull(saved.getProductName());
        assertEquals("9.90", saved.getPrice().toPlainString());

        jdbcTemplate.update("DELETE FROM order_item WHERE id = ?", item.getId());
    }
}
