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

    public Leilao(Licitador autor, String objeto, double valorInicial, Date date) {
        NUM ++;
        this.id = NUM;
        this.autor = autor;
        this.objeto = objeto;
        this.date = date;
        this.maiorLicitacao = new Licitacao(autor.getUsername(), valorInicial, this.id);
        this.licitadores = new HashSet<Licitador>();
    }

    public Licitador getAutor() {
        return autor;
    }

    public Set<Licitador> getLicitadores() {
        return licitadores;
    }

    public Licitacao getMaiorLicitacao() {
        return maiorLicitacao;
    }

    public synchronized boolean licitar(Licitador licitador, Licitacao licitacao) {
        if (licitacao.getQuantia() > maiorLicitacao.getQuantia()) {
            licitadores.add(licitador);
            maiorLicitacao = licitacao;
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
        return date.after(new Date());
    }

    //<ID> <Descrição> <Data de fecho> <Valor da Proposta> <Username>
    @Override
    public String toString() {
        return this.id + " " + this.objeto + " " + this.date.getDay() + "/" + this.date.getMonth() + "/" + this.date.getYear() + " " + this.maiorLicitacao.getQuantia() + " " + maiorLicitacao.getUsername();
    }
}
