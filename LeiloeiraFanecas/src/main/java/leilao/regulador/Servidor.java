package leilao.regulador;

import leilao.Autenticacao;
import leilao.Licitacao;
import leilao.Pedido;
import leilao.PedidoCriarLeilao;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

public class Servidor {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private int port;
    private List<Licitador> licitadores;
    private List<Leilao> leiloes;

    public Servidor(int port) throws Exception {
        this.port = port;
        serverSocket = new ServerSocket(port);
        licitadores = new ArrayList<Licitador>();
        leiloes = new ArrayList<Leilao>();
    }

    public void start() throws Exception {
        lerFicheiroLicitadores();
        lerFicheiroLeiloes();
        Thread threadRegistarLicitadores = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        registarLicitador();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        threadRegistarLicitadores.start();
        Thread threadVerificarLeiloes = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    verificarLeiloes();
                }
            }
        });
        threadVerificarLeiloes.start();
        while (true) {
            clientSocket = serverSocket.accept();
            Thread threadPedidos = new Thread(new Runnable() {
                public void run() {
                    try {
                        reendirecionarPedido(clientSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            threadPedidos.start();
        }
    }

    public void registarLicitador() throws Exception {
        Scanner scanner = new Scanner(System.in);
        boolean usernameJaExiste;
        String username;
        do {
            usernameJaExiste = false;
            System.out.println("Insira um username");
            username = scanner.nextLine();
            for (Licitador licitador : this.licitadores) {
                if (licitador.getUsername().equals(username)) {
                    usernameJaExiste = true;
                    break;
                }
            }
            if (usernameJaExiste) {
                System.out.println("O username " + username + " já existe");
            }
        } while (usernameJaExiste);
        System.out.println("Insira uma password");
        String password = SHA256.generate(scanner.nextLine().getBytes());
        System.out.println("Insira o plafond inicial");
        double plafond = Double.parseDouble(scanner.nextLine());
        licitadores.add(new Licitador(username, password, plafond));
        atualizarFicheiroLicitadores();
        System.out.println("Licitador registado");
    }

    public synchronized void enviarNotificacoes(String mensagem, String address) throws IOException {
        DatagramPacket pacote = new DatagramPacket(mensagem.getBytes(), mensagem.getBytes().length, InetAddress.getByName(address), port);
        DatagramSocket output = new DatagramSocket();
        output.send(pacote);
    }

    public void reendirecionarPedido(Socket socket) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String [] pedido;
        do {
            pedido = input.readLine().split("#");
            switch (Integer.parseInt(pedido[1])) {
                case Pedido.QUIT:
                    for (Licitador licitador : this.licitadores) {
                        if (licitador.getUsername().equals(pedido[0])) {
                            licitador.desconectar();
                            break;
                        }
                    }
                    enviarNotificacoes("quit", socket.getInetAddress().getHostAddress());
                    System.out.println("O user " + pedido[0] + " desconectou-se");
                    break;
                case Pedido.AUTENTICACAO:
                    responderAutenticacao(new Autenticacao(pedido[0], pedido[2]), socket);
                    break;
                case Pedido.LICITACAO:
                    responderLicitacao(new Licitacao(pedido[0], Double.parseDouble(pedido[2]), Integer.parseInt(pedido[3])));
                    break;
                case Pedido.CRIAR_LEILAO:
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Integer.parseInt(pedido[3]), Integer.parseInt(pedido[4]), Integer.parseInt(pedido[5]), Integer.parseInt(pedido[6]), Integer.parseInt(pedido[7]));
                    responderCriarLeilao(new PedidoCriarLeilao(pedido[0], pedido[2], calendar, Double.parseDouble(pedido[8])));
                    break;
                case Pedido.LISTAR_LEILAO:
                    responderListaLeiloes(new Pedido(pedido[0], Integer.parseInt(pedido[1])));
                    break;
                case Pedido.PLAFOND:
                    responderPlafond(new Pedido(pedido[0], Integer.parseInt(pedido[1])));
                    break;
                default:
                    enviarNotificacoes("Comando inválido", socket.getInetAddress().getHostAddress());
            }
        } while (Integer.parseInt(pedido[1]) != Pedido.QUIT);
    }

    public synchronized void responderAutenticacao(Autenticacao pedido, Socket socket) throws Exception {
        boolean autenticacaoValida = false;
        for (Licitador licitador : this.licitadores) {
            if (licitador.autenticar(pedido.getUsername(), pedido.getPassword())) {
                licitador.conectar(socket.getInetAddress().getHostAddress());
                enviarNotificacoes("Utilizador verificado", licitador.getAddress());
                System.out.println("O utilizador " + pedido.getUsername() + " foi autenticado.");
                autenticacaoValida = true;
                break;
            }
        }
        if (!autenticacaoValida) {
            enviarNotificacoes("As credenciais estão incorretas", socket.getInetAddress().getHostAddress());
        }
    }

    public synchronized void responderCriarLeilao(PedidoCriarLeilao pedido) throws Exception {
        Leilao leilao = new Leilao(pedido.getUsername(), pedido.getObjeto(), pedido.getValorInicial(), pedido.getDateFecho());
        this.leiloes.add(leilao);
        atualizarFicheiroLeiloes();
        for (Licitador licitador : this.licitadores) {
            if (licitador.estaConectado()) {
                if (licitador.getUsername().equals(pedido.getUsername())) {
                    enviarNotificacoes("O seu leilão foi criado com sucesso com ID " + leilao.getId() + ".", licitador.getAddress());
                } else {
                    enviarNotificacoes("Há um novo leilão disponível, queira consultar os leilões disponíveis.", licitador.getAddress());
                }
            }
        }
        System.out.println("Foi criado um leilao com os seguintes dados.\n" + leilao);
    }

    public synchronized void responderLicitacao(Licitacao pedido) throws Exception {
        boolean leilaoExiste = false;
        boolean chegaParaLicitar = false;
        boolean temPlafond = false;
        Licitacao maiorLicitacao = null;
        Set<String> participantes = new HashSet<String>();
        for (Leilao leilao : this.leiloes) {
            if (leilao.getId() == pedido.getIdLeilao() && !leilao.terminado()) {
                leilaoExiste = true;
                participantes = leilao.getParticipantes();
                if (leilao.temLicitacoes()) {
                    maiorLicitacao = leilao.getMaiorLicitacao();
                }
                chegaParaLicitar = leilao.chegaParaLicitar(pedido);
                if (chegaParaLicitar) {
                    for (Licitador licitador : this.licitadores) {
                        if (licitador.getUsername().equals(pedido.getUsername())) {
                            temPlafond = licitador.temQuantia(pedido.getQuantia());
                            if (temPlafond) {
                                leilao.licitar(pedido);
                            }
                            break;
                        }
                    }
                }
                break;
            }
        }
        if (leilaoExiste && chegaParaLicitar && temPlafond) {
            System.out.println("O licitador " + pedido.getUsername() + " fez uma licitação de " + pedido.getQuantia() + " no leilão " + pedido.getIdLeilao() + ".");
            for (Licitador licitador : this.licitadores) {
                if (maiorLicitacao != null && licitador.getUsername().equals(maiorLicitacao.getUsername())) {
                    licitador.adicionarDinheiro(maiorLicitacao.getQuantia());
                }
                if (licitador.estaConectado() && participantes.contains(licitador.getUsername())) {
                    if (licitador.getUsername().equals(pedido.getUsername())) {
                        licitador.retirarDinheiro(pedido.getQuantia());
                        enviarNotificacoes("A sua licitação foi aceite.", licitador.getAddress());
                    }
                    else {
                        enviarNotificacoes("Foi recebida uma nova licitação no leilão com ID " + pedido.getIdLeilao() + ".", licitador.getAddress());
                    }
                }
            }
            atualizarFicheiroLeiloes();
            atualizarFicheiroLicitadores();
        }
        else {
            for (Licitador licitador : this.licitadores) {
                if (licitador.estaConectado() && licitador.getUsername().equals(pedido.getUsername())) {
                    if (!leilaoExiste) {
                        System.out.println("O licitador " + pedido.getUsername() + " tentou licitar num leilão que não existe");
                        enviarNotificacoes("O leilão com o ID " + pedido.getIdLeilao() + " não existe ou já não está disponível.", licitador.getAddress());
                    }
                    else if (!chegaParaLicitar) {
                        System.out.println("O licitador " + pedido.getUsername() + " tentou licitar no leilão com Id " + pedido.getIdLeilao() + " mas o valor da licitação era inferior à maior licitação do leilão");
                        enviarNotificacoes("A sua licitação não foi aceite, o valor proposto não é superior ao máximo atual.", licitador.getAddress());
                        }
                    else if (!temPlafond) {
                        System.out.println("O licitador " + pedido.getUsername() + " tentou licitar no leilão com Id " + pedido.getIdLeilao() + " mas o valor da licitação era superior ao seu plafond");
                        enviarNotificacoes("A sua solicitação não foi aceite, o valor da sua proposta é superior ao seu plafond.", licitador.getAddress());
                        }

                    break;
                }
            }
        }
    }

    public void responderListaLeiloes(Pedido pedido) throws Exception {
        System.out.println("O licitador " + pedido.getUsername() + " pediu a lista de leilões");
        for (Licitador licitador : licitadores) {
            if (licitador.getUsername().equals(pedido.getUsername())) {
                String mensagem = "Plafond disponivel: " + licitador.getPlafond() + "\n---------------------------------------------------------------";
                for (Leilao leilao :leiloes) {
                    if (!leilao.terminado()) {
                        mensagem += "\n" + leilao;
                    }
                }
                enviarNotificacoes(mensagem, licitador.getAddress());
                break;
            }
        }
    }

    public void responderPlafond(Pedido pedido) throws Exception {
        System.out.println("O licitador " + pedido.getUsername() + " pediu o seu plafond");
        for (Licitador licitador : licitadores) {
            if (licitador.getUsername().equals(pedido.getUsername())) {
                enviarNotificacoes("O seu plafond atual é de " + licitador.getPlafond() + " euros.", licitador.getAddress());
                break;
            }
        }
    }

    public void lerFicheiroLeiloes() throws IOException, ClassNotFoundException {
        File file = new File("leiloes.data");
        if (file.exists()) {
            this.leiloes = (List<Leilao>)(Serializador.deserialize("leiloes.data"));
            for (Leilao leilao : leiloes) {
                if (leilao.getId() > Leilao.NUM) {
                    Leilao.NUM = leilao.getId();
                }
            }
        }
    }

    public void lerFicheiroLicitadores() throws IOException, ClassNotFoundException {
        File file = new File("licitadores.data");
        if (file.exists()) {
            this.licitadores = (ArrayList<Licitador>)(Serializador.deserialize("licitadores.data"));
            for (Licitador licitador : licitadores) {
                licitador.desconectar();
            }
        }
    }

    public synchronized void atualizarFicheiroLeiloes() throws IOException {
        Serializador.serialize(leiloes, "leiloes.data");
    }

    public synchronized void atualizarFicheiroLicitadores() throws IOException {
        Serializador.serialize(licitadores, "licitadores.data");
    }

    public void verificarLeiloes() {
        for (int i = 0; i < this.leiloes.size(); i++){
            if (leiloes.get(i).hasFinished() && !leiloes.get(i).terminado()) {
                leiloes.get(i).terminar();
                final int j = i;
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            fecharLeiloes(leiloes.get(j));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
                try {
                    atualizarFicheiroLeiloes();
                    atualizarFicheiroLicitadores();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void fecharLeiloes(Leilao leilao) throws Exception {
        if (leilao.temLicitacoes()) {
            for (Licitador licitador : this.licitadores) {
                if (licitador.estaConectado()) {
                    if (leilao.getParticipantes().contains(licitador.getUsername())) {
                        if (licitador.getUsername().equals(leilao.getMaiorLicitacao().getUsername())) {
                            enviarNotificacoes("Parabéns! Foi o vencedor do leilão com o ID " + leilao.getId() + " no valor de " + leilao.getMaiorLicitacao().getQuantia() + "euros.", licitador.getAddress());
                        }
                        else {
                            enviarNotificacoes("O leilão com o ID " + leilao.getId() + " no qual realizou licitações já fechou, infelizmente você não foi o vencedor.", licitador.getAddress());
                        }
                    }
                    if (leilao.getAutor().equals(licitador.getUsername())) {
                        licitador.adicionarDinheiro(leilao.getMaiorLicitacao().getQuantia());
                        enviarNotificacoes("O bem presente no leilão com o ID " + leilao.getId() + " foi vendido à pessoa " + leilao.getMaiorLicitacao().getUsername() + "com o valor " + leilao.getMaiorLicitacao().getQuantia() + "euros.", licitador.getAddress());
                    }
                }
            }
        }
        else {
            for (Licitador licitador : this.licitadores) {
                if (licitador.estaConectado() && licitador.getUsername().equals(leilao.getAutor())) {
                    enviarNotificacoes("Lamentamos, mas o seu leilão com o ID " + leilao.getId() + " fechou sem qualquer licitacão.", licitador.getAddress());
                    break;
                }
            }
        }
    }
}
