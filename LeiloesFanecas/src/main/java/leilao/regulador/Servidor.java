package leilao.regulador;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import leilao.Autenticacao;
import leilao.Pedido;
import leilao.licitador.Licitador;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Servidor {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Map<String, Licitador> licitadores;

    public Servidor(String ip) throws IOException {
        serverSocket = new ServerSocket(3000);
        licitadores = new HashMap<String, Licitador>();
    }

    public void start() throws IOException {
        System.out.println("Servidor inicializado");
        while (true) {
            clientSocket = serverSocket.accept();
            System.out.println("Nova conexão");
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        reendirecionarPedido(clientSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
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
                    break;
                case 3:
                    //PedidoCriarLeilao
                    break;
                case 4:
                    //ListaLeiloes
                    break;
                case 5:
                    //Plafond
                    break;
                default:
                    enviarNotificacoes("Comando inválido", socket);
            }
        } while (Integer.parseInt(pedido[1]) != 0);
    }

    public void responderAutenticacao(Autenticacao pedido, Socket socket) throws IOException, NoSuchAlgorithmException {
        if (!licitadores.containsKey(pedido.getUsername())) {
            if (licitadores.get(pedido.getUsername()).conectar(pedido.getPassword())) {
                enviarNotificacoes("Utilizador verificado", socket);
            }
            else {
                enviarNotificacoes("Password incorreta", socket);
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
