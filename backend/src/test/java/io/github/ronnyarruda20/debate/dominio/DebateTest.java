package io.github.ronnyarruda20.debate.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;

import io.github.ronnyarruda20.debate.dominio.Modelos.Pedido;
import io.github.ronnyarruda20.debate.suporte.ModeloPorPapel;

class DebateTest {

    ModeloPorPapel modelo;
    Debate debate;

    @BeforeEach
    void preparar() {
        modelo = new ModeloPorPapel();
        debate = new Debate(participante("arbitro-forte"), participante("modelo-a"), participante("modelo-b"),
                new FiltroAntiEco(0.6), Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC));
    }

    Debate.Participante participante(String nome) {
        return new Debate.Participante(ChatClient.builder(modelo).defaultOptions(ChatOptions.builder().model(nome)).build(),
                nome);
    }

    List<Evento> rodar(int rodadas) {
        List<Evento> eventos = new ArrayList<>();
        debate.executar(new Pedido("Offset ou cursor?", "40 milhões de linhas", rodadas), eventos::add);
        return eventos;
    }

    @Test
    void enquadraDebateEmTurnosAlternadosEDecide() {
        var eventos = rodar(2);

        assertThat(eventos).extracting(Evento::nome)
                .containsExactly("enquadramento", "turno", "turno", "turno", "turno", "adr", "fim");
        var turnos = eventos.stream().filter(Evento.Falou.class::isInstance).map(e -> ((Evento.Falou) e).turno()).toList();
        assertThat(turnos).extracting(Modelos.Turno::lado).containsExactly("A", "B", "A", "B");
        assertThat(turnos).extracting(Modelos.Turno::opcao).containsExactly("Offset", "Cursor", "Offset", "Cursor");
        assertThat(turnos).extracting(Modelos.Turno::rodada).containsExactly(1, 1, 2, 2);
    }

    @Test
    void cadaPapelUsaOProprioModelo() {
        rodar(1);

        assertThat(modelo.prompts).extracting(p -> p.getOptions().getModel())
                .containsExactly("arbitro-forte", "modelo-a", "modelo-b", "arbitro-forte");
    }

    @Test
    void cadaAdvogadoRecebeOQueOOutroDisse() {
        rodar(1);

        String paraB = modelo.advogados().get(1).getInstructions().getLast().getText();
        assertThat(paraB).contains("[Rodada 1, defesa de Offset]").contains("Defendo Offset");
        assertThat(modelo.advogados().get(0).getInstructions().getLast().getText()).contains("você abre");
    }

    @Test
    void arbitroRecebeODebateInteiroEOAdrViraMarkdown() {
        var eventos = rodar(1);

        String paraArbitro = modelo.prompts.getLast().getInstructions().getLast().getText();
        assertThat(paraArbitro).contains("Defendo Offset").contains("Defendo Cursor").contains("Opções: Offset x Cursor");
        var decidido = (Evento.Decidido) eventos.stream().filter(Evento.Decidido.class::isInstance).findFirst().orElseThrow();
        assertThat(decidido.adr().decisao()).isEqualTo("Cursor");
        assertThat(decidido.markdown()).startsWith("# ADR: paginação por cursor").contains("- Data: 2026-09-28");
    }

    @Test
    void ladoQueSeRepeteEncerraODebateEOArbitroDecideMesmoAssim() {
        // O lado B diz sempre a mesma coisa: na segunda rodada, o anti-eco para o debate.
        var original = modelo.falaDe;
        modelo.falaDe = opcao -> opcao.equals("Cursor") ? "Defendo Cursor porque escala bem e é simples" : original.apply(opcao);

        var eventos = rodar(3);

        assertThat(eventos).extracting(Evento::nome)
                .containsExactly("enquadramento", "turno", "turno", "turno", "eco", "adr", "fim");
    }

    @Test
    void numeroDeChamadasCresceComAsRodadas() {
        rodar(3);

        assertThat(modelo.prompts).hasSize(2 + 3 * 2);
    }

    @Test
    void markdownDoAdrTrazSecoesDoModeloMadr() {
        var adr = new Modelos.Adr("ADR: X", "Contexto.", List.of(new Modelos.Opcao("A", List.of("rápido"), List.of("caro"))),
                "A", "Porque sim.", List.of("bom"), List.of("ruim"), List.of("se mudar"), "BAIXA");

        String md = AdrMarkdown.gerar(adr, LocalDate.of(2026, 9, 28));

        assertThat(md).contains("## Contexto", "## Opções consideradas", "### A", "- A favor: rápido", "- Contra: caro",
                "## Decisão", "**A.** Porque sim.", "## Consequências", "## Quando revisitar",
                "Confiança: baixa (o contexto não bastou");
    }
}
