package com.google.appinventor.buildserver;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
  private int mx;

  private AabPaths aab;
  private long start = 0;
  private String originalManifest = null;
  private String originalDexDir = null;
  private String originalResDir = null;
  private String originalAssetsDir = null;
  private String originalLibsDir = null;

  private String aapt2 = null;
  private String androidRuntime = null;

  private static class AabPaths {
    public File ROOT = null;

    public File assetsDir = null;
    public File dexDir = null;
    public File libDir = null;
    public File manifestDir = null;
    public File resDir = null;
  }

  public AabCompiler(PrintStream out, BuildServer.ProgressReporter reporter, File buildDir, int mx) {
    assert out != null;
    assert reporter != null;
    assert buildDir != null;
    assert mx > 0;

    this.out = out;
    this.reporter = reporter;
    this.buildDir = buildDir;
    this.mx = mx;

    aab = new AabPaths();
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

  public void setAapt2(String aapt2) {
    this.aapt2 = aapt2;
  }

  public void setAndroidRuntime(String androidRuntime) {
    this.androidRuntime = androidRuntime;
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
    aab.ROOT = aabDir;

    out("________Generating AAB resources");
    if (!generateResources(aabDir)) {
      // return false;
    }

    out("________Zipping Files");

    out("________Running bundletool");

    // TODO: This is just added so compile process can be stopped and temporal files are been kept for debugging
    try {
      Thread.sleep(10000);
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
    aab.manifestDir = createDir(aabDir, "manifest");
    /* File manifest = new File(aab.manifestDir, "AndroidManifest.xml");
    try {
      Files.move(new File(originalManifest), manifest);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    } */

    aab.dexDir = createDir(aabDir, "dex");
    File[] dexFiles = new File(originalDexDir).listFiles();
    if (dexFiles != null) {
      for (File dex : dexFiles) {
        if (dex.isFile()) {
          try {
            Files.move(dex, new File(aab.dexDir, dex.getName()));
          } catch (IOException e) {
            e.printStackTrace();
            return false;
          }
        }
      }
    }

    // Resources are linked in the AAPT2 step
    aab.resDir = createDir(aabDir, "res");

    aab.assetsDir = createDir(aabDir, "assets");
    /* File[] assetFiles = new File(originalAssetsDir).listFiles();
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
    } */

    aab.libDir = createDir(aabDir, "lib");
    File[] libFiles = new File(originalLibsDir).listFiles();
    if (libFiles != null) {
      for (File lib : libFiles) {
        try {
          Files.move(lib, new File(aab.libDir, lib.getName()));
        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }
      }
    }

    return true;
  }

  private boolean generateResources(File aabDir) {
    /* File compiledResourcesDir = createDir(new File(aabDir + File.separator + ".."), "res_compiled");
    int packSize = 100;
    List<String> resources = Filewalker.walk(originalResDir);
    for (int i = 0; i < resources.size(); i = i + 10) {
      List<String> aapt2CommandLine = new ArrayList<>();
      aapt2CommandLine.add(this.aapt2);
      aapt2CommandLine.add("compile");
      for (int j = 0; j < packSize && (i + j) < resources.size(); j++) {
        aapt2CommandLine.add(resources.get(i + j));
      }
      aapt2CommandLine.add("-o");
      aapt2CommandLine.add(compiledResourcesDir.getAbsolutePath());
      aapt2CommandLine.add("-v");
      String[] aapt2PackageCommandLine = aapt2CommandLine.toArray(new String[aapt2CommandLine.size()]);
      if (!Execution.execute(null, aapt2PackageCommandLine, System.out, System.err)) {
        return false;
      }
    } */

    /* for (File f1 : Objects.requireNonNull(new File(originalResDir).listFiles())) {
      List<String> aapt2CommandLine = new ArrayList<>();
      aapt2CommandLine.add(this.aapt2);
      aapt2CommandLine.add("compile");
      if (f1.isFile()) {
        // Is this even possible?
        aapt2CommandLine.add(f1.getAbsolutePath());
      } else {
        for (File f2 : Objects.requireNonNull(f1.listFiles())) {
          aapt2CommandLine.add(f2.getAbsolutePath());
        }
      }
      aapt2CommandLine.add("-o");
      aapt2CommandLine.add(compiledResourcesDir.getAbsolutePath());
      aapt2CommandLine.add("-v");
      String[] aapt2PackageCommandLine = aapt2CommandLine.toArray(new String[aapt2CommandLine.size()]);
      if (!Execution.execute(null, aapt2PackageCommandLine, System.out, System.err)) {
        return false;
      }
    } */

    List<String> aapt2CommandLine = new ArrayList<>();
    aapt2CommandLine.add(this.aapt2);
    aapt2CommandLine.add("compile");
    aapt2CommandLine.add("--dir");
    aapt2CommandLine.add(originalResDir);
    aapt2CommandLine.add("-o");
    aapt2CommandLine.add(aabDir.getAbsolutePath() + File.separator + "resources.zip");
    aapt2CommandLine.add("--no-crunch");
    aapt2CommandLine.add("-v");
    String[] aapt2PackageCommandLine = aapt2CommandLine.toArray(new String[aapt2CommandLine.size()]);
    if (!Execution.execute(null, aapt2PackageCommandLine, System.out, System.err)) {
      return false;
    }
    if (!linkResources(aabDir)) {
      return false;
    }
    return true;
  }

  private boolean linkResources(File aabDir) {
    List<String> aapt2CommandLine = new ArrayList<String>();
    aapt2CommandLine.add(this.aapt2);
    aapt2CommandLine.add("link");
    aapt2CommandLine.add("--proto-format");
    aapt2CommandLine.add("-o");
    aapt2CommandLine.add(new File(aabDir, "output.apk").getAbsolutePath());
    aapt2CommandLine.add("-I");
    aapt2CommandLine.add(androidRuntime);
    aapt2CommandLine.add("-R");
    aapt2CommandLine.add(aabDir.getAbsolutePath() + File.separator + "resources.zip");
    // aapt2CommandLine.add(originalRJavaDir + File.separator + "R.txt");
    aapt2CommandLine.add("-A");
    aapt2CommandLine.add(originalAssetsDir);
    aapt2CommandLine.add("--manifest");
    aapt2CommandLine.add(originalManifest);
    aapt2CommandLine.add("--auto-add-overlay");
    aapt2CommandLine.add("--no-version-vectors");
    aapt2CommandLine.add("--no-auto-version");
    aapt2CommandLine.add("--no-version-transitions");
    aapt2CommandLine.add("--no-resource-deduping");
    String[] aapt2PackageCommandLine = aapt2CommandLine.toArray(new String[aapt2CommandLine.size()]);
    if (!Execution.execute(null, aapt2PackageCommandLine, System.out, System.err)) {
      return false;
    }
    return true;
  }

  private static class Filewalker {
    public static List<String> walk(String path) {
      List<String> files = new ArrayList<>();

      File root = new File(path);
      File[] list = root.listFiles();

      if (list == null) return files;
      for (File f : list) {
        if (f.isDirectory()) {
          files.addAll(walk(f.getAbsolutePath()));
        } else {
          System.out.println(f.getAbsolutePath());
          files.add(f.getAbsolutePath());
        }
      }
      return files;
    }
  }
}
