package com.google.appinventor.buildserver.compiler;


public final class ExecutorResources {
  private static final String RUNTIME_FILES_DIR = "/" + "files" + "/";

  private static final String COMP_BUILD_INFO = RUNTIME_FILES_DIR + "simple_components_build_info.json";

  private static final String BUNDLETOOL_JAR = RUNTIME_FILES_DIR + "bundletool.jar";

  private ExecutorResources() {
  }

  public static String getRuntimeFilesDir() {
    return ExecutorResources.RUNTIME_FILES_DIR;
  }

  public static String getCompBuildInfo() {
    return ExecutorUtils.getResource(COMP_BUILD_INFO);
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

  public static String bundletool() {
    return ExecutorUtils.getResource(BUNDLETOOL_JAR);
  }
}
