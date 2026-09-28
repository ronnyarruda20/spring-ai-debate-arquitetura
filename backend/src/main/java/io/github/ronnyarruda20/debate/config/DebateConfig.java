package io.github.ronnyarruda20.debate.config;

import java.time.Clock;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
     */
    @ConfigurationProperties("app.debate")
    public record DebateProperties(String modeloArbitro, String modeloA, String modeloB,
            @DefaultValue("3") int rodadasMaximas, @DefaultValue("0.6") double limiteEco) {
    }

    @Bean
    Debate debate(ChatModel modelo, DebateProperties props) {
        return new Debate(participante(modelo, props.modeloArbitro()), participante(modelo, props.modeloA()),
                participante(modelo, props.modeloB()), new FiltroAntiEco(props.limiteEco()), Clock.systemDefaultZone());
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
