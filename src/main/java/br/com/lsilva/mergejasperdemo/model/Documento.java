package br.com.lsilva.mergejasperdemo.model;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Data
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Documento implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotBlank
    private String nome;
    @NotBlank
    private String arquivo;
    private Integer prioridade;
    @NotNull
    private boolean respeitarPrioridade;
    @NotNull
    @Temporal(TemporalType.DATE)
    private Date dataCriacao;
    @NotNull
    @ManyToOne
    @JoinColumn(name = "processo_id")
    private ProcessoDigital processoDigital;
}
