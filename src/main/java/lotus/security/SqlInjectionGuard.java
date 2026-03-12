package lotus.security;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SqlInjectionGuard {

    private static final List<Pattern> SQL_INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)(?:')\\s*(?:or|and)\\s+(?:'[^']*'\\s*=\\s*'[^']*'|\\d+\\s*=\\s*\\d+)"),
            Pattern.compile("(?i)\\b(?:or|and)\\b\\s+\\d+\\s*=\\s*\\d+"),
            Pattern.compile("(?i)\\bunion\\b\\s+\\bselect\\b"),
            Pattern.compile("(?i);\\s*(?:select|insert|update|delete|drop|alter|create|truncate|exec(?:ute)?)\\b"),
            Pattern.compile("(?i)(?:--|/\\*|\\*/)"),
            Pattern.compile("(?i)\\b(?:information_schema|xp_cmdshell|@@version|sleep|benchmark|pg_sleep)\\b\\s*\\(?")
    );

    public boolean containsRisk(String value) {
        if (value == null) {
            return false;
        }

        String normalizedValue = normalize(value);
        if (normalizedValue.isBlank()) {
            return false;
        }

        return SQL_INJECTION_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(normalizedValue).find());
    }

    public void validate(String value, String fieldName) {
        if (containsRisk(value)) {
            throw new SqlInjectionAttemptException(fieldName);
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\u0000', ' ')
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}