package leilao.regulador;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Servidor servidor = new Servidor(Integer.parseInt(args[0]));
        servidor.start();
    }
}
