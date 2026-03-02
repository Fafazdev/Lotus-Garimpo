package lotus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import lotus.model.Usuario;
import lotus.model.Produto;
import lotus.repositories.UsuarioRepository;
import lotus.repositories.ProdutoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // repositório para peças
    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping("/perfil")
    public String perfil(Model model, HttpSession session,
                         @org.springframework.web.bind.annotation.RequestParam(value = "pecaAdicionada", required = false) String pecaAdicionada) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        model.addAttribute("usuario", usuario);
        if (pecaAdicionada != null) {
            model.addAttribute("mensagemPeca", "Peça adicionada com sucesso!");
        }
        return "perfil";
    }

    @PostMapping("/perfil/completar")
    public String completarPerfil(
            @RequestParam("telefone") String telefone,
            @RequestParam("cpf") String cpf,
            @RequestParam("cep") String cep,
            @RequestParam("endereco") String endereco,
            @RequestParam("numero") String numero,
            @RequestParam("complemento") String complemento,
            @RequestParam("cidade") String cidade,
            @RequestParam("estado") String estado,
            HttpSession session) {
        
        // Busca o usuário logado na sessão
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        
        // Verifica se o usuário está autenticado
        if (usuarioLogado == null) {
            return "redirect:/";
        }

        // Atualiza todos os dados do usuário
        usuarioLogado.setTelefone(telefone);
        usuarioLogado.setCpf(cpf);
        usuarioLogado.setCep(cep);
        usuarioLogado.setEndereco(endereco);
        usuarioLogado.setNumero(numero);
        usuarioLogado.setComplemento(complemento);
        usuarioLogado.setCidade(cidade);
        usuarioLogado.setEstado(estado);

        // Salva no banco de dados
        usuarioRepository.save(usuarioLogado);

        // Atualiza a sessão
        session.setAttribute("usuarioLogado", usuarioLogado);

        return "redirect:/perfil?sucesso=true";
    }

    @GetMapping("/api/endereco-por-cep")
    @ResponseBody
    public Map<String, String> buscarEnderecoPorCep(@RequestParam("cep") String cep) {
        try {
            // Remove caracteres especiais do CEP
            cep = cep.replaceAll("[^0-9]", "");

            // Faz a requisição para a API ViaCEP
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://viacep.com.br/ws/" + cep + "/json/"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, String> resultado = mapper.readValue(response.body(), Map.class);
                
                // Verifica se o CEP é válido
                if (resultado.containsKey("erro")) {
                    Map<String, String> erro = new HashMap<>();
                    erro.put("erro", "CEP não encontrado");
                    return erro;
                }

                // Renomeia as chaves para nomes mais amigáveis
                Map<String, String> endereco = new HashMap<>();
                endereco.put("endereco", resultado.getOrDefault("logradouro", ""));
                endereco.put("bairro", resultado.getOrDefault("bairro", ""));
                endereco.put("cidade", resultado.getOrDefault("localidade", ""));
                endereco.put("estado", resultado.getOrDefault("uf", ""));
                
                return endereco;
            } else {
                Map<String, String> erro = new HashMap<>();
                erro.put("erro", "Erro ao buscar CEP");
                return erro;
            }
        } catch (Exception e) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", "Erro na requisição: " + e.getMessage());
            return erro;
        }
    }

    @PostMapping("/api/usuario/atualizar")
    @ResponseBody
    public Map<String, Object> atualizarUsuario(
            @org.springframework.web.bind.annotation.RequestBody Map<String, String> dados,
            HttpSession session) {
        
        Map<String, Object> resposta = new HashMap<>();
        
        try {
            // Busca o usuário logado na sessão
            Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
            
            // Verifica se o usuário está autenticado
            if (usuarioLogado == null) {
                resposta.put("sucesso", false);
                resposta.put("mensagem", "Usuário não autenticado");
                return resposta;
            }

            String campo = dados.get("campo");
            String valor = dados.get("valor");

            if (campo == null || valor == null) {
                resposta.put("sucesso", false);
                resposta.put("mensagem", "Campo ou valor não fornecido");
                return resposta;
            }

            // Atualiza o campo especificado
            switch(campo) {
                case "nome":
                    usuarioLogado.setNome(valor);
                    break;
                case "email":
                    usuarioLogado.setEmail(valor);
                    break;
                case "telefone":
                    usuarioLogado.setTelefone(valor);
                    break;
                case "cpf":
                    // verifica se já existe outro usuário com este CPF
                    Optional<Usuario> outroUsuario = usuarioRepository.findByCpf(valor);
                    if (outroUsuario.isPresent() && !outroUsuario.get().getId().equals(usuarioLogado.getId())) {
                        resposta.put("sucesso", false);
                        resposta.put("mensagem", "CPF já cadastrado em outra conta");
                        return resposta;
                    }
                    usuarioLogado.setCpf(valor);
                    break;
                case "cep":
                    usuarioLogado.setCep(valor);
                    break;
                case "endereco":
                    usuarioLogado.setEndereco(valor);
                    break;
                case "numero":
                    usuarioLogado.setNumero(valor);
                    break;
                case "complemento":
                    usuarioLogado.setComplemento(valor);
                    break;
                case "cidade":
                    usuarioLogado.setCidade(valor);
                    break;
                case "estado":
                    usuarioLogado.setEstado(valor);
                    break;
                default:
                    resposta.put("sucesso", false);
                    resposta.put("mensagem", "Campo desconhecido: " + campo);
                    return resposta;
            }

            // Salva no banco de dados
            usuarioRepository.save(usuarioLogado);

            // Atualiza a sessão
            session.setAttribute("usuarioLogado", usuarioLogado);
            
            resposta.put("sucesso", true);
            resposta.put("mensagem", "Campo atualizado com sucesso");
            
        } catch (Exception e) {
            resposta.put("sucesso", false);
            resposta.put("mensagem", "Erro ao atualizar: " + e.getMessage());
        }
        
        return resposta;
    }

    @PostMapping("/peca/adicionar")
    public String adicionarPeca(
            @RequestParam("nome") String nome,
            @RequestParam("descricao") String descricao,
            @RequestParam("preco") Double preco,
            @RequestParam("tamanho") String tamanho,
            @RequestParam("categoria") String categoria,
            @RequestParam("imagem") MultipartFile imagem,
            HttpSession session) {
        
        // Busca o usuário logado na sessão
        Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        
        // Verifica se o usuário está autenticado
        if (usuarioLogado == null) {
            return "redirect:/";
        }
        // apenas vendedores podem inserir peças
        if (usuarioLogado.getTipo() == null || usuarioLogado.getTipo() != 2) {
            return "redirect:/perfil";
        }

        // cria o objeto produto e preenche os campos
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setDescricao(descricao);
        // converte Double para BigDecimal
        produto.setPreco(preco != null ? java.math.BigDecimal.valueOf(preco) : null);
        produto.setTamanho(tamanho);
        produto.setCategoria(categoria);
        produto.setUsuario(usuarioLogado);

        // trata a imagem enviada pelo formulário
        if (imagem != null && !imagem.isEmpty()) {
            try {
                // usa caminho absoluto para garantir que sempre funcione
                java.nio.file.Path uploadDir = java.nio.file.Paths.get("src/main/resources/static/imagens").toAbsolutePath().normalize();
                java.nio.file.Files.createDirectories(uploadDir);
                String filename = System.currentTimeMillis() + "_" + imagem.getOriginalFilename();
                java.nio.file.Path filePath = uploadDir.resolve(filename);
                imagem.transferTo(filePath.toFile());
                // salva o caminho relativo que será usado nas páginas
                produto.setImagem("/imagens/" + filename);
            } catch (Exception e) {
                // se falhar no upload, apenas registra no console e continua sem imagem
                e.printStackTrace();
            }
        }

        // salva no banco de dados
        produtoRepository.save(produto);

        // opcional: imprimir para debug
        System.out.println("Peça adicionada:");
        System.out.println("Nome: " + nome);
        System.out.println("Descrição: " + descricao);
        System.out.println("Preço: " + preco);
        System.out.println("Tamanho: " + tamanho);
        System.out.println("Categoria: " + categoria);
        System.out.println("Usuário: " + usuarioLogado.getNome());

        // Redireciona de volta para o perfil mostrando mensagem de sucesso
        return "redirect:/perfil?pecaAdicionada=true";
    }
}
