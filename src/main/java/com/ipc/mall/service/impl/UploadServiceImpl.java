package com.ipc.mall.service.impl;

import com.ipc.mall.common.exception.BizException;
import com.ipc.mall.common.result.ResultCode;
import com.ipc.mall.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传实现（docs/api.md A17 / PRD §6.3）。
 * <p>
 * 只允许 jpg/jpeg/png/webp、单张 ≤5MB；文件名用 UUID 重命名，杜绝路径穿越与原文件名注入；
 * 保存目录来自 {@code mall.upload.path}，由 WebMvcConfig 映射成 {@code /uploads/**}。
 */
@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final String uploadPath;

    public UploadServiceImpl(@Value("${mall.upload.path:./uploads/}") String uploadPath) {
        this.uploadPath = uploadPath;
    }

    @Override
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BizException(ResultCode.UPLOAD_INVALID);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ResultCode.UPLOAD_INVALID);
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXT.contains(extension)) {
            throw new BizException(ResultCode.UPLOAD_INVALID);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException(ResultCode.UPLOAD_INVALID);
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                // 理论不可达（文件名是生成的 UUID），留一道防御
                throw new BizException(ResultCode.UPLOAD_INVALID);
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("图片已保存: {} ({} bytes)", target, file.getSize());
            return "/uploads/" + filename;
        } catch (IOException e) {
            log.error("图片保存失败", e);
            throw new BizException(ResultCode.ERROR.getCode(), "图片保存失败，请重试");
        }
    }

    private String extensionOf(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return null;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return null;
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
