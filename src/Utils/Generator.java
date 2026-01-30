package Utils;

import java.util.UUID;

public class Generator {

    public static String generateId(String modelName) {
        String prefix = modelName.substring(0, 2).toUpperCase(); // e.g., "US", "MG"
        String uniqueId = UUID.randomUUID().toString().substring(0, 8); // shorten for readability
        return prefix + "_" + uniqueId;
    }
}