package leilao.regulador;

import leilao.Licitacao;
import leilao.licitador.Licitador;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Leilao {
    private static int NUM = 0;
    private int id;
    private Licitacao maiorLicitacao;
    private Licitador autor;
    private Set<Licitador> licitadores;
    private String objeto;
    private Date date;

    public Leilao(Licitador autor, String objeto, Date date) {
        this.autor = autor;
        this.objeto = objeto;
        this.date = date;
        this.maiorLicitacao = null;
        this.licitadores = new HashSet<Licitador>();
        NUM ++;
        this.id = NUM;
    }

    public boolean licitar(Licitador licitador, Licitacao licitacao) {
        if (maiorLicitacao != null && licitador.getPlafond() >= licitacao.getQuantia() && licitacao.getQuantia() > maiorLicitacao.getQuantia()) {
            licitadores.add(licitador);
            maiorLicitacao = licitacao;
            return true;
        }
        return false;
    }

    public boolean hasFinished() {
        if (date.equals(new Date())) {
            return true;
        }
        return false;
    }
}
