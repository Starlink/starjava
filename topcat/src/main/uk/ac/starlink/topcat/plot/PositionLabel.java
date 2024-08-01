package uk.ac.starlink.topcat.plot;

import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import uk.ac.starlink.ttools.plot.PlotSurface;

/**
 * Component whose text gives the current position of the mouse on a
 * given plot surface.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2005
 */
public class PositionLabel extends JLabel {

    /**
     * Constructs a new PositionLabel which reports on a given surface.
     *
     * @param  surface  plot surface to report on
     */
    @SuppressWarnings("this-escape")
    public PositionLabel( PlotSurface surface ) {

        /* Make some cosmetic changes.  A monospaced font is suitable so
         * that the length of the text doesn't keep changing, which would
         * be visually distracting. */
        setFont( new Font( "Monospaced", getFont().getStyle(), 
                           getFont().getSize() ) );
        setBorder( BorderFactory.createEtchedBorder() );

        /* Ensure that mouse changes are reflected in the text. */
        surface.getComponent()
               .addMouseMotionListener( new PositionReporter( surface ) {
            protected void reportPosition( String[] coords ) {
                PositionLabel.this.reportPosition( coords );
            }
        } );

        /* Intitialise. */
        reportPosition( null );
    }

    /**
     * Invoked when the position has changed.
     *
     * @param  coords  2-element array of formatted coordinate values -
     *                 null if the mouse is not over the surface
     */
    private void reportPosition( String[] coords ) {
        StringBuffer sbuf = new StringBuffer( " Position:" );
        if ( coords != null ) {
            sbuf.append( '(' )
                .append( coords[ 0 ] )
                .append( ", " )
                .append( coords[ 1 ] )
                .append( ')' );
        }
        setText( sbuf.toString() );
    }
}
