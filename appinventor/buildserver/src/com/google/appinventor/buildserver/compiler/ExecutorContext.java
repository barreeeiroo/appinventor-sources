package com.google.appinventor.buildserver.compiler;

import com.google.appinventor.buildserver.Project;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class ExecutorContext {
  InitialContext initial;
  Utils utils;

  public ExecutorContext(Project project, Set<String> compTypes, Map<String, Set<String>> compBlocks,
                         Reporter reporter, boolean isForCompanion, boolean isForEmulator,
                         boolean includeDangerousPermissions, String keystoreFilePath,
                         int childProcessRam, String dexCacheDir, String outputFileName) {
    this.initial = new InitialContext(project, compTypes, compBlocks, reporter, isForCompanion,
        isForEmulator, includeDangerousPermissions, keystoreFilePath, childProcessRam, dexCacheDir, outputFileName);
    this.utils = new Utils();
  }

  public Reporter getReporter() {
    return this.initial.getReporter();
  }

  public Utils getUtils() {
    return this.utils;
  }

  private class InitialContext {
    private Project project;
    private Set<String> compTypes;
    private Map<String, Set<String>> compBlocks;
    private Reporter reporter;
    private boolean isForCompanion;
    private boolean isForEmulator;
    private boolean includeDangerousPermissions;
    private String keystoreFilePath;
    private int childProcessRam;
    private String dexCacheDir;
    private String outputFileName;

    public InitialContext(Project project, Set<String> compTypes, Map<String, Set<String>> compBlocks,
                          Reporter reporter, boolean isForCompanion, boolean isForEmulator,
                          boolean includeDangerousPermissions, String keystoreFilePath,
                          int childProcessRam, String dexCacheDir, String outputFileName) {
      this.project = project;
      this.compTypes = compTypes;
      this.compBlocks = compBlocks;
      this.reporter = reporter;
      this.isForCompanion = isForCompanion;
      this.isForEmulator = isForEmulator;
      this.includeDangerousPermissions = includeDangerousPermissions;
      this.keystoreFilePath = keystoreFilePath;
      this.childProcessRam = childProcessRam;
      this.dexCacheDir = dexCacheDir;
      this.outputFileName = outputFileName;
    }

    public Project getProject() {
      return project;
    }

    public Set<String> getCompTypes() {
      return compTypes;
    }

    public Map<String, Set<String>> getCompBlocks() {
      return compBlocks;
    }

    public Reporter getReporter() {
      return reporter;
    }

    public boolean isForCompanion() {
      return isForCompanion;
    }

    public boolean isForEmulator() {
      return isForEmulator;
    }

    public boolean isIncludeDangerousPermissions() {
      return includeDangerousPermissions;
    }

    public String getKeystoreFilePath() {
      return keystoreFilePath;
    }

    public int getChildProcessRam() {
      return childProcessRam;
    }

    public String getDexCacheDir() {
      return dexCacheDir;
    }

    public String getOutputFileName() {
      return outputFileName;
    }
  }

  private class Utils {
    private File createDir(File parentDir, String name) {
      File dir = new File(parentDir, name);
      if (!dir.exists()) {
        dir.mkdir();
      }
      return dir;
    }
  }
}
