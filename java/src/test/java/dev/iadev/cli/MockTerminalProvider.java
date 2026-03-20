package dev.iadev.cli;

import dev.iadev.exception.GenerationCancelledException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

/**
 * Test implementation of {@link TerminalProvider} with pre-configured responses.
 *
 * <p>Responses are queued and consumed in order. Supports configuring
 * a cancellation at a specific prompt to test Ctrl+C scenarios.</p>
 */
public class MockTerminalProvider implements TerminalProvider {

    private final Deque<String> readLineResponses = new ArrayDeque<>();
    private final Deque<String> selectResponses = new ArrayDeque<>();
    private final Deque<List<String>> multiSelectResponses =
            new ArrayDeque<>();
    private final Deque<Boolean> confirmResponses = new ArrayDeque<>();
    private final List<String> displayedMessages = new ArrayList<>();
    private int cancelAfterPrompts = -1;
    private int promptCount;

    /**
     * Queues a response for the next {@code readLine} or
     * {@code readLineWithValidation} call.
     *
     * @param response the text to return
     * @return this instance for chaining
     */
    public MockTerminalProvider addReadLine(String response) {
        readLineResponses.add(response);
        return this;
    }

    /**
     * Queues a selection response for the next {@code selectFromList} call.
     *
     * @param response the option to return
     * @return this instance for chaining
     */
    public MockTerminalProvider addSelect(String response) {
        selectResponses.add(response);
        return this;
    }

    /**
     * Queues a multi-select response for the next
     * {@code selectMultiple} call.
     *
     * @param response the list of selected options
     * @return this instance for chaining
     */
    public MockTerminalProvider addMultiSelect(List<String> response) {
        multiSelectResponses.add(response);
        return this;
    }

    /**
     * Queues a confirm response for the next {@code confirm} call.
     *
     * @param response true for yes, false for no
     * @return this instance for chaining
     */
    public MockTerminalProvider addConfirm(boolean response) {
        confirmResponses.add(response);
        return this;
    }

    /**
     * Configures cancellation after a specific number of total prompts.
     *
     * @param count cancel after this many prompts (across all types)
     * @return this instance for chaining
     */
    public MockTerminalProvider cancelAfter(int count) {
        this.cancelAfterPrompts = count;
        return this;
    }

    /**
     * Returns all messages passed to {@link #display(String)}.
     *
     * @return list of displayed messages
     */
    public List<String> getDisplayedMessages() {
        return List.copyOf(displayedMessages);
    }

    @Override
    public String readLine(String prompt) {
        checkCancellation();
        if (readLineResponses.isEmpty()) {
            throw new IllegalStateException(
                    "No readLine response configured for prompt: "
                            + prompt);
        }
        return readLineResponses.poll();
    }

    @Override
    public String readLineWithValidation(
            String prompt,
            Predicate<String> validator,
            String errorMsg) {
        while (true) {
            checkCancellation();
            if (readLineResponses.isEmpty()) {
                throw new IllegalStateException(
                        "No readLine response configured for prompt: "
                                + prompt);
            }
            String response = readLineResponses.poll();
            if (validator.test(response)) {
                return response;
            }
            displayedMessages.add(errorMsg);
        }
    }

    @Override
    public String selectFromList(
            String prompt, List<String> options, int defaultIndex) {
        checkCancellation();
        if (selectResponses.isEmpty()) {
            throw new IllegalStateException(
                    "No select response configured for prompt: "
                            + prompt);
        }
        return selectResponses.poll();
    }

    @Override
    public List<String> selectMultiple(
            String prompt, List<String> options,
            List<String> defaults) {
        checkCancellation();
        if (multiSelectResponses.isEmpty()) {
            throw new IllegalStateException(
                    "No multi-select response configured for prompt: "
                            + prompt);
        }
        return multiSelectResponses.poll();
    }

    @Override
    public boolean confirm(String prompt,
                           ConfirmDefault confirmDefault) {
        checkCancellation();
        if (confirmResponses.isEmpty()) {
            throw new IllegalStateException(
                    "No confirm response configured"
                            + " for prompt: " + prompt);
        }
        return confirmResponses.poll();
    }

    @Override
    public void display(String message) {
        displayedMessages.add(message);
    }

    private void checkCancellation() {
        promptCount++;
        if (cancelAfterPrompts >= 0
                && promptCount > cancelAfterPrompts) {
            throw new GenerationCancelledException(
                    InteractivePrompter.CANCELLED_BY_USER);
        }
    }
}
