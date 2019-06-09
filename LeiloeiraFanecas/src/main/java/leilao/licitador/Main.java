package leilao.licitador;

public class Main {
    public static void main(String[] args) throws Exception{
        Cliente cliente = new Cliente(args[0], Integer.parseInt(args[1]));
        cliente.start();
    }
}
