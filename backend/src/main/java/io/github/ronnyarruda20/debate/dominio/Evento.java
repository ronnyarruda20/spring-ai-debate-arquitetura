package io.github.ronnyarruda20.debate.dominio;

import io.github.ronnyarruda20.debate.dominio.Modelos.Adr;
import io.github.ronnyarruda20.debate.dominio.Modelos.Enquadramento;
import io.github.ronnyarruda20.debate.dominio.Modelos.Turno;

/** O que a tela recebe durante o debate. Cada tipo vira um evento SSE com o nome dado por {@link #nome()}. */
public sealed interface Evento {

    String nome();

    record Enquadrado(Enquadramento enquadramento) implements Evento {
        public String nome() {
            return "enquadramento";
        }
    }

    record Falou(Turno turno) implements Evento {
        public String nome() {
            return "turno";
        }
    }

    /** O debate parou antes das rodadas previstas porque um lado passou a se repetir. */
    record Eco(String lado, double similaridade) implements Evento {
        public String nome() {
            return "eco";
        }
    }

    record Decidido(Adr adr, String markdown) implements Evento {
        public String nome() {
            return "adr";
        }
    }

    record Fim(long duracaoMs) implements Evento {
        public String nome() {
            return "fim";
        }
    }

    record Erro(String mensagem) implements Evento {
        public String nome() {
            return "erro";
        }
    }
}
