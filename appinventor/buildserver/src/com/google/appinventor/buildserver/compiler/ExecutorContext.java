package com.google.appinventor.buildserver.compiler;

import com.google.appinventor.buildserver.BuildServer.ProgressReporter;
import com.google.appinventor.buildserver.Project;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

public class ExecutorContext {
  InitialContext initial;

  public ExecutorContext(Project project, Set<String> compTypes, Map<String, Set<String>> compBlocks,
                                           PrintStream out, PrintStream err, PrintStream userErrors,
                                           boolean isForCompanion, boolean isForEmulator,
                                           boolean includeDangerousPermissions, String keystoreFilePath,
                                           int childProcessRam, String dexCacheDir, String outputFileName,
                                           ProgressReporter reporter, boolean isAab) {
    this.initial = new InitialContext(project, compTypes, compBlocks,out, err, userErrors, isForCompanion,
        isForEmulator, includeDangerousPermissions, keystoreFilePath, childProcessRam, dexCacheDir, outputFileName,
        reporter, isAab);
  }

  private class InitialContext {
    private Project project;
    private Set<String> compTypes;
    private Map<String, Set<String>> compBlocks;
    private PrintStream out;
    private PrintStream err;
    private PrintStream userErrors;
    private boolean isForCompanion;
    private boolean isForEmulator;
    private boolean includeDangerousPermissions;
    private String keystoreFilePath;
    private int childProcessRam;
    private String dexCacheDir;
    private String outputFileName;
    private ProgressReporter reporter;
    private boolean isAab;

    public InitialContext(Project project, Set<String> compTypes, Map<String, Set<String>> compBlocks,
                          PrintStream out, PrintStream err, PrintStream userErrors,
                          boolean isForCompanion, boolean isForEmulator,
                          boolean includeDangerousPermissions, String keystoreFilePath,
                          int childProcessRam, String dexCacheDir, String outputFileName,
                          ProgressReporter reporter, boolean isAab) {
      this.project = project;
      this.compTypes = compTypes;
      this.compBlocks = compBlocks;
      this.out = out;
      this.err = err;
      this.userErrors = userErrors;
      this.isForCompanion = isForCompanion;
      this.isForEmulator = isForEmulator;
      this.includeDangerousPermissions = includeDangerousPermissions;
      this.keystoreFilePath = keystoreFilePath;
      this.childProcessRam = childProcessRam;
      this.dexCacheDir = dexCacheDir;
      this.outputFileName = outputFileName;
      this.reporter = reporter;
      this.isAab = isAab;
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

    public PrintStream getOut() {
      return out;
    }

    public PrintStream getErr() {
      return err;
    }

    public PrintStream getUserErrors() {
      return userErrors;
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

    public ProgressReporter getReporter() {
      return reporter;
    }

    public boolean isAab() {
      return isAab;
    }
  }
}
