package com.google.appinventor.buildserver.compiler.tasks;

import com.google.appinventor.buildserver.compiler.*;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * prepareCompTypes(compTypes);
 * readBuildInfo();
 */
@BuildType(apk = true, aab = true)
public class ReadBuildInfo implements Task {
  private final String TASK_NAME = "ReadBuildInfo";

  @Override
  public TaskResult execute(ExecutorContext context) {
    try {
      JSONArray buildInfo = new JSONArray(context.getResources().getCompBuildInfo());

      Set<String> allSimpleTypes = Sets.newHashSet();
      for (int i = 0; i < buildInfo.length(); ++i) {
        JSONObject comp = buildInfo.getJSONObject(i);
        allSimpleTypes.add(comp.getString("type"));
      }

      HashSet<String> simpleCompTypes = Sets.newHashSet(context.getCompTypes());
      simpleCompTypes.retainAll(allSimpleTypes);
      context.setSimpleCompTypes(simpleCompTypes);

      HashSet<String> extCompTypes = Sets.newHashSet(context.getCompTypes());
      extCompTypes.removeAll(allSimpleTypes);
      context.setExtCompTypes(extCompTypes);
    } catch (JSONException e) {
      e.printStackTrace();
      return TaskResult.generateError(e);
    }

    try {
      JSONArray simpleCompsBuildInfo = new JSONArray(context.getResources().getCompBuildInfo());
      context.setSimpleCompsBuildInfo(simpleCompsBuildInfo);

      JSONArray extCompsBuildInfo = new JSONArray();
      Set<String> readComponentInfos = new HashSet<String>();
      for (String type : context.getExtCompTypes()) {
        // .../assets/external_comps/com.package.MyExtComp/files/component_build_info.json
        File extCompRuntimeFileDir = new File(ExecutorUtils.getExtCompDirPath(type, context.getProject(), context.getExtTypePathCache())
            + context.getResources().getRuntimeFilesDir());
        if (!extCompRuntimeFileDir.exists()) {
          // try extension package name for multi-extension files
          String path = ExecutorUtils.getExtCompDirPath(type, context.getProject(), context.getExtTypePathCache());
          path = path.substring(0, path.lastIndexOf('.'));
          extCompRuntimeFileDir = new File(path + context.getResources().getRuntimeFilesDir());
        }
        File jsonFile = new File(extCompRuntimeFileDir, "component_build_infos.json");
        if (!jsonFile.exists()) {
          // old extension with a single component?
          jsonFile = new File(extCompRuntimeFileDir, "component_build_info.json");
          if (!jsonFile.exists()) {
            throw new IllegalStateException("No component_build_info.json in extension for " + type);
          }
        }
        if (readComponentInfos.contains(jsonFile.getAbsolutePath())) {
          continue;  // already read the build infos for this type (bundle extension)
        }

        String buildInfo = com.google.common.io.Resources.toString(jsonFile.toURI().toURL(), Charsets.UTF_8);
        JSONTokener tokener = new JSONTokener(buildInfo);
        Object value = tokener.nextValue();
        if (value instanceof JSONObject) {
          extCompsBuildInfo.put(value);
          readComponentInfos.add(jsonFile.getAbsolutePath());
        } else if (value instanceof JSONArray) {
          JSONArray infos = (JSONArray) value;
          for (int i = 0; i < infos.length(); i++) {
            extCompsBuildInfo.put(infos.getJSONObject(i));
          }
          readComponentInfos.add(jsonFile.getAbsolutePath());
        }
      }
      context.setExtCompsBuildInfo(extCompsBuildInfo);
    } catch (JSONException | IOException e) {
      return TaskResult.generateError(e);
    }
    return TaskResult.generateSuccess();
  }

  @Override
  public String getName() {
    return this.TASK_NAME;
  }
}
