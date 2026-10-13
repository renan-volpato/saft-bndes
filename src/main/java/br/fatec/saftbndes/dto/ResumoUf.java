package br.fatec.saftbndes.dto;

import java.math.BigDecimal;

public record ResumoUf(String uf, Long quantidade, BigDecimal valorTotal) {
}