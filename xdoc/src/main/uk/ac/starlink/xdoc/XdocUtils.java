package uk.ac.starlink.xdoc;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 * Utilities designed for use during XSTL processing.
 * These generally provide some kind of processing based on the values of
 * strings which return strings for insertion into a document. 
 * They can be used in conjunction with Xalan's XSLT processing by
 * adding the attribute
 * <pre>
 *    xmlns:XdocUtils="xalan://uk.ac.starlink.xdoc.XdocUtils"
 * </pre>
 * to the xsl:stylesheet element of an XSLT stylesheet.
 * Other processors probably have their own mechanisms.
 *
 * @author   Mark Taylor
 * @since    20 Feb 2006
 */
public class XdocUtils {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.xdoc" );

    private XdocUtils() {
    }

    /**
     * Returns the usage string for a given class.
     * It invokes the <code>main(String[])</code> method of the 
     * named class with the single argument "<code>-help</code>",
     * and captures and returns the output.
     *
     * @param   clazzName  fully qualified name of class having main() method
     * @return  usage string
     */
    public static String classUsage( String clazzName )
            throws ClassNotFoundException, NoSuchMethodException,
                   IllegalAccessException, InvocationTargetException {
        Class<?> clazz = Class.forName( clazzName );
        Method main = clazz.getMethod( "main", String[].class );
        PrintStream origOut = System.out;
        ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream( bufOut );
        System.setOut( pout );
        main.invoke( null, new Object[] { new String[] { "-help" } } );
        pout.flush();
        pout.close();
        System.setOut( origOut );
		return new String( bufOut.toByteArray() );
    }

    /**
     * Returns the approximate size of a file in human-readable form
     * given the location of a file.
     *
     * @param   loc  file location
     * @return  file  file length string, such as "5.6M"
     */
    public static String reportFileSize( String loc ) {
        long size = getFileSize( loc );
        if ( size > 1024 * 1024 ) {
            long mb10 = Math.round( size * 10.0 / 1024 / 1024 );
            float mb = (float) ( mb10 / 10.0 );
            return Float.toString( mb ) + "M";
        }
        else {
            int kb = Math.round( size / 1024 );
            return Integer.toString( kb ) + "k";
        }
    }

    /**
     * Returns the length of a file in bytes given its location.
     *
     * @param  loc  file location
     * @return  file length
     */
    private static long getFileSize( String loc ) {
        File f = new File( loc );
        if ( ! f.exists() ) {
            System.err.println( "File " + loc + " does not exist" );
            System.exit( 1 );
        }
        long leng = f.length();
        if ( leng == 0 ) {
            System.err.println( "File " + loc + " has zero length" );
            System.exit( 1 );
        }
        return leng;
    }

    /**
     * Returns the dimensions of an image file with the given filename.
     * If no dimensions can be established, a message is reported through
     * the logging system, and a Dimension object with negative dimensions
     * is returned.
     *
     * @param  fname  filename of image file
     * @return   dimensions of image, or dummy <code>new Dimension(-1,-1)</code>
     */
    public static Dimension getImageSize( String fname ) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[ 8096 ];
        try ( InputStream in = new FileInputStream( fname ) ) {
            for ( int n; ( n = in.read( buf ) ) > 0; ) {
                bout.write( buf, 0, n );
            }
            bout.close();
            ImageIcon icon = new ImageIcon( bout.toByteArray() );
            return new Dimension( icon.getIconWidth(),
                                  icon.getIconHeight() );
        }
        catch ( IOException e ) {
            logger_.warning( "Can't establish dimensions for \"" + fname + "\""
                           + " (" + e + ")" );
            return new Dimension( -1, -1 );
        }
    }
}
