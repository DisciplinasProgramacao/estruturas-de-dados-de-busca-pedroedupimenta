import java.util.Objects;

public class Fornecedor {

    private static int contadorDocumentos = 1;

    private final int documento;
    private final String nome;
    private Lista<Produto> produtos;

    public Fornecedor(String nome) {
        if (nome == null || nome.strip().split(" ").length < 2)
            throw new IllegalArgumentException("O nome deve conter pelo menos duas palavras!");

        this.nome = nome;
        this.documento = contadorDocumentos++;
        this.produtos = new Lista<>();
    }

    public int getDocumento() {
        return documento;
    }

    public String getNome() {
        return nome;
    }

    public Lista<Produto> getProdutos() {
        return produtos;
    }

    public void adicionarProduto(Produto p) {
        if (p == null)
            throw new IllegalArgumentException("Produto não pode ser nulo!");

        produtos.inserirFinal(p);
    }

    @Override
    public int hashCode() {
        return documento;
    }

    @Override
    public String toString() {
        String texto = "Fornecedor: " + nome +
                " (Documento: " + documento + ")\nProdutos:\n";

        if (produtos.vazia()) texto += "Nenhum produto registrado.\n";
        else texto += produtos.toString();

        return texto;
    }
}
