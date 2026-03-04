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
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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