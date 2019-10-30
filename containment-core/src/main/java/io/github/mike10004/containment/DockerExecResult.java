package io.github.mike10004.containment;

public interface DockerExecResult<T> {

     int exitCode();
     T stdout();
     T stderr();

     static <T> DockerExecResult<T> create(int exitCode, T stdout, T stderr) {
          return new PredefinedExecResult<>(exitCode, stdout, stderr);
     }

     static <T> DockerExecResult<T> noContent(int exitCode) {
          return new PredefinedExecResult.ContentlessExecResult<T>(exitCode);
     };

}
