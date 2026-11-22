package com.ipc.mall.vo;

import com.ipc.mall.entity.Address;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 收货地址卡片视图（PRD §5.10）。
 * <p>
 * 列表页只展示打码手机号与拼接后的完整地址；编辑回填直接读 {@link Address} 实体。
 */
@Data
public class AddressVO {

    private Long id;

    private String receiver;

    /** 138****0001，与个人中心口径一致 */
    private String phoneMasked;

    private String regionText;

    private String detail;

    /** 省市区 + 详细地址，页面一行展示 */
    private String fullAddress;

    private boolean defaultFlag;

    public static AddressVO of(Address address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setReceiver(address.getReceiver());
        vo.setPhoneMasked(AccountProfileVO.maskPhone(address.getPhone()));
        vo.setDetail(address.getDetail());
        vo.setRegionText(joinRegion(address));
        vo.setFullAddress(join(listOf(joinRegion(address), address.getDetail())));
        vo.setDefaultFlag(address.getIsDefault() != null && address.getIsDefault() == 1);
        return vo;
    }

    private static String joinRegion(Address address) {
        return join(listOf(address.getProvince(), address.getCity(), address.getDistrict()));
    }

    private static String join(List<String> parts) {
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                values.add(part.trim());
            }
        }
        return String.join(" ", values);
    }

    private static List<String> listOf(String... values) {
        List<String> list = new ArrayList<>(values.length);
        for (String value : values) {
            list.add(value);
        }
        return list;
    }
}
