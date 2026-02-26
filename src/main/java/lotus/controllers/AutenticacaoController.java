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
            @RequestParam("tipo") Integer tipo,
            HttpSession session) {

        if (!senha.equals(confirmarSenha)) {
            return "redirect:/?erro=senha";
        }

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(email);
        if (usuarioExistente.isPresent()) {
            return "redirect:/?erro=email";
        }

        // aceita só 1 (cliente) ou 2 (vendedor); default seguro = 1 (cliente)
        int tipoNormalizado = (tipo != null && (tipo == 1 || tipo == 2)) ? tipo : 1;

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(nome);
        novoUsuario.setEmail(email);
        novoUsuario.setSenha(senha);
        novoUsuario.setTipo(tipoNormalizado);
        novoUsuario.setDataNascimento(LocalDate.now());

        novoUsuario = usuarioRepository.save(novoUsuario);
        session.setAttribute("usuarioLogado", novoUsuario);

        return "redirect:/perfil";
    }

    @PostMapping("/login")
    public String processoLogin(
            @RequestParam("email") String email,
            @RequestParam("senha") String senha,
            HttpSession session) {

        Optional<Usuario> usuarioOptional = usuarioRepository.findByEmail(email);

        if (usuarioOptional.isPresent()) {
            Usuario usuario = usuarioOptional.get();
            if (usuario.getSenha().equals(senha)) {
                session.setAttribute("usuarioLogado", usuario);
                return "redirect:/perfil";
            }
        }

        return "redirect:/?erro=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("usuarioLogado");
        return "redirect:/";
    }
}
