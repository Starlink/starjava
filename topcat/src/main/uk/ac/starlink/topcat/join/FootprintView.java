package uk.ac.starlink.topcat.join;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
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
    public void setFootprint( final Footprint footprint ) {
        footprint_ = footprint;
        if ( footprint == null || footprint.getCoverage() != null ) {
            repaint();
        }
        else {
            new Thread( "Footprinter" ) {
                public void run() {
                    if ( footprint == footprint_ ) {
                        try {
                            footprint.initFootprint();
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    FootprintView.this.repaint();
                                }
                            } );
                        }
                        catch ( IOException e ) {
                        }
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
        super.paintComponent( g );
        Dimension size = getSize();
        Insets insets = getInsets();
        int w = size.width - insets.left - insets.right;
        int h = size.height - insets.top - insets.bottom;
        int x = insets.left;
        int y = insets.top;
        int wover = w - h * 2;
        if ( wover > 0 ) {
            w = h * 2;
            x += wover / 2;
        }
        else {
            h = w / 2;
            y -= wover / 4;
        }
        Shape orb = new Ellipse2D.Float( x, y, w, h );
        Footprint.Coverage coverage =
            footprint_ == null ? null : footprint_.getCoverage();
        Graphics2D g2 = (Graphics2D) g.create();
        if ( coverage == Footprint.Coverage.ALL_SKY ) {
            g2.fill( orb );
        }
        else if ( coverage == Footprint.Coverage.NO_SKY ) {
            g2.draw( orb );
        }
        else if ( coverage == Footprint.Coverage.SOME_SKY ) {
            g2.draw( orb );
            g2.clip( orb );
            g2.fillPolygon( new int[] { x + w, x + 0, x + w },
                            new int[] { y + h, y + h, y + 0 }, 3 );
        }
        else {
            assert coverage == Footprint.Coverage.NO_DATA || coverage == null;
            g2.setColor( Color.GRAY );
            g2.fill( orb );
        }
        setToolTipText( "Footprint sky coverage: " + coverage );
    }

    @Override
    public Dimension getPreferredSize() {
        int h = super.getPreferredSize().height;
        if ( h < 4 ) {
            h = 16;
        }
        Insets insets = getInsets();
        return new Dimension( 2 * h + insets.left + insets.right,
                              h + insets.top + insets.bottom );
    }
}
