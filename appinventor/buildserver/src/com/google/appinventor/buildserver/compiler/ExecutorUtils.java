package com.google.appinventor.buildserver.compiler;

import java.io.File;

public final class ExecutorUtils {
  private ExecutorUtils() {
  }

  public static File createDir(File parentDir, String name) {
    File dir = new File(parentDir, name);
    if (!dir.exists()) {
      dir.mkdir();
    }
    return dir;
  }
}
