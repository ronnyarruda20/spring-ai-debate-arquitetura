/**
 * Negrito (**x**) e código (`x`) nas falas dos agentes, sem gerar HTML:
 * o template monta os elementos, então nada do texto do modelo vira HTML.
 */
export interface Trecho {
  texto: string;
  estilo: 'normal' | 'negrito' | 'codigo';
}

const PADRAO = /(\*\*[^*]+\*\*|`[^`]+`)/g;

export function trechos(texto: string): Trecho[] {
  return texto
    .split(PADRAO)
    .filter((p) => p !== '')
    .map((p) => {
      if (p.startsWith('**') && p.endsWith('**') && p.length > 4) {
        return { texto: p.slice(2, -2), estilo: 'negrito' as const };
      }
      if (p.startsWith('`') && p.endsWith('`') && p.length > 2) {
        return { texto: p.slice(1, -1), estilo: 'codigo' as const };
      }
      return { texto: p, estilo: 'normal' as const };
    });
}

/** Quem fala a seguir: A abre cada rodada, B responde. */
export function proximoLado(turnos: { lado: 'A' | 'B' }[]): 'A' | 'B' {
  return turnos.at(-1)?.lado === 'A' ? 'B' : 'A';
}
