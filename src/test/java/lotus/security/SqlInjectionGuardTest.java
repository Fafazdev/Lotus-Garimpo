package lotus.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlInjectionGuardTest {

    private final SqlInjectionGuard sqlInjectionGuard = new SqlInjectionGuard();

    @Test
    void shouldAllowRegularApplicationText() {
        assertFalse(sqlInjectionGuard.containsRisk("Vestido floral azul"));
        assertFalse(sqlInjectionGuard.containsRisk("categoria feminina"));
        assertFalse(sqlInjectionGuard.containsRisk("M"));
    }

    @Test
    void shouldIgnoreNullOrBlankValues() {
        assertFalse(sqlInjectionGuard.containsRisk(null));
        assertFalse(sqlInjectionGuard.containsRisk("   "));
    }

    @Test
    void shouldBlockBooleanBasedInjectionPayload() {
        assertTrue(sqlInjectionGuard.containsRisk("' OR 1=1 --"));
    }

    @Test
    void shouldBlockUnionBasedInjectionPayload() {
        assertTrue(sqlInjectionGuard.containsRisk("camisa'; UNION SELECT senha FROM usuario --"));
    }

    @Test
    void shouldThrowExceptionForSuspiciousInput() {
        assertThrows(SqlInjectionAttemptException.class,
                () -> sqlInjectionGuard.validate("' OR 1=1 --", "email"));
    }
}