package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
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
    private final ToggleButtonModel tangentErrorToggler_;
    private final ErrorModeSelectionModel radialErrorModeModel_;
    private final ErrorModeSelectionModel[] tangentErrorModeModels_;

    private final static ErrorRenderer[] ERROR_RENDERERS =
        ErrorRenderer.getOptionsSpherical();

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for positioning)
     */
    public SphereWindow( Component parent ) {
        super( "Spherical Plot",
               new String[] { "Longitude", "Latitude", "Radius" }, parent, 
               new ErrorModeSelectionModel[ 0 ], new SphericalPlot3D() );

        /* Set up toggle button model for logarithmic radial axis. */
        logToggler_ =
            new ToggleButtonModel( "Log", ResourceIcon.XLOG,
                                   "Scale radius value logarithmically" );
        logToggler_.addActionListener( getReplotListener() );


        /* Set up toggle button model for tangential errors. */
        tangentErrorToggler_ = 
            new ToggleButtonModel( "Tangent errors", createTangentErrorIcon(),
                                   "Draw tangential error regions" );
        tangentErrorToggler_.addActionListener( getReplotListener() );

        /* Set error mode selection models for tangential errors.
         * These are only used for reading by components which need them -
         * they are slaves of the toggle button model which is what is
         * used for user interaction. */
        tangentErrorModeModels_ = new ErrorModeSelectionModel[] {
            new ErrorModeSelectionModel( 0, "Longitude" ),
            new ErrorModeSelectionModel( 1, "Latitude" ),
        };
        tangentErrorToggler_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                boolean hasTan = tangentErrorToggler_.isSelected();
                ErrorMode mode = hasTan ? ErrorMode.SYMMETRIC
                                        : ErrorMode.NONE;
                if ( tangentErrorModeModels_[ 0 ].getMode() != mode ) {
                    assert tangentErrorModeModels_[ 1 ].getMode() != mode;
                    tangentErrorModeModels_[ 0 ].setMode( mode );
                    tangentErrorModeModels_[ 1 ].setMode( mode );
                }
            }
        } );

        /* Set up error mode selection model for radial errors. */
        radialErrorModeModel_ = new ErrorModeSelectionModel( 2, "Radial" );
        radialErrorModeModel_.addActionListener( getReplotListener() );

        /* Add toolbar buttons. */
        getToolBar().add( tangentErrorToggler_.createToolbarButton() );
        getToolBar().add( radialErrorModeModel_.createOnOffToolbarButton() );
        getToolBar().addSeparator();
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
        return new SphericalPolarPointSelector( getStyles(), logToggler_,
                                                tangentErrorToggler_,
                                                radialErrorModeModel_ );
    }

    protected StyleEditor createStyleEditor() {
        return new MarkStyleEditor( false, true, ERROR_RENDERERS,
                                    new ErrorModeSelectionModel[] {
                                        tangentErrorModeModels_[ 0 ],
                                        tangentErrorModeModels_[ 1 ],
                                        radialErrorModeModel_,
                                    } );
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
            double r2max = 0.0;
            for ( int ip = 0; ip < npoint; ip++ ) {
                long lp = (long) ip;
                double[] coords = points.getPoint( ip );
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

    /**
     * Returns an icon for the button which toggles whether tangential errors
     * will be drawn.
     *
     * @return   error icon
     */
    private static Icon createTangentErrorIcon() {
        ErrorMode[] modes = new ErrorMode[] {
            ErrorMode.SYMMETRIC, ErrorMode.SYMMETRIC,
        };
        final Icon icon =
            ErrorRenderer.TANGENT.getLegendIcon( modes, 24, 24, 1, 4 );
        return new Icon() {
            public int getIconHeight() {
                return icon.getIconHeight();
            }
            public int getIconWidth() {
                return icon.getIconWidth();
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                Color oldColor = g.getColor();
                g.setColor( Color.BLACK );
                icon.paintIcon( c, g, x, y );
                g.setColor( Color.WHITE );
                int radius = 2;
                g.drawOval( x + getIconWidth() / 2 - radius,
                            y + getIconHeight() / 2 - radius,
                            radius * 2, radius * 2 );
                g.setColor( oldColor );
            }
        };
    }
}
