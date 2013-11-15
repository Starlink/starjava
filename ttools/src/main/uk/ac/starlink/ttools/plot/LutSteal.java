package uk.ac.starlink.ttools.plot;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Acquires a lookup table by scraping pixels from a colour ramp icon.
 *
 * @author   Mark Taylor
 * @since    28 Aug 2013
 */
public class LutSteal {

    private final Icon icon_;
    private final boolean horiz_;

    /**
     * Constructor.
     *
     * @param   icon   icon containing color ramp
     * @param   horiz  true to traverse ramp horizontally, false for vertically
     */
    public LutSteal( Icon icon, boolean horiz ) {
        icon_ = icon;
        horiz_ = horiz;
    }

    /**
     * Writes the strip of pixels down the middle of the ramp icon
     * as a lookup table that can be used by the Shaders class.
     *
     * @param   out  output stream
     * @param   verbose  true to write RGB values to stderr
     */
    public void writeLut( OutputStream out, boolean verbose )
            throws IOException {
        int w = icon_.getIconWidth();
        int h = icon_.getIconHeight();
        BufferedImage im =
            new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
        Graphics g = im.createGraphics();
        icon_.paintIcon( null, g, 0, 0 );
        g.dispose();
        int n = horiz_ ? w : h;
        Rectangle line = horiz_ ? new Rectangle( 0, h / 2, w, 1 )
                                : new Rectangle( w / 2, 0, 1, h );
        DataBuffer dbuf = im.getData( line ).getDataBuffer();
        DataOutputStream dout =
            new DataOutputStream( new BufferedOutputStream( out ) );
        for ( int i = 0; i < n; i++ ) {
            int isamp = dbuf.getElem( i );
            for ( int j = 0; j < 3; j++ ) {
                int bs = ( isamp & 0xff0000 ) >> 16;
                isamp = isamp << 8;
                float fsamp = bs / 255f;
                dout.writeFloat( fsamp );
                if ( verbose ) {
                    System.err.print( "\t" + bs );
                }
            }
            if ( verbose ) {
                System.err.println();
            }
        }
        dout.flush();
    }

    /**
     * Main method.  Run with -h for help.
     */
    public static void main( String[] args ) throws IOException {
        String usage = LutSteal.class.getName() + " [-x|-y] [-v] <iconfile>";
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        boolean horiz = true;
        boolean verbose = false;
        String filename = null;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( "-x".equals( arg ) ) {
                it.remove();
                horiz = true;
            }
            else if ( "-y".equals( arg ) ) {
                it.remove();
                horiz = false;
            }
            else if ( "-v".equals( arg ) ) {
                it.remove();
                verbose = true;
            }
            else if ( arg.startsWith( "-h" ) ) {
                System.out.println( usage );
                return;
            }
            else if ( filename == null ) {
                it.remove();
                filename = arg;
            }
        }
        if ( filename == null || argList.size() > 0 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        new LutSteal( new ImageIcon( filename ), horiz )
           .writeLut( System.out, verbose );
    }
}
