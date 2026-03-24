package lotus.security;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Map;

@ControllerAdvice
public class SqlInjectionRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private final SqlInjectionGuard sqlInjectionGuard;

    public SqlInjectionRequestBodyAdvice(SqlInjectionGuard sqlInjectionGuard) {
        this.sqlInjectionGuard = sqlInjectionGuard;
    }

    @Override
    public boolean supports(MethodParameter methodParameter,
                            Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body,
                                HttpInputMessage inputMessage,
                                MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        validateValue(body, null);
        return body;
    }

    private void validateValue(Object value, String fieldName) {
        if (value == null) {
            return;
        }

        if (value instanceof String stringValue) {
            sqlInjectionGuard.validate(stringValue, fieldName);
            return;
        }

        if (value instanceof Map<?, ?> mapValue) {
            mapValue.forEach((key, mapEntryValue) -> {
                String nestedFieldName = key == null ? fieldName : key.toString();
                validateValue(mapEntryValue, nestedFieldName);
            });
            return;
        }

        if (value instanceof Iterable<?> iterableValue) {
            for (Object item : iterableValue) {
                validateValue(item, fieldName);
            }
            return;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                validateValue(Array.get(value, index), fieldName);
            }
        }
    }
}