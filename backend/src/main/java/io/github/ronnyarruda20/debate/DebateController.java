package io.github.ronnyarruda20.debate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.ronnyarruda20.debate.config.DebateConfig.DebateProperties;
import io.github.ronnyarruda20.debate.dominio.Debate;
import io.github.ronnyarruda20.debate.dominio.Evento;
import io.github.ronnyarruda20.debate.dominio.Modelos.Pedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * Debate em Server-Sent Events: {@code enquadramento}, vários {@code turno}, talvez {@code eco},
 * {@code adr} e {@code fim}; ou {@code erro}.
 */
@RestController
public class DebateController {

    public record PedidoDebate(
            @NotBlank @Size(max = 500) String pergunta,
            @Size(max = 3000) String contexto,
            @Min(1) @Max(5) Integer rodadas) {
    }

    public record Info(String provedor, String modeloArbitro, String modeloA, String modeloB, int rodadasMaximas) {
    }

    private static final Logger log = LoggerFactory.getLogger(DebateController.class);

    private final Debate debate;
    private final DebateProperties props;
    private final String provedor;

    public DebateController(Debate debate, DebateProperties props, org.springframework.core.env.Environment env) {
        this.debate = debate;
        this.props = props;
        this.provedor = env.getProperty("spring.ai.model.chat", "google-genai");
    }

    @GetMapping("/api/info")
    public Info info() {
        return new Info(provedor, props.modeloArbitro(), props.modeloA(), props.modeloB(), props.rodadasMaximas());
    }

    @PostMapping(value = "/api/debates", produces = "text/event-stream;charset=UTF-8")
    public Flux<ServerSentEvent<Evento>> debater(@Valid @RequestBody PedidoDebate corpo) {
        int rodadas = Math.min(corpo.rodadas() == null ? 2 : corpo.rodadas(), props.rodadasMaximas());
        var pedido = new Pedido(corpo.pergunta().strip(), corpo.contexto(), rodadas);

        Sinks.Many<Evento> sink = Sinks.many().unicast().onBackpressureBuffer();
        Schedulers.boundedElastic().schedule(() -> {
            try {
                debate.executar(pedido, sink::tryEmitNext);
            } catch (RuntimeException e) {
                log.error("Falha no debate: {}", pedido.pergunta(), e);
                sink.tryEmitNext(new Evento.Erro("O debate foi interrompido: um dos modelos não respondeu. Tente de novo."));
            } finally {
                sink.tryEmitComplete();
            }
        });
        return sink.asFlux().map(e -> ServerSentEvent.builder(e).event(e.nome()).build());
    }
}
