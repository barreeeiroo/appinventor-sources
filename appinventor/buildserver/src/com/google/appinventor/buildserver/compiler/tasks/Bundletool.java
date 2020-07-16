package com.google.appinventor.buildserver.compiler.tasks;

import com.google.appinventor.buildserver.compiler.*;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@BuildType(aab = true)
public class Bundletool implements Task {
  private final String TASK_NAME = "Bundletool";
  private AabPaths aab;

  @Override
  public TaskResult execute(ExecutorContext context) {
    this.aab = new AabPaths();

    context.getReporter().info("Creating structure");
    aab.setROOT(ExecutorUtils.createDir(context.getProject().getBuildDirectory(), "aab"));
    if (!createStructure()) {
      return TaskResult.generateError(new Exception("Could not create AAB structure"));
    }

    context.getReporter().info("Extracting protobuf resources");
    if (!extractProtobuf()) {
      return TaskResult.generateError(new Exception("Could not extract protobuf"));
    }

    context.getReporter().info("Running bundletool");
    if (!bundletool()) {
      return TaskResult.generateError(new Exception("Could not run bundletool"));
    }

    context.getReporter().info("Signing bundle");
    if (!jarsigner()) {
      return TaskResult.generateError(new Exception("Could not sign bundle"));
    }
    return TaskResult.generateSuccess();
  }

  @Override
  public String getName() {
    return TASK_NAME;
  }

  private boolean createStructure() {
    // Manifest is extracted from the protobuffed APK
    aab.setManifestDir(ExecutorUtils.createDir(aab.getROOT(), "manifest"));

    // Resources are extracted from the protobuffed APK
    aab.setResDir(ExecutorUtils.createDir(aab.getROOT(), "res"));

    // Assets are extracted from the protobuffed APK
    aab.setAssetsDir(ExecutorUtils.createDir(aab.getROOT(), "assets"));

    aab.setDexDir(ExecutorUtils.createDir(aab.getROOT(), "dex"));
    File[] dexFiles = new File(originalDexDir).listFiles();
    if (dexFiles != null) {
      for (File dex : dexFiles) {
        if (dex.isFile()) {
          try {
            Files.move(dex, new File(aab.getDexDir(), dex.getName()));
          } catch (IOException e) {
            e.printStackTrace();
            return false;
          }
        }
      }
    }

    aab.setLibDir(ExecutorUtils.createDir(aab.getROOT(), "lib"));
    File[] libFiles = originalLibsDir.listFiles();
    if (libFiles != null) {
      for (File lib : libFiles) {
        try {
          Files.move(lib, new File(ExecutorUtils.createDir(aab.getROOT(), "lib"), lib.getName()));
        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }
      }
    }

    return true;
  }
}


class AabPaths {
  private File ROOT = null;
  private File BASE = null;
  private File protoApk = null;

  private File assetsDir = null;
  private File dexDir = null;
  private File libDir = null;
  private File manifestDir = null;
  private File resDir = null;

  public File getROOT() {
    return ROOT;
  }

  public void setROOT(File ROOT) {
    this.ROOT = ROOT;
  }

  public File getBASE() {
    return BASE;
  }

  public void setBASE(File BASE) {
    this.BASE = BASE;
  }

  public File getProtoApk() {
    return protoApk;
  }

  public void setProtoApk(File protoApk) {
    this.protoApk = protoApk;
  }

  public File getAssetsDir() {
    return assetsDir;
  }

  public void setAssetsDir(File assetsDir) {
    this.assetsDir = assetsDir;
  }

  public File getDexDir() {
    return dexDir;
  }

  public void setDexDir(File dexDir) {
    this.dexDir = dexDir;
  }

  public File getLibDir() {
    return libDir;
  }

  public void setLibDir(File libDir) {
    this.libDir = libDir;
  }

  public File getManifestDir() {
    return manifestDir;
  }

  public void setManifestDir(File manifestDir) {
    this.manifestDir = manifestDir;
  }

  public File getResDir() {
    return resDir;
  }

  public void setResDir(File resDir) {
    this.resDir = resDir;
  }
}

class AabZipper {
  public static boolean zipBundle(File src, File dest, String root) {
    try {
      FileOutputStream fos = new FileOutputStream(dest);
      ZipOutputStream zipOut = new ZipOutputStream(fos);

      zipFile(src, src.getName(), zipOut, root);
      zipOut.close();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut, String root) throws IOException {
    if (fileToZip.isHidden()) {
      return;
    }
    String zipFileName = fileName;
    if (zipFileName.startsWith(root)) {
      zipFileName = zipFileName.substring(root.length());
    }

    boolean windows = !File.separator.equals("/");
    if (windows) {
      zipFileName = zipFileName.replace(File.separator, "/");
    }

    if (fileToZip.isDirectory()) {
      if (zipFileName.endsWith("/")) {
        zipOut.putNextEntry(new ZipEntry(zipFileName));
      } else {
        zipOut.putNextEntry(new ZipEntry(zipFileName + "/"));
      }
      zipOut.closeEntry();
      File[] children = fileToZip.listFiles();
      assert children != null;
      for (File childFile : children) {
        zipFile(childFile, fileName + File.separator + childFile.getName(), zipOut, root);
      }
      return;
    }

    FileInputStream fis = new FileInputStream(fileToZip);
    ZipEntry zipEntry = new ZipEntry(zipFileName);
    zipOut.putNextEntry(zipEntry);
    byte[] bytes = new byte[1024];
    int length;
    while ((length = fis.read(bytes)) >= 0) {
      zipOut.write(bytes, 0, length);
    }
    fis.close();
  }
}
