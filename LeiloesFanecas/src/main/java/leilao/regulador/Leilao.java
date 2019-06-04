package leilao.regulador;

import leilao.Licitacao;
import leilao.licitador.Licitador;

import java.util.*;

public class Leilao {
    private static int NUM = 0;
    private int id;
    private Licitador autor;
    private Set<Licitador> licitadores;
    private String objeto;
    private Date date;
    private List<Licitacao> licitacoes;

    public Leilao(Licitador autor, String objeto, double valorInicial, Date date) {
        NUM ++;
        this.id = NUM;
        this.autor = autor;
        this.objeto = objeto;
        this.date = date;
        this.licitadores = new HashSet<Licitador>();
        this.licitacoes = new ArrayList<Licitacao>();
        this.licitacoes.add(new Licitacao(autor.getUsername(), valorInicial, this.id));
    }

    public Licitador getAutor() {
        return autor;
    }

    public Set<Licitador> getLicitadores() {
        return licitadores;
    }

    public Licitacao getMaiorLicitacao() {
        return licitacoes.get(licitacoes.size() - 1);
    }

    public synchronized boolean licitar(Licitador licitador, Licitacao licitacao) {
        if (licitacao.getQuantia() > getMaiorLicitacao().getQuantia()) {
            licitadores.add(licitador);
            licitacoes.add(licitacao);
            return true;
        }
        return false;
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
