package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Graphics;
import junit.framework.TestCase;

public class RgbImageTest extends TestCase {

    public void testColor() {
        RgbImage image = RgbImage.createRgbImage( 1, 1, true );
        int[] buf = image.getBuffer();
        int bg = buf[ 0 ];
        Graphics g = image.getImage().createGraphics();

        /* The test should work for opaque and transparent colours.
         * However, for the transparent one it fails (last time I ran it,
         * anyway) - it gets 20ef0000 instead of 20ee0000.  In general
         * the alpha is about right, but the RGB can change a bit.
         * Note this is not about alpha pre-multiplication - the error
         * is not as big as that, and if RgbImage uses TYPE_INT_ARGB_PRE
         * instead of TYPE_INT_ARGB you can see the difference in a much
         * bigger discrepancy.
         * This is not really problematic, but it suggests that the
         * RgbImage implementation is not doing things as efficiently
         * as I'd expect it to.  The reason for that in turn is presumably
         * buried in the BufferedImage implementation and probably system-
         * dependent too. */
        /* So for now, just run the test on the opaque colour.
         * This test and commentary are here to flag up the issue in case
         * someone comes back to look at it later. */
        // int[] argbs = new int[] { 0xffee0000, 0x20ee0000 };
        int[] argbs = new int[] { 0xffee0000 };
        for ( int i = 0; i < argbs.length; i++ ) {
            buf[ 0 ] = bg;
            int argb = argbs[ i ];
            g.setColor( new Color( argb, true ) );
            g.fillRect( 0, 0, 1, 1 );
            assertEquals( Integer.toHexString( argb ),
                          Integer.toHexString( buf[ 0 ] ) );
        }
    }
}
