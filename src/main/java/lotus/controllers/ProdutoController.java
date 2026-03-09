package lotus.controllers;

import lotus.model.Produto;
import lotus.model.Usuario;
import lotus.repositories.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
public class ProdutoController {

    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping("/produtos")
    public String produto(Model model) {
        List<Produto> produtos = produtoRepository.findAll();
        model.addAttribute("produtos", produtos);
        return "produtos"; 
    }

    @GetMapping("/produto/sugestoes")
    @ResponseBody
    public List<String> buscarSugestoes(@RequestParam(value = "q", defaultValue = "") String query) {
        String termoDigitado = query == null ? "" : query.trim();
        String termoNormalizado = normalize(termoDigitado);

        if (termoNormalizado.isEmpty()) {
            return List.of();
        }

        List<Produto> encontrados = produtoRepository
                .findTop30ByNomeContainingIgnoreCaseOrDescricaoContainingIgnoreCaseOrCategoriaContainingIgnoreCaseOrTamanhoContainingIgnoreCaseOrderByIdDesc(
                        termoDigitado,
                        termoDigitado,
                        termoDigitado,
                        termoDigitado
                );

        Set<String> sugestoes = new LinkedHashSet<>();
        for (Produto produto : encontrados) {
            addSuggestionIfMatches(sugestoes, produto.getNome(), termoNormalizado);
            addSuggestionIfMatches(sugestoes, produto.getCategoria(), termoNormalizado);
            addSuggestionIfMatches(sugestoes, produto.getTamanho(), termoNormalizado);

            if (sugestoes.size() >= 10) {
                break;
            }
        }

        return new ArrayList<>(sugestoes);
    }

    @GetMapping("/produto/categoria-por-termo")
    @ResponseBody
    public Map<String, String> buscarCategoriaPorTermo(@RequestParam(value = "q", defaultValue = "") String query) {
        String termoDigitado = query == null ? "" : query.trim();
        String termoNormalizado = normalize(termoDigitado);

        if (termoNormalizado.isEmpty()) {
            return Map.of("categoria", "");
        }

        List<Produto> encontrados = produtoRepository
                .findTop30ByNomeContainingIgnoreCaseOrDescricaoContainingIgnoreCaseOrCategoriaContainingIgnoreCaseOrTamanhoContainingIgnoreCaseOrderByIdDesc(
                        termoDigitado,
                        termoDigitado,
                        termoDigitado,
                        termoDigitado
                );

        String categoriaEncontrada = "";

        for (Produto produto : encontrados) {
            if (containsNormalized(produto.getNome(), termoNormalizado)) {
                String categoria = safeValue(produto.getCategoria());
                if (!categoria.isEmpty()) {
                    categoriaEncontrada = categoria;
                    break;
                }
            }
        }

        if (categoriaEncontrada.isEmpty()) {
            for (Produto produto : encontrados) {
                String categoria = safeValue(produto.getCategoria());
                if (categoria.isEmpty()) {
                    continue;
                }

                if (containsNormalized(produto.getDescricao(), termoNormalizado)
                        || containsNormalized(produto.getCategoria(), termoNormalizado)
                        || containsNormalized(produto.getTamanho(), termoNormalizado)) {
                    categoriaEncontrada = categoria;
                    break;
                }
            }
        }

        return Map.of("categoria", categoriaEncontrada);
    }

    private static void addSuggestionIfMatches(Set<String> suggestions, String rawValue, String normalizedTerm) {
        if (rawValue == null) {
            return;
        }

        String value = rawValue.trim();
        if (value.isEmpty()) {
            return;
        }

        if (normalize(value).contains(normalizedTerm)) {
            suggestions.add(value);
        }
    }

    private static boolean containsNormalized(String value, String normalizedTerm) {
        if (value == null || normalizedTerm == null || normalizedTerm.isEmpty()) {
            return false;
        }
        return normalize(value).contains(normalizedTerm);
    }

    private static String safeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        String safe = value == null ? "" : value;
        return Normalizer
                .normalize(safe, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .trim();
    }

    @GetMapping("/produto/editar/{id}")
    public String editarProdutoForm(@PathVariable("id") Long id, Model model, HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        Optional<Produto> optProduto = produtoRepository.findById(id);
        if (optProduto.isEmpty()) {
            return "redirect:/produtos";
        }

        Produto produto = optProduto.get();
        if (produto.getUsuario() == null || !usuarioLogado.getId().equals(produto.getUsuario().getId())) {
            return "redirect:/produtos";
        }

        model.addAttribute("produto", produto);
        return "editar-produto";
    }

    @PostMapping("/produto/editar")
    public String editarProduto(@RequestParam("id") Long id,
                                @RequestParam("nome") String nome,
                                @RequestParam("descricao") String descricao,
                                @RequestParam("preco") BigDecimal preco,
                                @RequestParam("tamanho") String tamanho,
                                @RequestParam("categoria") String categoria,
                                @RequestParam(value = "imagem", required = false) MultipartFile imagem,
                                @RequestParam(value = "origem", required = false) String origem,
                                HttpSession session) {

        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        Optional<Produto> optProduto = produtoRepository.findById(id);
        if (optProduto.isEmpty()) {
            return "redirect:/produtos";
        }

        Produto produto = optProduto.get();
        if (produto.getUsuario() == null || !usuarioLogado.getId().equals(produto.getUsuario().getId())) {
            return "redirect:/produtos";
        }

        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setTamanho(tamanho);
        produto.setCategoria(categoria);

        if (imagem != null && !imagem.isEmpty()) {
            try {
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("src/main/resources/static/imagens").toAbsolutePath().normalize();
                java.nio.file.Files.createDirectories(uploadDir);
                String filename = System.currentTimeMillis() + "_" + imagem.getOriginalFilename();
                java.nio.file.Path filePath = uploadDir.resolve(filename);
                imagem.transferTo(filePath.toFile());
                produto.setImagem("/imagens/" + filename);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        produtoRepository.save(produto);

        // Após editar, redireciona com flag de sucesso para exibir alerta na tela
        if ("home".equalsIgnoreCase(origem)) {
            return "redirect:/?sucesso=produtoAtualizado";
        }
        return "redirect:/produtos?sucesso=produtoAtualizado";
    }

    @PostMapping("/produto/excluir/{id}")
    public String excluirProduto(@PathVariable("id") Long id,
                                 @RequestParam(value = "origem", required = false) String origem,
                                 HttpSession session) {
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        Optional<Produto> optProduto = produtoRepository.findById(id);
        if (optProduto.isEmpty()) {
            return "redirect:/produtos";
        }

        Produto produto = optProduto.get();
        if (produto.getUsuario() != null && usuarioLogado.getId().equals(produto.getUsuario().getId())) {
            produtoRepository.delete(produto);
        }

        if ("home".equalsIgnoreCase(origem)) {
            return "redirect:/";
        }
        return "redirect:/produtos";
    }
}