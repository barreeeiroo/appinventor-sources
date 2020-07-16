package com.google.appinventor.buildserver.compiler;

import com.google.appinventor.buildserver.Project;

import java.util.Map;
import java.util.Set;

public class ExecutorContext {
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

  public static class Builder {
    private final Project project;
    private Set<String> compTypes;
    private Map<String, Set<String>> compBlocks;
    private Reporter reporter;
    private boolean isForCompanion = false;
    private boolean isForEmulator = false;
    private boolean includeDangerousPermissions = false;
    private String keystoreFilePath;
    private int childProcessRam = 2048;
    private String dexCacheDir = null;
    private String outputFileName = null;

    public Builder(Project project) {
      this.project = project;
    }

    public Builder withTypes(Set<String> compTypes) {
      this.compTypes = compTypes;
      return this;
    }

    public Builder withBlocks(Map<String, Set<String>> compBlocks) {
      this.compBlocks = compBlocks;
      return this;
    }

    public Builder withReporter(Reporter reporter) {
      this.reporter = reporter;
      return this;
    }

    public Builder withCompanion(boolean isForCompanion) {
      this.isForCompanion = isForCompanion;
      return this;
    }

    public Builder withEmulator(boolean isForEmulator) {
      this.isForEmulator = isForEmulator;
      return this;
    }

    public Builder withDangerousPermissions(boolean includeDangerousPermissions) {
      this.includeDangerousPermissions = includeDangerousPermissions;
      return this;
    }

    public Builder withKeystore(String keystoreFilePath) {
      this.keystoreFilePath = keystoreFilePath;
      return this;
    }

    public Builder withRam(int childProcessRam) {
      this.childProcessRam = childProcessRam;
      return this;
    }

    public Builder withCache(String dexCacheDir) {
      this.dexCacheDir = dexCacheDir;
      return this;
    }

    public Builder withOutput(String outputFileName) {
      this.outputFileName = outputFileName;
      return this;
    }

    public ExecutorContext build() {
      ExecutorContext context = new ExecutorContext();
      if (project == null) {
        System.out.println("[ERROR] ExecutorContext needs Project");
        return null;
      } else if (compTypes == null) {
        System.out.println("[ERROR] ExecutorContext needs CompTypes");
        return null;
      } else if (compBlocks == null) {
        System.out.println("[ERROR] ExecutorContext needs CompBlocks");
        return null;
      } else if (reporter == null) {
        System.out.println("[ERROR] ExecutorContext needs Reporter");
        return null;
      } else if (keystoreFilePath == null) {
        System.out.println("[ERROR] ExecutorContext needs KeystoreFilePath");
        return null;
      }
      context.project = project;
      context.compTypes = compTypes;
      context.compBlocks = compBlocks;
      context.reporter = reporter;
      context.isForCompanion = isForCompanion;
      context.isForEmulator = isForEmulator;
      context.includeDangerousPermissions = includeDangerousPermissions;
      context.keystoreFilePath = keystoreFilePath;
      context.dexCacheDir = dexCacheDir;
      context.outputFileName = outputFileName;
      context.childProcessRam = childProcessRam;

      System.out.println(this.toString());

      return context;
    }
  }

  private ExecutorContext() {
  }

  public Project getProject() {
    return this.project;
  }

  public Reporter getReporter() {
    return this.reporter;
  }

  @Override
  public String toString() {
    return "ExecutorContext{" +
        "project=" + project +
        ", compTypes=" + compTypes +
        ", compBlocks=" + compBlocks +
        ", reporter=" + reporter +
        ", isForCompanion=" + isForCompanion +
        ", isForEmulator=" + isForEmulator +
        ", includeDangerousPermissions=" + includeDangerousPermissions +
        ", keystoreFilePath='" + keystoreFilePath + '\'' +
        ", childProcessRam=" + childProcessRam +
        ", dexCacheDir='" + dexCacheDir + '\'' +
        ", outputFileName='" + outputFileName + '\'' +
        '}';
  }
}

