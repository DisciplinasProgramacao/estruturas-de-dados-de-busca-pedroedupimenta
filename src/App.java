import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class App {

    /** Nome do arquivo de dados. O arquivo deve estar localizado na raiz do projeto */
    static String nomeArquivoDados;
    
    /** Scanner para leitura de dados do teclado */
    static Scanner teclado;

    /** Quantidade de produtos cadastrados atualmente na lista */
    static int quantosProdutos = 0;

    static AVL<String, Produto> produtosBalanceadosPorNome;
    
    static AVL<Integer, Produto> produtosBalanceadosPorId;
    
    static TabelaHash<Produto, Lista<Pedido>> pedidosPorProduto;
    
    static void limparTela() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** Gera um efeito de pausa na CLI. Espera por um enter para continuar */
    static void pausa() {
        System.out.println("Digite enter para continuar...");
        teclado.nextLine();
    }

    /** Cabeçalho principal da CLI do sistema */
    static void cabecalho() {
        System.out.println("AEDs II COMÉRCIO DE COISINHAS");
        System.out.println("=============================");
    }
   
    static <T extends Number> T lerOpcao(String mensagem, Class<T> classe) {
        
        T valor;
        
        System.out.println(mensagem);
        try {
            valor = classe.getConstructor(String.class).newInstance(teclado.nextLine());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException 
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            return null;
        }
        return valor;
    }
    
    /** * Imprime o menu principal, lê a opção do usuário e a retorna (int).
     * Modificado para Tarefa 4 (Robustez): Trata exceção caso usuário digite texto.
     */
    static int menu() {
        cabecalho();
        System.out.println("1 - Procurar produto, por id");
        System.out.println("2 - Gravar, em arquivo, pedidos de um produto");
        System.out.println("0 - Sair");
        System.out.print("Digite sua opção: ");
        
        try {
            return Integer.parseInt(teclado.nextLine());
        } catch (NumberFormatException e) {
            return -1; // Retorna opção inválida
        }
    }
    
    /**
     * Lê os dados de um arquivo-texto e retorna uma árvore de produtos.
     */
    static <K> AVL<K, Produto> lerProdutos(String nomeArquivoDados, Function<Produto, K> extratorDeChave) {
        
        Scanner arquivo = null;
        int numProdutos;
        String linha;
        Produto produto;
        AVL<K, Produto> produtosCadastrados;
        K chave;
        
        try {
            arquivo = new Scanner(new File(nomeArquivoDados), Charset.forName("UTF-8"));
            
            numProdutos = Integer.parseInt(arquivo.nextLine());
            produtosCadastrados = new AVL<K, Produto>();
            
            for (int i = 0; i < numProdutos; i++) {
                linha = arquivo.nextLine();
                produto = Produto.criarDoTexto(linha);
                chave = extratorDeChave.apply(produto);
                produtosCadastrados.inserir(chave, produto);
            }
            quantosProdutos = numProdutos;
            
        } catch (IOException excecaoArquivo) {
            produtosCadastrados = null;
        } finally {
            if (arquivo != null) arquivo.close();
        }
        
        return produtosCadastrados;
    }
    
    static <K> Produto localizarProduto(ABB<K, Produto> produtosCadastrados, K procurado) {
        
        Produto produto;
        
        cabecalho();
        System.out.println("Localizando um produto...");
        
        try {
            produto = produtosCadastrados.pesquisar(procurado);
        } catch (NoSuchElementException excecao) {
            produto = null;
        }
        
        System.out.println("Número de comparações realizadas: " + produtosCadastrados.getComparacoes());
        System.out.println("Tempo de processamento da pesquisa: " + produtosCadastrados.getTempo() + " ms");
        
        return produto;
    }
    
    static Produto localizarProdutoID(ABB<Integer, Produto> produtosCadastrados) {
        Integer idProduto = lerOpcao("Digite o identificador do produto desejado: ", Integer.class);
        if (idProduto == null) return null; // Robustez extra se lerOpcao falhar
        return localizarProduto(produtosCadastrados, idProduto);
    }
    
    static Produto localizarProdutoNome(ABB<String, Produto> produtosCadastrados) {
        System.out.println("Digite o nome ou a descrição do produto desejado:");
        String descricao = teclado.nextLine();
        return localizarProduto(produtosCadastrados, descricao);
    }
    
    private static void mostrarProduto(Produto produto) {
        cabecalho();
        String mensagem = "Dados inválidos para o produto ou produto não encontrado!";
        
        if (produto != null){
            mensagem = String.format("Dados do produto:\n%s", produto);
        }
        
        System.out.println(mensagem);
    }
    
    private static Lista<Pedido> gerarPedidos(int quantidade) {
        Lista<Pedido> pedidos = new Lista<>();
        Random sorteio = new Random(42);
        int quantProdutos;
        int formaDePagamento;
        for (int i = 0; i < quantidade; i++) {
            formaDePagamento = sorteio.nextInt(2) + 1;
            Pedido pedido = new Pedido(LocalDate.now(), formaDePagamento);
            quantProdutos = sorteio.nextInt(8) + 1;
            for (int j = 0; j < quantProdutos; j++) {
                int id = sorteio.nextInt(7750) + 10_000;
                try {
                    Produto produto = produtosBalanceadosPorId.pesquisar(id);
                    pedido.incluirProduto(produto);
                    inserirNaTabela(produto, pedido);
                } catch (NoSuchElementException e) {
                    // Ignora se o ID sorteado não existir na árvore (apenas robustez para o gerador)
                }
            }
            pedidos.inserirFinal(pedido);
        }
        return pedidos;
    }
    
    /**
     * Tarefa 2: Método para inserir o pedido na tabela hash mapeada pelo produto.
     * Se a lista de pedidos para aquele produto não existir, ela é criada.
     */
    private static void inserirNaTabela(Produto produto, Pedido pedido) {
        
        Lista<Pedido> listaDePedidos;
        
        try {
            listaDePedidos = pedidosPorProduto.pesquisar(produto);
        } catch (NoSuchElementException excecao) {
            listaDePedidos = new Lista<>();
            pedidosPorProduto.inserir(produto, listaDePedidos);
        }
        listaDePedidos.inserirFinal(pedido);
    }
    
    /**
     * Tarefa 3 e 4: Gera relatório em arquivo e implementa robustez.
     */
    static void pedidosDoProduto() {
        
        // Tarefa 4: Robustez - Verifica se o produto existe antes de tentar gerar relatório
        Produto produto = localizarProdutoID(produtosBalanceadosPorId);
        
        if (produto == null) {
            System.out.println("Operação cancelada: Produto não encontrado.");
            return;
        }
        
        Lista<Pedido> listaDePedidos;
        String nomeArquivo = "RelatorioProduto" + produto.hashCode() + ".txt";  
        
        try {
            // Tenta buscar os pedidos na Tabela Hash
            listaDePedidos = pedidosPorProduto.pesquisar(produto);
            
            // Gravação do arquivo usando try-with-resources para garantir o fechamento
            try (FileWriter arquivoRelatorio = new FileWriter(nomeArquivo, Charset.forName("UTF-8"))) {
                arquivoRelatorio.write("RELATÓRIO DE PEDIDOS\n");
                arquivoRelatorio.write("Produto consultado: " + produto.toString() + "\n");
                arquivoRelatorio.write("==================================================\n");
                arquivoRelatorio.write(listaDePedidos.toString() + "\n");
                
                System.out.println("Sucesso! O relatório foi salvo no arquivo: " + nomeArquivo);
            } catch(IOException excecao) {
                System.out.println("Erro ao gravar o arquivo " + nomeArquivo + ": " + excecao.getMessage());        	
            }
            
        } catch (NoSuchElementException e) {
            // Tarefa 4: Robustez - Produto existe no cadastro, mas não foi vendido em nenhum pedido
            System.out.println("Atenção: Nenhum pedido encontrado contendo este produto.");
        }
    }
    
    public static void main(String[] args) {
        teclado = new Scanner(System.in, Charset.forName("UTF-8"));
        nomeArquivoDados = "produtos.txt";
        
        produtosBalanceadosPorId = lerProdutos(nomeArquivoDados, Produto::hashCode);
        
        // Verifica se a leitura do arquivo funcionou antes de continuar
        if (produtosBalanceadosPorId == null) {
            System.out.println("Erro crítico: Não foi possível ler o arquivo " + nomeArquivoDados);
            System.out.println("Verifique se o arquivo está na raiz do projeto.");
            return;
        }

        produtosBalanceadosPorNome = new AVL<>(produtosBalanceadosPorId, produto -> produto.descricao, String::compareTo);
        pedidosPorProduto = new TabelaHash<>((int)(quantosProdutos * 1.25));
        
        System.out.println("Gerando pedidos aleatórios...");
        gerarPedidos(25_000); // Tarefa 1/2 implícita
       
        int opcao = -1;
      
        do {
            opcao = menu();
            switch (opcao) {
                case 1 -> mostrarProduto(localizarProdutoID(produtosBalanceadosPorId));
                case 2 -> pedidosDoProduto();
                case 0 -> System.out.println("Saindo...");
                default -> System.out.println("Opção inválida!");
            }
            if (opcao != 0) pausa();
        } while(opcao != 0);       

        teclado.close();    
    }
}
