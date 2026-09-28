import { NgTemplateOutlet } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';

import { Api } from './api';
import { Adr, Enquadramento, Info, Turno } from './modelos';
import { proximoLado, trechos } from './texto';

export const EXEMPLOS = [
  {
    rotulo: 'Paginação: offset ou cursor',
    pergunta: 'Paginação na API REST de pedidos: offset/limit ou cursor (keyset)?',
    contexto:
      'Tabela de pedidos com 40 milhões de linhas, crescendo 200 mil por dia. Consumida por um app mobile com rolagem infinita e por um painel web administrativo que precisa pular para a página N e mostrar o total. Time de 4 devs Java/Spring, Postgres 16.',
  },
  {
    rotulo: 'Monolito modular ou microsserviços',
    pergunta: 'Novo sistema de agendamento: monolito modular ou microsserviços?',
    contexto:
      'Startup com 5 desenvolvedores, produto ainda validando mercado, previsão de 3 mil clínicas em 2 anos. Picos de uso às segundas de manhã. Deploy em nuvem, sem time de plataforma dedicado.',
  },
  {
    rotulo: 'Postgres JSONB ou MongoDB',
    pergunta: 'Catálogo de produtos com atributos variáveis por categoria: Postgres com JSONB ou MongoDB?',
    contexto:
      'E-commerce com 200 mil produtos em 300 categorias, cada uma com atributos próprios. Pedidos e estoque já estão no Postgres. Buscas por filtros de atributos são frequentes. Time domina SQL.',
  },
];

@Component({
  selector: 'app-root',
  imports: [NgTemplateOutlet],
  template: `
    <header>
      <h1>Debate de arquitetura <span>dois agentes discutem, um árbitro escreve o ADR</span></h1>
      @if (info(); as i) {
        <span class="modelo" [title]="'Árbitro: ' + i.modeloArbitro">{{ i.modeloA }} × {{ i.modeloB }}</span>
      }
    </header>

    <main>
      <form class="pedido" (submit)="$event.preventDefault(); debater()">
        <label>
          <span>Decisão a tomar</span>
          <input
            type="text"
            maxlength="500"
            placeholder="Ex.: Fila de mensagens: RabbitMQ ou Kafka?"
            [value]="pergunta()"
            (input)="pergunta.set($any($event.target).value)"
          />
        </label>
        <label>
          <span>Contexto: volume, time, restrições. Quanto mais concreto, melhor a decisão.</span>
          <textarea rows="3" maxlength="3000" [value]="contexto()" (input)="contexto.set($any($event.target).value)"></textarea>
        </label>
        <div class="linha">
          <label class="rodadas">
            <span>Rodadas</span>
            <select [value]="rodadas()" (change)="rodadas.set(+$any($event.target).value)">
              @for (n of opcoesRodadas(); track n) {
                <option [value]="n">{{ n }}</option>
              }
            </select>
          </label>
          <div class="exemplos">
            @for (e of exemplos; track e.rotulo) {
              <button type="button" (click)="usarExemplo(e)" [disabled]="rodando()">{{ e.rotulo }}</button>
            }
          </div>
          @if (rodando()) {
            <button type="button" class="secundario" (click)="parar()">Parar</button>
          } @else {
            <button type="submit" [disabled]="!pergunta().trim()">Começar debate</button>
          }
        </div>
      </form>

      @if (erro(); as e) {
        <p class="erro" role="alert">{{ e }}</p>
      }

      @if (enquadramento(); as q) {
        <section class="enquadramento">
          <h2>{{ q.titulo }}</h2>
          <div class="lados">
            <span class="lado a">A · {{ q.opcaoA }}</span>
            <span class="vs">×</span>
            <span class="lado b">B · {{ q.opcaoB }}</span>
          </div>
          <p class="criterios"><strong>Critérios:</strong> {{ q.criterios.join(' · ') }}</p>
        </section>
      }

      <ng-template #fala let-texto>
        @for (t of trechos(texto); track $index) {
          @switch (t.estilo) {
            @case ('negrito') {
              <strong>{{ t.texto }}</strong>
            }
            @case ('codigo') {
              <code>{{ t.texto }}</code>
            }
            @default {
              <span>{{ t.texto }}</span>
            }
          }
        }
      </ng-template>

      @if (turnos().length || (rodando() && enquadramento())) {
        <section class="debate" aria-live="polite">
          @for (t of turnos(); track $index) {
            <article class="turno" [class.b]="t.lado === 'B'">
              <header>
                <strong>{{ t.lado }} · {{ t.opcao }}</strong>
                <span>rodada {{ t.rodada }} · {{ t.modelo }}</span>
              </header>
              <p><ng-container *ngTemplateOutlet="fala; context: { $implicit: t.texto }" /></p>
            </article>
          }
          @if (eco(); as e) {
            <p class="aviso">O lado {{ e }} começou a se repetir, então o debate foi encerrado e seguiu para o árbitro.</p>
          }
          @if (rodando()) {
            <p class="aguardando" [class.b]="aguardando() === 'B'">
              <span class="pontos" aria-hidden="true"><i></i><i></i><i></i></span>
              {{ aguardando() === 'arbitro' ? 'O árbitro está escrevendo o ADR' : 'O lado ' + aguardando() + ' está argumentando' }}
            </p>
          }
        </section>
      }

      @if (adr(); as a) {
        <section class="adr">
          <div class="adr-topo">
            <h2>{{ a.titulo }}</h2>
            <span class="confianca" [attr.data-nivel]="a.confianca">confiança {{ nivel(a.confianca) }}</span>
          </div>
          <p class="decisao"><strong>Decisão: {{ a.decisao }}.</strong> {{ a.justificativa }}</p>
          <h3>Contexto</h3>
          <p>{{ a.contexto }}</p>
          <h3>Opções consideradas</h3>
          <div class="opcoes">
            @for (o of a.opcoes; track o.nome) {
              <div class="opcao" [class.escolhida]="o.nome === a.decisao">
                <strong>{{ o.nome }}</strong>
                <ul>
                  @for (p of o.pros; track $index) {
                    <li class="pro">{{ p }}</li>
                  }
                  @for (c of o.contras; track $index) {
                    <li class="contra">{{ c }}</li>
                  }
                </ul>
              </div>
            }
          </div>
          <h3>Consequências</h3>
          <ul>
            @for (c of a.consequenciasPositivas; track $index) {
              <li class="pro">{{ c }}</li>
            }
            @for (c of a.consequenciasNegativas; track $index) {
              <li class="contra">{{ c }}</li>
            }
          </ul>
          <h3>Quando revisitar</h3>
          <ul>
            @for (q of a.quandoRevisitar; track $index) {
              <li>{{ q }}</li>
            }
          </ul>
          <div class="acoes">
            <button type="button" (click)="baixar()">Baixar ADR (.md)</button>
            <button type="button" class="secundario" (click)="copiar()">{{ copiado() ? 'Copiado' : 'Copiar Markdown' }}</button>
            @if (duracao(); as d) {
              <span class="tempo">Debate e decisão em {{ (d / 1000).toFixed(0) }} s</span>
            }
          </div>
        </section>
      }
    </main>
  `,
})
export class App implements OnInit {
  private readonly api = inject(Api);

  protected readonly exemplos = EXEMPLOS;
  protected readonly trechos = trechos;
  protected readonly info = signal<Info | null>(null);
  protected readonly pergunta = signal('');
  protected readonly contexto = signal('');
  protected readonly rodadas = signal(2);
  protected readonly rodando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly enquadramento = signal<Enquadramento | null>(null);
  protected readonly turnos = signal<Turno[]>([]);
  protected readonly eco = signal<string | null>(null);
  protected readonly adr = signal<Adr | null>(null);
  protected readonly markdown = signal('');
  protected readonly duracao = signal<number | null>(null);
  protected readonly copiado = signal(false);

  protected readonly opcoesRodadas = computed(() =>
    Array.from({ length: this.info()?.rodadasMaximas ?? 3 }, (_, i) => i + 1),
  );
  protected readonly aguardando = computed(() => {
    const total = this.turnos().length;
    return this.eco() || total >= this.rodadas() * 2 ? 'arbitro' : proximoLado(this.turnos());
  });

  private abortar?: AbortController;

  ngOnInit(): void {
    this.api
      .info()
      .then((i) => this.info.set(i))
      .catch(() => this.info.set(null));
  }

  protected usarExemplo(e: (typeof EXEMPLOS)[number]): void {
    this.pergunta.set(e.pergunta);
    this.contexto.set(e.contexto);
  }

  async debater(): Promise<void> {
    if (!this.pergunta().trim() || this.rodando()) {
      return;
    }
    this.enquadramento.set(null);
    this.turnos.set([]);
    this.eco.set(null);
    this.adr.set(null);
    this.erro.set(null);
    this.duracao.set(null);
    this.rodando.set(true);
    this.abortar = new AbortController();
    try {
      const pedido = { pergunta: this.pergunta(), contexto: this.contexto(), rodadas: this.rodadas() };
      for await (const e of this.api.debater(pedido, this.abortar.signal)) {
        switch (e.tipo) {
          case 'enquadramento':
            this.enquadramento.set(e.enquadramento);
            break;
          case 'turno':
            this.turnos.update((t) => [...t, e.turno]);
            break;
          case 'eco':
            this.eco.set(e.lado);
            break;
          case 'adr':
            this.adr.set(e.adr);
            this.markdown.set(e.markdown);
            break;
          case 'fim':
            this.duracao.set(e.duracaoMs);
            break;
          case 'erro':
            this.erro.set(e.mensagem);
            break;
        }
      }
    } catch (e) {
      if ((e as Error).name !== 'AbortError') {
        this.erro.set((e as Error).message);
      }
    } finally {
      this.rodando.set(false);
    }
  }

  protected nivel(c: string): string {
    return ({ ALTA: 'alta', MEDIA: 'média', BAIXA: 'baixa' } as Record<string, string>)[c?.toUpperCase()] ?? c;
  }

  protected parar(): void {
    this.abortar?.abort();
  }

  protected baixar(): void {
    const url = URL.createObjectURL(new Blob([this.markdown()], { type: 'text/markdown' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = 'adr.md';
    a.click();
    URL.revokeObjectURL(url);
  }

  protected async copiar(): Promise<void> {
    await navigator.clipboard.writeText(this.markdown());
    this.copiado.set(true);
    setTimeout(() => this.copiado.set(false), 2000);
  }
}
