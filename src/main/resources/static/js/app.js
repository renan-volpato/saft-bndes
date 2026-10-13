const API = '/api/operacoes';
const TAMANHO_PAGINA = 15;
const UFS = ['AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
             'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO'];
const CAMPOS_TEXTO = ['cliente', 'cnpj', 'numeroContrato', 'uf', 'municipio',
                      'produto', 'setorBndes', 'porteCliente', 'inovacao'];

let pagina = 0;
let totalFiltrado = 0;
let grafico = null;
let dadosResumo = [];
let temporizador;

const $ = (id) => document.getElementById(id);
const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/* ---------- utilitários ---------- */

function mostrarMensagem(texto, tipo = 'sucesso') {
  const el = $('mensagem');
  el.textContent = texto;
  el.className = `mensagem ${tipo}`;
  clearTimeout(temporizador);
  temporizador = setTimeout(() => el.classList.add('oculto'), 8000);
}

async function lerJson(res) {
  try { return await res.json(); } catch { return null; }
}

function formatarData(iso) {
  if (!iso) return '—';
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

function celula(texto, classe) {
  const td = document.createElement('td');
  td.textContent = (texto === null || texto === undefined || texto === '') ? '—' : texto;
  if (classe) td.className = classe;
  return td;
}

function botao(rotulo, classe, acao) {
  const b = document.createElement('button');
  b.type = 'button';
  b.textContent = rotulo;
  b.className = classe;
  b.addEventListener('click', acao);
  return b;
}

/* ---------- tabela, filtros e paginação ---------- */

function parametrosFiltro() {
  const p = new URLSearchParams();
  const uf = $('filtroUf').value;
  const setor = $('filtroSetor').value.trim();
  const min = $('filtroMin').value;
  const max = $('filtroMax').value;
  if (uf) p.set('uf', uf);
  if (setor) p.set('setor', setor);
  if (min) p.set('valorMin', min);
  if (max) p.set('valorMax', max);
  return p;
}

async function carregarTabela() {
  const p = parametrosFiltro();
  p.set('page', pagina);
  p.set('size', TAMANHO_PAGINA);
  try {
    const res = await fetch(`${API}/filtro?${p}`);
    if (!res.ok) throw new Error(`Falha ao consultar a API (status ${res.status}).`);
    totalFiltrado = Number(res.headers.get('X-Total-Count')) || 0;
    const lista = await res.json();
    if (lista.length === 0 && pagina > 0) { // última página ficou vazia (ex.: após excluir)
      pagina--;
      return carregarTabela();
    }
    renderTabela(lista);
    renderPaginacao();
  } catch (e) {
    mostrarMensagem(e.message, 'erro');
  }
}

function renderTabela(lista) {
  const corpo = $('corpoTabela');
  corpo.replaceChildren();

  if (lista.length === 0) {
    const tr = document.createElement('tr');
    const td = celula('Nenhum registro encontrado.', 'vazio');
    td.colSpan = 8;
    tr.appendChild(td);
    corpo.appendChild(tr);
    return;
  }

  lista.forEach((op) => {
    const tr = document.createElement('tr');
    const tdCliente = celula(op.cliente, 'cliente');
    tdCliente.title = op.cliente;
    const tdAcoes = document.createElement('td');
    tdAcoes.className = 'acoes';
    tdAcoes.append(
      botao('Editar', 'btn pequeno', () => editar(op.id)),
      botao('Excluir', 'btn pequeno perigo', () => excluir(op.id))
    );
    tr.append(
      celula(op.id),
      tdCliente,
      celula(op.uf),
      celula(formatarData(op.dataContratacao)),
      celula(moeda.format(op.valorContratado), 'num'),
      celula(op.setorBndes),
      celula(op.produto),
      tdAcoes
    );
    corpo.appendChild(tr);
  });
}

function renderPaginacao() {
  const totalPaginas = Math.max(1, Math.ceil(totalFiltrado / TAMANHO_PAGINA));
  const inicio = totalFiltrado === 0 ? 0 : pagina * TAMANHO_PAGINA + 1;
  const fim = Math.min((pagina + 1) * TAMANHO_PAGINA, totalFiltrado);
  $('paginaAtual').textContent = `Página ${pagina + 1} de ${totalPaginas}`;
  $('btnAnterior').disabled = pagina === 0;
  $('btnProxima').disabled = pagina + 1 >= totalPaginas;
  $('infoTabela').textContent =
    `${totalFiltrado.toLocaleString('pt-BR')} registro(s) encontrado(s) · exibindo ${inicio}–${fim}`;
}

async function carregarContadorGeral() {
  try {
    const res = await fetch(`${API}/filtro?page=0&size=1`);
    $('contadorGeral').textContent = (Number(res.headers.get('X-Total-Count')) || 0).toLocaleString('pt-BR');
  } catch {
    $('contadorGeral').textContent = '–';
  }
}

async function atualizarTudo() {
  await Promise.all([carregarTabela(), carregarContadorGeral(), carregarGrafico()]);
}

/* ---------- busca por ID ---------- */

async function buscarPorId() {
  const id = $('buscaId').value;
  const caixa = $('detalhe');
  if (!id) {
    mostrarMensagem('Informe um ID para buscar.', 'erro');
    return;
  }
  try {
    const res = await fetch(`${API}/${id}`);
    if (res.status === 404) {
      caixa.classList.add('oculto');
      mostrarMensagem(`Registro com ID ${id} não encontrado (404).`, 'erro');
      return;
    }
    if (!res.ok) throw new Error(`Erro ao buscar (status ${res.status}).`);
    const op = await res.json();
    const linhas = [
      ['ID', op.id], ['Cliente', op.cliente], ['CNPJ', op.cnpj],
      ['Contrato', op.numeroContrato], ['UF', op.uf], ['Município', op.municipio],
      ['Data', formatarData(op.dataContratacao)],
      ['Valor contratado', moeda.format(op.valorContratado)],
      ['Valor desembolsado', op.valorDesembolsado == null ? '—' : moeda.format(op.valorDesembolsado)],
      ['Produto', op.produto], ['Setor', op.setorBndes],
      ['Porte', op.porteCliente], ['Inovação', op.inovacao]
    ];
    caixa.replaceChildren();
    linhas.forEach(([rotulo, valor]) => {
      const p = document.createElement('p');
      const forte = document.createElement('strong');
      forte.textContent = `${rotulo}: `;
      p.append(forte, document.createTextNode(valor ?? '—'));
      caixa.appendChild(p);
    });
    caixa.classList.remove('oculto');
  } catch (e) {
    mostrarMensagem(e.message, 'erro');
  }
}

/* ---------- cadastro, edição e exclusão ---------- */

function abrirFormulario(op) {
  $('formOperacao').reset();
  $('errosForm').classList.add('oculto');
  $('campoId').value = op ? op.id : '';
  $('tituloForm').textContent = op ? `Editar registro #${op.id}` : 'Novo registro';
  if (op) {
    CAMPOS_TEXTO.forEach((c) => { $(c).value = op[c] ?? ''; });
    $('dataContratacao').value = op.dataContratacao ?? '';
    $('valorContratado').value = op.valorContratado ?? '';
    $('valorDesembolsado').value = op.valorDesembolsado ?? '';
  }
  $('dialogForm').showModal();
}

async function editar(id) {
  try {
    const res = await fetch(`${API}/${id}`);
    if (res.status === 404) {
      mostrarMensagem(`Registro com ID ${id} não encontrado (404).`, 'erro');
      return;
    }
    abrirFormulario(await res.json());
  } catch {
    mostrarMensagem('Não foi possível carregar o registro.', 'erro');
  }
}

function lerFormulario() {
  const texto = (id) => { const v = $(id).value.trim(); return v === '' ? null : v; };
  const numero = (id) => { const v = $(id).value; return v === '' ? null : Number(v); };
  return {
    cliente: texto('cliente'),
    cnpj: texto('cnpj'),
    numeroContrato: texto('numeroContrato'),
    uf: texto('uf'),
    municipio: texto('municipio'),
    dataContratacao: texto('dataContratacao'),
    valorContratado: numero('valorContratado'),
    valorDesembolsado: numero('valorDesembolsado'),
    produto: texto('produto'),
    setorBndes: texto('setorBndes'),
    porteCliente: texto('porteCliente'),
    inovacao: texto('inovacao')
  };
}

function mostrarErrosForm(corpo) {
  const caixa = $('errosForm');
  caixa.replaceChildren();
  const titulo = document.createElement('strong');
  titulo.textContent = corpo?.mensagem ?? 'Não foi possível salvar o registro.';
  caixa.appendChild(titulo);
  if (corpo?.erros) {
    const ul = document.createElement('ul');
    Object.values(corpo.erros).forEach((msg) => {
      const li = document.createElement('li');
      li.textContent = msg;
      ul.appendChild(li);
    });
    caixa.appendChild(ul);
  }
  caixa.classList.remove('oculto');
}

async function salvar(evento) {
  evento.preventDefault();
  const id = $('campoId').value;
  try {
    const res = await fetch(id ? `${API}/${id}` : API, {
      method: id ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(lerFormulario())
    });
    const corpo = await lerJson(res);
    if (res.ok) {
      $('dialogForm').close();
      mostrarMensagem(id ? 'Registro atualizado com sucesso.' : 'Registro cadastrado com sucesso.');
      await atualizarTudo();
    } else if (res.status === 404) {
      $('dialogForm').close();
      mostrarMensagem('Registro não encontrado (404). Ele pode ter sido excluído.', 'erro');
      await atualizarTudo();
    } else {
      mostrarErrosForm(corpo);
    }
  } catch {
    mostrarErrosForm({ mensagem: 'Não foi possível conectar à API.' });
  }
}

async function excluir(id) {
  if (!confirm(`Excluir o registro #${id}?`)) return;
  try {
    const res = await fetch(`${API}/${id}`, { method: 'DELETE' });
    if (res.status === 204) {
      mostrarMensagem(`Registro #${id} excluído com sucesso.`);
      await atualizarTudo();
    } else if (res.status === 404) {
      mostrarMensagem(`Registro #${id} não encontrado (404).`, 'erro');
    } else {
      mostrarMensagem(`Erro ao excluir (status ${res.status}).`, 'erro');
    }
  } catch {
    mostrarMensagem('Não foi possível conectar à API.', 'erro');
  }
}

/* ---------- importação ---------- */

function mostrarResultadoImportacao(r) {
  const caixa = $('resultadoImportacao');
  caixa.replaceChildren();
  const ul = document.createElement('ul');
  [
    `Linhas lidas: ${r.linhasLidas.toLocaleString('pt-BR')}`,
    `Importadas: ${r.importadas.toLocaleString('pt-BR')}`,
    `Rejeitadas: ${r.rejeitadas.toLocaleString('pt-BR')}`,
    `Duplicadas: ${r.duplicadas.toLocaleString('pt-BR')}`
  ].forEach((t) => {
    const li = document.createElement('li');
    li.textContent = t;
    ul.appendChild(li);
  });
  caixa.appendChild(ul);

  const motivos = Object.entries(r.motivosRejeicao ?? {});
  if (motivos.length > 0) {
    const titulo = document.createElement('strong');
    titulo.textContent = 'Motivos de rejeição:';
    const lista = document.createElement('ul');
    motivos.forEach(([motivo, qtd]) => {
      const li = document.createElement('li');
      li.textContent = `${motivo}: ${qtd}`;
      lista.appendChild(li);
    });
    caixa.append(titulo, lista);
  }
  caixa.classList.remove('oculto');
}

async function importar() {
  const arquivo = $('arquivo').files[0];
  if (!arquivo) {
    mostrarMensagem('Selecione um arquivo antes de importar.', 'erro');
    return;
  }
  const dados = new FormData();
  dados.append('arquivo', arquivo); // mesmo nome do @RequestParam do Controller

  const botaoImportar = $('btnImportar');
  botaoImportar.disabled = true;
  botaoImportar.textContent = 'Importando...';
  try {
    const res = await fetch(`${API}/importar`, { method: 'POST', body: dados });
    const corpo = await lerJson(res);
    if (res.ok) {
      mostrarMensagem(corpo.mensagem);
      mostrarResultadoImportacao(corpo);
      pagina = 0;
      await atualizarTudo();
    } else {
      $('resultadoImportacao').classList.add('oculto');
      mostrarMensagem(corpo?.mensagem ?? `Erro na importação (status ${res.status}).`, 'erro');
    }
  } catch {
    mostrarMensagem('Não foi possível enviar o arquivo.', 'erro');
  } finally {
    botaoImportar.disabled = false;
    botaoImportar.textContent = 'Importar';
  }
}

/* ---------- gráfico ---------- */

async function carregarGrafico() {
  try {
    const res = await fetch(`${API}/resumo-uf`);
    if (!res.ok) throw new Error();
    dadosResumo = await res.json();
    desenharGrafico();
  } catch {
    mostrarMensagem('Não foi possível carregar os dados do gráfico.', 'erro');
  }
}

function desenharGrafico() {
  if (typeof Chart === 'undefined') {
    mostrarMensagem('Biblioteca do gráfico indisponível (verifique a conexão com a internet).', 'erro');
    return;
  }
  const metrica = $('metricaGrafico').value;
  const ehValor = metrica === 'valorTotal';
  const top = [...dadosResumo]
    .sort((a, b) => Number(b[metrica]) - Number(a[metrica]))
    .slice(0, 10);

  if (grafico) grafico.destroy();
  grafico = new Chart($('grafico'), {
    type: 'bar',
    data: {
      labels: top.map((d) => d.uf),
      datasets: [{
        data: top.map((d) => ehValor ? Number(d.valorTotal) / 1e9 : Number(d.quantidade)),
        backgroundColor: '#2e9e6b'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => ehValor
              ? moeda.format(top[ctx.dataIndex].valorTotal)
              : `${Number(top[ctx.dataIndex].quantidade).toLocaleString('pt-BR')} operações`
          }
        }
      },
      scales: {
        x: { title: { display: true, text: 'UF' } },
        y: {
          beginAtZero: true,
          title: { display: true, text: ehValor ? 'Valor contratado (R$ bilhões)' : 'Quantidade de operações' }
        }
      }
    }
  });
}

/* ---------- inicialização ---------- */

document.addEventListener('DOMContentLoaded', () => {
  UFS.forEach((uf) => {
    const opcao = document.createElement('option');
    opcao.value = uf;
    opcao.textContent = uf;
    $('filtroUf').appendChild(opcao);
  });

  $('formFiltros').addEventListener('submit', (e) => {
    e.preventDefault();
    pagina = 0;
    carregarTabela();
  });
  $('btnLimpar').addEventListener('click', () => {
    $('formFiltros').reset();
    pagina = 0;
    carregarTabela();
  });
  $('btnAnterior').addEventListener('click', () => { pagina--; carregarTabela(); });
  $('btnProxima').addEventListener('click', () => { pagina++; carregarTabela(); });

  $('btnNovo').addEventListener('click', () => abrirFormulario(null));
  $('btnCancelar').addEventListener('click', () => $('dialogForm').close());
  $('formOperacao').addEventListener('submit', salvar);

  $('btnImportar').addEventListener('click', importar);
  $('btnBuscarId').addEventListener('click', buscarPorId);
  $('buscaId').addEventListener('keydown', (e) => { if (e.key === 'Enter') buscarPorId(); });
  $('metricaGrafico').addEventListener('change', desenharGrafico);

  atualizarTudo();
});