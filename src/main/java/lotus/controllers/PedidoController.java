package lotus.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestParam;
import lotus.model.Endereco;
import lotus.model.Pedido;
import lotus.model.Usuario;
import lotus.repositories.EnderecoRepository;
import lotus.repositories.PedidoRepository;
import lotus.services.MercadoPagoPixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class PedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private MercadoPagoPixService mercadoPagoPixService;

    @GetMapping("/meus-pedidos")
    public String listarPedidosDoUsuario(HttpSession session, Model model) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        List<Pedido> pedidos = pedidoRepository.findByClienteOrderByDataCompraDesc(usuarioLogado);
        model.addAttribute("pedidos", pedidos);

        boolean hasSacolinhaAberta = pedidos.stream()
                .anyMatch(p -> "SACOLINHA_ABERTA".equalsIgnoreCase(p.getStatus()));
        model.addAttribute("hasSacolinhaAberta", hasSacolinhaAberta);

        return "meus-pedidos";
    }

    @PostMapping("/meus-pedidos/fechar-sacolinha")
    public String fecharSacolinha(@RequestParam("pedidoId") Long pedidoId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido == null || pedido.getCliente() == null ||
                !usuarioLogado.getId().equals(pedido.getCliente().getId()) ||
                !"SACOLINHA_ABERTA".equalsIgnoreCase(pedido.getStatus())) {
            redirectAttributes.addFlashAttribute("pixErro",
                    "Pedido da sacolinha não encontrado ou já fechado.");
            return "redirect:/meus-pedidos";
        }

        // Define um endere\u00e7o para calcular o frete da sacolinha
        List<Endereco> enderecos = enderecoRepository.findByUsuario(usuarioLogado);
        if (enderecos.isEmpty()) {
            redirectAttributes.addFlashAttribute("pixErro",
                    "Cadastre um endereço de entrega para fechar este pedido da sacolinha.");
            return "redirect:/meus-pedidos";
        }

        Endereco endereco = enderecos.get(0); // por enquanto usamos o primeiro endere\u00e7o cadastrado

        // Calcula subtotal e quantidade de itens apenas deste pedido
        BigDecimal subtotal = pedido.getValorPago() != null ? pedido.getValorPago() : BigDecimal.ZERO;

        int quantidadeItens = 1;

        BigDecimal frete = calcularFreteInterno(subtotal, endereco, quantidadeItens);

        // Atualiza status do pedido e registra o valor de frete
        pedido.setStatus("AGUARDANDO_ENVIO");
        pedido.setValorFrete(frete);
        pedidoRepository.save(pedido);

        // Gera um Pix apenas para o frete da sacolinha
        try {
            MercadoPagoPixService.PixPaymentResponse pix =
                    mercadoPagoPixService.criarCobrancaPix(frete,
                            "Frete pedido sacolinha Lotus Garimpo",
                            usuarioLogado.getEmail());

            redirectAttributes.addFlashAttribute("fretePixIniciado", true);
            redirectAttributes.addFlashAttribute("freteQrCode", pix.getQrCode());
            redirectAttributes.addFlashAttribute("freteQrCodeBase64", pix.getQrCodeBase64());
            redirectAttributes.addFlashAttribute("freteValorPix", frete);

            redirectAttributes.addFlashAttribute("sucessoCheckout",
                    "Pedido da sacolinha fechado! Pague o frete para liberar o envio.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("pixErro",
                    "N\u00e3o foi poss\u00edvel gerar o Pix do frete. Tente novamente.");
        }

        return "redirect:/meus-pedidos";
    }

    private BigDecimal calcularFreteInterno(BigDecimal subtotal, Endereco endereco, int quantidadeItens) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0 || endereco == null || quantidadeItens <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal freteGratisLimite = new BigDecimal("250.00");
        if (subtotal.compareTo(freteGratisLimite) >= 0) {
            return BigDecimal.ZERO;
        }

        String uf = endereco.getUf() != null ? endereco.getUf().trim().toUpperCase() : "";
        String cidade = endereco.getCidade() != null ? endereco.getCidade().trim().toLowerCase() : "";

        if ("SP".equals(uf)) {
            boolean isBaixadaSantista =
                    cidade.equals("santos") ||
                            cidade.equals("s\u00e3o vicente") || cidade.equals("sao vicente") ||
                            cidade.equals("praia grande") ||
                            cidade.equals("cubat\u00e3o") || cidade.equals("cubatao") ||
                            cidade.equals("guaruj\u00e1") || cidade.equals("guaruja") ||
                            cidade.equals("bertioga") ||
                            cidade.equals("mongagu\u00e1") || cidade.equals("mongagua") ||
                            cidade.equals("itanha\u00e9m") || cidade.equals("itanhaem") ||
                            cidade.equals("peru\u00edbe") || cidade.equals("peruibe");

            if (isBaixadaSantista) {
                return new BigDecimal("9.90");
            }

            return new BigDecimal("12.00");
        }

        return new BigDecimal("16.00");
    }
}
