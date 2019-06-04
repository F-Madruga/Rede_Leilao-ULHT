package leilao.licitador;

import java.io.Serializable;
import java.net.Socket;

public class Licitador implements Serializable {

    private String username;
    private String password;
    private double plafond;
    private boolean conectado;
    private Socket socket;

    public Licitador(String username, String password, double plafond) {
        this.username = username;
        this.password = password;
        this.plafond = plafond;
        this.conectado = false;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public double getPlafond() {
        return plafond;
    }

    public boolean estaConectado() {
        return conectado;
    }

    public boolean conectar(String password, Socket socket) {
        if (this.password.equals(password)) {
            conectado = true;
            this.socket = socket;
            return true;
        }
        return false;
    }

    public Socket getSocket() {
        return socket;
    }

    public void desconectar() {
        this.conectado = false;
    }

    @Override
    public boolean equals(Object obj) {
        Licitador outroLicitador = (Licitador) obj;
        return outroLicitador.getUsername().equals(this.username);
    }

    public synchronized boolean retirarDinheiro(double dinheiro) {
        if (this.plafond - dinheiro >= 0) {
            this.plafond -= dinheiro;
            return true;
        }
        return false;
    }

    public synchronized void adicionarDinheiro(double dinheiro) {
        this.plafond += dinheiro;
    }
}

