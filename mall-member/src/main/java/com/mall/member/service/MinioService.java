package com.mall.member.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import com.mall.member.config.MinioConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * MinIO 上传服务：用于把微博/微信头像等转存到本地 MinIO，返回本地可访问 URL
 */
@Service
@Slf4j
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    @Value("${minio.bucketName:default}")
    private String bucketName;

    /**
     * 上传头像字节，返回公开访问 URL（endpoint/bucket/avatar/日期/uuid.ext）
     */
    public String uploadAvatar(byte[] data, String contentType, String ext) throws Exception {
        createBucketIfNotExists();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String object = "avatar/" + df.format(new Date()) + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(object)
                        .stream(new ByteArrayInputStream(data), data.length, -1)
                        .contentType(contentType == null ? "image/jpeg" : contentType)
                        .build()
        );
        return publicUrl(object);
    }

    private String publicUrl(String object) {
        String ep = minioConfig.getEndpoint();
        if (ep.endsWith("/")) {
            ep = ep.substring(0, ep.length() - 1);
        }
        return ep + "/" + bucketName + "/" + object;
    }

    private void createBucketIfNotExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("创建MinIO存储桶: {}", bucketName);
        }
        // 确保桶为公开读，否则上传后的 URL 无法直接访问
        setBucketPublicPolicy();
    }

    private void setBucketPublicPolicy() {
        try {
            String policy = String.format(
                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}",
                    bucketName);
            minioClient.setBucketPolicy(
                    io.minio.SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
        } catch (Exception e) {
            log.warn("设置存储桶公开读失败: " + e.getMessage());
        }
    }
}
