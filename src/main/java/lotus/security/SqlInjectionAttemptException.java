package lotus.security;

public class SqlInjectionAttemptException extends RuntimeException {

    public SqlInjectionAttemptException(String fieldName) {
        super(fieldName == null || fieldName.isBlank()
                ? "Entrada invalida detectada."
                : "Entrada invalida detectada no campo: " + fieldName);
    }
}