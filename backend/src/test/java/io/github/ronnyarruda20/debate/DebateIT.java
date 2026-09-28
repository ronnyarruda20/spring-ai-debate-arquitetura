package io.github.ronnyarruda20.debate;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import io.github.ronnyarruda20.debate.suporte.ModeloPorPapel;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DebateIT.Config.class)
@ActiveProfiles("test")
class DebateIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {
        @Bean
        ModeloPorPapel chatModel() {
            return new ModeloPorPapel();
        }
    }

    @LocalServerPort
    int porta;

    @Autowired
    ModeloPorPapel modelo;

    RestClient http;

    @BeforeEach
    void preparar() {
        http = RestClient.builder().baseUrl("http://localhost:" + porta).defaultStatusHandler(s -> true, (q, r) -> { }).build();
        modelo.limpar();
    }

    @Test
    void debatePorSseComOsModelosConfiguradosPorPapel() {
        var nomes = eventos(Map.of("pergunta", "Offset ou cursor?", "contexto", "40 milhões de linhas", "rodadas", 1));

        assertThat(nomes).containsExactly("enquadramento", "turno", "turno", "adr", "fim");
        assertThat(modelo.prompts).extracting(p -> p.getOptions().getModel())
                .containsExactly("arbitro-teste", "a-teste", "b-teste", "arbitro-teste");
    }

    @Test
    void rodadasPedidasAcimaDoTetoSaoLimitadas() {
        eventos(Map.of("pergunta", "Offset ou cursor?", "rodadas", 5));

        assertThat(modelo.advogados()).hasSize(2 * 3);
    }

    @Test
    void perguntaVaziaDa400() {
        var r = http.post().uri("/api/debates").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("pergunta", " ")).retrieve().toBodilessEntity();

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void falhaDoModeloViraEventoDeErro() {
        modelo.falhar = true;

        assertThat(eventos(Map.of("pergunta", "Offset ou cursor?"))).containsExactly("erro");
    }

    @Test
    void infoMostraOsModelosDeCadaPapel() {
        var info = http.get().uri("/api/info").retrieve().body(Map.class);

        assertThat(info).containsEntry("modeloArbitro", "arbitro-teste").containsEntry("modeloA", "a-teste")
                .containsEntry("rodadasMaximas", 3);
    }

    private List<String> eventos(Map<String, Object> corpo) {
        byte[] bytes = http.post().uri("/api/debates").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM).body(corpo).retrieve().body(byte[].class);
        List<String> nomes = new ArrayList<>();
        for (String linha : new String(bytes, StandardCharsets.UTF_8).split("\\r?\\n")) {
            if (linha.startsWith("event:")) {
                nomes.add(linha.substring(6).strip());
            }
        }
        return nomes;
    }
}
