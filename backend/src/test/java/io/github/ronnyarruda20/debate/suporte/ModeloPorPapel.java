package io.github.ronnyarruda20.debate.suporte;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Modelo falso que responde conforme o papel, reconhecido pelas instruções de sistema:
 * árbitro enquadrando, advogado de cada lado ou árbitro decidindo. Guarda os prompts.
 */
public class ModeloPorPapel implements ChatModel {

    public static final String ENQUADRAMENTO = """
            {"titulo":"Paginação da API de pedidos","opcaoA":"Offset","opcaoB":"Cursor",
             "criterios":["Desempenho em páginas profundas","Pular para a página N","Consistência na rolagem"]}""";

    public static final String ADR = """
            {"titulo":"ADR: paginação por cursor","contexto":"Tabela grande e rolagem infinita.",
             "opcoes":[{"nome":"Offset","pros":["Simples"],"contras":["Lento no fundo"]},
                       {"nome":"Cursor","pros":["Constante"],"contras":["Sem pular página"]}],
             "decisao":"Cursor","justificativa":"Desempenho constante.",
             "consequenciasPositivas":["Rolagem estável"],"consequenciasNegativas":["Painel precisa de filtros"],
             "quandoRevisitar":["Se o painel exigir pular páginas"],"confianca":"ALTA"}""";

    public final List<Prompt> prompts = new CopyOnWriteArrayList<>();
    /** Argumentos bem diferentes entre si, para o filtro anti-eco não encerrar o debate. */
    static final List<String> ARGUMENTOS = List.of(
            "usa o índice direto e responde em milissegundos mesmo com milhões de linhas",
            "o painel administrativo precisa pular para qualquer página e mostrar o total",
            "novos pedidos entrando durante a rolagem causam itens duplicados na tela",
            "o time pequeno ganha com uma única estratégia bem testada e documentada",
            "o custo de CPU no Postgres sob concorrência cai drasticamente",
            "clientes antigos continuam funcionando sem mudança de contrato");
    private final java.util.concurrent.atomic.AtomicInteger falas = new java.util.concurrent.atomic.AtomicInteger();
    public volatile Function<String, String> falaDe = opcao ->
            "Defendo " + opcao + ": " + ARGUMENTOS.get(falas.getAndIncrement() % ARGUMENTOS.size()) + ".";
    public volatile boolean falhar;

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        if (falhar) {
            throw new IllegalStateException("provedor fora do ar");
        }
        String sistema = prompt.getInstructions().getFirst().getText();
        String texto;
        if (sistema.contains("Não decida nada ainda")) {
            texto = ENQUADRAMENTO;
        } else if (sistema.contains("escreva um ADR")) {
            texto = ADR;
        } else {
            String opcao = sistema.contains("Defenda a opção \"Offset\"") ? "Offset" : "Cursor";
            texto = falaDe.apply(opcao) + " (fala " + prompts.size() + ")";
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(texto))));
    }

    public List<Prompt> advogados() {
        return prompts.stream().filter(p -> p.getInstructions().getFirst().getText().contains("Defenda a opção")).toList();
    }

    public void limpar() {
        prompts.clear();
        falhar = false;
        falas.set(0);
        falaDe = opcao -> "Defendo " + opcao + ": " + ARGUMENTOS.get(falas.getAndIncrement() % ARGUMENTOS.size()) + ".";
    }
}
