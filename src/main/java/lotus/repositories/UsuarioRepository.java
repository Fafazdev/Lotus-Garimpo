package lotus.repositories;

import lotus.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email); // verifica se já existe

    Optional<Usuario> findByCpf(String cpf); // verifica se CPF já existe
}
