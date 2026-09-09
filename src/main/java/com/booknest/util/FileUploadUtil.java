package com.booknest.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUploadUtil {

    public static String saveFile(String uploadSubDir, MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            originalFilename = "file";
        }
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
        
        // Base paths for development runtime and source directory
        String srcPathStr = "src/main/resources/static/uploads/" + uploadSubDir;
        String targetPathStr = "target/classes/static/uploads/" + uploadSubDir;

        Path srcUploadPath = Paths.get(srcPathStr).toAbsolutePath();
        if (!Files.exists(srcUploadPath)) {
            Files.createDirectories(srcUploadPath);
        }

        Path srcFilePath = srcUploadPath.resolve(fileName);
        Files.copy(multipartFile.getInputStream(), srcFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Also copy to target build directory if present for instant live reload
        Path targetUploadPath = Paths.get(targetPathStr).toAbsolutePath();
        if (Files.exists(targetUploadPath.getParent())) {
            if (!Files.exists(targetUploadPath)) {
                Files.createDirectories(targetUploadPath);
            }
            Path targetFilePath = targetUploadPath.resolve(fileName);
            Files.copy(srcFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/uploads/" + uploadSubDir + "/" + fileName;
    }
}
