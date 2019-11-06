package io.github.mike10004.containment;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Immutable value class that specifies a container image.
 */
public class ImageSpecifier {

    /**
     * Bare image name. For example, {@code apache} is the name
     */
    public final String name;

    /**
     * Image tag. For example, {@code latest}.
     */
    @Nullable
    public final String tag;

    /**
     * Image repository. For example {@code fedora}.
     */
    @Nullable
    public final String repository;

    /**
     * Image registry. For example, {@code localhost:5000}.
     */
    @Nullable
    public final String registry;

    private transient final String stringification;

    /**
     * Constructs a new instance from a bare image name.
     * @param name bare image name without any tag, repository, or registry specification
     * @return a new instance
     */
    public static ImageSpecifier fromNameOnly(String name) {
        return fromNameAndTag(name, null);
    }

    /**
     * Constructs and returns a new instance.
     * @param name Bare image name
     * @param tag optional tag
     * @return a new instance
     */
    public static ImageSpecifier fromNameAndTag(String name, @Nullable String tag) {
        return new ImageSpecifier(name, tag, null, null);
    }

    /**
     * Constructs a new instance.
     * @param name bare image name
     * @param tag optional tag
     * @param repository optional repository
     * @param registry optional registry
     */
    public ImageSpecifier(String name, @Nullable String tag, @Nullable String repository, @Nullable String registry) {
        this.name = Preconditions.checkNotNull(name, "name");
        Preconditions.checkArgument(!name.trim().isEmpty(), "name must be nonempty/nonwhitespace");
        this.tag = tag;
        this.repository = repository;
        Preconditions.checkArgument(repository == null || (repository.equals(repository.toLowerCase())), "repository name must be lowercase: %s", StringUtils.abbreviate(repository, 128));
        this.registry = registry;
        stringification = stringify(name, tag, repository, registry);
    }

    private static int length(@Nullable String s) {
        return s == null ? 0 : s.length();
    }

    private static final String DEFAULT_REPOSITORY = "library";

    private static String stringify(String name, @Nullable String tag, @Nullable String repository, @Nullable String registry) {
        StringBuilder sb = new StringBuilder(length(name) + length(tag) + length(repository) + length(registry) + 3);
        if (registry != null && !registry.isEmpty()) {
            sb.append(registry);
            if (repository == null) {
                repository = DEFAULT_REPOSITORY;
            }
        }
        if (repository != null && !repository.isEmpty()) {
            sb.append(repository);
            sb.append("/");
        }
        Preconditions.checkArgument(name != null && !name.trim().isEmpty(), "name must be non-null and nonempty");
        sb.append(name);
        if (tag != null && !tag.isEmpty()) {
            sb.append(':');
            sb.append(tag);
        }
        return sb.toString();
    }

    /**
     * Returns an instance that has the given tag if and only if this instance's tag is not defined.
     * @param defaultTag the default tag to apply
     * @return a specifier instance
     */
    public ImageSpecifier withDefaultTag(String defaultTag) {
        return withTag(tag == null ? defaultTag : tag);
    }

    /**
     * Returns an instance that has the given tag and all other fields equal to this instance's fields.
     * @param tag the tag
     * @return a specifier instance
     */
    public ImageSpecifier withTag(String tag) {
        return Objects.equals(this.tag, tag) ? this : new ImageSpecifier(name, tag, repository, registry);
    }

    /**
     * Returns an instance that has the given repository and all other fields equal to this instance's fields.
     * @param repository the repository
     * @return a specifier instance
     */
    public ImageSpecifier withRepository(String repository) {
        return Objects.equals(this.repository, repository) ? this : new ImageSpecifier(name, tag, repository, registry);
    }

    /**
     * Returns an instance that has the given registry and all other fields equal to this instance's fields.
     * @param registry the registry
     * @return a specifier instance
     */
    public ImageSpecifier withRegistry(String registry) {
        return Objects.equals(this.registry, registry) ? this : new ImageSpecifier(name, tag, repository, registry);
    }

    @Override
    public String toString() {
        return stringification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageSpecifier)) return false;
        ImageSpecifier that = (ImageSpecifier) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(tag, that.tag) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(registry, that.registry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tag, repository, registry);
    }

    public static ImageSpecifier parseSpecifier(String token) {
        List<String> parts = Splitter.on('/').limit(3).splitToList(token);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("specifier is empty");
        }
        String nameAndTag = parts.get(parts.size() - 1);
        String repository, registry;
        if (parts.size() == 2) {
            repository = parts.get(0);
            registry = null;
        } else if (parts.size() == 3) {
            repository = parts.get(1);
            registry = parts.get(0);
        } else if (parts.size() == 1) {
            repository = null;
            registry = null;
        } else {
            throw new IllegalStateException("BUG: parsing image specifier");
        }
        List<String> nameAndTagParts = Splitter.on(':').limit(2).splitToList(nameAndTag);
        String tag = nameAndTagParts.size() > 1 ? nameAndTagParts.get(1) : null;
        String name = nameAndTagParts.get(0);
        return new ImageSpecifier(name, tag, repository, registry);
    }

}
