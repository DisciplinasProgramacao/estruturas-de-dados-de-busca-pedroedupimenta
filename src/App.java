import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;
import java.util.function.Function;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class App {

    static AVL<Integer, Fornecedor> arvoreFornecedores;
    static TabelaHash<Produto, Lista<Fornecedor>> fornecedoresPorProduto;

    static String nomeArquivoDados;
    
    static Scanner teclado;

    static int quantosProdutos = 0;

    static AVL<String, Produto> produtosBalanceadosPorNome;
    
    static AVL<Integer, Produto> produtosBalanceadosPorId;
    
    static TabelaHash<Produto, Lista<Pedido>> pedidosPorProduto;
        private static Lista<Produto> produtosCadastrados;
        
        static void limparTela() {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
    
        static void pausa() {
            System.out.println("Digite enter para continuar...");
            teclado.nextLine();
        }
    
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
        
        static int menu() {
            cabecalho();
            System.out.println("1 - Procurar produto, por id");
            System.out.println("2 - Gravar, em arquivo, pedidos de um produto");
            System.out.println("0 - Sair");
            System.out.print("Digite sua opção: ");
            
            try {
                return Integer.parseInt(teclado.nextLine());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        
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
            if (idProduto == null) return null;
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
                    }
                }
                pedidos.inserirFinal(pedido);
            }
            return pedidos;
        }
        
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
        
        static void pedidosDoProduto() {
            
            Produto produto = localizarProdutoID(produtosBalanceadosPorId);
            
            if (produto == null) {
                System.out.println("Operação cancelada: Produto não encontrado.");
                return;
            }
            
            Lista<Pedido> listaDePedidos;
            String nomeArquivo = "RelatorioProduto" + produto.hashCode() + ".txt";  
            
            try {
                listaDePedidos = pedidosPorProduto.pesquisar(produto);
                
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
                System.out.println("Atenção: Nenhum pedido encontrado contendo este produto.");
            }
        }
        public static <K> AVL<K, Fornecedor> lerFornecedores(
            String nomeArquivoDados,
            Function<Fornecedor, K> extratorDeChave)
    {
        AVL<K, Fornecedor> arvore = new AVL<K, Fornecedor>((a, b) -> ((Comparable<K>) a).compareTo(b));
        Random random = new Random();
    
        try {
            File arq = new File(nomeArquivoDados);
            Scanner in = new Scanner(arq, "UTF-8");
    
            int quantidade = Integer.parseInt(in.nextLine().trim());
    
            for (int i = 0; i < quantidade; i++) {
    
                if (!in.hasNextLine()) break;
                String nome = in.nextLine().trim();
    
                Fornecedor f = new Fornecedor(nome);
    
                K chave = extratorDeChave.apply(f);
                arvore.inserir(chave, f);
    
                int qtdProdutos = random.nextInt(6) + 1;
    
                for (int j = 0; j < qtdProdutos; j++) {
    
                    Produto p = ((Lista<Produto>) App.produtosCadastrados).get(
                            random.nextInt(App.produtosCadastrados.tamanho()));
    
                    f.adicionarProduto(p);
    
                    try {
                        Lista<Fornecedor> listaForns = App.fornecedoresPorProduto.pesquisar(p);
                        listaForns.inserirFinal(f);
    
                    } catch (NoSuchElementException e) {
                        Lista<Fornecedor> nova = new Lista<>();
                        nova.inserirFinal(f);
                        App.fornecedoresPorProduto.inserir(p, nova);
                    }
                }
            }
    
            in.close();
            return arvore;
    
        } catch (Exception e) {
            return new AVL<K, Fornecedor>((a, b) -> ((Comparable<K>) a).compareTo(b));
        }
    }

    public static String relatorioDeFornecedor(int documento) {
        try {
            Fornecedor f = arvoreFornecedores.pesquisar(documento);
            return f.toString();
        } catch (Exception e) {
            return "Fornecedor não encontrado.";
        }
    }

    public static void fornecedoresDoProduto(Produto p, String nomeArquivoSaida) {
        try {
            Lista<Fornecedor> lista = fornecedoresPorProduto.pesquisar(p);
    
            FileWriter fw = new FileWriter(nomeArquivoSaida);
            fw.write("RELATÓRIO DE FORNECEDORES DO PRODUTO\n");
            fw.write("Produto: " + p.getDescricao() + " (Código: " + p.getCodigo() + ")\n\n");
    
            Celula<Fornecedor> aux = lista.getPrimeiro().getProximo();
            while (aux != null) {
                fw.write(aux.getItem().getNome() +
                        " - Documento: " + aux.getItem().getDocumento() + "\n");
                aux = aux.getProximo();
            }
    
            fw.close();
            System.out.println("Relatório salvo em: " + nomeArquivoSaida);
    
        } catch (Exception e) {
            System.out.println("Nenhum fornecedor encontrado para este produto.");
        }
    }
    
    
    

    
    public static void main(String[] args) {
        teclado = new Scanner(System.in, Charset.forName("UTF-8"));
        nomeArquivoDados = "produtos.txt";
        String nomeArquivoFornecedores = "fornecedores.txt";
        
        produtosBalanceadosPorId = lerProdutos(nomeArquivoDados, Produto::hashCode);
        
        if (produtosBalanceadosPorId == null) {
            System.out.println("Erro crítico: Não foi possível ler o arquivo " + nomeArquivoDados);
            System.out.println("Verifique se o arquivo está na raiz do projeto.");
            return;
        }

        arvoreFornecedores = new AVL<Integer, Fornecedor>(Comparator.naturalOrder());
        fornecedoresPorProduto = new TabelaHash<>(1009);


        produtosBalanceadosPorNome = new AVL<>(produtosBalanceadosPorId, produto -> produto.descricao, String::compareTo);
        pedidosPorProduto = new TabelaHash<>((int)(quantosProdutos * 1.25));
        
        System.out.println("Gerando pedidos aleatórios...");
        gerarPedidos(25_000);
       
        int opcao = -1;
      
        do {
            opcao = menu();
            switch (opcao) {
                case 1 -> mostrarProduto(localizarProdutoID(produtosBalanceadosPorId));
                case 2 -> pedidosDoProduto();
                case 3 -> relatorioDeFornecedor(opcao);
                case 4 -> fornecedoresDoProduto(null, nomeArquivoDados);
                case 0 -> System.out.println("Saindo...");
                default -> System.out.println("Opção inválida!");
            }
            if (opcao != 0) pausa();
        } while(opcao != 0);       

        teclado.close();    
    }
}
