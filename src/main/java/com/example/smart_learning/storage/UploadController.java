package com.example.smart_learning.storage;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.security.RequiresPriv;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/upload")
public class UploadController {

    private final FileStorageService fileStorageService;

    public UploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    @RequiresPriv(module = "file", method = "upload")
    public ApiResponse<FileStorageService.StoredFile> upload(@RequestParam("file") MultipartFile file,
                                                             @RequestParam(defaultValue = "common") String category) {
        return ApiResponse.success(fileStorageService.store(file, category));
    }
}
