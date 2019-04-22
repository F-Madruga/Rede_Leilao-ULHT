package leilao.licitador;

public class Licitador {

    private String username;
    private String password;
    private double plafond;
    private boolean conectado;

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

    public boolean conectar(String password) {
        if (this.password.equals(password)) {
            conectado = true;
            return true;
        }
        return false;
    }

    public void desconectar() {
        this.conectado = false;
    }

    @Override
    public boolean equals(Object obj) {
        Licitador outroLicitador = (Licitador) obj;
        return outroLicitador.getUsername().equals(this.username);
    }
}

