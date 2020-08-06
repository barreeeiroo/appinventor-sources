package com.google.appinventor.buildserver.compiler.tasks;

import com.google.appinventor.buildserver.compiler.BuildType;
import com.google.appinventor.buildserver.compiler.ExecutorContext;
import com.google.appinventor.buildserver.compiler.Task;
import com.google.appinventor.buildserver.compiler.TaskResult;


@BuildType(apk = true, aab = true)
public class RunMultidex implements Task {
  @Override
  public TaskResult execute(ExecutorContext context) {
    return null;
  }
}
