package leilao;

public class PedidoCriarLeilao extends Pedido {

    private String objeto;

    public PedidoCriarLeilao(String username, String objeto) {
        super(username, 3);
        this.objeto = objeto;
    }

    public String getObjeto() {
        return objeto;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + objeto;
    }
}
