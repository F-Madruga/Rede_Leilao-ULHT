package leilao.regulador;

import leilao.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Servidor {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private int port;
    private List<Licitador> licitadores;
    private List<Leilao> leiloes;

    public Servidor(int port) throws IOException {
        this.port = port;
        serverSocket = new ServerSocket(port);
        licitadores = new ArrayList<Licitador>();
        leiloes = new ArrayList<Leilao>();
    }

    public void start() throws IOException {
        System.out.println("Servidor inicializado");
        try {
            lerFicheiroLeiloes();
            lerFicheiroLicitadores();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread threadVerificarLeiloes = new Thread(new Runnable() {
            public void run() {
                verificarLeiloes();
            }
        });
        threadVerificarLeiloes.start();
        Thread threadRegistarLicitadores = new Thread(new Runnable() {
            public void run() {
                try {
                    registarLicitadores();
                } catch (Exception e) {
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

    public void registarLicitadores() throws NoSuchAlgorithmException, IOException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String username;
            boolean usernameJaExiste = false;
            do {
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
            String plafond;
            do {
                System.out.println("Insira o seu plafond");
                plafond = scanner.nextLine();
            } while (!verificarSeDouble(plafond));
            licitadores.add(new Licitador(username, password, Double.parseDouble(plafond)));
            atualizarFicheiroLicitadores();
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
        //ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //Pedido pedido;
        String [] pedido;
        do {
            //pedido = (Pedido)input.readObject();
            pedido = input.readLine().split("#");
            switch (Integer.parseInt(pedido[1])) {
                case Pedido.QUIT:
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
        } while (Integer.parseInt(pedido[1]) != 0);
    }

    public void responderPlafond(Pedido pedido) throws IOException {
        for (Licitador licitador : licitadores) {
            if (licitador.getUsername().equals(pedido.getUsername())) {
                enviarNotificacoes("O seu plafond atual é de " + Double.toString(licitador.getPlafond()) + " euros.", licitador.getAddress());
                break;
            }
        }
    }

    public void responderAutenticacao(Autenticacao pedido, Socket socket) throws IOException, NoSuchAlgorithmException {
        boolean autenticacaoValida = false;
        for (Licitador licitador : licitadores) {
            if (licitador.getUsername().equals(pedido.getUsername()) && licitador.conectar(pedido.getPassword(), socket.getInetAddress().getHostAddress())) {
                enviarNotificacoes("Utilizador verificado", licitador.getAddress());
                autenticacaoValida = true;
                break;
            }
        }
        if (!autenticacaoValida) {
            enviarNotificacoes("As credenciais estão incorretas", socket.getInetAddress().getHostAddress());
        }
    }

    public synchronized void responderCriarLeilao(PedidoCriarLeilao pedido) throws IOException {
        for (Licitador licitador : licitadores) {
            if (licitador.getUsername().equals(pedido.getUsername())) {
                Leilao leilao = new Leilao(licitador, pedido.getObjeto(), pedido.getValorInicial(), pedido.getDateFecho());
                leiloes.add(leilao);
                enviarNotificacoes("O seu leilão foi criado com sucesso com ID " + leilao.getId() + "." , licitador.getAddress());
                break;
            }
        }
        atualizarFicheiroLeiloes();
        for (Licitador licitador : licitadores) {
            if (!licitador.getUsername().equals(pedido.getUsername()) && licitador.estaConectado()) {
                enviarNotificacoes("Há um novo leilão disponível, queira consultar os leilões disponíveis." , licitador.getAddress());
            }
        }
    }

    public synchronized void responderLicitacao(Licitacao pedido) throws IOException {
        boolean leilaoExiste = false;
        boolean maiorLicitacao = false;
        boolean temPlafond = false;
        for (Leilao leilao : leiloes) {
            if (leilao.getId() == pedido.getIdLeilao()) {
                leilaoExiste = true;
                for (Licitador licitador : licitadores) {
                    if (licitador.getUsername().equals(pedido.getUsername())) {
                        if (leilao.temLicitacoes()) {
                            if (leilao.getMaiorLicitacao().getQuantia() < pedido.getQuantia()) {
                                maiorLicitacao = true;
                                if (licitador.retirarDinheiro(pedido.getQuantia())) {
                                    temPlafond = true;
                                    for (Licitador antigoMaiorLicitador : licitadores) {
                                        if (antigoMaiorLicitador.getUsername().equals(leilao.getMaiorLicitacao().getUsername())) {
                                            antigoMaiorLicitador.adicionarDinheiro(leilao.getMaiorLicitacao().getQuantia());
                                            break;
                                        }
                                    }
                                    leilao.fazerLicitacao(pedido);
                                }
                            }
                        }
                        else {
                            if (leilao.getValorInicial() <= pedido.getQuantia()) {
                                maiorLicitacao = true;
                                if (licitador.retirarDinheiro(pedido.getQuantia())) {
                                    temPlafond = true;
                                    leilao.fazerLicitacao(pedido);
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
        if (leilaoExiste && maiorLicitacao && temPlafond) {
            atualizarFicheiroLeiloes();
            atualizarFicheiroLicitadores();
            for (Licitador licitador : licitadores) {
                if (licitador.getUsername().equals(pedido.getUsername())) {
                    enviarNotificacoes("A sua licitação foi aceite.", licitador.getAddress());
                }
                else {
                    enviarNotificacoes("Foi recebida uma nova licitação no leilão com ID " + pedido.getIdLeilao() + ".", licitador.getAddress());
                }
            }
        }
        else if (!leilaoExiste) {
            for (Licitador licitador : licitadores) {
                if (licitador.getUsername().equals(pedido.getUsername())) {
                    enviarNotificacoes("O leilão com o ID " + pedido.getIdLeilao() + " não existe ou já não está disponível.", licitador.getAddress());
                    break;
                }
            }
        }
        else if (!maiorLicitacao) {
            for (Licitador licitador : licitadores) {
                if (licitador.getUsername().equals(pedido.getUsername())) {
                    enviarNotificacoes("A sua licitação não foi aceite, o valor proposto não é superior ao máximo atual.", licitador.getAddress());
                    break;
                }
            }
        }
        else {
            for (Licitador licitador : licitadores) {
                if (licitador.getUsername().equals(pedido.getUsername())) {
                    enviarNotificacoes("A sua solicitação não foi aceite, o valor da sua proposta é superior ao seu plafond.", licitador.getAddress());
                    break;
                }
            }
        }
    }

    public void responderListaLeiloes(Pedido pedido) throws IOException {
        for (Licitador licitador : licitadores) {
            if (licitador.getUsername().equals(pedido.getUsername())) {
                String mensagem = "Plafond disponivel: " + licitador.getPlafond() + "\n---------------------------------------------------------------";
                for (Leilao leilao :leiloes) {
                    mensagem += "\n" + leilao;
                }
                enviarNotificacoes(mensagem, licitador.getAddress());
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

    public synchronized void enviarNotificacoes(String mensagem, String address) throws IOException {
            DatagramPacket pacote = new DatagramPacket(mensagem.getBytes(), mensagem.getBytes().length, InetAddress.getByName(address), port);
            DatagramSocket output = new DatagramSocket();
            output.send(pacote);
    }

    public void verificarLeiloes() {
        while (true) {
            for (int i = 0; i < leiloes.size(); i++) {
                if (leiloes.get(i).hasFinished()) {
                    final Leilao leilao = leiloes.remove(i);
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                fecharLeiloes(leilao);
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
    }

    public void fecharLeiloes(Leilao leilao) throws IOException {
        if (leilao.temLicitacoes()) {
            leilao.getAutor().adicionarDinheiro(leilao.getMaiorLicitacao().getQuantia());
            Set<Licitador> licitadorSet = new HashSet<Licitador>();
            for (Licitador licitador : licitadores) {
                if (leilao.getMaiorLicitacao().getUsername().equals(licitador.getUsername())) {
                    enviarNotificacoes("Parabéns! Foi o vencedor do leilão com o ID " + leilao.getId() + " no valor de " + leilao.getMaiorLicitacao().getQuantia() + "euros.", licitador.getAddress());
                }
                if (licitador.equals(leilao.getAutor())) {
                    enviarNotificacoes("O bem presente no leilão com o ID " + leilao.getId() + " foi vendido à pessoa " + leilao.getMaiorLicitacao().getUsername() + "com o valor " + leilao.getMaiorLicitacao().getQuantia() + "euros.", licitador.getAddress());
                }
                for (Licitacao licitacao : leilao.getLicitacoes()) {
                    if (!licitador.getUsername().equals(leilao.getAutor()) || !licitador.getUsername().equals(leilao.getMaiorLicitacao().getUsername())) {
                        if (!licitadorSet.contains(licitador) && licitacao.getUsername().equals(licitador.getUsername())) {
                            enviarNotificacoes("O leilão com o ID " + leilao.getId() + " no qual realizou licitações já fechou, infelizmente você não foi o vencedor.", licitador.getAddress());
                            licitadorSet.add(licitador);
                        }
                    }
                }
            }
        } else {
            enviarNotificacoes("Lamentamos, mas o seu leilão com o ID " + leilao.getId() + "fechou sem qualquer licitacão.", leilao.getAutor().getAddress());
        }
    }
}
