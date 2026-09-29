package io.github.ronnyarruda20.debate.config;

import java.time.Clock;
import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import io.github.ronnyarruda20.debate.dominio.Debate;
import io.github.ronnyarruda20.debate.dominio.FiltroAntiEco;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DebateConfig.DebateProperties.class)
public class DebateConfig {

    /**
     * @param modeloArbitro   enquadra e escreve o ADR; o mais forte dos três
     * @param modeloA         defende a primeira opção
     * @param modeloB         defende a segunda; de preferência diferente de A
     * @param rodadasMaximas  teto de rodadas por debate, para limitar custo
     * @param limiteEco       similaridade a partir da qual uma fala conta como repetição
     * @param endpoints       endereço próprio por papel, para misturar provedores no mesmo debate
     */
    @ConfigurationProperties("app.debate")
    public record DebateProperties(String modeloArbitro, String modeloA, String modeloB,
            @DefaultValue("3") int rodadasMaximas, @DefaultValue("0.6") double limiteEco,
            @DefaultValue Endpoints endpoints) {
    }

    public record Endpoints(@DefaultValue Endpoint arbitro, @DefaultValue Endpoint a, @DefaultValue Endpoint b) {
    }

    /** API compatível com a da OpenAI. Sem {@code baseUrl}, o papel usa o provedor principal. */
    public record Endpoint(String baseUrl, String apiKey) {

        public boolean definido() {
            return StringUtils.hasText(baseUrl);
        }
    }

    @Bean
    Debate debate(ObjectProvider<ChatModel> principal, DebateProperties props) {
        Endpoints e = props.endpoints();
        return new Debate(participante(principal, e.arbitro(), props.modeloArbitro(), "árbitro"),
                participante(principal, e.a(), props.modeloA(), "lado A"),
                participante(principal, e.b(), props.modeloB(), "lado B"),
                new FiltroAntiEco(props.limiteEco()), Clock.systemDefaultZone());
    }

    static Debate.Participante participante(ObjectProvider<ChatModel> principal, Endpoint endpoint,
            String nomeDoModelo, String papel) {
        return participante(modeloDoPapel(principal, endpoint, nomeDoModelo, papel), nomeDoModelo);
    }

    /**
     * Com endereço próprio, o papel ganha um cliente OpenAI só dele (DeepSeek, Gemini, OpenRouter, Ollama...).
     * Sem, usa o modelo do provedor principal, escolhido por {@code CHAT_PROVIDER}.
     */
    static ChatModel modeloDoPapel(ObjectProvider<ChatModel> principal, Endpoint endpoint, String nomeDoModelo,
            String papel) {
        if (endpoint.definido()) {
            return OpenAiChatModel.builder()
                    .options(OpenAiChatOptions.builder()
                            .baseUrl(endpoint.baseUrl())
                            .apiKey(endpoint.apiKey())
                            .model(nomeDoModelo)
                            // O ADR do árbitro pode passar de um minuto em modelos de raciocínio.
                            .timeout(Duration.ofMinutes(3))
                            .build())
                    .build();
        }
        ChatModel modelo = principal.getIfAvailable();
        if (modelo == null) {
            throw new IllegalStateException("O " + papel + " não tem endereço próprio e não há provedor principal: "
                    + "defina CHAT_PROVIDER ou o endereço desse papel.");
        }
        return modelo;
    }

    /**
     * Um cliente de chat por papel. As opções partem das do provedor (Spring AI 2 exige o tipo
     * específico) e só trocam o modelo.
     */
    static Debate.Participante participante(ChatModel modelo, String nomeDoModelo) {
        ChatOptions.Builder<?> opcoes = modelo.getDefaultOptions().mutate().model(nomeDoModelo);
        return new Debate.Participante(ChatClient.builder(modelo).defaultOptions(opcoes).build(), nomeDoModelo);
    }
}
