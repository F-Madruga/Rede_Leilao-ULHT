package leilao.regulador;

public class Main {

    public static void main(String[] args) throws Exception {
        Servidor servidor = new Servidor(Integer.parseInt(args[0]));
        servidor.start();
    }
}