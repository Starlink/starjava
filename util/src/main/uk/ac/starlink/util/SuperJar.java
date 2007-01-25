package uk.ac.starlink.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static Set doneJars = new HashSet();
    private static JarOutputStream jarOut;
    private static PrintStream logStrm = System.err;
    private static Set jarExcludes = new HashSet();
    private static Set fileExcludes = new HashSet();
    private static Set dirExcludes = new HashSet();
    static {
        dirExcludes.add( "META-INF/" );
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

        /* Turn the arguments into a list of jar files. */
        List arglist = new ArrayList( Arrays.asList( args ) );
        List jarlist = new ArrayList();
        File outfile = new File( "superjar.jar" );
        for ( Iterator it = arglist.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if ( arg.startsWith( "-h" ) ) {
                logStrm.println( usage );
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
                jarExcludes.add( (String) it.next() );
                it.remove();
            }
            else if ( arg.equals( "-xent" ) ) {
                it.remove();
                String name = (String) it.next();
                it.remove();
                ( name.endsWith( "/" ) ? dirExcludes : fileExcludes )
                                     .add( name );
            }
            else {
                jarlist.add( arg );
            }
        }
        if ( jarlist.size() == 0 ) {
            logStrm.println( usage );
            System.exit( 1 );
        }

        /* Construct a Manifest; this will have the same Main-Class as
         * the one from the first input jar file, but no Class-Path. */
        InputStream istrm = new FileInputStream( (String) jarlist.get( 0 ) );
        JarInputStream primaryJar = new JarInputStream( istrm, false );
        Manifest manifest = primaryJar.getManifest();
        istrm.close();
        Attributes mainAtts = manifest.getMainAttributes();
        for ( Iterator it = mainAtts.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            if ( entry.getKey().equals( Attributes.Name.CLASS_PATH ) ) {
                it.remove();
            }
            else {
                logStrm.println( entry.getKey() + ": " + entry.getValue() );
            }
        }

        /* Open the output file. */
        OutputStream ostrm = 
            new BufferedOutputStream( new FileOutputStream( outfile ) );
        jarOut = new JarOutputStream( ostrm, manifest );

        /* Process each jar in turn. */
        for ( Iterator it = jarlist.iterator(); it.hasNext(); ) {
            processJar( new File( (String) it.next() ), 0 );
        }

        /* Close the output stream. */
        jarOut.close();
    }

    /**
     * Takes an input jar file and copies all its entries into the output
     * jar file.
     *
     * @param  jfile  a jarfile whose contents is to be integrated into
     *         the output
     * @param  level  the recursion level at which this is being included.
     *         Used for logging display only
     */
    private static void processJar( File jfile, int level )
            throws IOException {

        /* Check we have a file not directory entry. */
        if ( jfile.isDirectory() ) {
            throw new IllegalArgumentException( jfile + " is a directory, " +
                                                "only jarfiles allowed" );
        }

        /* Skip this one if we've already seen it. */
        String name = jfile.toString();
        String cname = jfile.getCanonicalPath();
        if ( doneJars.contains( cname ) ) {
            logStrm.println( "        Duplicate: " + name );
            return;
        }
        doneJars.add( cname );
        logStrm.println( levelPrefix( level ) + name );

        /* Open a stream from the jar file. */
        InputStream istrm = 
            new BufferedInputStream( new FileInputStream( jfile ) );
        JarInputStream jstrm = new JarInputStream( istrm, false );
        Manifest manifest = jstrm.getManifest();

        /* Copy all the entries to the output jar. */
        byte[] buffer = new byte[ 4096 ];
        for ( JarEntry jent; ( jent = jstrm.getNextJarEntry() ) != null; ) {
            if ( ! excludeEntry( jent ) ) {
                jarOut.putNextEntry( jent );
                for ( int nbyte; ( nbyte = jstrm.read( buffer ) ) >= 0; ) {
                    jarOut.write( buffer, 0, nbyte );
                }
            }
            jstrm.closeEntry();
        }
        jstrm.close();

        /* Recurse. */
        if ( manifest != null ) {
            Attributes atts = manifest.getMainAttributes();
            String classpath = atts.getValue( Attributes.Name.CLASS_PATH );
            if ( classpath != null && classpath.trim().length() > 0 ) {
                File dir = jfile.getParentFile();
                String[] cpents = classpath.trim().split( " +" );
                for ( int i = 0; i < cpents.length; i++ ) {
                    File jf = new File( dir, cpents[ i ] );
                    if ( excludeJar( jf ) ) {
                        logStrm.println( levelPrefix( level ) 
                                       + "        Excluding: " + jf );
                    }
                    else {
                        processJar( jf, level + 1 );
                    }
                }
            }
        }
    }

    /**
     * Whether to exclude a given jar file from the list.
     *
     * @param   file to test
     */
    private static boolean excludeJar( File file ) throws IOException {
        String cname = file.getCanonicalPath();
        for ( Iterator it = jarExcludes.iterator(); it.hasNext(); ) {
            String excl = (String) it.next();
            if ( cname.equals( excl ) ||
                 cname.endsWith( File.separator + excl ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether to exclude a given jar entry from the output.
     *
     * @param  jar entry to test
     */
    private static boolean excludeEntry( JarEntry entry ) {
        if ( entry.isDirectory() ) {
            return true;
        }
        String name = entry.getName();
        if ( fileExcludes.contains( name ) ) {
            return true;
        }
        for ( Iterator it = dirExcludes.iterator(); it.hasNext(); ) {
            String exclude = (String) it.next();
            if ( name.startsWith( exclude ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Outputs a prefix string which serves as a indent for logging at
     * a given level.
     *
     * @param  level  the logging level
     */
    private static String levelPrefix( int level ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < level; i++ ) {
            sbuf.append( "" );  // do nothing
        }
        return sbuf.toString();
    }
 
}
