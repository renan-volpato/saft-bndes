package br.fatec.saftbndes.controller;

import br.fatec.saftbndes.dto.ResultadoImportacao;
import br.fatec.saftbndes.dto.ResumoUf;
import br.fatec.saftbndes.model.Operacao;
import br.fatec.saftbndes.service.ImportacaoService;
import br.fatec.saftbndes.service.OperacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/operacoes")
public class OperacaoController {

    private final OperacaoService service;
    private final ImportacaoService importacaoService;

    public OperacaoController(OperacaoService service, ImportacaoService importacaoService) {
        this.service = service;
        this.importacaoService = importacaoService;
    }

    @GetMapping
    public ResponseEntity<List<Operacao>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    // Sem "size" devolve tudo; com "page" e "size" devolve uma página.
    // O total de registros que atendem ao filtro vai no cabeçalho X-Total-Count.
    @GetMapping("/filtro")
    public ResponseEntity<List<Operacao>> filtrar(
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String setor,
            @RequestParam(required = false) BigDecimal valorMin,
            @RequestParam(required = false) BigDecimal valorMax,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Pageable pageable = (size == null)
                ? Pageable.unpaged()
                : PageRequest.of(page == null ? 0 : Math.max(page, 0),
                                 Math.min(Math.max(size, 1), 100));
        Page<Operacao> resultado = service.filtrar(uf, setor, valorMin, valorMax, pageable);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(resultado.getTotalElements()))
                .body(resultado.getContent());
    }

    // Dados agrupados por UF para o gráfico
    @GetMapping("/resumo-uf")
    public ResponseEntity<List<ResumoUf>> resumoPorUf() {
        return ResponseEntity.ok(service.resumoPorUf());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operacao> buscar(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Operacao> cadastrar(@Valid @RequestBody Operacao operacao) {
        Operacao salva = service.salvar(operacao);
        return ResponseEntity.created(URI.create("/api/operacoes/" + salva.getId())).body(salva);
    }

    @PostMapping("/importar")
    public ResponseEntity<ResultadoImportacao> importar(
            @RequestParam(value = "arquivo", required = false) MultipartFile arquivo) {
        return ResponseEntity.ok(importacaoService.importar(arquivo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Operacao> atualizar(@PathVariable Long id,
                                              @Valid @RequestBody Operacao operacao) {
        return service.atualizar(id, operacao)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        return service.excluir(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}