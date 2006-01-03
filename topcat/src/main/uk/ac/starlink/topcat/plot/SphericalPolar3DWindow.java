package uk.ac.starlink.topcat.plot;

import java.awt.Component;

/**
 * Graphics window for viewing 3D scatter plots using spherical polar
 * coordinates.
 *
 * @author   Mark Taylor
 * @since    23 Dec 2005
 */
public class SphericalPolar3DWindow extends Plot3DWindow {

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for positioning)
     */
    public SphericalPolar3DWindow( Component parent ) {
        super( "Sky", new String[] { "Longitude", "Latitude", "Radius" },
               parent );
        addHelp( "SphericalPolar3DWindow" );
    }

    protected PlotState createPlotState() {
        Plot3DState state = (Plot3DState) super.createPlotState();
        state.setSpherical( true );
        return state;
    }

    protected PointSelector createPointSelector() {
        return new SphericalPolarPointSelector( createPooledStyleSet() );
    }
}
