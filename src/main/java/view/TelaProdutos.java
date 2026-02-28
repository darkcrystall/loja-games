package view;

// Importa o DAO (quem conversa com o MySQL)
import dao.JogoDAO;

// Importa o Model (objeto de dados)
import model.Jogo;

// Utilitário que copia a imagem para a pasta ./imagens e devolve o caminho
import util.ImageStorage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Image;
import java.io.File;
import java.util.List;
import javax.swing.table.TableColumn;

// Essa é a tela principal (View) da aplicação.
// Ela herda de JFrame: uma janela do Swing.
public class TelaProdutos extends javax.swing.JFrame {

    // Cria o DAO uma única vez para usar em toda a tela.
    // Ele executa inserir/atualizar/excluir/listar no banco.
    private final JogoDAO dao = new JogoDAO();

    // Guarda o ID selecionado na tabela:
    // null => modo "novo cadastro"
    // número => modo "edição" de um item existente
    private Integer idSelecionado = null;

    // Guarda o arquivo de imagem que o usuário escolheu no computador.
    // (ainda não foi salvo na pasta ./imagens)
    private File imagemEscolhida = null;

    // Construtor da tela (quando a janela é criada)
    public TelaProdutos() {
        initComponents();     // Monta os componentes do formulário (gerado pelo NetBeans)
        configurarTabela();   // Configura clique/seleção da tabela
        recarregarTabela();   // Carrega dados do banco para a JTable
        novo();               // Deixa tudo limpo para cadastrar um produto novo
        // torna a coluna id não vísivel para o usuário
        TableColumn coluna = tabela.getColumnModel().getColumn(0);
        tabela.getColumnModel().removeColumn(coluna);
    }

    // ===================== LÓGICA (CRUD + IMAGEM) =====================

    private void configurarTabela() {
        // Garante que só um item por vez pode ser selecionado.
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Listener: dispara quando o usuário seleciona uma linha na tabela.
        tabela.getSelectionModel().addListSelectionListener(e -> {

            // Evita disparar duas vezes (um evento "ajustando" e outro final).
            if (!e.getValueIsAdjusting()) {

                // Pega a linha selecionada
                int row = tabela.getSelectedRow();

                // Se realmente existe uma linha selecionada...
                if (row >= 0) {

                    // Pega o ID (coluna 0) da linha
                    int id = (int) tabela.getModel().getValueAt(row, 0);

                    // Carrega esse jogo nos campos para editar
                    carregarParaEdicao(id);
                }
            }
        });
    }

    private void carregarParaEdicao(int id) {
        // Busca o jogo no banco pelo ID
        Jogo j = dao.buscarPorId(id);

        // Se não encontrou, não faz nada
        if (j == null) return;

        // Marca que agora estamos editando (não é mais "novo")
        idSelecionado = j.getId();

        // Preenche os campos do formulário
        txtTitulo.setText(j.getTitulo());
        txtPlataforma.setText(j.getPlataforma());
        txtPreco.setText(String.valueOf(j.getPreco()));

        // Mostra o caminho da imagem (ou mensagem se não tiver)
        txtImagem.setText(j.getImagemPath() == null ? "Nenhuma imagem" : j.getImagemPath());

        // Muito importante:
        // Ao carregar para edição, não obrigamos escolher imagem de novo.
        imagemEscolhida = null;

        // Mostra a imagem do produto no preview
        mostrarImagem(j.getImagemPath(), false);
    }

    private void novo() {
        // Volta ao modo "novo cadastro"
        idSelecionado = null;

        // Esquece a imagem escolhida no PC
        imagemEscolhida = null;

        // Limpa campos
        txtTitulo.setText("");
        txtPlataforma.setText("");
        txtPreco.setText("");
        txtImagem.setText("Nenhuma imagem");

        // Limpa preview
        lblCapa.setIcon(null);
        lblCapa.setText("Selecione um item");

        // Remove seleção da tabela
        tabela.clearSelection();
    }

    private void escolherImagem() {
        // Abre janela do Windows para escolher arquivo
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Escolha a imagem do produto");

        // Mostra a janela
        int result = chooser.showOpenDialog(this);

        // Se o usuário clicou em "Abrir"
        if (result == JFileChooser.APPROVE_OPTION) {

            // Salva o arquivo escolhido
            imagemEscolhida = chooser.getSelectedFile();

            // Mostra preview imediatamente usando o caminho absoluto do Windows
            mostrarImagem(imagemEscolhida.getAbsolutePath(), true);

            // Informa no campo que a imagem ainda será copiada quando salvar
            txtImagem.setText("Será copiada ao salvar (./imagens/)");
        }
    }

    private void salvarOuAtualizar() {

        // Validação: impede salvar campos vazios
        if (txtTitulo.getText().isBlank() || txtPlataforma.getText().isBlank() || txtPreco.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Preencha título, plataforma e preço.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Converte o preço do texto para double
        double preco;
        try {
            // Aceita "199,90" e transforma em "199.90"
            preco = Double.parseDouble(txtPreco.getText().replace(",", "."));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Preço inválido! Ex: 199.90", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Caminho final que será gravado no banco
        String caminhoImagem = null;

        // Se o usuário escolheu uma imagem nova agora...
        if (imagemEscolhida != null) {
            // Copia para ./imagens e devolve o caminho relativo
            caminhoImagem = ImageStorage.salvarImagem(imagemEscolhida);

        // Senão, se estamos editando um item existente...
        } else if (idSelecionado != null) {

            // Mantém a imagem atual do banco (não perde ao editar)
            Jogo atual = dao.buscarPorId(idSelecionado);
            if (atual != null) caminhoImagem = atual.getImagemPath();
        }

        // Monta o objeto Jogo com os dados do form
        Jogo j = new Jogo(
                txtTitulo.getText().trim(),
                txtPlataforma.getText().trim(),
                preco,
                caminhoImagem
        );

        // Se não tem ID selecionado => é cadastro (INSERT)
        if (idSelecionado == null) {
            dao.inserir(j);
            JOptionPane.showMessageDialog(this, "Produto cadastrado!", "OK", JOptionPane.INFORMATION_MESSAGE);

        // Se tem ID => é edição (UPDATE)
        } else {
            j.setId(idSelecionado);
            dao.atualizar(j);
            JOptionPane.showMessageDialog(this, "Produto atualizado!", "OK", JOptionPane.INFORMATION_MESSAGE);
        }

        // Atualiza a tabela e limpa o form
        recarregarTabela();
        novo();
    }

    private void excluir() {
        // Se não selecionou nada, não dá pra excluir
        if (idSelecionado == null) {
            JOptionPane.showMessageDialog(this, "Selecione um item para excluir.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirmação para evitar exclusões acidentais
        int confirm = JOptionPane.showConfirmDialog(this, "Excluir este produto?", "Confirmar", JOptionPane.YES_NO_OPTION);

        // Se confirmou...
        if (confirm == JOptionPane.YES_OPTION) {
            dao.excluir(idSelecionado);
            JOptionPane.showMessageDialog(this, "Produto excluído!", "OK", JOptionPane.INFORMATION_MESSAGE);
            recarregarTabela();
            novo();
        }
    }

    private void recarregarTabela() {
        // Pega o model da JTable (onde ficam as linhas)
        DefaultTableModel m = (DefaultTableModel) tabela.getModel();

        // Limpa todas as linhas antes de recarregar
        m.setRowCount(0);

        // Busca lista do banco
        List<Jogo> lista = dao.listar();

        // Adiciona cada jogo como uma nova linha da tabela
        for (Jogo j : lista) {
            m.addRow(new Object[]{ j.getId(), j.getTitulo(), j.getPlataforma(), j.getPreco(), j.getImagemPath() });
        }
    }

    private void mostrarImagem(String caminho, boolean caminhoAbsoluto) {
        try {
            // Se não tem caminho, mostra texto
            if (caminho == null || caminho.isBlank()) {
                lblCapa.setIcon(null);
                lblCapa.setText("Sem imagem");
                return;
            }

            // Cria um ícone usando o caminho recebido
            ImageIcon icon = new ImageIcon(caminho);

            // Redimensiona a imagem para caber no preview
            Image img = icon.getImage().getScaledInstance(280, 280, Image.SCALE_SMOOTH);
            // Troca o texto pelo ícone
            lblCapa.setText("");
            lblCapa.setIcon(new ImageIcon(img));

        } catch (Exception e) {
            // Se deu erro (arquivo não encontrado, etc.)
            lblCapa.setIcon(null);
            lblCapa.setText("Erro ao carregar imagem");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        componentMoverUtil1 = new com.k33ptoo.utils.ComponentMoverUtil();
        app1 = new com.k33ptoo.App();
        kGradientPanel1 = new com.k33ptoo.components.KGradientPanel();
        jLabel1 = new javax.swing.JLabel();
        painelForm = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        txtTitulo = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtPlataforma = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtPreco = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtImagem = new javax.swing.JTextField();
        btnNovo = new javax.swing.JButton();
        btnSalvar = new javax.swing.JButton();
        btnExcluir = new javax.swing.JButton();
        btnEscolherImagem = new javax.swing.JButton();
        painelTabela = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabela = new javax.swing.JTable();
        painelView = new javax.swing.JPanel();
        lblCapa = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Loja de Games");
        setResizable(false);

        kGradientPanel1.setkEndColor(new java.awt.Color(163, 185, 230));
        kGradientPanel1.setkStartColor(new java.awt.Color(7, 22, 56));
        kGradientPanel1.setName(""); // NOI18N
        kGradientPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("DejaVu Serif", 0, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Loja de Games");
        kGradientPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 28, 1067, 43));

        painelForm.setBackground(new java.awt.Color(121, 152, 170));
        painelForm.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)), "Cadastro / Edição", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.BELOW_TOP, new java.awt.Font("DejaVu Serif", 0, 14), new java.awt.Color(255, 255, 255))); // NOI18N
        painelForm.setForeground(new java.awt.Color(255, 255, 255));

        jLabel3.setFont(new java.awt.Font("DejaVu Serif", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Plataforma");

        txtTitulo.setToolTipText("");

        jLabel4.setFont(new java.awt.Font("DejaVu Serif", 0, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Título");

        txtPlataforma.setToolTipText("");

        jLabel5.setFont(new java.awt.Font("DejaVu Serif", 0, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Imagem");

        txtPreco.setToolTipText("");

        jLabel6.setFont(new java.awt.Font("DejaVu Serif", 0, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Preço");

        txtImagem.setEditable(false);

        btnNovo.setFont(new java.awt.Font("DejaVu Serif", 0, 12)); // NOI18N
        btnNovo.setForeground(new java.awt.Color(7, 22, 56));
        btnNovo.setText("Novo");
        btnNovo.addActionListener(this::btnNovoActionPerformed);

        btnSalvar.setFont(new java.awt.Font("DejaVu Serif", 0, 12)); // NOI18N
        btnSalvar.setForeground(new java.awt.Color(7, 22, 56));
        btnSalvar.setText("Salvar /Cadastrar");
        btnSalvar.addActionListener(this::btnSalvarActionPerformed);

        btnExcluir.setFont(new java.awt.Font("DejaVu Serif", 0, 12)); // NOI18N
        btnExcluir.setForeground(new java.awt.Color(7, 22, 56));
        btnExcluir.setText("Excluir");
        btnExcluir.addActionListener(this::btnExcluirActionPerformed);

        btnEscolherImagem.setFont(new java.awt.Font("DejaVu Serif", 0, 12)); // NOI18N
        btnEscolherImagem.setForeground(new java.awt.Color(7, 22, 56));
        btnEscolherImagem.setText("Escolher Imagem");
        btnEscolherImagem.setBorderPainted(false);
        btnEscolherImagem.setFocusPainted(false);
        btnEscolherImagem.addActionListener(this::btnEscolherImagemActionPerformed);

        javax.swing.GroupLayout painelFormLayout = new javax.swing.GroupLayout(painelForm);
        painelForm.setLayout(painelFormLayout);
        painelFormLayout.setHorizontalGroup(
            painelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(painelFormLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(painelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(painelFormLayout.createSequentialGroup()
                        .addGroup(painelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtTitulo, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(painelFormLayout.createSequentialGroup()
                        .addGroup(painelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtPreco, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE)
                            .addComponent(txtPlataforma, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 259, Short.MAX_VALUE)
                            .addComponent(btnEscolherImagem, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE)
                            .addComponent(txtImagem, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18))))
            .addGroup(painelFormLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(btnNovo, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSalvar, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnExcluir, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        painelFormLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btnEscolherImagem, txtImagem, txtPlataforma, txtPreco, txtTitulo});

        painelFormLayout.setVerticalGroup(
            painelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(painelFormLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addGap(10, 10, 10)
                .addComponent(txtTitulo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPlataforma, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPreco, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnEscolherImagem)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtImagem, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 101, Short.MAX_VALUE)
                .addGroup(painelFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnNovo)
                    .addComponent(btnSalvar)
                    .addComponent(btnExcluir))
                .addGap(16, 16, 16))
        );

        kGradientPanel1.add(painelForm, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 90, -1, 420));

        painelTabela.setBackground(new java.awt.Color(121, 152, 170));
        painelTabela.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)), "Produtos Cadastrados", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.BELOW_TOP, new java.awt.Font("DejaVu Serif", 0, 14), new java.awt.Color(255, 255, 255))); // NOI18N
        painelTabela.setForeground(new java.awt.Color(255, 255, 255));

        tabela.setFont(new java.awt.Font("DejaVu Serif", 0, 12)); // NOI18N
        tabela.setForeground(new java.awt.Color(7, 22, 56));
        tabela.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID", "Titulo", "Plataforma", "Preço"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabela.setToolTipText("");
        tabela.setAutoscrolls(false);
        tabela.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        tabela.setEditingColumn(0);
        tabela.setEditingRow(0);
        tabela.setGridColor(new java.awt.Color(255, 255, 255));
        tabela.setSelectionBackground(new java.awt.Color(204, 255, 255));
        tabela.setSelectionForeground(new java.awt.Color(7, 22, 56));
        tabela.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        tabela.setShowGrid(false);
        tabela.getTableHeader().setResizingAllowed(false);
        tabela.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(tabela);
        if (tabela.getColumnModel().getColumnCount() > 0) {
            tabela.getColumnModel().getColumn(0).setResizable(false);
        }

        javax.swing.GroupLayout painelTabelaLayout = new javax.swing.GroupLayout(painelTabela);
        painelTabela.setLayout(painelTabelaLayout);
        painelTabelaLayout.setHorizontalGroup(
            painelTabelaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(painelTabelaLayout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 303, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );
        painelTabelaLayout.setVerticalGroup(
            painelTabelaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(painelTabelaLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 363, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        kGradientPanel1.add(painelTabela, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 90, -1, 420));

        painelView.setBackground(new java.awt.Color(121, 152, 170));
        painelView.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)), "Capa do Produto", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.BELOW_TOP, new java.awt.Font("DejaVu Serif", 0, 14), new java.awt.Color(255, 255, 255))); // NOI18N
        painelView.setForeground(new java.awt.Color(255, 255, 255));

        lblCapa.setFont(new java.awt.Font("DejaVu Serif", 0, 12)); // NOI18N
        lblCapa.setForeground(new java.awt.Color(255, 255, 255));
        lblCapa.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblCapa.setText("Selecione um item");
        lblCapa.setPreferredSize(new java.awt.Dimension(280, 280));

        javax.swing.GroupLayout painelViewLayout = new javax.swing.GroupLayout(painelView);
        painelView.setLayout(painelViewLayout);
        painelViewLayout.setHorizontalGroup(
            painelViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(painelViewLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(lblCapa, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(23, Short.MAX_VALUE))
        );
        painelViewLayout.setVerticalGroup(
            painelViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, painelViewLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblCapa, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(52, 52, 52))
        );

        kGradientPanel1.add(painelView, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 90, -1, 420));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(kGradientPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 1065, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(kGradientPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 540, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    // ===================== EVENTOS DOS BOTÕES =====================
    
    // Botão "Excluir" remove o item selecionado
    private void btnExcluirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExcluirActionPerformed
        excluir();
    }//GEN-LAST:event_btnExcluirActionPerformed

   // Botão "Salvar" decide entre inserir ou atualizar
    private void btnSalvarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalvarActionPerformed
        salvarOuAtualizar();
    }//GEN-LAST:event_btnSalvarActionPerformed
    // Botão "Novo" limpa o formulário
    private void btnNovoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNovoActionPerformed
        novo();
    }//GEN-LAST:event_btnNovoActionPerformed
    // Botão "Escolher imagem" abre o explorador de arquivos
    private void btnEscolherImagemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEscolherImagemActionPerformed
        escolherImagem();
    }//GEN-LAST:event_btnEscolherImagemActionPerformed
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.k33ptoo.App app1;
    private javax.swing.JButton btnEscolherImagem;
    private javax.swing.JButton btnExcluir;
    private javax.swing.JButton btnNovo;
    private javax.swing.JButton btnSalvar;
    private com.k33ptoo.utils.ComponentMoverUtil componentMoverUtil1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private com.k33ptoo.components.KGradientPanel kGradientPanel1;
    private javax.swing.JLabel lblCapa;
    private javax.swing.JPanel painelForm;
    private javax.swing.JPanel painelTabela;
    private javax.swing.JPanel painelView;
    private javax.swing.JTable tabela;
    private javax.swing.JTextField txtImagem;
    private javax.swing.JTextField txtPlataforma;
    private javax.swing.JTextField txtPreco;
    private javax.swing.JTextField txtTitulo;
    // End of variables declaration//GEN-END:variables
}
