package com.example.smart_learning.storage;

import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorage implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif", "svg", "pdf", "mp4", "zip", "txt");
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "course", "avatar", "article", "material", "banner", "common");

    private final Path root;

    public LocalFileStorage(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }
        String safeCategory = category == null || category.isBlank() ? "common" : category.trim();
        if (!ALLOWED_CATEGORIES.contains(safeCategory)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的上传分类");
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directory = root.resolve(safeCategory).normalize();
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法文件路径");
        }
        try {
            Files.createDirectories(directory);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }
        return new StoredFile("/uploads/" + safeCategory + "/" + storedName, originalName, file.getSize());
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
