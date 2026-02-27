package lotus.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dt_compra")
    private LocalDateTime dataCompra = LocalDateTime.now();

    @Column(name = "status_pedido")
    private String status;

    @Column(name = "valor_pago", nullable = false)
    private BigDecimal valorPago;

    @Column(name = "valor_frete")
    private BigDecimal valorFrete = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario cliente;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne
    @JoinColumn(name = "pagamento_id")
    private Pagamento pagamento;

    @ManyToOne
    @JoinColumn(name = "envio_id")
    private Envio envio;

    public Pedido() {}
    // Getters e Setters...
}