package lotus.repositories;

import lotus.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findTop30ByNomeContainingIgnoreCaseOrDescricaoContainingIgnoreCaseOrCategoriaContainingIgnoreCaseOrTamanhoContainingIgnoreCaseOrderByIdDesc(
            String nome,
            String descricao,
            String categoria,
            String tamanho
    );
}