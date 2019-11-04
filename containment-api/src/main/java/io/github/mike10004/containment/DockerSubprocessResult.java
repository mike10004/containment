package io.github.mike10004.containment;

public interface DockerSubprocessResult<T> {

     int exitCode();
     T stdout();
     T stderr();

     static <T> DockerSubprocessResult<T> create(int exitCode, T stdout, T stderr) {
          return new PredefinedSubprocessResult<>(exitCode, stdout, stderr);
     }

     static <T> DockerSubprocessResult<T> noContent(int exitCode) {
          return new PredefinedSubprocessResult.ContentlessSubprocessResult<T>(exitCode);
     };

}
