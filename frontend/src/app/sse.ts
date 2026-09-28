/**
 * Leitor incremental de Server-Sent Events.
 *
 * O EventSource do navegador só faz GET, e o chat precisa de POST com corpo JSON,
 * então a resposta é lida com fetch e os eventos são montados aqui, pedaço a pedaço.
 * Um evento pode chegar partido entre dois pedaços da rede; o buffer cuida disso.
 */
export interface EventoSse {
  evento: string;
  dados: string;
}

export class LeitorSse {
  private buffer = '';

  receber(pedaco: string): EventoSse[] {
    this.buffer += pedaco.replace(/\r\n?/g, '\n');
    const eventos: EventoSse[] = [];
    let fim: number;
    while ((fim = this.buffer.indexOf('\n\n')) >= 0) {
      const bloco = this.buffer.slice(0, fim);
      this.buffer = this.buffer.slice(fim + 2);
      const evento = LeitorSse.interpretar(bloco);
      if (evento) {
        eventos.push(evento);
      }
    }
    return eventos;
  }

  private static interpretar(bloco: string): EventoSse | null {
    let evento = 'message';
    const dados: string[] = [];
    for (const linha of bloco.split('\n')) {
      if (linha.startsWith('event:')) {
        evento = linha.slice(6).trim();
      } else if (linha.startsWith('data:')) {
        dados.push(linha.slice(5).replace(/^ /, ''));
      }
    }
    return dados.length ? { evento, dados: dados.join('\n') } : null;
  }
}
