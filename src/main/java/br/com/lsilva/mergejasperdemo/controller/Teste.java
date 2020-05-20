package br.com.lsilva.mergejasperdemo.controller;

import br.com.lsilva.mergejasperdemo.service.TesteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RequestMapping("/teste")
@RestController
public class Teste {

    @Autowired
    private TesteService service;

    @GetMapping
    public ResponseEntity<?> getPdf() throws IOException {
        return ResponseEntity.ok(this.service.getPdfFormat());
    }
}
