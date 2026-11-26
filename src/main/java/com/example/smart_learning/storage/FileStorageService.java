package com.example.smart_learning.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file, String category);

    record StoredFile(String url, String originalName, long size) {
    }
}
