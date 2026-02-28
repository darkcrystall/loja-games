// Define o pacote "model".
// Aqui ficam as classes que representam dados do sistema.
package model;

// Classe Jogo representa um jogo da lojinha.
// Ela é o "modelo de dados" no padrão MVC.
public class Jogo {

    // Atributos (características do jogo).
    // private = só a própria classe acessa diretamente.
    private int id;
    private String titulo;
    private String plataforma;
    private double preco;
    private String imagemPath;

    // Construtor vazio.
    // Necessário para frameworks, DAO e criação sem dados iniciais.
    public Jogo() {}

    // Construtor com parâmetros.
    // Facilita criar um jogo já com dados.
    public Jogo(String titulo, String plataforma, double preco, String imagemPath) {
        this.titulo = titulo;
        this.plataforma = plataforma;
        this.preco = preco;
        this.imagemPath = imagemPath;
    }

    // ===== GETTERS E SETTERS =====
    // Servem para acessar e modificar atributos privados.

    public int getId() { 
        return id; 
    }

    public void setId(int id) { 
        this.id = id; 
    }

    public String getTitulo() { 
        return titulo; 
    }

    public void setTitulo(String titulo) { 
        this.titulo = titulo; 
    }

    public String getPlataforma() { 
        return plataforma; 
    }

    public void setPlataforma(String plataforma) { 
        this.plataforma = plataforma; 
    }

    public double getPreco() { 
        return preco; 
    }

    public void setPreco(double preco) { 
        this.preco = preco; 
    }

    public String getImagemPath() { 
        return imagemPath; 
    }

    public void setImagemPath(String imagemPath) { 
        this.imagemPath = imagemPath; 
    }
}