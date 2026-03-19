package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LanguageCommandSet")
class LanguageCommandSetTest {

    @Test
    @DisplayName("creates record with all fields")
    void constructor_allFields_allAccessible() {
        var set = new LanguageCommandSet(
                "compile", "build", "test", "coverage",
                ".java", "pom.xml", "maven");

        assertThat(set.compileCmd()).isEqualTo("compile");
        assertThat(set.buildCmd()).isEqualTo("build");
        assertThat(set.testCmd()).isEqualTo("test");
        assertThat(set.coverageCmd()).isEqualTo("coverage");
        assertThat(set.fileExtension()).isEqualTo(".java");
        assertThat(set.buildFile()).isEqualTo("pom.xml");
        assertThat(set.packageManager()).isEqualTo("maven");
    }

    @Test
    @DisplayName("equals and hashCode work for identical records")
    void equals_sameValues_equal() {
        var a = new LanguageCommandSet(
                "c", "b", "t", "cv", ".java", "pom.xml", "maven");
        var b = new LanguageCommandSet(
                "c", "b", "t", "cv", ".java", "pom.xml", "maven");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("not equal when fields differ")
    void equals_differentValues_notEqual() {
        var a = new LanguageCommandSet(
                "c", "b", "t", "cv", ".java", "pom.xml", "maven");
        var b = new LanguageCommandSet(
                "c", "b", "t", "cv", ".kt", "build.gradle.kts", "gradle");

        assertThat(a).isNotEqualTo(b);
    }
}
