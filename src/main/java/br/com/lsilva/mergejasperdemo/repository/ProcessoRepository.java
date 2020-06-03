package br.com.lsilva.mergejasperdemo.repository;

import br.com.lsilva.mergejasperdemo.model.ProcessoDigital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessoRepository extends JpaRepository<ProcessoDigital, Integer> {
}
