package br.fatec.saftbndes.service;

import br.fatec.saftbndes.dto.ResultadoImportacao;
import br.fatec.saftbndes.exception.ImportacaoException;
import br.fatec.saftbndes.model.Operacao;
import br.fatec.saftbndes.repository.OperacaoRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImportacaoService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_EXEMPLOS = 20;
    private static final Set<String> COLUNAS_OBRIGATORIAS = Set.of(
            "cliente", "numero_do_contrato", "uf", "data_da_contratacao",
            "valor_contratado_reais", "produto", "setor_bndes");

    private final OperacaoRepository repository;
    private final Validator validator;

    public ImportacaoService(OperacaoRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    // Transacional: um erro estrutural desfaz tudo; registros inválidos são apenas rejeitados.
    @Transactional
    public ResultadoImportacao importar(MultipartFile arquivo) {
        validarArquivo(arquivo);
        String texto = lerTexto(arquivo);
        char delimitador = detectarDelimitador(texto);

        // chaves dos registros que já existem no banco (detecção de duplicidade)
        Set<String> chaves = new HashSet<>();
        repository.findAll().forEach(o -> chaves.add(chave(o)));

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimitador)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get(); 

        int lidas = 0, rejeitadas = 0, duplicadas = 0;
        Map<String, Integer> motivos = new LinkedHashMap<>();
        List<String> exemplos = new ArrayList<>();
        List<Operacao> validas = new ArrayList<>();

        try (CSVParser parser = formato.parse(new StringReader(texto))) {
            Map<String, String> colunas = new HashMap<>();
            for (String nome : parser.getHeaderNames()) {
                colunas.put(normalizar(nome), nome);
            }
            validarCabecalho(colunas);

            for (CSVRecord registro : parser) {
                if (linhaVazia(registro)) {
                     continue; // ignora linhas sem nenhum dado (ex.: ";;;;;" gerado pelo Excel)
                }
                lidas++;
                long numero = registro.getRecordNumber();

                if (!registro.isConsistent()) {
                    rejeitadas++;
                    rejeitar(motivos, exemplos, numero,
                            List.of("Quantidade de colunas diferente do cabeçalho"));
                    continue;
                }

                Operacao operacao;
                try {
                    operacao = converter(registro, colunas);
                } catch (IllegalArgumentException e) {
                    rejeitadas++;
                    rejeitar(motivos, exemplos, numero, List.of(e.getMessage()));
                    continue;
                }

                Set<ConstraintViolation<Operacao>> erros = validator.validate(operacao);
                if (!erros.isEmpty()) {
                    rejeitadas++;
                    List<String> mensagens = erros.stream()
                            .map(ConstraintViolation::getMessage)
                            .sorted()
                            .collect(Collectors.toList());
                    rejeitar(motivos, exemplos, numero, mensagens);
                    continue;
                }

                if (!chaves.add(chave(operacao))) {
                    duplicadas++;
                    continue;
                }
                validas.add(operacao);
            }
        } catch (IOException | UncheckedIOException | IllegalStateException e) {
            throw new ImportacaoException(
                    "Erro ao ler o CSV (separador ou aspas incorretos): " + e.getMessage());
        }

        if (lidas == 0) {
            throw new ImportacaoException("O arquivo tem cabeçalho, mas nenhum registro de dados.");
        }

        repository.saveAll(validas);

        String mensagem = String.format(
                "Importação concluída: %d linhas lidas, %d importadas, %d rejeitadas e %d duplicadas.",
                lidas, validas.size(), rejeitadas, duplicadas);
        return new ResultadoImportacao(lidas, validas.size(), rejeitadas, duplicadas,
                mensagem, motivos, exemplos);
    }

    // ---------- validações estruturais ----------

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ImportacaoException("Nenhum arquivo enviado ou arquivo vazio.");
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.toLowerCase().endsWith(".csv")) {
            throw new ImportacaoException("Extensão incompatível: envie um arquivo .csv.");
        }
    }

    private void validarCabecalho(Map<String, String> colunas) {
        List<String> faltando = COLUNAS_OBRIGATORIAS.stream()
                .filter(c -> !colunas.containsKey(c))
                .sorted()
                .collect(Collectors.toList());
        if (!faltando.isEmpty()) {
            throw new ImportacaoException(
                    "Cabeçalho inválido ou ausente. Colunas obrigatórias não encontradas: " + faltando);
        }
    }

    private String lerTexto(MultipartFile arquivo) {
        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new ImportacaoException("Não foi possível ler o arquivo enviado.");
        }
        String texto;
        try { // tenta UTF-8 estrito; se houver bytes inválidos, usa Windows-1252 (ISO-8859-1)
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            texto = new String(bytes, Charset.forName("windows-1252"));
        }
        if (texto.startsWith("\uFEFF")) {
            texto = texto.substring(1); // remove BOM
        }
        if (texto.isBlank()) {
            throw new ImportacaoException("O arquivo está vazio.");
        }
        return texto;
    }

    private char detectarDelimitador(String texto) {
        int fim = texto.indexOf('\n');
        String cabecalho = fim < 0 ? texto : texto.substring(0, fim);
        long tabs = cabecalho.chars().filter(c -> c == '\t').count();
        long pontoVirgulas = cabecalho.chars().filter(c -> c == ';').count();
        long virgulas = cabecalho.chars().filter(c -> c == ',').count();
        long maior = Math.max(tabs, Math.max(pontoVirgulas, virgulas));
        if (maior == 0) {
            throw new ImportacaoException(
                    "Separador não reconhecido no cabeçalho. Use tabulação, ponto e vírgula ou vírgula.");
        }
        if (maior == tabs) return '\t';
        if (maior == pontoVirgulas) return ';';
        return ',';
    }

    // ---------- conversão e tratamento de cada linha ----------

    private Operacao converter(CSVRecord r, Map<String, String> col) {
        Operacao o = new Operacao();
        o.setCliente(texto(r, col, "cliente"));
        o.setCnpj(texto(r, col, "cnpj"));
        o.setNumeroContrato(texto(r, col, "numero_do_contrato"));
        String uf = texto(r, col, "uf");
        o.setUf(uf == null ? null : uf.toUpperCase());
        o.setMunicipio(texto(r, col, "municipio"));
        o.setDataContratacao(data(texto(r, col, "data_da_contratacao")));
        o.setValorContratado(valor(texto(r, col, "valor_contratado_reais"), "Valor contratado"));
        o.setValorDesembolsado(valor(texto(r, col, "valor_desembolsado_reais"), "Valor desembolsado"));
        o.setProduto(texto(r, col, "produto"));
        o.setSetorBndes(texto(r, col, "setor_bndes"));
        o.setPorteCliente(texto(r, col, "porte_do_cliente"));
        o.setInovacao(texto(r, col, "inovacao"));
        return o;
    }

    private String texto(CSVRecord r, Map<String, String> col, String nome) {
        String original = col.get(nome);
        if (original == null || !r.isSet(original)) return null;
        return limpar(r.get(original));
    }

    // trim, espaços repetidos e marcador "----------" viram texto limpo ou null
    private String limpar(String t) {
        if (t == null) return null;
        String s = t.trim().replaceAll("\\s+", " ");
        return (s.isEmpty() || s.matches("-+")) ? null : s;
    }

    private boolean linhaVazia(CSVRecord registro) {
    for (String valor : registro) {
        if (valor != null && !valor.isBlank()) {
            return false;
        }
    }
    return true;
    }

    private LocalDate data(String s) {
        if (s == null) return null;
        try {
            return LocalDate.parse(s, DATA_BR);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDate.parse(s); // aceita também aaaa-mm-dd
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Data inválida: " + s);
            }
        }
    }

    private BigDecimal valor(String s, String campo) {
        if (s == null) return null;
        String v = s.replace("R$", "").trim();
        if (v.contains(",")) {
            v = v.replace(".", "").replace(",", "."); // 1.234,56 ou 9007445,1
        }
        try {
            return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " inválido: " + s);
        }
    }

    private String normalizar(String nome) {
        return nome == null ? "" : nome.replace("\uFEFF", "").trim().toLowerCase();
    }

    // duplicidade = mesmo contrato + data + valor contratado + valor desembolsado
    private String chave(Operacao o) {
        return o.getNumeroContrato() + "|" + o.getDataContratacao() + "|"
                + plano(o.getValorContratado()) + "|" + plano(o.getValorDesembolsado());
    }

    private String plano(BigDecimal v) {
        return v == null ? "" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void rejeitar(Map<String, Integer> motivos, List<String> exemplos,
                          long numero, List<String> mensagens) {
        mensagens.forEach(m -> motivos.merge(m, 1, Integer::sum));
        if (exemplos.size() < MAX_EXEMPLOS) {
            exemplos.add("Registro " + numero + ": " + String.join("; ", mensagens));
        }
    }
}