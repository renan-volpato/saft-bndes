package br.fatec.saftbndes.service;

import br.fatec.saftbndes.model.Operacao;
import br.fatec.saftbndes.repository.OperacaoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.fatec.saftbndes.dto.ResumoUf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OperacaoService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("999999999999999");

    private final OperacaoRepository repository;

    public OperacaoService(OperacaoRepository repository) {
        this.repository = repository;
    }

    public List<Operacao> listar() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "dataContratacao", "id"));
    }

    public Optional<Operacao> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Operacao salvar(Operacao operacao) {
        operacao.setId(null); // garante INSERT
        normalizar(operacao);
        return repository.save(operacao);
    }

    public Optional<Operacao> atualizar(Long id, Operacao dados) {
        return repository.findById(id).map(existente -> {
            normalizar(dados);
            existente.setCliente(dados.getCliente());
            existente.setCnpj(dados.getCnpj());
            existente.setNumeroContrato(dados.getNumeroContrato());
            existente.setUf(dados.getUf());
            existente.setMunicipio(dados.getMunicipio());
            existente.setDataContratacao(dados.getDataContratacao());
            existente.setValorContratado(dados.getValorContratado());
            existente.setValorDesembolsado(dados.getValorDesembolsado());
            existente.setProduto(dados.getProduto());
            existente.setSetorBndes(dados.getSetorBndes());
            existente.setPorteCliente(dados.getPorteCliente());
            existente.setInovacao(dados.getInovacao());
            return repository.save(existente);
        });
    }

    public boolean excluir(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    public Page<Operacao> filtrar(String uf, String setor, BigDecimal valorMin,
                                  BigDecimal valorMax, Pageable pageable) {
        String ufFiltro = (uf == null) ? "" : uf.trim().toUpperCase();
        String setorFiltro = (setor == null) ? "" : setor.trim();
        BigDecimal min = (valorMin == null) ? BigDecimal.ZERO : valorMin;
        BigDecimal max = (valorMax == null) ? VALOR_MAXIMO : valorMax;
        return repository.filtrar(ufFiltro, setorFiltro, min, max, pageable);
    }

    public List<ResumoUf> resumoPorUf() {
        return repository.resumoPorUf();
    }

    // Padroniza os textos antes de gravar (regra de negócio do tratamento)
    private void normalizar(Operacao o) {
        o.setCliente(limpar(o.getCliente()));
        o.setCnpj(limpar(o.getCnpj()));
        o.setNumeroContrato(limpar(o.getNumeroContrato()));
        o.setUf(o.getUf() == null ? null : o.getUf().trim().toUpperCase());
        o.setMunicipio(limpar(o.getMunicipio()));
        o.setProduto(limpar(o.getProduto()));
        o.setSetorBndes(limpar(o.getSetorBndes()));
        o.setPorteCliente(limpar(o.getPorteCliente()));
        o.setInovacao(limpar(o.getInovacao()));
    }

    private String limpar(String texto) {
        if (texto == null) return null;
        String t = texto.trim().replaceAll("\\s+", " ");
        return t.isEmpty() || t.matches("-+") ? null : t; // vazio e "----------" viram null
    }
}