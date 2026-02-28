package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.*;

public class ConnectionFactory {
    // Banco local
    private static final String URL_BASE = "jdbc:mysql://localhost:3306/";
    // Variáveis de ambiente 
    private static final String USER = System.getenv("db_user");
    private static final String PASS = System.getenv("db_pass");
    
    // Método que cria o banco, caso ainda não exista
    public static void criarBanco() {
        try (Connection conn = DriverManager.getConnection(
                URL_BASE, USER, PASS); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS loja_games;");
            stmt.execute("USE loja_games;");
            stmt.execute("CREATE TABLE IF NOT EXISTS jogo (id INT AUTO_INCREMENT PRIMARY KEY, titulo VARCHAR(120) NOT NULL, plataforma VARCHAR(50) NOT NULL, preco DECIMAL(10,2) NOT NULL, imagem_path VARCHAR(255));");
     } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar banco" + e.getMessage());
        }
    }
    
    private static final String URL_FINAL = "jdbc:mysql://localhost:3306/loja_games";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL_FINAL, USER, PASS);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conectar no MySQL: " + e.getMessage());
        }
    }
}