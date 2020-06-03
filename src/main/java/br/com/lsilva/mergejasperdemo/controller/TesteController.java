package br.com.lsilva.mergejasperdemo.controller;

import br.com.lsilva.mergejasperdemo.service.MergeWithTocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
