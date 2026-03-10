package lotus.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MercadoPagoPixService {

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.base-url:https://api.mercadopago.com}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public PixPaymentResponse criarCobrancaPix(BigDecimal valorTotal, String descricao, String payerEmail) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Access token do Mercado Pago não configurado.");
        }

        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da cobrança Pix inválido.");
        }

        if (payerEmail == null || payerEmail.isBlank()) {
            throw new IllegalArgumentException("E-mail do pagador (payer) é obrigatório para o Pix.");
        }

        String url = baseUrl + "/v1/payments";

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", valorTotal);
        body.put("description", descricao != null ? descricao : "Pedido Lotus Garimpo");
        body.put("payment_method_id", "pix");

        Map<String, Object> payer = new HashMap<>();
        payer.put("email", payerEmail);
        body.put("payer", payer);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        // Header exigido pela API do Mercado Pago para identificar a operação
        headers.add("X-Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        } catch (HttpClientErrorException e) {
            System.out.println("Erro HTTP ao chamar Mercado Pago:");
            System.out.println("Status: " + e.getStatusCode());
            System.out.println("Corpo: " + e.getResponseBodyAsString());
            throw e;
        }
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("Resposta vazia do Mercado Pago ao criar cobrança Pix.");
        }

        Object idObj = responseBody.get("id");
        Long paymentId = (idObj instanceof Number) ? ((Number) idObj).longValue() : null;

        String qrCode = null;
        String qrCodeBase64 = null;

        Object poiObj = responseBody.get("point_of_interaction");
        if (poiObj instanceof Map<?, ?> poi) {
            Object txDataObj = poi.get("transaction_data");
            if (txDataObj instanceof Map<?, ?> tx) {
                Object qr = tx.get("qr_code");
                Object qrB64 = tx.get("qr_code_base64");
                qrCode = qr != null ? qr.toString() : null;
                qrCodeBase64 = qrB64 != null ? qrB64.toString() : null;
            }
        }

        return new PixPaymentResponse(paymentId, qrCode, qrCodeBase64);
    }

    public static class PixPaymentResponse {
        private final Long paymentId;
        private final String qrCode;
        private final String qrCodeBase64;

        public PixPaymentResponse(Long paymentId, String qrCode, String qrCodeBase64) {
            this.paymentId = paymentId;
            this.qrCode = qrCode;
            this.qrCodeBase64 = qrCodeBase64;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public String getQrCode() {
            return qrCode;
        }

        public String getQrCodeBase64() {
            return qrCodeBase64;
        }
    }

    public String consultarStatusPagamento(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("paymentId obrigat\u00f3rio");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Access token do Mercado Pago n\u00e3o configurado");
        }

        String url = baseUrl + "/v1/payments/" + paymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

        Object statusObj = response.getBody() != null ? response.getBody().get("status") : null;
        return statusObj != null ? statusObj.toString() : null;
    }
}
