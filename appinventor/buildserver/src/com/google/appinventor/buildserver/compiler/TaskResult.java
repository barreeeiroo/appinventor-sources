package com.google.appinventor.buildserver.compiler;

import java.util.Optional;


public interface TaskResult {
  /**
   * Returns the exit code of the task
   * @return int
   */
  int getExitCode();

  /**
   * Optinal exception that is present when exit code is not success
   * @return Exception
   */
  Optional<Exception> getError();

  /**
   * Check if the exit code is success
   * @return boolean
   */
  default boolean isSuccess() {
    return getExitCode() == TaskExecutionCodes.SUCCESS_EXIT_CODE;
  }
}
