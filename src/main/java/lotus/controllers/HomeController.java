package lotus.controllers;

import lotus.model.Produto;
import lotus.repositories.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import lotus.model.Usuario;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping("/")
    public String home(Model model) {
        // busca os 3 produtos mais recentes (ordenados por ID decrescente)
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"));
        List<Produto> produtosRecentes = produtoRepository.findAll(pageable).getContent();
        model.addAttribute("produtosRecentes", produtosRecentes);
        return "home";
    }

    @GetMapping("/home")
    public String homeRedirect(Model model, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        model.addAttribute("usuarioLogado", usuarioLogado);
        
        // busca os 3 produtos mais recentes (ordenados por ID decrescente)
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"));
        List<Produto> produtosRecentes = produtoRepository.findAll(pageable).getContent();
        model.addAttribute("produtosRecentes", produtosRecentes);
        
        return "home";
    }
}