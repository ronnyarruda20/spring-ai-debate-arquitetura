package io.github.ronnyarruda20.debate.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

import io.github.ronnyarruda20.debate.config.DebateConfig.Endpoint;
import io.github.ronnyarruda20.debate.suporte.ModeloPorPapel;

class DebateConfigTest {

    private static final Endpoint SEM_ENDERECO = new Endpoint(null, null);

    private static ObjectProvider<ChatModel> principal(ChatModel modelo) {
        var fabrica = new StaticListableBeanFactory();
        if (modelo != null) {
            fabrica.addBean("principal", modelo);
        }
        return fabrica.getBeanProvider(ChatModel.class);
    }

    @Test
    void papelComEnderecoGanhaClienteOpenAiProprio() {
        ChatModel principal = new ModeloPorPapel();
        var deepseek = new Endpoint("https://api.deepseek.com/v1", "chave-deepseek");

        ChatModel modelo = DebateConfig.modeloDoPapel(principal(principal), deepseek, "deepseek-flash", "lado A");

        assertThat(modelo).isInstanceOf(OpenAiChatModel.class).isNotSameAs(principal);
        var opcoes = (OpenAiChatOptions) modelo.getDefaultOptions();
        assertThat(opcoes.getBaseUrl()).isEqualTo("https://api.deepseek.com/v1");
        assertThat(opcoes.getModel()).isEqualTo("deepseek-flash");
    }

    @Test
    void papelSemEnderecoUsaOProvedorPrincipal() {
        ChatModel principal = new ModeloPorPapel();

        assertThat(DebateConfig.modeloDoPapel(principal(principal), SEM_ENDERECO, "x", "lado B")).isSameAs(principal);
    }

    @Test
    void semEnderecoESemProvedorPrincipalExplicaOQueFalta() {
        assertThatThrownBy(() -> DebateConfig.modeloDoPapel(principal(null), SEM_ENDERECO, "x", "árbitro"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("árbitro")
                .hasMessageContaining("CHAT_PROVIDER");
    }

    @Test
    void verificadorDispensaChaveDoPrincipalQuandoTodosOsPapeisTemEndereco() {
        var env = new MockEnvironment().withProperty("spring.ai.model.chat", "google-genai");
        for (String papel : new String[] { "arbitro", "a", "b" }) {
            env.setProperty("app.debate.endpoints." + papel + ".base-url", "https://exemplo/v1");
            env.setProperty("app.debate.endpoints." + papel + ".api-key", "chave");
        }
        assertThat(VerificadorDeChaves.faltando(env)).isEmpty();

        env.setProperty("app.debate.endpoints.b.api-key", "");
        assertThat(VerificadorDeChaves.faltando(env)).containsExactly("CHAVE_B");
    }

    @Test
    void verificadorAindaExigeChaveDoPrincipalSeAlgumPapelDependeDele() {
        var env = new MockEnvironment().withProperty("spring.ai.model.chat", "google-genai")
                .withProperty("app.debate.endpoints.a.base-url", "https://api.deepseek.com/v1")
                .withProperty("app.debate.endpoints.a.api-key", "chave");

        assertThat(VerificadorDeChaves.faltando(env)).containsExactly("GEMINI_API_KEY");
    }
}
