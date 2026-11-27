package com.ipc.mall.controller.admin;

import com.ipc.mall.annotation.OperLog;
import com.ipc.mall.common.result.Result;
import com.ipc.mall.service.UploadService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传（docs/api.md A17）：主图与相册共用，单次一张。
 * 返回的可访问 URL 形如 {@code /uploads/xxxx.png}，由 WebMvcConfig 映射到本地上传目录。
 */
@Controller
public class AdminUploadController {

    private final UploadService uploadService;

    public AdminUploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/admin/upload/image")
    @OperLog(module = "商品管理", action = "上传图片")
    @ResponseBody
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok("上传成功", uploadService.storeImage(file));
    }
}
