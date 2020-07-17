package com.google.appinventor.buildserver.compiler.tasks;

import com.google.appinventor.buildserver.compiler.ExecutorContext;
import com.google.appinventor.buildserver.compiler.Task;
import com.google.appinventor.buildserver.compiler.TaskResult;
import com.google.appinventor.components.common.ComponentDescriptorConstants;
import com.google.common.collect.Sets;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class LoadJsonInfo implements Task {
  private String TASK_NAME = "LoadJsonInfo";

  @Override
  public TaskResult execute(ExecutorContext context) {
    this.generateAssets();
    this.generateActivities();
    this.generateBroadcastReceivers();
    this.generateLibNames();
    this.generateNativeLibNames();
    this.generatePermissions();
    this.generateMinSdks();

    // TODO(Will): Remove the following call once the deprecated
    //             @SimpleBroadcastReceiver annotation is removed. It should
    //             should remain for the time being because otherwise we'll break
    //             extensions currently using @SimpleBroadcastReceiver.
    this.generateBroadcastReceiver();
    return null;
  }

  /*
   * Generate the set of Android libraries needed by this project.
   */
  void generateLibNames() {
    try {
      loadJsonInfo(libsNeeded, ComponentDescriptorConstants.LIBRARIES_TARGET);
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Libraries"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Libraries"));
    }

    int n = 0;
    for (String type : libsNeeded.keySet()) {
      n += libsNeeded.get(type).size();
    }

    System.out.println("Libraries needed, n = " + n);
  }

  /*
   * Generate the set of conditionally included libraries needed by this project.
   */
  void generateNativeLibNames() {
    if (isForEmulator) {  // no libraries for emulator
      return;
    }
    try {
      loadJsonInfo(nativeLibsNeeded, ComponentDescriptorConstants.NATIVE_TARGET);
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Native Libraries"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Native Libraries"));
    }

    int n = 0;
    for (String type : nativeLibsNeeded.keySet()) {
      n += nativeLibsNeeded.get(type).size();
    }

    System.out.println("Native Libraries needed, n = " + n);
  }

  /*
   * Generate the set of conditionally included assets needed by this project.
   */
  void generateAssets() {
    try {
      loadJsonInfo(assetsNeeded, ComponentDescriptorConstants.ASSETS_TARGET);
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Assets"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Assets"));
    }

    int n = 0;
    for (String type : assetsNeeded.keySet()) {
      n += assetsNeeded.get(type).size();
    }

    System.out.println("Component assets needed, n = " + n);
  }

  /*
   * Generate the set of conditionally included activities needed by this project.
   */
  void generateActivities() {
    try {
      loadJsonInfo(activitiesNeeded, ComponentDescriptorConstants.ACTIVITIES_TARGET);
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Activities"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Activities"));
    }

    int n = 0;
    for (String type : activitiesNeeded.keySet()) {
      n += activitiesNeeded.get(type).size();
    }

    System.out.println("Component activities needed, n = " + n);
  }

  /*
   * Generate a set of conditionally included broadcast receivers needed by this project.
   */
  void generateBroadcastReceivers() {
    try {
      loadJsonInfo(broadcastReceiversNeeded, ComponentDescriptorConstants.BROADCAST_RECEIVERS_TARGET);
    }
    catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceivers"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceivers"));
    }

    mergeConditionals(conditionals.get(ComponentDescriptorConstants.BROADCAST_RECEIVERS_TARGET), broadcastReceiversNeeded);
  }

  /*
   * TODO(Will): Remove this method once the deprecated @SimpleBroadcastReceiver
   *             annotation is removed. This should remain for the time being so
   *             that we don't break extensions currently using the
   *             @SimpleBroadcastReceiver annotation.
   */
  void generateBroadcastReceiver() {
    try {
      loadJsonInfo(componentBroadcastReceiver, ComponentDescriptorConstants.BROADCAST_RECEIVER_TARGET);
    }
    catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceiver"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "BroadcastReceiver"));
    }
  }

  private void generateMinSdks() {
    try {
      loadJsonInfo(minSdksNeeded, ComponentDescriptorConstants.ANDROIDMINSDK_TARGET);
    } catch (IOException|JSONException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "AndroidMinSDK"));
    }
  }

  void generatePermissions() {
    try {
      loadJsonInfo(permissionsNeeded, ComponentDescriptorConstants.PERMISSIONS_TARGET);
      if (project != null) {    // Only do this if we have a project (testing doesn't provide one :-( ).
        LOG.log(Level.INFO, "usesLocation = " + project.getUsesLocation());
        if (project.getUsesLocation().equals("True")) { // Add location permissions if any WebViewer requests it
          Set<String> locationPermissions = Sets.newHashSet(); // via a Property.
          // See ProjectEditor.recordLocationSettings()
          locationPermissions.add("android.permission.ACCESS_FINE_LOCATION");
          locationPermissions.add("android.permission.ACCESS_COARSE_LOCATION");
          locationPermissions.add("android.permission.ACCESS_MOCK_LOCATION");
          permissionsNeeded.put("com.google.appinventor.components.runtime.WebViewer", locationPermissions);
        }
      }
    } catch (IOException e) {
      // This is fatal.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Permissions"));
    } catch (JSONException e) {
      // This is fatal, but shouldn't actually ever happen.
      e.printStackTrace();
      userErrors.print(String.format(ERROR_IN_STAGE, "Permissions"));
    }

    mergeConditionals(conditionals.get(ComponentDescriptorConstants.PERMISSIONS_TARGET), permissionsNeeded);

    int n = 0;
    for (String type : permissionsNeeded.keySet()) {
      n += permissionsNeeded.get(type).size();
    }

    System.out.println("Permissions needed, n = " + n);
  }

  private void loadJsonInfo(ConcurrentMap<String, Set<String>> infoMap, String targetInfo)
      throws IOException, JSONException {
    synchronized (infoMap) {
      if (!infoMap.isEmpty()) {
        return;
      }

      JSONArray buildInfo = new JSONArray(
          "[" + simpleCompsBuildInfo.join(",") + "," +
              extCompsBuildInfo.join(",") + "]");

      for (int i = 0; i < buildInfo.length(); ++i) {
        JSONObject compJson = buildInfo.getJSONObject(i);
        JSONArray infoArray = null;
        String type = compJson.getString("type");
        try {
          infoArray = compJson.getJSONArray(targetInfo);
        } catch (JSONException e) {
          // Older compiled extensions will not have a broadcastReceiver
          // defined. Rather then require them all to be recompiled, we
          // treat the missing attribute as empty.
          if (e.getMessage().contains("broadcastReceiver")) {
            LOG.log(Level.INFO, "Component \"" + type + "\" does not have a broadcast receiver.");
            continue;
          } else if (e.getMessage().contains(ComponentDescriptorConstants.ANDROIDMINSDK_TARGET)) {
            LOG.log(Level.INFO, "Component \"" + type + "\" does not specify a minimum SDK.");
            continue;
          } else {
            throw e;
          }
        }

        if (!simpleCompTypes.contains(type) && !extCompTypes.contains(type)) {
          continue;
        }

        Set<String> infoSet = Sets.newHashSet();
        for (int j = 0; j < infoArray.length(); ++j) {
          String info = infoArray.getString(j);
          if (!info.isEmpty()) {
            infoSet.add(info);
          }
        }

        if (!infoSet.isEmpty()) {
          infoMap.put(type, infoSet);
        }

        processConditionalInfo(compJson, type, targetInfo);
      }
    }
  }

  @Override
  public String getName() {
    return this.TASK_NAME;
  }
}
