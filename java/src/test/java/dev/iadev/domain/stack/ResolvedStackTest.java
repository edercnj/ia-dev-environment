package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResolvedStack")
class ResolvedStackTest {

    @Test
    @DisplayName("creates record with all fields")
    void constructor_allFields_allAccessible() {
        var stack = new ResolvedStack(
                "compile", "build", "test", "coverage",
                ".java", "pom.xml", "maven",
                8080, "/q/health", "eclipse-temurin:21-jre-alpine",
                false, "api", List.of("openapi"));

        assertThat(stack.compileCmd()).isEqualTo("compile");
        assertThat(stack.buildCmd()).isEqualTo("build");
        assertThat(stack.testCmd()).isEqualTo("test");
        assertThat(stack.coverageCmd()).isEqualTo("coverage");
        assertThat(stack.fileExtension()).isEqualTo(".java");
        assertThat(stack.buildFile()).isEqualTo("pom.xml");
        assertThat(stack.packageManager()).isEqualTo("maven");
        assertThat(stack.defaultPort()).isEqualTo(8080);
        assertThat(stack.healthPath()).isEqualTo("/q/health");
        assertThat(stack.dockerBaseImage())
                .isEqualTo("eclipse-temurin:21-jre-alpine");
        assertThat(stack.nativeSupported()).isFalse();
        assertThat(stack.projectType()).isEqualTo("api");
        assertThat(stack.protocols()).containsExactly("openapi");
    }

    @Test
    @DisplayName("protocols list is immutable copy")
    void constructor_mutableProtocols_copiedImmutable() {
        var mutableList = new ArrayList<>(List.of("openapi"));
        var stack = new ResolvedStack(
                "", "", "", "", "", "", "",
                0, "", "", false, "", mutableList);

        mutableList.add("grpc");

        assertThat(stack.protocols()).containsExactly("openapi");
        assertThatThrownBy(() -> stack.protocols().add("proto3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("equals and hashCode work for identical records")
    void equals_sameValues_equal() {
        var a = new ResolvedStack(
                "c", "b", "t", "cv", ".java", "pom.xml", "mvn",
                8080, "/health", "img", false, "api", List.of("openapi"));
        var b = new ResolvedStack(
                "c", "b", "t", "cv", ".java", "pom.xml", "mvn",
                8080, "/health", "img", false, "api", List.of("openapi"));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
