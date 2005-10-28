package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import junit.framework.TestCase;

public class MarkerTest extends TestCase {

    final static MarkStyleProfile[] PROFILES = new MarkStyleProfile[] {
        MarkStyleProfile.spots( "Small spots", 3 ),
        MarkStyleProfile.spots( "Large spots", 5 ),
        MarkStyleProfile.openShapes( "Small open shapes", 3, Color.BLACK ),
        MarkStyleProfile.openShapes( "Large open shapes", 5, null ),
        MarkStyleProfile.filledShapes( "Small filled shapes", 3, Color.RED ),
        MarkStyleProfile.filledShapes( "Large filled shapes", 5, null ),
        MarkStyleProfile.ghosts( "Small ghosts", 2, 0.6f ),
        MarkStyleProfile.ghosts( "Large ghosts", 5, 0.3f ),
    };

    public MarkerTest( String name ) {
        super( name );
    }

    public void testProfiles() {
        checkProfiles( PROFILES );
        checkProfiles( PlotWindow.MARKER_PROFILES );
    }

    public void checkProfiles( MarkStyleProfile[] profiles ) {
        for ( int i = 0; i < profiles.length; i++ ) {
            for ( int j = 0; j < 16; j++ ) {
                MarkStyleProfile profile = profiles[ i ];
                MarkStyle style = profile.getStyle( j );
                assertEquals( style, profile.getStyle( j ) );
                for ( int k = 0; k < 16; k++ ) {
                    if ( k == j ) {
                        assertEquals( style, profile.getStyle( k ) );
                    }
                    else {
                        assertTrue( style != profile.getStyle( k ) );
                    }
                }
            }
        }
    }

    public void testMarkers() throws InterruptedException {
        try {
            JFrame toplev = new JFrame();
            Container panel = toplev.getContentPane();
            panel.setLayout( new FlowLayout() );
            panel.add( new MarkSamples() );
            panel.add( new ProfileSamples() );
            toplev.pack();
            toplev.setVisible( true );
            Thread.currentThread().sleep( 4000 );
            toplev.dispose();
        }
        catch ( HeadlessException e ) {
            System.out.println( "Headless environment - no marker test" );
        }
    }
}


class MarkSamples extends JPanel {
    final static int STEP = 16;
    final static int SIZES = 8;
    final static int TYPES = 12;

    MarkSamples() {
        setBackground( Color.WHITE );
        setPreferredSize( new Dimension( STEP * ( SIZES + 2 ) + 1,
                                         STEP * ( TYPES + 2 ) + 1 ) );
    }

    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );

        Graphics g2 = (Graphics2D) g;

        g2.setColor( Color.GRAY );

        for ( int i = 0; i < 1000; i += STEP ) {
            g2.drawLine( 0, i, 1000, i );
            g2.drawLine( i, 0, i, 1000 );
        }

        Color c = Color.BLACK;
        for ( int i = 0; i < SIZES; i++ ) {
            MarkStyle[] styles = new MarkStyle[] {
                MarkStyle.openCircleStyle( c, i ),
                MarkStyle.filledCircleStyle( c, i ),
                MarkStyle.openSquareStyle( c, i ),
                MarkStyle.filledSquareStyle( c, i ),
                MarkStyle.openDiamondStyle( c, i ),
                MarkStyle.filledDiamondStyle( c, i ),
                MarkStyle.openTriangleStyle( c, i, true ),
                MarkStyle.filledTriangleStyle( c, i, true ),
                MarkStyle.openTriangleStyle( c, i, false ),
                MarkStyle.filledTriangleStyle( c, i, false ),
                MarkStyle.crossStyle( c, i ),
                MarkStyle.xStyle( c, i ),
            };
            TestCase.assertTrue( styles.length == TYPES );
            for ( int j = 0; j < styles.length; j++ ) {
                styles[ j ].drawMarker( g2, ( i + 1 ) * STEP, 
                                            ( j + 1 ) * STEP );
            }
        }
    }
}

class ProfileSamples extends JPanel {
    final static int STEP = 16;
    final static int ITEMS = 16;
    final static MarkStyleProfile[] PROFILES = MarkerTest.PROFILES;

    ProfileSamples() {
        setBackground( Color.WHITE );
        setPreferredSize( new Dimension( STEP * ( ITEMS + 2 ) + 1,
                                         STEP * ( PROFILES.length + 2 ) + 1 ) );
    }

    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor( Color.GRAY );

        for ( int i = 0; i < 1000; i += STEP ) {
            g2.drawLine( 0, i, 1000, i );
            g2.drawLine( i, 0, i, 1000 );
        }

        for ( int i = 0; i < ITEMS; i++ ) {
            for ( int j = 0; j < PROFILES.length; j++ ) {
                PROFILES[ j ].getStyle( i ).drawMarker( g2, ( i + 1 ) * STEP,
                                                            ( j + 1 ) * STEP );
            }
        }
    }
}
