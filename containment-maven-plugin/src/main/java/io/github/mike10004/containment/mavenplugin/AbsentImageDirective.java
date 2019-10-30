package io.github.mike10004.containment.mavenplugin;

import com.google.common.base.Splitter;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Objects.requireNonNull;

class AbsentImageDirective {

    public final AbsentImageAction action;

    @Nullable
    public final String parameter;

    public AbsentImageDirective(AbsentImageAction action, @Nullable String parameter) {
        this.action = requireNonNull(action);
        this.parameter = parameter;
    }

    static final String DELIMITER = ":";

    private static final Splitter DIRECTIVE_TOKEN_SPLITTER = Splitter.on(DELIMITER).limit(2).trimResults();

    public static AbsentImageDirective parse(String token) {
        List<String> tokens = DIRECTIVE_TOKEN_SPLITTER.splitToList(token);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("empty token");
        }
        String actionStr = tokens.get(0).toLowerCase();
        AbsentImageAction action = AbsentImageAction.valueOf(actionStr);
        String parameter = null;
        if (tokens.size() > 1) {
            parameter = tokens.get(1);
        }
        return new AbsentImageDirective(action, parameter);
    }
}
