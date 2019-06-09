package leilao.regulador;

import leilao.Licitacao;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class Leilao implements Serializable {

    public static int NUM = 0;
    private int id;
    private String autor;
    private String objeto;
    private Calendar dataFecho;
    private double valorInicial;
    private Licitacao maiorLicitacao;
    private Set<String> participantes;
    private boolean terminado;

    public Leilao(String autor, String objeto, double valorInicial, Calendar dataFecho) {
        NUM ++;
        this.id = NUM;
        this.autor = autor;
        this.objeto = objeto;
        this.dataFecho = dataFecho;
        this.valorInicial = valorInicial;
        this.participantes = new HashSet<String>();
        this.maiorLicitacao = null;
        this.terminado = false;
    }

    public int getId() {
        return id;
    }

    public boolean terminado() {
        return terminado;
    }

    public void terminar() {
        terminado = true;
    }

    public String getAutor() {
        return autor;
    }

    public String getObjeto() {
        return objeto;
    }

    public Calendar getDataFecho() {
        return dataFecho;
    }

    public double getValorInicial() {
        return valorInicial;
    }

    public Licitacao getMaiorLicitacao() {
        return maiorLicitacao;
    }

    public Set<String> getParticipantes() {
        return participantes;
    }

    public boolean temLicitacoes() {
        return maiorLicitacao != null;
    }

    public boolean chegaParaLicitar(Licitacao licitacao) {
        return (this.temLicitacoes() && licitacao.compareTo(maiorLicitacao) > 0) || (!this.temLicitacoes()) && licitacao.getQuantia() >= valorInicial;

        }

    public synchronized void licitar(Licitacao licitacao) {
        this.maiorLicitacao = licitacao;
        this.participantes.add(licitacao.getUsername());
    }

    public boolean hasFinished() {
        return dataFecho.before(Calendar.getInstance());
    }

    @Override
    public boolean equals(Object obj) {
        return this.id == ((Leilao)obj).getId();
    }

    @Override
    public String toString() {
        if (maiorLicitacao != null) {
            return this.id + " " + this.objeto + " " + dataFecho.getTime() + " " + maiorLicitacao.getQuantia() + " " + maiorLicitacao.getUsername();
        }
        else {
            return this.id + " " + this.objeto + " " + dataFecho.getTime() + " " + valorInicial + " " + autor;
        }
    }
}
