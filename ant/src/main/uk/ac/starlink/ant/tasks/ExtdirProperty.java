package uk.ac.starlink.ant.tasks;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Reference;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Set a property to contain all the jars files contained in a list of
 * directories. 
 * <p>
 * The value of the property is a classpath that contains references
 * to all the jars and zip files located. The directories specified
 * may not exist, in which case they are just skipped.
 * <p>
 * The list of directories can be given as either a reference to a 
 * path-like structure, or by using embedded path elements.
 *
 * @author Peter W. Draper
 * @since $Id$
 */
public class ExtdirProperty extends Task 
{
    protected String name;
    protected StringBuffer value;
    protected Path path;
    protected Reference ref;

    public ExtdirProperty() 
    {
        super();
    }

    /**
     * Set name of the property.
     *
     * @param name property name
     */
    public void setName( String name ) 
    {
        this.name = name;
    }

    /**
     * Return the name of the property.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Sets a reference to an Ant datatype declared elsewhere. 
     * This should be PATH like structure that defines the extdirs.
     *
     * @param ref reference 
     */
    public void setRefid( Reference ref ) 
    {
        this.ref = ref;
        setPath( (Path) ref.getReferencedObject( getProject() ) );
    }

    /**
     * Get the reference id.
     */
    public Reference getRefid() 
    {
        return ref;
    }

    /**
     * A PATH that pointing to a list of extdirs that should be
     * appended any existing lists.
     *
     * @param path to add to any existing classpath 
     */
    public void setPath( Path path ) 
    {
        if ( this.path == null ) {
            this.path = path;
        } 
        else {
            this.path.append( path );
        }
    }

    /**
     * The PATH to use when forming the extdirs CLASSPATH.
     */
    public Path createPath() 
    {
        if ( this.path == null ) {
            this.path = new Path( project );
        }
        return this.path.createPath();
    }

    /**
     * Get the PATH being used.
     */
    public Path getPath() 
    {
        return path;
    }

    /**
     * Get the value as a string.
     *
     * @return the current value or the empty string
     */
    public String toString() 
    {
        return value == null ? "" : value.toString();
    }

    /**
     * Convert the PATH into a CLASSPATH.
     */
    public void execute() throws BuildException 
    {
        if ( name == null ) {
            throw new BuildException( "You must specify a name for the" +
                                      " property that will contain the" + 
                                      " CLASSPATH");
        } 
        else if ( path == null || ref == null ) {
            throw new BuildException( "You must specify a path " +
                                      "that contains the extension "+
                                      "directories");
        }
        resolveClassPath();
        project.setProperty( name, value.toString() );
    }

    /**
     * Resolve each directory on the PATH into a list of jars files
     * and append these to the CLASSPATH.
     */
    protected void resolveClassPath()
    {
        String[] dirs = path.list();
        ArrayList elements = new ArrayList();
        for ( int i = 0; i < dirs.length; i++ ) {
            File dir = getProject().resolveFile( dirs[i] );
            if ( dir.exists() && dir.isDirectory() ) {
                FileSet fs = new FileSet();
                fs.setDir( dir );
                fs.setIncludes( "*.jar" );
                fs.setIncludes( "*.zip" );

                DirectoryScanner ds = fs.getDirectoryScanner( getProject() );
                String[] s = ds.getIncludedFiles();
                if ( s != null && s.length > 0 ) {
                    addFiles( dir.getAbsolutePath(), s );
                }
            }
        }
    }

    /**
     * Add an array of files to the CLASSPATH.
     */
    protected void addFiles( String dir, String[] s )
    {
        int start = 0;
        if ( value == null ) {
            value = new StringBuffer( dir + File.separatorChar + s[0] );
            start = 1;
        }
        for ( int i = start; i < s.length; i++ ) {
            value.append( File.pathSeparatorChar );
            value.append( dir + File.separatorChar + s[i] );
        }
    }
}
