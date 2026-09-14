package com.svtu.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    // 本地存储路径
    private static final String BASE_PATH = "D:/upload/avatar/";

    public String upload(MultipartFile file) throws IOException {
        // 1. 判空
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 2. 获取文件后缀  .jpg .png
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 3. 生成唯一文件名
        String fileName = UUID.randomUUID() + suffix;

        // 4. 创建文件夹
        File folder = new File(BASE_PATH);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // 5. 保存文件
        File targetFile = new File(folder, fileName);
        file.transferTo(targetFile);

        // 6. 返回可访问路径（给前端用）
        return "/avatar/" + fileName;
    }
}