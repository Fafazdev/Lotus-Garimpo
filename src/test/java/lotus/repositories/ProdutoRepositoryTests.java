package lotus.repositories;

import lotus.model.Produto;
import lotus.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProdutoRepositoryTests {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void save_shouldPersistProductAndAssociateUser() {
        Usuario u = new Usuario();
        u.setEmail("test@domain.com");
        u.setNome("Test User");
        u.setSenha("password");
        u.setDataNascimento(java.time.LocalDate.now());
        u.setTipo(1);
        u = usuarioRepository.save(u);

        Produto p = new Produto();
        p.setNome("Teste");
        p.setDescricao("Descrição teste");
        p.setPreco(BigDecimal.valueOf(123.45));
        p.setTamanho("M");
        p.setCategoria("Testes");
        p.setUsuario(u);

        p = produtoRepository.save(p);

        assertThat(p.getId()).isNotNull();
        assertThat(produtoRepository.findById(p.getId())).isPresent();
        assertThat(produtoRepository.findById(p.getId()).get().getUsuario().getId()).isEqualTo(u.getId());
    }
}
