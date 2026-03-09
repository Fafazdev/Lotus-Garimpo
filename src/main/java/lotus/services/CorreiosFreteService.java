package lotus.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

@Service
public class CorreiosFreteService {

    // Webservice oficial dos Correios costuma responder melhor em HTTP simples
    private static final String CORREIOS_URL = "http://ws.correios.com.br/calculador/CalcPrecoPrazo.aspx";

    @Value("${lotus.frete.cep-origem:00000000}")
    private String cepOrigem;

    @Value("${lotus.frete.codigo-servico:04510}") // 04510 = PAC sem contrato
    private String codigoServico;

    private final RestTemplate restTemplate;

    public CorreiosFreteService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public BigDecimal calcularFrete(String cepDestino, BigDecimal pesoKg, BigDecimal valorDeclarado) {
        try {
            if (cepDestino == null) {
                return BigDecimal.ZERO;
            }

            String cepDestinoLimpo = cepDestino.replaceAll("\\D", "");
            String cepOrigemLimpo = cepOrigem != null ? cepOrigem.replaceAll("\\D", "") : "";

            if (cepOrigemLimpo.isEmpty() || cepOrigemLimpo.length() != 8 || cepDestinoLimpo.length() != 8) {
                return BigDecimal.ZERO;
            }

            String peso = "1";
            if (pesoKg != null && pesoKg.compareTo(BigDecimal.ZERO) > 0) {
                String bruto = pesoKg.toPlainString();
                peso = bruto.replace(".", ","); // Correios usam vírgula como separador decimal
            }

            String valor = "0";
            if (valorDeclarado != null && valorDeclarado.compareTo(BigDecimal.ZERO) > 0) {
                String brutoValor = valorDeclarado.toPlainString();
                valor = brutoValor.replace(".", ",");
            }

            String url = UriComponentsBuilder.fromHttpUrl(CORREIOS_URL)
                    .queryParam("nCdEmpresa", "")
                    .queryParam("sDsSenha", "")
                    .queryParam("nCdServico", codigoServico)
                    .queryParam("sCepOrigem", cepOrigemLimpo)
                    .queryParam("sCepDestino", cepDestinoLimpo)
                    .queryParam("nVlPeso", peso)
                    .queryParam("nCdFormato", "1")
                    .queryParam("nVlComprimento", "20")
                    .queryParam("nVlAltura", "5")
                    .queryParam("nVlLargura", "15")
                    .queryParam("nVlDiametro", "0")
                    .queryParam("sCdMaoPropria", "N")
                    .queryParam("nVlValorDeclarado", valor)
                    .queryParam("sCdAvisoRecebimento", "N")
                    .queryParam("StrRetorno", "xml")
                    .toUriString();

            String xmlResponse = restTemplate.getForObject(url, String.class);
            if (xmlResponse == null || xmlResponse.isBlank()) {
                return BigDecimal.ZERO;
            }

            // Log simples para ajudar a diagnosticar erros do Correios
            System.out.println("[CorreiosFreteService] URL chamada: " + url);
            System.out.println("[CorreiosFreteService] Resposta XML: " + xmlResponse);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes()));

            NodeList valorNodes = doc.getElementsByTagName("Valor");
            if (valorNodes.getLength() == 0) {
                return BigDecimal.ZERO;
            }

            String valorTexto = valorNodes.item(0).getTextContent(); // exemplo "19,90"
            if (valorTexto == null || valorTexto.isBlank()) {
                return BigDecimal.ZERO;
            }

            // Também tentar extrair possível código/mensagem de erro
            NodeList erroNodes = doc.getElementsByTagName("Erro");
            NodeList msgErroNodes = doc.getElementsByTagName("MsgErro");
            if (erroNodes.getLength() > 0) {
                String codErro = erroNodes.item(0).getTextContent();
                String msgErro = msgErroNodes.getLength() > 0 ? msgErroNodes.item(0).getTextContent() : "";
                System.out.println("[CorreiosFreteService] Erro Correios: " + codErro + " - " + msgErro);
            }

            // Correios devolve com vírgula decimal, convertemos para BigDecimal com ponto
            String normalizado = valorTexto.replace(".", "").replace(",", ".");
            return new BigDecimal(normalizado);
        } catch (Exception e) {
            System.out.println("[CorreiosFreteService] Exceção ao chamar Correios: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}