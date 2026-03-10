package lotus.repositories;

import java.util.List;
import lotus.model.Pedido;
import lotus.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteOrderByDataCompraDesc(Usuario cliente);

    List<Pedido> findByClienteAndStatus(Usuario cliente, String status);
}
