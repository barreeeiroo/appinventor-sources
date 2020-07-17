package com.google.appinventor.buildserver.compiler;

import com.google.appinventor.buildserver.PathUtil;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ExecutorUtils {
  private static final ConcurrentMap<String, File> resources = new ConcurrentHashMap<>();

  private ExecutorUtils() {
  }

  public static File createDir(File dir) {
    if (!dir.exists()) {
      if (!dir.mkdir()) {
        System.out.println("[WARN] Could not create directory: " + dir);
      }
    }
    return dir;
  }

  public static File createDir(File parentDir, String name) {
    File dir = new File(parentDir, name);
    if (!dir.exists()) {
      if (!dir.mkdir()) {
        System.out.println("[WARN] Could not create directory: " + dir);
      }
    }
    return dir;
  }

  static synchronized String getResource(String resourcePath) {
    try {
      File file = resources.get(resourcePath);
      if (file == null) {
        String basename = PathUtil.basename(resourcePath);
        StringBuilder prefix;
        String suffix;
        int lastDot = basename.lastIndexOf(".");
        if (lastDot != -1) {
          prefix = new StringBuilder(basename.substring(0, lastDot));
          suffix = basename.substring(lastDot);
        } else {
          prefix = new StringBuilder(basename);
          suffix = "";
        }
        while (prefix.length() < 3) {
          prefix.append("_");
        }
        file = File.createTempFile(prefix.toString(), suffix);
        if (!file.setExecutable(true)) {
          System.out.println("[WARN] Could not mark resources as executable: " + file);
        }
        file.deleteOnExit();
        if (!file.getParentFile().mkdirs()) {
          System.out.println("[WARN] Could not make directory: " + file.getParentFile());
        }
        Files.copy(Resources.newInputStreamSupplier(Executor.class.getResource(resourcePath)),
            file);
        resources.put(resourcePath, file);
      }
      return file.getAbsolutePath();
    } catch (IOException | NullPointerException e) {
      System.out.println("[ERROR] " + e.getMessage());
      return null;
    }
  }
}
