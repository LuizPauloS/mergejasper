package br.com.lsilva.mergejasperdemo.model;

import br.com.lsilva.mergejasperdemo.model.enums.TipoProcesso;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@ToString
public class ProcessoDigital implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    private TipoProcesso tipoProcesso;
    @NotBlank
    private String numeroProcesso;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "processo_id")
    List<Documento> documentos;
}
