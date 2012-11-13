package uk.ac.starlink.topcat.join;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.geom.Ellipse2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import uk.ac.starlink.ttools.cone.Footprint;

/**
 * Small component which gives a visual representation of a coverage footprint.
 * This is currently a pixel-resolution Mollweide projection of of the
 * coverage.  Component foreground and background colours are used.
 *
 * @author   Mark Taylor
 * @since    5 Jan 2012
 */
public class FootprintView extends JComponent {

    private volatile Footprint footprint_;
    private Icon fpIcon_;
    private Object iconKey_;
    private String subject_;
    private volatile Thread footprintThread_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.join" );

    /**
     * Constructs a footprint with subject description.
     *
     * @param   subject   short description of what the footprint is of,
     *                    used in tooltip
     */
    public FootprintView( String subject ) {
        super();
        subject_ = subject;
    }

    /**
     * Constructor.
     */
    public FootprintView() {
        this( null );
    }

    /**
     * Sets the footprint for this component to display.
     * The footprint is initialised asynchronously; the initialising
     * thread will be <code>interrrupt</code>ed if a new footprint needs
     * initialising before it's done.
     *
     * @param  footprint  footprint for display
     */
    public void setFootprint( final Footprint footprint ) {
        footprint_ = footprint;
        if ( footprint != null && footprint.getCoverage() == null ) {
            final Thread thread = new Thread( "Footprinter" ) {
                public void run() {
                    final Thread fthread = this;
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
                            logger_.info( "Footprint initialization did not "
                                        + "complete: " + e );
                        }
                        finally {
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    if ( footprintThread_ == fthread ) {
                                        footprintThread_ = null;
                                    }
                                }
                            } );
                        }
                    }
                }
            };
            if ( footprintThread_ != null ) {
                footprintThread_.interrupt();
            }
            footprintThread_ = thread;
            thread.start();
        }
        repaint();
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
        Ellipse2D orb = new Ellipse2D.Float( x, y, w, h );
        Footprint.Coverage coverage =
            footprint_ == null ? null : footprint_.getCoverage();
        Graphics2D g2 = (Graphics2D) g;
        if ( coverage == Footprint.Coverage.ALL_SKY ) {
            g2.fill( orb );
        }
        else if ( coverage == Footprint.Coverage.NO_SKY ) {
            Color color = g2.getColor();
            g2.setColor( getBackground() );
            g2.fill( orb );
            g2.setColor( color );
        }
        else if ( coverage == Footprint.Coverage.SOME_SKY ) {
            getFootprintIcon( footprint_, h ).paintIcon( this, g2, x, y );
        }
        else {
            assert coverage == Footprint.Coverage.NO_DATA || coverage == null;
            Color color = g2.getColor();
            g2.setColor( getBackground() );
            g2.draw( orb );
            g2.setColor( color );
        }
        StringBuffer sbuf = new StringBuffer( "Sky footprint" );
        if ( subject_ != null ) {
            sbuf.append( " for " )
                .append( subject_ );
        }
        sbuf.append( ": " )
            .append( coverage );
        setToolTipText( sbuf.toString() );
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

    /**
     * Returns an icon at a given size for a given footprint object.
     * Some caching (the most recently-used icon) is performed.
     *
     * @param  footprint  footprint to represent
     * @param  height   height in pixels of icon
     */
    private synchronized Icon getFootprintIcon( Footprint footprint,
                                                int height ) {
        Object iconKey =
            Arrays.asList( new Object[] { footprint, new Integer( height ) } );
        if ( ! iconKey.equals( iconKey_ ) ) {
            iconKey_ = iconKey;
            fpIcon_ = new MollweideIcon( footprint, height );
        }
        return fpIcon_;
    }

    /**
     * Icon displaying footprint coverage by colouring in covered pixels
     * on a Mollweide projection of the sky.
     * Component foreground and background colours are used.
     */
    private static class MollweideIcon implements Icon {
        
        private final Footprint footprint_;
        private final int nx_;
        private final int ny_;
        private final BitSet mask_;
        private static final double SPHERE_AREA_DEG = 360 * 360 / Math.PI;

        /** 
         * Constructor.
         *
         * @param  footprint  footprint
         * @param  height  icon height in pixels (width will be 2*height)
         */
        MollweideIcon( Footprint footprint, int height ) {
            footprint_ = footprint;

            nx_ = height * 2;
            ny_ = height;
            mask_ = new BitSet( nx_ * ny_ );

            /* Work out the equivalent radius in degrees of one screen pixel
             * (i.e. the radius of a disc which has the same area as a
             * screen pixel).  This makes sense because Mollweide is an
             * equal-area projection. */
            int npix = (int) Math.round( nx_ * ny_ * Math.PI * 0.25 );
            double pixArea = SPHERE_AREA_DEG / npix;
            double radiusDeg = Math.sqrt( pixArea / Math.PI );

            /* Set up a pixel mask that can be drawn (fast) later.
             * Ellipse coordinates on screen look like:
             *                       (0,+90)
             *   (180,0)    (90,0)   (0,0)     (-90,0)  (-180,0)
             *                       (0,-90)  
             * i.e. both directions are negative of screen directions,
             * Y because screen directions increase towards the bottom, and
             * X because astronomers like it that way (as per CDS portal
             * footprint display). */
            for ( int iy = 0; iy < ny_; iy++ ) {
                double y = ( ( 2.0 * iy / ny_ ) - 1 ) * - Math.sqrt( 2 );
                double theta = Math.asin( y * Math.sqrt( 0.5 ) );
                double phi = Math.asin( ( 2 * theta + Math.sin( 2 * theta ) )
                                        / Math.PI );
                for ( int ix = 0; ix < nx_; ix++ ) {
                    double x = ( ( 2.0 * ix / nx_ ) - 1 )
                             * - Math.sqrt( 2 ) * 2;
                    double lambda = Math.PI * x
                                  / ( 2 * Math.sqrt( 2 ) * Math.cos( theta ) );
                    if ( lambda >= -Math.PI && lambda <= +Math.PI ) {
                        double alphaDeg = lambda / Math.PI * 180;
                        double deltaDeg = phi / Math.PI * 180;
                        if ( footprint.discOverlaps( alphaDeg, deltaDeg,
                                                     radiusDeg ) ) {
                             mask_.set( pixIndex( ix, iy ) );
                        }
                    }
                }
            }
        }

        public int getIconHeight() {
            return ny_;
        }

        public int getIconWidth() {
            return nx_;
        }

        public void paintIcon( Component c, Graphics g, int ox, int oy ) {
            Color color = g.getColor();
            g.setColor( c.getBackground() );
            g.fillOval( ox, oy, nx_, ny_ );
            g.setColor( color );
            for ( int ix = 0; ix < nx_; ix++ ) {
                for ( int iy = 0; iy < ny_; iy++ ) {
                    if ( mask_.get( pixIndex( ix, iy ) ) ) {
                        g.fillRect( ox + ix, oy + iy, 1, 1 );
                    }
                }
            }
        }

        /**
         * Maps (x,y)->index for a vector representation of the icon rectangle.
         *
         * @param  ix  X position
         * @param  iy  Y position
         * @return   offset into pixel array
         */
        private final int pixIndex( int ix, int iy ) {
            return ix * ny_ + iy;
        }
    }

}
