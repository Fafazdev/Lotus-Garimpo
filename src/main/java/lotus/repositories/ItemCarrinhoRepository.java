package lotus.repositories;

import lotus.model.ItemCarrinho;
import lotus.model.Produto;
import lotus.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {

    List<ItemCarrinho> findByUsuario(Usuario usuario);

    Optional<ItemCarrinho> findByUsuarioAndProduto(Usuario usuario, Produto produto);
}
