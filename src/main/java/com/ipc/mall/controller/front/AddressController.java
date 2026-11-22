package com.ipc.mall.controller.front;

import com.ipc.mall.common.constant.SessionKey;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AddressForm;
import com.ipc.mall.entity.Address;
import com.ipc.mall.service.AddressService;
import com.ipc.mall.vo.LoginAccount;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * 前台收货地址（PRD §5.10 / docs/api.md F19–F23）。
 * <p>
 * F19/F20 返回视图；F21–F23 返回 {@code Result}，由页面用 fetch 调用（与购物车一致）。
 * 归属一律取自会话里的 {@link LoginAccount}，不接收前端传的 accountId。
 */
@Controller
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /** F19：地址列表（默认地址最前，上限 20） */
    @GetMapping("/address/list")
    public String list(HttpSession session, Model model) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        model.addAttribute("pageTitle", "收货地址");
        model.addAttribute("addresses", addressService.list(login.getAccountId()));
        model.addAttribute("limit", AddressService.MAX_COUNT);
        return "front/address/list";
    }

    /** F20：新增 / 编辑共用表单；编辑时回填，越权或不存在则回列表并提示 */
    @GetMapping({"/address/form", "/address/form/{id}"})
    public String form(@PathVariable(value = "id", required = false) Long id,
                       @RequestParam(value = "id", required = false) Long queryId,
                       HttpSession session,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return "redirect:/login";
        }
        Long addressId = id != null ? id : queryId;
        AddressForm form = new AddressForm();
        if (addressId != null) {
            Optional<Address> address = addressService.get(login.getAccountId(), addressId);
            if (address.isEmpty()) {
                redirectAttributes.addFlashAttribute("listMessage", "收货地址不存在或不属于当前账号");
                return "redirect:/address/list";
            }
            fill(form, address.get());
        }
        model.addAttribute("pageTitle", addressId == null ? "新增收货地址" : "编辑收货地址");
        model.addAttribute("addressForm", form);
        return "front/address/form";
    }

    /** F21：新增 / 编辑保存 */
    @PostMapping("/address/save")
    @ResponseBody
    public Result<Void> save(@Valid @ModelAttribute AddressForm addressForm,
                             BindingResult bindingResult,
                             HttpSession session) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors().isEmpty()
                    ? ResultCode.FAIL.getMessage()
                    : bindingResult.getAllErrors().get(0).getDefaultMessage();
            return Result.fail(ResultCode.FAIL.getCode(), message);
        }
        boolean creating = addressForm.getId() == null;
        addressService.save(login.getAccountId(), addressForm);
        return Result.ok(creating ? "收货地址已保存" : "收货地址已更新", (Void) null);
    }

    /** F22：删除（逻辑删除，非本人 3004） */
    @DeleteMapping("/address/{id}")
    @ResponseBody
    public Result<Void> remove(@PathVariable Long id, HttpSession session) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        addressService.remove(login.getAccountId(), id);
        return Result.ok("收货地址已删除", (Void) null);
    }

    /** F23：设为默认（同事务内清其它默认标记） */
    @PostMapping("/address/{id}/default")
    @ResponseBody
    public Result<Void> setDefault(@PathVariable Long id, HttpSession session) {
        LoginAccount login = currentAccount(session);
        if (login == null) {
            return Result.fail(ResultCode.UNAUTHORIZED);
        }
        addressService.setDefault(login.getAccountId(), id);
        return Result.ok("已设为默认地址", (Void) null);
    }

    // ------------------------------------------------------------------ 内部方法

    private void fill(AddressForm form, Address address) {
        form.setId(address.getId());
        form.setReceiver(address.getReceiver());
        form.setPhone(address.getPhone());
        form.setProvince(address.getProvince());
        form.setCity(address.getCity());
        form.setDistrict(address.getDistrict());
        form.setDetail(address.getDetail());
        form.setIsDefault(address.getIsDefault() != null && address.getIsDefault() == 1 ? "1" : null);
    }

    private LoginAccount currentAccount(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(SessionKey.LOGIN_ACCOUNT);
        return value instanceof LoginAccount account ? account : null;
    }
}
