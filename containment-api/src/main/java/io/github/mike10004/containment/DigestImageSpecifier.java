package io.github.mike10004.containment;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Value class that represents an image specifier that contains a digest specification.
 *
 */
public final class DigestImageSpecifier extends ImageSpecifier {

    public final Digest digest;

    public DigestImageSpecifier(String name, Digest digest, @Nullable String repository, @Nullable String registry) {
        super(name, repository, registry);
        this.digest = requireNonNull(digest, "digest");
    }

    @Override
    public ImageSpecifier withRepository(String repository) {
        if (Objects.equals(this.repository, repository)) {
            return this;
        }
        return new DigestImageSpecifier(name, digest, repository, registry);
    }

    @Override
    public ImageSpecifier withRegistry(String registry) {
        if (Objects.equals(this.registry, registry)) {
            return this;
        }
        return new DigestImageSpecifier(name, digest, repository, registry);
    }

    @Override
    protected String stringify() {
        return doStringify(name, repository, registry, '@', digest);
    }

    @Override
    protected Digest pin() {
        return digest;
    }

    public static final class Digest {
        public final String algorithm;
        public final String hash;

        public Digest(String algorithm, String hash) {
            this.algorithm = requireNonNull(algorithm);
            checkArgument(!algorithm.trim().isEmpty(), "algorithm must be nonempty");
            this.hash = requireNonNull(hash);
            checkArgument(!hash.trim().isEmpty(), "hash must be nonempty");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Digest)) return false;
            Digest digest = (Digest) o;
            return Objects.equals(algorithm, digest.algorithm) &&
                    Objects.equals(hash, digest.hash);
        }

        @Override
        public int hashCode() {
            return Objects.hash(algorithm, hash);
        }

        public static Digest parseDigest(String digest) {
            List<String> parts = Splitter.on(':').limit(2).splitToList(digest);
            if (parts.size() != 2) {
                throw new IllegalArgumentException("digest must be of the form 'algorithm:hash'; argument = " + StringUtils.abbreviate(digest, 256));
            }
            String hash = parts.get(1);
            checkArgument(CharMatcher.is('/').matchesNoneOf(hash), "hash must not contain special characters that could cause parse failure");
            return new Digest(parts.get(0), parts.get(1));
        }

        @Override
        public String toString() {
            return String.format("%s:%s", algorithm, hash);
        }
    }

}
