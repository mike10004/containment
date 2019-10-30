package io.github.mike10004.containment;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;

class PredefinedExecResult<T> implements DockerExecResult<T> {

    private final int exitCode;
    private final T stdout, stderr;

    public PredefinedExecResult(int exitCode, T stdout, T stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    @Override
    public int exitCode() {
        return  exitCode;
    }

    @Override
    public T stdout() {
        return stdout;
    }

    @Override
    public T stderr() {
        return stderr;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this);
        h.add("exitCode", exitCode);
        if (stdout != null) h.add("stdout", abbreviate(stdout, 64));
        if (stderr != null) h.add("stderr", abbreviate(stderr, 64));
        return h.toString();
    }

    @SuppressWarnings("SameParameterValue")
    private static String abbreviate(Object thing, int maxLength) {
        if (thing == null) {
            return "null";
        }
        return StringUtils.abbreviate(thing.toString(), maxLength);
    }

    public static class ContentlessExecResult<T> extends PredefinedExecResult<T> {

        public ContentlessExecResult(int exitCode) {
            super(exitCode, null, null);
        }

        @Override
        public String toString() {
            return "ContentlessExecResult{exitCode=" + exitCode() + "}";
        }
    }
}
