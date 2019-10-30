package io.github.mike10004.containment.mavenplugin;

public enum AbsentImageAction {

    /**
     * Pull the image from a remote repository.
     */
    pull,

    /**
     * Build the image from a local directory containing a dockerfile.
     */
    build,

    /**
     * Fail the build.
     */
    fail,

    /**
     * Ignore and move on.
     */
    ignore;
}
