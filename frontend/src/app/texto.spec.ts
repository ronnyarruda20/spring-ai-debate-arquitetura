import { proximoLado, trechos } from './texto';

describe('texto das falas', () => {
  it('separa negrito e código do texto normal', () => {
    expect(trechos('Use **cursor** com `WHERE id < :ultimo`.')).toEqual([
      { texto: 'Use ', estilo: 'normal' },
      { texto: 'cursor', estilo: 'negrito' },
      { texto: ' com ', estilo: 'normal' },
      { texto: 'WHERE id < :ultimo', estilo: 'codigo' },
      { texto: '.', estilo: 'normal' },
    ]);
  });

  it('texto sem marcação fica inteiro', () => {
    expect(trechos('Sem marcação.')).toEqual([{ texto: 'Sem marcação.', estilo: 'normal' }]);
  });

  it('A abre e B responde', () => {
    expect(proximoLado([])).toBe('A');
    expect(proximoLado([{ lado: 'A' }])).toBe('B');
    expect(proximoLado([{ lado: 'A' }, { lado: 'B' }])).toBe('A');
  });
});
