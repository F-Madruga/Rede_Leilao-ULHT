package leilao;

import java.io.Serializable;

public class Pedido implements Serializable {

    public static final int QUIT = 0;
    public static final int AUTENTICACAO = 1;
    public static final int LICITACAO = 2;
    public static final int CRIAR_LEILAO = 3;
    public static final int LISTAR_LEILAO = 4;
    public static final int PLAFOND = 5;

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
        return username + "#" + tipo;
    }
}
