package leilao;

public class Pedido {

    private String username;
    private int tipo;

    public Pedido(String username, int tipo) {
        this.username = username;
        this.tipo = tipo;
    }

    public String getUsername() {
        return username;
    }

    public int getTipo() {
        return tipo;
    }

    @Override
    public String toString() {
        return username + ":" + tipo;
    }
}
