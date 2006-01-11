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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.util.FileUtils;

/**
 * <p>An ant task which creates a Mac OS X Application Bundle for a
 * Java application.</p>
 *
 * <pre>
 * Required attributes:
 *
 *  dir         The directory into which to put the new application bundle.
 *  name        The name of the application bundle.
 *  mainclass   The main Java class to call when running the application.
 *
 * One of the following three MUST be used:
 *  jars        Space or comma-separated list of JAR files to include.
 * OR
 *  One or more nested  &lt;jarfileset&gt;s. These are normal ANT FileSets.
 * OR
 *  One or more nested &lt;jarfilelist&gt;s. These are standard ANT FileLists.
 *
 * Optional attributes:
 *
 * The following attributes are not required, but you can use them to
 * override default behavior.
 *
 *   verbose       If true, show more verbose output while running the task
 *   version       Version information about your application (e.g., "1.0")
 *   infostring    String to show in the "Get Info" dialog
 *   jvmversion    (e.g. "1.3", "1.3+", "1.4+", "1.4.1")
 *
 * These attributes control the fine-tuning of the "Mac OS X" look and
 * feel.
 *
 *   arguments               Command line arguments. (no default)
 *   smalltabs               Use small tabs. (default "false")
 *                             Deprecated under JVM 1.4.1
 *   antialiasedgraphics     Use anti-aliased graphics (default "false")
 *   antialiasedtext         Use anti-aliased text (default "false")
 *   bundleid                Unique identifier for this bundle, in the
 *                           form of a Java package.  No default.
 *   developmentregion       Development Region.  Default "English".
 *   execs                   Files to be copied into "Resources/MacOS" and
 *                           made executable
 *   liveresize              Use "Live resizing" (default "false")
 *                             Deprecated under JVM 1.4.1
 *   growbox                 Show growbox (default "true")
 *   growboxintrudes         Intruding growbox (default "false")
 *                             Deprecated under JVM 1.4.1
 *   screenmenu              Put swing menu into Mac OS X menu bar.
 *   type                    Bundle type (default "APPL")
 *   signature               Bundle Signature (default "????")
 *   stubfile                The Java Application Stub file to copy for
 *                           your application (default MacOS system stub file)
 *
 * Rarely used optional attributes.
 *
 *   chmod                   Full path to the chmod command.  This almost
 *                           certainly does NOT need to be set.
 *
 * The task also supports nested &lt;execfileset&gt; and/or &lt;execfilelist&gt;
 * elements, which are standard Ant FileSet and FileList elements.
 *
 * eg:
 * Minimum example:
 *
 *  &lt;jarbundler dir="release" name="Bar Project" mainclass="org.bar.Main"
 *      jars="bin/Bar.jar" /&gt;
 *
 * Using Filesets
 *  &lt;jarbundler dir="release" name="Bar Project" mainclass="org.bar.Main"&gt;
 *    &lt;jarfileset dir="bin"&gt;
 *      &lt;include name="*.jar" /&gt;
 *      &lt;exclude name="test.jar" /&gt;
 *    &lt;/jarfileset&gt;
 *    &lt;execfileset dir="execs"&gt;
 *      &lt;include name="**" /&gt;
 *    &lt;/execfileset&gt;
 *  &lt;/jarbundler&gt;
 *
 * Much Longer example:
 *
 *  &lt;jarbundler dir="release" name="Foo Project" mainclass="org.bar.Main"
 *      version="1.0 b 1" infostring="Foo Project (c) 2002" type="APPL"
 *      jars="bin/foo.jar bin/bar.jar" execs="exec/foobar"
 *      signature="????" aboutmenuname="Foo Project"
 *      workingdirectory="temp" icon="resources/foo.icns"
 *      jvmversion="1.4.1+" vmoptions="-Xmx256m" smalltabs="false"
 *      antialiasedgraphics="false" antialiasedtext="false"
 *      liveresize="false" growbox="false" screenmenu="true"/&gt;
 * </pre>
 */

public class JarBundler extends MatchingTask {
  private static final String DEFAULT_STUB =
    "/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub";
  private static final String DEFAULT_CHMOD =
    "/bin/chmod";

  private File mAppIcon;
  private File mRootDir;

  private List mJarFileSets = new ArrayList();
  private List mExecFileSets = new ArrayList();
  private List mExtraClassPathFileSets = new ArrayList();
  private List mJarFileLists = new ArrayList();
  private List mExecFileLists = new ArrayList();
  private List mExtraClassPathFileLists = new ArrayList();
  private List mJarAttrs = new ArrayList();
  private List mExecAttrs = new ArrayList();
  private List mExtraClassPathAttrs = new ArrayList();

  private boolean mVerbose = false;

  // Java properties used by Mac OS X Java applications
  private File mStubFile = new File(DEFAULT_STUB);
  private String mAboutMenuName;
  private boolean mSmallTabs = false;
  private boolean mAntiAliasedGraphics = false;
  private boolean mAntiAliasedText = false;
  private boolean mLiveResize = false;
  private boolean mScreenMenuBar = false;
  private boolean mGrowbox = true;
  private boolean mGrowboxIntrudes = false;

  // "Contents" directory in the application bundle.
  private File mContentsDir;

  // "Contents/MacOS" directory in the application bundle.
  private File mMacOsDir;

  // "Contents/Resources/Java" directory in the application bundle.
  private File mJavaDir;

  // Full path to the 'chmod' command.  Can be overridden
  // with the 'chmod' attribute.  Won't cause any harm if
  // not set, or if this executable doesn't exist.
  private String mChmodCommand = DEFAULT_CHMOD;

  private AppBundleProperties mProps = new AppBundleProperties();

  // Ant file utilities
  private FileUtils mFileUtils = FileUtils.newFileUtils();

  /**
   * The method executing the task
   */
  public void execute() throws BuildException {

    // Validate
    if (mRootDir == null) {
      throw new BuildException("Required attribute \"dir\" is not set.");
    }

    if (mJarAttrs.isEmpty() && mJarFileSets.isEmpty() && mJarFileLists.isEmpty()) {
      throw new BuildException("Either the attribute \"jars\" must " +
			       "be set, or one or more jarfilelists or " +
			       "jarfilesets must be added.");
    }

    if (!mJarAttrs.isEmpty() && (!mJarFileSets.isEmpty() || !mJarFileLists.isEmpty())) {
      throw new BuildException("Cannot set both the attribute " +
			       "\"jars\" and use jar " +
			       "filesets/filelists.  Use " +
			       "only one or the other.");
    }

    if (mProps.getCFBundleName() == null) {
      throw new BuildException("Required attribute \"name\" is not set.");
    }

    if (mProps.getMainClass() == null) {
      throw new BuildException("Required attribute \"mainclass\" is " +
			       "not set.");
    }

    // Set up some Java properties
    if (mAboutMenuName == null) {
      mAboutMenuName = mProps.getCFBundleName();
    }

    // About Menu
    mProps.addJavaProperty("com.apple.mrj.application.apple.menu.about.name",
			   mAboutMenuName);

    // Small Tabs
    mProps.addJavaProperty("com.apple.smallTabs",
			   new Boolean(mSmallTabs).toString());

    // Anti Aliased Graphics
    String antiAliasedProperty = useOldPropertyNames() ?
      "com.apple.macosx.AntiAliasedGraphicsOn" :
      "apple.awt.antialiasing";

    mProps.addJavaProperty(antiAliasedProperty,
			   new Boolean(mAntiAliasedGraphics).toString().toString());

    // Anti Aliased Text
    String antiAliasedTextProperty = useOldPropertyNames() ?
      "com.apple.macosx.AntiAliasedTextOn" :
      "apple.awt.textantialiasing";

    mProps.addJavaProperty(antiAliasedTextProperty,
			   new Boolean(mAntiAliasedText).toString());

    // Live Resize
    mProps.addJavaProperty("com.apple.mrj.application.live-resize",
			   new Boolean(mLiveResize).toString());

    // Screen Menu Bar
    String screenMenuBarProperty = useOldPropertyNames() ?
      "com.apple.macos.useScreenMenuBar" :
      "apple.laf.useScreenMenuBar";

    mProps.addJavaProperty(screenMenuBarProperty,
			   new Boolean(mScreenMenuBar).toString());

    // Growbox (only used if the Java VM is 1.4 or higher)
    if (!useOldPropertyNames()) {
      mProps.addJavaProperty("apple.awt.showGrowBox",
			     new Boolean(mGrowbox).toString());
    }

    // Growbox Intrudes (only used if the Java VM is 1.3 or lower)
    if (useOldPropertyNames()) {
      mProps.addJavaProperty("com.apple.mrj.application.growbox.intrudes",
			     new Boolean(mGrowboxIntrudes).toString());
    }

    if (!mRootDir.exists() || (mRootDir.exists() && !mRootDir.isDirectory())) {
      throw new BuildException("Destination directory specified by \"dir\" " +
			       "attribute must already exist.");
    }

    File bundleDir = new File(mRootDir, mProps.getCFBundleName() + ".app");

    if (bundleDir.exists()) {
      throw new BuildException("The App Bundle " + bundleDir.getName() +
			       " already exists, cannot continue.");
    }

    // Status
    System.out.println("Creating application bundle " + bundleDir);

    if (!bundleDir.mkdir()) {
      throw new BuildException("Unable to create bundle: "
			       + bundleDir);
    }

    // Make the Contents directory
    mContentsDir = new File(bundleDir, "Contents");
    if (!mContentsDir.mkdir()) {
      throw new BuildException("Unable to create directory "
			       + mContentsDir);
    }

    // Make the "MacOS" directory
    mMacOsDir = new File(mContentsDir, "MacOS");
    if (!mMacOsDir.mkdir()) {
      throw new BuildException("Unable to create directory "
			       + mMacOsDir);
    }

    // Make the Resources directory
    File resourceDir = new File(mContentsDir, "Resources");
    if (!resourceDir.mkdir()) {
      throw new BuildException("Unable to create directory "
			       + resourceDir);
    }

    // Make the Resources/Java directory
    mJavaDir = new File(resourceDir, "Java");
    if (!mJavaDir.mkdir()) {
      throw new BuildException("Unable to create directory "
			       + mJavaDir);
    }

    // Copy icon file to resource dir.  If no icon parameter
    // is supplied, the default icon will be used.
    if (mAppIcon != null) {
      try {
	mFileUtils.copyFile(mAppIcon, new File(resourceDir,
					       mAppIcon.getName()));
      } catch (IOException ex) {
	throw new BuildException("Cannot copy icon file: " + ex);
      }
    }

    // Copy application jar(s) from the "jars" attribute (if any)
    processJarAttrs();

    // Copy application jar(s) from the nested jarfileset element(s)
    processJarFileSets();

    // Copy application jar(s) from the nested jarfilelist element(s)
    processJarFileLists();

    // Copy executable(s) from the "execs" attribute (if any)
    processExecAttrs();

    // Copy executable(s) from the nested execfileset element(s)
    processExecFileSets();

    // Copy executable(s) from the nested execfilelist element(s)
    processExecFileLists();

    // Add external classpath references from the extraclasspath
    // attributes
    processExtraClassPathAttrs();

    // Add external classpath references from the nested
    // extraclasspathfileset element(s)
    processExtraClassPathFileSets();

    // Add external classpath references from the nested
    // extraclasspathfilelist attributes
    processExtraClassPathFileLists();

    // Copy the JavaApplicationStub file from the Java
    // system directory to the MacOS directory
    copyApplicationStub();

    // Create the Info.plist file
    writeInfoPlist();

    // Create the PkgInfo file
    writePkgInfo();

    // Done!
  }

  public void addJarfileset(FileSet fs) {
    mJarFileSets.add(fs);
  }

  public void addJarfilelist(FileList fl) {
    mJarFileLists.add(fl);
  }

  public void addExecfileset(FileSet fs) {
    mExecFileSets.add(fs);
  }

  public void addExecfilelist(FileList fl) {
    mExecFileLists.add(fl);
  }

  public void addExtraclasspathfileset(FileSet fs) {
    mExtraClassPathFileSets.add(fs);
  }

  public void addExtraclasspathfilelist(FileList fl) {
    mExtraClassPathFileLists.add(fl);
  }

  /**
   * Arguments to the 
   * @param s The arguments to pass to the application being launched.
   */
  public void setArguments(String s) {
    mProps.setArguments(s);
  }

  /**
   * Override the stub file path to build on non-MacOS platforms
   * @param s the path to the stub file
   */
  public void setStubFile(File f) {
    mStubFile = f;
  }

  /**
   * Setter for the "dir" attribute
   */
  public void setDir(File f) {
    mRootDir = f;
  }

  /**
   * Setter for the "name" attribute (required)
   */
  public void setName(String s) {
    mProps.setCFBundleName(s);
  }

  /**
   * Setter for the "mainclass" attribute (required)
   */
  public void setMainClass(String s) {
    mProps.setMainClass(s);
  }

  /**
   * Setter for the "WorkingDirectory" attribute (optional)
   */
  public void setWorkingDirectory(String s) {
    mProps.setWorkingDirectory(s);
  }

  /**
   * Setter for the "icon" attribute (optional)
   */
  public void setIcon(File f) {
    mAppIcon = f;
    mProps.setCFBundleIconFile(f.getName());
  }

  /**
   * Setter for the "bundleid" attribute (optional)
   * No default.
   */
  public void setBundleid(String s) {
    mProps.setCFBundleIdentifier(s);
  }

  /**
   * Setter for the "developmentregion" attribute(optional)
   * Default "English".
   */
  public void setDevelopmentregion(String s) {
    mProps.setCFBundleDevelopmentRegion(s);
  }

  /**
   * Setter for the "aboutmenuname" attribute (optional)
   */
  public void setAboutmenuname(String s) {
    mAboutMenuName = s;
  }

  /**
   * Setter for the "smalltabs" attribute (optional)
   */
  public void setSmallTabs(boolean b) {
    mSmallTabs = b;
  }

  /**
   * Setter for the "vmoptions" attribute (optional)
   */
  public void setVmoptions(String s) {
    mProps.setVMOptions(s);
  }

  /**
   * Setter for the "antialiasedgraphics" attribute (optional)
   */
  public void setAntialiasedgraphics(boolean b) {
    mAntiAliasedGraphics = b;
  }

  /**
   * Setter for the "antialiasedtext" attribute (optional)
   */
  public void setAntialiasedtext(boolean b) {
    mAntiAliasedText = b;
  }

  /**
   * Setter for the "screenmenu" attribute (optional)
   */
  public void setScreenmenu(boolean b) {
    mScreenMenuBar = b;
  }

  /**
   * Setter for the "growbox" attribute (optional)
   */
  public void setGrowbox(boolean b) {
    mGrowbox = b;
  }

  /**
   * Setter for the "growboxintrudes" attribute (optional)
   */
  public void setGrowboxintrudes(boolean b) {
    mGrowboxIntrudes = b;
  }

  /**
   * Setter for the "liveresize" attribute (optional)
   */
  public void setLiveresize(boolean b) {
    mLiveResize = b;
  }

  /**
   * Setter for the "type" attribute (optional)
   */
  public void setType(String s) {
    mProps.setCFBundlePackageType(s);
  }

  /**
   * Setter for the "signature" attribute (optional)
   */
  public void setSignature(String s) {
    mProps.setCFBundleSignature(s);
  }

  /**
   * Setter for the "jvmversion" attribute (optional)
   */
  public void setJvmversion(String s) {
    mProps.setJVMVersion(s);
  }

  /**
   * Setter for the "infostring" attribute (optional)
   */
  public void setInfostring(String s) {
    mProps.setCFBundleGetInfoString(s);
  }

  /**
   * Setter for the "verbose" attribute (optional)
   */
  public void setVerbose(boolean b) {
    mVerbose = b;
  }

  /**
   * Setter for the "version" attribute (optional)
   */
  public void setVersion(String s) {
    mProps.setCFBundleVersion(s);
  }

  /**
   * Setter for the "jars" attribute (required if no "jarfileset" is present)
   */
  public void setJars(String s) {
    PatternSet patset = new PatternSet();
    patset.setIncludes(s);
    String[] jarNames = patset.getIncludePatterns(getProject());

    for (int i = 0; i < jarNames.length; i++) {
      File f = new File(jarNames[i]);
      mJarAttrs.add(f);
    }
  }

  /**
   * Setter for the "execs" attribute (optional)
   */
  public void setExecs(String s) {
    PatternSet patset = new PatternSet();
    patset.setIncludes(s);
    String[] execNames = patset.getIncludePatterns(getProject());

    for (int i = 0; i < execNames.length; i++) {
      File f = new File(execNames[i]);
      mExecAttrs.add(f);
    }
  }

  /**
   * Setter for the "extraclasspath" attribute (optional)
   */
  public void setExtraclasspath(String s) {
    PatternSet patset = new PatternSet();
    patset.setIncludes(s);
    String[] cpNames = patset.getIncludePatterns(getProject());

    for (int i = 0; i < cpNames.length; i++) {
      File f = new File(cpNames[i]);
      mExtraClassPathAttrs.add(f);
    }    
  }

  /**
   * Set the 'chmod' executable.
   */
  public void setChmod(String s) {
    this.mChmodCommand = s;
  }

  /* ******************************************************************
   * Private utility methods.
   * ******************************************************************/

  /**
   * A terrible hack: Set executable permissions on file f.  This
   * should work on all versions of OS X, assuming /bin/chmod doesn't
   * go away, which seems like a safe bet.  This method will simply
   * return if /etc/chmod doesn't exist.
   *
   * TODO: Make this more platform-independant.  I'm not sure what
   * the correct behavior should be if this is being run on Windows,
   * for example.
   *
   */
  private void setExecutable(File f) throws IOException {
    String filePath = f.getAbsolutePath();

    // See if the the chmod command is really present.  If not, return.
    File test = new File(mChmodCommand);
    if (!test.exists()) {
      return;
    }

    if (mVerbose) {
      System.out.println("Setting file " + f + " executable.");
    }

    Process p = Runtime.
      getRuntime().exec(new String[] {mChmodCommand, "a+x", filePath});

    // The process *may* block until all input and error
    // is consumed.  The command should not produce any output,
    // however.
    InputStream is = null;
    InputStream es = null;
    try {
      is = p.getInputStream();
      es = p.getErrorStream();

      byte[] buf = new byte[1024];
      int len = 0;

      while ((len = is.read(buf)) != -1) {
	System.out.write(buf, 0, len);
      }

      while ((len = es.read(buf)) != -1) {
	System.out.write(buf, 0, len);
      }

    } finally {
      if (is != null) is.close();
      if (es != null) es.close();
    }
  }

  /**
   * Utility method to determine whether this app bundle is targeting a 1.3 or 1.4
   * VM.  The Mac OS X 1.3 VM uses different Java property names from the 1.4 VM
   * to hint at native Mac OS X look and feel options.  For example, on 1.3
   * the Java property to tell the VM to display Swing menu bars as screen menus
   * is "com.apple.macos.useScreenMenuBar".  Under 1.4, it becomes
   * "apple.laf.useScreenMenuBar".  Such is the price of progress, I suppose.
   *
   * Obviously, this logic may need refactoring in the future.
   */
  private boolean useOldPropertyNames() {
    return (mProps.getJVMVersion().startsWith("1.3"));
  }

  private void processJarAttrs() throws BuildException {
    try {
      for (Iterator jarIter = mJarAttrs.iterator(); jarIter.hasNext(); ) {
	File src = (File)jarIter.next();
	File dest = new File(mJavaDir, src.getName());
	if (mVerbose) {
	  System.out.println("Copying from " + src + " to " + dest);
	}
	mFileUtils.copyFile(src, dest);
	mProps.addToClassPath(dest.getName());
      }
    } catch (IOException ex) {
      throw new BuildException("Cannot copy jar file: " + ex);
    }
  }

  private void processJarFileSets() throws BuildException {
    for (Iterator jarIter = mJarFileSets.iterator(); jarIter.hasNext();) {
      FileSet fs = (FileSet)jarIter.next();
      Project p = fs.getProject();
      File srcDir = fs.getDir(p);
      FileScanner ds = fs.getDirectoryScanner(p);
      fs.setupDirectoryScanner(ds,p);
      ds.scan();
      String[] files = ds.getIncludedFiles();
      try {
	for (int i = 0; i < files.length; i++) {
	  String fileName = files[i];
	  File src = new File(srcDir, fileName);
	  File dest = new File(mJavaDir, fileName);
	  if (mVerbose) {
	    System.out.println("Copying from " + src + " to " + dest);
	  }
	  mFileUtils.copyFile(src, dest);
	  mProps.addToClassPath(fileName);
	}

      } catch (IOException ex) {
	throw new BuildException("Cannot copy jar file: " + ex);
      }
    }
  }

  private void processJarFileLists() throws BuildException {
    for (Iterator jarIter = mJarFileLists.iterator(); jarIter.hasNext();) {
      FileList fl = (FileList)jarIter.next();
      Project p = fl.getProject();
      File srcDir = fl.getDir(p);
      String[] files = fl.getFiles(p);
      try {
	for (int i = 0; i < files.length; i++) {
	  String fileName = files[i];
	  File src = new File(srcDir, fileName);
	  File dest = new File(mJavaDir, fileName);
	  if (mVerbose) {
	    System.out.println("Copying from " + src + " to " + dest);
	  }
	  mFileUtils.copyFile(src, dest);
	  mProps.addToClassPath(fileName);
	}
      } catch (IOException ex) {
	throw new BuildException("Cannot copy jar file: " + ex);
      }
    }
  }

  private void processExtraClassPathAttrs() throws BuildException {
    for (Iterator jarIter = mExtraClassPathAttrs.iterator(); jarIter.hasNext(); ) {
      File src = (File)jarIter.next();
      mProps.addToExtraClassPath(src.getPath());
    }
  }

  private void processExtraClassPathFileSets() throws BuildException {
    for (Iterator jarIter = mExtraClassPathFileSets.iterator(); jarIter.hasNext();) {
      FileSet fs = (FileSet)jarIter.next();
      Project p = fs.getProject();
      File srcDir = fs.getDir(p);
      FileScanner ds = fs.getDirectoryScanner(p);
      fs.setupDirectoryScanner(ds,p);
      ds.scan();
      String[] files = ds.getIncludedFiles();
      for (int i = 0; i < files.length; i++) {
	File f = new File(srcDir, files[i]);
	mProps.addToExtraClassPath(f.getPath());
      }
    }
  }

  private void processExtraClassPathFileLists() throws BuildException {
    for (Iterator jarIter = mExtraClassPathFileLists.iterator(); jarIter.hasNext();) {
      FileList fl = (FileList)jarIter.next();
      Project p = fl.getProject();
      File srcDir = fl.getDir(p);
      String[] files = fl.getFiles(p);
      for (int i = 0; i < files.length; i++) {
	File f = new File(srcDir, files[i]);
	mProps.addToExtraClassPath(f.getPath());
      }
    }
  }

  private void processExecAttrs() throws BuildException {
    try {
      for (Iterator execIter = mExecAttrs.iterator(); execIter.hasNext();) {
	File src = (File)execIter.next();
	File dest = new File(mMacOsDir, src.getName());
	if (mVerbose) {
	  System.out.println("Copying from " + src + " to " + dest);
	}
	mFileUtils.copyFile(src, dest);
	setExecutable(dest);
      }
    } catch (IOException ex) {
      throw new BuildException("Cannot copy exec file: " + ex);
    }
  }

  private void processExecFileSets() {
    for (Iterator execIter = mExecFileSets.iterator(); execIter.hasNext();) {
      FileSet fs = (FileSet)execIter.next();
      Project p = fs.getProject();
      File srcDir = fs.getDir(p);
      FileScanner ds = fs.getDirectoryScanner(p);
      fs.setupDirectoryScanner(ds,p);
      ds.scan();
      String[] files = ds.getIncludedFiles();
      try {
	for (int i = 0; i < files.length; i++) {
	  String fileName = files[i];
	  File src = new File(srcDir, fileName);
	  File dest = new File(mMacOsDir, fileName);
	  if (mVerbose) {
	    System.out.println("Copying from " + src + " to " + dest);
	  }
	  mFileUtils.copyFile(src, dest);
	  setExecutable(dest);
	}
      } catch (IOException ex) {
	throw new BuildException("Cannot copy exec file: " + ex);
      }
    }
  }

  private void processExecFileLists() throws BuildException {
    for (Iterator execIter = mExecFileLists.iterator(); execIter.hasNext();) {
      FileList fl = (FileList)execIter.next();
      Project p = fl.getProject();
      File srcDir = fl.getDir(p);
      String[] files = fl.getFiles(p);
      try {
	for (int i = 0; i < files.length; i++) {
	  String fileName = files[i];
	  File src = new File(srcDir, fileName);
	  File dest = new File(mMacOsDir, fileName);
	  if (mVerbose) {
	    System.out.println("Copying from " + src + " to " + dest);
	  }
	  mFileUtils.copyFile(src, dest);
	  setExecutable(dest);
	}
      } catch (IOException ex) {
	throw new BuildException("Cannot copy jar file: " + ex);
      }
    }
  }

  private void copyApplicationStub() throws BuildException {
    if (mVerbose) {
      System.out.println("Copying JavaApplicationStub...");
    }

    File newStubFile = new File(mMacOsDir, "JavaApplicationStub");

    try {
      mFileUtils.copyFile(mStubFile, newStubFile);
    } catch (IOException ex) {
      throw new BuildException("Cannot copy JavaApplicationStub: " + ex);
    }

    // Tweak the permissions on the stub file to set it executable
    try {
      setExecutable(newStubFile);
    } catch (IOException ex) {
      throw new BuildException("Cannot set executable bit: " + ex);
    }
  }

  private void writeInfoPlist() throws BuildException {
    PropertyListWriter listWriter = new PropertyListWriter(mProps);
    File infoPlist = new File(mContentsDir, "Info.plist");
    listWriter.writeFile(infoPlist);
  }

  private void writePkgInfo() throws BuildException {
    File pkgInfo = new File(mContentsDir, "PkgInfo");
    PrintWriter pkgWriter = null;
    try {
      pkgWriter =
	new PrintWriter(new BufferedWriter(new FileWriter(pkgInfo)));
      pkgWriter.println(mProps.getCFBundlePackageType()
			+ mProps.getCFBundleSignature());
      pkgWriter.flush();
    } catch (IOException ex) {
      throw new BuildException("Cannot create PkgInfo file: " + ex);
    } finally {
      if (pkgWriter != null) pkgWriter.close();
    }
  }

}
