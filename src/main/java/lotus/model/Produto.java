package lotus.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tb_produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nm_produto")
    private String nome;

    @Column(name = "ds_descricao")
    private String descricao;

    @Column(name = "cd_preco")
    private BigDecimal preco;

    @Column(name = "tamanho_produto")
    private String tamanho;

    @Column(name = "nm_categoria")
    private String categoria;

    @Column(name = "url_imagem") // ou "nm_imagem", conforme você quiser usar
    private String imagem;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Produto() {}

    // --- GETTERS E SETTERS COMPLETOS (Obrigatórios!) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public String getTamanho() { return tamanho; }
    public void setTamanho(String tamanho) { this.tamanho = tamanho; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getImagem() { return imagem; }
    public void setImagem(String imagem) { this.imagem = imagem; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}