package leilao.licitador;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException{
        Cliente cliente = new Cliente("192.168.1.85", 3000);
        cliente.start();
    }
}
