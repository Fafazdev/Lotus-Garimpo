package lotus.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@ControllerAdvice
public class SqlInjectionExceptionHandler {

    @ExceptionHandler(SqlInjectionAttemptException.class)
    public Object handleSqlInjectionAttempt(SqlInjectionAttemptException exception, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sucesso", false,
                            "mensagem", "Entrada invalida detectada."
                    ));
        }

        return new ModelAndView("redirect:" + resolveRedirectPath(request.getRequestURI()));
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        String requestedWith = request.getHeader("X-Requested-With");

        return (uri != null && uri.startsWith("/api/"))
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE))
                || (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private String resolveRedirectPath(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "/?erro=entradaInvalida";
        }

        if (requestUri.startsWith("/perfil") || requestUri.startsWith("/peca/")) {
            return "/perfil?erro=entradaInvalida";
        }

        if (requestUri.startsWith("/produto")) {
            return "/produtos?erro=entradaInvalida";
        }

        return "/?erro=entradaInvalida";
    }
}