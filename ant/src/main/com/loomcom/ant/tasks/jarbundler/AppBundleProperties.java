/*
 * A Mac OS X Jar Bundler Ant Task.
 *
 * Copyright (c) 2003, Seth J. Morabito <sethm@loomcom.com> All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See  the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.loomcom.ant.tasks.jarbundler;

import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

public class AppBundleProperties {

  // Required
  private String mCFBundleName;
  private String mMainClass;

  // Required keys, with defaults
  private String mCFBundleVersion = "1.0";
  // Explicit default: false
  private boolean mCFBundleAllowMixedLocalizations = false;
  // Explicit default: JavaApplicationStub
  private String mCFBundleExecutable = "JavaApplicationStub";
  // Explicit default: English
  private String mCFBundleDevelopmentRegion = "English";
  // Explicit default: APPL
  private String mCFBundlePackageType = "APPL";
  // Explicit default: ????
  private String mCFBundleSignature = "????";
  // Explicit default: 1.3+
  private String mJVMVersion = "1.3+";
  // Explicit default: 6.0
  private String mCFBundleInfoDictionaryVersion = "6.0";

  // Optional keys, with no defaults.
  private String mCFBundleIconFile;
  private String mCFBundleGetInfoString;
  private String mCFBundleIdentifier;
  private String mVMOptions; // Java VM options
  private String mWorkingDirectory; // Java Working Dir
  private String mArguments; // Java command line arguments
  // Class path and extra class path elements
  private List mClassPath = new ArrayList();
  private List mExtraClassPath = new ArrayList();

  // User-defined Java properties
  private Hashtable mJavaProperties;

  // Property Getters and Setters
  public void setCFBundleName(String s) {
    mCFBundleName = s;
  }

  public String getCFBundleName() {
    return mCFBundleName;
  }

  public void setCFBundleVersion(String s) {
    mCFBundleVersion = s;
  }

  public String getCFBundleVersion() {
    return mCFBundleVersion;
  }

  public void setCFBundleInfoDictionaryVersion(String s) {
    mCFBundleInfoDictionaryVersion = s;
  }

  public String getCFBundleInfoDictionaryVersion() {
    return mCFBundleInfoDictionaryVersion;
  }

  public void setCFBundleIdentifier(String s) {
    mCFBundleIdentifier = s;
  }

  public String getCFBundleIdentifier() {
    return mCFBundleIdentifier;
  }

  public void setCFBundleGetInfoString(String s) {
    mCFBundleGetInfoString = s;
  }

  public String getCFBundleGetInfoString() {
    return mCFBundleGetInfoString;
  }

  public void setCFBundleIconFile(String s) {
    mCFBundleIconFile = s;
  }

  public String getCFBundleIconFile() {
    return mCFBundleIconFile;
  }

  public void setCFBundleAllowMixedLocalizations(boolean b) {
    mCFBundleAllowMixedLocalizations = b;
  }

  public boolean getCFBundleAllowMixedLocalizations() {
    return mCFBundleAllowMixedLocalizations;
  }

  public void setCFBundleExecutable(String s) {
    mCFBundleExecutable = s;
  }

  public String getCFBundleExecutable() {
    return mCFBundleExecutable;
  }

  public void setCFBundleDevelopmentRegion(String s) {
    mCFBundleDevelopmentRegion = s;
  }

  public String getCFBundleDevelopmentRegion() {
    return mCFBundleDevelopmentRegion;
  }

  public void setCFBundlePackageType(String s) {
    mCFBundlePackageType = s;
  }

  public String getCFBundlePackageType() {
    return mCFBundlePackageType;
  }

  public void setCFBundleSignature(String s) {
    mCFBundleSignature = s;
  }

  public String getCFBundleSignature() {
    return mCFBundleSignature;
  }

  public void setMainClass(String s) {
    mMainClass = s;
  }

  public String getMainClass() {
    return mMainClass;
  }

  public void setJVMVersion(String s) {
    mJVMVersion = s;
  }

  public String getJVMVersion() {
    return mJVMVersion;
  }

  public Hashtable getJavaProperties() {
    return mJavaProperties;
  }

  /**
   * Add a Java runtime property to the properties hashtable.
   */
  public void addJavaProperty(String prop, String val) {
    if (mJavaProperties == null) {
      mJavaProperties = new Hashtable();
    }
    mJavaProperties.put(prop, val);
  }

  public void setVMOptions(String s) {
    mVMOptions = s;
  }

  public String getVMOptions() {
    return mVMOptions;
  }

  public void setWorkingDirectory(String s) {
    mWorkingDirectory = s;
  }

  public String getWorkingDirectory() {
    return mWorkingDirectory;
  }

  public void setArguments(String s) {
    mArguments = s;
  }

  public String getArguments() {
    return mArguments;
  }

  public void addToClassPath(String s) {
    mClassPath.add("$JAVAROOT/" + s);
  }

  public List getClassPath() {
    return mClassPath;
  }

  public void addToExtraClassPath(String s) {
    mExtraClassPath.add(s);
  }

  public List getExtraClassPath() {
    return mExtraClassPath;
  }
}
