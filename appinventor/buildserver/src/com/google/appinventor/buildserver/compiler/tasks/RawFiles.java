package com.google.appinventor.buildserver.compiler.tasks;

import com.google.appinventor.buildserver.compiler.*;
import com.google.appinventor.buildserver.util.AARLibraries;
import com.google.appinventor.buildserver.util.AARLibrary;
import com.google.appinventor.components.common.ComponentDescriptorConstants;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * compiler.insertNativeLibs()
 * compiler.attachAarLibraries()
 * compiler.attachCompAssets()
 */
public class RawFiles implements Task {
  private final String TASK_NAME = "RawFiles";

  ExecutorContext context;


  @Override
  public TaskResult execute(ExecutorContext context) {
    this.context = context;

    // Insert native libraries
    context.getReporter().info("Attaching native libraries");
    if (!this.insertNativeLibs()) {
      return TaskResult.generateError("Error when attaching native libraries");
    }

    // Attach Android AAR Library dependencies
    context.getReporter().info("Attaching Android Archive (AAR) libraries");
    if (!this.attachAarLibraries()) {
      return TaskResult.generateError("Error when attaching AAR libraries");
    }

    // Add raw assets to sub-directory of project assets.
    context.getReporter().info("Attaching component assets");
    if (!this.attachCompAssets()) {
      return TaskResult.generateError("Error when attaching assets");
    }

    return TaskResult.generateSuccess();
  }

  /**
   * Native libraries are targeted for particular processor architectures.
   * Here, non-default architectures (ARMv5TE is default) are identified with suffixes
   * before being placed in the appropriate directory with their suffix removed.
   */
  private boolean insertNativeLibs(){
    context.getPaths().setLibsDir(ExecutorUtils.createDir(context.getPaths().getBuildDir(), YoungAndroidValues.LIBS_DIR_NAME));
    File armeabiDir = ExecutorUtils.createDir(context.getPaths().getLibsDir(), YoungAndroidValues.ARMEABI_DIR_NAME);
    File armeabiV7aDir = ExecutorUtils.createDir(context.getPaths().getLibsDir(), YoungAndroidValues.ARMEABI_V7A_DIR_NAME);
    File arm64V8aDir = ExecutorUtils.createDir(context.getPaths().getLibsDir(), YoungAndroidValues.ARM64_V8A_DIR_NAME);
    File x8664Dir = ExecutorUtils.createDir(context.getPaths().getLibsDir(), YoungAndroidValues.X86_64_DIR_NAME);

    try {
      for (String type : context.getComponentInfo().getNativeLibsNeeded().keySet()) {
        for (String lib : context.getComponentInfo().getNativeLibsNeeded().get(type)) {
          boolean isV7a = lib.endsWith(ComponentDescriptorConstants.ARMEABI_V7A_SUFFIX);
          boolean isV8a = lib.endsWith(ComponentDescriptorConstants.ARM64_V8A_SUFFIX);
          boolean isx8664 = lib.endsWith(ComponentDescriptorConstants.X86_64_SUFFIX);

          String sourceDirName;
          File targetDir;
          if (isV7a) {
            sourceDirName = YoungAndroidValues.ARMEABI_V7A_DIR_NAME;
            targetDir = armeabiV7aDir;
            lib = lib.substring(0, lib.length() - ComponentDescriptorConstants.ARMEABI_V7A_SUFFIX.length());
          } else if (isV8a) {
            sourceDirName = YoungAndroidValues.ARM64_V8A_DIR_NAME;
            targetDir = arm64V8aDir;
            lib = lib.substring(0, lib.length() - ComponentDescriptorConstants.ARM64_V8A_SUFFIX.length());
          } else if (isx8664) {
            sourceDirName = YoungAndroidValues.X86_64_DIR_NAME;
            targetDir = x8664Dir;
            lib = lib.substring(0, lib.length() - ComponentDescriptorConstants.X86_64_SUFFIX.length());
          } else {
            sourceDirName = YoungAndroidValues.ARMEABI_DIR_NAME;
            targetDir = armeabiDir;
          }

          String sourcePath;
          String pathSuffix = context.getResources().getRuntimeFilesDir() + sourceDirName + "/" + lib;

          if (context.getSimpleCompTypes().contains(type)) {
            sourcePath = context.getResource(pathSuffix);
          } else if (context.getExt().contains(type)) {
            sourcePath = ExecutorUtils.getExtCompDirPath(type, context.getProject(), context.getExtTypePathCache()) + pathSuffix;
            targetDir = ExecutorUtils.createDir(targetDir, YoungAndroidValues.EXT_COMPS_DIR_NAME);
            targetDir = ExecutorUtils.createDir(targetDir, type);
          } else {
            context.getReporter().error("There was an unexpected error while processing native code", true);
            return false;
          }

          Files.copy(new File(sourcePath), new File(targetDir, lib));
        }
      }
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      context.getReporter().error("There was an unknown error while processing native code", true);
      return false;
    }
  }

  /**
   * Attach any AAR libraries to the build.
   *
   * @return true on success, otherwise false
   */
  private boolean attachAarLibraries() {
    final File explodedBaseDir = ExecutorUtils.createDir(context.getPaths().getBuildDir(), "exploded-aars");
    final File generatedDir = ExecutorUtils.createDir(context.getPaths().getBuildDir(), "generated");
    final File genSrcDir = ExecutorUtils.createDir(generatedDir, "src");
    context.getComponentInfo().setExplodedAarLibs(new AARLibraries(genSrcDir));
    final Set<String> processedLibs = new HashSet<>();

    // Attach the Android support libraries (needed by every app)
    context.getComponentInfo().getNativeLibsNeeded().put("ANDROID", new HashSet<>(Arrays.asList(context.getResources().getSupportAars())));

    // walk components list for libraries ending in ".aar"
    try {
      for (Set<String> libs : context.getComponentInfo().getLibsNeeded().values()) {
        Iterator<String> i = libs.iterator();
        while (i.hasNext()) {
          String libname = i.next();
          if (libname.endsWith(".aar")) {
            i.remove();
            if (!processedLibs.contains(libname)) {
              // explode libraries into ${buildDir}/exploded-aars/<package>/
              AARLibrary aarLib = new AARLibrary(new File(context.getResource(context.getResources().getRuntimeFilesDir() + libname)));
              aarLib.unpackToDirectory(explodedBaseDir);
              context.getComponentInfo().getExplodedAarLibs().add(aarLib);
              processedLibs.add(libname);
            }
          }
        }
      }
      return true;
    } catch(IOException e) {
      context.getReporter().error("There was an unknown error while adding AAR libraries", true);
      return false;
    }
  }

  private boolean attachCompAssets() {
    try {
      // Gather non-library assets to be added to apk's Asset directory.
      // The assets directory have been created before this.
      File mergedAssetDir = ExecutorUtils.createDir(context.getProject().getBuildDirectory(), YoungAndroidValues.ASSET_DIR_NAME);

      // Copy component/extension assets to build/assets
      for (String type : context.getComponentInfo().getAssetsNeeded().keySet()) {
        for (String assetName : context.getComponentInfo().getAssetsNeeded().get(type)) {
          File targetDir = mergedAssetDir;
          String sourcePath;

          if (context.getSimpleCompTypes().contains(type)) {
            String pathSuffix = context.getResources().getRuntimeFilesDir() + assetName;
            sourcePath = context.getResource(pathSuffix);
          } else if (context.getExtCompTypes().contains(type)) {
            final String extCompDir = ExecutorUtils.getExtCompDirPath(type, context.getProject(), context.getExtTypePathCache());
            sourcePath = extCompDir + File.separator + YoungAndroidValues.ASSET_DIR_NAME + File.separator + assetName;
            // If targetDir's location is changed here, you must update Form.java in components to
            // reference the new location. The path for assets in compiled apps is assumed to be
            // assets/EXTERNAL-COMP-PACKAGE/ASSET-NAME
            targetDir = ExecutorUtils.createDir(targetDir, new File(extCompDir).getName());
          } else {
            context.getReporter().error("There was an unexpected error while processing assets", true);
            return false;
          }

          Files.copy(new File(sourcePath), new File(targetDir, assetName));
        }
      }

      // Copy project assets to build/assets
      File[] assets = context.getProject().getAssetsDirectory().listFiles();
      if (assets != null) {
        for (File asset : assets) {
          if (asset.isFile()) {
            Files.copy(asset, new File(mergedAssetDir, asset.getName()));
          }
        }
      }
      return true;
    } catch (IOException e) {
      context.getReporter().error("There was an unknown error while processing assets", true);
      return false;
    }
  }

  @Override
  public String getName() {
    return TASK_NAME;
  }
}
