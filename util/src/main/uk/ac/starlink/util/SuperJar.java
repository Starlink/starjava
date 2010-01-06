package uk.ac.starlink.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Utility to generate a single jar file containing all the resources
 * referenced by a given jar file.  As well as the contents of the
 * named jar file itself, the contents of any jar files referenced
 * in the Class-Path line of that file's Manifest will be included, and
 * so on recursively.  This can be used to produce a standalone jar
 * file for applications which do not require JNI components.
 * <p>
 * This class has a <tt>main</tt> method, and is designed to be invoked
 * from the command line.
 *
 * @author   Mark Taylor (Starlink)
 */
public class SuperJar {

    private final File[] jarFiles_;
    private final Collection jarExcludeSet_;
    private final Collection fileExcludeSet_;
    private final Collection dirExcludeSet_;
    PrintStream logStrm_ = System.err;

    /**
     * Constructor.
     *
     * @param   jarFiles  top-level jar files containing files and dependencies
     * @param   jarExcludes  names of jar files which may be named as class-path
     *          dependencies but which should not be included in the result
     * @param   entryExcludes  jar file entries which should be excluded 
     *          from the result
     */
    public SuperJar( File[] jarFiles, String[] jarExcludes,
                     String[] entryExcludes ) {
        jarFiles_ = jarFiles;
        jarExcludeSet_ = new HashSet( Arrays.asList( jarExcludes ) );
        fileExcludeSet_ = new HashSet();
        dirExcludeSet_ = new HashSet();
        for ( int i = 0; i < entryExcludes.length; i++ ) {
            String name = entryExcludes[ i ];
            ( ( name.charAt( name.length() - 1 ) == '/' ) ? dirExcludeSet_
                                                          : fileExcludeSet_ )
                .add( name );
        }
    }

    /**
     * Writes the data from this object to a single jar file combining the
     * contents of all the dependencies.
     *
     * @param   out  destination stream
     */
    public void writeSingleJar( OutputStream out ) throws IOException {
        File[] jdeps = getDependencies( jarFiles_ );

        /* Construct a manifest for the output jar file.
         * This contains the main (not per-entry) attributes from the first
         * input jar file, minus any class-path entry. */
        Manifest inManifest = readManifest( jarFiles_[ 0 ] );
        Manifest outManifest = new Manifest();
        for ( Iterator it = inManifest.getMainAttributes().entrySet()
                                      .iterator();
              it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            Attributes outAtts = outManifest.getMainAttributes();
            if ( ! entry.getKey().equals( Attributes.Name.CLASS_PATH ) ) {
                outAtts.put( entry.getKey(), entry.getValue() );
            }
        }

        /* Write all items from input jar files to the single output file. */
        JarOutputStream jout = new JarOutputStream( out, outManifest );
        for ( int ij = 0; ij < jdeps.length; ij++ ) {
            JarInputStream jin =
                new JarInputStream(
                    new BufferedInputStream(
                        new FileInputStream( jdeps[ ij ] ) ) );
            for ( JarEntry jent; ( jent = jin.getNextJarEntry() ) != null;
                  jin.closeEntry() ) {
                if ( ! excludeEntry( jent ) &&
                     ! jent.getName().startsWith( "META-INF" ) ) {
                    jout.putNextEntry( jent );
                    IOUtils.copy( jin, jout );
                }
            }
        }
        jout.finish();
    }

    public void writeZipOfJars( OutputStream out ) throws IOException {
    }

    /**
     * Returns an array of all the jar files which count as dependencies of
     * the given array of jar files.  This is the given files themselves,
     * as well as any files named in their Class-Path manifest attributes,
     * assembled recursively, and excluding duplicates and any that 
     * have been marked for exclusion.
     *
     * @param   jarfiles  input jar files
     * @return  input jarfiles plus recursive dependencies
     */
    public File[] getDependencies( File[] jarfiles ) throws IOException {
        Collection jfSet = new HashSet();
        for ( int ij = 0; ij < jarfiles.length; ij++ ) {
            accumulateDependencies( jarfiles[ ij ], jfSet );
        }
        File[] jfiles = (File[]) jfSet.toArray( new File[ 0 ] );
        Arrays.sort( jfiles );
        return jfiles;
    }

    /**
     * Recursively accumulate dependencies for a given jar file.
     *
     * @param   jfile  input jar file
     * @param   jfSet  set of File objects representing the current list of
     *          known dependencies; this method appends to it
     */
    private void accumulateDependencies( File jfile, Collection jfSet )
            throws IOException {
        if ( jfile.isDirectory() ) {
            throw new IllegalArgumentException( jfile + " is a directory, " +
                                                "only jarfiles allowed" );
        }
        if ( containsFilename( jarExcludeSet_, jfile ) ) {
            if ( logStrm_ != null ) {
                logStrm_.println( "        Excluding: " + jfile );
            }
        }
        else if ( containsFile( jfSet, jfile ) ) {
            if ( logStrm_ != null ) {
                logStrm_.println( "        Duplicate: " + jfile );
            }
        }
        else {
            if ( logStrm_ != null ) {
                logStrm_.println( jfile );
            }
            jfSet.add( jfile );
            Manifest manifest = readManifest( jfile );
            if ( manifest != null ) {
                Attributes atts = manifest.getMainAttributes();
                String classpath = atts.getValue( Attributes.Name.CLASS_PATH );
                if ( classpath != null && classpath.trim().length() > 0 ) {
                    File dir = jfile.getParentFile();
                    String[] cpents = classpath.trim().split( " +" );
                    for ( int ie = 0; ie < cpents.length; ie++ ) {
                        accumulateDependencies( new File( dir, cpents[ ie ] ),
                                                jfSet );
                    }
                }
            }
        }
    }

    /**
     * Reads the manifest from a jar file.
     *
     * @param   jfile   jar file
     * @return   manifest
     */
    private Manifest readManifest( File jfile ) throws IOException {
        JarInputStream jin =
            new JarInputStream(
                new BufferedInputStream( new FileInputStream( jfile ) ) );
        Manifest manifest = jin.getManifest();
        jin.close();
        return manifest;
    }

    /**
     * Determines whether a given file is named in a collection of filenames.
     *
     * @param fnameSet  collection of Strings, each which may be the 
     *        trailing part of a canonical filename
     * @param file  file to test
     * @return   true iff <code>file</code> is named in <code>fnameSet</code>
     */
    private boolean containsFilename( Collection fnameSet, File file )
            throws IOException {
        String cname = file.getCanonicalPath();
        for ( Iterator it = fnameSet.iterator(); it.hasNext(); ) {
            String excl = (String) it.next();
            if ( cname.equals( excl ) ||
                 cname.endsWith( File.separator + excl ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether a given file is one of a collection of filenames.
     *
     * @param   fileSet  collection of Files, may be relative
     * @param   file   file to test
     * @param   true iff <code>file</code> is listed in <code>fileSet</code>
     */
    private boolean containsFile( Collection fileSet, File file )
            throws IOException {
        String cname = file.getCanonicalPath();
        for ( Iterator it = fileSet.iterator(); it.hasNext(); ) {
            if ( ((File) it.next()).getCanonicalPath().equals( cname ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether this object has marked a given jar file entry for
     * exclusion from the result.
     *
     * @param  entry   entry to test
     * @return  true iff <code>entry</code> is marked for exclusion
     */
    private boolean excludeEntry( JarEntry entry ) {
        if ( entry.isDirectory() ) {
            return true;
        }
        String name = entry.getName();
        if ( fileExcludeSet_.contains( name ) ) {
            return true;
        }
        for ( Iterator it = dirExcludeSet_.iterator(); it.hasNext(); ) {
            String exclude = (String) it.next();
            if ( name.startsWith( exclude ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a new jar file based on the contents of an existing jar file
     * and the jar files referenced by its manifest.
     * <p>
     * <h4>Usage:</h4>
     * <pre>
     *    SuperJar [-o outjar]
     *             [[-xjar jar] -xjar jar ...]
     *             [[-xentry entry] -xentry entry ...]
     *             jarfile [jarfile ...]
     * </pre>
     *
     * <p>The <tt>-o</tt> flag may optionally be supplied to provide the
     * name of the output jar file.  Otherwise it will be given a default
     * name (superjar.jar).
     *
     * <p>The <tt>-xjar</tt> (or <tt>-x</tt>, which is a deprecated
     * synonym) flag may be supplied one or more times to define
     * jarfiles which should not be included, even if they are referenced
     * in the manifest's Class-Path entry of a jar file which is included.
     * The <tt>exclude</tt> argument thus defined is the name, optionally
     * with one or more prepended path elements of the jar file to be
     * excluded (e.g. axis.jar or axis/axis.jar would both work).
     *
     * <p>The <tt>-xent</tt> flag may be supplied one or more times to define
     * jar entry names (names of directories or files within the included
     * jar archives) which should not be included.  Directory names should
     * include a trailing "/".
     *
     * <p>The <tt>jarfile</tt> argument(s) will be combined to form
     * the output file, all their contents and those of the jar files
     * referenced in their Class-Path manifest entries will be used.
     * The manifest of the first one will be used as the manifest of
     * the output file (though its Class-Path entry will be empty).
     * Zip files can be used as well, they work the same but have no
     * manifest.
     *
     * @param  args  an array of command-line arguments as described above
     */
    public static void main( String[] args ) throws IOException {
        String usage = "SuperJar [-o out-jar]\n"
                     + "         [-xjar jar [-xjar jar] ..]\n"
                     + "         [-xent entry [-xent entry] ..]\n"
                     + "         jarfile [jarfile ..]";

        /* Process arguments. */
        List arglist = new ArrayList( Arrays.asList( args ) );
        List jarlist = new ArrayList();
        File outfile = new File( "superjar.jar" );
        List jarExcludeList = new ArrayList();
        List entryExcludeList = new ArrayList();
        for ( Iterator it = arglist.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-h" ) ) {
                System.err.println( usage );
                System.exit( 0 );
            }
            else if ( arg.equals( "-o" ) ) {
                it.remove();
                outfile = new File( (String) it.next() );
                it.remove();
            }
            else if ( arg.equals( "-x" ) ||
                      arg.equals( "-xjar" ) ) {
                it.remove();
                jarExcludeList.add( (String) it.next() );
                it.remove();
            }
            else if ( arg.equals( "-xent" ) ) {
                it.remove();
                entryExcludeList.add( (String) it.next() );
                it.remove();
            }
            else {
                jarlist.add( new File( arg ) );
            }
        }
        if ( jarlist.size() == 0 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        File[] jarFiles =
            (File[]) jarlist.toArray( new File[ 0 ] );
        String[] jarExcludes =
            (String[]) jarExcludeList.toArray( new String[ 0 ] );
        String[] entryExcludes =
            (String[]) entryExcludeList.toArray( new String[ 0 ] );

        /* Construct and use the jar writer to output the result. */
        SuperJar sj = new SuperJar( jarFiles, jarExcludes, entryExcludes );
        OutputStream out =
            new BufferedOutputStream( new FileOutputStream( outfile ) );
        sj.writeSingleJar( out );
        out.close();
    }
}
