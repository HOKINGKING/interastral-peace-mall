package com.ipc.mall.admin;

import com.ipc.mall.common.exception.GlobalExceptionHandler;
import com.ipc.mall.controller.admin.AdminUploadController;
import com.ipc.mall.service.impl.UploadServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 图片上传（A17）：正常图片返回 URL，类型不对/超大返回 4001。
 * <p>
 * 用系统临时目录做上传根，避免把测试图片留在仓库里。
 */
class AdminUploadControllerTest {

    private static Path uploadRoot;

    private MockMvc mockMvc;

    @BeforeAll
    static void createRoot() throws IOException {
        uploadRoot = Files.createTempDirectory("mall-upload-test-");
    }

    @AfterAll
    static void cleanUp() throws IOException {
        if (uploadRoot != null && Files.exists(uploadRoot)) {
            try (Stream<Path> paths = Files.walk(uploadRoot)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // 测试清理失败不影响结论
                    }
                });
            }
        }
    }

    private void buildMockMvc() {
        UploadServiceImpl uploadService = new UploadServiceImpl(uploadRoot.toString() + "/");
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUploadController(uploadService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 上传PNG返回可访问地址() throws Exception {
        buildMockMvc();
        MockMultipartFile file = new MockMultipartFile("file", "cup.png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});

        mockMvc.perform(multipart("/admin/upload/image").file(file))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.startsWith("/uploads/")))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.endsWith(".png")));
    }

    @Test
    void 非图片扩展名返回4001() throws Exception {
        buildMockMvc();
        MockMultipartFile file = new MockMultipartFile("file", "hack.exe", "application/octet-stream",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/admin/upload/image").file(file))
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.message").value("文件类型不支持或超过大小限制"));
    }

    @Test
    void 超过五MB返回4001() throws Exception {
        buildMockMvc();
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

        mockMvc.perform(multipart("/admin/upload/image").file(file))
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void 空文件返回4001() throws Exception {
        buildMockMvc();
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/admin/upload/image").file(file))
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void 内容类型不是图片返回4001() throws Exception {
        buildMockMvc();
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "text/plain",
                new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/admin/upload/image").file(file))
                .andExpect(jsonPath("$.code").value(4001));
    }
}
