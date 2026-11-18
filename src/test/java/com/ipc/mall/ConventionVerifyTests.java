package com.ipc.mall;

import com.ipc.mall.entity.Item;
import com.ipc.mall.mapper.ItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 实体层约定的实测（对应 PRD §7.1 / §7.3）。
 * <p>
 * 依赖一个已执行 db/schema.sql + db/data.sql 的数据库；库没建好时本测试会失败。
 * <p>
 * 注意：本测试会插入并逻辑删除一条 SKU（spec=测试-SKU），验证完不影响演示数据查询。
 */
@SpringBootTest
class ConventionVerifyTests {

    @Autowired
    private ItemMapper itemMapper;

    @Test
    void 实体层约定验证() {
        // 1) 查询：create_time 映射到 createTime
        Item it = itemMapper.selectById(1L);
        assertNotNull(it, "查不到 id=1 的 SKU，请先执行 db/schema.sql 与 db/data.sql");
        assertNotNull(it.getCreateTime(), "createTime 未映射，检查列名 create_time");
        assertEquals(0, it.getDeleted(), "deleted 应为 0");
        System.out.println("[1] 查询 OK: spec=" + it.getSpec()
                + " price=" + it.getPrice() + " stock=" + it.getStock()
                + " createTime=" + it.getCreateTime() + " version=" + it.getVersion());

        // 2) 插入：create_time / update_time / deleted 自动填充
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

        // 3) 乐观锁：旧 version 更新必须失败
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

        // 4) 逻辑删除：deleteById 只改 deleted，普通查询查不到
        assertEquals(1, itemMapper.deleteById(saved.getId()));
        assertNull(itemMapper.selectById(saved.getId()), "逻辑删除后仍能查到，配置未生效");
        System.out.println("[4] 逻辑删除 OK: 普通查询已查不到该 SKU");
    }
}
