import { TestBed } from '@angular/core/testing';

import { Api } from './api';
import { App } from './app';
import { EventoDebate, PedidoDebate } from './modelos';

describe('App', () => {
  let pedidos: PedidoDebate[];

  async function* roteiro(): AsyncGenerator<EventoDebate> {
    yield {
      tipo: 'enquadramento',
      enquadramento: { titulo: 'Paginação', opcaoA: 'Offset', opcaoB: 'Cursor', criterios: ['Desempenho', 'Pular página'] },
    };
    yield { tipo: 'turno', turno: { lado: 'A', opcao: 'Offset', modelo: 'modelo-a', rodada: 1, texto: 'O **Pageable** resolve.' } };
    yield { tipo: 'turno', turno: { lado: 'B', opcao: 'Cursor', modelo: 'modelo-b', rodada: 1, texto: 'Use `WHERE id < :x`.' } };
    yield {
      tipo: 'adr',
      markdown: '# ADR',
      adr: {
        titulo: 'ADR: paginação por cursor',
        contexto: 'Tabela grande.',
        opcoes: [
          { nome: 'Offset', pros: ['Simples'], contras: ['Lento'] },
          { nome: 'Cursor', pros: ['Rápido'], contras: ['Sem pular página'] },
        ],
        decisao: 'Cursor',
        justificativa: 'Desempenho constante.',
        consequenciasPositivas: ['Rolagem estável'],
        consequenciasNegativas: ['Painel com filtros'],
        quandoRevisitar: ['Se o painel exigir pular páginas'],
        confianca: 'ALTA',
      },
    };
    yield { tipo: 'fim', duracaoMs: 42000 };
  }

  beforeEach(async () => {
    pedidos = [];
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        {
          provide: Api,
          useValue: {
            info: async () => ({
              provedor: 'google-genai', modeloArbitro: 'arbitro', modeloA: 'modelo-a', modeloB: 'modelo-b', rodadasMaximas: 3,
            }),
            debater: (p: PedidoDebate) => {
              pedidos.push(p);
              return roteiro();
            },
          },
        },
      ],
    }).compileComponents();
  });

  it('exemplo preenche o formulário e o debate mostra turnos, formatação e o ADR', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await new Promise((r) => setTimeout(r));
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('header .modelo')?.textContent).toBe('modelo-a × modelo-b');
    (el.querySelectorAll('.exemplos button')[0] as HTMLButtonElement).click();
    await fixture.componentInstance.debater();
    await fixture.whenStable();

    expect(pedidos[0].pergunta).toContain('offset/limit ou cursor');
    expect(pedidos[0].rodadas).toBe(2);
    const turnos = el.querySelectorAll('.turno');
    expect(turnos.length).toBe(2);
    expect(turnos[1].classList).toContain('b');
    expect(turnos[0].querySelector('p strong')?.textContent).toBe('Pageable');
    expect(turnos[1].querySelector('code')?.textContent).toBe('WHERE id < :x');
    expect(el.querySelector('.adr .decisao')?.textContent).toContain('Decisão: Cursor.');
    expect(el.querySelector('.opcao.escolhida strong')?.textContent).toBe('Cursor');
    expect(el.querySelector('.tempo')?.textContent).toContain('42 s');
  });
});
