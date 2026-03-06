package lotus.controllers;

import jakarta.servlet.http.HttpSession;
import lotus.model.ItemCarrinho;
import lotus.model.Usuario;
import lotus.repositories.ItemCarrinhoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@ControllerAdvice
@Component
public class CarrinhoModelAdvice {

    @Autowired
    private ItemCarrinhoRepository itemCarrinhoRepository;

    @ModelAttribute
    public void adicionarCarrinhoNoModel(Model model, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            model.addAttribute("itensCarrinho", Collections.emptyList());
            model.addAttribute("subtotalCarrinho", BigDecimal.ZERO);
            model.addAttribute("quantidadeCarrinho", 0);
            return;
        }

        List<ItemCarrinho> itens = itemCarrinhoRepository.findByUsuario(usuarioLogado);

        BigDecimal subtotal = itens.stream()
                .map(item -> item.getProduto().getPreco()
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int quantidadeTotal = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();

        model.addAttribute("itensCarrinho", itens);
        model.addAttribute("subtotalCarrinho", subtotal);
        model.addAttribute("quantidadeCarrinho", quantidadeTotal);
    }
}
