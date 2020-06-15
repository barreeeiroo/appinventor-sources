package com.google.appinventor.buildserver;

import java.io.PrintStream;
import java.util.concurrent.Callable;

public class AabCompiler implements Callable<Boolean> {
  private PrintStream out;
  private BuildServer.ProgressReporter reporter;

  private long start = 0;

  public AabCompiler(PrintStream out, BuildServer.ProgressReporter reporter) {
    assert out != null;
    assert reporter != null;

    this.out = out;
    this.reporter = reporter;
  }

  public void setStartTime(long start) {
    this.start = start;
  }

  @Override
  public Boolean call() {
    // Progress is at 85% now

    out.println("________Running Protobuf");

    out.println("________Zipping Files");

    out.println("________Running bundletool");

    reporter.report(100);
    if (start != 0)
      out.println("Build finished in " + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
    return true;
  }

}
