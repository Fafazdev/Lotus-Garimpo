package lotus.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_envios")
public class Envio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dt_envio")
    private LocalDateTime dataEnvio = LocalDateTime.now();

    @Column(name = "cod_rastreio")
    private String codigoRastreio;

    @Column(name = "valor_total_frete")
    private BigDecimal valorTotalFrete;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Envio() {}
    // Getters e Setters...
}