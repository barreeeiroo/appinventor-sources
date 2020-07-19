package com.google.appinventor.buildserver.compiler.context;


import com.google.appinventor.buildserver.PathUtil;
import com.google.appinventor.buildserver.compiler.Executor;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Resources {
  private final ConcurrentMap<String, File> resources;

  public final static String RUNTIME_FILES_DIR = "/" + "files" + "/";

  private final String COMP_BUILD_INFO = Resources.RUNTIME_FILES_DIR + "simple_components_build_info.json";
  private final String BUNDLETOOL_JAR = Resources.RUNTIME_FILES_DIR + "bundletool.jar";

  public Resources() {
    resources = new ConcurrentHashMap<>();
  }

  public synchronized String getResource(String resourcePath) {
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
        Files.copy(com.google.common.io.Resources.newInputStreamSupplier(Executor.class.getResource(resourcePath)),
            file);
        resources.put(resourcePath, file);
      }
      return file.getAbsolutePath();
    } catch (IOException | NullPointerException e) {
      System.out.println("[ERROR] " + e.getMessage());
      return null;
    }
  }

  public String getRuntimeFilesDir() {
    return Resources.RUNTIME_FILES_DIR;
  }

  public String getCompBuildInfo() {
    try {
      return com.google.common.io.Resources.toString(Resources.class.getResource(COMP_BUILD_INFO), Charsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }

  public String jarsigner() {
    String osName = System.getProperty("os.name");
    String jarsignerTool;
    if (osName.equals("Mac OS X")) {
      jarsignerTool = System.getenv("JAVA_HOME") + "/bin/jarsigner";
    } else if (osName.equals("Linux")) {
      jarsignerTool = System.getenv("JAVA_HOME") + "/bin/jarsigner";
    } else if (osName.startsWith("Windows")) {
      jarsignerTool = System.getenv("JAVA_HOME") + "\\bin\\jarsigner.exe";
    } else {
      jarsignerTool = null;
    }
    return jarsignerTool;
  }

  public String bundletool() {
    return this.getResource(BUNDLETOOL_JAR);
  }
}
