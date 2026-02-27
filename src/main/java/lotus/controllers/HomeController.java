package lotus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Faltava este import
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession; // Faltava este import
import lotus.model.Usuario;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home"; // Certifique-se de que o arquivo é home.html
    }

    @GetMapping("/home")
    public String home(Model model, HttpSession session) {
        // Busca o usuário logado na sessão
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        // Passa para o Thymeleaf
        model.addAttribute("usuarioLogado", usuarioLogado);

        return "home";
    }
}