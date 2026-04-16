package dev.iadev.release.prompt;

import java.util.List;

/**
 * Port wrapping the Claude Code {@code AskUserQuestion}
 * tool. Abstracted so that tests can inject stubs/spies
 * without invoking the real UI prompt.
 */
@FunctionalInterface
public interface AskUserQuestionPort {

    /**
     * Presents a question with fixed options and returns
     * the operator's selected option label.
     *
     * @param question display text for the prompt
     * @param options  selectable option labels
     * @return the label chosen by the operator
     */
    String ask(String question, List<String> options);
}
