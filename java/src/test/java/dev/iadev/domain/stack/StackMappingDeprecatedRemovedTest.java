package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reflection test asserting that the 4 symbols marked
 * {@code @Deprecated(forRemoval = true)} in story-0044-0001
 * have been removed from {@link StackMapping}.
 *
 * <p>This test is the verifiable output contract for the
 * removal of:
 * <ul>
 *   <li>{@code StackMapping.DATABASE_SETTINGS_MAP} (field)</li>
 *   <li>{@code StackMapping.CACHE_SETTINGS_MAP} (field)</li>
 *   <li>{@code StackMapping.getDatabaseSettingsKey(String)}
 *       (method)</li>
 *   <li>{@code StackMapping.getCacheSettingsKey(String)}
 *       (method)</li>
 * </ul>
 *
 * <p>Substitutes live in {@link DatabaseSettingsMapping}.
 */
@DisplayName("StackMapping — deprecated symbols removed (story-0044-0001)")
class StackMappingDeprecatedRemovedTest {

    @Test
    @DisplayName("DATABASE_SETTINGS_MAP field removed")
    void databaseSettingsMapField_notDeclared() {
        assertThatThrownBy(() ->
                StackMapping.class.getDeclaredField(
                        "DATABASE_SETTINGS_MAP"))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    @DisplayName("CACHE_SETTINGS_MAP field removed")
    void cacheSettingsMapField_notDeclared() {
        assertThatThrownBy(() ->
                StackMapping.class.getDeclaredField(
                        "CACHE_SETTINGS_MAP"))
                .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    @DisplayName("getDatabaseSettingsKey(String) method removed")
    void getDatabaseSettingsKeyMethod_notDeclared() {
        assertThatThrownBy(() ->
                StackMapping.class.getDeclaredMethod(
                        "getDatabaseSettingsKey", String.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    @DisplayName("getCacheSettingsKey(String) method removed")
    void getCacheSettingsKeyMethod_notDeclared() {
        assertThatThrownBy(() ->
                StackMapping.class.getDeclaredMethod(
                        "getCacheSettingsKey", String.class))
                .isInstanceOf(NoSuchMethodException.class);
    }
}
