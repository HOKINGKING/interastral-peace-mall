package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Product;

/**
 * 商品 Mapper。
 * <p>
 * 不需要加 {@code @Mapper}：启动类上已有 {@code @MapperScan("com.ipc.mall.mapper")}，
 * 在包内声明接口即可被扫描注册。
 * <p>
 * BaseMapper 已经提供单表 CRUD 与 Wrapper 条件构造，简单查询不用写 XML；
 * 只有多表关联、复杂统计才在 resources/mapper/ProductMapper.xml 里写 SQL。
 */
public interface ProductMapper extends BaseMapper<Product> {
}
