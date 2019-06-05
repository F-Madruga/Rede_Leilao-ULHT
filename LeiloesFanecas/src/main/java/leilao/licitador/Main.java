package leilao.licitador;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException{
        Cliente cliente = new Cliente(args[0], Integer.parseInt(args[1]));
        cliente.start();
    }
}
