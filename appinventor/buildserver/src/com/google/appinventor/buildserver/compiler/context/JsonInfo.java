package com.google.appinventor.buildserver.compiler.context;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JsonInfo {
  private ConcurrentMap<String, Set<String>> assetsNeeded = new ConcurrentHashMap<String, Set<String>>();
  private ConcurrentMap<String, Set<String>> activitiesNeeded = new ConcurrentHashMap<String, Set<String>>();
  private ConcurrentMap<String, Set<String>> broadcastReceiversNeeded = new ConcurrentHashMap<String, Set<String>>();
  private ConcurrentMap<String, Set<String>> libsNeeded = new ConcurrentHashMap<String, Set<String>>();
  private ConcurrentMap<String, Set<String>> nativeLibsNeeded = new ConcurrentHashMap<String, Set<String>>();
  private ConcurrentMap<String, Set<String>> permissionsNeeded = new ConcurrentHashMap<String, Set<String>>();
  private ConcurrentMap<String, Set<String>> minSdksNeeded = new ConcurrentHashMap<String, Set<String>>();
  private Set<String> uniqueLibsNeeded = Sets.newHashSet();
  private ConcurrentMap<String, Map<String, Map<String, Set<String>>>> conditionals = new ConcurrentHashMap<>();

  public ConcurrentMap<String, Set<String>> getAssetsNeeded() {
    return assetsNeeded;
  }

  public void setAssetsNeeded(ConcurrentMap<String, Set<String>> assetsNeeded) {
    this.assetsNeeded = assetsNeeded;
  }

  public ConcurrentMap<String, Set<String>> getActivitiesNeeded() {
    return activitiesNeeded;
  }

  public void setActivitiesNeeded(ConcurrentMap<String, Set<String>> activitiesNeeded) {
    this.activitiesNeeded = activitiesNeeded;
  }

  public ConcurrentMap<String, Set<String>> getBroadcastReceiversNeeded() {
    return broadcastReceiversNeeded;
  }

  public void setBroadcastReceiversNeeded(ConcurrentMap<String, Set<String>> broadcastReceiversNeeded) {
    this.broadcastReceiversNeeded = broadcastReceiversNeeded;
  }

  public ConcurrentMap<String, Set<String>> getLibsNeeded() {
    return libsNeeded;
  }

  public void setLibsNeeded(ConcurrentMap<String, Set<String>> libsNeeded) {
    this.libsNeeded = libsNeeded;
  }

  public ConcurrentMap<String, Set<String>> getNativeLibsNeeded() {
    return nativeLibsNeeded;
  }

  public void setNativeLibsNeeded(ConcurrentMap<String, Set<String>> nativeLibsNeeded) {
    this.nativeLibsNeeded = nativeLibsNeeded;
  }

  public ConcurrentMap<String, Set<String>> getPermissionsNeeded() {
    return permissionsNeeded;
  }

  public void setPermissionsNeeded(ConcurrentMap<String, Set<String>> permissionsNeeded) {
    this.permissionsNeeded = permissionsNeeded;
  }

  public ConcurrentMap<String, Set<String>> getMinSdksNeeded() {
    return minSdksNeeded;
  }

  public void setMinSdksNeeded(ConcurrentMap<String, Set<String>> minSdksNeeded) {
    this.minSdksNeeded = minSdksNeeded;
  }

  public Set<String> getUniqueLibsNeeded() {
    return uniqueLibsNeeded;
  }

  public void setUniqueLibsNeeded(Set<String> uniqueLibsNeeded) {
    this.uniqueLibsNeeded = uniqueLibsNeeded;
  }

  public ConcurrentMap<String, Map<String, Map<String, Set<String>>>> getConditionals() {
    return conditionals;
  }

  public void setConditionals(ConcurrentMap<String, Map<String, Map<String, Set<String>>>> conditionals) {
    this.conditionals = conditionals;
  }

  @Override
  public String toString() {
    return "JsonInfo{" +
        "assetsNeeded=" + assetsNeeded +
        ", activitiesNeeded=" + activitiesNeeded +
        ", broadcastReceiversNeeded=" + broadcastReceiversNeeded +
        ", libsNeeded=" + libsNeeded +
        ", nativeLibsNeeded=" + nativeLibsNeeded +
        ", permissionsNeeded=" + permissionsNeeded +
        ", minSdksNeeded=" + minSdksNeeded +
        ", uniqueLibsNeeded=" + uniqueLibsNeeded +
        ", conditionals=" + conditionals +
        '}';
  }
}
