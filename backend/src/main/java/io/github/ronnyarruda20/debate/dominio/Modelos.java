package io.github.ronnyarruda20.debate.dominio;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Tipos do debate. Os que o árbitro devolve têm descrições que viram parte do esquema JSON. */
public final class Modelos {

    private Modelos() {
    }

    public record Pedido(String pergunta, String contexto, int rodadas) {
    }

    /** Como o árbitro enquadra a decisão antes do debate. */
    public record Enquadramento(
            @JsonPropertyDescription("Título curto da decisão, como no título de um ADR") String titulo,
            @JsonPropertyDescription("A primeira alternativa, em poucas palavras") String opcaoA,
            @JsonPropertyDescription("A segunda alternativa, em poucas palavras") String opcaoB,
            @JsonPropertyDescription("De 3 a 5 critérios que pesam nesta decisão, dado o contexto") List<String> criterios) {
    }

    public record Turno(String lado, String opcao, String modelo, int rodada, String texto) {
    }

    /** Architecture Decision Record, no espírito do modelo MADR. */
    public record Adr(
            String titulo,
            @JsonPropertyDescription("O problema e as forças em jogo, em um parágrafo") String contexto,
            List<Opcao> opcoes,
            @JsonPropertyDescription("A opção escolhida, com o nome igual ao de uma das opções") String decisao,
            @JsonPropertyDescription("Por que essa opção, citando os argumentos mais fortes do debate") String justificativa,
            List<String> consequenciasPositivas,
            List<String> consequenciasNegativas,
            @JsonPropertyDescription("Sinais concretos de que a decisão deve ser revista") List<String> quandoRevisitar,
            @JsonPropertyDescription("BAIXA, MEDIA ou ALTA, conforme o contexto dado bastar ou não para decidir") String confianca) {
    }

    public record Opcao(String nome, List<String> pros, List<String> contras) {
    }
}
