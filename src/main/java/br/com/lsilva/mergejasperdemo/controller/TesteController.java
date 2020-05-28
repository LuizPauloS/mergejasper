package br.com.lsilva.mergejasperdemo.controller;

import br.com.lsilva.mergejasperdemo.service.TesteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/teste")
@RestController
public class TesteController {

    @Autowired
    private TesteService service;

    @GetMapping
    public ResponseEntity<?> getPdf() throws Exception {
        byte[] documento = this.service.getPdfDocumentImage();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok().headers(headers).contentLength(documento.length)
                .contentType(MediaType.parseMediaType("application/pdf"))
                .body(documento);
    }
}
