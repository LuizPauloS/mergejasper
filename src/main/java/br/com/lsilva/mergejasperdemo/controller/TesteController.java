package br.com.lsilva.mergejasperdemo.controller;

import br.com.lsilva.mergejasperdemo.service.MergeWithTocService;
import br.com.lsilva.mergejasperdemo.service.ReduceSizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequestMapping("/teste")
@RestController
public class TesteController {

    @Autowired
    private MergeWithTocService mergeWithTocService;

    @Autowired
    private ReduceSizeService reduceSizeService;

    @GetMapping("/{idProcesso}")
    public ResponseEntity<?> getPdf(@PathVariable("idProcesso") Integer id) throws Exception {
        File documento = reduceSizeService.manipulatePdf();
        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + documento.getName());
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        Path path = Paths.get(documento.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(documento.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }
}
