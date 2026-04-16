package dev.iadev.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test (story-0044-0002): ensures the two
 * {@code @Deprecated(forRemoval = true)} overloads of
 * {@code resolveResourcesRoot} no longer exist on
 * {@link ResourceResolver}.
 *
 * <p>This test guards against accidental re-introduction of the
 * deprecated API and documents the removal commitment in executable
 * form — a CI-level contract between the module and its callers.</p>
 */
@DisplayName("ResourceResolver — deprecated overloads removed")
class ResourceResolverDeprecatedRemovedTest {

    @Test
    @DisplayName("resolveResourcesRoot(String) is absent")
    void singleArgOverload_whenLookedUp_throwsNoSuchMethod() {
        assertThatThrownBy(() ->
                ResourceResolver.class.getDeclaredMethod(
                        "resolveResourcesRoot", String.class))
                .isInstanceOf(NoSuchMethodException.class);
    }

    @Test
    @DisplayName("resolveResourcesRoot(String, int) is absent")
    void twoArgOverload_whenLookedUp_throwsNoSuchMethod() {
        assertThatThrownBy(() ->
                ResourceResolver.class.getDeclaredMethod(
                        "resolveResourcesRoot",
                        String.class, int.class))
                .isInstanceOf(NoSuchMethodException.class);
    }
}
