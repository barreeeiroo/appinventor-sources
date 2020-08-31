package com.google.appinventor.buildserver.tasks;

import com.google.appinventor.buildserver.*;
import com.google.appinventor.components.common.YaVersion;
import com.google.common.collect.Sets;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;


/**
 * compiler.writeAndroidManifest()
 */
// CreateManifest
@BuildType(apk = true, aab = true)
public class CreateManifest implements Task {
  @Override
  public TaskResult execute(CompilerContext context) {
    context.getPaths().setManifest(new File(context.getPaths().getBuildDir(), "AndroidManifest.xml"));

    // Create AndroidManifest.xml
    context.getReporter().info("Reading project specs...");
    String mainClass = context.getProject().getMainClass();
    String packageName = Signatures.getPackageName(mainClass);
    String className = Signatures.getClassName(mainClass);
    String projectName = context.getProject().getProjectName();
    String vCode = (context.getProject().getVCode() == null) ? YoungAndroidConstants.DEFAULT_VERSION_CODE : context.getProject().getVCode();
    String vName = (context.getProject().getVName() == null) ? YoungAndroidConstants.DEFAULT_VERSION_NAME : cleanName(context.getProject().getVName());
    if (context.isIncludeDangerousPermissions()) {
      vName += "u";
    }
    String aName = (context.getProject().getAName() == null) ? YoungAndroidConstants.DEFAULT_APP_NAME : cleanName(context.getProject().getAName());
    context.getReporter().log("VCode: " + context.getProject().getVCode());
    context.getReporter().log("VName: " + context.getProject().getVName());

    // TODO(user): Use com.google.common.xml.XmlWriter
    try {
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(context.getPaths().getManifest()), StandardCharsets.UTF_8));
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      // TODO(markf) Allow users to set versionCode and versionName attributes.
      // See http://developer.android.com/guide/publishing/publishing.html for
      // more info.
      out.write("<manifest " +
          "xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
          "package=\"" + packageName + "\" " +
          // TODO(markf): uncomment the following line when we're ready to enable publishing to the
          // Android Market.
          "android:versionCode=\"" + vCode + "\" " + "android:versionName=\"" + vName + "\" " +
          ">\n");

      // If we are building the Wireless Debugger (AppInventorDebugger) add the uses-feature tag which
      // is used by the Google Play store to determine which devices the app is available for. By adding
      // these lines we indicate that we use these features BUT THAT THEY ARE NOT REQUIRED so it is ok
      // to make the app available on devices that lack the feature. Without these lines the Play Store
      // makes a guess based on permissions and assumes that they are required features.
      if (context.isForCompanion()) {
        out.write("  <uses-feature android:name=\"android.hardware.bluetooth\" android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.location\" android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.telephony\" android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.location.network\" android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.location.gps\" android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.microphone\" android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.touchscreen\" android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.camera\" android:required=\"false\" />\n");
        out.write("  <uses-feature android:name=\"android.hardware.camera.autofocus\" android:required=\"false\" />\n");
        if (context.isForEmulator()) {
          out.write("  <uses-feature android:name=\"android.hardware.wifi\" android:required=\"false\" />\n"); // We actually require wifi
        } else {
          out.write("  <uses-feature android:name=\"android.hardware.wifi\" />\n"); // We actually require wifi
        }
      }

      int minSdk = Integer.parseInt((context.getProject().getMinSdk() == null) ? YoungAndroidConstants.DEFAULT_MIN_SDK : context.getProject().getMinSdk());
      if (!context.isForCompanion()) {
        for (Set<String> minSdks : context.getComponentInfo().getMinSdksNeeded().values()) {
          for (String sdk : minSdks) {
            int sdkInt = Integer.parseInt(sdk);
            if (sdkInt > minSdk) {
              minSdk = sdkInt;
            }
          }
        }
      }
      context.getReporter().log("Min SDK " + minSdk);

      // make permissions unique by putting them in one set
      Set<String> permissions = Sets.newHashSet();
      for (Set<String> compPermissions : context.getComponentInfo().getPermissionsNeeded().values()) {
        permissions.addAll(compPermissions);
      }

      // Remove Google's Forbidden Permissions
      // This code is crude because we had to do this on short notice
      // List of permissions taken from
      // https://support.google.com/googleplay/android-developer/answer/9047303#intended
      if (context.isForCompanion() && !context.isIncludeDangerousPermissions()) {
        // Default SMS handler
        permissions.remove("android.permission.READ_SMS");
        permissions.remove("android.permission.RECEIVE_MMS");
        permissions.remove("android.permission.RECEIVE_SMS");
        permissions.remove("android.permission.RECEIVE_WAP_PUSH");
        permissions.remove("android.permission.SEND_SMS");
        permissions.remove("android.permission.WRITE_SMS");
        // Default Phone handler
        permissions.remove("android.permission.PROCESS_OUTGOING_CALLS");
        permissions.remove("android.permission.CALL_PHONE");
        permissions.remove("android.permission.READ_CALL_LOG");
        permissions.remove("android.permission.WRITE_CALL_LOG");
      }

      for (String permission : permissions) {
        context.getReporter().log("Needs permission " + permission);
        out.write("  <uses-permission android:name=\"" + permission + "\" />\n");
      }

      if (context.isForCompanion()) { // This is so ACRA can do a logcat on phones older then Jelly Bean
        out.write("  <uses-permission android:name=\"android.permission.READ_LOGS\" />\n");
      }

      // TODO(markf): Change the minSdkVersion below if we ever require an SDK beyond 1.5.
      // The market will use the following to filter apps shown to devices that don't support
      // the specified SDK version.  We right now support building for minSDK 4.
      // We might also want to allow users to specify minSdk version or targetSDK version.
      out.write("  <uses-sdk android:minSdkVersion=\"" + minSdk + "\" android:targetSdkVersion=\"" + YaVersion.TARGET_SDK_VERSION + "\" />\n");

      out.write("  <application ");

      // TODO(markf): The preparing to publish doc at
      // http://developer.android.com/guide/publishing/preparing.html suggests removing the
      // 'debuggable=true' but I'm not sure that our users would want that while they're still
      // testing their packaged apps.  Maybe we should make that an option, somehow.
      // TODONE(jis): Turned off debuggable. No one really uses it and it represents a security
      // risk for App Inventor App end-users.
      out.write("android:debuggable=\"false\" ");
      // out.write("android:debuggable=\"true\" "); // DEBUGGING
      if (aName.equals("")) {
        out.write("android:label=\"" + projectName + "\" ");
      } else {
        out.write("android:label=\"" + aName + "\" ");
      }
      out.write("android:networkSecurityConfig=\"@xml/network_security_config\" ");
      out.write("android:icon=\"@mipmap/ic_launcher\" ");
      out.write("android:roundIcon=\"@mipmap/ic_launcher\" ");
      if (context.isForCompanion()) {              // This is to hook into ACRA
        out.write("android:name=\"com.google.appinventor.components.runtime.ReplApplication\" ");
      } else {
        out.write("android:name=\"com.google.appinventor.components.runtime.multidex.MultiDexApplication\" ");
      }
      // Write theme info if we are not using the "Classic" theme (i.e., no theme)
      //      if (!"Classic".equalsIgnoreCase(project.getTheme())) {
      out.write("android:theme=\"@style/AppTheme\" ");
      out.write(">\n");

      out.write("<uses-library android:name=\"org.apache.http.legacy\" android:required=\"false\" />");

      for (Project.SourceDescriptor source : context.getProject().getSources()) {
        String formClassName = source.getQualifiedName();
        context.getReporter().info("Writing screen '" + formClassName + "'");
        // String screenName = formClassName.substring(formClassName.lastIndexOf('.') + 1);
        boolean isMain = formClassName.equals(mainClass);

        if (isMain) {
          // The main activity of the application.
          out.write("    <activity android:name=\"." + className + "\" ");
        } else {
          // A secondary activity of the application.
          out.write("    <activity android:name=\"" + formClassName + "\" ");
        }

        // This line is here for NearField and NFC.   It keeps the activity from
        // restarting every time NDEF_DISCOVERED is signaled.
        // TODO:  Check that this doesn't screw up other components.  Also, it might be
        // better to do this programmatically when the NearField component is created, rather
        // than here in the manifest.
        if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.NearField") && !context.isForCompanion() && isMain) {
          out.write("android:launchMode=\"singleTask\" ");
        } else if (isMain && context.isForCompanion()) {
          out.write("android:launchMode=\"singleTop\" ");
        }

        out.write("android:windowSoftInputMode=\"stateHidden\" ");

        // The keyboard option prevents the app from stopping when a external (bluetooth)
        // keyboard is attached.
        out.write("android:configChanges=\"orientation|screenSize|keyboardHidden|keyboard|"
            + "screenLayout|smallestScreenSize\">\n");

        out.write("      <intent-filter>\n");
        out.write("        <action android:name=\"android.intent.action.MAIN\" />\n");
        if (isMain) {
          out.write("        <category android:name=\"android.intent.category.LAUNCHER\" />\n");
        }
        out.write("      </intent-filter>\n");

        if (context.getSimpleCompTypes().contains("com.google.appinventor.components.runtime.NearField") && !context.isForCompanion() && isMain) {
          //  make the form respond to NDEF_DISCOVERED
          //  this will trigger the form's onResume method
          //  For now, we're handling text/plain only,but we can add more and make the Nearfield
          // component check the type.
          out.write("      <intent-filter>\n");
          out.write("        <action android:name=\"android.nfc.action.NDEF_DISCOVERED\" />\n");
          out.write("        <category android:name=\"android.intent.category.DEFAULT\" />\n");
          out.write("        <data android:mimeType=\"text/plain\" />\n");
          out.write("      </intent-filter>\n");
        }
        out.write("    </activity>\n");

        // Companion display a splash screen... define it's activity here
        if (isMain && context.isForCompanion()) {
          out.write("    <activity android:name=\"com.google.appinventor.components.runtime.SplashActivity\" android:screenOrientation=\"behind\" android:configChanges=\"keyboardHidden|orientation\">\n");
          out.write("      <intent-filter>\n");
          out.write("        <action android:name=\"android.intent.action.MAIN\" />\n");
          out.write("      </intent-filter>\n");
          out.write("    </activity>\n");
        }
      }

      // Collect any additional <application> subelements into a single set.
      Set<Map.Entry<String, Set<String>>> subelements = Sets.newHashSet();
      subelements.addAll(context.getComponentInfo().getActivitiesNeeded().entrySet());
      subelements.addAll(context.getComponentInfo().getBroadcastReceiversNeeded().entrySet());


      // If any component needs to register additional activities or
      // broadcast receivers, insert them into the manifest here.
      if (!subelements.isEmpty()) {
        for (Map.Entry<String, Set<String>> componentSubElSetPair : subelements) {
          Set<String> subelementSet = componentSubElSetPair.getValue();
          for (String subelement : subelementSet) {
            if (context.isForCompanion() && !context.isIncludeDangerousPermissions() && subelement.contains("android.provider.Telephony.SMS_RECEIVED")) {
              continue;
            }
            out.write(subelement);
          }
        }
      }

      // TODO(Will): Remove the following legacy code once the deprecated
      //             @SimpleBroadcastReceiver annotation is removed. It should
      //             should remain for the time being because otherwise we'll break
      //             extensions currently using @SimpleBroadcastReceiver.

      // Collect any legacy simple broadcast receivers
      Set<String> simpleBroadcastReceivers = Sets.newHashSet();
      for (String componentType : context.getComponentInfo().getComponentBroadcastReceiver().keySet()) {
        simpleBroadcastReceivers.addAll(context.getComponentInfo().getComponentBroadcastReceiver().get(componentType));
      }

      // The format for each legacy Broadcast Receiver in simpleBroadcastReceivers is
      // "className,Action1,Action2,..." where the class name is mandatory, and
      // actions are optional (and as many as needed).
      for (String broadcastReceiver : simpleBroadcastReceivers) {
        String[] brNameAndActions = broadcastReceiver.split(",");
        if (brNameAndActions.length == 0) continue;
        // Remove the SMS_RECEIVED broadcast receiver if we aren't including dangerous permissions
        if (context.isForCompanion() && !context.isIncludeDangerousPermissions()) {
          boolean skip = false;
          for (String action : brNameAndActions) {
            if (action.equalsIgnoreCase("android.provider.Telephony.SMS_RECEIVED")) {
              skip = true;
              break;
            }
          }
          if (skip) continue;
        }
        out.write(
            "<receiver android:name=\"" + brNameAndActions[0] + "\" >\n");
        if (brNameAndActions.length > 1) {
          out.write("  <intent-filter>\n");
          for (int i = 1; i < brNameAndActions.length; i++) {
            out.write("    <action android:name=\"" + brNameAndActions[i] + "\" />\n");
          }
          out.write("  </intent-filter>\n");
        }
        out.write("</receiver> \n");
      }

      // Add the FileProvider because in Sdk >=24 we cannot pass file:
      // URLs in intents (and in other contexts)

      out.write("      <provider\n");
      out.write("         android:name=\"android.support.v4.content.FileProvider\"\n");
      out.write("         android:authorities=\"" + packageName + ".provider\"\n");
      out.write("         android:exported=\"false\"\n");
      out.write("         android:grantUriPermissions=\"true\">\n");
      out.write("         <meta-data\n");
      out.write("            android:name=\"android.support.FILE_PROVIDER_PATHS\"\n");
      out.write("            android:resource=\"@xml/provider_paths\"/>\n");
      out.write("      </provider>\n");

      out.write("  </application>\n");
      out.write("</manifest>\n");
      out.close();
    } catch (IOException e) {
      return TaskResult.generateError(e);
    }

    return TaskResult.generateSuccess();
  }

  private String cleanName(String name) {
    return name.replace("&", "and");
  }
}
