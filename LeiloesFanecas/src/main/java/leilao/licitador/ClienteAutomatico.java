package leilao.licitador;

public class ClienteAutomatico {
    private String username;
    private int idLeilao;
    private int numeroLicitacoes;
    private int tempoEntreLicitacao;
    private double valorPrimeiraLicitacao;
    private double incrementoLicitacao;

    public ClienteAutomatico(String username, int idLeilao, int numeroLicitacoes, int tempoEntreLicitacao, double valorPrimeiraLicitacao, double incrementoLicitacao) {
        this.username = username;
        this.idLeilao = idLeilao;
        this.numeroLicitacoes = numeroLicitacoes;
        this.tempoEntreLicitacao = tempoEntreLicitacao;
        this.valorPrimeiraLicitacao = valorPrimeiraLicitacao;
        this.incrementoLicitacao = incrementoLicitacao;
    }

    public String getUsername() {
        return username;
    }

    public int getIdLeilao() {
        return idLeilao;
    }

    public int getNumeroLicitacoes() {
        return numeroLicitacoes;
    }

    public int getTempoEntreLicitacao() {
        return tempoEntreLicitacao;
    }

    public double getValorPrimeiraLicitacao() {
        return valorPrimeiraLicitacao;
    }

    public double getIncrementoLicitacao() {
        return incrementoLicitacao;
    }
}
