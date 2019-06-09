package leilao;

public class Autenticacao extends Pedido {

    private String password;

    public Autenticacao (String username, String password) {
        super(username, Pedido.AUTENTICACAO);
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return super.toString() + "#" + password;
    }
}
