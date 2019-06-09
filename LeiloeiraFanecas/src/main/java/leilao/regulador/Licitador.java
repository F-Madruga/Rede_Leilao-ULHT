package leilao.regulador;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

public class Licitador implements Serializable {

    private String username;
    private String password;
    private double plafond;
    private boolean conectado;
    private String address;

    public Licitador(String username, String password, double plafond) {
        this.username = username;
        this.password = password;
        this.plafond = plafond;
        this.conectado = false;
    }

    public String getUsername() {
        return username;
    }

    public double getPlafond() {
        return plafond;
    }

    public String getAddress() {
        return address;
    }

    public boolean estaConectado() {
        return this.conectado;
    }

    public boolean autenticar(String username, String password) throws NoSuchAlgorithmException {
        return this.username.equals(username) && SHA256.generate(password.getBytes()).equals(this.password) && !conectado;
    }

    public synchronized void conectar(String address) {
        this.address = address;
        this.conectado = true;
    }

    public synchronized void desconectar() {
        this.address = "";
        this.conectado = false;
    }

    public boolean temQuantia(double dinheiro) {
        return this.plafond >= dinheiro;
    }

    public synchronized void retirarDinheiro(double dinheiro) {
        this.plafond -= dinheiro;
    }

    public synchronized void adicionarDinheiro(double dinheiro) {
        this.plafond += dinheiro;
    }

    @Override
    public boolean equals(Object obj) {
        return ((Licitador) obj).getUsername().equals(this.username);
    }

    @Override
    public String toString() {
        return this.getUsername() + " " + this.plafond + " " + this.address + " " + this.password;
    }
}
