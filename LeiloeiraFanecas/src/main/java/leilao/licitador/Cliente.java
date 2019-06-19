package leilao.licitador;

import leilao.Autenticacao;
import leilao.Licitacao;
import leilao.Pedido;
import leilao.PedidoCriarLeilao;

import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

public class Cliente {

    private DatagramSocket inputSocket;
    private Socket outputSocket;
    private String ip;
    private int port;

    public Cliente(String ip, int port) throws Exception {
        this.ip = ip;
        this.port = port;
        this.inputSocket = new DatagramSocket(port);
    }

    public void start() throws Exception {
        int resposta;
        do {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Escolha uma opção");
            System.out.println("1 - Modo manual");
            System.out.println("2 - Modo automatico");
            System.out.println("3 - Quit");
            resposta = Integer.parseInt(scanner.nextLine());
            switch (resposta) {
                case 1:
                    iniciarModoManual();
                    break;
                case 2:
                    iniciarModoAutomatico();
                    break;
            }
        } while (resposta != 3);
    }

    public void iniciarModoManual() throws Exception {
        this.outputSocket = new Socket(ip, port);
        Scanner scanner = new Scanner(System.in);
        String username;
        String password;
        do {
            System.out.println("Insira um username");
            username = scanner.nextLine();
            System.out.println("Insira uma password");
            password = scanner.nextLine();
        } while (!autenticar(username, password));
        Thread threadReceberNotificacoes = new Thread(new Runnable() {
            public void run() {
                String resposta = "";
                do {
                    try {
                        resposta = receberNotificacoes();
                        if (!resposta.equals("quit")) {
                            System.out.println(resposta);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (!resposta.equals("quit"));
            }
        });
        threadReceberNotificacoes.start();
        selecionarPedido(username);
    }

    public void iniciarModoAutomatico() throws Exception {
        List<LicitadorAutomatico> licitadoresAutomaticos = autenticarLicitadoresAutomaticos();
        for (final LicitadorAutomatico licitadorAutomatico : licitadoresAutomaticos) {
            System.out.println(licitadorAutomatico);
            Thread threadPedidoAutomaticos = new Thread(new Runnable() {
                public void run() {
                    try {
                        fazerPedidosAutomaticos(licitadorAutomatico);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            threadPedidoAutomaticos.start();
        }
    }

    public void fazerPedidosAutomaticos(LicitadorAutomatico licitadorAutomatico) throws Exception {
        for (int i = 0; i < licitadorAutomatico.getNumeroLicitacoes(); i++) {
            licitadorAutomatico.enviarPedido(new Licitacao(licitadorAutomatico.getUsername(), licitadorAutomatico.getValorInicial() + (licitadorAutomatico.getIncrementoLicitacao() * i), licitadorAutomatico.getIdLeilao()));
            long tempoInicial = System.currentTimeMillis();
            while (tempoInicial + licitadorAutomatico.getTempoEspera() > System.currentTimeMillis());
        }
        licitadorAutomatico.enviarPedido(new Pedido(licitadorAutomatico.getUsername(), Pedido.QUIT));
    }

    public List<LicitadorAutomatico> autenticarLicitadoresAutomaticos() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Quantos licitadores quer iniciar?");
        int numeroLicitadores = Integer.parseInt(scanner.nextLine());
        Socket outputSocket = new Socket(ip, port);
        List<LicitadorAutomatico> licitadoresAutomaticos = new ArrayList<LicitadorAutomatico>();
        for (int i = 1; i <= numeroLicitadores; i++) {
            System.out.println("Insira o username do cliente " + i);
            String user = scanner.nextLine();
            System.out.println("Insira a password");
            String pass = scanner.nextLine();
            System.out.println("Insere o Id do leilão que o cliente " + i + " deve licitar");
            int idLeilao = Integer.parseInt(scanner.nextLine());
            System.out.println("Insira o numero de licitacoes que o cliente " + i + " deve licitar");
            int numeroLicitacoes = Integer.parseInt(scanner.nextLine());
            System.out.println("Insere o tempo entre licitacoes que o cliente " + i + " deve esperar");
            long tempoEspera = Long.parseLong(scanner.nextLine());
            System.out.println("Insira o valor inicial que o cliente " + i + " deve licitar");
            double valorInicial = Integer.parseInt(scanner.nextLine());
            System.out.println("Insira o valor a incrementar às licitações ao cliente " + i);
            double incremento = Double.parseDouble(scanner.nextLine());
            LicitadorAutomatico licitadorAutomatico = new LicitadorAutomatico(user, pass, idLeilao, numeroLicitacoes, tempoEspera, valorInicial, incremento, outputSocket);
            licitadorAutomatico.enviarPedido(new Autenticacao(licitadorAutomatico.getUsername(), licitadorAutomatico.getPassword()));
            String respota = receberNotificacoes();
            while (!respota.equals("Utilizador verificado")) {
                System.out.println("Insira de novo o username do cliente " + i);
                licitadorAutomatico.setUsername(scanner.nextLine());
                System.out.println("Insira de novo a password do cliente " + i);
                licitadorAutomatico.setPassword(scanner.nextLine());
                licitadorAutomatico.enviarPedido(new Autenticacao(licitadorAutomatico.getUsername(), licitadorAutomatico.getPassword()));
                respota = receberNotificacoes();
            }
            System.out.println(respota);
            licitadoresAutomaticos.add(licitadorAutomatico);
        }
        return licitadoresAutomaticos;
    }

    public String receberNotificacoes() throws Exception {
        byte[] buffer = new byte[256];
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
        inputSocket.receive(pacote);
        return new String(pacote.getData()).trim();
    }

    public boolean autenticar(String username, String password) throws Exception {
        PrintWriter output = new PrintWriter(outputSocket.getOutputStream(), true);
        output.println(new Autenticacao(username, password));
        String resposta = receberNotificacoes();
        System.out.println(resposta);
        return resposta.equals("Utilizador verificado");
    }

    public void selecionarPedido(String username) throws Exception {
        Scanner scanner = new Scanner(System.in);
        String resposta;
        System.out.println("Escolha uma opção");
        System.out.println("1 - Licitar");
        System.out.println("2 - Criar Leilão");
        System.out.println("3 - Listar Leilões");
        System.out.println("4 - Plafond");
        System.out.println("5 - Quit");
        do {
            resposta = scanner.nextLine();
            switch (Integer.parseInt(resposta)) {
                case 1:
                    fazerLicitacao(username);
                    break;
                case 2:
                    criarLeilao(username);
                    break;
                case 3:
                    pedirLeiloes(username);
                    break;
                case 4:
                    pedirPlafond(username);
                    break;
                case 5:
                    quit(username);
                    break;
            }
        } while (Integer.parseInt(resposta) != 5);
    }

    public void fazerLicitacao(String username) throws Exception {
        PrintWriter output = new PrintWriter(outputSocket.getOutputStream(), true);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Indique o Id do leilão em que quer licitar");
        int idLeilao = Integer.parseInt(scanner.nextLine());
        System.out.println("Insira a quantia que deseja licitar");
        double quantia = Double.parseDouble(scanner.nextLine());
        output.println(new Licitacao(username, quantia, idLeilao));
    }

    public void criarLeilao(String username) throws Exception {
        PrintWriter output = new PrintWriter(outputSocket.getOutputStream(), true);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Descreve o objeto que quer leiloar");
        String objeto = scanner.nextLine();
        Calendar calendar;
        do {
            System.out.println("Insira a data de fecho");
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
            calendar = Calendar.getInstance();
            calendar.set(ano, mes - 1, dia, hora, minuto);
        } while (calendar.before(Calendar.getInstance()));
        System.out.println("Insira o valor inicial do leilão");
        double valorInicial = Double.parseDouble(scanner.nextLine());
        output.println(new PedidoCriarLeilao(username, objeto, calendar, valorInicial));
    }

    public void pedirLeiloes(String username) throws Exception {
        PrintWriter output = new PrintWriter(outputSocket.getOutputStream(), true);
        output.println(new Pedido(username, Pedido.LISTAR_LEILAO));
    }

    public void pedirPlafond(String username) throws Exception {
        PrintWriter output = new PrintWriter(outputSocket.getOutputStream(), true);
        output.println(new Pedido(username, Pedido.PLAFOND));
    }

    public void quit(String username) throws Exception {
        PrintWriter output = new PrintWriter(outputSocket.getOutputStream(), true);
        output.println(new Pedido(username, Pedido.QUIT));
    }
}
