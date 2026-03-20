package dev.iadev.cli;

import java.util.List;
import java.util.function.Predicate;

/**
 * Abstraction over terminal I/O for interactive prompts.
 *
 * <p>Enables testability by decoupling prompt logic from JLine terminal
 * implementation. Production code uses {@link JLineTerminalProvider};
 * tests use a mock that returns pre-configured responses.</p>
 */
public interface TerminalProvider {

    /**
     * Reads a line of text from the user.
     *
     * @param prompt the prompt text to display
     * @return the user's input
     * @throws GenerationCancelledException if the user cancels (Ctrl+C)
     */
    String readLine(String prompt);

    /**
     * Reads a line of text with inline validation and re-prompt on failure.
     *
     * @param prompt the prompt text to display
     * @param validator predicate that returns true for valid input
     * @param errorMsg message displayed when validation fails
     * @return the validated user input
     * @throws GenerationCancelledException if the user cancels (Ctrl+C)
     */
    String readLineWithValidation(
            String prompt,
            Predicate<String> validator,
            String errorMsg);

    /**
     * Presents a list of options for single selection.
     *
     * @param prompt the prompt text to display
     * @param options available options
     * @param defaultIndex zero-based index of the default selection
     * @return the selected option string
     * @throws GenerationCancelledException if the user cancels (Ctrl+C)
     */
    String selectFromList(
            String prompt, List<String> options, int defaultIndex);

    /**
     * Presents a list of options for multiple selection (checkboxes).
     *
     * @param prompt the prompt text to display
     * @param options available options
     * @param defaults options selected by default
     * @return the list of selected option strings (at least one)
     * @throws GenerationCancelledException if the user cancels (Ctrl+C)
     */
    List<String> selectMultiple(
            String prompt, List<String> options, List<String> defaults);

    /**
     * Presents a yes/no confirmation prompt.
     *
     * @param prompt         the prompt text to display
     * @param confirmDefault the default answer
     * @return true if confirmed, false otherwise
     * @throws GenerationCancelledException if the user
     *         cancels (Ctrl+C)
     */
    boolean confirm(String prompt,
                    ConfirmDefault confirmDefault);

    /**
     * Displays a message to the user (no input expected).
     *
     * @param message the message to display
     */
    void display(String message);
}
