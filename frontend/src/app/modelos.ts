/** Tipos que espelham a API do backend. */

export interface Enquadramento {
  titulo: string;
  opcaoA: string;
  opcaoB: string;
  criterios: string[];
}

export interface Turno {
  lado: 'A' | 'B';
  opcao: string;
  modelo: string;
  rodada: number;
  texto: string;
}

export interface Opcao {
  nome: string;
  pros: string[];
  contras: string[];
}

export interface Adr {
  titulo: string;
  contexto: string;
  opcoes: Opcao[];
  decisao: string;
  justificativa: string;
  consequenciasPositivas: string[];
  consequenciasNegativas: string[];
  quandoRevisitar: string[];
  confianca: string;
}

export interface Info {
  provedor: string;
  modeloArbitro: string;
  modeloA: string;
  modeloB: string;
  rodadasMaximas: number;
}

export type EventoDebate =
  | { tipo: 'enquadramento'; enquadramento: Enquadramento }
  | { tipo: 'turno'; turno: Turno }
  | { tipo: 'eco'; lado: string; similaridade: number }
  | { tipo: 'adr'; adr: Adr; markdown: string }
  | { tipo: 'fim'; duracaoMs: number }
  | { tipo: 'erro'; mensagem: string };

export interface PedidoDebate {
  pergunta: string;
  contexto: string;
  rodadas: number;
}
