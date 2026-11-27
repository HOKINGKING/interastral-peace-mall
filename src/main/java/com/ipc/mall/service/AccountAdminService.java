package com.ipc.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ipc.mall.dto.AdminAccountForm;
import com.ipc.mall.vo.AdminAccountDetailVO;
import com.ipc.mall.vo.AdminAccountRowVO;

/**
 * 后台用户管理（PRD §6.4 / docs/api.md A18–A21）。
 * <p>
 * 后台只有一种角色，不做行级归属校验；但所有写操作都只按主键改单个账号，
 * 用户名与密码不在编辑范围内（密码走独立的重置入口）。
 */
public interface AccountAdminService {

    /** A18：分页列表，支持用户名 / 昵称 / 手机号关键词与状态筛选 */
    IPage<AdminAccountRowVO> page(String keyword, Integer status, int pageNo, int size);

    /** A19：详情，含未删除订单数 */
    AdminAccountDetailVO detail(Long id);

    /** A20：修改昵称 / 手机号 / 状态（用户名不可改） */
    void save(AdminAccountForm form);

    /** A21：重置密码，BCrypt 加密后入库，旧密码立即失效 */
    void resetPassword(Long id, String password);
}
