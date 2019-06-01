package leilao.regulador;

import leilao.*;
import leilao.licitador.Licitador;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Servidor {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private Map<String, Licitador> licitadores;
    private List<Leilao> leiloes;

    public Servidor(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        licitadores = new HashMap<String, Licitador>();
        leiloes = new ArrayList<Leilao>();
    }

    public void start() throws IOException {
        System.out.println("Servidor inicializado");
        Thread threadLerLeiloes = new Thread(new Runnable() {
            public void run() {
                //TODO usar função ler leilao
            }
        });
        threadLerLeiloes.start();
        //TODO usar função ler licitadores (não por numa thread
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
        }
        else {
            enviarNotificacoes("As credenciais estão incorretas", socket);
        }
    }

    public synchronized void responderCriarLeilao(PedidoCriarLeilao pedido) throws IOException {
        Leilao leilao = new Leilao(this.licitadores.get(pedido.getUsername()), pedido.getObjeto(), pedido.getValorInicial(), pedido.getDate());
        leiloes.add(leilao);
        //TODO chamar função atualizarLeiloes
        for (Licitador licitador : new ArrayList<Licitador>(this.licitadores.values())) {
            if (!licitador.getUsername().equals(pedido.getUsername()) && licitador.estaConectado()) {
                enviarNotificacoes("Há um novo leilão disponível, queira consultar os leilões disponíveis." , licitador.getSocket());
            }
            else if (licitador.estaConectado()) {
                enviarNotificacoes("O seu leilão foi criado com sucesso com ID " + leilao.getId() + "." , licitador.getSocket());
            }
        }
    }

    public void responderLicitacao(Licitacao pedido) throws IOException {
        enviarNotificacoes("Licitar ainda não está disponivel", licitadores.get(pedido.getUsername()).getSocket());
        /*TODO
            - utiliza o metodo licitar do leilao que devolve um boolean se a licitação for aceite
            - tens a função equals para compara os leilões
            - o pedido tem metodo get para receber o usename do licitador, podes usa-lo para ir ao hasmap licitadores buscar o licitador
            - tens de chamar a função atualizar licitadores e a função atualizar leiloes*/
    }

    public void responderListaLeiloes(Pedido pedido) throws IOException {
        enviarNotificacoes("Listar leilões ainda não está disponivel", licitadores.get(pedido.getUsername()).getSocket());
        /*TODO
            - usa o toString do Leilão que já está como é suposto para a resposta
            - o pedido tem metodo get para receber o usename do licitador, podes usa-lo para ir ao hasmap licitadores buscar o licitador*/
    }

    public void lerFicheiroLeiloes() {
        /*TODO
            - tens de implementar o serializador
            - tens de por a class que vais serializar e deserializar com o implements Serializeble*/
    }

    public void lerFicheiroLicitadores() {
        /*TODO
            - tens de implementar o serializador
            - tens de por a class que vais serializar e deserializar com o implements Serializeble*/
    }

    public void atualizarFicheiroLeiloes() {
        /*TODO
            - este metod pega na lista de leiloes e escreve-a para ficheiro (serielize(list(leiloes)))
            - tens de implementar o serializador
            - tens de por a class que vais serializar e deserializar com o implements Serializeble*/
    }

    public void atualizarFicheiroLicitadores() {
        /*TODO
            - este metod pega na lista de leiloes e escreve-a para ficheiro (serielize(map(licitadores)))
            - tens de implementar o serializador
            - tens de por a class que vais serializar e deserializar com o implements Serializeble*/
    }

    public synchronized void enviarNotificacoes(String mensagem, Socket socket) throws IOException {
        InetAddress address = socket.getInetAddress();
        int port = socket.getLocalPort();
        System.out.println(address);
        System.out.println(port);
        DatagramPacket pacote = new DatagramPacket(mensagem.getBytes(), mensagem.getBytes().length, address, port);
        DatagramSocket output = new DatagramSocket();
        output.send(pacote);
    }

    public void verificarLeiloes() {
        while (true) {
            for (final Leilao leilao : this.leiloes) {
                if (leilao.hasFinished()) {
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
                    this.leiloes.remove(leilao);
                    //TODO chamar função atualizar leilao e atualizar licitador
                }
            }
        }
    }

    public void fecharLeiloes(Leilao leilao) throws IOException {
        if (leilao.getMaiorLicitacao().getUsername().equals(leilao.getAutor().getUsername()) && licitadores.get(leilao.getAutor().getUsername()).estaConectado()) {
            enviarNotificacoes("Lamentamos, mas o seu leilão com o ID " + leilao.getId() + " fechou sem qualquer licitacão.", this.licitadores.get(leilao.getAutor().getUsername()).getSocket());
        }
        else {
            if (licitadores.get(leilao.getMaiorLicitacao().getUsername()).estaConectado()) {
                enviarNotificacoes("Parabéns! Foi o vencedor do leilão com o ID " + leilao.getId() + " no valor de " + leilao.getMaiorLicitacao().getQuantia() + " euros.", this.licitadores.get(leilao.getMaiorLicitacao().getUsername()).getSocket());
            }
            if (licitadores.get(leilao.getAutor().getUsername()).estaConectado()) {
                enviarNotificacoes("O bem presente no leilão com o ID " + leilao.getId() +" foi vendido à pessoa "+ leilao.getMaiorLicitacao().getUsername() + " com o valor " + leilao.getMaiorLicitacao().getQuantia() +" de euros.", leilao.getAutor().getSocket());
            }
        }
        for (Licitador licitador : leilao.getLicitadores()) {
            if (!licitador.getUsername().equals(leilao.getMaiorLicitacao().getUsername()) && !licitador.equals(leilao.getAutor()) && licitadores.get(licitador.getUsername()).estaConectado()) {
                enviarNotificacoes("O leilão com o ID X no qual realizou licitações já fechou, infelizmente você não foi o vencedor.", licitadores.get(licitador.getUsername()).getSocket());
            }
        }
    }
}
