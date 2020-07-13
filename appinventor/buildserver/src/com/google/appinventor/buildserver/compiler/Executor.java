package com.google.appinventor.buildserver.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class Executor implements Callable<Boolean> {
  private List<Class<? extends Task>> tasks;
  private final ExecutorContext context;
  private final String ext;

  public Executor(ExecutorContext context, String ext) {
    this.context = context;
    this.tasks = new ArrayList<>();
    this.ext = ext;
  }

  public Executor add(Class<? extends Task> task) {
    assert task != null;
    this.tasks.add(task);
    return this;
  }

  @Override
  public Boolean call() {
    setProgress(0);
    int TASKS_SIZE = this.tasks.size();

    if (TASKS_SIZE == 0) {
      context.getReporter().warn("No tasks were executed");
      setProgress(100);
      return true;
    }

    for (int i = 0; i < TASKS_SIZE; i++) {
      Class<? extends Task> task = this.tasks.get(i);
      String taskName = task.getName();

      Object taskObject;
      try {
        taskObject = task.newInstance();
      } catch (IllegalAccessException | InstantiationException e) {
        e.printStackTrace();
        context.getReporter().error("Could create new task " + taskName);
        return false;
      }

      try {
        Method getName = task.getMethod("getName");
        taskName = (String) getName.invoke(taskObject);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
        context.getReporter().warn("Could not check real name for task " + taskName);
      }

      if (task.isAnnotationPresent(BuildType.class)) {
        BuildType buildType = task.getAnnotation(BuildType.class);
        switch (ext) {
          case "aab":
            if (!buildType.aab()) {
              context.getReporter().error("Task " + taskName + " does not support builds on AABs!");
              return false;
            }
            break;
          default:
          case "apk":
            if (!buildType.apk()) {
              context.getReporter().error("Task " + taskName + " does not support builds on APKs!");
              return false;
            }
            break;
        }
      } else {
        context.getReporter().warn("Task " + taskName + " does not contain build type targets!");
      }

      long start = System.currentTimeMillis();
      context.getReporter().taskStart(taskName);

      TaskResult result = null;
      try {
        Method execute = task.getMethod("execute", ExecutorContext.class);
        result = (TaskResult) execute.invoke(taskObject, context);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        context.getReporter().taskError(-1);
        e.printStackTrace();
      }
      double endTime = (System.currentTimeMillis() - start) / 1000.0;

      if (result == null || !result.isSuccess()) {
        context.getReporter().error(result == null || result.getError() == null ? "Unknown exception" : result.getError().getMessage(), true);
        context.getReporter().taskError(endTime);
        return false;
      }

      context.getReporter().setProgress(((i + 1) * 100) / TASKS_SIZE);
      context.getReporter().taskSuccess((System.currentTimeMillis() - start) / 1000.0);
    }
    return true;
  }

  private void setProgress(int p) {

  }
}
