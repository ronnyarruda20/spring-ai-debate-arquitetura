package io.github.ronnyarruda20.debate.dominio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;

import io.github.ronnyarruda20.debate.dominio.Modelos.Adr;
import io.github.ronnyarruda20.debate.dominio.Modelos.Enquadramento;
import io.github.ronnyarruda20.debate.dominio.Modelos.Pedido;
import io.github.ronnyarruda20.debate.dominio.Modelos.Turno;

/**
 * Orquestra o debate em três fases:
 * <ol>
 *   <li>o árbitro enquadra a decisão em duas opções e alguns critérios;</li>
 *   <li>dois advogados se alternam, cada um defendendo uma opção e respondendo ao outro;</li>
 *   <li>o árbitro lê o debate e escreve o ADR.</li>
 * </ol>
 * Cada papel pode usar um modelo diferente. O árbitro usa o mais forte; os advogados,
 * modelos distintos entre si, para não soarem como a mesma voz discutindo consigo mesma.
 */
public class Debate {

    /** Um participante: o cliente de chat já configurado com o modelo dele, e o nome desse modelo. */
    public record Participante(ChatClient chat, String modelo) {
    }

    static final String ENQUADRAR = """
            Você é o árbitro de um debate técnico entre dois engenheiros de software sênior.
            Recebe uma decisão de arquitetura e o contexto. Identifique as duas alternativas mais fortes \
            em disputa (se a pergunta citar mais, escolha as duas mais relevantes para o contexto) e de 3 a 5 \
            critérios que devem pesar na decisão. Não decida nada ainda.
            """;

    static final String ADVOGADO = """
            Você é um engenheiro de software sênior num debate técnico. Defenda a opção "%s" contra \
            "%s" na decisão: %s
            Contexto: %s
            Critérios em jogo: %s

            Regras:
            - Responda diretamente ao argumento mais recente do outro lado; não repita o que você já disse.
            - Use exemplos concretos e números quando fizer sentido.
            - Admita as fraquezas reais da sua opção e explique como mitigá-las. Não mude de lado.
            - No máximo 150 palavras, em português do Brasil, sem títulos.
            """;

    static final String DECIDIR = """
            Você é o árbitro de um debate técnico. Leia o debate e escreva um ADR (Architecture Decision Record).
            Decida com base nos argumentos e no contexto dado, não na popularidade das opções. Se o contexto \
            não bastar para uma decisão segura, decida mesmo assim e marque a confiança como BAIXA, dizendo \
            no campo quandoRevisitar que informação mudaria a decisão. Escreva em português do Brasil.
            """;

    private final Participante arbitro;
    private final Participante defensorA;
    private final Participante defensorB;
    private final FiltroAntiEco antiEco;
    private final Clock clock;

    public Debate(Participante arbitro, Participante defensorA, Participante defensorB, FiltroAntiEco antiEco, Clock clock) {
        this.arbitro = arbitro;
        this.defensorA = defensorA;
        this.defensorB = defensorB;
        this.antiEco = antiEco;
        this.clock = clock;
    }

    public void executar(Pedido pedido, Consumer<Evento> eventos) {
        long inicio = clock.millis();
        String contexto = pedido.contexto() == null || pedido.contexto().isBlank() ? "não informado" : pedido.contexto().strip();

        Enquadramento e = arbitro.chat().prompt()
                .system(ENQUADRAR)
                .user("Decisão: " + pedido.pergunta() + "\nContexto: " + contexto)
                .call().entity(Enquadramento.class);
        eventos.accept(new Evento.Enquadrado(e));

        List<Turno> turnos = new ArrayList<>();
        debate:
        for (int rodada = 1; rodada <= pedido.rodadas(); rodada++) {
            for (String lado : List.of("A", "B")) {
                Participante p = "A".equals(lado) ? defensorA : defensorB;
                String minha = "A".equals(lado) ? e.opcaoA() : e.opcaoB();
                String outra = "A".equals(lado) ? e.opcaoB() : e.opcaoA();
                String texto = p.chat().prompt()
                        .system(ADVOGADO.formatted(minha, outra, pedido.pergunta(), contexto, String.join("; ", e.criterios())))
                        .user(transcricao(turnos) + "\n\nSua vez de falar, defendendo " + minha + ".")
                        .call().content();
                texto = texto == null ? "" : texto.strip();

                String anterior = ultimaFala(turnos, lado);
                if (antiEco.ehEco(texto, anterior)) {
                    eventos.accept(new Evento.Eco(lado, FiltroAntiEco.similaridade(texto, anterior)));
                    break debate;
                }
                Turno t = new Turno(lado, minha, p.modelo(), rodada, texto);
                turnos.add(t);
                eventos.accept(new Evento.Falou(t));
            }
        }

        Adr adr = arbitro.chat().prompt()
                .system(DECIDIR)
                .user("Decisão: " + pedido.pergunta() + "\nContexto: " + contexto + "\nOpções: " + e.opcaoA() + " x "
                        + e.opcaoB() + "\nCritérios: " + String.join("; ", e.criterios()) + "\n\n" + transcricao(turnos))
                .call().entity(Adr.class);
        eventos.accept(new Evento.Decidido(adr, AdrMarkdown.gerar(adr, LocalDate.now(clock))));
        eventos.accept(new Evento.Fim(clock.millis() - inicio));
    }

    static String transcricao(List<Turno> turnos) {
        if (turnos.isEmpty()) {
            return "O debate ainda não começou; você abre.";
        }
        return "Debate até agora:\n\n" + turnos.stream()
                .map(t -> "[Rodada " + t.rodada() + ", defesa de " + t.opcao() + "]\n" + t.texto())
                .collect(Collectors.joining("\n\n"));
    }

    private static String ultimaFala(List<Turno> turnos, String lado) {
        for (int i = turnos.size() - 1; i >= 0; i--) {
            if (turnos.get(i).lado().equals(lado)) {
                return turnos.get(i).texto();
            }
        }
        return null;
    }
}
