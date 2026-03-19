package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MockTerminalProvider")
class MockTerminalProviderTest {

    @Nested
    @DisplayName("readLine")
    class ReadLine {

        @Test
        @DisplayName("readLine_withQueuedResponse_returnsResponse")
        void readLine_withQueuedResponse_returnsResponse() {
            var mock = new MockTerminalProvider()
                    .addReadLine("hello");

            assertThat(mock.readLine("prompt"))
                    .isEqualTo("hello");
        }

        @Test
        @DisplayName("readLine_multipleResponses_consumedInOrder")
        void readLine_multipleResponses_consumedInOrder() {
            var mock = new MockTerminalProvider()
                    .addReadLine("first")
                    .addReadLine("second");

            assertThat(mock.readLine("p1")).isEqualTo("first");
            assertThat(mock.readLine("p2")).isEqualTo("second");
        }

        @Test
        @DisplayName("readLine_noResponse_throwsIllegalState")
        void readLine_noResponse_throwsIllegalState() {
            var mock = new MockTerminalProvider();

            assertThatThrownBy(() -> mock.readLine("prompt"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("readLineWithValidation")
    class ReadLineWithValidation {

        @Test
        @DisplayName("readLineWithValidation_validInput_returnsIt")
        void readLineWithValidation_validInput_returnsIt() {
            var mock = new MockTerminalProvider()
                    .addReadLine("valid");

            String result = mock.readLineWithValidation(
                    "prompt",
                    input -> input.length() >= 3,
                    "Too short");

            assertThat(result).isEqualTo("valid");
        }

        @Test
        @DisplayName("readLineWithValidation_invalidThenValid_retries")
        void readLineWithValidation_invalidThenValid_retries() {
            var mock = new MockTerminalProvider()
                    .addReadLine("ab")
                    .addReadLine("valid");

            String result = mock.readLineWithValidation(
                    "prompt",
                    input -> input.length() >= 3,
                    "Too short");

            assertThat(result).isEqualTo("valid");
            assertThat(mock.getDisplayedMessages())
                    .contains("Too short");
        }
    }

    @Nested
    @DisplayName("selectFromList")
    class SelectFromList {

        @Test
        @DisplayName("selectFromList_returnsQueuedSelection")
        void selectFromList_returnsQueuedSelection() {
            var mock = new MockTerminalProvider()
                    .addSelect("option2");

            String result = mock.selectFromList(
                    "prompt",
                    List.of("option1", "option2"),
                    0);

            assertThat(result).isEqualTo("option2");
        }

        @Test
        @DisplayName("selectFromList_noResponse_throwsIllegalState")
        void selectFromList_noResponse_throwsIllegalState() {
            var mock = new MockTerminalProvider();

            assertThatThrownBy(() -> mock.selectFromList(
                    "prompt", List.of("a"), 0))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("selectMultiple")
    class SelectMultiple {

        @Test
        @DisplayName("selectMultiple_returnsQueuedSelection")
        void selectMultiple_returnsQueuedSelection() {
            var mock = new MockTerminalProvider()
                    .addMultiSelect(List.of("a", "c"));

            List<String> result = mock.selectMultiple(
                    "prompt",
                    List.of("a", "b", "c"),
                    List.of("a"));

            assertThat(result).containsExactly("a", "c");
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("confirm_true_returnsTrue")
        void confirm_true_returnsTrue() {
            var mock = new MockTerminalProvider()
                    .addConfirm(true);

            assertThat(mock.confirm("proceed?", true)).isTrue();
        }

        @Test
        @DisplayName("confirm_false_returnsFalse")
        void confirm_false_returnsFalse() {
            var mock = new MockTerminalProvider()
                    .addConfirm(false);

            assertThat(mock.confirm("proceed?", true)).isFalse();
        }
    }

    @Nested
    @DisplayName("display")
    class Display {

        @Test
        @DisplayName("display_recordsMessages")
        void display_recordsMessages() {
            var mock = new MockTerminalProvider();

            mock.display("hello");
            mock.display("world");

            assertThat(mock.getDisplayedMessages())
                    .containsExactly("hello", "world");
        }
    }

    @Nested
    @DisplayName("cancelAfter")
    class CancelAfter {

        @Test
        @DisplayName("cancelAfter_zero_cancelsFirstPrompt")
        void cancelAfter_zero_cancelsFirstPrompt() {
            var mock = new MockTerminalProvider()
                    .addReadLine("value")
                    .cancelAfter(0);

            assertThatThrownBy(() -> mock.readLine("prompt"))
                    .isInstanceOf(
                            GenerationCancelledException.class);
        }

        @Test
        @DisplayName("cancelAfter_two_cancelsThirdPrompt")
        void cancelAfter_two_cancelsThirdPrompt() {
            var mock = new MockTerminalProvider()
                    .addReadLine("first")
                    .addReadLine("second")
                    .addReadLine("third")
                    .cancelAfter(2);

            mock.readLine("p1");
            mock.readLine("p2");

            assertThatThrownBy(() -> mock.readLine("p3"))
                    .isInstanceOf(
                            GenerationCancelledException.class);
        }
    }
}
