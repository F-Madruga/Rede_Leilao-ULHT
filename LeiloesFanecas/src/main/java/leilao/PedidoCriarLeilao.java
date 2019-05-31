package leilao;

import java.util.Date;

public class PedidoCriarLeilao extends Pedido {

    private String objeto;
    private Date date;
    private double valorInicial;

    public PedidoCriarLeilao(String username, String objeto, Date date, double valorInicial) {
        super(username, Pedido.CRIAR_LEILAO);
        this.objeto = objeto;
        this.date = date;
        this.valorInicial = valorInicial;
    }

    public Date getDate() {
        return date;
    }

    public double getValorInicial() {
        return valorInicial;
    }

    public String getObjeto() {
        return objeto;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + objeto + ":" + date.getYear() + ":" + date.getMonth() + ":" + date.getDay() + ":" + valorInicial;
    }
}
