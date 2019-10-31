package io.github.mike10004.containment;

enum ProcessOutputStreamType {

    stdout, stderr;

    boolean isStdout() {
        return this == stdout;
    }

    boolean isStderr() {
        return this == stderr;
    }
}
