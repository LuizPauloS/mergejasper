package br.com.lsilva.mergejasperdemo.service;

import br.com.lsilva.mergejasperdemo.model.Documento;
import br.com.lsilva.mergejasperdemo.model.ProcessoDigital;
import br.com.lsilva.mergejasperdemo.repository.ProcessoRepository;
import br.com.lsilva.mergejasperdemo.storage.FileStorageServiceImp;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

@Service
public class ProcessoDigitalService {

    @Autowired
    private ProcessoRepository repository;
    @Autowired
    private FileStorageServiceImp storageService;

    public ProcessoDigital savePdf(ProcessoDigital processo) {
        return this.repository.save(processo);
    }

    public ProcessoDigital saveDocumentProcesso(Integer id,
                                                          Integer prioridade,
                                                          boolean respeitarPrioridade,
                                                          MultipartFile multipartFile) {
        Optional<ProcessoDigital> optionalProcessoDigital = this.repository.findById(id);
        if (optionalProcessoDigital.isPresent()) {
            if (optionalProcessoDigital.get().getDocumentos() == null) {
                optionalProcessoDigital.get().setDocumentos(new ArrayList<>());
            }
            optionalProcessoDigital.get().getDocumentos()
                    .add(buildDocumento(prioridade, respeitarPrioridade, optionalProcessoDigital.get(), multipartFile));
        }
        return repository.save(optionalProcessoDigital.get());
    }

    private Documento buildDocumento(Integer prioridade,
                                     boolean respeitarPrioridade,
                                     ProcessoDigital processoDigital,
                                     MultipartFile multipartFile) {
        return Documento.builder()
                .nome(FilenameUtils.getBaseName(multipartFile.getOriginalFilename()))
                .dataCriacao(new Date())
                .arquivo(storageService.saveDocument(multipartFile))
                .prioridade(prioridade)
                .respeitarPrioridade(respeitarPrioridade)
                .processoDigital(processoDigital)
                .build();
    }
}
