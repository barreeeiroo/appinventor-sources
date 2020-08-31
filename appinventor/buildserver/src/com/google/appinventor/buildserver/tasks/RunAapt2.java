package com.google.appinventor.buildserver.tasks;

import com.google.appinventor.buildserver.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@BuildType(aab = true)
public class RunAapt2 implements Task {
  CompilerContext context;
  File resourcesZip;

  @Override
  public TaskResult execute(CompilerContext context) {
    this.context = context;

    File sourceOutputDir = ExecutorUtils.createDir(context.getPaths().getBuildDir(), "generated/src");
    File symbolOutputDir = ExecutorUtils.createDir(context.getPaths().getBuildDir(), "generated/symbols");
    // Need to make sure assets directory exists otherwise aapt2 will fail.
    context.getPaths().setAssetsDir(ExecutorUtils.createDir(context.getProject().getBuildDirectory(), YoungAndroidConstants.ASSET_DIR_NAME));

    String aapt2Tool = context.getResources().aapt2();
    if (aapt2Tool == null) {
      return TaskResult.generateError("Could not find a suitable AAPT tool for this OS");
    }

    if (!this.runAapt2Compile(aapt2Tool)) {
      return TaskResult.generateError("Error while compiling with AAPT2");
    }
    if (!this.runAapt2Link(aapt2Tool, symbolOutputDir)) {
      return TaskResult.generateError("Error while linking with AAPT2");
    }
    return TaskResult.generateSuccess();
  }

  private boolean runAapt2Compile(String aapt2Tool) {
    resourcesZip = new File(context.getPaths().getResDir(), "resources.zip");

    List<String> aapt2CommandLine = new ArrayList<>();
    aapt2CommandLine.add(aapt2Tool);
    aapt2CommandLine.add("compile");
    aapt2CommandLine.add("--dir");
    aapt2CommandLine.add(context.getPaths().getMergedResDir().getAbsolutePath());
    aapt2CommandLine.add("-o");
    aapt2CommandLine.add(resourcesZip.getAbsolutePath());
    aapt2CommandLine.add("--no-crunch");
    aapt2CommandLine.add("-v");
    String[] aapt2CompileCommandLine = aapt2CommandLine.toArray(new String[0]);

    if (!Execution.execute(null, aapt2CompileCommandLine, context.getReporter().getSystemOut(), System.err)) {
      context.getReporter().error("Could not execute AAPT2 compile step");
      return false;
    }

    return true;
  }

  private boolean runAapt2Link(String aapt2Tool, File symbolOutputDir) {
    context.getResources().setAppRTxt(new File(symbolOutputDir, "R.txt"));

    List<String> aapt2CommandLine = new ArrayList<>();
    aapt2CommandLine.add(aapt2Tool);
    aapt2CommandLine.add("link");
    aapt2CommandLine.add("--proto-format");
    aapt2CommandLine.add("-o");
    aapt2CommandLine.add(context.getPaths().getTmpPackageName().getAbsolutePath());
    aapt2CommandLine.add("-I");
    aapt2CommandLine.add(context.getResources().getAndroidRuntime());
    aapt2CommandLine.add("-R");
    aapt2CommandLine.add(resourcesZip.getAbsolutePath());
    aapt2CommandLine.add("-A");
    aapt2CommandLine.add(context.getPaths().getAssetsDir().getAbsolutePath());
    aapt2CommandLine.add("--manifest");
    aapt2CommandLine.add(context.getPaths().getManifest().getAbsolutePath());
    aapt2CommandLine.add("--output-text-symbols");
    aapt2CommandLine.add(context.getResources().getAppRTxt().getAbsolutePath());
    aapt2CommandLine.add("--auto-add-overlay");
    aapt2CommandLine.add("--no-version-vectors");
    aapt2CommandLine.add("--no-auto-version");
    aapt2CommandLine.add("--no-version-transitions");
    aapt2CommandLine.add("--no-resource-deduping");
    aapt2CommandLine.add("-v");
    String[] aapt2LinkCommandLine = aapt2CommandLine.toArray(new String[0]);

    if (!Execution.execute(null, aapt2LinkCommandLine, context.getReporter().getSystemOut(), System.err)) {
      context.getReporter().error("Could not execute AAPT2 link step");
      return false;
    }

    return true;
  }
}
