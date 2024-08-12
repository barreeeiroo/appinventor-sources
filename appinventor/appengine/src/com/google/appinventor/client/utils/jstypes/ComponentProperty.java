package com.google.appinventor.client.utils.jstypes;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(namespace = JsPackage.GLOBAL)
public class ComponentProperty {
  private final JSONObject obj;

  public ComponentProperty(String name, JSONValue value) {
    this.obj = new JSONObject();

    obj.put("name", new JSONString(name));
    obj.put("value", value);
  }

  public String toJsonString() {
    return obj.toString();
  }
}
