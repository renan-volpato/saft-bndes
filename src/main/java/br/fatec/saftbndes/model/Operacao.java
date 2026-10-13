package br.fatec.saftbndes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "operacao")
public class Operacao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "operacao_seq")
    @SequenceGenerator(name = "operacao_seq", sequenceName = "operacao_seq", allocationSize = 50)
    private Long id;

    @NotBlank(message = "O cliente é obrigatório")
    @Size(max = 255)
    @Column(nullable = false)
    private String cliente;

    @Size(max = 20)
    @Column(length = 20)
    private String cnpj;

    @NotBlank(message = "O número do contrato é obrigatório")
    @Size(max = 50)
    @Column(name = "numero_contrato", nullable = false, length = 50)
    private String numeroContrato;

    @NotBlank(message = "A UF é obrigatória")
    @Size(min = 2, max = 2, message = "A UF deve ter 2 letras")
    @Column(nullable = false, length = 2)
    private String uf;

    @Size(max = 100)
    private String municipio;

    @NotNull(message = "A data da contratação é obrigatória")
    @Column(name = "data_contratacao", nullable = false)
    private LocalDate dataContratacao;

    @NotNull(message = "O valor contratado é obrigatório")
    @Positive(message = "O valor contratado deve ser maior que zero")
    @Column(name = "valor_contratado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorContratado;

    @PositiveOrZero(message = "O valor desembolsado não pode ser negativo")
    @Column(name = "valor_desembolsado", precision = 19, scale = 2)
    private BigDecimal valorDesembolsado;

    @NotBlank(message = "O produto é obrigatório")
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String produto;

    @NotBlank(message = "O setor é obrigatório")
    @Size(max = 100)
    @Column(name = "setor_bndes", nullable = false, length = 100)
    private String setorBndes;

    @Size(max = 30)
    @Column(name = "porte_cliente", length = 30)
    private String porteCliente;

    @Size(max = 3)
    @Column(length = 3)
    private String inovacao;

    public Operacao() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getNumeroContrato() { return numeroContrato; }
    public void setNumeroContrato(String numeroContrato) { this.numeroContrato = numeroContrato; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public LocalDate getDataContratacao() { return dataContratacao; }
    public void setDataContratacao(LocalDate dataContratacao) { this.dataContratacao = dataContratacao; }

    public BigDecimal getValorContratado() { return valorContratado; }
    public void setValorContratado(BigDecimal valorContratado) { this.valorContratado = valorContratado; }

    public BigDecimal getValorDesembolsado() { return valorDesembolsado; }
    public void setValorDesembolsado(BigDecimal valorDesembolsado) { this.valorDesembolsado = valorDesembolsado; }

    public String getProduto() { return produto; }
    public void setProduto(String produto) { this.produto = produto; }

    public String getSetorBndes() { return setorBndes; }
    public void setSetorBndes(String setorBndes) { this.setorBndes = setorBndes; }

    public String getPorteCliente() { return porteCliente; }
    public void setPorteCliente(String porteCliente) { this.porteCliente = porteCliente; }

    public String getInovacao() { return inovacao; }
    public void setInovacao(String inovacao) { this.inovacao = inovacao; }
}