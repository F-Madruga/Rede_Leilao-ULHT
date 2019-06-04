package leilao.regulador;

import leilao.Licitacao;
import leilao.licitador.Licitador;

import java.util.*;

public class Leilao {
    private static int NUM = 0;
    private int id;
    private Licitador autor;
    private String objeto;
    private Date date;
    private List<Licitacao> licitacoes;

    public Leilao(Licitador autor, String objeto, double valorInicial, Date date) {
        NUM ++;
        this.id = NUM;
        this.autor = autor;
        this.objeto = objeto;
        this.date = date;
        this.licitacoes = new ArrayList<Licitacao>();
        this.licitacoes.add(new Licitacao(autor.getUsername(), valorInicial, this.id));
    }

    public Licitador getAutor() {
        return autor;
    }

    public List<Licitacao> getLicitacoes() {
        return licitacoes;
    }

    public Licitacao getMaiorLicitacao() {
        return licitacoes.get(licitacoes.size() - 1);
    }

    public void licitar(Licitacao licitacao) {
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
        return date.before(new Date());
    }

    @Override
    public String toString() {
        return this.id + " " + this.objeto + " " + this.date.getDay() + "/" + this.date.getMonth() + "/" + this.date.getYear() + " " + getMaiorLicitacao().getQuantia() + " " + getMaiorLicitacao().getUsername();
    }
}
