package com.ipc.mall.address;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AddressForm;
import com.ipc.mall.entity.Address;
import com.ipc.mall.mapper.AddressMapper;
import com.ipc.mall.service.impl.AddressServiceImpl;
import com.ipc.mall.vo.AddressVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 收货地址业务规则（PRD §5.10 / handoff §6）。
 * <p>
 * 重点：首个地址自动默认、设默认清其他、上限只挡新增、删除默认不补选、所有按 id 操作都校验归属。
 */
class AddressServiceImplTest {

    private AddressMapper addressMapper;
    private AddressServiceImpl addressService;

    @BeforeEach
    void setUp() {
        addressMapper = mock(AddressMapper.class);
        addressService = new AddressServiceImpl(addressMapper);
    }

    // ------------------------------------------------------------ 新增

    @Test
    void 第一条地址自动成为默认() {
        when(addressMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        addressService.save(1L, form(null, "星野", "13800000001", "湖南省", "长沙市", "岳麓区", "星轨大道 88 号", null));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        Address saved = captor.getValue();
        assertEquals(1L, saved.getAccountId());
        assertEquals("星野", saved.getReceiver());
        assertEquals(1, saved.getIsDefault(), "第一条地址必须自动默认");
        verify(addressMapper).clearDefault(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void 已有地址时新增默认不勾选就是普通地址() {
        when(addressMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        addressService.save(1L, form(null, "星野", "13800000002", "湖南省", "长沙市", "开福区", "开拓路 12 号", "0"));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getIsDefault());
        verify(addressMapper, never()).clearDefault(any(), any());
    }

    @Test
    void 新增时勾选默认会清掉其他默认() {
        when(addressMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        // 前端 checkbox 传 "on"
        addressService.save(1L, form(null, "星野", "13800000003", "湖南省", "长沙市", "天心区", "湘江路 9 号", "on"));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getIsDefault());
        verify(addressMapper).clearDefault(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void 超过二十条时拒绝新增() {
        when(addressMapper.selectCount(any(Wrapper.class))).thenReturn(20L);

        BizException exception = assertThrows(BizException.class,
                () -> addressService.save(1L, form(null, "星野", "13800000001", null, null, null, "某地", "0")));

        assertEquals(ResultCode.FAIL.getCode(), exception.getCode());
        assertEquals("收货地址最多保存 20 条", exception.getMessage());
        verify(addressMapper, never()).insert(any(Address.class));
    }

    // ------------------------------------------------------------ 编辑

    @Test
    void 编辑自己的地址走条件更新且不触发上限() {
        Address own = address(1L, 1L, "星野", 0);
        when(addressMapper.selectById(1L)).thenReturn(own);
        when(addressMapper.updateOwned(any(Address.class))).thenReturn(1);

        addressService.save(1L, form(1L, " 星野改 ", "13800000001", "湖南省", "长沙市", "岳麓区", "新地址 1 号", "0"));

        // 编辑不查 count，不受 20 条上限影响
        verify(addressMapper, never()).selectCount(any());
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).updateOwned(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(1L, captor.getValue().getAccountId());
        assertEquals("星野改", captor.getValue().getReceiver());
        assertEquals(0, captor.getValue().getIsDefault());
        verify(addressMapper, never()).clearDefault(any(), any());
    }

    @Test
    void 编辑时勾选默认会清其他() {
        when(addressMapper.selectById(2L)).thenReturn(address(2L, 1L, "星野", 0));
        when(addressMapper.updateOwned(any(Address.class))).thenReturn(1);

        addressService.save(1L, form(2L, "星野", "13800000001", null, null, null, "地址", "1"));

        verify(addressMapper).clearDefault(eq(1L), any(LocalDateTime.class));
        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).updateOwned(captor.capture());
        assertEquals(1, captor.getValue().getIsDefault());
    }

    @Test
    void 改别人的地址返回三千零四() {
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L, "星野", 1));

        BizException exception = assertThrows(BizException.class,
                () -> addressService.save(2L, form(1L, "bob", "13900000000", null, null, null, "地址", "0")));

        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
        verify(addressMapper, never()).updateOwned(any(Address.class));
    }

    @Test
    void 条件更新零行视为越权() {
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L, "星野", 0));
        when(addressMapper.updateOwned(any(Address.class))).thenReturn(0);

        BizException exception = assertThrows(BizException.class,
                () -> addressService.save(1L, form(1L, "星野", "13800000001", null, null, null, "地址", "0")));

        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
    }

    // ------------------------------------------------------------ 设为默认

    @Test
    void 设为默认先清其他再置目标() {
        when(addressMapper.selectById(2L)).thenReturn(address(2L, 1L, "星野", 0));
        when(addressMapper.markDefault(eq(2L), eq(1L), any(LocalDateTime.class))).thenReturn(1);

        addressService.setDefault(1L, 2L);

        verify(addressMapper).clearDefault(eq(1L), any(LocalDateTime.class));
        verify(addressMapper).markDefault(eq(2L), eq(1L), any(LocalDateTime.class));
    }

    @Test
    void 设别人的地址为默认返回三千零四() {
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L, "星野", 1));

        BizException exception = assertThrows(BizException.class, () -> addressService.setDefault(2L, 1L));

        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
        verify(addressMapper, never()).markDefault(any(), any(), any());
        verify(addressMapper, never()).clearDefault(any(), any());
    }

    // ------------------------------------------------------------ 删除

    @Test
    void 删除自己的地址走逻辑删除() {
        when(addressMapper.selectById(2L)).thenReturn(address(2L, 1L, "星野", 0));

        addressService.remove(1L, 2L);

        verify(addressMapper).deleteById(2L);
    }

    @Test
    void 删除默认地址后不自动补选() {
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L, "星野", 1));

        addressService.remove(1L, 1L);

        verify(addressMapper).deleteById(1L);
        // 不补选：既不清默认，也不把别的地址设成默认
        verify(addressMapper, never()).clearDefault(any(), any());
        verify(addressMapper, never()).markDefault(any(), any(), any());
        verify(addressMapper, never()).updateOwned(any(Address.class));
    }

    @Test
    void 删别人的地址返回三千零四() {
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L, "星野", 0));

        BizException exception = assertThrows(BizException.class, () -> addressService.remove(2L, 1L));

        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
        verify(addressMapper, never()).deleteById(any(Long.class));
    }

    // ------------------------------------------------------------ 列表 / 计数

    @Test
    void 列表把省市拼成完整地址并打码手机号() {
        when(addressMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                address(1L, 1L, "星野", 1),
                address(2L, 1L, "星野（公司）", 0)));

        List<AddressVO> list = addressService.list(1L);

        assertEquals(2, list.size());
        AddressVO first = list.get(0);
        assertTrue(first.isDefaultFlag());
        assertEquals("星野", first.getReceiver());
        assertEquals("138****0001", first.getPhoneMasked());
        assertEquals("湖南省 长沙市 岳麓区", first.getRegionText());
        assertEquals("湖南省 长沙市 岳麓区 星轨大道 88 号和平公寓 3 栋 502", first.getFullAddress());

        assertFalse(list.get(1).isDefaultFlag());
    }

    @Test
    void 省市区为空时完整地址只有详细地址() {
        Address address = address(3L, 1L, "bob", 0);
        address.setProvince(null);
        address.setCity("");
        address.setDistrict("  ");
        when(addressMapper.selectList(any(Wrapper.class))).thenReturn(List.of(address));

        AddressVO vo = addressService.list(1L).get(0);

        assertEquals("", vo.getRegionText());
        assertEquals("近地轨道仓 A 区 收货点 7 号柜", vo.getFullAddress());
    }

    @Test
    void 查别人的地址返回空() {
        when(addressMapper.selectById(1L)).thenReturn(address(1L, 1L, "星野", 1));

        assertTrue(addressService.get(2L, 1L).isEmpty());
        assertTrue(addressService.get(1L, 1L).isPresent());
        assertTrue(addressService.get(1L, null).isEmpty());
    }

    @Test
    void 计数只查未删除记录且空账号返回零() {
        when(addressMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        assertEquals(2, addressService.count(1L));
        assertEquals(0, addressService.count(null));
        verify(addressMapper, times(1)).selectCount(any(Wrapper.class));
    }

    @Test
    void 表单空白会被去掉且省市区空串归一化为null() {
        when(addressMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        AddressForm form = form(null, "  星野  ", " 13800000001 ", "  ", "", null, "  星轨大道 1 号  ", "0");

        addressService.save(1L, form);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressMapper).insert(captor.capture());
        Address saved = captor.getValue();
        assertEquals("星野", saved.getReceiver());
        assertEquals("13800000001", saved.getPhone());
        assertNull(saved.getProvince());
        assertNull(saved.getCity());
        assertNull(saved.getDistrict());
        assertEquals("星轨大道 1 号", saved.getDetail());
    }

    // ------------------------------------------------------------ 夹具

    private AddressForm form(Long id, String receiver, String phone,
                             String province, String city, String district, String detail, String isDefault) {
        AddressForm form = new AddressForm();
        form.setId(id);
        form.setReceiver(receiver);
        form.setPhone(phone);
        form.setProvince(province);
        form.setCity(city);
        form.setDistrict(district);
        form.setDetail(detail);
        form.setIsDefault(isDefault);
        return form;
    }

    private Address address(Long id, Long accountId, String receiver, int isDefault) {
        Address address = new Address();
        address.setId(id);
        address.setAccountId(accountId);
        address.setReceiver(receiver);
        address.setPhone("13800000001");
        address.setProvince("湖南省");
        address.setCity("长沙市");
        address.setDistrict("岳麓区");
        address.setDetail(receiver.equals("bob") ? "近地轨道仓 A 区 收货点 7 号柜" : "星轨大道 88 号和平公寓 3 栋 502");
        address.setIsDefault(isDefault);
        return address;
    }
}
