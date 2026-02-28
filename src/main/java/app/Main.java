// Define o pacote (pasta lógica) onde essa classe está.
// Ajuda a organizar o projeto.
package app;

// Importa classes do Swing para trabalhar com interface gráfica.
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Importa a tela principal do sistema (View no MVC).
import view.TelaProdutos;

import util.ConnectionFactory;

public class Main {

    // Método principal: é o ponto de entrada do programa Java.
    public static void main(String[] args) {
        // Chama o banco para ser criado
        ConnectionFactory.criarBanco();
        try {
            // Percorre todos os "temas visuais" (Look and Feel) disponíveis no sistema.
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {

                // Verifica se o nome do tema é "Nimbus".
                // Nimbus é um visual moderno do Swing.
                if ("Nimbus".equals(info.getName())) {

                    // Define o Nimbus como tema da aplicação.
                    UIManager.setLookAndFeel(info.getClassName());

                    // Sai do laço depois de encontrar o Nimbus.
                    break;
                }
            }

        // Caso dê erro ao mudar o tema, ele simplesmente ignora.
        // Isso evita que o programa pare por causa do visual.
        } catch (Exception ignored) {}

        // Garante que a interface gráfica rode na Thread de interface (Event Dispatch Thread).
        // Isso é uma boa prática no Swing.
        SwingUtilities.invokeLater(() ->

            // Cria a tela de produtos e a deixa visível.
            // Aqui a aplicação realmente abre para o usuário.
new TelaProdutos().setVisible(true)
        );
    }
}

// Esse Main é responsável por:
// Iniciar o programa
// Definir o visual (Nimbus)
// Abrir a tela principal  