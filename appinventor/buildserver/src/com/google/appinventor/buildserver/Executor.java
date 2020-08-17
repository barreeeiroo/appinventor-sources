// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.buildserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Executor class for the YAIL compiler. Callable implementation
 * allows running different threads easier.
 *
 * <p>Will recursively accept {@link Task} extended classes
 * and build them to produce an output. It uses a builder
 * style pattern, where receives build information, and then
 * {@link Task} can be added.</p>
 *
 * @author diego@barreiro.xyz (Diego Barreiro)
 */
public class Executor implements Callable<Boolean> {
  private final List<Class<? extends Task>> tasks;
  private ExecutorContext context;
  private String ext = BuildType.APK_EXTENSION;

  // The Builder class will construct a Executor object on which Task's
  // can be added.
  public static class Builder {
    private ExecutorContext context;
    private String ext;

    public Builder() {
    }

    // Passes the previously constructed ExecutorContext with all
    // build info.
    public Builder withContext(ExecutorContext context) {
      this.context = context;
      return this;
    }

    // Specifies the build type (the extension output actually).
    // Only accepts the one specified in BuildType interface.
    public Builder withType(String ext) {
      if (ext == null || !ext.equals(BuildType.APK_EXTENSION) && !ext.equals(BuildType.AAB_EXTENSION)) {
        System.out.println("[ERROR] BuildType '" + ext + "' is not supported!");
      } else {
        this.ext = ext;
      }
      return this;
    }

    // Constructs the Executor object, making sure all needed
    // attributes are passed.
    public Executor build() {
      if (context == null) {
        System.out.println("[ERROR] ExecutorContext was not provided to Executor");
        return null;
      } else if (ext == null) {
        this.ext = BuildType.APK_EXTENSION;
        System.out.println("[WARN] No BuildType specified; using BuildType.APK_EXTENSION");
      }

      Executor executor = new Executor();
      executor.context = context;
      executor.ext = ext;
      return executor;
    }
  }

  // Actually, constructor is private, as it will be always
  // built using the Executor.Builder.
  private Executor() {
    this.tasks = new ArrayList<>();
  }

  // Adds a new Task to the build.
  public Executor add(Class<? extends Task> task) {
    assert task != null;
    this.tasks.add(task);
    return this;
  }

  // "Main" method that returns either true or false, depending
  // on result.
  @Override
  public Boolean call() {
    // Initializes progress to 0.
    context.getReporter().setProgress(0);
    int TASKS_SIZE = this.tasks.size();

    // If no tasks, we technically have successfully build everything.
    if (TASKS_SIZE == 0) {
      context.getReporter().warn("No tasks were executed");
      context.getReporter().setProgress(100);
      return true;
    }

    for (int i = 0; i < TASKS_SIZE; i++) {
      Class<? extends Task> task = this.tasks.get(i);
      String taskName = task.getSimpleName();

      Object taskObject;
      try {
        taskObject = task.newInstance();
      } catch (IllegalAccessException | InstantiationException e) {
        e.printStackTrace();
        context.getReporter().error("Could create new task " + taskName);
        return false;
      }

      if (task.isAnnotationPresent(BuildType.class)) {
        BuildType buildType = task.getAnnotation(BuildType.class);
        switch (ext) {
          case BuildType.AAB_EXTENSION:
            if (!buildType.aab()) {
              context.getReporter().error("Task " + taskName + " does not support builds on AABs!");
              return false;
            }
            break;
          default:
          case BuildType.APK_EXTENSION:
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
  
  @Override
  public String toString() {
    return "Executor{" +
        "tasks=" + tasks +
        ", context=" + context +
        ", ext='" + ext + '\'' +
        '}';
  }
}