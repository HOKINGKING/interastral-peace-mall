package com.ipc.mall.controller.admin;

import com.ipc.mall.common.result.Result;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.dto.AdminSkuForm;
import com.ipc.mall.entity.Item;
import com.ipc.mall.entity.Product;
import com.ipc.mall.service.ItemAdminService;
import com.ipc.mall.service.ProductAdminService;
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
 * 后台 SKU 管理（PRD §6.3 / docs/api.md A14–A16）。
 * <p>
 * 必须从某个 SPU 进入（{@code productId}）；编辑带 version 做乐观锁。
 */
@Controller
public class AdminItemController {

    private final ItemAdminService itemAdminService;
    private final ProductAdminService productAdminService;

    public AdminItemController(ItemAdminService itemAdminService, ProductAdminService productAdminService) {
        this.itemAdminService = itemAdminService;
        this.productAdminService = productAdminService;
    }

    /** A14：某 SPU 下的 SKU 列表 */
    @GetMapping("/admin/item")
    public String list(@RequestParam(required = false) Long productId,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (productId == null) {
            redirectAttributes.addFlashAttribute("listMessage", "请从商品列表进入 SKU 管理");
            return "redirect:/admin/product";
        }
        Optional<Product> product = productAdminService.get(productId);
        if (product.isEmpty()) {
            redirectAttributes.addFlashAttribute("listMessage", "商品不存在或已被删除");
            return "redirect:/admin/product";
        }
        model.addAttribute("pageTitle", "SKU 管理");
        model.addAttribute("product", product.get());
        model.addAttribute("items", itemAdminService.listByProduct(productId));
        return "admin/item/list";
    }

    /** A15 的表单页：新增（带 productId）/ 编辑（带 id） */
    @GetMapping({"/admin/item/form", "/admin/item/form/{id}"})
    public String form(@RequestParam(required = false) Long productId,
                       @PathVariable(value = "id", required = false) Long id,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        AdminSkuForm form = new AdminSkuForm();
        Long targetProductId = productId;
        if (id != null) {
            Optional<Item> item = itemAdminService.get(id);
            if (item.isEmpty()) {
                redirectAttributes.addFlashAttribute("listMessage", "SKU 不存在或已被删除");
                return "redirect:/admin/product";
            }
            fill(form, item.get());
            targetProductId = item.get().getProductId();
        }
        Optional<Product> product = productAdminService.get(targetProductId);
        if (product.isEmpty()) {
            redirectAttributes.addFlashAttribute("listMessage", "商品不存在或已被删除");
            return "redirect:/admin/product";
        }
        form.setProductId(product.get().getId());
        model.addAttribute("pageTitle", id == null ? "新增 SKU" : "编辑 SKU");
        model.addAttribute("product", product.get());
        model.addAttribute("skuForm", form);
        return "admin/item/form";
    }

    /** A15：保存 */
    @PostMapping("/admin/item/save")
    @ResponseBody
    public Result<Void> save(@Valid @ModelAttribute("skuForm") AdminSkuForm skuForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors().isEmpty()
                    ? ResultCode.FAIL.getMessage()
                    : bindingResult.getAllErrors().get(0).getDefaultMessage();
            return Result.fail(ResultCode.FAIL.getCode(), message);
        }
        boolean creating = skuForm.getId() == null;
        itemAdminService.save(skuForm);
        return Result.ok(creating ? "SKU 已创建" : "SKU 已保存", (Void) null);
    }

    /** A16：删除（最后一条 SKU 禁删） */
    @DeleteMapping("/admin/item/{id}")
    @ResponseBody
    public Result<Void> remove(@PathVariable Long id) {
        itemAdminService.remove(id);
        return Result.ok("SKU 已删除", (Void) null);
    }

    private void fill(AdminSkuForm form, Item item) {
        form.setId(item.getId());
        form.setProductId(item.getProductId());
        form.setSpec(item.getSpec());
        form.setPrice(item.getPrice());
        form.setStock(item.getStock());
        form.setImage(item.getImage());
        form.setVersion(item.getVersion());
    }
}
