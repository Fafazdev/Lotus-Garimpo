package lotus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
            @RequestParam("cpf") String cpf,
            @RequestParam("senha") String senha,
            @RequestParam("confirmar_senha") String confirmarSenha,
            @RequestParam("tipo") Integer tipo,
            HttpSession session) {

        // Normaliza CPF para apenas dígitos para validação
        String cpfNumerico = cpf != null ? cpf.replaceAll("\\D", "") : "";

        // Validação básica de CPF (11 dígitos + dígitos verificadores)
        if (!isCpfValido(cpfNumerico)) {
            return "redirect:/?erro=cpf";
        }

        if (!senha.equals(confirmarSenha)) {
            return "redirect:/?erro=senha";
        }

        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(email);
        if (usuarioExistente.isPresent()) {
            // se já existe, NÃO cria conta
            return "redirect:/?erro=email";
        }

        Optional<Usuario> cpfExistente = usuarioRepository.findByCpf(cpf);
        if (cpfExistente.isPresent()) {
            // se CPF já existe, NÃO cria conta
            return "redirect:/?erro=cpf";
        }

        // aceita só 1 (cliente) ou 2 (vendedor); default seguro = 1 (cliente)
        int tipoNormalizado = (tipo != null && (tipo == 1 || tipo == 2)) ? tipo : 1;

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(nome);
        novoUsuario.setEmail(email);
        novoUsuario.setCpf(cpf);
        novoUsuario.setSenha(senha);
        novoUsuario.setTipo(tipoNormalizado);
        novoUsuario.setDataNascimento(LocalDate.now());

        novoUsuario = usuarioRepository.save(novoUsuario);
        session.setAttribute("usuarioLogado", novoUsuario);

        return "redirect:/perfil";
    }

    @GetMapping("/api/usuarios/verificar-cpf")
    @ResponseBody
    public boolean verificarCpf(@RequestParam("cpf") String cpf) {
        return usuarioRepository.findByCpf(cpf).isPresent();
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

    // Validador simples de CPF (11 dígitos e dígitos verificadores)
    private boolean isCpfValido(String cpf) {
        if (cpf == null) return false;

        // Mantém apenas dígitos
        String numero = cpf.replaceAll("\\D", "");

        if (numero.length() != 11) return false;

        // Rejeita CPFs com todos os dígitos iguais (111.111.111-11 etc.)
        if (numero.chars().distinct().count() == 1) return false;

        try {
            // Primeiro dígito verificador
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                int digito = Character.getNumericValue(numero.charAt(i));
                soma += digito * (10 - i);
            }
            int resto = soma % 11;
            int dv1 = (resto < 2) ? 0 : 11 - resto;
            if (dv1 != Character.getNumericValue(numero.charAt(9))) {
                return false;
            }

            // Segundo dígito verificador
            soma = 0;
            for (int i = 0; i < 10; i++) {
                int digito = Character.getNumericValue(numero.charAt(i));
                soma += digito * (11 - i);
            }
            resto = soma % 11;
            int dv2 = (resto < 2) ? 0 : 11 - resto;

            return dv2 == Character.getNumericValue(numero.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }
}
