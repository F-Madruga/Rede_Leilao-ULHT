package leilao;

public class Licitacao extends Pedido{

    private double quantia;
    private int idLeilao;

    public Licitacao(String username, double quantia, int idLeilao) {
        super(username, 2);
        this.quantia = quantia;
        this.idLeilao = idLeilao;
    }

    public double getQuantia() {
        return quantia;
    }

    public int getIdLeilao() {
        return idLeilao;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + quantia + ":" + idLeilao;
    }
}
