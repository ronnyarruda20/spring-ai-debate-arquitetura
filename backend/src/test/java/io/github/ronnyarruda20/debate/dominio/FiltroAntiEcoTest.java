package io.github.ronnyarruda20.debate.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FiltroAntiEcoTest {

    private final FiltroAntiEco filtro = new FiltroAntiEco(0.6);

    @Test
    void falaQuaseIgualEEco() {
        assertThat(filtro.ehEco(
                "O cursor escala melhor porque usa o índice e não escaneia linhas descartadas.",
                "O cursor escala melhor, pois usa o índice e não escaneia as linhas descartadas.")).isTrue();
    }

    @Test
    void argumentoNovoNaoEEco() {
        assertThat(filtro.ehEco(
                "O painel precisa pular para a página N, e o cursor não permite isso sem gambiarra.",
                "O cursor escala melhor porque usa o índice e não escaneia linhas descartadas.")).isFalse();
    }

    @Test
    void primeiraFalaNuncaEEco() {
        assertThat(filtro.ehEco("qualquer coisa", null)).isFalse();
    }

    @Test
    void ignoraAcentosEMaiusculas() {
        assertThat(FiltroAntiEco.similaridade("Índice ÚNICO", "indice unico")).isEqualTo(1.0);
    }
}
