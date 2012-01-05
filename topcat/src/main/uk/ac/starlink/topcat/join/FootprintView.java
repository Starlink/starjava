package uk.ac.starlink.topcat.join;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.cone.Footprint;

/**
 * Small component which gives a visual representation of a coverage footprint.
 *
 * @author   Mark Taylor
 * @since    5 Jan 2012
 */
public class FootprintView extends JComponent {

    private Footprint footprint_;

    /**
     * Constructor.
     */
    public FootprintView() {
        super();
    }

    /**
     * Sets the footprint for this component to display.
     *
     * @param  footprint  footprint for display
     */
    public void setFootprint( Footprint footprint ) {
        footprint_ = footprint;
        if ( footprint_ == null || footprint_.getCoverage() != null ) {
            repaint();
        }
        else {
            new Thread( "Footprinter" ) {
                public void run() {
                    try {
                        footprint_.initFootprint();
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                FootprintView.this.repaint();
                            }
                        } );
                    }
                    catch ( IOException e ) {
                    }
                }
            }.start();
        }
    }

    /**
     * Returns the footprint currently displayed by this component.
     *
     * @return   footprint
     */
    public Footprint getFootprint() {
        return footprint_;
    }

    @Override
    protected void paintComponent( Graphics g ) {
        Dimension size = getSize();
        Insets insets = getInsets();
        int x = 2 + insets.left;
        int y = 2 + insets.top;
        int w = size.width - 4 - insets.left - insets.right;
        int h = size.height - 4 - insets.top - insets.bottom;
        Footprint.Coverage coverage =
            footprint_ == null ? null : footprint_.getCoverage();
        if ( coverage == Footprint.Coverage.ALL_SKY ) {
            g.fillRect( x, y, w, h );
        }
        else if ( coverage == Footprint.Coverage.NO_SKY ) {
            g.drawRect( x, y, w, h );
        }
        else if ( coverage == Footprint.Coverage.SOME_SKY ) {
            g.drawRect( x, y, w, h );
            g.fillPolygon( new int[] { x + w, x + 0, x + w },
                           new int[] { y + h, y + h, y + 0 }, 3 );
        }
        else {
            assert coverage == Footprint.Coverage.NO_DATA || coverage == null;
        }
        setToolTipText( "Footprint sky coverage: " + coverage );
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension( 24, super.getPreferredSize().height );
    }
}
