package io.github.mike10004.containment;

/**
 * Interface of an immutable value class that represents the result of
 * launching a process within a container.
 * @param <T> type of output consumed from the process standard output and error streams
 */
public interface ContainerSubprocessResult<T> {

     /**
      * Returns the process exit code.
      * @return exit code
      */
     int exitCode();

     /**
      * Returns content printed on the process standard output stream.
      * @return process standard output
      */
     T stdout();

     /**
      * Returns content printed on the process standard error stream.
      * @return process standard error
      */
     T stderr();

     /**
      * Creates a result instance.
      * @param exitCode exit code
      * @param stdout standard output content
      * @param stderr standard error content
      * @param <T> type of standard stream content
      * @return new result instance
      */
     static <T> ContainerSubprocessResult<T> create(int exitCode, T stdout, T stderr) {
          return new PredefinedSubprocessResult<>(exitCode, stdout, stderr);
     }

     /**
      * Creates a result instance that does not contain any content.
      * @param exitCode exit code
      * @param <T> standard stream content type
      * @return a new result instance
      */
     static <T> ContainerSubprocessResult<T> noContent(int exitCode) {
          return new PredefinedSubprocessResult.ContentlessSubprocessResult<>(exitCode);
     };

}
