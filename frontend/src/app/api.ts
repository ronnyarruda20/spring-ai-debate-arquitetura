import { Injectable } from '@angular/core';

import { EventoDebate, Info, PedidoDebate } from './modelos';
import { LeitorSse } from './sse';

/** Cliente da API. O debate chega em streaming por POST, lido com fetch. */
@Injectable({ providedIn: 'root' })
export class Api {
  async info(): Promise<Info> {
    return (await fetch('/api/info')).json();
  }

  async *debater(pedido: PedidoDebate, sinal?: AbortSignal): AsyncGenerator<EventoDebate> {
    const r = await fetch('/api/debates', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
      body: JSON.stringify(pedido),
      signal: sinal,
    });
    if (!r.ok || !r.body) {
      let mensagem = `O servidor respondeu com erro ${r.status}.`;
      try {
        mensagem = (await r.json()).detail ?? mensagem;
      } catch {
        // corpo não era JSON
      }
      throw new Error(mensagem);
    }
    const leitor = new LeitorSse();
    const decodificador = new TextDecoder('utf-8');
    const corpo = r.body.getReader();
    while (true) {
      const { done, value } = await corpo.read();
      if (done) {
        return;
      }
      for (const e of leitor.receber(decodificador.decode(value, { stream: true }))) {
        const d = JSON.parse(e.dados);
        switch (e.evento) {
          case 'enquadramento':
            yield { tipo: 'enquadramento', enquadramento: d.enquadramento };
            break;
          case 'turno':
            yield { tipo: 'turno', turno: d.turno };
            break;
          case 'eco':
            yield { tipo: 'eco', lado: d.lado, similaridade: d.similaridade };
            break;
          case 'adr':
            yield { tipo: 'adr', adr: d.adr, markdown: d.markdown };
            break;
          case 'fim':
            yield { tipo: 'fim', duracaoMs: d.duracaoMs };
            break;
          case 'erro':
            yield { tipo: 'erro', mensagem: d.mensagem };
            break;
        }
      }
    }
  }
}
