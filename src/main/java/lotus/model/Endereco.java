package lotus.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_enderecos")
public class Endereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cd_cep", nullable = false, length = 9)
    private String cep;

    @Column(name = "nm_logradouro")
    private String logradouro;

    @Column(name = "nr_numero")
    private String numero;

    @Column(name = "nm_bairro")
    private String bairro;

    @Column(name = "nm_cidade")
    private String cidade;

    @Column(name = "sg_uf", length = 2)
    private String uf;

    @Column(name = "ds_complemento")
    private String complemento;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Endereco() {}
    // Getters e Setters...
}