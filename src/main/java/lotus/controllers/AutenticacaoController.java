package lotus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lotus.model.Usuario;
import lotus.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;
import java.util.Optional;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Controller
public class AutenticacaoController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/cadastro")
    public String processoCadastro(
            @RequestParam("nome") String nome,
            @RequestParam("email") String email,
            @RequestParam("cpf") String cpf,
            @RequestParam("senha") String senha,
            @RequestParam("confirmar_senha") String confirmarSenha,
            @RequestParam("tipo") Integer tipo,
            HttpServletRequest request) {

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

        // Gera hash BCrypt da senha
        novoUsuario.setSenha(passwordEncoder.encode(senha));
        novoUsuario.setTipo(tipoNormalizado);
        novoUsuario.setDataCriacao(LocalDate.now());

        novoUsuario = usuarioRepository.save(novoUsuario);

        // Rotação de sessão: invalida sessão anterior e cria uma nova
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);
        newSession.setAttribute("usuarioLogado", novoUsuario);

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
            HttpServletRequest request) {

        Optional<Usuario> usuarioOptional = usuarioRepository.findByEmail(email);

        if (usuarioOptional.isPresent()) {
            Usuario usuario = usuarioOptional.get();

            if (isPasswordValid(senha, usuario)) {
                // Rotação de sessão: invalida a sessão anterior e cria uma nova
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }
                HttpSession newSession = request.getSession(true);
                newSession.setAttribute("usuarioLogado", usuario);
                return "redirect:/perfil";
            }
        }

        // Login inválido: email não encontrado ou senha incorreta
        return "redirect:/?erro=login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
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

    // Verifica senha: suporta BCrypt (novo) e SHA-256 legado (migra automaticamente)
    private boolean isPasswordValid(String rawPassword, Usuario usuario) {
        String storedHash = usuario.getSenha();
        if (storedHash == null) return false;

        // Hash BCrypt (começa com $2)
        if (storedHash.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedHash);
        }

        // Hash SHA-256 legado (64 chars hex) — migra para BCrypt automaticamente no próximo login
        if (storedHash.length() == 64 && storedHash.matches("[a-f0-9]+")) {
            if (legacySha256(rawPassword).equals(storedHash)) {
                usuario.setSenha(passwordEncoder.encode(rawPassword));
                usuarioRepository.save(usuario);
                return true;
            }
        }

        return false;
    }

    // Mantido apenas para migração de senhas SHA-256 existentes
    private String legacySha256(String value) {
        if (value == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao processar senha", e);
        }
    }
}
