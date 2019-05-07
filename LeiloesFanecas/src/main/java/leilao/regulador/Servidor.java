package leilao.regulador;

import leilao.Autenticacao;
import leilao.Pedido;
import leilao.SHA256;
import leilao.licitador.Licitador;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Servidor {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Map<String, Licitador> licitadores;

    public Servidor(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        licitadores = new HashMap<String, Licitador>();
    }

    public void start() throws IOException {
        System.out.println("Servidor inicializado");
        Thread threadRegistarLicitadores = new Thread(new Runnable() {
            public void run() {
                try {
                    registarLicitadores();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
        });
        threadRegistarLicitadores.start();
        while (true) {
            clientSocket = serverSocket.accept();
            Thread threadRedirecionarPedidos = new Thread(new Runnable() {
                public void run() {
                    try {
                        reendirecionarPedido(clientSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            threadRedirecionarPedidos.start();
        }
    }

    public void registarLicitadores() throws NoSuchAlgorithmException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String username;
            do {
                System.out.println("Insira um username");
                username = scanner.nextLine();
                if (licitadores.containsKey(username)) {
                    System.out.println("O username " + username + " já existe");
                }
            } while (licitadores.containsKey(username));
            System.out.println("Insira uma password");
            String password = SHA256.generate(scanner.nextLine().getBytes());
            String plafond;
            do {
                System.out.println("Insira o seu plafond");
                plafond = scanner.nextLine();
            } while (!verificarSeDouble(plafond));
            Licitador licitador = new Licitador(username, password, Double.parseDouble(plafond));
            licitadores.put(licitador.getUsername(), licitador);
            //todo Atualizar ficheiro de utilizadores
            System.out.println("Utilizador registado");
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

    public void reendirecionarPedido(Socket socket) throws IOException, NoSuchAlgorithmException {
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String [] pedido;
        do {
            pedido = input.readLine().split(":");
            switch (Integer.parseInt(pedido[1])) {
                case 0:
                    enviarNotificacoes("quit", socket);
                    System.out.println("O user " + pedido[0] + " desconectou-se");
                    break;
                case 1:
                    responderAutenticacao(new Autenticacao(pedido[0], pedido[2]), socket);
                    break;
                case 2:
                    //Licitacao
                    enviarNotificacoes("Licitar ainda não está disponivel", socket);
                    break;
                case 3:
                    //PedidoCriarLeilao
                    enviarNotificacoes("Criar leilão ainda não está disponivel", socket);
                    break;
                case 4:
                    //ListaLeiloes
                    enviarNotificacoes("Listar leilões ainda não está disponivel", socket);
                    break;
                case 5:
                    responderPlafond(new Pedido(pedido[0], 5), socket);
                    break;
                default:
                    enviarNotificacoes("Comando inválido", socket);
            }
        } while (Integer.parseInt(pedido[1]) != 0);
    }

    public void responderPlafond(Pedido pedido, Socket socket) throws IOException {
        enviarNotificacoes(Double.toString(licitadores.get(pedido.getUsername()).getPlafond()), socket);
    }

    public void responderAutenticacao(Autenticacao pedido, Socket socket) throws IOException, NoSuchAlgorithmException {
        if (licitadores.containsKey(pedido.getUsername())) {
            if (!licitadores.get(pedido.getUsername()).conectar(pedido.getPassword())) {
                enviarNotificacoes("Password incorreta", socket);
            }
            else if (!licitadores.get(pedido.getUsername()).estaConectado()) {
                enviarNotificacoes("O utilizador já está autenticado", socket);
            }
            else {
                enviarNotificacoes("Utilizador verificado", socket);
            }
        }
        else {
            enviarNotificacoes("Utilizador não existente", socket);
        }
    }

    public synchronized void enviarNotificacoes(String mensagem, Socket socket) throws IOException {
        InetAddress address = socket.getInetAddress();
        int port = socket.getLocalPort();
        DatagramPacket pacote = new DatagramPacket(mensagem.getBytes(), mensagem.getBytes().length, address, port);
        DatagramSocket output = new DatagramSocket();
        output.send(pacote);
    }
}
