package com.google.appinventor.buildserver.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class Executor implements Callable<Boolean> {
  private List<Task> tasks;

  public Executor() {
    this.tasks = new ArrayList<>();
  }

  public Executor add(Task task) {
    assert task != null;
    this.tasks.add(task);
    return this;
  }

  @Override
  public Boolean call() {
    setProgress(0);
    int TASKS_SIZE = this.tasks.size();

    if (TASKS_SIZE == 0) {
      System.out.println("WARN: No tasks were executed");
      setProgress(100);
      return true;
    }

    for (int i = 0; i < TASKS_SIZE; i++) {
      Task task = this.tasks.get(i);

      long start = System.currentTimeMillis();
      System.out.println("INFO: Starting {" + task.getName() + "} at " + (start / 1000.0) + "seconds");

      TaskResult result;
      try {
        result = task.execute();
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }

      assert result != null;
      if (!result.isSuccess()) {
        return false;
      }

      setProgress(((i + 1) * 100) / TASKS_SIZE);
      System.out.println("INFO: Finished {" + task.getName() + "} at " + ((System.currentTimeMillis() - start) / 1000.0) + "seconds");
    }
    return true;
  }

  private void setProgress(int p) {

  }
}
