package com.ipc.mall.vo;

import com.ipc.mall.entity.Category;
import lombok.Data;

import java.util.List;

/**
 * 分类分组：一级分类 + 其下二级分类。
 * 前台的导航、列表页左侧分类栏、首页分类入口都用这一份数据渲染。
 */
@Data
public class CategoryGroupVO {

    private final Category root;

    private final List<Category> children;
}
