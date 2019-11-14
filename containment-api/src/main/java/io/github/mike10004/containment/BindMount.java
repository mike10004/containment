package io.github.mike10004.containment;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class BindMount {

    public final String hostDirectory;

    public final String containerDirectory;

    public final Permission permission;

    public BindMount(String hostDirectory, String containerDirectory, Permission permission) {
        this.hostDirectory = requireNonNull(hostDirectory);
        this.containerDirectory = requireNonNull(containerDirectory);
        this.permission = requireNonNull(permission);
    }

    public enum Permission {
        WRITE_AND_READ,
        READ_ONLY,
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BindMount)) return false;
        BindMount bindMount = (BindMount) o;
        return hostDirectory.equals(bindMount.hostDirectory) &&
                containerDirectory.equals(bindMount.containerDirectory) &&
                permission == bindMount.permission;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostDirectory, containerDirectory, permission);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BindMount.class.getSimpleName() + "[", "]")
                .add("hostDirectory='" + hostDirectory + "'")
                .add("containerDirectory='" + containerDirectory + "'")
                .add("permission=" + permission)
                .toString();
    }
}
