package dev.iadev.domain.stack;

import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Protocol derivation from interface types.
 *
 * <p>Maps interface types to protocol directory names for selecting
 * the correct protocol documentation during generation.</p>
 *
 * <p>Zero external framework dependencies (RULE-007).</p>
 */
public final class ProtocolMapping {

    private ProtocolMapping() {
        // utility class
    }

    /** Interface type to protocol directory names. */
    public static final Map<String, List<String>> INTERFACE_PROTOCOL_MAP =
            Map.of(
                    "rest", List.of("rest"),
                    "grpc", List.of("grpc"),
                    "graphql", List.of("graphql"),
                    "websocket", List.of("websocket"),
                    "event-consumer",
                    List.of("event-driven", "messaging"),
                    "event-producer",
                    List.of("event-driven", "messaging"),
                    "cli", List.of()
            );

    /** Prefix for event-type interfaces. */
    public static final String EVENT_PREFIX = "event-";

    /** Protocol name for event-driven interfaces. */
    public static final String EVENT_DRIVEN_PROTOCOL = "event-driven";

    /**
     * Derives protocol directory names from interface types.
     *
     * <p>Returns deduplicated, sorted list of protocol names.</p>
     *
     * @param config the project configuration
     * @return sorted list of protocol names
     */
    public static List<String> deriveProtocols(ProjectConfig config) {
        LinkedHashSet<String> protocols = new LinkedHashSet<>();
        for (var iface : config.interfaces()) {
            List<String> mapped =
                    INTERFACE_PROTOCOL_MAP.get(iface.type());
            if (mapped != null) {
                protocols.addAll(mapped);
            } else if (iface.type().startsWith(EVENT_PREFIX)) {
                protocols.add(EVENT_DRIVEN_PROTOCOL);
            }
        }
        List<String> sorted = new ArrayList<>(protocols);
        sorted.sort(String::compareTo);
        return sorted;
    }

    /**
     * Extracts broker name from the first interface that has one.
     *
     * @param config the project configuration
     * @return the broker name, or empty string if none
     */
    public static String extractBroker(ProjectConfig config) {
        for (var iface : config.interfaces()) {
            if (iface.broker() != null && !iface.broker().isEmpty()) {
                return iface.broker();
            }
        }
        return "";
    }
}
