package lotus.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lotus.model.ItemCarrinho;
import lotus.model.Produto;
import lotus.model.Usuario;
import lotus.repositories.ItemCarrinhoRepository;
import lotus.repositories.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CarrinhoController {

    @Autowired
    private ItemCarrinhoRepository itemCarrinhoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @PostMapping("/carrinho/adicionar")
    public String adicionarAoCarrinho(@RequestParam("produtoId") Long produtoId,
                                      HttpSession session,
                                      HttpServletRequest request) {

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            // se não estiver logado, volta para a página anterior
            return voltarParaPaginaAnterior(request, "/");
        }

        Optional<Produto> optProduto = produtoRepository.findById(produtoId);
        if (optProduto.isEmpty()) {
            return voltarParaPaginaAnterior(request, "/");
        }

        Produto produto = optProduto.get();

        Optional<ItemCarrinho> optItem = itemCarrinhoRepository.findByUsuarioAndProduto(usuarioLogado, produto);
        ItemCarrinho item;
        if (optItem.isPresent()) {
            item = optItem.get();
            item.setQuantidade(item.getQuantidade() + 1);
        } else {
            item = new ItemCarrinho();
            item.setUsuario(usuarioLogado);
            item.setProduto(produto);
            item.setQuantidade(1);
        }

        itemCarrinhoRepository.save(item);

        return voltarParaPaginaAnterior(request, "/");
    }

    @PostMapping("/carrinho/adicionar-ajax")
    @ResponseBody
    public Map<String, Object> adicionarAoCarrinhoAjax(@RequestParam("produtoId") Long produtoId,
                                                       HttpSession session) {

        Map<String, Object> resposta = new HashMap<>();

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            resposta.put("success", false);
            resposta.put("requiresLogin", true);
            resposta.put("message", "Usuário não autenticado");
            return resposta;
        }

        Optional<Produto> optProduto = produtoRepository.findById(produtoId);
        if (optProduto.isEmpty()) {
            resposta.put("success", false);
            resposta.put("message", "Produto não encontrado");
            return resposta;
        }

        Produto produto = optProduto.get();

        Optional<ItemCarrinho> optItem = itemCarrinhoRepository.findByUsuarioAndProduto(usuarioLogado, produto);
        ItemCarrinho item;
        if (optItem.isPresent()) {
            item = optItem.get();
            item.setQuantidade(item.getQuantidade() + 1);
        } else {
            item = new ItemCarrinho();
            item.setUsuario(usuarioLogado);
            item.setProduto(produto);
            item.setQuantidade(1);
        }

        itemCarrinhoRepository.save(item);

        List<ItemCarrinho> itens = itemCarrinhoRepository.findByUsuario(usuarioLogado);

        BigDecimal subtotal = itens.stream()
                .map(ic -> ic.getProduto().getPreco()
                        .multiply(BigDecimal.valueOf(ic.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int quantidadeTotal = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();

        String subtotalFormatado = subtotal
                .setScale(2, RoundingMode.HALF_UP)
                .toString()
                .replace('.', ',');

        Map<String, Object> novoItem = new HashMap<>();
        novoItem.put("id", item.getId());
        novoItem.put("quantidade", item.getQuantidade());
        novoItem.put("nome", item.getProduto().getNome());
        novoItem.put("tamanho", item.getProduto().getTamanho());
        novoItem.put("imagem", item.getProduto().getImagem());

        BigDecimal precoUnitario = item.getProduto().getPreco() != null
            ? item.getProduto().getPreco()
            : BigDecimal.ZERO;
        String precoFormatado = precoUnitario
            .setScale(2, RoundingMode.HALF_UP)
            .toString()
            .replace('.', ',');

        novoItem.put("precoFormatado", precoFormatado);

        resposta.put("success", true);
        resposta.put("subtotal", subtotal);
        resposta.put("subtotalFormatado", subtotalFormatado);
        resposta.put("quantidadeCarrinho", quantidadeTotal);
        resposta.put("novoItem", novoItem);

        return resposta;
    }

    @PostMapping("/carrinho/remover")
    public String removerDoCarrinho(@RequestParam("itemId") Long itemId,
                                    HttpSession session,
                                    HttpServletRequest request) {

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return voltarParaPaginaAnterior(request, "/");
        }

        itemCarrinhoRepository.findById(itemId).ifPresent(item -> {
            if (item.getUsuario() != null && item.getUsuario().getId().equals(usuarioLogado.getId())) {
                if (item.getQuantidade() > 1) {
                    item.setQuantidade(item.getQuantidade() - 1);
                    itemCarrinhoRepository.save(item);
                } else {
                    itemCarrinhoRepository.delete(item);
                }
            }
        });

        return voltarParaPaginaAnterior(request, "/");
    }

    @PostMapping("/carrinho/remover-ajax")
    @ResponseBody
    public Map<String, Object> removerDoCarrinhoAjax(@RequestParam("itemId") Long itemId,
                                                     HttpSession session) {

        Map<String, Object> resposta = new HashMap<>();

        final ItemCarrinho[] itemAlterado = new ItemCarrinho[1];

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            resposta.put("success", false);
            resposta.put("message", "Usuário não autenticado");
            return resposta;
        }

        itemCarrinhoRepository.findById(itemId).ifPresent(item -> {
            if (item.getUsuario() != null && item.getUsuario().getId().equals(usuarioLogado.getId())) {
                if (item.getQuantidade() > 1) {
                    item.setQuantidade(item.getQuantidade() - 1);
                    itemCarrinhoRepository.save(item);
                    itemAlterado[0] = item;
                } else {
                    itemCarrinhoRepository.delete(item);
                    itemAlterado[0] = null;
                }
            }
        });

        List<ItemCarrinho> itensRestantes = itemCarrinhoRepository.findByUsuario(usuarioLogado);

        BigDecimal subtotal = itensRestantes.stream()
                .map(item -> item.getProduto().getPreco()
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int quantidadeTotal = itensRestantes.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();

        String subtotalFormatado = subtotal
                .setScale(2, RoundingMode.HALF_UP)
                .toString()
                .replace('.', ',');

        Integer quantidadeItem = null;
        Boolean itemRemovido = null;
        if (itemAlterado[0] != null) {
            quantidadeItem = itemAlterado[0].getQuantidade();
            itemRemovido = false;
        } else {
            quantidadeItem = 0;
            itemRemovido = true;
        }

        resposta.put("success", true);
        resposta.put("itemId", itemId);
        resposta.put("subtotal", subtotal);
        resposta.put("subtotalFormatado", subtotalFormatado);
        resposta.put("quantidadeCarrinho", quantidadeTotal);
        resposta.put("quantidadeItem", quantidadeItem);
        resposta.put("itemRemovido", itemRemovido);

        return resposta;
    }

    @PostMapping("/carrinho/limpar")
    public String limparCarrinho(HttpSession session,
                                 HttpServletRequest request) {

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return voltarParaPaginaAnterior(request, "/");
        }

        List<ItemCarrinho> itens = itemCarrinhoRepository.findByUsuario(usuarioLogado);
        itemCarrinhoRepository.deleteAll(itens);

        return voltarParaPaginaAnterior(request, "/");
    }

    @PostMapping("/carrinho/limpar-ajax")
    @ResponseBody
    public Map<String, Object> limparCarrinhoAjax(HttpSession session) {

        Map<String, Object> resposta = new HashMap<>();

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            resposta.put("success", false);
            resposta.put("message", "Usuário não autenticado");
            return resposta;
        }

        List<ItemCarrinho> itens = itemCarrinhoRepository.findByUsuario(usuarioLogado);
        itemCarrinhoRepository.deleteAll(itens);

        BigDecimal subtotal = BigDecimal.ZERO;
        String subtotalFormatado = subtotal
                .setScale(2, RoundingMode.HALF_UP)
                .toString()
                .replace('.', ',');

        resposta.put("success", true);
        resposta.put("subtotal", subtotal);
        resposta.put("subtotalFormatado", subtotalFormatado);
        resposta.put("quantidadeCarrinho", 0);

        return resposta;
    }

    private String voltarParaPaginaAnterior(HttpServletRequest request, String defaultUrl) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "redirect:" + defaultUrl;
        }
        return "redirect:" + referer;
    }
}
