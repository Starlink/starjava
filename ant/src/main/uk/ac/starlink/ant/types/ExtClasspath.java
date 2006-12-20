package uk.ac.starlink.ant.types;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Create a path-like structure to contain all the given jar files and those
 * that are specified as bundled optional packages in their manifests. This
 * generates a path that is equivalent to the path used by the extension
 * mechanism classloader when using the given list of jar files.
 * <p>
 * In Java 1.5 this function should no longer be needed as the compiler does
 * this job for us (but seems to have problems with circular dependencies), so
 * this function becomes a simple path-like structure. Eventually all use of
 * this type should be deprecated and replaced with a normal path.
 * <p>
 * The list of jar files can be given as either a reference to a path-like
 * structure, or by using embedded path elements.
 *
 * @author Peter W. Draper
 * @since $Id$
 */
public class ExtClasspath extends Path
{
    protected String name;
    protected StringBuffer value;
    protected Path path;
    protected Path resultPath;
    protected Reference ref;

    // List of download jar files. Need to avoid referencing same more than
    // once as this will lead to infinite recursion (may fail later as this is
    // a circular dependency).
    private Vector jarsDone = new Vector();

    public ExtClasspath( Project p )
    {
        super( p );
    }

    public ExtClasspath( Project p, String path )
    {
        super( p, path );
    }

    /**
     * Returns all path elements defined by this and nested path objects.
     *
     * @return list of path elements.
     */
    public String[] list()
    {
        //  Java 1.5 doesn't need this and we don't support JDKs before 1.4,
        //  so if not 1.4 nothing to do.
        if ( JavaEnvUtils.isJavaVersion( JavaEnvUtils.JAVA_1_4 ) ) {

            //  Extend any jar files to include their bundled optional
            //  references and in turn any references in these new jars files
            //  until path is complete and all bundled optional jars have been
            //  located and then proceed as normal.
            String[] jars = super.list();
            for ( int i = 0; i < jars.length; i++ ) {
                File jarFile = project.resolveFile( jars[i] );
                if ( jarFile.exists() ) {
                    if ( "jar".equals( getExtension( jarFile ) ) ) {
                        addDownloads( jarFile );
                    }
                }
            }
        }
        else {
            log( "[extclasspath] not required for this compiler", Project.MSG_VERBOSE );
        }
        return super.list();
    }

    /**
     * Add the downloads from a jar file to the path. The downloads
     * are also checked for any downloads they reference.
     */
    protected void addDownloads( File jarFile )
    {
        log( "[extclasspath] adding extension jar files from " + jarFile, 
             Project.MSG_VERBOSE );
        File jarBase = jarFile.getParentFile();
        File newJarFile = null;
        String s[] = getDownloads( jarFile );
        if ( s != null && s.length > 0 ) {
            for ( int j = 0; j < s.length; j++ ) {
                newJarFile = new File( jarBase, s[j] );
                try {
                    //  May be expensive, but needed for cyclical dependencies
                    //  that may be spoofed by relative names.
                    newJarFile = newJarFile.getCanonicalFile();
                }
                catch (IOException e) {
                    // Cannot resolve name so just use non-canonical form.
                    e.printStackTrace();
                }

                //  Add this Jar file if it exists on disk and hasn't been
                //  seen already. Visit its extension jar files too.
                if ( newJarFile.exists() ) {
                    if ( "jar".equals( getExtension( newJarFile ) ) ) {

                        //  Note this should compare canonical name against
                        //  those visited already.
                        if ( jarsDone.indexOf( newJarFile ) == -1 ) {
                            setLocation( newJarFile );

                            // Mark this as done now, before visiting its
                            // extensions.
                            jarsDone.add( newJarFile );
                            addDownloads( newJarFile );
                            log( "[extclasspath] adding " + newJarFile + 
                                 " to extension jar files",
                                 Project.MSG_VERBOSE );
                        } 
                        else {
                            log( "[extclasspath] dropping " + newJarFile + 
                                 " from extension jar files as already done",
                                 Project.MSG_VERBOSE );
                        }
                    }
                    else {
                        log( "[extclasspath] dropping " + newJarFile + 
                             " from extension jar files as not jar file",
                             Project.MSG_VERBOSE );
                    }
                }
                else {
                    log( "[extclasspath] dropping " + newJarFile + 
                         " from extension jar files as does not exist",
                         Project.MSG_VERBOSE ) ;
                }                
            }
        }
    }

    /**
     * Extract any bundled optional packages from a jar file's manifest and
     * return them as an array of Strings.
     */
    protected String[] getDownloads( File jarFile )
    {
        ArrayList results = new ArrayList();
        JarFile jarFileFile = null;
        Manifest manifest = null;
        try {
            jarFileFile = new JarFile( jarFile );
            manifest = jarFileFile.getManifest();
            if ( manifest == null ) {
                return null;
            }
        }
        catch (Exception e) {
            return null;
        }
        Attributes attributes = manifest.getMainAttributes();

        Attributes.Name pathKey = Attributes.Name.CLASS_PATH;
        String path = attributes.getValue( pathKey );
        if ( path != null ) {

            //  Needs splitting into parts (space separated ).
            StringTokenizer tokenizer = new StringTokenizer( path, " " );
            String[] result = new String[ tokenizer.countTokens() ];
            for( int i = 0; i < result.length; i++ ) {
                results.add( tokenizer.nextToken() );
            }
        }
        return (String []) results.toArray( new String[0] );
    }

    /**
     * Get a file extension.
     */
    public String getExtension( File file )
    {
        if ( file != null ) {
            String filename = file.getName();
            int i = filename.lastIndexOf( '.' );
            if( i > 0 && i < filename.length() - 1 ) {
                return filename.substring( i + 1 ).toLowerCase();
            }
        }
        return null;
    }
}
