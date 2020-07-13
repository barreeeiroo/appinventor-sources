package com.google.appinventor.buildserver.compiler;

public interface Task {
  /**
   * Main method to run the task
   *
   * @return TaskResult
   */
  TaskResult execute(ExecutorContext context);

  /**
   * Identify the task with a name
   *
   * @return String
   */
  String getName();
}
