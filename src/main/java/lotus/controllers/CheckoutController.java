package lotus.controllers;

import jakarta.servlet.http.HttpSession;
import lotus.model.Endereco;
import lotus.model.ItemCarrinho;
import lotus.model.Pagamento;
import lotus.model.Pedido;
import lotus.model.Usuario;
import lotus.repositories.EnderecoRepository;
import lotus.repositories.ItemCarrinhoRepository;
import lotus.repositories.PagamentoRepository;
import lotus.repositories.PedidoRepository;
import lotus.services.CorreiosFreteService;
import lotus.services.MercadoPagoPixService;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private MercadoPagoPixService mercadoPagoPixService;

    @Autowired
    private ItemCarrinhoRepository itemCarrinhoRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    // Em desenvolvimento, podemos simular a aprovação do Pix sem consultar o Mercado Pago
    @Value("${lotus.mercadopago.skip-status-check:true}")
    private boolean skipPixStatusCheck;

    @GetMapping("/checkout")
    public String mostrarCheckoutEnderecos(HttpSession session, Model model) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        // Se o subtotal do carrinho for zero, considera carrinho vazio e volta para a home
        BigDecimal subtotalCarrinho = (BigDecimal) model.getAttribute("subtotalCarrinho");
        if (subtotalCarrinho == null || subtotalCarrinho.compareTo(BigDecimal.ZERO) <= 0) {
            return "redirect:/?erro=carrinhoVazio";
        }

        List<Endereco> enderecos = enderecoRepository.findByUsuario(usuarioLogado);
        model.addAttribute("enderecos", enderecos);

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

        return "redirect:/checkout";
    }

    @PostMapping("/checkout/continuar-para-pagamento")
    public String continuarParaPagamento(@RequestParam(value = "enderecoId", required = false) Long enderecoId,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        if (enderecoId == null) {
            redirectAttributes.addFlashAttribute("erroCheckout", "Selecione um endere\u00e7o para continuar para o pagamento.");
            return "redirect:/checkout";
        }

        Optional<Endereco> optEndereco = enderecoRepository.findByIdAndUsuario(enderecoId, usuarioLogado);
        if (optEndereco.isEmpty()) {
            redirectAttributes.addFlashAttribute("erroCheckout", "Endere\u00e7o inv\u00e1lido para o usu\u00e1rio logado.");
            return "redirect:/checkout";
        }

        session.setAttribute("enderecoSelecionado", optEndereco.get());
        redirectAttributes.addFlashAttribute("sucessoCheckout", "Endere\u00e7o selecionado com sucesso.");

        return "redirect:/pagamento";
    }

    @GetMapping("/pagamento")
    public String mostrarPagamento(HttpSession session, Model model) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        BigDecimal subtotalCarrinho = (BigDecimal) model.getAttribute("subtotalCarrinho");
        Integer quantidadeCarrinho = (Integer) model.getAttribute("quantidadeCarrinho");
        Endereco enderecoSelecionado = (Endereco) session.getAttribute("enderecoSelecionado");

        if (subtotalCarrinho == null || subtotalCarrinho.compareTo(BigDecimal.ZERO) <= 0 ||
                enderecoSelecionado == null) {
            return "redirect:/checkout";
        }

        int qtd = quantidadeCarrinho != null ? quantidadeCarrinho : 1;
        BigDecimal pesoPorItem = new BigDecimal("0.3");
        BigDecimal pesoTotal = pesoPorItem.multiply(BigDecimal.valueOf(qtd));

        BigDecimal freteCarrinho = calcularFrete(subtotalCarrinho, enderecoSelecionado, pesoTotal);
        BigDecimal totalCarrinhoComFrete = subtotalCarrinho.add(freteCarrinho);

        // Pagamento padrão: envio imediato (com frete já incluso)
        model.addAttribute("freteCarrinho", freteCarrinho);
        model.addAttribute("totalCarrinhoComFrete", totalCarrinhoComFrete);
        model.addAttribute("tipoPedido", "ENVIO");
        model.addAttribute("valorPix", totalCarrinhoComFrete);

        return "pagamento";
    }

    @PostMapping("/pagamento/pix")
    public String iniciarPagamentoPix(@RequestParam(name = "tipoPedido", defaultValue = "ENVIO") String tipoPedido,
                                      HttpSession session,
                                      Model model) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        BigDecimal subtotalCarrinho = (BigDecimal) model.getAttribute("subtotalCarrinho");
        Integer quantidadeCarrinho = (Integer) model.getAttribute("quantidadeCarrinho");
        Endereco enderecoSelecionado = (Endereco) session.getAttribute("enderecoSelecionado");

        if (subtotalCarrinho == null || subtotalCarrinho.compareTo(BigDecimal.ZERO) <= 0 ||
                enderecoSelecionado == null) {
            return "redirect:/checkout";
        }

        int qtd = quantidadeCarrinho != null ? quantidadeCarrinho : 1;
        BigDecimal pesoPorItem = new BigDecimal("0.3");
        BigDecimal pesoTotal = pesoPorItem.multiply(BigDecimal.valueOf(qtd));

        BigDecimal freteCarrinho = calcularFrete(subtotalCarrinho, enderecoSelecionado, pesoTotal);
        BigDecimal totalCarrinhoComFrete = subtotalCarrinho.add(freteCarrinho);

        // Define quanto será cobrado agora no Pix
        BigDecimal valorPix;
        if ("SACOLINHA".equalsIgnoreCase(tipoPedido)) {
            // Sacolinha: cliente paga só o valor das peças, sem frete agora
            tipoPedido = "SACOLINHA"; // normaliza
            valorPix = subtotalCarrinho;
            freteCarrinho = BigDecimal.ZERO; // frete será cobrado apenas quando fechar a sacolinha
        } else {
            // Envio normal: paga produtos + frete agora
            tipoPedido = "ENVIO";
            valorPix = totalCarrinhoComFrete;
        }

        model.addAttribute("freteCarrinho", freteCarrinho);
        model.addAttribute("totalCarrinhoComFrete", totalCarrinhoComFrete);
        model.addAttribute("tipoPedido", tipoPedido);
        model.addAttribute("valorPix", valorPix);

        // guarda algumas informa\u00e7\u00f5es na sess\u00e3o para a etapa de confirma\u00e7\u00e3o
        session.setAttribute("tipoPedidoAtual", tipoPedido);
        session.setAttribute("valorPixAtual", valorPix);
        session.setAttribute("freteCarrinhoAtual", freteCarrinho);

        try {
            MercadoPagoPixService.PixPaymentResponse pix =
                mercadoPagoPixService.criarCobrancaPix(valorPix,
                    "Pedido Lotus Garimpo",
                    usuarioLogado.getEmail());

            model.addAttribute("pixIniciado", true);
            model.addAttribute("pixQrCode", pix.getQrCode());
            model.addAttribute("pixQrCodeBase64", pix.getQrCodeBase64());
            model.addAttribute("pixPaymentId", pix.getPaymentId());

            session.setAttribute("pixPaymentId", pix.getPaymentId());
        } catch (Exception e) {
            // Log simples no console para entendermos o erro que veio do Mercado Pago
            e.printStackTrace();
            model.addAttribute("pixErro", "Não foi possível gerar o Pix no momento. Tente novamente.");
        }

        return "pagamento";
    }

    @PostMapping("/pagamento/pix/confirmar")
    public String confirmarPagamentoPix(HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/?erro=login";
        }

        Object paymentIdObj = session.getAttribute("pixPaymentId");
        String paymentId = paymentIdObj != null ? paymentIdObj.toString() : null;
        if (paymentId == null) {
            redirectAttributes.addFlashAttribute("pixErro", "Nenhum pagamento Pix para confirmar.");
            return "redirect:/pagamento";
        }

        String tipoPedido = (String) session.getAttribute("tipoPedidoAtual");
        BigDecimal valorPix = (BigDecimal) session.getAttribute("valorPixAtual");
        BigDecimal freteCarrinho = (BigDecimal) session.getAttribute("freteCarrinhoAtual");

        if (tipoPedido == null || valorPix == null) {
            redirectAttributes.addFlashAttribute("pixErro", "Informacoes do pagamento n\u00e3o encontradas.");
            return "redirect:/pagamento";
        }

        try {
            String statusPagamento;
            if (skipPixStatusCheck) {
                // Modo de teste: não consulta o Mercado Pago, considera como aprovado
                statusPagamento = "approved";
            } else {
                statusPagamento = mercadoPagoPixService.consultarStatusPagamento(paymentId);
                if (!"approved".equalsIgnoreCase(statusPagamento)) {
                    redirectAttributes.addFlashAttribute("pixErro",
                            "Pagamento ainda n\u00e3o aprovado pelo Mercado Pago (status: " + statusPagamento + ").");
                    return "redirect:/pagamento";
                }
            }

            // Cria registro de pagamento
            Pagamento pagamento = new Pagamento();
            pagamento.setMetodo("PIX");
            pagamento.setStatus("PAGO");
            pagamento.setValor(valorPix);
            pagamentoRepository.save(pagamento);

            // Cria um pedido agregando todos os itens do carrinho do usu\u00e1rio
            java.util.List<ItemCarrinho> itens = itemCarrinhoRepository.findByUsuario(usuarioLogado);

            Pedido pedido = new Pedido();
            pedido.setCliente(usuarioLogado);
            pedido.setPagamento(pagamento);

            java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
            for (ItemCarrinho item : itens) {
                lotus.model.ItemPedido itemPedido = new lotus.model.ItemPedido();
                itemPedido.setPedido(pedido);
                itemPedido.setProduto(item.getProduto());
                itemPedido.setQuantidade(item.getQuantidade());

                java.math.BigDecimal precoUnitario = item.getProduto().getPreco();
                itemPedido.setPrecoUnitario(precoUnitario);

                subtotal = subtotal.add(precoUnitario.multiply(java.math.BigDecimal.valueOf(item.getQuantidade())));

                pedido.getItens().add(itemPedido);
            }

            pedido.setValorPago(subtotal);

            if (freteCarrinho != null && freteCarrinho.compareTo(java.math.BigDecimal.ZERO) > 0
                    && "ENVIO".equalsIgnoreCase(tipoPedido)) {
                pedido.setValorFrete(freteCarrinho);
            }

            if ("SACOLINHA".equalsIgnoreCase(tipoPedido)) {
                pedido.setStatus("SACOLINHA_ABERTA");
            } else {
                pedido.setStatus("AGUARDANDO_ENVIO");
            }

            pedidoRepository.save(pedido);

            // Limpa carrinho ap\u00f3s o pagamento
            itemCarrinhoRepository.deleteAll(itens);

            // Limpa dados da sess\u00e3o relacionados ao pagamento Pix
            session.removeAttribute("pixPaymentId");
            session.removeAttribute("tipoPedidoAtual");
            session.removeAttribute("valorPixAtual");
            session.removeAttribute("freteCarrinhoAtual");

            redirectAttributes.addFlashAttribute("sucessoCheckout",
                    "Pagamento confirmado com sucesso! Seu pedido foi registrado.");
            return "redirect:/perfil";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("pixErro",
                    "Erro ao confirmar o pagamento no Mercado Pago. Tente novamente.");
            return "redirect:/pagamento";
        }
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
