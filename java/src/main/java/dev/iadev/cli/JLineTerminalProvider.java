package dev.iadev.cli;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * JLine 3.x implementation of {@link TerminalProvider}.
 *
 * <p>Uses a JLine {@link Terminal} and {@link LineReader} for real terminal
 * interaction. Handles {@link UserInterruptException} (Ctrl+C) by throwing
 * {@link GenerationCancelledException}.</p>
 */
public class JLineTerminalProvider implements TerminalProvider {

    private final LineReader reader;
    private final PrintWriter writer;

    /**
     * Creates a provider with a default system terminal.
     *
     * @throws IOException if the terminal cannot be created
     */
    public JLineTerminalProvider() throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        this.reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();
        this.writer = terminal.writer();
    }

    /**
     * Creates a provider with a pre-built line reader.
     *
     * @param reader the JLine line reader
     * @param writer the output writer
     */
    JLineTerminalProvider(LineReader reader, PrintWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public String readLine(String prompt) {
        try {
            return reader.readLine(prompt + " ");
        } catch (UserInterruptException | EndOfFileException e) {
            throw new GenerationCancelledException(
                    InteractivePrompter.CANCELLED_BY_USER);
        }
    }

    @Override
    public String readLineWithValidation(
            String prompt,
            Predicate<String> validator,
            String errorMsg) {
        while (true) {
            String input = readLine(prompt);
            if (validator.test(input)) {
                return input;
            }
            writer.println(errorMsg);
            writer.flush();
        }
    }

    @Override
    public String selectFromList(
            String prompt, List<String> options, int defaultIndex) {
        writer.println(prompt);
        for (int i = 0; i < options.size(); i++) {
            String marker = (i == defaultIndex) ? "> " : "  ";
            writer.printf("%s%d) %s%n", marker, i + 1, options.get(i));
        }
        writer.flush();

        while (true) {
            String input = readLine(
                    "Select [1-" + options.size() + "] (default: "
                            + (defaultIndex + 1) + "):");
            if (input.isBlank()) {
                return options.get(defaultIndex);
            }
            try {
                int choice = Integer.parseInt(input.trim());
                if (choice >= 1 && choice <= options.size()) {
                    return options.get(choice - 1);
                }
            } catch (NumberFormatException ignored) {
                // re-prompt
            }
            writer.println(
                    "Invalid selection. Enter a number between 1 and "
                            + options.size() + ".");
            writer.flush();
        }
    }

    @Override
    public List<String> selectMultiple(
            String prompt, List<String> options,
            List<String> defaults) {
        writer.println(prompt);
        writer.println(
                "(Enter comma-separated numbers, "
                        + "e.g., 1,3)");
        for (int i = 0; i < options.size(); i++) {
            String marker = defaults.contains(options.get(i))
                    ? "[x] " : "[ ] ";
            writer.printf(
                    "%s%d) %s%n", marker, i + 1, options.get(i));
        }
        writer.flush();

        while (true) {
            String input = readLine("Select:");
            if (input.isBlank()) {
                return new ArrayList<>(defaults);
            }
            List<String> selected = parseMultiSelect(input, options);
            if (!selected.isEmpty()) {
                return selected;
            }
            writer.println(
                    "At least one interface must be selected.");
            writer.flush();
        }
    }

    @Override
    public boolean confirm(String prompt, boolean defaultValue) {
        String defHint = defaultValue ? "[Y/n]" : "[y/N]";
        String input = readLine(prompt + " " + defHint);
        if (input.isBlank()) {
            return defaultValue;
        }
        String trimmed = input.trim().toLowerCase();
        return "y".equals(trimmed) || "yes".equals(trimmed);
    }

    @Override
    public void display(String message) {
        writer.println(message);
        writer.flush();
    }

    private List<String> parseMultiSelect(
            String input, List<String> options) {
        List<String> selected = new ArrayList<>();
        for (String part : input.split(",")) {
            try {
                int idx = Integer.parseInt(part.trim());
                if (idx >= 1 && idx <= options.size()) {
                    String option = options.get(idx - 1);
                    if (!selected.contains(option)) {
                        selected.add(option);
                    }
                }
            } catch (NumberFormatException ignored) {
                // skip invalid entries
            }
        }
        return selected;
    }
}
