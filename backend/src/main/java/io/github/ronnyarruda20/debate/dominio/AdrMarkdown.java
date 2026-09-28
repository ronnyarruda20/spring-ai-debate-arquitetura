package io.github.ronnyarruda20.debate.dominio;

import java.time.LocalDate;
import java.util.List;

import io.github.ronnyarruda20.debate.dominio.Modelos.Adr;
import io.github.ronnyarruda20.debate.dominio.Modelos.Opcao;

/** Transforma o ADR em Markdown, pronto para ir para a pasta docs/adr de um repositório. */
public final class AdrMarkdown {

    private AdrMarkdown() {
    }

    public static String gerar(Adr adr, LocalDate data) {
        var sb = new StringBuilder();
        sb.append("# ").append(adr.titulo()).append("\n\n");
        sb.append("- Status: proposto\n");
        sb.append("- Data: ").append(data).append('\n');
        sb.append("- Confiança: ").append(confianca(adr.confianca())).append("\n\n");
        sb.append("## Contexto\n\n").append(adr.contexto()).append("\n\n");
        sb.append("## Opções consideradas\n\n");
        for (Opcao o : lista(adr.opcoes())) {
            sb.append("### ").append(o.nome()).append("\n\n");
            lista(o.pros()).forEach(p -> sb.append("- A favor: ").append(p).append('\n'));
            lista(o.contras()).forEach(c -> sb.append("- Contra: ").append(c).append('\n'));
            sb.append('\n');
        }
        sb.append("## Decisão\n\n**").append(adr.decisao()).append(".** ").append(adr.justificativa()).append("\n\n");
        sb.append("## Consequências\n\n");
        lista(adr.consequenciasPositivas()).forEach(c -> sb.append("- Positiva: ").append(c).append('\n'));
        lista(adr.consequenciasNegativas()).forEach(c -> sb.append("- Negativa: ").append(c).append('\n'));
        sb.append("\n## Quando revisitar\n\n");
        lista(adr.quandoRevisitar()).forEach(q -> sb.append("- ").append(q).append('\n'));
        sb.append("\n---\n\n*Rascunho gerado por um debate entre agentes de IA. Revise antes de aceitar.*\n");
        return sb.toString();
    }

    private static String confianca(String c) {
        return switch (c == null ? "" : c.toUpperCase()) {
            case "ALTA" -> "alta";
            case "BAIXA" -> "baixa (o contexto não bastou para decidir com segurança)";
            default -> "média";
        };
    }

    private static <T> List<T> lista(List<T> l) {
        return l == null ? List.of() : l;
    }
}
