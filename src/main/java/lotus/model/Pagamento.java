package lotus.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pagamentos")
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metodo_pagamento", nullable = false)
    private String metodo;

    @Column(name = "status_pagamento")
    private String status = "PENDENTE";

    private BigDecimal valor;
    private Integer parcelas = 1;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento = LocalDateTime.now();

    public Pagamento() {}
    // Getters e Setters...
}