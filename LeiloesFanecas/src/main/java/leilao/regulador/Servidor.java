package leilao.regulador;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import leilao.Pedido;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class Servidor {

    private ServerSocket serverSocket;
    private Socket clientSocket;

    public Servidor(String ip) throws IOException {
        serverSocket = new ServerSocket(3000);
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
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    public void reendirecionarPedido(Socket socket) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String [] pedido = input.readLine().split(":");
        switch (Integer.parseInt(pedido[1])) {
            case 1:
                //Autenticaçao
                enviarNotificacoes(pedido[0], socket);
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
                System.out.println("Comando Inválido");
        }
    }

    public void enviarNotificacoes(String mensagem, Socket socket) throws IOException {
        InetAddress address = socket.getInetAddress();
        int port = socket.getLocalPort();
        DatagramPacket pacote = new DatagramPacket(mensagem.getBytes(), mensagem.getBytes().length, address, port);
        DatagramSocket output = new DatagramSocket();
        output.send(pacote);
    }
}
