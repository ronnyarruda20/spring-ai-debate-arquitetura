# Exemplo real: paginação offset × cursor

Debate gerado pela aplicação em 28/09/2026, com Gemini 3.8 Flash (lado A), Gemini 3.5 Flash (lado B) e Gemini 3.1 Pro (árbitro). Nada foi editado.

**Pergunta:** Paginação na API REST de pedidos: offset/limit ou cursor (keyset)?

**Contexto:** Tabela de pedidos com 40 milhões de linhas, crescendo 200 mil por dia. Consumida por um app mobile com rolagem infinita e por um painel web administrativo que precisa pular para a página N e mostrar o total. Time de 4 devs Java/Spring, Postgres 16.

## Debate

### Rodada 1 · lado A: Paginação tradicional baseada em Offset e Limit (gemini-3.8-flash)

Para atender simultaneamente o painel administrativo e o app, o modelo Offset/Limit é a escolha pragmática. O Admin exige pular para a página N e ver totais; implementar isso com cursor exigiria gambiarras complexas que nosso time de 4 devs não tem tempo de manter. No Spring Data com Postgres 16, `Pageable` entrega isso nativamente em minutos.

Reconheço o problema: `OFFSET` elevado em 40 milhões de linhas degrada o Postgres ao escanear tuplas descartadas, além do risco de itens duplicados no mobile com 200 mil inserções/dia. 

A mitigação é simples e padrão de mercado: limitamos a navegação a um teto (ex: máximo de 100 páginas), forçando filtros por data ou status para buscas antigas — ninguém audita a página 5.000 sem filtro. Para o total, usamos contagem aproximada via estatísticas do Postgres ou cache. Resolvemos o negócio sem complexidade desnecessária.

### Rodada 1 · lado B: Paginação baseada em Cursor (Keyset Pagination) (gemini-3.5-flash)

Limitar a 100 páginas é um paliativo que não resolve a experiência do app mobile. Com 200 mil novos pedidos/dia, a rolagem infinita via offset gerará duplicidade constante de itens na tela enquanto novos pedidos entram. Isso gera uma péssima experiência e suporte aberto.

O cursor resolve isso com performance $O(1)$. No Postgres 16, a query `WHERE id < :ultimo_id ORDER BY id DESC LIMIT 20` bate direto no índice e responde em sub-milissegundos, seja na primeira ou na página de número 1 milhão.

Admito que pular para a página N no painel é inviável com cursor puro. Mitigamos isso usando uma abordagem híbrida: o Spring Data 3+ já suporta `ScrollPosition` nativamente para o cursor do app. Para o painel Admin, forçamos filtros obrigatórios (como intervalo de datas de 30 dias), reduzindo a massa de dados a ponto de tornar o offset seguro e performático apenas nessa tela específica.

### Rodada 2 · lado A: Paginação tradicional baseada em Offset e Limit (gemini-3.8-flash)

Sugerir uma arquitetura híbrida apenas comprova a insuficiência do cursor para o negócio e impõe sobrecarga desnecessária a um time de 4 devs: manteremos dois contratos de API, duas estratégias de cache e o dobro de testes.

O risco de duplicação no mobile é superestimado. Os 200 mil pedidos diários diluem-se na base; o app mobile consulta pedidos do *usuário logado* (`WHERE user_id = ?`), cujo volume individual é minúsculo, tornando o drift por novas inserções praticamente nulo. Caso ocorra em feeds globais, um simples `Set` de IDs no cliente resolve.

Admito que offsets profundos pesam ao carregar muitas colunas. Mitigamos isso no Postgres 16 com *Deferred Joins*: paginamos apenas as chaves pelo índice (`SELECT id FROM pedidos ORDER BY created_at DESC LIMIT 20 OFFSET 1000`) e fazemos `JOIN` tardio para buscar o payload. Mantemos uma API única, padronizada e performática.

### Rodada 2 · lado B: Paginação baseada em Cursor (Keyset Pagination) (gemini-3.5-flash)

Confiar em `Set` no mobile para mascarar inconsistências delega responsabilidade do backend para o cliente. Além disso, *Deferred Joins* não eliminam o escaneamento do índice: um `OFFSET 1000000` em uma tabela de 40 milhões de linhas ainda causará alto consumo de CPU e I/O no Postgres 16 sob concorrência.

A suposta complexidade de manter o modelo híbrido é mitigada pelo Spring Data 3. Usando `Window` e `ScrollPosition`, implementamos chaves baseadas em cursor de forma quase idêntica ao `Pageable` tradicional, sem duplicar código.

Assim, blindamos o banco com consultas $O(1)$ na API mobile — que sofre o maior volume de acessos concorrentes — e usamos o offset apenas no Admin, onde o volume é controlado por filtros temporais obrigatórios. É a única forma de garantir que o app não quebre quando a base duplicar de tamanho.

## ADR escrito pelo árbitro

> # ADR: Estratégia de Paginação na API REST de Pedidos
>
> - Status: proposto
> - Data: 2026-09-28
> - Confiança: alta
>
> ## Contexto
>
> A base de pedidos possui 40 milhões de linhas e cresce em 200 mil registros diários. Há dois consumidores principais: um app mobile que utiliza rolagem infinita e um painel web administrativo que necessita pular para páginas específicas e visualizar totais. O desafio é escolher uma estratégia de paginação que forneça alta performance e consistência para o mobile, sem inviabilizar os requisitos de navegação do painel, considerando uma equipe enxuta de 4 desenvolvedores utilizando Java, Spring Data e PostgreSQL 16.
>
> ## Opções consideradas
>
> ### Paginação tradicional baseada em Offset e Limit
>
> - A favor: Suporte nativo e amplamente conhecido no Spring Data via interface Pageable.
> - A favor: Permite pular para a página N e obter o total de registros facilmente, atendendo os requisitos do painel administrativo.
> - A favor: Mantém a simplicidade de um único contrato de API para todos os clientes.
> - Contra: Degradação severa de performance em páginas profundas (alto custo de CPU e I/O no banco ao escanear e descartar tuplas).
> - Contra: Risco de duplicação ou omissão de registros na rolagem infinita do mobile devido a novas inserções concorrentes.
>
> ### Paginação baseada em Cursor (Keyset Pagination)
>
> - A favor: Performance O(1) altamente escalável, utilizando diretamente o índice do banco sem escanear tuplas descartadas.
> - A favor: Garante consistência absoluta dos dados exibidos na rolagem infinita, imune a novas inserções na tabela.
> - A favor: Implementação facilitada na stack atual através do suporte nativo a Window e ScrollPosition no Spring Data 3+.
> - Contra: Incapacidade de pular para páginas arbitrárias ou calcular o número total de páginas sem consultas auxiliares custosas.
> - Contra: Exige a manutenção de uma abordagem híbrida ou a imposição de filtros restritivos no painel administrativo.
>
> ## Decisão
>
> **Paginação baseada em Cursor (Keyset Pagination).** A paginação baseada em cursor é a única capaz de garantir performance constante O(1) e evitar anomalias de dados (duplicidades) na rolagem infinita do app mobile, que representa o maior volume de acessos e sofre com a alta taxa de inserções diárias. O argumento de que offsets profundos podem ser mitigados com Deferred Joins não anula o custo de I/O e CPU sob alta concorrência. A suposta complexidade de manter uma arquitetura híbrida para atender o painel web (usando offset com filtros obrigatórios) é superada pelo suporte nativo ao 'ScrollPosition' no Spring Data 3+, permitindo que a equipe de 4 desenvolvedores implemente a solução sem grande sobrecarga de código, protegendo o banco de dados principal do crescimento acelerado.
>
> ## Consequências
>
> - Positiva: Performance constante O(1) nas consultas do app mobile, garantindo escalabilidade para a tabela de 40 milhões de linhas.
> - Positiva: Eliminação completa de inconsistências (duplicação ou omissão de registros) durante a rolagem infinita no app.
> - Positiva: Redução de consumo de CPU e I/O no PostgreSQL 16 ao evitar varreduras de índices longos geradas por offsets profundos.
> - Negativa: Necessidade de manter e testar dois contratos e estratégias de paginação no backend (Cursor e Offset).
> - Negativa: Usuários do painel administrativo terão a navegação restrita por filtros obrigatórios (como intervalos de datas) para viabilizar o uso do offset.
>
> ## Quando revisitar
>
> - Se as regras de negócio do painel administrativo mudarem, exigindo navegação irrestrita em toda a base histórica sem a possibilidade de usar filtros temporais.
> - Se a equipe relatar alta complexidade técnica e bugs frequentes na manutenção do modelo híbrido de paginação.
> - Se o PostgreSQL introduzir otimizações nativas revolucionárias para a cláusula OFFSET em grandes volumes de dados.
>
> ---
>
> *Rascunho gerado por um debate entre agentes de IA. Revise antes de aceitar.*
