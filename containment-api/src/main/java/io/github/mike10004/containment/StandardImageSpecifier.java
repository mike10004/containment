package io.github.mike10004.containment;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Value class that represents a standard image specification.
 * Create one with {@link ImageSpecifier#standard(String, String, String, String)}.
 */
public final class StandardImageSpecifier extends ImageSpecifier {

    /**
     * Constructs a new instance.
     *
     * @param name bare image name
     * @param tag optional tag
     * @param repository optional repository
     * @param registry optional registry
     */
    StandardImageSpecifier(String name, @Nullable String tag, @Nullable String repository, @Nullable String registry) {
        super(name, repository, registry);
        this.tag = tag;
    }

    /**
     * Image tag. For example, {@code latest}.
     */
    @Nullable
    public final String tag;

    @Override
    public ImageSpecifier withRepository(String repository) {
        return Objects.equals(this.repository, repository) ? this : new StandardImageSpecifier(name, tag, repository, registry);
    }

    @Override
    public ImageSpecifier withRegistry(String registry) {
        return Objects.equals(this.registry, registry) ? this : new StandardImageSpecifier(name, tag, repository, registry);
    }

    @Override
    protected String stringify() {
        return doStringify(name, repository, registry, ':', tag);
    }

    @Override
    protected String pin() {
        return tag;
    }
}
