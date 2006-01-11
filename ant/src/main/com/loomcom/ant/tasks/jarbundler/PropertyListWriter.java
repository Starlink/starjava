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

import java.io.*;
import java.util.*;

import org.apache.tools.ant.BuildException;

/**
 * Write out a Java application bundle property list file.
 */
public class PropertyListWriter {

  private PrintWriter mOut;     // Where to write
  private AppBundleProperties mProps;  // Our app bundle properties

  /**
   * Create a new Property List writer.
   */
  public PropertyListWriter(AppBundleProperties p) {
    mProps = p;
  }

  public void writeFile(File fileName) throws BuildException {
    try {
      mOut = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
    } catch (IOException ex) {
      throw new BuildException("Unable to open " + fileName
			       + " for writing.");
    }

    try {
      // Begin Plist
      mOut.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      mOut.println("<!DOCTYPE plist SYSTEM " +
		   "\"file://localhost/System/Library/DTDs/PropertyList.dtd\">");
      mOut.println("<plist version=\"1.0\">");

      // Begin contents
      openDict(0);

      // Required key
      writeKey(1, "CFBundleName");
      writeString(1, mProps.getCFBundleName());

      // Required key
      writeKey(1, "CFBundleVersion");
      writeString(1, mProps.getCFBundleVersion());

      // Required Key
      writeKey(1, "CFBundleAllowMixedLocalizations");
      writeString(1, new Boolean(mProps.getCFBundleAllowMixedLocalizations()).toString());

      // Optional key
      if (mProps.getCFBundleGetInfoString() != null) {
	writeKey(1, "CFBundleGetInfoString");
	writeString(1, mProps.getCFBundleGetInfoString());
      }

      writeKey(1, "CFBundleInfoDictionaryVersion");
      writeString(1, mProps.getCFBundleInfoDictionaryVersion());

      // Optional key
      if (mProps.getCFBundleIdentifier() != null) {
	writeKey(1, "CFBundleIdentifier");
	writeString(1, mProps.getCFBundleIdentifier());
      }

      // Required key
      writeKey(1, "CFBundleExecutable");
      writeString(1, mProps.getCFBundleExecutable());

      // Required key
      writeKey(1, "CFBundleDevelopmentRegion");
      writeString(1, mProps.getCFBundleDevelopmentRegion());

      // Required key
      writeKey(1, "CFBundlePackageType");
      writeString(1, mProps.getCFBundlePackageType());

      // Required key
      writeKey(1, "CFBundleSignature");
      writeString(1, mProps.getCFBundleSignature());

      // Optional key
      if (mProps.getCFBundleIconFile() != null) {
	writeKey(1, "CFBundleIconFile");
	writeString(1, mProps.getCFBundleIconFile());
      }

      // Required key
      writeKey(1, "Java");

      // Open the "Java" dictionary
      openDict(1);

      // Required key.
      writeKey(2, "MainClass");
      writeString(2, mProps.getMainClass());

      // Recommended key
      if (mProps.getJVMVersion() != null) {
	writeKey(2, "JVMVersion");
	writeString(2, mProps.getJVMVersion());
      }

      // Classpath is composed of two types.
      // 1: Jars bundled into the JAVA_ROOT of the application
      // 2: External directories or files with an absolute path
      List classPath = mProps.getClassPath();
      List extraClassPath = mProps.getExtraClassPath();
      if (classPath.size() > 0 || extraClassPath.size() > 0) {
	writeKey(2, "ClassPath");
	openArray(2);
	writeArray(3, classPath);
	writeArray(3, extraClassPath);
	closeArray(2);
      }

      // Optional key
      if (mProps.getVMOptions() != null) {
	writeKey(2, "VMOptions");
	writeString(2, mProps.getVMOptions());
      }

      // Optional key
      if (mProps.getWorkingDirectory() != null) {
	writeKey(2, "WorkingDirectory");
	writeString(2, mProps.getWorkingDirectory());
      }

      // Optional key
      if (mProps.getArguments() != null) {
	writeKey(2, "Arguments");
	writeString(2, mProps.getArguments());
      }

      // Write out user Java properties (optional)
      Hashtable javaProperties = mProps.getJavaProperties();
      if (javaProperties != null) {
	writeKey(2, "Properties");
	openDict(2);
	for (Iterator i = javaProperties.keySet().iterator(); i.hasNext(); ) {
	  String key = (String)i.next();
	  writeKey(3, key);
	  writeString(3, (String)javaProperties.get(key));
	}
	closeDict(2);
      }

      // Close the "Java" dictionary
      closeDict(1);

      // End contents
      closeDict(0);

      // End Plist
      mOut.println("</plist>");
      mOut.flush();
    } finally {
      if (mOut != null) { mOut.close(); }
    }
  }

  private void openDict(int lvl) {
    for (int i = 0; i < lvl; i++) {
      mOut.print("    ");
    }
    mOut.println("<dict>");
  }

  private void closeDict(int lvl) {
    for (int i = 0; i < lvl; i++) {
      mOut.print("    ");
    }
    mOut.println("</dict>");
  }

  private void openArray(int lvl) {
    for (int i = 0; i < lvl; i++) {
      mOut.print("    ");
    }
    mOut.println("<array>");
  }

  private void closeArray(int lvl) {
    for (int i = 0; i < lvl; i++) {
      mOut.print("    ");
    }
    mOut.println("</array>");
  }

  private void writeArray(int lvl, List stringList) {
    for (Iterator it = stringList.iterator(); it.hasNext(); ) {
      try {
	writeString(lvl, (String)it.next());
      } catch (ClassCastException ex) {
	// Poorly handled exception, but at least we
	// won't exit.
	continue;
      }
    }
  }

  private void writeKey(int lvl, String s) {
    for (int i = 0; i < lvl; i++) {
      mOut.print("    ");
    }
    mOut.println("<key>" + s + "</key>");
  }

  private void writeString(int lvl, String s) {
    for (int i = 0; i < lvl; i++) {
      mOut.print("    ");
    }
    mOut.println("<string>" + s + "</string>");
  }
}
