package br.fatec.saftbndes.repository;

import br.fatec.saftbndes.dto.ResumoUf;
import br.fatec.saftbndes.model.Operacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

    @Query(value = """
            SELECT o FROM Operacao o
            WHERE (:uf = '' OR o.uf = :uf)
              AND LOWER(o.setorBndes) LIKE LOWER(CONCAT('%', :setor, '%'))
              AND o.valorContratado >= :valorMin
              AND o.valorContratado <= :valorMax
            ORDER BY o.dataContratacao DESC, o.id DESC
            """,
            countQuery = """
            SELECT COUNT(o) FROM Operacao o
            WHERE (:uf = '' OR o.uf = :uf)
              AND LOWER(o.setorBndes) LIKE LOWER(CONCAT('%', :setor, '%'))
              AND o.valorContratado >= :valorMin
              AND o.valorContratado <= :valorMax
            """)
    Page<Operacao> filtrar(@Param("uf") String uf,
                           @Param("setor") String setor,
                           @Param("valorMin") BigDecimal valorMin,
                           @Param("valorMax") BigDecimal valorMax,
                           Pageable pageable);

    // Agrupamento do gráfico: quantidade e valor total por UF
    @Query("""
            SELECT new br.fatec.saftbndes.dto.ResumoUf(o.uf, COUNT(o), SUM(o.valorContratado))
            FROM Operacao o
            GROUP BY o.uf
            ORDER BY SUM(o.valorContratado) DESC
            """)
    List<ResumoUf> resumoPorUf();
}