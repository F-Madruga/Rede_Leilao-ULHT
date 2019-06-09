package leilao.licitador;

public class LicitadorAutomatico {

    private String username;
    private String password;
    private int idLeilao;
    private int numeroLicitacoes;
    private long tempoEspera;
    private double valorInicial;
    private double incrementoLicitacao;

    public LicitadorAutomatico(String username, String password, int idLeilao, int numeroLicitacoes, long tempoEspera, double valorInicial, double incrementoLicitacao) {
        this.username = username;
        this.password = password;
        this.idLeilao = idLeilao;
        this.numeroLicitacoes = numeroLicitacoes;
        this.tempoEspera = tempoEspera;
        this.valorInicial = valorInicial;
        this.incrementoLicitacao = incrementoLicitacao;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getIdLeilao() {
        return idLeilao;
    }

    public int getNumeroLicitacoes() {
        return numeroLicitacoes;
    }

    public long getTempoEspera() {
        return tempoEspera;
    }

    public double getValorInicial() {
        return valorInicial;
    }

    public double getIncrementoLicitacao() {
        return incrementoLicitacao;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "Username - " + this.username + "\nId leilao - " + this.idLeilao + "\nNumero de licitacoes - " + this.numeroLicitacoes + "\nTempo entre licitacoes - " + this.tempoEspera + "ms\nValor da primeira licitação - " + this.valorInicial + "\nIncremento das licitações - " + this.incrementoLicitacao + "\n";
    }
}
