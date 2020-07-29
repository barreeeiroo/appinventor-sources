package com.google.appinventor.buildserver.compiler.context;

import java.io.File;

public class Paths {
  private final String outputFileName;

  private File buildDir;
  private File deployDir;
  private File resDir;
  private File drawableDir;
  private File tmpDir;
  private File libsDir;
  private File manifest;

  public Paths(String outputFileName) {
    this.outputFileName = outputFileName;
  }

  public File getBuildDir() {
    return buildDir;
  }

  public void setBuildDir(File buildDir) {
    this.buildDir = buildDir;
  }

  public File getDeployDir() {
    return deployDir;
  }

  public File getDeployFile() {
    return new File(this.deployDir, this.outputFileName);
  }

  public void setDeployDir(File deployDir) {
    this.deployDir = deployDir;
  }

  public File getResDir() {
    return resDir;
  }

  public void setResDir(File resDir) {
    this.resDir = resDir;
  }

  public File getDrawableDir() {
    return drawableDir;
  }

  public void setDrawableDir(File drawableDir) {
    this.drawableDir = drawableDir;
  }

  public File getTmpDir() {
    return tmpDir;
  }

  public void setTmpDir(File tmpDir) {
    this.tmpDir = tmpDir;
  }

  public File getLibsDir() {
    return libsDir;
  }

  public void setLibsDir(File libsDir) {
    this.libsDir = libsDir;
  }

  public File getManifest() {
    return manifest;
  }

  public void setManifest(File manifest) {
    this.manifest = manifest;
  }

  @Override
  public String toString() {
    return "Paths{" +
        "outputFileName='" + outputFileName + '\'' +
        ", buildDir=" + buildDir +
        ", deployDir=" + deployDir +
        ", resDir=" + resDir +
        ", drawableDir=" + drawableDir +
        ", tmpDir=" + tmpDir +
        ", libsDir=" + libsDir +
        ", manifest=" + manifest +
        '}';
  }
}
