package com.google.appinventor.buildserver;

import com.google.common.io.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
  private String bundletool = null;
  private String jarsigner = null;

  private String androidRuntime = null;
  private String originalRtxt = null;
  private String deploy = null;
  private String keystore = null;

  private class AabPaths {
    private File ROOT = null;
    private File BASE = null;

    private File assetsDir = null;
    private File dexDir = null;
    private File libDir = null;
    private File manifestDir = null;
    private File resDir = null;

    private File protoApk = null;
    private File resZip = null;

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

    public File getProtoApk() {
      return protoApk;
    }

    public void setProtoApk(File protoApk) {
      this.protoApk = protoApk;
    }

    public File getResZip() {
      return resZip;
    }

    public void setResZip(File resZip) {
      this.resZip = resZip;
    }
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

  public void setBundletool(String bundletool) {
    this.bundletool = bundletool;
  }

  public void setDeploy(String deploy) {
    this.deploy = deploy;
  }

  public void setKeystore(String keystore) {
    this.keystore = keystore;
  }

  public void setJarsigner(String jarsigner) {
    this.jarsigner = jarsigner;
  }

  public void setOriginalRtxt(String rtxt) {
    this.originalRtxt = rtxt;
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
    aab.setROOT(createDir(buildDir, "aab"));
    if (!createStructure()) {
      return false;
    }

    out("________Generating AAB resources");
    if (!generateResources()) {
      return false;
    }

    out("___________Linking AAB resources");
    if (!linkResources()) {
      // return false;
    }

    out("___________Extracting protobuf resources");
    if (!extractProtobuf()) {
      // return false;
    }

    out("________Running bundletool");
    if (!bundletool()) {
      // return false;
    }

    out("________Signing bundle");
    if (!jarsigner()) {
      // return false;
    }

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

  private boolean createStructure() {
    // Manifest is extracted from the protobuf APK
    aab.setManifestDir(createDir(aab.ROOT, "manifest"));

    // Resources are extracted from the protobuf APK
    aab.setResDir(createDir(aab.ROOT, "res"));

    // Assets are extracted from the protobuf APK
    aab.setAssetsDir(createDir(aab.ROOT, "assets"));

    aab.setDexDir(createDir(aab.ROOT, "dex"));
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

    aab.setLibDir(createDir(aab.ROOT, "lib"));
    File[] libFiles = new File(originalLibsDir).listFiles();
    if (libFiles != null) {
      for (File lib : libFiles) {
        try {
          Files.move(lib, new File(createDir(aab.ROOT, "lib"), lib.getName()));
        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }
      }
    }

    return true;
  }

  private boolean generateResources() {
    aab.setResZip(new File(aab.getROOT().getAbsolutePath(), "resources.zip"));

    List<String> aapt2CommandLine = new ArrayList<>();
    aapt2CommandLine.add(this.aapt2);
    aapt2CommandLine.add("compile");
    aapt2CommandLine.add("--dir");
    aapt2CommandLine.add(originalResDir);
    aapt2CommandLine.add("-o");
    aapt2CommandLine.add(aab.getResZip().getAbsolutePath());
    aapt2CommandLine.add("--no-crunch");
    aapt2CommandLine.add("-v");
    String[] aapt2CompileCommandLine = aapt2CommandLine.toArray(new String[0]);

    return Execution.execute(null, aapt2CompileCommandLine, System.out, System.err);
  }

  private boolean linkResources() {
    aab.setProtoApk(new File(aab.getROOT().getAbsolutePath(), "output.apk"));

    List<String> aapt2CommandLine = new ArrayList<String>();
    aapt2CommandLine.add(this.aapt2);
    aapt2CommandLine.add("link");
    aapt2CommandLine.add("--proto-format");
    aapt2CommandLine.add("-o");
    aapt2CommandLine.add(aab.getProtoApk().getAbsolutePath());
    aapt2CommandLine.add("-I");
    aapt2CommandLine.add(androidRuntime);
    aapt2CommandLine.add("-R");
    aapt2CommandLine.add(aab.getResZip().getAbsolutePath());
    aapt2CommandLine.add("-A");
    aapt2CommandLine.add(originalAssetsDir);
    aapt2CommandLine.add("--manifest");
    aapt2CommandLine.add(originalManifest);
    aapt2CommandLine.add("--emit-ids");
    aapt2CommandLine.add(buildDir.getAbsolutePath() + "/ids.txt");
    aapt2CommandLine.add("--auto-add-overlay");
    aapt2CommandLine.add("--no-version-vectors");
    aapt2CommandLine.add("--no-auto-version");
    aapt2CommandLine.add("--no-version-transitions");
    aapt2CommandLine.add("--no-resource-deduping");
    aapt2CommandLine.add("--non-final-ids");
    aapt2CommandLine.add("-v");
    String[] aapt2LinkCommandLine = aapt2CommandLine.toArray(new String[0]);

    return Execution.execute(null, aapt2LinkCommandLine, System.out, System.err);
  }

  private boolean extractProtobuf() {
    try (ZipInputStream is = new ZipInputStream(new FileInputStream(aab.getProtoApk()))) {
      ZipEntry entry;
      byte[] buffer = new byte[1024];
      while ((entry = is.getNextEntry()) != null) {
        String n = entry.getName();
        File f = null;
        if (n.equals("AndroidManifest.xml")) {
          f = new File(aab.getManifestDir(), n);
        } else if (n.equals("resources.pb")) {
          f = new File(aab.getROOT(), n);
        } else if (n.startsWith("assets")) {
          f = new File(aab.getAssetsDir(), n.substring(("assets").length()));
        } else if (n.startsWith("res")) {
          f = new File(aab.getResDir(), n.substring(("res").length()));
        }

        if (f != null) {
          f.getParentFile().mkdirs();
          FileOutputStream fos = new FileOutputStream(f);
          int len;
          while ((len = is.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
          fos.close();
        }
      }

      is.close();
      return aab.getResZip().delete() && aab.getProtoApk().delete();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  private boolean bundletool() {
    aab.setBASE(new File(buildDir, "base.zip"));

    if (!AabZipper.zipBundle(aab.getROOT(), aab.getBASE(), aab.getROOT().getName() + File.separator)) {
      return false;
    }

    List<String> bundletoolCommandLine = new ArrayList<String>();
    bundletoolCommandLine.add(System.getProperty("java.home") + "/bin/java");
    bundletoolCommandLine.add("-jar");
    bundletoolCommandLine.add("-mx" + mx + "M");
    bundletoolCommandLine.add(bundletool);
    bundletoolCommandLine.add("build-bundle");
    bundletoolCommandLine.add("--modules=" + aab.getBASE());
    bundletoolCommandLine.add("--output=" + deploy);
    String[] bundletoolBuildCommandLine = bundletoolCommandLine.toArray(new String[0]);

    return Execution.execute(null, bundletoolBuildCommandLine, System.out, System.err);
  }

  private boolean jarsigner() {
    List<String> jarsignerCommandLine = new ArrayList<String>();
    jarsignerCommandLine.add(jarsigner);
    jarsignerCommandLine.add("-sigalg");
    jarsignerCommandLine.add("SHA256withRSA");
    jarsignerCommandLine.add("-digestalg");
    jarsignerCommandLine.add("SHA-256");
    jarsignerCommandLine.add("-keystore");
    jarsignerCommandLine.add(keystore);
    jarsignerCommandLine.add("-storepass");
    jarsignerCommandLine.add("android");
    jarsignerCommandLine.add(deploy);
    jarsignerCommandLine.add("AndroidKey");
    String[] jarsignerSignCommandLine = jarsignerCommandLine.toArray(new String[0]);

    return Execution.execute(null, jarsignerSignCommandLine, System.out, System.err);
  }
}
