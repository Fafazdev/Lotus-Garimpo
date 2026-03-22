package lotus.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @GetMapping("/vendedor/pedidos")
    public String listarPedidosDoVendedor(HttpSession session, Model model) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");

        if (usuarioLogado == null || usuarioLogado.getTipo() == null || usuarioLogado.getTipo() != 2) {
            return "redirect:/?erro=permissao";
        }

        List<Pedido> pedidos = pedidoRepository.findAllByOrderByDataCompraDesc();

        model.addAttribute("pedidos", pedidos);

        // Dados da dashboard para o vendedor
        long totalPedidos = pedidos.size();
        long totalSacolinha = pedidos.stream()
            .filter(p -> "SACOLINHA_ABERTA".equalsIgnoreCase(p.getStatus()))
            .count();
        long totalAguardandoEnvio = pedidos.stream()
            .filter(p -> "AGUARDANDO_ENVIO".equalsIgnoreCase(p.getStatus()))
            .count();
        long totalEnviados = pedidos.stream()
            .filter(p -> "ENVIADO".equalsIgnoreCase(p.getStatus()))
            .count();

        BigDecimal totalValorPecas = pedidos.stream()
            .map(Pedido::getValorPago)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFrete = pedidos.stream()
            .map(Pedido::getValorFrete)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalPedidos", totalPedidos);
        model.addAttribute("totalSacolinha", totalSacolinha);
        model.addAttribute("totalAguardandoEnvio", totalAguardandoEnvio);
        model.addAttribute("totalEnviados", totalEnviados);
        model.addAttribute("totalValorPecas", totalValorPecas);
        model.addAttribute("totalFrete", totalFrete);

        // Dados do gráfico (últimos 7 dias)
        LocalDate hoje = LocalDate.now();
        List<String> vendasLabels = new ArrayList<>();
        List<BigDecimal> vendasValores = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoje.minusDays(i);
            String label = formataDiaSemana(dia.getDayOfWeek());

            BigDecimal totalDia = pedidos.stream()
                .filter(p -> p.getDataCompra() != null &&
                    p.getDataCompra().toLocalDate().isEqual(dia))
                .map(Pedido::getValorPago)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            vendasLabels.add(label);
            vendasValores.add(totalDia);
        }

        model.addAttribute("vendasLabels", vendasLabels);
        model.addAttribute("vendasValores", vendasValores);

        // Nome do vendedor para saudação no dashboard
        model.addAttribute("vendedorNome", usuarioLogado.getNome());
        return "pedidos-vendedor";
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

    @PostMapping("/vendedor/pedidos/{id}/confirmar-envio")
    public String confirmarEnvio(@PathVariable("id") Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null || usuarioLogado.getTipo() == null || usuarioLogado.getTipo() != 2) {
            return "redirect:/?erro=permissao";
        }

        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) {
            redirectAttributes.addFlashAttribute("vendedorErro", "Pedido não encontrado.");
            return "redirect:/vendedor/pedidos";
        }

        if (!"AGUARDANDO_ENVIO".equalsIgnoreCase(pedido.getStatus())) {
            redirectAttributes.addFlashAttribute("vendedorErro", "Este pedido não está aguardando envio.");
            return "redirect:/vendedor/pedidos";
        }

        pedido.setStatus("ENVIADO");
        pedidoRepository.save(pedido);

        redirectAttributes.addFlashAttribute("vendedorSucesso", "Pedido marcado como ENVIADO.");
        return "redirect:/vendedor/pedidos";
    }

    private String formataDiaSemana(DayOfWeek diaSemana) {
        return switch (diaSemana) {
            case MONDAY -> "Seg";
            case TUESDAY -> "Ter";
            case WEDNESDAY -> "Qua";
            case THURSDAY -> "Qui";
            case FRIDAY -> "Sex";
            case SATURDAY -> "Sáb";
            case SUNDAY -> "Dom";
        };
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
