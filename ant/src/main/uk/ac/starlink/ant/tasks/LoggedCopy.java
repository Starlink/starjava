/*
 * Standard header here.
 */

package uk.ac.starlink.ant.tasks;

import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.Project;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Enumeration;

/**
 *
 * Extends the Copy task to add the ability to record what files are
 * copied to a simple text file. This file can be loaded back into
 * a property using the <loadfile> task, and then into a FileList.
 * (the <listdelete> extension task can process such a list, undoing
 * a <loggedcopy>).
 * <p>
 * New attributes for this task are "logfile", the name of file to
 * write, and "logfileAppend", a boolean indicating if an existing
 * logfile should be deleting or appended to.
 * <p>
 * Note that only files that are actually copied are recorded, files
 * that do not require copying (because an identical copy is already
 * in place) will not be recorded, unless the overwrite attribute is
 * true.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LoggedCopy extends Copy
{
    /** The file used for logging the copied file names */
    protected File logfile = null;

    /** Whether the log file should be just appended */
    protected boolean logfileAppend = false;

    /** Buffered writer for the log file */
    protected BufferedWriter logfileWriter = null;

    /**
     * LoggedCopy task constructor.
     */
    public LoggedCopy()
    {
        super();
    }

    /**
     * Sets the file to use for logging copy operations. A directory is taken
     * to mean no logfile (this is what you get for a "" logfile).
     */
    public void setLogfile( File logfile )
    {
        if ( ! logfile.isDirectory() ) {
            this.logfile = logfile;
        }
        else {
            logfile = null;
        }
    }

    /**
     * Sets whether the logging file should be appended or overwritten, if it
     * exists already.
     */
    public void setLogfileAppend( boolean logfileAppend )
    {
        this.logfileAppend = logfileAppend;
    }

    /**
     * Add a file to the log file. Only done if a log file is available. 
     * Note you should close the log file when all operations are complete.
     */
    protected void addToLogfile( String filename )
    {
        if ( logfile != null ) {
            if ( logfileWriter == null ) {
                try {
                    FileOutputStream f =
                        new FileOutputStream( logfile, logfileAppend );
                    logfileWriter =
                        new BufferedWriter( new OutputStreamWriter( f ) );
                }
                catch ( FileNotFoundException e ) {
                    logfile = null;
                    log( e.getMessage() );
                }
            }
            try {
                logfileWriter.write( filename + "\n" );
            }
            catch ( IOException e ) {
                log( e.getMessage() );
            }
        }
    }

    /**
     * From Copy.doFileOperations:
     *
     * Actually does the file (and possibly empty directory) copies.
     * This is a good method for subclasses to override.
     *
     * So we will to append copied filenames to the log, if requested.
     * Note we log all files, even if they are not copied because the
     * target file is judged to be up to date already.
     */
    protected void doFileOperations() 
    {
        if (fileCopyMap.size() > 0) {
            log("Copying " + fileCopyMap.size()
                + " file" + (fileCopyMap.size() == 1 ? "" : "s")
                + " to " + destDir.getAbsolutePath());

            Enumeration e = fileCopyMap.keys();
            while (e.hasMoreElements()) {
                String fromFile = (String) e.nextElement();
                String[] toFiles = (String[]) fileCopyMap.get(fromFile);

                for (int i = 0; i < toFiles.length; i++) {
                    String toFile = toFiles[i];

                    if (fromFile.equals(toFile)) {
                        log("Skipping self-copy of " + fromFile, verbosity);

                        // PWD: modification from original method.
                        addToLogfile( toFile );
                        continue;
                    }

                    try {
                        log("Copying " + fromFile + " to " + toFile, verbosity);

                        FilterSetCollection executionFilters =
                            new FilterSetCollection();
                        if (filtering) {
                            executionFilters
                                .addFilterSet(getProject().getGlobalFilterSet());
                        }
                        for (Enumeration filterEnum = getFilterSets().elements();
                            filterEnum.hasMoreElements();) {
                            executionFilters
                                .addFilterSet((FilterSet) filterEnum.nextElement());
                        }
                        getFileUtils().copyFile(fromFile, toFile, executionFilters,
                                                getFilterChains(), forceOverwrite,
                                                preserveLastModified, getEncoding(),
                                                getOutputEncoding(), getProject());

                        // PWD: modification from original method.
                        addToLogfile( toFile );

                    } catch (IOException ioe) {
                        String msg = "Failed to copy " + fromFile + " to " + toFile
                            + " due to " + ioe.getMessage();
                        File targetFile = new File(toFile);
                        if (targetFile.exists() && !targetFile.delete()) {
                            msg += " and I couldn't delete the corrupt " + toFile;
                        }
                        throw new BuildException(msg, ioe, getLocation());
                    }
                }
            }

            //  PWD: modification to original method here.
            //  Make sure that the log file is closed (need this as other
            //  processes may write to it).
            if ( logfileWriter != null ) {
                try {
                    logfileWriter.close();
                    logfileWriter = null;
                }
                catch ( IOException ie ) {
                    //  Do nothing, not fatal.
                }
            }
        }

        if (includeEmpty) {
            Enumeration e = dirCopyMap.elements();
            int createCount = 0;
            while (e.hasMoreElements()) {
                String[] dirs = (String[]) e.nextElement();
                for (int i = 0; i < dirs.length; i++) {
                    File d = new File(dirs[i]);
                    if (!d.exists()) {
                        if (!d.mkdirs()) {
                            log("Unable to create directory "
                                + d.getAbsolutePath(), Project.MSG_ERR);
                        } else {
                            createCount++;
                        }
                    }
                }
            }
            if (createCount > 0) {
                log("Copied " + dirCopyMap.size()
                    + " empty director"
                    + (dirCopyMap.size() == 1 ? "y" : "ies")
                    + " to " + createCount
                    + " empty director"
                    + (createCount == 1 ? "y" : "ies") + " under "
                    + destDir.getAbsolutePath());
            }
        }
    }

}
