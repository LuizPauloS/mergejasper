package br.com.lsilva.mergejasperdemo.controller;

import br.com.lsilva.mergejasperdemo.service.MergeWithTocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequestMapping("/teste")
@RestController
public class TesteController {

    @Autowired
    private MergeWithTocService mergeWithTocService;

    @GetMapping("/{idProcesso}")
    public ResponseEntity<?> getPdf(@PathVariable("idProcesso") Integer id) throws Exception {
        byte[] documento = mergeWithTocService.manipulatePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok().headers(headers).contentLength(documento.length)
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(documento);
    }

    private static final String SERVER_LOCATION = "/templates/pdf/Java 8 Pratico Lambdas Streams E os Novos Recursos da Linguagem.pdf";

    @GetMapping("/download")
    public ResponseEntity<?> download() throws IOException {
        File file = new File(new ClassPathResource(SERVER_LOCATION).getURI());

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=java8.pdf");
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }
}
