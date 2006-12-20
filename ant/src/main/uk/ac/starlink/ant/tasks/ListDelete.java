package uk.ac.starlink.ant.tasks;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.PatternSet;

import org.apache.tools.ant.taskdefs.Delete;

import java.io.File;

/**
 * Extends the delete task so that it can also accepts a simple
 * FileList of names to delete. These can include files and
 * directories.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ListDelete extends Delete
{
    public ListDelete()
    {
        super();
    }

    /**
     * The list of files, if defined.
     */
    protected FileList filelist = null;

    /**
     * Adds a list of files to be deleted.
     */
    public void addFilelist( FileList filelist )
    {
        this.filelist = filelist;
    }

    /**
     * Delete the file(s). Override to add FileList logic.
     */
    public void execute() throws BuildException
    {
        if ( filelist != null ) {

            // Loop over all files in the list.
            String[] files = filelist.getFiles( project );

            for ( int i = 0; i < files.length; i++ ) {
                File file = new File( files[i] );

                if ( file.exists() ) {
                    if ( file.isDirectory() ) {
                        removeDir( file );
                    }
                    else {
                        log( "Deleting: " + file.getAbsolutePath(),
                             Project.MSG_VERBOSE );

                        if ( ! file.delete() ) {
                            String message = "Unable to delete file "
                                             + file.getAbsolutePath();
                            if ( isFailOnError() ) {
                                throw new BuildException( message );
                            }
                            else {
                                log( message, isQuiet() ? Project.MSG_VERBOSE
                                                        : Project.MSG_WARN );
                            }
                        }
                    }
                }
                else {
                    log( "Could not find file " + file.getAbsolutePath()
                         + " to delete.", Project.MSG_VERBOSE );
                }
            }
        }

        //  Avoid unpleasant error messages that are not appropriate
        //  when we have a list of files.
        if ( file == null && dir == null && filesets.size() == 0 &&
             filelist != null ) {
            return;
        }
        super.execute();
    }
}
