package dev.iadev.cli;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JLineTerminalProvider")
class JLineTerminalProviderTest {

    @Mock
    private LineReader lineReader;

    private JLineTerminalProvider createProvider() {
        var sw = new StringWriter();
        return new JLineTerminalProvider(
                lineReader, new PrintWriter(sw, true));
    }

    private JLineTerminalProvider createProviderWithWriter(
            PrintWriter writer) {
        return new JLineTerminalProvider(lineReader, writer);
    }

    @Nested
    @DisplayName("readLine")
    class ReadLine {

        @Test
        @DisplayName("readLine_normalInput_returnsText")
        void readLine_normalInput_returnsText() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("hello");
            var provider = createProvider();

            assertThat(provider.readLine("prompt"))
                    .isEqualTo("hello");
        }

        @Test
        @DisplayName("readLine_userInterrupt_throwsCancelled")
        void readLine_userInterrupt_throwsCancelled() {
            when(lineReader.readLine(anyString()))
                    .thenThrow(new UserInterruptException(""));
            var provider = createProvider();

            assertThatThrownBy(() -> provider.readLine("prompt"))
                    .isInstanceOf(
                            GenerationCancelledException.class)
                    .hasMessage(
                            InteractivePrompter.CANCELLED_BY_USER);
        }

        @Test
        @DisplayName("readLine_endOfFile_throwsCancelled")
        void readLine_endOfFile_throwsCancelled() {
            when(lineReader.readLine(anyString()))
                    .thenThrow(new EndOfFileException());
            var provider = createProvider();

            assertThatThrownBy(() -> provider.readLine("prompt"))
                    .isInstanceOf(
                            GenerationCancelledException.class);
        }
    }

    @Nested
    @DisplayName("readLineWithValidation")
    class ReadLineWithValidation {

        @Test
        @DisplayName("readLineWithValidation_validFirst_returnsIt")
        void readLineWithValidation_validFirst_returnsIt() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("valid-name");
            var provider = createProvider();

            String result = provider.readLineWithValidation(
                    "prompt",
                    s -> s.length() >= 3,
                    "Too short");

            assertThat(result).isEqualTo("valid-name");
        }

        @Test
        @DisplayName(
                "readLineWithValidation_invalidThenValid_retries")
        void readLineWithValidation_invalidThenValid_retries() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("ab")
                    .thenReturn("valid");
            var sw = new StringWriter();
            var pw = new PrintWriter(sw, true);
            var provider = createProviderWithWriter(pw);

            String result = provider.readLineWithValidation(
                    "prompt",
                    s -> s.length() >= 3,
                    "Too short");

            assertThat(result).isEqualTo("valid");
            assertThat(sw.toString()).contains("Too short");
        }
    }

    @Nested
    @DisplayName("selectFromList")
    class SelectFromList {

        @Test
        @DisplayName("selectFromList_validChoice_returnsOption")
        void selectFromList_validChoice_returnsOption() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("2");
            var provider = createProvider();

            String result = provider.selectFromList(
                    "pick:", List.of("a", "b", "c"), 0);

            assertThat(result).isEqualTo("b");
        }

        @Test
        @DisplayName("selectFromList_emptyInput_returnsDefault")
        void selectFromList_emptyInput_returnsDefault() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("");
            var provider = createProvider();

            String result = provider.selectFromList(
                    "pick:", List.of("a", "b"), 1);

            assertThat(result).isEqualTo("b");
        }

        @Test
        @DisplayName(
                "selectFromList_invalidThenValid_retriesAndReturns")
        void selectFromList_invalidThenValid_retriesAndReturns() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("99")
                    .thenReturn("1");
            var provider = createProvider();

            String result = provider.selectFromList(
                    "pick:", List.of("a", "b"), 0);

            assertThat(result).isEqualTo("a");
        }

        @Test
        @DisplayName(
                "selectFromList_nonNumeric_retriesUntilValid")
        void selectFromList_nonNumeric_retriesUntilValid() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("abc")
                    .thenReturn("1");
            var provider = createProvider();

            String result = provider.selectFromList(
                    "pick:", List.of("x", "y"), 0);

            assertThat(result).isEqualTo("x");
        }
    }

    @Nested
    @DisplayName("selectMultiple")
    class SelectMultiple {

        @Test
        @DisplayName("selectMultiple_validCommaList_returnsSelected")
        void selectMultiple_validCommaList_returnsSelected() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("1,3");
            var provider = createProvider();

            List<String> result = provider.selectMultiple(
                    "pick:", List.of("a", "b", "c"),
                    List.of("a"));

            assertThat(result).containsExactly("a", "c");
        }

        @Test
        @DisplayName("selectMultiple_emptyInput_returnsDefaults")
        void selectMultiple_emptyInput_returnsDefaults() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("");
            var provider = createProvider();

            List<String> result = provider.selectMultiple(
                    "pick:", List.of("a", "b"),
                    List.of("a"));

            assertThat(result).containsExactly("a");
        }

        @Test
        @DisplayName("selectMultiple_duplicateNumbers_deduplicates")
        void selectMultiple_duplicateNumbers_deduplicates() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("1,1,2");
            var provider = createProvider();

            List<String> result = provider.selectMultiple(
                    "pick:", List.of("a", "b"),
                    List.of());

            assertThat(result).containsExactly("a", "b");
        }

        @Test
        @DisplayName(
                "selectMultiple_allInvalidNumbers_retriesUntilValid")
        void selectMultiple_allInvalidNumbers_retriesUntilValid() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("abc")
                    .thenReturn("1");
            var provider = createProvider();

            List<String> result = provider.selectMultiple(
                    "pick:", List.of("x"),
                    List.of());

            assertThat(result).containsExactly("x");
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("confirm_yInput_returnsTrue")
        void confirm_yInput_returnsTrue() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("y");
            var provider = createProvider();

            assertThat(provider.confirm("proceed?", false))
                    .isTrue();
        }

        @Test
        @DisplayName("confirm_yesInput_returnsTrue")
        void confirm_yesInput_returnsTrue() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("yes");
            var provider = createProvider();

            assertThat(provider.confirm("proceed?", false))
                    .isTrue();
        }

        @Test
        @DisplayName("confirm_nInput_returnsFalse")
        void confirm_nInput_returnsFalse() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("n");
            var provider = createProvider();

            assertThat(provider.confirm("proceed?", true))
                    .isFalse();
        }

        @Test
        @DisplayName("confirm_emptyInput_returnsDefault")
        void confirm_emptyInput_returnsDefault() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("");
            var provider = createProvider();

            assertThat(provider.confirm("proceed?", true))
                    .isTrue();
        }

        @Test
        @DisplayName("confirm_emptyInputDefaultFalse_returnsFalse")
        void confirm_emptyInputDefaultFalse_returnsFalse() {
            when(lineReader.readLine(anyString()))
                    .thenReturn("");
            var provider = createProvider();

            assertThat(provider.confirm("proceed?", false))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("display")
    class Display {

        @Test
        @DisplayName("display_writesMessage")
        void display_writesMessage() {
            var sw = new StringWriter();
            var pw = new PrintWriter(sw, true);
            var provider = createProviderWithWriter(pw);

            provider.display("Hello World");

            assertThat(sw.toString()).contains("Hello World");
        }
    }
}
