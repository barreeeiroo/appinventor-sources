package com.google.appinventor.buildserver.compiler;


public final class ExecutorResources {
  public static final String RUNTIME_FILES_DIR = "/" + "files" + "/";

  private static final String BUNDLETOOL_JAR =
      RUNTIME_FILES_DIR + "bundletool.jar";

  private ExecutorResources() {
  }

  public static String bundletool() {
    return ExecutorUtils.getResource(BUNDLETOOL_JAR);
  }

  public static String jarsigner() {
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
}
