package br.com.lsilva.mergejasperdemo.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String saveDocument(MultipartFile document);
}
