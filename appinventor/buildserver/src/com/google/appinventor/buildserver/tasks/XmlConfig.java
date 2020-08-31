package com.google.appinventor.buildserver.tasks;

import com.google.appinventor.buildserver.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * compiler.createValuesXml()
 * compiler.createProviderXml()
 * compiler.createNetworkConfigXml()
 * compiler.writeICLauncher()
 * compiler.writeICLauncher()
 * compiler.writeICLauncherBackground()
 *
 */
// createValuesXml
// createResXml
// GenerateXmlRes
@BuildType(apk = true, aab = true)
public class XmlConfig implements Task {
  CompilerContext context;

  @Override
  public TaskResult execute(CompilerContext context) {
    this.context = context;

    // Create the "any" dpi dir
    context.getReporter().info("Creating animation xml");
    File mipmapV26 = ExecutorUtils.createDir(context.getPaths().getResDir(), "mipmap-anydpi-v26");

    // Create anim directory and animation xml files
    File animDir = ExecutorUtils.createDir(context.getPaths().getResDir(), "anim");
    if (!this.createAnimationXml(animDir)) {
      return TaskResult.generateError("There was an error creating the animation xml");
    }

    // Create values directory and style xml files
    context.getReporter().info("Creating style xml");
    File styleDir = ExecutorUtils.createDir(context.getPaths().getResDir(), "values");
    List<String> standardStyleVersions = Arrays.asList("", "-v11", "-v14", "-v21", "-v23");
    for (String standardStyleVersion : standardStyleVersions) {
      File tmpStyleDir = ExecutorUtils.createDir(context.getPaths().getResDir(), "values" + standardStyleVersion);
      if (!this.createValuesXml(tmpStyleDir, standardStyleVersion)) {
        return TaskResult.generateError("There was an error while generating the values" + standardStyleVersion + " style");
      }
    }

    context.getReporter().info("Creating provider_path xml");
    File providerDir = ExecutorUtils.createDir(context.getPaths().getResDir(), "xml");
    if (!this.createProviderXml(providerDir)) {
      return TaskResult.generateError("There was an error creating the provider_path xml");
    }

    context.getReporter().info("Creating network_security_config xml");
    if (!this.createNetworkConfigXml(providerDir)) {
      return TaskResult.generateError("There was an error creating the network_security_config xml");
    }

    // Generate ic_launcher.xml
    context.getReporter().info("Generating adaptive icon file");
    File icLauncher = new File(mipmapV26, "ic_launcher.xml");
    if (!this.writeICLauncher(icLauncher)) {
      return TaskResult.generateError("There was an error creating the adaptive icon file");
    }

    // Generate ic_launcher_round.xml
    context.getReporter().info("Generating round adaptive icon file");
    File icLauncherRound = new File(mipmapV26, "ic_launcher_round.xml");
    if (!this.writeICLauncher(icLauncherRound)) {
      return TaskResult.generateError("There was an error creating the round adaptive icon file");
    }

    // Generate ic_launcher_background.xml
    context.getReporter().info("Generating adaptive icon background file");
    File icBackgroundColor = new File(styleDir, "ic_launcher_background.xml");
    if (!this.writeICLauncherBackground(icBackgroundColor)) {
      return TaskResult.generateError("There was an error creating the adaptive icon background file");
    }

    return TaskResult.generateSuccess();
  }


  /*
   * Writes the given string input to the provided file.
   */
  private boolean writeXmlFile(File file, String input) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      writer.write(input);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
      context.getReporter().error("Error writing to XML file " + file.getName());
      return false;
    }
    return true;
  }


  /*
   * Creates all the animation xml files.
   */
  private boolean createAnimationXml(File animDir) {
    // Store the filenames, and their contents into a HashMap
    // so that we can easily add more, and also to iterate
    // through creating the files.
    Map<String, String> files = new HashMap<>();
    files.put("fadein.xml", AnimationXmlConstants.FADE_IN_XML);
    files.put("fadeout.xml", AnimationXmlConstants.FADE_OUT_XML);
    files.put("hold.xml", AnimationXmlConstants.HOLD_XML);
    files.put("zoom_enter.xml", AnimationXmlConstants.ZOOM_ENTER);
    files.put("zoom_exit.xml", AnimationXmlConstants.ZOOM_EXIT);
    files.put("zoom_enter_reverse.xml", AnimationXmlConstants.ZOOM_ENTER_REVERSE);
    files.put("zoom_exit_reverse.xml", AnimationXmlConstants.ZOOM_EXIT_REVERSE);
    files.put("slide_exit.xml", AnimationXmlConstants.SLIDE_EXIT);
    files.put("slide_enter.xml", AnimationXmlConstants.SLIDE_ENTER);
    files.put("slide_exit_reverse.xml", AnimationXmlConstants.SLIDE_EXIT_REVERSE);
    files.put("slide_enter_reverse.xml", AnimationXmlConstants.SLIDE_ENTER_REVERSE);
    files.put("slide_v_exit.xml", AnimationXmlConstants.SLIDE_V_EXIT);
    files.put("slide_v_enter.xml", AnimationXmlConstants.SLIDE_V_ENTER);
    files.put("slide_v_exit_reverse.xml", AnimationXmlConstants.SLIDE_V_EXIT_REVERSE);
    files.put("slide_v_enter_reverse.xml", AnimationXmlConstants.SLIDE_V_ENTER_REVERSE);

    for (String filename : files.keySet()) {
      context.getReporter().log("Creating " + filename);
      File file = new File(animDir, filename);
      if (!writeXmlFile(file, files.get(filename))) {
        context.getReporter().error("Error writing animations XML file");
        return false;
      }
    }
    return true;
  }


  /**
   * Create the default color and styling for the app.
   */
  private boolean createValuesXml(File valuesDir, String suffix) {
    String colorPrimary = context.getProject().getPrimaryColor() == null ? "#A5CF47" : context.getProject().getPrimaryColor();
    String colorPrimaryDark = context.getProject().getPrimaryColorDark() == null ? "#41521C" : context.getProject().getPrimaryColorDark();
    String colorAccent = context.getProject().getAccentColor() == null ? "#00728A" : context.getProject().getAccentColor();
    String theme = context.getProject().getTheme() == null ? "Classic" : context.getProject().getTheme();
    String actionbar = context.getProject().getActionBar();
    String parentTheme;
    boolean isClassicTheme = "Classic".equals(theme) || suffix.isEmpty();  // Default to classic theme prior to SDK 11
    boolean needsBlackTitleText = false;
    boolean holo = "-v11".equals(suffix) || "-v14".equals(suffix);
    int sdk = suffix.isEmpty() ? 7 : Integer.parseInt(suffix.substring(2));
    if (isClassicTheme) {
      parentTheme = "android:Theme";
    } else {
      if (suffix.equals("-v11")) {  // AppCompat needs SDK 14, so we explicitly name Holo for SDK 11 through 13
        parentTheme = theme.replace("AppTheme", "android:Theme.Holo");
        needsBlackTitleText = theme.contains("Light") && !theme.contains("DarkActionBar");
        if (theme.contains("Light")) {
          parentTheme = "android:Theme.Holo.Light";
        }
      } else {
        parentTheme = theme.replace("AppTheme", "Theme.AppCompat");
      }
      if (!"true".equalsIgnoreCase(actionbar)) {
        if (parentTheme.endsWith("DarkActionBar")) {
          parentTheme = parentTheme.replace("DarkActionBar", "NoActionBar");
        } else {
          parentTheme += ".NoActionBar";
        }
      }
    }
    colorPrimary = cleanColor(colorPrimary);
    colorPrimaryDark = cleanColor(colorPrimaryDark);
    colorAccent = cleanColor(colorAccent);
    File colorsXml = new File(valuesDir, "colors" + suffix + ".xml");
    File stylesXml = new File(valuesDir, "styles" + suffix + ".xml");
    try {
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(colorsXml), StandardCharsets.UTF_8));
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      out.write("<resources>\n");
      out.write("<color name=\"colorPrimary\">");
      out.write(colorPrimary);
      out.write("</color>\n");
      out.write("<color name=\"colorPrimaryDark\">");
      out.write(colorPrimaryDark);
      out.write("</color>\n");
      out.write("<color name=\"colorAccent\">");
      out.write(colorAccent);
      out.write("</color>\n");
      out.write("</resources>\n");
      out.close();
      out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stylesXml), StandardCharsets.UTF_8));
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      out.write("<resources>\n");

      // writeTheme >>>
      out.write("<style name=\"");
      out.write("AppTheme");
      out.write("\" parent=\"");
      out.write(parentTheme);
      out.write("\">\n");
      out.write("<item name=\"colorPrimary\">@color/colorPrimary</item>\n");
      out.write("<item name=\"colorPrimaryDark\">@color/colorPrimaryDark</item>\n");
      out.write("<item name=\"colorAccent\">@color/colorAccent</item>\n");
      boolean needsClassicSwitch = false;
      if (!parentTheme.equals("android:Theme")) {
        out.write("<item name=\"windowActionBar\">true</item>\n");
        out.write("<item name=\"android:windowActionBar\">true</item>\n");  // Honeycomb ActionBar
        if (parentTheme.contains("Holo") || holo) {
          out.write("<item name=\"android:actionBarStyle\">@style/AIActionBar</item>\n");
          out.write("<item name=\"actionBarStyle\">@style/AIActionBar</item>\n");
        }
        // Handles theme for Notifier
        out.write("<item name=\"android:dialogTheme\">@style/AIDialog</item>\n");
        out.write("<item name=\"dialogTheme\">@style/AIDialog</item>\n");
        out.write("<item name=\"android:cacheColorHint\">#000</item>\n");  // Fixes crash in ListPickerActivity
      } else {
        out.write("<item name=\"switchStyle\">@style/ClassicSwitch</item>\n");
        needsClassicSwitch = true;
      }
      out.write("</style>\n");
      if (needsClassicSwitch) {
        out.write("<style name=\"ClassicSwitch\" parent=\"Widget.AppCompat.CompoundButton.Switch\">\n");
        if (sdk == 23) {
          out.write("<item name=\"android:background\">@drawable/abc_control_background_material</item>\n");
        } else {
          out.write("<item name=\"android:background\">@drawable/abc_item_background_holo_light</item>\n");
        }
        out.write("</style>\n");
      }
      // <<< writeTheme

      if (!isClassicTheme) {
        if (holo) {  // Handle Holo
          // writeActionBarStyle >>>
          out.write("<style name=\"");
          out.write("AIActionBar");
          out.write("\" parent=\"");
          if (parentTheme.contains("Light")) {
            out.write("android:Widget.Holo.Light.ActionBar");
          } else {
            out.write("android:Widget.Holo.ActionBar");
          }
          out.write("\">\n");
          out.write("<item name=\"android:background\">@color/colorPrimary</item>\n");
          out.write("<item name=\"android:titleTextStyle\">@style/AIActionBarTitle</item>\n");
          out.write("</style>\n");
          out.write("<style name=\"AIActionBarTitle\" parent=\"android:TextAppearance.Holo.Widget.ActionBar.Title\">\n");
          out.write("<item name=\"android:textColor\">" + (needsBlackTitleText ? "#000" : "#fff") + "</item>\n");
          out.write("</style>\n");
          // <<< writeActionBarStyle
        }
        if (parentTheme.contains("Light")) {
          writeDialogTheme(out, "AIDialog", "Theme.AppCompat.Light.Dialog");
          writeDialogTheme(out, "AIAlertDialog", "Theme.AppCompat.Light.Dialog.Alert");
        } else {
          writeDialogTheme(out, "AIDialog", "Theme.AppCompat.Dialog");
          writeDialogTheme(out, "AIAlertDialog", "Theme.AppCompat.Dialog.Alert");
        }
      }
      out.write("<style name=\"TextAppearance.AppCompat.Button\">\n");
      out.write("<item name=\"textAllCaps\">false</item>\n");
      out.write("</style>\n");
      out.write("</resources>\n");
      out.close();
    } catch (IOException e) {
      context.getReporter().error("Error writing values XML file");
      return false;
    }
    return true;
  }

  private String cleanColor(String color) {
    String result = color;
    if (color.startsWith("&H") || color.startsWith("&h")) {
      result = "#" + color.substring(2);
    }
    if (result.length() == 9) {  // true for #AARRGGBB strings
      result = "#" + result.substring(3);  // remove any alpha value
    }
    return result;
  }

  private void writeDialogTheme(Writer out, String name, String parent) throws IOException {
    out.write("<style name=\"");
    out.write(name);
    out.write("\" parent=\"");
    out.write(parent);
    out.write("\">\n");
    out.write("<item name=\"colorPrimary\">@color/colorPrimary</item>\n");
    out.write("<item name=\"colorPrimaryDark\">@color/colorPrimaryDark</item>\n");
    out.write("<item name=\"colorAccent\">@color/colorAccent</item>\n");
    if (parent.contains("Holo")) {
      // workaround for weird window border effect
      out.write("<item name=\"android:windowBackground\">@android:color/transparent</item>\n");
      out.write("<item name=\"android:gravity\">center</item>\n");
      out.write("<item name=\"android:layout_gravity\">center</item>\n");
      out.write("<item name=\"android:textColor\">@color/colorPrimary</item>\n");
    }
    out.write("</style>\n");
  }


  /*
   * Creates the provider_paths file which is used to setup a "Files" content
   * provider.
   */
  private boolean createProviderXml(File providerDir) {
    File paths = new File(providerDir, "provider_paths.xml");
    try {
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(paths), StandardCharsets.UTF_8));
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      out.write("<paths xmlns:android=\"http://schemas.android.com/apk/res/android\">\n");
      out.write("   <external-path name=\"external_files\" path=\".\"/>\n");
      out.write("</paths>\n");
      out.close();
    } catch (IOException e) {
      context.getReporter().error("Error writing provider_paths XML file");
      return false;
    }
    return true;
  }


  private boolean createNetworkConfigXml(File configDir) {
    File networkConfig = new File(configDir, "network_security_config.xml");
    try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(networkConfig)))) {
      out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
      out.println("<network-security-config>");
      out.println("<base-config cleartextTrafficPermitted=\"true\">");
      out.println("<trust-anchors>");
      out.println("<certificates src=\"system\"/>");
      out.println("</trust-anchors>");
      out.println("</base-config>");
      out.println("</network-security-config>");
    } catch (IOException e) {
      context.getReporter().error("Error writing network_config XML file");
      return false;
    }
    return true;
  }


  // Writes ic_launcher.xml to initialize adaptive icon
  private boolean writeICLauncher(File adaptiveIconFile) {
    try {
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(adaptiveIconFile), StandardCharsets.UTF_8));
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      out.write("<adaptive-icon " + "xmlns:android=\"http://schemas.android.com/apk/res/android\" " + ">\n");
      out.write("<background android:drawable=\"@color/ic_launcher_background\" />\n");
      out.write("<foreground android:drawable=\"@mipmap/ic_launcher_foreground\" />\n");
      out.write("</adaptive-icon>\n");
      out.close();
    } catch (IOException e) {
      context.getReporter().error("Error writing IC launcher file");
      return false;
    }
    return true;
  }


  // Writes ic_launcher_background.xml to indicate background color of adaptive icon
  private boolean writeICLauncherBackground(File icBackgroundFile) {
    try {
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(icBackgroundFile), StandardCharsets.UTF_8));
      out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
      out.write("<resources>\n");
      out.write("<color name=\"ic_launcher_background\">#ffffff</color>\n");
      out.write("</resources>\n");
      out.close();
    } catch (IOException e) {
      context.getReporter().error("Error writing IC launcher background file");
      return false;
    }
    return true;
  }
}
