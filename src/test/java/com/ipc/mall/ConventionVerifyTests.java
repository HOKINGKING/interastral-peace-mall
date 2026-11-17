package com.ipc.mall;

import com.ipc.mall.entity.Product;
import com.ipc.mall.mapper.ProductMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 三项 Java 侧约定的实测：created_at 映射、自动填充、@Version 乐观锁。
 * <p>
 * 默认关闭，因为它依赖一个已经执行过 db/schema.sql + db/data.sql 的数据库，
 * 否则每次 mvn test 都会红。建好库后把 @Disabled 那行去掉即可运行：
 * <pre>
 * mvn test -Dtest=ConventionVerifyTests
 * </pre>
 * 注意：本测试会插入一条 sku=IPC-TEST-99 的 DRAFT 商品用于验证，验证完可手动删除。
 */
@Disabled("需先建库并导入演示数据；建好后注释掉本行")
@SpringBootTest
class ConventionVerifyTests {

    @Autowired
    private ProductMapper productMapper;

    @Test
    void 三项约定验证() {
        // ---------- 约定 1：created_at 映射为 createdAt ----------
        Product p = productMapper.selectById(1L);
        assertNotNull(p, "查不到 id=1 的商品，请先执行 db/schema.sql 与 db/data.sql");
        assertNotNull(p.getCreatedAt(), "createdAt 未映射，检查列名 created_at 与属性名 createdAt");
        System.out.println("[1] 查询 OK: sku=" + p.getSku()
                + " status=" + p.getStatus() + "(" + p.getStatus().getClass().getSimpleName() + ")"
                + " createdAt=" + p.getCreatedAt()
                + " version=" + p.getVersion());

        // ---------- 约定 2：insert 时 created_at / updated_at 自动填充 ----------
        Product n = new Product();
        n.setSku("IPC-TEST-99");
        n.setCategoryId(101L);
        n.setName("自动填充验证商品");
        n.setCoverImage("images/products/placeholder-1.jpg");
        n.setPrice(new BigDecimal("9.90"));
        n.setStock(1);
        n.setStatus("DRAFT");
        n.setSortOrder(999);
        assertEquals(1, productMapper.insert(n));

        Product saved = productMapper.selectById(n.getId());
        assertNotNull(saved.getCreatedAt(), "insert 未填充 createdAt");
        assertNotNull(saved.getUpdatedAt(), "insert 未填充 updatedAt");
        System.out.println("[2] 插入 OK: id=" + saved.getId()
                + " createdAt=" + saved.getCreatedAt() + " updatedAt=" + saved.getUpdatedAt());

        // ---------- 约定 3：String 状态 + @Version 乐观锁 ----------
        Long v0 = saved.getVersion();
        assertNotNull(v0, "version 未映射");

        Product first = new Product();
        first.setId(saved.getId());
        first.setName("改名-A");
        first.setVersion(v0);
        assertEquals(1, productMapper.updateById(first), "首次更新应成功");

        Product afterFirst = productMapper.selectById(saved.getId());
        assertEquals(v0 + 1, afterFirst.getVersion(), "version 未递增");
        assertEquals("改名-A", afterFirst.getName());
        System.out.println("[3] 首次更新 OK: version " + v0 + " -> " + afterFirst.getVersion()
                + " updatedAt=" + afterFirst.getUpdatedAt());

        Product stale = new Product();
        stale.setId(saved.getId());
        stale.setName("改名-B");
        stale.setVersion(v0);   // 故意用旧版本
        assertEquals(0, productMapper.updateById(stale), "旧版本更新必须失败，说明乐观锁未生效");
        System.out.println("[4] 旧版本更新被拒 OK（影响行数 0），当前 version="
                + productMapper.selectById(saved.getId()).getVersion());
    }
}
