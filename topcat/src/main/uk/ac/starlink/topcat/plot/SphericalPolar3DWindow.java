package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;

/**
 * Graphics window for viewing 3D scatter plots using spherical polar
 * coordinates.
 *
 * @author   Mark Taylor
 * @since    23 Dec 2005
 */
public class SphericalPolar3DWindow extends Plot3DWindow {

    private final ToggleButtonModel logToggler_;

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for positioning)
     */
    public SphericalPolar3DWindow( Component parent ) {
        super( "Spherical Polar",
               new String[] { "Longitude", "Latitude", "Radius" }, parent );
        logToggler_ =
            new ToggleButtonModel( "Log", ResourceIcon.XLOG,
                                   "Scale radius value logarithmically" );
        logToggler_.addActionListener( getReplotListener() );
        addHelp( "SphericalPolar3DWindow" );
    }

    protected PlotState createPlotState() {
        SphericalPlotState state = new SphericalPlotState();
        ValueInfo rInfo =
            ((SphericalPolarPointSelector) getPointSelectors()
                                          .getMainSelector()).getRadialInfo();
        state.setRadialInfo( rInfo );
        if ( rInfo != null ) {
            state.setRadialLog( logToggler_.isSelected() );
        }
        return state;
    }

    protected PointSelector createPointSelector() {
        return new SphericalPolarPointSelector( createPooledStyleSet(),
                                                logToggler_ );
    }
}
