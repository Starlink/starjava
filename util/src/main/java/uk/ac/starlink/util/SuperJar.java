package uk.ac.starlink.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility to generate a single jar file containing all the resources
 * referenced by a given jar file.  As well as the contents of the
 * named jar file itself, the contents of any jar files referenced
 * in the Class-Path line of that file's Manifest will be included, and
 * so on recursively.  This can be used to produce a standalone jar
 * file for applications which do not require JNI components.
 * <p>
 * This class has a <code>main</code> method, and is designed to be invoked
 * from the command line.
 *
 * @author   Mark Taylor (Starlink)
 */
public class SuperJar {

    private final File[] jarFiles_;
    private final File[] flatFiles_;
    private final File[] jarDeps_;
    private final Collection<String> excludeSet_;
    private static PrintStream logStrm_ = System.err;

    /**
     * Constructor.
     *
     * <p>The <code>entryExcludes</code> values are full pathnames
     * with no leading "/", interpreted as follows:
     * <ul>
     * <li>ending in "/": exclude that directory and all children
     * <li>ending in "/*": exclude all flat files, but not subdirectories
     *     of that directory
     * <li>otherwise: exclude the flat file with that exact name
     * </ul>
     *
     * @param   jarFiles  top-level jar files containing files and dependencies
     * @param   flatFiles  files for inclusion at top level of output
     * @param   jarExcludes  names of jar files which may be named as class-path
     *          dependencies but which should not be included in the result
     * @param   entryExcludes  jar file entries which should be excluded 
     *          from the result
     */
    public SuperJar( File[] jarFiles, File[] flatFiles, String[] jarExcludes,
                     String[] entryExcludes ) throws IOException {
        jarFiles_ = jarFiles;
        flatFiles_ = flatFiles;
        excludeSet_ = new HashSet<String>( Arrays.asList( entryExcludes ) );
        excludeSet_.add( "MANIFEST.MF" );
        jarDeps_ = getDependencies( jarFiles, jarExcludes );
    }

    /**
     * Writes the data from this object to a single jar file combining the
     * contents of all the dependencies.
     *
     * @param   out  destination stream
     */
    public void writeSingleJar( OutputStream out ) throws IOException {

        /* Construct a manifest for the output jar file.
         * This contains the main (not per-entry) attributes from the first
         * input jar file, minus any class-path entry. */
        Manifest inManifest = readManifest( jarFiles_[ 0 ] );
        Manifest outManifest = new Manifest();
        for ( Map.Entry<Object,Object> entry :
              inManifest.getMainAttributes().entrySet() ) {
            Attributes outAtts = outManifest.getMainAttributes();
            if ( ! entry.getKey().equals( Attributes.Name.CLASS_PATH ) ) {
                outAtts.put( entry.getKey(), entry.getValue() );
            }
        }

        /* Set up output to single output jar file. */
        JarOutputStream jout = new JarOutputStream( out, outManifest );

        /* Write flat files. */
        writeFlatFiles( jout );

        /* Write all items from input jar files. */
        for ( int ij = 0; ij < jarDeps_.length; ij++ ) {
            JarInputStream jin =
                new JarInputStream(
                    new BufferedInputStream(
                        new FileInputStream( jarDeps_[ ij ] ) ) );
            for ( JarEntry jent; ( jent = jin.getNextJarEntry() ) != null;
                  jin.closeEntry() ) {
                if ( ! excludeEntry( jent ) ) {
                    jout.putNextEntry( jent );
                    IOUtils.copy( jin, jout );
                    jout.closeEntry();
                }
            }
        }
        jout.finish();
    }

    /**
     * Writes the data from this object to a zip file containing all the
     * top-level and dependency jars as individual jar files in the same
     * directory.
     *
     * @param   out  destination stream
     */
    public void writeZipOfJars( OutputStream out ) throws IOException {

        /* All the jar files will go effectively in the same directory
         * (at the same level in the zip file).  Check that they have
         * different names. */
        Map<String,File> tailMap = new HashMap<String,File>();
        for ( int ij = 0; ij < jarDeps_.length; ij++ ) {
            File file = jarDeps_[ ij ];
            String tail = file.getName();
            if ( tailMap.containsKey( tail ) ) {
                throw new RuntimeException( "Jar file name clash: "
                                          + tailMap.get( tail ) + ", " + file );
            }
            tailMap.put( tail, file );
        }

        /* Prepare to write to a big zip file. */
        ZipOutputStream zout = new ZipOutputStream( out );

        /* Write flat files. */
        writeFlatFiles( zout );

        /* Write each of the jar files. */
        for ( int ij = 0; ij < jarDeps_.length; ij++ ) {
            File jfile = jarDeps_[ ij ];
            String jtail = jfile.getName();
            Manifest inManifest = readManifest( jfile );

            /* If it's a jar file (has a manifest), doctor the manifest
             * to get the classpath right, and then copy entries. */
            if ( inManifest != null ) {
                String[] cpents = getClassPath( inManifest );
                StringBuffer cpbuf = new StringBuffer();
                for ( int ic = 0; ic < cpents.length; ic++ ) {
                    String cptail = new File( cpents[ ic ] ).getName();
                    if ( tailMap.containsKey( cptail ) ) {
                        if ( cpbuf.length() > 0 ) {
                            cpbuf.append( ' ' );
                        }
                        cpbuf.append( cptail );
                    }
                }
                Attributes atts = inManifest.getMainAttributes();
                atts.put( Attributes.Name.CLASS_PATH, cpbuf.toString() );
                Manifest outManifest = new Manifest();
                outManifest.getMainAttributes().putAll( atts );
                zout.putNextEntry( new ZipEntry( jtail ) );
                JarOutputStream jout = new JarOutputStream( zout, outManifest );
                JarInputStream jin =
                    new JarInputStream(
                        new BufferedInputStream(
                            new FileInputStream( jfile ) ) );
                for ( JarEntry jent; ( jent = jin.getNextJarEntry() ) != null;
                      jin.closeEntry() ) {
                    if ( ! excludeEntry( jent ) &&
                         ! jent.getName().startsWith( "META-INF" ) ) {
                        jout.putNextEntry( jent );
                        IOUtils.copy( jin, jout );
                        jout.closeEntry();
                    }
                }
                jout.finish();
                zout.closeEntry();
            }

            /* Otherwise just copy it as a zip file. */
            else {
                zout.putNextEntry( new ZipEntry( jtail ) );
                ZipOutputStream z2out = new ZipOutputStream( zout );
                ZipInputStream zin =
                    new ZipInputStream(
                        new BufferedInputStream(
                            new FileInputStream( jfile ) ) );
                for ( ZipEntry zent; ( zent = zin.getNextEntry() ) != null;
                      zin.closeEntry() ) {
                    if ( ! excludeEntry( zent ) ) {
                        z2out.putNextEntry( zent );
                        IOUtils.copy( zin, z2out );
                    }
                }
                z2out.finish();
                zout.closeEntry();
            }
        }
        zout.finish();
    }

    /**
     * Determines whether this object has marked a given jar file entry for
     * exclusion from the result.
     *
     * @param  entry   entry to test
     * @return  true iff <code>entry</code> is marked for exclusion
     */
    private boolean excludeEntry( ZipEntry entry ) {
        if ( entry.isDirectory() ) {
            return true;
        }
        String name = entry.getName();
        for ( String exclude : excludeSet_ ) {
            if ( exclude.endsWith( "/" ) ) {
                if ( name.startsWith( exclude ) ) {
                    return true;
                }
            }
            else if ( exclude.endsWith( "/*" ) || exclude.equals( "*" ) ) {
                String dir = exclude.substring( 0, exclude.length() - 1 );
                if ( name.startsWith( dir ) && 
                     name.indexOf( '/', dir.length() ) == -1 ) {
                    return true;
                }
            }
            else {
                if ( name.equals( exclude ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Outputs this objects designated flat files to a given zip output stream.
     *
     * @param  zout  destination archive stream
     */
    private void writeFlatFiles( ZipOutputStream zout ) throws IOException {
        for ( int i = 0; i < flatFiles_.length; i++ ) {
            File file = flatFiles_[ i ];
            String ftail = file.getName();
            ZipEntry zent = new ZipEntry( ftail );
            zout.putNextEntry( zent );
            InputStream fin = new FileInputStream( file );
            IOUtils.copy( fin, zout );
            fin.close();
            zout.closeEntry();
        }
    }

    /**
     * Returns an array of all the jar files which count as dependencies of
     * the given array of jar files.  This is the given files themselves,
     * as well as any files named in their Class-Path manifest attributes,
     * assembled recursively, and excluding duplicates and any that 
     * have been marked for exclusion.
     *
     * @param   jarfiles  input jar files
     * @param   jarExcludes  names of jar files which may be named as class-path
     *          dependencies but which should not be included in the result
     * @return  input jarfiles plus recursive dependencies
     */
    private static File[] getDependencies( File[] jarfiles,
                                           String[] jarExcludes )
            throws IOException {
        Collection<File> jfSet = new HashSet<File>();
        Collection<String> jarExcludeSet =
            new HashSet<String>( Arrays.asList( jarExcludes ) );
        for ( int ij = 0; ij < jarfiles.length; ij++ ) {
            accumulateDependencies( jarfiles[ ij ], jarExcludeSet, jfSet );
        }
        File[] jfiles = jfSet.toArray( new File[ 0 ] );
        Arrays.sort( jfiles );
        return jfiles;
    }

    /**
     * Recursively accumulate dependencies for a given jar file.
     *
     * @param   jfile  input jar file
     * @param   jarExcludeSet  collection of Strings naming jar files which 
     *          may be named as class-path dependencies but which should not
     *          be included in the result
     * @param   jfSet  set of File objects representing the current list of
     *          known dependencies; this method appends to it
     */
    private static void accumulateDependencies( File jfile,
                                               Collection<String> jarExcludeSet,
                                               Collection<File> jfSet )
            throws IOException {
        if ( jfile.isDirectory() ) {
            throw new IllegalArgumentException( jfile + " is a directory, " +
                                                "only jarfiles allowed" );
        }
        if ( containsFilename( jarExcludeSet, jfile ) ) {
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
            String[] cpents = getClassPath( readManifest( jfile ) );
            File dir = jfile.getParentFile();
            for ( int ie = 0; ie < cpents.length; ie++ ) {
                accumulateDependencies( new File( dir, cpents[ ie ] ),
                                        jarExcludeSet, jfSet );
            }
        }
    }

    /**
     * Reads the manifest from a jar file.
     *
     * @param   jfile   jar file
     * @return   manifest
     */
    private static Manifest readManifest( File jfile ) throws IOException {
        JarInputStream jin =
            new JarInputStream(
                new BufferedInputStream( new FileInputStream( jfile ) ) );
        Manifest manifest = jin.getManifest();
        jin.close();
        return manifest;
    }

    /**
     * Gets the declared class-path attribute from a manifest, returning
     * the result as an array of strings, one for each entry.
     *
     * @param   manifest  manifest
     * @return  declared class path elements
     */
    private static String[] getClassPath( Manifest manifest ) {
        if ( manifest == null ) {
            return new String[ 0 ];
        }
        else {
            Attributes atts = manifest.getMainAttributes();
            String classpath = atts.getValue( Attributes.Name.CLASS_PATH );
            return classpath == null || classpath.trim().length() == 0
                 ? new String[ 0 ]
                 : classpath.trim().split( " +" );
        }
    }

    /**
     * Determines whether a given file is named in a collection of filenames.
     *
     * @param fnameSet  collection of Strings, each which may be the 
     *        trailing part of a canonical filename
     * @param file  file to test
     * @return   true iff <code>file</code> is named in <code>fnameSet</code>
     */
    private static boolean containsFilename( Collection<String> fnameSet,
                                             File file )
            throws IOException {
        String cname = file.getCanonicalPath();
        for ( String excl : fnameSet ) {
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
    private static boolean containsFile( Collection<File> fileSet, File file )
            throws IOException {
        String cname = file.getCanonicalPath();
        for ( File f : fileSet ) {
            if ( f.getCanonicalPath().equals( cname ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a new jar or zip file based on the contents of an existing
     * jar file and the jar files referenced by its manifest.
     *
     * <p><strong>Usage:</strong></p>
     * <pre>
     *    SuperJar [-oj outjar] [-oz outzip]
     *             [[-xjar jar] -xjar jar ...]
     *             [[-xentry entry] -xentry entry ...]
     *             jarfile [jarfile ...]
     * </pre>
     *
     * <p>If the <code>-oj</code> flag is given, it supplies the name of
     * a monolithic jar file to output.
     * <code>-o</code> is a deprecated synonym for <code>-oj</code>.
     *
     * <p>If the <code>-oz</code> flag is given, it supplies the name of a zip
     * file to output.  This zip file will contain all the named and 
     * referenced jar files in a single flat directory.
     *
     * <p>The <code>-xjar</code>
     * flag may be supplied one or more times to define
     * jarfiles which should not be included, even if they are referenced
     * in the manifest's Class-Path entry of a jar file which is included.
     * The <code>exclude</code> argument thus defined is the name, optionally
     * with one or more prepended path elements of the jar file to be
     * excluded (e.g. axis.jar or axis/axis.jar would both work).
     * <code>-x</code> is a deprecated synonym for <code>-xjar</code>.
     *
     * <p>The <code>-xent</code> flag may be supplied one or more times to give
     * full paths for jar entries which should not be included in the output.
     * These are interpreted as follows:
     * <ul>
     * <li>ending in "/": exclude that directory and all children
     * <li>ending in "/*": exclude all flat files, but not subdirectories
     *     of that directory
     * <li>otherwise: exclude the flat file with that exact name
     * </ul>
     *
     * <p>The <code>jarfile</code> argument(s) will be combined to form
     * the output file, all their contents and those of the jar files
     * referenced in their Class-Path manifest entries will be used.
     * The manifest of the first one will be used as the manifest of
     * the output file (though its Class-Path entry will be empty).
     * Zip files can be used as well, they work the same but have no
     * manifest.
     *
     * <p>Any <code>flat-file</code> arguments will be included as files
     * at the top level of the output jar or zip file.
     *
     * @param  args  an array of command-line arguments as described above
     */
    public static void main( String[] args ) throws IOException {
        String usage = "SuperJar [-oj out-jar] [-oz out-zip]\n"
                     + "         [-xjar jar [-xjar jar] ..]\n"
                     + "         [-xent entry [-xent entry] ..]\n"
                     + "         [-file flatfile [-file flatfile] ..]\n"
                     + "         jarfile [jarfile ..]";

        /* Process arguments. */
        List<String> arglist = new ArrayList<String>( Arrays.asList( args ) );
        List<File> jarlist = new ArrayList<File>();
        List<File> flatFileList = new ArrayList<File>();
        File outJar = null;
        File outZip = null;
        List<String> jarExcludeList = new ArrayList<String>();
        List<String> entryExcludeList = new ArrayList<String>();
        for ( Iterator<String> it = arglist.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.startsWith( "-h" ) ) {
                System.err.println( usage );
                System.exit( 0 );
            }
            else if ( arg.equals( "-oj" ) ||
                      arg.equals( "-o" ) ) {
                it.remove();
                outJar = new File( it.next() );
                it.remove();
            }
            else if ( arg.equals( "-oz" ) ) {
                it.remove();
                outZip = new File( it.next() );
                it.remove();
            }
            else if ( arg.equals( "-x" ) ||
                      arg.equals( "-xjar" ) ) {
                it.remove();
                jarExcludeList.add( it.next() );
                it.remove();
            }
            else if ( arg.equals( "-xent" ) ) {
                it.remove();
                entryExcludeList.add( it.next() );
                it.remove();
            }
            else if ( arg.equals( "-file" ) ) {
                it.remove();
                flatFileList.add( new File( it.next() ) );
                it.remove();
            }
            else {
                jarlist.add( new File( arg ) );
                it.remove();
            }
        }
        if ( jarlist.size() == 0 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        File[] jarFiles = jarlist.toArray( new File[ 0 ] );
        File[] flatFiles = flatFileList.toArray( new File[ 0 ] );
        String[] jarExcludes = jarExcludeList.toArray( new String[ 0 ] );
        String[] entryExcludes = entryExcludeList.toArray( new String[ 0 ] );

        /* Construct the writer. */
        SuperJar sj =
            new SuperJar( jarFiles, flatFiles, jarExcludes, entryExcludes );

        /* Warn if no output. */
        if ( outJar == null && outZip == null ) {
            System.err.println( "No output requested (use -oj or -oz)" );
        }

        /* Write a monolithic jar file if required. */
        if ( outJar != null ) {
            System.err.println( "Writing monolithic jar file: " + outJar );
            OutputStream jout =
                new BufferedOutputStream( new FileOutputStream( outJar ) );
            sj.writeSingleJar( jout );
            jout.close();
        }

        /* Write a monolithic zip file if required. */
        if ( outZip != null ) {
            System.err.println( "Writing zip of jars: " + outZip );
            OutputStream zout =
                new BufferedOutputStream( new FileOutputStream( outZip ) );
            sj.writeZipOfJars( zout );
            zout.close();
        }
    }
}
