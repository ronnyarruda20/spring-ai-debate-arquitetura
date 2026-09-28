package io.github.ronnyarruda20.debate.dominio;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detecta quando um agente passa a repetir o que já disse: similaridade de Jaccard entre
 * os conjuntos de palavras de duas falas.
 *
 * <p>Ideia herdada do meu projeto anterior de GPT conversando com GPT, em que o filtro
 * encerrava a conversa quando os dois entravam em eco. Aqui ele compara cada fala com a
 * fala anterior do mesmo lado: repetir-se é sinal de que o argumento acabou, e o debate
 * vai direto para o árbitro, sem gastar mais chamadas.
 */
public class FiltroAntiEco {

    private final double limite;

    public FiltroAntiEco(double limite) {
        this.limite = limite;
    }

    public boolean ehEco(String atual, String anterior) {
        return anterior != null && similaridade(atual, anterior) >= limite;
    }

    static double similaridade(String a, String b) {
        Set<String> pa = palavras(a);
        Set<String> pb = palavras(b);
        if (pa.isEmpty() && pb.isEmpty()) {
            return 1.0;
        }
        Set<String> intersecao = new HashSet<>(pa);
        intersecao.retainAll(pb);
        Set<String> uniao = new HashSet<>(pa);
        uniao.addAll(pb);
        return (double) intersecao.size() / Math.max(1, uniao.size());
    }

    private static Set<String> palavras(String s) {
        if (s == null) {
            return Set.of();
        }
        String limpo = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(limpo.split("[^a-z0-9]+")).filter(p -> p.length() > 2).collect(Collectors.toSet());
    }
}
