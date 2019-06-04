package leilao.regulador;

import leilao.*;
import leilao.licitador.Licitador;
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
    private Map<String, Licitador> licitadores;
    private Map<Integer, Leilao> leiloes;

    public Servidor(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        licitadores = new HashMap<String, Licitador>();
        leiloes = new HashMap<Integer, Leilao>();
    }

    public void start() throws IOException {
        System.out.println("Servidor inicializado");
        Thread threadLerLeiloes = new Thread(new Runnable() {
            public void run() {
                try {
                    lerFicheiroLeiloes();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        threadLerLeiloes.start();
        try {
            lerFicheiroLicitadores();
        } catch (Exception e) {
            e.printStackTrace();
        }
/*        Thread threadVerificarLeiloes = new Thread(new Runnable() {
            public void run() {
                verificarLeiloes();
            }
        });
        threadVerificarLeiloes.start();*/
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
            atualizarFicheiroLicitadores();//TODO garantir que continua a funcionar aqui
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

    public void reendirecionarPedido(Socket socket) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
        //ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
        BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //Pedido pedido;
        String [] pedido;
        do {
            //pedido = (Pedido)input.readObject();
            pedido = input.readLine().split(":");
            switch (Integer.parseInt(pedido[1])) {
                case Pedido.QUIT:
                    enviarNotificacoes("quit", socket);
                    System.out.println("O user " + pedido[0] + " desconectou-se");
                    break;
                case Pedido.AUTENTICACAO:
                    responderAutenticacao(new Autenticacao(pedido[0], pedido[2]), socket);
                    break;
                case Pedido.LICITACAO:
                    responderLicitacao(new Licitacao(pedido[0], Double.parseDouble(pedido[2]), Integer.parseInt(pedido[3])));
                    break;
                case Pedido.CRIAR_LEILAO:
                    responderCriarLeilao(new PedidoCriarLeilao(pedido[0], pedido[2], new Date(Integer.parseInt(pedido[3]), Integer.parseInt(pedido[4]), Integer.parseInt(pedido[5])), Double.parseDouble(pedido[6])));
                    break;
                case Pedido.LISTAR_LEILAO:
                    responderListaLeiloes(new Pedido(pedido[0], Integer.parseInt(pedido[1])));
                    break;
                case Pedido.PLAFOND:
                    responderPlafond(new Pedido(pedido[0], Integer.parseInt(pedido[1])));
                    break;
                default:
                    enviarNotificacoes("Comando inválido", socket);
            }
        } while (Integer.parseInt(pedido[1]) != 0);
    }

    public void responderPlafond(Pedido pedido) throws IOException {
        enviarNotificacoes("O seu plafond atual é de " + Double.toString(licitadores.get(pedido.getUsername()).getPlafond()) + " euros.", licitadores.get(pedido.getUsername()).getSocket());
    }

    public void responderAutenticacao(Autenticacao pedido, Socket socket) throws IOException, NoSuchAlgorithmException {
        if  (licitadores.containsKey(pedido.getUsername()) && licitadores.get(pedido.getUsername()).conectar(pedido.getPassword(), socket)) {
            enviarNotificacoes("Utilizador verificado", licitadores.get(pedido.getUsername()).getSocket());
            //TODO atualizar licitadores
        }
        else {
            enviarNotificacoes("As credenciais estão incorretas", socket);
        }
    }

    public synchronized void responderCriarLeilao(PedidoCriarLeilao pedido) throws IOException {
        Leilao leilao = new Leilao(this.licitadores.get(pedido.getUsername()), pedido.getObjeto(), pedido.getValorInicial(), pedido.getDate());
        leiloes.put(leilao.getId(), leilao);
        //TODO atualizar ficheiro de leilao
        for (Licitador licitador : new ArrayList<Licitador>(this.licitadores.values())) {
            if (!licitador.getUsername().equals(pedido.getUsername()) && licitador.estaConectado()) {
                enviarNotificacoes("Há um novo leilão disponível, queira consultar os leilões disponíveis." , licitador.getSocket());
            }
            else if (licitador.estaConectado()) {
                enviarNotificacoes("O seu leilão foi criado com sucesso com ID " + leilao.getId() + "." , licitador.getSocket());
            }
        }
    }

    public synchronized void responderLicitacao(Licitacao pedido) throws IOException {
        if (leiloes.containsKey(pedido.getIdLeilao())) {
            if (leiloes.get(pedido.getIdLeilao()).temLicitacoes()) {
                if (pedido.getQuantia() > leiloes.get(pedido.getIdLeilao()).getMaiorLicitacao().getQuantia()) {
                    if (licitadores.get(pedido.getUsername()).retirarDinheiro(pedido.getQuantia())) {
                        licitadores.get(leiloes.get(pedido.getIdLeilao()).getMaiorLicitacao().getUsername()).adicionarDinheiro(leiloes.get(pedido.getIdLeilao()).getMaiorLicitacao().getQuantia());
                        leiloes.get(pedido.getIdLeilao()).fazerLicitacao(pedido);
                        //TODO atualizar os dois ficheiros
                        enviarNotificacoes("A sua licitação foi aceite.", licitadores.get(pedido.getUsername()).getSocket());
                        HashSet<String> licitadoresDoLeilao = new HashSet<String>();
                        for (Licitacao licitacao : leiloes.get(pedido.getIdLeilao()).getLicitacoes()) {
                            if (!licitacao.getUsername().equals(pedido.getUsername()) && !licitadoresDoLeilao.contains(licitacao.getUsername())) {
                                enviarNotificacoes("Foi recebida uma nova licitação no leilão com ID " + pedido.getIdLeilao() + ".", licitadores.get(licitacao.getUsername()).getSocket());
                                licitadoresDoLeilao.add(licitacao.getUsername());
                            }
                        }
                    }
                    else {
                        enviarNotificacoes("A sua solicitação não foi aceite, o valor da sua proposta é superior ao seu plafond.", licitadores.get(pedido.getUsername()).getSocket());
                    }
                } else {
                    enviarNotificacoes("A sua licitação não foi aceite, o valor proposto não é superior ao máximo atual.", licitadores.get(pedido.getUsername()).getSocket());
                }
            }
            else {
                if (pedido.getQuantia() >= leiloes.get(pedido.getIdLeilao()).getValorInicial()) {
                    if (licitadores.get(pedido.getUsername()).retirarDinheiro(pedido.getQuantia())) {
                        leiloes.get(pedido.getIdLeilao()).fazerLicitacao(pedido);
                        //TODO atualizar os dois ficheiros
                        enviarNotificacoes("A sua licitação foi aceite.", licitadores.get(pedido.getUsername()).getSocket());
                        HashSet<String> licitadoresDoLeilao = new HashSet<String>();
                        for (Licitacao licitacao : leiloes.get(pedido.getIdLeilao()).getLicitacoes()) {
                            if (!licitacao.getUsername().equals(pedido.getUsername()) && !licitadoresDoLeilao.contains(licitacao.getUsername())) {
                                enviarNotificacoes("Foi recebida uma nova licitação no leilão com ID " + pedido.getIdLeilao() + ".", licitadores.get(licitacao.getUsername()).getSocket());
                                licitadoresDoLeilao.add(licitacao.getUsername());
                            }
                        }
                    }
                    else {
                        enviarNotificacoes("A sua solicitação não foi aceite, o valor da sua proposta é superior ao seu plafond.", licitadores.get(pedido.getUsername()).getSocket());
                    }
                } else {
                    enviarNotificacoes("A sua licitação não foi aceite, o valor proposto não é superior ao máximo atual.", licitadores.get(pedido.getUsername()).getSocket());
                }
            }
        }
        else  {
            enviarNotificacoes("O leilão com o ID " + pedido.getIdLeilao() + " não existe ou já não está disponível.", licitadores.get(pedido.getUsername()).getSocket());
        }
    }

    public void responderListaLeiloes(Pedido pedido) throws IOException {
        String mensagem = "Plafond disponivel: " + licitadores.get(pedido.getUsername()).getPlafond() + "\n---------------------------------------------------------------";
        for (Leilao leilao : new ArrayList<Leilao>(this.leiloes.values())) {
            mensagem += "\n" + leilao;
        }
        enviarNotificacoes(mensagem, licitadores.get(pedido.getUsername()).getSocket());
    }

    public void lerFicheiroLeiloes() throws IOException, ClassNotFoundException {
        File file = new File("leiloes.data");
        if (file.exists()) {
            this.leiloes = (HashMap<Integer, Leilao>)(Serializador.deserialize("leiloes.data"));
            for (Leilao leilao : new ArrayList<Leilao>(this.leiloes.values())) {
                if (leilao.getId() > Leilao.NUM) {
                    Leilao.NUM = leilao.getId();
                }
            }
        }
    }

    public void lerFicheiroLicitadores() throws IOException, ClassNotFoundException {
        File file = new File("licitadores.data");
        if (file.exists()) {
            this.licitadores = (HashMap<String, Licitador>)(Serializador.deserialize("licitadores.data"));
            for (Licitador licitador : new ArrayList<Licitador>(licitadores.values())) {
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

    public synchronized void enviarNotificacoes(String mensagem, Socket socket) throws IOException {
        InetAddress address = socket.getInetAddress();
        int port = socket.getLocalPort();
        DatagramPacket pacote = new DatagramPacket(mensagem.getBytes(), mensagem.getBytes().length, address, port);
        DatagramSocket output = new DatagramSocket();
        output.send(pacote);
    }

/*    public void verificarLeiloes() {
        while (true) {
            for (final Integer leilaoId : new ArrayList<Integer>(this.leiloes.keySet())) {
                if (leiloes.get(leilaoId).hasFinished()) {
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            try {
                                fecharLeiloes(leiloes.get(leilaoId));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    thread.start();
                    this.leiloes.remove(leilaoId);
                    //TODO atualizar os dois ficheiros
                }
            }
        }
    }

    public void fecharLeiloes(Leilao leilao) throws IOException {
        if (leilao.temLicitacoes()) {
            licitadores.get(leilao.getAutor().getUsername()).adicionarDinheiro(leilao.getMaiorLicitacao().getQuantia());
            enviarNotificacoes("O bem presente no leilão com o ID " + leilao.getId() + " foi vendido à pessoa " + leilao.getMaiorLicitacao().getUsername() + " com o valor " + leilao.getMaiorLicitacao().getQuantia() + " de euros.", licitadores.get(leilao.getAutor().getUsername()).getSocket());
            enviarNotificacoes("Parabéns! Foi o vencedor do leilão com o ID " + leilao.getId() + " no valor de " + leilao.getMaiorLicitacao().getQuantia() + " euros.", licitadores.get(leilao.getMaiorLicitacao().getUsername()).getSocket());
            Set<String> licitadoresDoLeilao = new HashSet<String>();
            for (Licitacao licitacao : leilao.getLicitacoes()) {
                if (!licitacao.equals(leilao.getAutor().getUsername()) && !licitacao.getUsername().equals(leilao.getMaiorLicitacao().getUsername()) && !licitadoresDoLeilao.contains(licitacao.getUsername())) {
                    licitadoresDoLeilao.add(licitacao.getUsername());
                    enviarNotificacoes("O leilão com o ID " + leilao.getId() + " no qual realizou licitações já fechou, infelizmente você não foi o vencedor.", licitadores.get(licitacao.getUsername()).getSocket());
                }
            }
        }
        else {
            enviarNotificacoes("Lamentamos, mas o seu leilão com o ID " + leilao.getId() + " fechou sem qualquer licitação.", licitadores.get(leilao.getAutor().getUsername()).getSocket());
        }
    }*/
}
