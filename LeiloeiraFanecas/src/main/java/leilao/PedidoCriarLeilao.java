package leilao;

import java.util.Calendar;
import java.util.Date;

public class PedidoCriarLeilao extends Pedido {

    private String objeto;
    private Calendar dataFecho;
    private double valorInicial;

    public PedidoCriarLeilao(String username, String objeto, Calendar dataFecho, double valorInicial) {
        super(username, Pedido.CRIAR_LEILAO);
        this.objeto = objeto;
        this.dataFecho = dataFecho;
        this.valorInicial = valorInicial;
    }

    public Calendar getDateFecho() {
        return dataFecho;
    }

    public double getValorInicial() {
        return valorInicial;
    }

    public String getObjeto() {
        return objeto;
    }

    @Override
    public String toString() {
        return super.toString() + "#" + objeto + "#" + dataFecho.get(Calendar.YEAR) + "#" + dataFecho.get(Calendar.MONTH) + "#" + dataFecho.get(Calendar.DATE) + "#" + dataFecho.get(Calendar.HOUR_OF_DAY) + "#" + dataFecho.get(Calendar.MINUTE) + "#" + valorInicial;
    }
}