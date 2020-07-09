package com.google.appinventor.buildserver.compiler;

import java.io.IOException;

public interface Task {
  /**
   * Main method to run the task
   * @return TaskResult
   * @throws IOException e
   */
  TaskResult execute() throws IOException;

  /**
   * Identify the task with a name
   * @return String
   */
  String getName();
}
