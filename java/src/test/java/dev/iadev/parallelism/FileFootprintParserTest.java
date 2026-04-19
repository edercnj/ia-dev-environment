package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class FileFootprintParserTest {

    private static final Path FIXTURES =
            Paths.get("src/test/resources/fixtures/parallelism");

    @Test
    void parse_nullInput_returnsEmpty() {
        FileFootprint fp = FileFootprintParser.parse(null);
        assertThat(fp).isEqualTo(FileFootprint.EMPTY);
        assertThat(fp.isEmpty()).isTrue();
    }

    @Test
    void parse_blankInput_returnsEmpty() {
        assertThat(FileFootprintParser.parse("   \n  \n")).isEqualTo(FileFootprint.EMPTY);
    }

    @Test
    void parse_simpleFixture_yieldsSingleWrite() throws IOException {
        FileFootprint fp = FileFootprintParser.parse(read("footprint-simple.md"));
        assertThat(fp.writes()).containsExactly("src/Foo.java");
        assertThat(fp.reads()).isEmpty();
        assertThat(fp.regens()).isEmpty();
    }

    @Test
    void parse_emptyFixture_yieldsEmptyFootprint_blockPresent() throws IOException {
        FileFootprint fp = FileFootprintParser.parse(read("footprint-empty.md"));
        assertThat(fp.isEmpty()).isTrue();
    }

    @Test
    void parse_fullFixture_yieldsAllThreeSectionsSorted() throws IOException {
        FileFootprint fp = FileFootprintParser.parse(read("footprint-full.md"));
        assertThat(fp.writes()).containsExactly(
                "src/main/A.java", "src/main/B.java", "src/main/C.java");
        assertThat(fp.reads()).containsExactly(
                "skills/knowledge-packs/parallelism-heuristics/SKILL.md");
        assertThat(fp.regens()).containsExactly(".claude/skills/x-foo/SKILL.md");
    }

    @Test
    void parse_partialFixture_skipsAbsentSubSection() throws IOException {
        FileFootprint fp = FileFootprintParser.parse(read("footprint-partial.md"));
        assertThat(fp.writes()).containsExactly("pom.xml");
        assertThat(fp.reads()).isEmpty();
        assertThat(fp.regens()).containsExactly(".claude/skills/x-bar/SKILL.md");
    }

    @Test
    void parse_legacyFixture_returnsEmptyWithoutThrowing() throws IOException {
        FileFootprint fp = FileFootprintParser.parse(read("footprint-legacy.md"));
        assertThat(fp).isEqualTo(FileFootprint.EMPTY);
    }

    @Test
    void parse_determinism_twoInvocationsProduceIdenticalFootprint() throws IOException {
        String body = read("footprint-full.md");
        FileFootprint first = FileFootprintParser.parse(body);
        FileFootprint second = FileFootprintParser.parse(body);
        assertThat(first).isEqualTo(second);
        assertThat(first.writes()).containsExactlyElementsOf(second.writes());
    }

    @Test
    void ofWrites_populatesOnlyWriteSubSection() {
        FileFootprint fp = FileFootprint.ofWrites(java.util.Set.of("b", "a"));
        assertThat(fp.writes()).containsExactly("a", "b");
        assertThat(fp.reads()).isEmpty();
        assertThat(fp.regens()).isEmpty();
    }

    private static String read(String name) throws IOException {
        return Files.readString(FIXTURES.resolve(name));
    }
}
