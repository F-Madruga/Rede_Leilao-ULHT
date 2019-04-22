package leilao.licitador;

import leilao.Autenticacao;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {

    private DatagramSocket inputSocket;
    private Socket outputSocket;
    private PrintWriter output;

    public Cliente(String ip, int port) throws IOException {
        inputSocket = new DatagramSocket(port);
        outputSocket = new Socket(ip, port);
        output = new PrintWriter(outputSocket.getOutputStream(), true);
    }

    public void start() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    receberNotificacoes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        fazerPedidos();
    }

    public void receberNotificacoes() throws IOException {
        byte[] buffer = new byte[256];
        String message;
        do {
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
            inputSocket.receive(pacote);
            message = new String(pacote.getData()).trim();
        } while (!message.equals("quit"));
    }

    public void fazerPedidos() {
        Scanner scanner = new Scanner(System.in);
        String resposta;
        do {
            System.out.println("Escolha uma opção");
            System.out.println("1 - Autenticação");
            System.out.println("2 - Licitar");
            System.out.println("3 - Criar Leilão");
            System.out.println("4 - Listar Leilões");
            System.out.println("5 - Plafond");
            resposta = scanner.nextLine();
            output.println(new Autenticacao("Rodrigo", "12345"));
        } while (!resposta.equals("quit"));
    }
}
