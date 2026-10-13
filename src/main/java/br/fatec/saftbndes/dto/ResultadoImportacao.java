package br.fatec.saftbndes.dto;

import java.util.List;
import java.util.Map;

public record ResultadoImportacao(
        int linhasLidas,
        int importadas,
        int rejeitadas,
        int duplicadas,
        String mensagem,
        Map<String, Integer> motivosRejeicao,
        List<String> exemplosRejeicao) {
}