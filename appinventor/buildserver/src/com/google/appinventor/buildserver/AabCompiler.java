package com.google.appinventor.buildserver;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Callable;

/**
 * This Callable class will convert the compiled files into an Android App Bundle.
 * An AAB file structure looks like this:
 * - assets.pb
 * - resources.pb
 * - native.pb
 * - manifest/AndroidManifest.xml
 * - dex/
 * - res/
 * - assets/
 * - lib/
 */
public class AabCompiler implements Callable<Boolean> {
  private PrintStream out;
  private BuildServer.ProgressReporter reporter;
  private File buildDir;

  private long start = 0;
  private String originalManifest = null;
  private String originalDexDir = null;
  private String originalResDir = null;
  private String originalAssetsDir = null;
  private String originalLibsDir = null;

  public AabCompiler(PrintStream out, BuildServer.ProgressReporter reporter, File buildDir) {
    assert out != null;
    assert reporter != null;
    assert buildDir != null;

    this.out = out;
    this.reporter = reporter;
    this.buildDir = buildDir;
  }

  private void out(String s) {
    out.println(s);
    System.out.println(s + "\n");
  }

  public void setStartTime(long start) {
    this.start = start;
  }

  public void setManifest(String manifest) {
    this.originalManifest = manifest;
  }

  public void setDexDir(String dexDir) {
    this.originalDexDir = dexDir;
  }

  public void setResDir(String resDir) {
    this.originalResDir = resDir;
  }

  public void setAssetsDir(String assetsDir) {
    this.originalAssetsDir = assetsDir;
  }

  public void setLibsDir(String libsDir) {
    this.originalLibsDir = libsDir;
  }

  private static File createDir(File parentDir, String name) {
    File dir = new File(parentDir, name);
    if (!dir.exists()) {
      dir.mkdir();
    }
    return dir;
  }

  @Override
  public Boolean call() {
    // Progress is at 85% now

    // First step: create the directory that will be zipped later, and start creating the AAB module layout
    out("________Creating AAB structure");
    String AAB_DIR = "aab";
    File aabDir = createDir(buildDir, AAB_DIR);
    if (!createStructure(aabDir)) {
      return false;
    }

    out("________Running Protobuf");

    out("________Zipping Files");

    out("________Running bundletool");

    // TODO: This is just added so compile process can be stopped and temporal files are been kept for debugging
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (reporter != null)
      reporter.report(100);
    if (start != 0)
      out("Build finished in " + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    return true;
  }

  private boolean createStructure(File aabDir) {
    File manifestDir = createDir(aabDir, "manifest");
    try {
      Files.move(new File(originalManifest), new File(manifestDir, "AndroidManifest.xml"));
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    File dexDir = createDir(aabDir, "dex");
    File[] dexFiles = new File(originalDexDir).listFiles();
    if (dexFiles != null) {
      for (File dex : dexFiles) {
        if (dex.isFile()) {
          try {
            Files.move(dex, new File(dexDir, dex.getName()));
          } catch (IOException e) {
            e.printStackTrace();
            return false;
          }
        }
      }
    }

    File resDir = createDir(aabDir, "res");
    File[] resFiles = new File(originalResDir).listFiles();
    if (resFiles != null) {
      for (File res : resFiles) {
        try {
          Files.move(res, new File(resDir, res.getName()));
        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }
      }
    }

    File assetsDir = createDir(aabDir, "assets");
    File[] assetFiles = new File(originalAssetsDir).listFiles();
    if (assetFiles != null) {
      for (File asset : assetFiles) {
        if (asset.isFile()) {
          try {
            Files.move(asset, new File(assetsDir, asset.getName()));
          } catch (IOException e) {
            e.printStackTrace();
            return false;
          }
        }
      }
    }

    File libDir = createDir(aabDir, "lib");
    File[] libFiles = new File(originalLibsDir).listFiles();
    if (libFiles != null) {
      for (File lib : libFiles) {
        try {
          Files.move(lib, new File(libDir, lib.getName()));
        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }
      }
    }

    return true;
  }
}
