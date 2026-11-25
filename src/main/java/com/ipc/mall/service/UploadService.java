package com.ipc.mall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传（docs/api.md A17）：jpg/png/webp，≤5MB，UUID 重命名，返回可访问 URL。
 */
public interface UploadService {

    /**
     * 保存一张图片并返回站内可访问地址，如 {@code /uploads/xxxx.png}。
     *
     * @throws com.ipc.mall.common.exception.BizException 4001 类型不支持或超过大小限制
     */
    String storeImage(MultipartFile file);
}
