package com.google.appinventor.buildserver.tasks;

import com.google.appinventor.buildserver.*;
import com.google.appinventor.buildserver.util.AARLibraries;
import com.google.appinventor.buildserver.util.AARLibrary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * compiler.attachAarLibraries()
 */

@BuildType(apk = true, aab = true)
public class AttachAarLibs implements Task {
  @Override
  public TaskResult execute(CompilerContext context) {
    final File explodedBaseDir = ExecutorUtils.createDir(context.getPaths().getBuildDir(), "exploded-aars");
    final File generatedDir = ExecutorUtils.createDir(context.getPaths().getBuildDir(), "generated");
    final File genSrcDir = ExecutorUtils.createDir(generatedDir, "src");
    context.getComponentInfo().setExplodedAarLibs(new AARLibraries(genSrcDir));
    final Set<String> processedLibs = new HashSet<>();

    // Attach the Android support libraries (needed by every app)
    context.getComponentInfo().getLibsNeeded().put("ANDROID", new HashSet<>(Arrays.asList(context.getResources().getSupportAars())));

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
    } catch(IOException e) {
      context.getReporter().error("There was an unknown error while adding AAR libraries", true);
      return TaskResult.generateError(e);
    }

    return TaskResult.generateSuccess();
  }
}
