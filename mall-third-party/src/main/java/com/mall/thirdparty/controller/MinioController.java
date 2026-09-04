package com.mall.thirdparty.controller;

import cn.hutool.core.lang.UUID;
import com.mall.common.utils.Result;
import com.mall.thirdparty.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/file")
@Slf4j
public class MinioController {

    @Autowired
    private MinioService minioService;

    /**
     * 允许上传的文件类型前缀（逗号分隔），默认仅图片；
     * 配置示例: image/,application/pdf
     */
    @Value("${minio.allowedContentTypes:image/}")
    private String[] allowedContentTypes;

    /**
     * 单文件大小上限（MB），默认 10；可配置: minio.maxFileSizeMB
     */
    @Value("${minio.maxFileSizeMB:10}")
    private long maxFileSizeMB;

    /**
     * 单文件上传
     * 可选参数 folder：指定上传目录（如 dataset_documents 或 document/dataset_documents），
     * 不传时按文件类型自动归类（image/document/audio/video/other）
     */
    @PostMapping("/upload")
    public ResponseEntity<Result<String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {
        try {
            // 参数校验
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("文件不能为空"));
            }

            // 文件大小校验（可配置：minio.maxFileSizeMB，默认10MB）
            if (file.getSize() > maxFileSizeMB * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("文件大小不能超过" + maxFileSizeMB + "MB"));
            }

            // 文件类型校验（白名单可配置：minio.allowedContentTypes，默认仅图片）
            String contentType = file.getContentType();
            boolean typeAllowed = contentType != null && Arrays.stream(allowedContentTypes)
                    .anyMatch(prefix -> contentType.startsWith(prefix));
            if (contentType != null && !typeAllowed) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("不支持的文件类型: " + contentType));
            }

            String fileName = generateFileName(file.getOriginalFilename());
            String fileUrl = minioService.uploadFile(file, fileName, folder);
            log.info("文件上传成功: {}", fileUrl);
            return ResponseEntity.ok(Result.success(fileUrl));
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail("文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 多文件上传
     * 可选参数 folder：指定上传目录，不传时按文件类型自动归类
     */
    @PostMapping("/upload/multiple")
    public ResponseEntity<Result<List<String>>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", required = false) String folder) {
        try {
            // 参数校验
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("文件列表不能为空"));
            }

            // 检查是否有空文件
            boolean hasEmptyFile = Arrays.stream(files)
                    .anyMatch(file -> file == null || file.isEmpty());
            if (hasEmptyFile) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("文件列表中包含空文件"));
            }

            // 文件数量限制
            if (files.length > 10) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("一次最多上传10个文件"));
            }

            // 文件大小校验
            for (MultipartFile file : files) {
                if (file.getSize() > maxFileSizeMB * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body(Result.fail("文件 " + file.getOriginalFilename() + " 大小不能超过" + maxFileSizeMB + "MB"));
                }
            }

            List<String> fileUrls = minioService.uploadFiles(files, folder);
            log.info("多文件上传成功，共 {} 个文件", fileUrls.size());
            return ResponseEntity.ok(Result.success(fileUrls));
        } catch (Exception e) {
            log.error("多文件上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail("多文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Result<Void>> deleteFile(@RequestParam String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("文件URL不能为空"));
            }

            // 从URL中提取文件路径
            String filePath = minioService.extractFilePathFromUrl(fileUrl);
            log.info("删除文件 - 原始URL: {}, 提取路径: {}", fileUrl, filePath);
            minioService.deleteFile(filePath);
            log.info("文件删除成功: {}", filePath);
            return ResponseEntity.ok(Result.success(null));
        } catch (Exception e) {
            log.error("文件删除失败: {}", fileUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail("文件删除失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除文件
     */
    @DeleteMapping("/delete/batch")
    public ResponseEntity<Result<Void>> deleteFiles(@RequestBody List<String> fileUrls) {
        try {
            if (fileUrls == null || fileUrls.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("文件URL列表不能为空"));
            }

            // 从URL中提取文件路径
            List<String> filePaths = new ArrayList<>();
            for (String fileUrl : fileUrls) {
                String filePath = minioService.extractFilePathFromUrl(fileUrl);
                filePaths.add(filePath);
                log.info("批量删除 - 原始URL: {}, 提取路径: {}", fileUrl, filePath);
            }

            minioService.deleteFiles(filePaths);
            log.info("批量删除文件成功，共 {} 个文件", filePaths.size());
            return ResponseEntity.ok(Result.success(null));
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail("批量删除文件失败: " + e.getMessage()));
        }
    }

    /**
     * 获取上传凭证
     */
    @GetMapping("/credential")
    public ResponseEntity<Result<String>> getUploadCredential(@RequestParam String fileName) {
        try {
            if (fileName == null || fileName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Result.fail("文件名不能为空"));
            }

            String credential = minioService.getUploadCredential(fileName);
            return ResponseEntity.ok(Result.success(credential));
        } catch (Exception e) {
            log.error("获取上传凭证失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.fail("获取上传凭证失败: " + e.getMessage()));
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Result<String>> healthCheck() {
        return ResponseEntity.ok(Result.success("File service is running"));
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String originalFileName) {
        if (originalFileName == null) {
            return UUID.randomUUID().toString();
        }

        String suffix = "";
        if (originalFileName.contains(".")) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + suffix;
    }
}