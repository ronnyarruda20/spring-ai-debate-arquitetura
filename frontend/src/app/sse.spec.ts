import { LeitorSse } from './sse';

describe('LeitorSse', () => {
  it('monta eventos completos', () => {
    const leitor = new LeitorSse();

    const eventos = leitor.receber('event:token\ndata:{"t":"Olá"}\n\nevent:fim\ndata:{"conversaId":"c1"}\n\n');

    expect(eventos).toEqual([
      { evento: 'token', dados: '{"t":"Olá"}' },
      { evento: 'fim', dados: '{"conversaId":"c1"}' },
    ]);
  });

  it('guarda o pedaço incompleto até o evento terminar', () => {
    const leitor = new LeitorSse();

    expect(leitor.receber('event:tok')).toEqual([]);
    expect(leitor.receber('en\ndata:{"t":"a"')).toEqual([]);
    expect(leitor.receber('}\n\n')).toEqual([{ evento: 'token', dados: '{"t":"a"}' }]);
  });

  it('aceita CRLF e junta várias linhas de data', () => {
    const leitor = new LeitorSse();

    const eventos = leitor.receber('event: fontes\r\ndata: [1,\r\ndata: 2]\r\n\r\n');

    expect(eventos).toEqual([{ evento: 'fontes', dados: '[1,\n2]' }]);
  });
});
