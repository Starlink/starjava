package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.RenderingHints;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import junit.framework.TestCase;

public class ErrorBarTest extends TestCase {

    public ErrorBarTest( String name ) {
        super( name );
    }

    public void test2dBars() throws InterruptedException {
        try {
            JFrame toplev = new JFrame();
            Container p = toplev.getContentPane();
            p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
            p.add( new ErrorBarSamples( new int[] { -5, 5, 0, 0, },
                                        new int[] { 0, 0, -8, 8, } ) );
            p.add( new ErrorBarSamples( new int[] { -4, 10, 0, 0, },
                                        new int[] { 0, 0, -5, 9, } ) );
            p.add( new ErrorBarSamples( new int[] { 4, -4, 4, -4, },
                                        new int[] { -4, 4, 4, -4, } ) );
            p.add( new ErrorBarSamples( new int[] { -2, 2, 6, -9, },
                                        new int[] { 8, -8, 4, -6, } ) );
            toplev.pack();
            toplev.setVisible( true );
            Thread.currentThread().sleep( 2000 );
            toplev.dispose();
        }
        catch ( HeadlessException e ) {
            System.out.println( "Headless environment - no error bar test" );
        }
    }

    public void test2d() {
        ErrorRenderer[] opts = ErrorRenderer.getOptions2d();
        for ( int i = 0; i < opts.length; i++ ) {
            assertTrue( opts[ i ].supportsDimensionality( 2 ) );
        }
    }

    private static class ErrorBarSamples extends JPanel {

        private final int[] xoffs_;
        private final int[] yoffs_;

        private final static int STEP = 32;
        private final static ErrorRenderer[] RENDERERS =
            ErrorRenderer.getOptions2d();

        ErrorBarSamples( int[] xoffs, int[] yoffs ) {
            xoffs_ = xoffs;
            yoffs_ = yoffs;
            setBackground( Color.WHITE );
            setPreferredSize( new Dimension( ( RENDERERS.length + 1 ) * STEP,
                                             STEP ) );
        }

        protected void paintComponent( Graphics g ) {
            super.paintComponent( g );

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
                                 RenderingHints.VALUE_ANTIALIAS_ON );

            g2.setColor( Color.LIGHT_GRAY );

            for ( int i = 0; i < 1000; i+= STEP ) {
                int yoff = STEP / 2;
                g2.drawLine( 0, i + yoff, 1000, i + yoff );
                g2.drawLine( i, 0, i, 1000 );
            }

            g2.setColor( Color.RED );
            int nsym = xoffs_.length;
            for ( int i = 0; i < RENDERERS.length; i++ ) {
                ErrorRenderer renderer = RENDERERS[ i ];
                renderer.drawErrors( g2, ( i + 1 ) * STEP, STEP / 2,
                                     xoffs_, yoffs_ );
            }
        }
    }
}
