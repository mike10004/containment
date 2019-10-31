package io.github.mike10004.containment;

import java.util.Arrays;
import java.util.List;

public interface CreatedContainer {
    static CreatedContainer define(String containerId, String[] warnings) {
        return new CreatedContainer() {
            @Override
            public String getId() {
                return containerId;
            }

            @Override
            public List<String> getWarnings() {
                return Arrays.asList(warnings);
            }

            @Override
            public String toString() {
                return String.format("CreatedContainer{id=%s}", containerId);
            }
        };
    }

    String getId();
    List<String> getWarnings();
}
