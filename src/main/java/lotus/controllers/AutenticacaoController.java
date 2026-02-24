package lotus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpSession;
import lotus.model.Usuario;
import lotus.repositories.UsuarioRepository;
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class AutenticacaoController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/cadastro")
    public String processoCadastro(
            @RequestParam("nome") String nome,
            @RequestParam("email") String email,
            @RequestParam("senha") String senha,
            @RequestParam("confirmar_senha") String confirmarSenha,
            HttpSession session) {

        // Verifica se as senhas conferem
        if (!senha.equals(confirmarSenha)) {
            return "redirect:/?erro=senha";
        }

        // Verifica se o email já está registrado no banco de dados
        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(email);
        if (usuarioExistente.isPresent()) {
            return "redirect:/?erro=email";
        }

        // Cria um novo usuário
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(nome);
        novoUsuario.setEmail(email);
        novoUsuario.setSenha(senha);
        novoUsuario.setTipo(1); // Tipo padrão de usuário
        novoUsuario.setDataNascimento(LocalDate.now()); // Data padrão

        // Salva no banco de dados
        novoUsuario = usuarioRepository.save(novoUsuario);

        // Armazena na sessão
        session.setAttribute("usuarioLogado", novoUsuario);

        return "redirect:/perfil";
    }

    @PostMapping("/login")
    public String processoLogin(
            @RequestParam("email") String email,
            @RequestParam("senha") String senha,
            HttpSession session) {

        // Busca o usuário no banco de dados
        Optional<Usuario> usuarioOptional = usuarioRepository.findByEmail(email);

        // Verifica se o usuário existe e a senha está correta
        if (usuarioOptional.isPresent()) {
            Usuario usuario = usuarioOptional.get();
            if (usuario.getSenha().equals(senha)) {
                // Armazena o usuário na sessão
                session.setAttribute("usuarioLogado", usuario);
                return "redirect:/perfil";
            }
        }
        
        // Redireciona para login com mensagem de erro
        return "redirect:/?erro=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Remove o usuário da sessão
        session.removeAttribute("usuarioLogado");
        return "redirect:/";
    }
}
