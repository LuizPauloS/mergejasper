package br.com.lsilva.mergejasperdemo.controller;

import br.com.lsilva.mergejasperdemo.model.ProcessoDigital;
import br.com.lsilva.mergejasperdemo.service.ProcessoDigitalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/processo")
public class ProcessoDigitalController {

    @Autowired
    private ProcessoDigitalService service;

    @PostMapping("/save")
    public ResponseEntity<?> saveProcesso(@Valid @RequestBody ProcessoDigital processo) {
        ProcessoDigital processoDigital = service.savePdf(processo);
        if (processoDigital != null) {
            return ResponseEntity.ok().body(processoDigital);
        }
        return ResponseEntity.badRequest().build();
    }

    @PutMapping("/upload/{id}")
    public ResponseEntity<?> uploadDocument(@PathVariable("id") Integer id,
                                            @RequestParam(value = "prioridade", required = false) Integer prioridade,
                                            @RequestParam(value = "respeitarPrioridade", required = false) boolean respeitarPrioridade,
                                            @RequestParam("file") MultipartFile multipartFile) throws Exception {
        ProcessoDigital processoDigital = service.saveDocumentProcesso(id, prioridade, respeitarPrioridade, multipartFile);
        if (processoDigital != null) {
            return ResponseEntity.ok(String.format("Documento adicionado com sucesso ao processo: %s",
                    processoDigital.getNumeroProcesso()));
        }
        return ResponseEntity.badRequest().build();
    }
}
