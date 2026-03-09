package lotus.controllers;

import jakarta.servlet.http.HttpSession;
import lotus.model.Endereco;
import lotus.model.Usuario;
import lotus.repositories.EnderecoRepository;
import lotus.services.CorreiosFreteService;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class CheckoutController {

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private CorreiosFreteService correiosFreteService;

    @GetMapping("/checkout")
    public String mostrarCheckoutEnderecos(HttpSession session, Model model) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        List<Endereco> enderecos = enderecoRepository.findByUsuario(usuarioLogado);
        model.addAttribute("enderecos", enderecos);

        BigDecimal subtotalCarrinho = (BigDecimal) model.getAttribute("subtotalCarrinho");
        Integer quantidadeCarrinho = (Integer) model.getAttribute("quantidadeCarrinho");
        Endereco enderecoSelecionado = (Endereco) session.getAttribute("enderecoSelecionado");

        BigDecimal freteCarrinho = null;
        BigDecimal totalCarrinhoComFrete = subtotalCarrinho != null ? subtotalCarrinho : BigDecimal.ZERO;

        if (enderecoSelecionado != null && subtotalCarrinho != null && subtotalCarrinho.compareTo(BigDecimal.ZERO) > 0) {
            int qtd = quantidadeCarrinho != null ? quantidadeCarrinho : 1;
            BigDecimal pesoPorItem = new BigDecimal("0.3"); // 300g por peça (estimativa)
            BigDecimal pesoTotal = pesoPorItem.multiply(BigDecimal.valueOf(qtd));

            freteCarrinho = calcularFrete(subtotalCarrinho, enderecoSelecionado, pesoTotal);
            totalCarrinhoComFrete = subtotalCarrinho.add(freteCarrinho);
        }

        model.addAttribute("freteCarrinho", freteCarrinho);
        model.addAttribute("totalCarrinhoComFrete", totalCarrinhoComFrete);

        return "checkout";
    }

    @PostMapping("/checkout/selecionar-endereco")
    public String selecionarEndereco(@RequestParam("enderecoId") Long enderecoId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        Optional<Endereco> optEndereco = enderecoRepository.findByIdAndUsuario(enderecoId, usuarioLogado);
        if (optEndereco.isEmpty()) {
            redirectAttributes.addFlashAttribute("erroCheckout", "Endere\u00e7o inv\u00e1lido para o usu\u00e1rio logado.");
            return "redirect:/checkout";
        }

        session.setAttribute("enderecoSelecionado", optEndereco.get());
        redirectAttributes.addFlashAttribute("sucessoCheckout", "Endere\u00e7o selecionado com sucesso.");

        // Por enquanto, mantemos o usu\u00e1rio na mesma tela; pr\u00f3xima etapa (pagamento)
        // pode ser implementada depois.
        return "redirect:/checkout";
    }

    private BigDecimal calcularFrete(BigDecimal subtotal, Endereco endereco, BigDecimal pesoTotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0 || endereco == null) {
            return BigDecimal.ZERO;
        }
        // Regras internas simples de frete (sem chamar API externa)
        BigDecimal freteGratisLimite = new BigDecimal("250.00");
        if (subtotal.compareTo(freteGratisLimite) >= 0) {
            return BigDecimal.ZERO;
        }

        String uf = endereco.getUf() != null ? endereco.getUf().trim().toUpperCase() : "";
        String cidade = endereco.getCidade() != null ? endereco.getCidade().trim().toLowerCase() : "";

        // Tabela baseada na média informada pela Thata:
        // Baixada Santista (SP) ≈ 9,90 | restante de SP ≈ 12,00 | demais estados ≈ 16,00
        if ("SP".equals(uf)) {
            boolean isBaixadaSantista =
                    cidade.equals("santos") ||
                    cidade.equals("são vicente") || cidade.equals("sao vicente") ||
                    cidade.equals("praia grande") ||
                    cidade.equals("cubatão") || cidade.equals("cubatao") ||
                    cidade.equals("guarujá") || cidade.equals("guaruja") ||
                    cidade.equals("bertioga") ||
                    cidade.equals("mongaguá") || cidade.equals("mongagua") ||
                    cidade.equals("itanhaém") || cidade.equals("itanhaem") ||
                    cidade.equals("peruíbe") || cidade.equals("peruibe");

            if (isBaixadaSantista) {
                return new BigDecimal("9.90");
            }

            return new BigDecimal("12.00");
        }

        // Demais estados (fora de SP)
        return new BigDecimal("16.00");
    }
}
