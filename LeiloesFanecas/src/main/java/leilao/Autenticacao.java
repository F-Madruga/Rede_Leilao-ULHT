package leilao;

import java.security.NoSuchAlgorithmException;

public class Autenticacao extends Pedido {

    private String password;

    public Autenticacao (String username, String password) {
        super(username, Pedido.AUTENTICACAO);
        this.password = password;
    }

    public String getPassword() throws NoSuchAlgorithmException {
        return SHA256.generate(password.getBytes());
    }

    @Override
    public String toString() {
        return super.toString() + ":" + password;
    }
}
