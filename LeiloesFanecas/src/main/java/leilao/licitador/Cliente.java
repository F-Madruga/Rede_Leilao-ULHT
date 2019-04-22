package leilao.licitador;

import leilao.Autenticacao;
import leilao.Licitacao;
import leilao.Pedido;
import leilao.PedidoCriarLeilao;

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
    private String username;

    public Cliente(String ip, int port) throws IOException {
        inputSocket = new DatagramSocket(port);
        outputSocket = new Socket(ip, port);
        output = new PrintWriter(outputSocket.getOutputStream(), true);
    }

    public void start() throws IOException {
        autenticar();
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
        String messagem;
        do {
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
            inputSocket.receive(pacote);
            messagem = new String(pacote.getData()).trim();
            System.out.println(messagem);
        } while (!messagem.equals("quit"));
    }

    public void fazerPedidos() {
        Scanner scanner = new Scanner(System.in);
        String resposta;
        System.out.println("Escolha uma opção");
        System.out.println("1 - Licitar");
        System.out.println("2 - Criar Leilão");
        System.out.println("3 - Listar Leilões");
        System.out.println("4 - Plafond");
        System.out.println("quit - quit");
        do {
            do {
                resposta = scanner.nextLine();
            } while (!verificarResposta(resposta, 5));
            if (!resposta.equals("quit")) {
                switch (Integer.parseInt(resposta)) {
                    case 1:
                        fazerLicitacao();
                        break;
                    case 2:
                        criarLeilao();
                        break;
                    case 3:
                        pedirLeiloes();
                        break;
                    case 4:
                        pedirPlafond();
                        break;
                }
            }
            else {
                output.println(new Pedido(username, 0));
            }
        } while (!resposta.equals("quit"));
    }

    public void autenticar() throws IOException {
        Scanner scanner = new Scanner(System.in);
        byte[] buffer = new byte[256];
        String resposta;
        do {
            System.out.println("Insira o seu username");
            username = scanner.nextLine();
            System.out.println("Insira a sua password");
            output.println(new Autenticacao(username, scanner.nextLine()));
            DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
            inputSocket.receive(pacote);
            resposta = new String(pacote.getData()).trim();
            System.out.println(resposta);
        } while (!resposta.equals("Utilizador verificado"));
    }

    public void pedirLeiloes() {
        output.println(new Pedido(username, 4));
    }

    public void pedirPlafond() {
        output.println(new Pedido(username, 5));
    }

    public void criarLeilao() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Descreve o objeto que quer leiloar");
        output.println(new PedidoCriarLeilao(username, scanner.nextLine()));
    }

    public void fazerLicitacao() {
        Scanner scanner = new Scanner(System.in);
        String idLeilao;
        String quantia;
        do {
            System.out.println("Indique o ID do leilão para o qual quer licitar");
            idLeilao = scanner.nextLine();
            System.out.println("Indique a quantia que quer licitar");
            quantia = scanner.nextLine();
        } while (!verificarSeInt(idLeilao) || !verificarSeDouble(quantia));
        output.println(new Licitacao(username, Double.parseDouble(quantia), Integer.parseInt(idLeilao)));
    }

    public boolean verificarResposta(String resposta, int numeroDeRespostas) {
        if (resposta.equals("quit")) {
            return true;
        }
        try {
            int opcao = Integer.parseInt(resposta);
            if (opcao > 0 && opcao <= numeroDeRespostas) {
                return true;
            }
            System.out.println("Resposta inválida");
            return false;
        } catch (NumberFormatException e) {
            System.out.println("Resposta inválida");
            return false;
        }
    }

    public boolean verificarSeDouble(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Resposta inválida");
            return false;
        }
    }

    public boolean verificarSeInt(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Resposta inválida");
            return false;
        }
    }
}
