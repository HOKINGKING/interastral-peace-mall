package com.ipc.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ipc.mall.entity.Address;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 收货地址 Mapper：按 accountId 查列表用 Wrapper 即可。
 * <p>
 * 「设为默认」要改多行（先把该用户其它地址清 0，再把目标置 1），
 * 两条语句都带 {@code account_id} 条件并用原生 SQL 写：
 * 这样归属校验和更新是同一条语句，不会漏掉别人家的地址。
 */
public interface AddressMapper extends BaseMapper<Address> {

    /** 清掉该用户所有默认标记（同事务内紧接着 markDefault 把目标置 1） */
    @Update("UPDATE address SET is_default = 0, update_time = #{updateTime} "
            + "WHERE account_id = #{accountId} AND deleted = 0 AND is_default = 1")
    int clearDefault(@Param("accountId") Long accountId, @Param("updateTime") LocalDateTime updateTime);

    /** 把目标地址置为默认；影响行数 0 = 不存在或不属于该账号 */
    @Update("UPDATE address SET is_default = 1, update_time = #{updateTime} "
            + "WHERE id = #{id} AND account_id = #{accountId} AND deleted = 0")
    int markDefault(@Param("id") Long id,
                    @Param("accountId") Long accountId,
                    @Param("updateTime") LocalDateTime updateTime);

    /**
     * 编辑：整行字段覆盖 + 归属校验一条 SQL 完成。
     * <p>
     * 用实体做参数（{@code #{a.xxx}}）而不是 Wrapper：省市区允许被改回空，
     * 实体式 update 会跳过 null 字段，Wrapper 的 {@code set()} 才能写入 NULL。
     */
    @Update("UPDATE address SET receiver = #{a.receiver}, phone = #{a.phone}, "
            + "province = #{a.province}, city = #{a.city}, district = #{a.district}, "
            + "detail = #{a.detail}, is_default = #{a.isDefault}, update_time = #{a.updateTime} "
            + "WHERE id = #{a.id} AND account_id = #{a.accountId} AND deleted = 0")
    int updateOwned(@Param("a") Address address);
}
