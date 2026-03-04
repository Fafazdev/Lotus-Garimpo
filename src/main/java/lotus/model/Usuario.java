package lotus.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String nome;
    private String senha;

    @Column(name = "cd_tipo")
    private Integer tipo;

    @Column(name = "dt_criacao", nullable = false)
    private LocalDate dataCriacao;

    @Column(name = "cd_telefone")
    private String telefone;

    @Column(name = "cd_cpf")
    private String cpf;

    @Column(name = "imagem")
    private String imagem;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Endereco> enderecos = new ArrayList<>();

    // Construtor padrão
    public Usuario() {}

    // --- GETTERS E SETTERS (Obrigatórios para o Thymeleaf e para sumir o erro azul) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public Integer getTipo() { return tipo; }
    public void setTipo(Integer tipo) { this.tipo = tipo; }

    public LocalDate getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDate dataCriacao) { this.dataCriacao = dataCriacao; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getImagem() { return imagem; }
    public void setImagem(String imagem) { this.imagem = imagem; }

    public List<Endereco> getEnderecos() { return enderecos; }
    public void setEnderecos(List<Endereco> enderecos) { this.enderecos = enderecos; }

    private Endereco getEnderecoPrincipal() {
        if (enderecos == null || enderecos.isEmpty()) {
            return null;
        }
        return enderecos.get(0);
    }

    private Endereco getOrCreateEnderecoPrincipal() {
        if (enderecos == null) {
            enderecos = new ArrayList<>();
        }
        if (enderecos.isEmpty()) {
            Endereco endereco = new Endereco();
            endereco.setUsuario(this);
            enderecos.add(endereco);
        }
        return enderecos.get(0);
    }

    // Métodos de conveniência para o Thymeleaf e controllers
    public String getEndereco() {
        Endereco endereco = getEnderecoPrincipal();
        return (endereco != null) ? endereco.getLogradouro() : null;
    }

    public void setEndereco(String enderecoLogradouro) {
        Endereco endereco = getEnderecoPrincipal();
        if (endereco == null) {
            return; // não cria endereço sem CEP
        }
        endereco.setLogradouro(enderecoLogradouro);
    }

    public String getNumero() {
        Endereco endereco = getEnderecoPrincipal();
        return (endereco != null) ? endereco.getNumero() : null;
    }

    public void setNumero(String numero) {
        Endereco endereco = getEnderecoPrincipal();
        if (endereco == null) {
            return;
        }
        endereco.setNumero(numero);
    }

    public String getComplemento() {
        Endereco endereco = getEnderecoPrincipal();
        return (endereco != null) ? endereco.getComplemento() : null;
    }

    public void setComplemento(String complemento) {
        Endereco endereco = getEnderecoPrincipal();
        if (endereco == null) {
            return;
        }
        endereco.setComplemento(complemento);
    }

    public String getCidade() {
        Endereco endereco = getEnderecoPrincipal();
        return (endereco != null) ? endereco.getCidade() : null;
    }

    public void setCidade(String cidade) {
        Endereco endereco = getEnderecoPrincipal();
        if (endereco == null) {
            return;
        }
        endereco.setCidade(cidade);
    }

    public String getEstado() {
        Endereco endereco = getEnderecoPrincipal();
        return (endereco != null) ? endereco.getUf() : null;
    }

    public void setEstado(String estado) {
        Endereco endereco = getEnderecoPrincipal();
        if (endereco == null) {
            return;
        }
        endereco.setUf(estado);
    }

    public String getCep() {
        Endereco endereco = getEnderecoPrincipal();
        return (endereco != null) ? endereco.getCep() : null;
    }

    public void setCep(String cep) {
        Endereco endereco = getOrCreateEnderecoPrincipal();
        endereco.setCep(cep);
    }
}