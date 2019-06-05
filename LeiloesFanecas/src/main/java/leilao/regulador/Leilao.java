package leilao.regulador;

import leilao.Licitacao;
import leilao.licitador.Licitador;

import java.io.Serializable;
import java.util.*;

public class Leilao implements Serializable {
    public static int NUM = 0;
    private int id;
    private Licitador autor;
    private String objeto;
    private Calendar dataFecho;
    private double valorInicial;
    private List<Licitacao> licitacoes;

    public Leilao(Licitador autor, String objeto, double valorInicial, Calendar dataFecho) {
        NUM ++;
        this.id = NUM;
        this.autor = autor;
        this.objeto = objeto;
        this.dataFecho = dataFecho;
        this.licitacoes = new ArrayList<Licitacao>();
        this.valorInicial = valorInicial;
    }

    public Licitador getAutor() {
        return autor;
    }

    public List<Licitacao> getLicitacoes() {
        return licitacoes;
    }

    public Licitacao getMaiorLicitacao() {
        if (!licitacoes.isEmpty()) {
            return licitacoes.get(licitacoes.size() - 1);
        }
        return null;
    }

    public boolean temLicitacoes() {
        return !licitacoes.isEmpty();
    }

    public double getValorInicial() {
        return valorInicial;
    }

    public void fazerLicitacao(Licitacao licitacao) {
        licitacoes.add(licitacao);
    }

    @Override
    public boolean equals(Object obj) {
        return this.id == ((Leilao)obj).getId();
    }

    public int getId() {
        return id;
    }

    public boolean hasFinished() {
        return dataFecho.before(Calendar.getInstance());
    }

    @Override
    public String toString() {
        if (!licitacoes.isEmpty()) {
            return this.id + " " + this.objeto + " " + dataFecho.getTime() + " " + getMaiorLicitacao().getQuantia() + " " + getMaiorLicitacao().getUsername();
        }
        else {
            return this.id + " " + this.objeto + " " + dataFecho.getTime() + " " + valorInicial + " " + autor.getUsername();
        }
    }
}
