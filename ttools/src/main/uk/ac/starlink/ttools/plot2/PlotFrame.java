package uk.ac.starlink.ttools.plot2;

import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Arrays;

/**
 * Defines the geometry of a screen area to contain plot content.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2023
 */
public interface PlotFrame {

    /**
     * Returns the area within which actual plot content will reside.
     * This excludes space for external decorations.
     *
     * @return  internal bounds rectangle
     */
    Rectangle getInternalBounds();

    /**
     * Returns the space required for external decorations.
     *
     * @return  decoration surround space
     */
    Surround getSurround();

    /**
     * Returns the captioner to be used for external annotation.
     *
     * @return  captioner
     */
    Captioner getCaptioner();

    /**
     * Adapts a Surface to a PlotFrame.
     * The internal bounds contain the actual plotting area,
     * and the surround includes space for axis labels.
     *
     * @param   surf   plotting surface
     * @param   withScroll  true if the decorations should work well
     *                      with future scrolling
     * @return  PlotFrame view of surface
     */
    public static PlotFrame createPlotFrame( Surface surf, boolean withScroll ){
        final Surround surround = surf.getSurround( withScroll );
        final Rectangle intBounds = surf.getPlotBounds();
        final Captioner captioner = surf.getCaptioner();
        return new PlotFrame() {
            public Rectangle getInternalBounds() {
                return intBounds;
            }
            public Surround getSurround() {
                return surround;
            }
            public Captioner getCaptioner() {
                return captioner;
            }
        };
    }

    /**
     * Creates a PlotFrame from an array of surfaces.
     * The bounds are created from the plot bounds of the gang members,
     * unless there are none, in which case the supplied fallback bounds
     * rectangle is used.
     *
     * @param  surfs  plot surfaces
     * @param   withScroll  true if the decorations should work well
     *                      with future scrolling
     * @param  dfltBounds   fallback bounds; only used if no surfaces
     *                      are present
     */
    public static PlotFrame createPlotFrame( Surface[] surfs,
                                             boolean withScroll,
                                             Rectangle dfltBounds ) {
        int ns = surfs.length;
        final Rectangle intBounds;
        final Surround surround;
        final Captioner captioner;
        if ( ns > 0 ) {
            Rectangle[] plotBounds =
                Arrays.stream( surfs )
                      .map( Surface::getPlotBounds )
                      .toArray( n -> new Rectangle[ n ] );
            intBounds = new Rectangle( plotBounds[ 0 ] );
            for ( int is = 1; is < ns; is++ ) {
                intBounds.add( plotBounds[ is ] );
            }
            surround = new Surround();
            for ( int is = 0; is < ns; is++ ) {
                Surface surf = surfs[ is ];
                Rectangle box = surf.getPlotBounds();
                Surround surr = surf.getSurround( withScroll );
                Insets insets = surr.toInsets();

                /* These calculations aren't perfect, in that they may not
                 * deal with overhangs defined by the Surround in all cases,
                 * but they should do a fairly good job. */
                if ( box.x - insets.left < intBounds.x ) {
                    surround.left = surround.left.union( surr.left );
                }
                if ( box.y - insets.top < intBounds.y ) {
                    surround.top = surround.top.union( surr.top );
                }
                if ( box.x + box.width + insets.right >
                     intBounds.x + intBounds.width ) {
                    surround.right = surround.right.union( surr.right );
                }
                if ( box.y + box.height + insets.bottom >
                     intBounds.y + intBounds.height ) { 
                    surround.bottom = surround.bottom.union( surr.bottom );
                }
            }
            captioner = surfs[ 0 ].getCaptioner();
        }
        else {
            intBounds = new Rectangle( dfltBounds );
            surround = new Surround();
            captioner = NullCaptioner.INSTANCE;
        }
        return new PlotFrame() {
            public Rectangle getInternalBounds() {
                return intBounds;
            }
            public Surround getSurround() {
                return surround;
            }
            public Captioner getCaptioner() {
                return captioner;
            }
        };
    }
}
