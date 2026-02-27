package lotus.repositories;

import lotus.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    // futuros métodos específicos de consulta podem ser adicionados aqui
}