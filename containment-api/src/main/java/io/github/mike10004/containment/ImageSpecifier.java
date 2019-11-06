package io.github.mike10004.containment;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Immutable value class that specifies a container image.
 */
public abstract class ImageSpecifier {

    /**
     * Bare image name. For example, {@code apache} is the name
     */
    public final String name;

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
        return new StandardImageSpecifier(name, tag, null, null);
    }

    /**
     * Constructs a new instance.
     * @param name bare image name
     * @param tag optional tag
     * @param repository optional repository
     * @param registry optional registry
     */
    public static ImageSpecifier standard(String name, @Nullable String tag, @Nullable String repository, @Nullable String registry) {
        return new StandardImageSpecifier(name, tag, repository, registry);
    }


    protected ImageSpecifier(String name, @Nullable String repository, @Nullable String registry) {
        this.name = Preconditions.checkNotNull(name, "name");
        checkArgument(!name.trim().isEmpty(), "name must be nonempty/nonwhitespace");
        this.repository = repository;
        checkArgument(repository == null || (repository.equals(repository.toLowerCase())), "repository name must be lowercase: %s", StringUtils.abbreviate(repository, 128));
        this.registry = registry;
    }

    protected static String doStringify(String name, @Nullable String repository, @Nullable String registry, char pinDelimiter, Object pin) {
        StringBuilder sb = new StringBuilder(length(name) + length(pin) + 1 + length(repository) + length(registry) + 3);
        if (registry != null && !registry.isEmpty()) {
            sb.append(registry);
            if (repository == null) {
                repository = DEFAULT_REPOSITORY;
            }
            sb.append('/');
        }
        if (repository != null && !repository.isEmpty()) {
            sb.append(repository);
            sb.append("/");
        }
        checkArgument(name != null && !name.trim().isEmpty(), "name must be non-null and nonempty");
        sb.append(name);
        if (pin != null) {
            sb.append(pinDelimiter);
            sb.append(pin);
        }
        return sb.toString();
    }

    private static int length(@Nullable Object s) {
        return s == null ? 0 : s.toString().length();
    }

    protected static final String DEFAULT_REPOSITORY = "library";

    /**
     * Returns an instance that has the given tag if and only if this instance's pin is not defined.
     * @param defaultTag the default tag to apply
     * @return a specifier instance
     */
    public ImageSpecifier withDefaultTag(String defaultTag) {
        checkArgument(defaultTag != null && !defaultTag.trim().isEmpty(), "default tag must be non-null and nonempty");
        Object pin = pin();
        if (pin != null) {
            return this;
        }
        return new StandardImageSpecifier(name, defaultTag, repository, registry);
    }

    /**
     * Returns an instance that has the given tag and all other fields equal to this instance's fields.
     * @param tag the tag
     * @return a specifier instance
     */
    public ImageSpecifier withTag(String tag) {
        if (this instanceof StandardImageSpecifier) {
            StandardImageSpecifier s = (StandardImageSpecifier) this;
            return Objects.equals(s.tag, tag) ? this : new StandardImageSpecifier(name, tag, repository, registry);
        }
        return new StandardImageSpecifier(name, tag, repository, registry);
    }

    /**
     * Returns an instance that has the given repository and all other fields equal to this instance's fields.
     * @param repository the repository
     * @return a specifier instance
     */
    public abstract ImageSpecifier withRepository(String repository);

    /**
     * Returns an instance that has the given registry and all other fields equal to this instance's fields.
     * @param registry the registry
     * @return a specifier instance
     */
    public abstract ImageSpecifier withRegistry(String registry);

    @Override
    public String toString() {
        return stringify();
    }

    /**
     * Returns the string representation accepted by {@code docker pull}.
     * @return string representation
     */
    protected abstract String stringify();

    /**
     * Returns the pin that narrows the specification of this image.
     * This is either a tag or a digest.
     * @return the pin
     */
    protected abstract Object pin();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageSpecifier)) return false;
        ImageSpecifier that = (ImageSpecifier) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(pin(), that.pin()) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(registry, that.registry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pin(), repository, registry);
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
        if (nameAndTag.contains("@")) {
            List<String> nameAndTagParts = Splitter.on('@').limit(2).splitToList(nameAndTag);
            String name = nameAndTagParts.get(0);
            String digestStr = nameAndTagParts.get(1);
            DigestImageSpecifier.Digest digest = DigestImageSpecifier.Digest.parseDigest(digestStr);
            return new DigestImageSpecifier(name, digest, repository, registry);
        } else {
            List<String> nameAndTagParts = Splitter.on(':').limit(2).splitToList(nameAndTag);
            String tag = nameAndTagParts.size() > 1 ? nameAndTagParts.get(1) : null;
            String name = nameAndTagParts.get(0);
            return new StandardImageSpecifier(name, tag, repository, registry);
        }
    }

    public String describe() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("repository", repository)
                .add("registry", registry)
                .add("pin", pin())
                .toString();
    }
}
