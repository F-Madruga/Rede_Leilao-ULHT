package leilao.licitador;

import leilao.Autenticacao;
import leilao.Licitacao;
import leilao.Pedido;
import leilao.PedidoCriarLeilao;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;

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
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    receberNotificacoes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Scanner scanner = new Scanner(System.in);
        System.out.println("Escolha uma opção");
        System.out.println("1 - Modo manual");
        System.out.println("2 - Modo automático");
        String resposta = scanner.nextLine();
        switch (Integer.parseInt(resposta)) {
            case 1:
                autenticar();
                thread.start();
                fazerPedidos();
                break;
            case 2:
                List<ClienteAutomatico> clienteAutomaticos = definirClientesAutomaticos();
                thread.start();
                for (final ClienteAutomatico clienteAutomatico : clienteAutomaticos) {
                    Thread threadClientAutomatico = new Thread(new Runnable() {
                        public void run() {
                            try {
                                fazerPedidosAutomatico( clienteAutomatico);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    threadClientAutomatico.start();
                }
        }
    }

    public void fazerPedidosAutomatico(ClienteAutomatico cliente) throws IOException {
        PrintWriter out = new PrintWriter(outputSocket.getOutputStream());
        for (int i = 0; i < cliente.getNumeroLicitacoes(); i++) {
            out.println(new Licitacao(cliente.getUsername(), cliente.getValorPrimeiraLicitacao() + (cliente.getIncrementoLicitacao() * i), cliente.getIdLeilao()));
            long tempoInicial = System.currentTimeMillis();
            while (System.currentTimeMillis() - tempoInicial < cliente.getTempoEntreLicitacao());
        }
    }

    public List<ClienteAutomatico> definirClientesAutomaticos() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Quantos cliente deseja iniciar?");
        int numeroClientes = Integer.parseInt(scanner.nextLine());
        List<ClienteAutomatico> clienteAutomaticos = new ArrayList<ClienteAutomatico>();
        for (int i = 1; i <= numeroClientes; i++) {
            String resposta;
            String username;
            do {
                byte[] buffer = new byte[256];
                System.out.println("Insira o seu username do cliente " + i);
                username = scanner.nextLine();
                System.out.println("Insira a sua password do cliente " + i);
                output.println(new Autenticacao(username, scanner.nextLine()));
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                inputSocket.receive(pacote);
                resposta = new String(pacote.getData()).trim();
                System.out.println(resposta);
            } while (!resposta.equals("Utilizador verificado"));
            System.out.println("Insira o ID do leilão que o cliente " + i + " deve licitar");
            int idLeilao = Integer.parseInt(scanner.nextLine());
            System.out.println("Insira o numero de licitações que o cliente " + i + " deve executar");
            int numeroLicitacoes = Integer.parseInt(scanner.nextLine());
            System.out.println("Insira o tempo que o cliente " + i + " deve esperar entre licitacoes");
            int tempoEspera = Integer.parseInt(scanner.nextLine());
            System.out.println("Insira o valor inicial que o cliente " + i + " deve licitar");
            double valorInicial = Double.parseDouble(scanner.nextLine());
            System.out.println("Insira o valor que o cliente " + i + " deve acrescentar à licitação anterior (incremento da licitação)");
            double incremento = Double.parseDouble(scanner.nextLine());
            clienteAutomaticos.add(new ClienteAutomatico(username, idLeilao, numeroLicitacoes, tempoEspera, valorInicial, incremento));
        }
        return clienteAutomaticos;
    }

    public void receberNotificacoes() throws IOException {
        String messagem;
        do {
            byte[] buffer = new byte[256];
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
                output.println(new Pedido(username, Pedido.QUIT));
            }
        } while (!resposta.equals("quit"));
    }

    public void autenticar() throws IOException {
        Scanner scanner = new Scanner(System.in);
        String resposta;
        do {
            byte[] buffer = new byte[256];
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
        output.println(new Pedido(username, Pedido.LISTAR_LEILAO));
    }

    public void pedirPlafond() {
        output.println(new Pedido(username, Pedido.PLAFOND));
    }

    public void criarLeilao() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Descreve o objeto que quer leiloar");
        String objeto = scanner.nextLine();
        System.out.println("Insira o dia de fecho do leilão");
        int dia = Integer.parseInt(scanner.nextLine());
        System.out.println("Insira o mes de fecho do leilão");
        int mes = Integer.parseInt(scanner.nextLine());
        System.out.println("Insira o ano de fecho do leilão");
        int ano = Integer.parseInt(scanner.nextLine());
        System.out.println("Insira a hora de fecho do leilão");
        int hora = Integer.parseInt(scanner.nextLine());
        System.out.println("Insira o minuto de fecho do leilão");
        int minuto = Integer.parseInt(scanner.nextLine());
        Calendar calendar = Calendar.getInstance();
        calendar.set(ano, mes - 1, dia, hora, minuto);
        System.out.println("Insira o valor inicial do leilão");
        double valorInicial = Double.parseDouble(scanner.nextLine());
        output.println(new PedidoCriarLeilao(username, objeto, calendar, valorInicial));
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
