package com.mall.thirdparty.service;

import cn.hutool.core.lang.UUID;
import io.minio.*;
import io.minio.http.Method;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.mall.thirdparty.config.MinioConfig;

import java.net.URI;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * -- GETTER --
     * 获取存储桶名称
     */
    @Getter
    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 预签名 URL 有效期（天），默认 7 天
     */
    @Value("${minio.urlExpiryDays:7}")
    private int urlExpiryDays;

    /**
     * 是否公开读模式：true 时上传直接返回永久公开 URL（需存储桶为公开读策略），false 返回预签名 URL
     */
    @Value("${minio.publicRead:false}")
    private boolean publicRead;

    /**
     * 上传文件到按类型+日期分组的文件夹（如 image/2026-08-22/xxx.jpg）
     * folder 非空时使用指定目录（如 dataset_documents/2026-08-22/xxx），否则按类型自动归类
     */
    public String uploadFile(MultipartFile file, String fileName, String folder) throws Exception {
        // 确保存储桶存在
        createBucketIfNotExists();

        // 生成带类型/指定目录+日期文件夹的文件路径
        String filePath = generateDateFolderPath(fileName, file.getContentType(), folder);

        // 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filePath)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        // 公开读模式返回永久 URL，否则返回预签名 URL
        return publicRead ? getPublicFileUrl(filePath) : getFileUrl(filePath);
    }

    /**
     * 多文件上传到按类型+日期分组的文件夹
     */
    public List<String> uploadFiles(MultipartFile[] files, String folder) throws Exception {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String fileName = generateFileName(file.getOriginalFilename());
            String url = uploadFile(file, fileName, folder);
            urls.add(url);
        }
        return urls;
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build()
        );
    }

    /**
     * 批量删除文件
     */
    public void deleteFiles(List<String> fileNames) throws Exception {
        for (String fileName : fileNames) {
            deleteFile(fileName);
        }
    }

    /**
     * 获取文件访问URL（预签名，有效期可配置）
     */
    public String getFileUrl(String filePath) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(filePath)
                        .expiry(urlExpiryDays, TimeUnit.DAYS)
                        .build()
        );
    }

    /**
     * 获取公开访问URL（永久有效，需存储桶为公开读策略）
     * 格式: endpoint/bucketName/文件路径（path-style）
     */
    public String getPublicFileUrl(String filePath) {
        String endpoint = minioConfig.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + bucketName + "/" + filePath;
    }

    /**
     * 获取上传凭证（预签名URL）
     */
    public String getUploadCredential(String fileName) throws Exception {
        // 生成带类型+日期文件夹的文件路径（凭证场景无 Content-Type，按扩展名归类）
        String filePath = generateDateFolderPath(fileName, null, null);

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketName)
                        .object(filePath)
                        .expiry(1, TimeUnit.HOURS) // 1小时有效期
                        .build()
        );
    }

    /**
     * 创建存储桶（如果不存在）
     */
    private void createBucketIfNotExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("创建MinIO存储桶: {}", bucketName);

            // 设置存储桶策略为公开读（可选，根据需求调整）
            setBucketPublicPolicy();
        }
    }

    /**
     * 设置存储桶策略为公开读（可选）
     */
    private void setBucketPublicPolicy() {
        try {
            String policy = String.format(
                    "{" +
                            "\"Version\": \"2012-10-17\"," +
                            "\"Statement\": [" +
                            "    {" +
                            "        \"Effect\": \"Allow\"," +
                            "        \"Principal\": \"*\"," +
                            "        \"Action\": [" +
                            "            \"s3:GetObject\"" +
                            "        ]," +
                            "        \"Resource\": [" +
                            "            \"arn:aws:s3:::%s/*\"" +
                            "        ]" +
                            "    }" +
                            "]" +
                            "}", bucketName);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
            log.info("设置存储桶 {} 为公开读策略", bucketName);
        } catch (Exception e) {
            log.warn("设置存储桶策略失败: {}", e.getMessage());
        }
    }

    /**
     * 生成按目录+日期分组的文件路径
     * 格式: 目录/yyyy-MM-dd/原始文件名
     * folder 非空 → folder/日期/文件；为空 → 按类型目录/日期/文件
     */
    private String generateDateFolderPath(String fileName, String contentType, String folder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateFolder = dateFormat.format(new Date());
        String dir = (folder != null && !folder.trim().isEmpty())
                ? folder.trim()
                : generateTypeFolder(fileName, contentType);
        return dir + "/" + dateFolder + "/" + fileName;
    }

    /**
     * 根据 Content-Type / 扩展名归类文件类型目录
     */
    private String generateTypeFolder(String fileName, String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (ct.startsWith("image/")) {
            return "image";
        } else if (ct.startsWith("audio/")) {
            return "audio";
        } else if (ct.startsWith("video/")) {
            return "video";
        }
        // 文档类（按扩展名兜底）
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt")
                || lower.endsWith(".csv") || lower.endsWith(".pdf") || lower.endsWith(".doc")
                || lower.endsWith(".docx") || lower.endsWith(".xls") || lower.endsWith(".xlsx")
                || lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".html")
                || lower.endsWith(".htm")) {
            return "document";
        }
        return "other";
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String originalFileName) {
        String suffix = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + suffix;
    }

    /**
     * 从文件URL中提取文件路径
     * 支持 path-style（http://host:9000/bucket/2025-11-01/a.png）
     * 和 virtual-host-style（http://bucket.host/2025-11-01/a.png）
     */
    public String extractFilePathFromUrl(String fileUrl) {
        try {
            log.info("原始文件URL: {}", fileUrl);

            // 1. URL解码
            String decodedUrl = URLDecoder.decode(fileUrl, "UTF-8");
            // 2. 移除查询参数与锚点
            String cleanUrl = decodedUrl.split("[?#]")[0];
            log.info("清理后URL: {}", cleanUrl);

            URI uri = URI.create(cleanUrl);
            String host = uri.getHost() == null ? "" : uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath();

            String bucketName = minioConfig.getBucketName();

            // 3. path-style: /bucketName/xxx/yyy.png
            String searchPattern = "/" + bucketName + "/";
            int bucketIndex = path.indexOf(searchPattern);
            if (bucketIndex != -1) {
                String filePath = path.substring(bucketIndex + searchPattern.length());
                log.info("✅ path-style 提取的文件路径: {}", filePath);
                return filePath;
            }

            // 4. virtual-host-style: bucketName.host/xxx/yyy.png
            if (host.startsWith(bucketName + ".")) {
                String filePath = path.startsWith("/") ? path.substring(1) : path;
                log.info("✅ virtual-host-style 提取的文件路径: {}", filePath);
                return filePath;
            }

            // 5. 兜底：按日期目录解析
            String[] segments = path.split("/");
            for (int i = 0; i < segments.length; i++) {
                if (segments[i].matches("\\d{4}-\\d{2}-\\d{2}") && i + 1 < segments.length) {
                    return segments[i] + "/" + segments[i + 1];
                }
            }

            // 6. 最后尝试：返回路径的最后一部分
            String lastPart = path.substring(path.lastIndexOf("/") + 1);
            log.info("⚠️ 使用最后一部分作为文件路径: {}", lastPart);
            return lastPart;

        } catch (Exception e) {
            log.error("URL解析失败: {}", e.getMessage());
            // 返回原始URL（不含查询参数）
            return fileUrl.split("[?#]")[0];
        }
    }
}