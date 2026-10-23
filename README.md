# SAFT BNDES — Sistema de Apoio ao Financiamento Tecnológico

Aplicação web com **API REST em Spring Boot**, banco **PostgreSQL no Neon** e interface **HTML, CSS e JavaScript**, que importa, trata, armazena, consulta, filtra e apresenta graficamente os dados abertos oficiais de **operações de financiamento do BNDES**.

Projeto P2 da disciplina **Programação Avançada Orientada a Objetos** (Profa. Me. Sirley Ambrosia Vitorio Addão) · FATEC Ipiranga · Turma A924 · **Grupo 9**.

## Integrantes

- Renan Volpato Santos
- Kauã Sato Sposato

## Objetivo

Transformar um conjunto público de dados grande e complexo (34 colunas, cerca de 24 mil linhas) em informação organizada e acessível, apoiando consultas e análises sobre operações de financiamento, principalmente para pequenas empresas de tecnologia.

## Funcionalidades

- CRUD completo de operações de financiamento (cadastrar, listar, buscar por ID, editar e excluir)
- Busca por ID, com retorno 404 quando o registro não existe
- Filtros por **UF**, por **setor** e por **faixa de valor contratado**, com paginação na tabela
- **Importação do CSV oficial** pela interface (multipart/form-data), com relatório de linhas lidas, importadas, rejeitadas e duplicadas
- Contador de registros e mensagens de sucesso e erro
- Gráfico de barras: **em quais estados o BNDES concentra o financiamento?** (valor contratado ou quantidade de operações por UF)

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem e build | Java 17+, Maven (wrapper incluído) |
| Framework | Spring Boot 4.1.1 (Web, Data JPA, Validation) |
| Banco de dados | PostgreSQL publicado no Neon |
| Leitura do CSV | Apache Commons CSV |
| Interface | HTML, CSS e JavaScript (`fetch`), Chart.js via CDN |
| Testes manuais | Insomnia |

## Arquitetura

```
src/main/java/br/fatec/saftbndes/
├── model/          Operacao (entidade JPA)
├── repository/     OperacaoRepository (JpaRepository, filtros e agrupamento por UF)
├── service/        OperacaoService (regras de negócio) e ImportacaoService (leitura e tratamento do CSV)
├── controller/     OperacaoController (endpoints) e GlobalExceptionHandler (erros 400)
├── dto/            ResultadoImportacao, ResumoUf
└── exception/      ImportacaoException

src/main/resources/
├── application.properties      configuração com variáveis de ambiente
└── static/                     index.html, css/style.css, js/app.js
```

Fluxo dos dados: CSV oficial → seleção no navegador → `fetch()` com `FormData` → Controller → `ImportacaoService` (leitura, tratamento, validação) → Repository → PostgreSQL (Neon) → API → `fetch()` → tabela e gráfico.

## Fonte dos dados

| Item | Informação |
|---|---|
| Órgão | Banco Nacional de Desenvolvimento Econômico e Social (BNDES) |
| Portal | Portal de Dados Abertos do BNDES |
| Conjunto | Operações de Financiamento (dados a partir de 2002) |
| Link | https://dadosabertos.bndes.gov.br/dataset/operacoes-financiamento |
| Recurso utilizado | Operações não automáticas |
| Arquivo | `operacoes-financiamento-operacoes-nao-automaticas.csv` |
| Dicionário de dados | "Dicionário de dados de operações não automáticas" (PDF, na mesma página) |
| Formato | CSV, 34 colunas, separador `;`, codificação UTF-8, cabeçalho na primeira linha |
| Volume | 23.896 registros |
| Data de acesso | 12/10/2026 |
| Licença | Licença Aberta para Bases de Dados (ODbL) do Open Data Commons |

Fonte: BNDES, Portal de Dados Abertos, conjunto *Operações de Financiamento*.

### Como obter o arquivo

O CSV completo **não está versionado** neste repositório (tamanho). Para usá-lo:

1. Acesse o link do conjunto acima.
2. Na lista de recursos, baixe **"Operações não automáticas"** (CSV).
3. Salve o arquivo na pasta `dados/` (ela está no `.gitignore`).
4. Importe-o pela interface web (card **Importar arquivo**).

## Modelo de dados e tratamento

A entidade `Operacao` usa 12 dos 34 campos do arquivo, escolhidos por identificarem o beneficiário, localizar, datar e valorar a operação e por sustentarem os filtros e o gráfico:

| Atributo | Tipo Java | Coluna de origem | Obrigatório |
|---|---|---|---|
| id | Long | gerado (sequência) | — |
| cliente | String | cliente | Sim |
| cnpj | String | cnpj | Não |
| numeroContrato | String | numero_do_contrato | Sim |
| uf | String | uf | Sim (2 letras) |
| municipio | String | municipio | Não |
| dataContratacao | LocalDate | data_da_contratacao | Sim |
| valorContratado | BigDecimal | valor_contratado_reais | Sim (maior que zero) |
| valorDesembolsado | BigDecimal | valor_desembolsado_reais | Não (maior ou igual a zero) |
| produto | String | produto | Sim |
| setorBndes | String | setor_bndes | Sim |
| porteCliente | String | porte_do_cliente | Não |
| inovacao | String | inovacao | Não |

Tratamentos feitos pelo `ImportacaoService`:

- remoção de espaços desnecessários; o marcador `----------` e campos vazios viram nulo;
- valores monetários convertidos para `BigDecimal` (vírgula decimal); datas `dd/MM/aaaa` convertidas para `LocalDate`; UF em maiúsculas;
- validação dos campos obrigatórios; registros inválidos são **rejeitados e contabilizados com o motivo**, sem impedir os demais;
- linhas totalmente vazias são ignoradas;
- **duplicidade**: o número do contrato se repete no arquivo (subcréditos), então a chave é composta por *número do contrato + data + valor contratado + valor desembolsado*. Reenviar o mesmo arquivo não cria registros novos;
- detecção automática do separador (tabulação, `;` ou `,`) e da codificação (UTF-8, com alternativa Windows-1252);
- erros estruturais (arquivo vazio, extensão diferente de `.csv`, cabeçalho ausente, separador não reconhecido) impedem a importação e retornam 400 com mensagem clara. A importação é transacional.

Observação sobre os dados: a coluna UF contém o código `IE`, com município "SEM MUNICIPIO" ou "DIVERSOS", que indica operações de abrangência interestadual (interpretação do grupo). Esses registros são mantidos e aparecem no gráfico como "IE (interestadual)".

## Requisitos

- JDK 17 ou superior
- Maven (opcional: o projeto inclui o Maven Wrapper `mvnw`)
- Conta no [Neon](https://neon.tech) com um banco PostgreSQL criado
- Git

## Configuração

O projeto **não contém credenciais**. O banco é configurado por variáveis de ambiente, lidas no `application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
```

1. Copie o modelo: `cp .env.example .env` (no Windows, copie e renomeie o arquivo manualmente).
2. Preencha o `.env` com os dados do seu banco no Neon:
   ```
   DB_URL=jdbc:postgresql://SEU-HOST.neon.tech/SEU_BANCO?sslmode=require
   DB_USER=seu_usuario
   DB_PASSWORD=sua_senha
   ```
   - Troque o prefixo `postgresql://` da string do Neon por `jdbc:postgresql://`.
   - Remova `usuario:senha@` da URL (eles vão em `DB_USER` e `DB_PASSWORD`).
   - Se a string vier com `channel_binding=require`, remova esse parâmetro.
   - Sem aspas e sem espaços ao redor do `=`.
3. O `.env` está no `.gitignore` e **nunca deve ser enviado ao GitHub**. A tabela `operacao` é criada automaticamente na primeira execução (`ddl-auto=update`).

## Como executar

### Pelo VS Code

Crie `.vscode/launch.json` apontando para o `.env` e execute com **F5**:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "SaftbndesApplication",
      "request": "launch",
      "mainClass": "br.fatec.saftbndes.SaftbndesApplication",
      "envFile": "${workspaceFolder}/.env"
    }
  ]
}
```

### Pelo terminal

Defina as variáveis e rode o Maven Wrapper.

Windows (PowerShell):

```powershell
$env:DB_URL="jdbc:postgresql://SEU-HOST.neon.tech/SEU_BANCO?sslmode=require"
$env:DB_USER="seu_usuario"
$env:DB_PASSWORD="sua_senha"
.\mvnw.cmd spring-boot:run
```

Linux ou macOS:

```bash
export DB_URL="jdbc:postgresql://SEU-HOST.neon.tech/SEU_BANCO?sslmode=require"
export DB_USER="seu_usuario"
export DB_PASSWORD="sua_senha"
./mvnw spring-boot:run
```

Com a aplicação no ar (`Started SaftbndesApplication`), acesse **http://localhost:8080**. Se a porta 8080 estiver ocupada, finalize a execução anterior antes de iniciar outra.

## Como usar a interface

1. Abra http://localhost:8080 e, no card **Importar arquivo**, selecione o CSV (oficial ou amostra) e clique em **Importar**. O painel mostra linhas lidas, importadas, rejeitadas e duplicadas.
2. Use **+ Novo registro** para cadastrar, e **Editar** e **Excluir** na tabela.
3. Filtre por UF, setor e faixa de valor; navegue pelas páginas da tabela.
4. Use **Buscar por ID** para consultar um registro (ID inexistente retorna 404).
5. No gráfico, alterne entre **Valor contratado (R$)** e **Quantidade de operações**. O gráfico usa Chart.js por CDN, então precisa de internet.

## API REST

Prefixo: `/api/operacoes`

| Método | Endpoint | Finalidade | Status |
|---|---|---|---|
| GET | `/api/operacoes` | Listar todos os registros | 200 |
| GET | `/api/operacoes/{id}` | Buscar por ID | 200 ou 404 |
| POST | `/api/operacoes` | Cadastrar | 201 ou 400 |
| PUT | `/api/operacoes/{id}` | Atualizar | 200, 400 ou 404 |
| DELETE | `/api/operacoes/{id}` | Excluir | 204 ou 404 |
| POST | `/api/operacoes/importar` | Importar CSV (campo `arquivo`, multipart/form-data) | 200 ou 400 |
| GET | `/api/operacoes/filtro` | Filtrar por `uf`, `setor`, `valorMin`, `valorMax`; paginação opcional com `page` e `size` (total no cabeçalho `X-Total-Count`) | 200 |
| GET | `/api/operacoes/resumo-uf` | Quantidade e valor total por UF (dados do gráfico) | 200 |

### Exemplos

**POST `/api/operacoes`** (entrada):

```json
{
  "cliente": "EMPRESA TESTE LTDA",
  "cnpj": "12.345.678/0001-90",
  "numeroContrato": "999001",
  "uf": "sp",
  "municipio": "SAO PAULO",
  "dataContratacao": "2024-03-15",
  "valorContratado": 1500000.50,
  "valorDesembolsado": 750000.00,
  "produto": "BNDES FINEM",
  "setorBndes": "INDUSTRIA",
  "porteCliente": "MEDIA",
  "inovacao": "SIM"
}
```

**Resposta 201** (UF normalizada para maiúsculas e `id` gerado):

```json
{
  "id": 1,
  "cliente": "EMPRESA TESTE LTDA",
  "cnpj": "12.345.678/0001-90",
  "numeroContrato": "999001",
  "uf": "SP",
  "municipio": "SAO PAULO",
  "dataContratacao": "2024-03-15",
  "valorContratado": 1500000.50,
  "valorDesembolsado": 750000.00,
  "produto": "BNDES FINEM",
  "setorBndes": "INDUSTRIA",
  "porteCliente": "MEDIA",
  "inovacao": "SIM"
}
```

**Resposta 400** (validação):

```json
{
  "mensagem": "Dados inválidos. Corrija os campos indicados.",
  "erros": {
    "cliente": "O cliente é obrigatório",
    "valorContratado": "O valor contratado deve ser maior que zero"
  }
}
```

**GET `/api/operacoes/filtro?uf=SP&setor=industria&page=0&size=15`**: filtros opcionais e combináveis; o setor aceita busca parcial sem diferenciar maiúsculas de minúsculas.

**POST `/api/operacoes/importar`** (resposta 200):

```json
{
  "linhasLidas": 23896,
  "importadas": 22637,
  "rejeitadas": 0,
  "duplicadas": 1259,
  "mensagem": "Importação concluída: 23896 linhas lidas, 22637 importadas, 0 rejeitadas e 1259 duplicadas.",
  "motivosRejeicao": {},
  "exemplosRejeicao": []
}
```

**Resposta 400** (erro estrutural na importação):

```json
{ "mensagem": "Extensão incompatível: envie um arquivo .csv." }
```

## Testes

Os endpoints foram testados no Insomnia (cadastro, listagem, busca por ID, ID inexistente com 404, atualização, exclusão, filtros, validação com 400 e as importações válida, inválida, com registros rejeitados e reimportação do mesmo arquivo). As evidências estão no relatório técnico entregue à disciplina.

## Segurança

- Credenciais do banco apenas em variáveis de ambiente (`DB_URL`, `DB_USER`, `DB_PASSWORD`).
- `.env` no `.gitignore`; o repositório traz somente o `.env.example`, sem valores secretos.
- Validação dos dados antes da persistência e tratamento global de erros com mensagens compreensíveis.

## Licença dos dados

Os dados pertencem ao BNDES e são distribuídos sob a licença ODbL (Open Data Commons). Ao reutilizá-los, cite a fonte: BNDES, Portal de Dados Abertos.
