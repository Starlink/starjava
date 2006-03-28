package uk.ac.starlink.topcat.plot;

import java.awt.Component;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.ToggleButtonModel;

/**
 * Graphics window for viewing 3D scatter plots using spherical polar
 * coordinates.
 *
 * @author   Mark Taylor
 * @since    23 Dec 2005
 */
public class SphereWindow extends Plot3DWindow {

    private final ToggleButtonModel logToggler_;

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for positioning)
     */
    public SphereWindow( Component parent ) {
        super( "Spherical Plot",
               new String[] { "Longitude", "Latitude", "Radius" }, parent );
        logToggler_ =
            new ToggleButtonModel( "Log", ResourceIcon.XLOG,
                                   "Scale radius value logarithmically" );
        logToggler_.addActionListener( getReplotListener() );
        addHelp( "SphereWindow" );
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
        return new SphericalPolarPointSelector( logToggler_ );
    }

    /**
     * Returns a single range, corresponding to the radial axis.
     * Ranges on the other axes aren't much use.
     * The radial one will just be between [0..1] if no radial coordinate
     * has been chosen.
     */
    public Range[] calculateRanges( PointSelection pointSelection,
                                    Points points ) {
        PointSelectorSet pointSelectors = getPointSelectors();
        boolean hasRadial = false;
        for ( int i = 0; i < pointSelectors.getSelectorCount(); i++ ) {
            SphericalPolarPointSelector psel =
                (SphericalPolarPointSelector) pointSelectors.getSelector( i );
            hasRadial = hasRadial || ( psel.getRadialInfo() != null );
        }
        if ( hasRadial ) {
            RowSubset[] sets = pointSelection.getSubsets();
            int nset = sets.length;
            int npoint = points.getCount();
            double[] coords = new double[ 3 ];
            double r2max = 0.0;
            for ( int ip = 0; ip < npoint; ip++ ) {
                long lp = (long) ip;
                points.getCoords( ip, coords );
                boolean isUsed = false;
                for ( int is = 0; is < nset && ! isUsed; is++ ) {
                    isUsed = isUsed || sets[ is ].isIncluded( lp );
                }
                if ( isUsed ) {
                    double r2 = coords[ 0 ] * coords[ 0 ]
                              + coords[ 1 ] * coords[ 1 ]
                              + coords[ 2 ] * coords[ 2 ];
                    if ( r2 > r2max && ! Double.isInfinite( r2 ) ) {
                        r2max = r2;
                    }
                }
            }
            double rmax = r2max > 0.0 ? Math.sqrt( r2max ) : 1.0;
            return new Range[] { new Range( 0.0, rmax ) };
        }
        else {
            return new Range[] { new Range( 0.0, 1.0 ) };
        }
    }
}
