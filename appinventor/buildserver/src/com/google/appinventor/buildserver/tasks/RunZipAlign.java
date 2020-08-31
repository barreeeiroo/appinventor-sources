package com.google.appinventor.buildserver.tasks;

import com.google.appinventor.buildserver.*;

import java.io.File;

/**
 * compiler.runZipAlign()
 */
@BuildType(apk = true)
public class RunZipAlign implements Task {
  @Override
  public TaskResult execute(CompilerContext context) {
    String zipAlignTool = context.getResources().zipalign();
    if (zipAlignTool == null) {
      return TaskResult.generateError("Could not find a suitable ZipAlign tool for this OS");
    }

    File zipAlignedApk = new File(context.getPaths().getTmpDir(), "zipaligned.apk");
    // zipalign -f 4 infile.zip outfile.zip
    String[] zipAlignCommandLine = {
        zipAlignTool, "-f", "4",
        context.getPaths().getDeployFile().getAbsolutePath(),
        zipAlignedApk.getAbsolutePath()
    };

    if (!Execution.execute(null, zipAlignCommandLine, context.getReporter().getSystemOut(), System.err)) {
      TaskResult.generateError("Error while running ZipAlign tool");
    }

    if (!ExecutorUtils.copyFile(zipAlignedApk.getAbsolutePath(), context.getPaths().getDeployFile().getAbsolutePath())) {
      TaskResult.generateError("Error while copying ZipAlign'ed APK");
    }

    return TaskResult.generateSuccess();
  }
}
