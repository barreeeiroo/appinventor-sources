package com.google.appinventor.buildserver.compiler.tasks;

import com.android.sdklib.build.ApkBuilder;
import com.google.appinventor.buildserver.compiler.BuildType;
import com.google.appinventor.buildserver.compiler.ExecutorContext;
import com.google.appinventor.buildserver.compiler.Task;
import com.google.appinventor.buildserver.compiler.TaskResult;

import java.io.File;

/**
 * compiler.runApkBuilder
 */
@BuildType(apk = true)
public class RunApkBuilder implements Task {
  @Override
  public TaskResult execute(ExecutorContext context) {
    try {
      ApkBuilder apkBuilder = new ApkBuilder(
          context.getPaths().getDeployFile().getAbsolutePath(),
          context.getPaths().getTmpPackageName().getAbsolutePath(),
          context.getPaths().getTmpDir().getAbsolutePath() + File.separator + "classes.dex",
          null,
          context.getReporter().getSystemOut()
      );
      if (context.getResources().getDexFiles().size() > 1) {
        for (File f : context.getResources().getDexFiles()) {
          if (!f.getName().equals("classes.dex")) {
            apkBuilder.addFile(f, f.getName());
          }
        }
      }
      if (context.getComponentInfo().getNativeLibsNeeded().size() != 0) { // Need to add native libraries...
        apkBuilder.addNativeLibraries(context.getPaths().getLibsDir());
      }
      apkBuilder.sealApk();
    } catch (Exception e) {
      return TaskResult.generateError(e);
    }
    return TaskResult.generateSuccess();
  }
}
