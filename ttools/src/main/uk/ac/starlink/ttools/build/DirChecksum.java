package uk.ac.starlink.ttools.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 * Utility to calculate a checksum for all the files in a directory.
 * Filenames and file contents are recursively considered.
 * The checksum is not cryptographically secure.
 * 
 * @author   Mark Taylor
 * @since    12 Feb 2026
 */
public class DirChecksum {

    /**
     * Calculates a checksum for the specified directory and writes it
     * unadorned to standard output.
     */
    public static void main( String[] args ) throws IOException {
        String usage =
            "Usage: " + DirChecksum.class.getSimpleName() + " <dir>\n";
        if ( args.length != 1 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        File file = new File( args[ 0 ] );
        if ( ! file.isDirectory() ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        long checksum = dirChecksum( file );
        System.out.println( checksum );
    }

    /**
     * Calculates and returns a checksum of the recursive content and
     * filenames for the supplied directory.
     *
     * @param  dir  directory to checksum
     * @return  hash
     */
    private static long dirChecksum( File dir ) throws IOException {
        File[] files = listDescendants( dir ).toArray( new File[ 0 ] );
        Arrays.sort( files );
        Checksum checksum = new Adler32();
        byte[] buf = new byte[ 1024 * 16 ];
        for ( File file : files ) {
            byte[] fname = file.getName().getBytes( StandardCharsets.UTF_8 );
            checksum.update( fname, 0, fname.length );
            try ( InputStream in = new FileInputStream( file ) ) {
                for ( int n; ( n = in.read( buf ) ) >= 0; ) {
                    checksum.update( buf, 0, n );
                }
            }
        }
        return checksum.getValue();
    }

    /**
     * Returns a list of recursively accumulated descendant files
     * of a given directory.  The supplied directory itself is not included.
     *
     * @param  dir  directory
     * @return  list of all descendant non-directory files
     */
    private static List<File> listDescendants( File dir ) {
        List<File> list = new ArrayList<>();
        for ( File f : dir.listFiles() ) {
            if ( f.isDirectory() ) {
                list.addAll( listDescendants( f ) );
            }
            else {
                list.add( f );
            }
        }
        return list;
    }
}
