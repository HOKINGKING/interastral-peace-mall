package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.dto.ProductQuery;
import com.ipc.mall.entity.Product;
import com.ipc.mall.vo.ProductCardVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品 Mapper。
 * <p>
 * 不需要加 {@code @Mapper}：启动类上已有 {@code @MapperScan("com.ipc.mall.mapper")}，
 * 在包内声明接口即可被扫描注册。
 * <p>
 * BaseMapper 已经提供单表 CRUD 与 Wrapper 条件构造，简单查询不用写 XML；
 * 这里只把「SPU + SKU 聚合 + 分类可见性 + 筛选排序」这类多表查询放到
 * {@code resources/mapper/ProductMapper.xml}。
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 商品卡分页查询（PRD §5.3）。
     * <p>
     * 一次 JOIN 聚合出最低价、总库存、SKU 数，避免列表页对每个 SPU 再查一次 SKU（N+1）。
     * 只返回上架（status=1）、所属二级分类可见（category.visible=1）且有 SKU 的商品。
     *
     * @param page        分页参数（MyBatis-Plus 分页插件自动追加 count 与 limit）
     * @param query       关键词 / 仅看有货 / 排序
     * @param categoryIds 分类范围：为空表示不按分类过滤；一级分类会传入「自身 + 所有子分类」
     */
    IPage<ProductCardVO> selectCardPage(IPage<ProductCardVO> page,
                                        @Param("q") ProductQuery query,
                                        @Param("categoryIds") List<Long> categoryIds);

    /**
     * 销量累加（PRD §7.6）：只在订单 PENDING_PAYMENT → PAID 的同一事务里调用，
     * 状态条件更新成功才累加，天然只加一次；取消/关单不回退。
     */
    @Update("UPDATE product SET sales = sales + #{qty} WHERE id = #{productId}")
    int increaseSales(@Param("productId") Long productId, @Param("qty") Integer qty);
}
