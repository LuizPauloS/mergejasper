package br.com.lsilva.mergejasperdemo.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static java.nio.file.FileSystems.getDefault;

@Slf4j
@Component
public class FileStorageServiceImp implements FileStorageService {

    private static final String DEST = "C:/Users/lsilva/Documents/Projetos/uploads/";
    //private static final String DEST = "/home/luiz/Documentos/uploads/";

    public FileStorageServiceImp() {
        try {
            File dir = new File(DEST);
            if (!dir.exists()) {
                System.out.println(String.format("Criando diretório de uploads de arquivos - %s", DEST));
                dir.mkdirs();
            }
            dir.getParentFile().mkdirs();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar diretório de uploads", e);
        }
    }

    @Override
    public String saveDocument(MultipartFile document) {
        String nameDocument = document.getOriginalFilename();
        String urlDocument;
        try {
            urlDocument = DEST + nameDocument;
            document.transferTo(new File(urlDocument));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Erro ao salvar arquivo", e);
        }
        return urlDocument;
    }
}
