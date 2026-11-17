-- ============================================================
-- Interastral Peace Mall | 星际和平商城
-- 第一步：仅创建数据库，用于验证 Spring Boot 能否连上本地 MySQL
-- 业务表结构（account / category / product / item / cart / orders ...）
-- 由下一步统一补充进来。
-- 执行方式：Navicat 里右键连接 -> 新建查询 -> 粘贴本文件 -> 运行
-- ============================================================

CREATE DATABASE IF NOT EXISTS `interastral_mall`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `interastral_mall`;

-- 连接自检表：建完后跑起来项目，能查通就说明数据源配置正确
CREATE TABLE IF NOT EXISTS `sys_check` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(64)  NOT NULL COMMENT '名称',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='环境自检表';

INSERT INTO `sys_check` (`name`) VALUES ('星际和平商城初始化成功');
