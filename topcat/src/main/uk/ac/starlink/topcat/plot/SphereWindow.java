package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
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
    private final ErrorModeSelectionModel[] errorModeModels3d_;

    private final static ErrorRenderer[] ERROR_RENDERERS =
        ErrorRenderer.getOptionsSpherical();

    /**
     * Constructs a new window.
     *
     * @param   parent  parent component (may be used for positioning)
     */
    public SphereWindow( Component parent ) {
        super( "Spherical Plot",
               new String[] { "Longitude", "Latitude", "Radius" }, 3, parent, 
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

        /* Set up a 3-element array of the error mode selection models - 
         * this is a convenience item for use when constructing error 
         * renderer icons for legends. */
        errorModeModels3d_ = new ErrorModeSelectionModel[] {
            tangentErrorModeModels_[ 0 ],
            tangentErrorModeModels_[ 1 ],
            radialErrorModeModel_,
        };

        /* Error mode menu. */
        JMenu errorMenu = new JMenu( "Errors" );
        JMenuItem[] radialItems = radialErrorModeModel_.createMenuItems();
        for ( int i = 0; i < radialItems.length; i++ ) {
            errorMenu.add( radialItems[ i ] );
        }
        errorMenu.addSeparator();
        errorMenu.add( tangentErrorToggler_.createMenuItem() );
        getJMenuBar().add( errorMenu );

        /* Customise stay north action. */
        getNorthModel().setDescription( "Select whether North pole is "
                                      + "always vertical on the screen" );
        getNorthModel().setSelected( true );

        /* Style Menu. */
        getJMenuBar().add( createMarkerStyleMenu( PlotWindow.STYLE_SETS ) );
        getJMenuBar().add( createErrorRendererMenu( ERROR_RENDERERS ) );

        /* Add toolbar buttons. */
        getToolBar().add( tangentErrorToggler_.createToolbarButton() );
        getToolBar().add( radialErrorModeModel_.createOnOffToolbarButton() );
        getToolBar().addSeparator();
        addHelp( "SphereWindow" );
    }

    protected PlotState createPlotState() {
        SphericalPlotState state = new SphericalPlotState();
        ValueInfo rInfo =
            getSphericalAxesSelector( getPointSelectors().getMainSelector() )
           .getRadialInfo();
        state.setRadialInfo( rInfo );
        if ( rInfo != null ) {
            state.setRadialLog( logToggler_.isSelected() );
        }
        return state;
    }

    public PlotState getPlotState() {
        PlotState state = super.getPlotState();
        int mainNdim = 1;
        state.setMainNdim( mainNdim );

        /* Modify ranges.  This is required because of confusion about what
         * axes mean between GraphicsWindow and SphereWindow. */
        if ( state.getValid() ) {
            double[][] bounds = state.getRanges();
            int naux = state.getShaders().length;
            Range[] viewRanges = getViewRanges();
            Range[] dataRanges = getDataRanges();
            boolean[] logFlags = state.getLogFlags();
            for ( int i = 0; i < naux; i++ ) {
                Range range = new Range( dataRanges[ mainNdim + i ] );
                range.limit( viewRanges[ mainNdim + i ] );
                bounds[ mainNdim + i ] = 
                    range.getFiniteBounds( logFlags[ 3 + i ] );
            }
        }
        return state;
    }

    protected PointSelector createPointSelector() {
        AxesSelector axsel =
            new SphericalAxesSelector( logToggler_, tangentErrorToggler_,
                                       radialErrorModeModel_ );
        axsel = addAuxAxes( axsel );
        PointSelector psel = new PointSelector( axsel, getStyles() );
        ActionListener errorModeListener = psel.getErrorModeListener();
        tangentErrorToggler_.addActionListener( errorModeListener );
        radialErrorModeModel_.addActionListener( errorModeListener );
        return psel;
    }

    protected StyleEditor createStyleEditor() {
        return new MarkStyleEditor( false, true, ERROR_RENDERERS,
                                    errorModeModels3d_ );
    }

    public ErrorModeSelectionModel[] getErrorModeModels() {
        return errorModeModels3d_;
    }

    /**
     * Returns a single range for the main axes, corresponding to the 
     * radial axis.
     * Ranges on the other main axes aren't much use.
     * The radial one will just be between [0..1] if no radial coordinate
     * has been chosen.
     * Ranges for any currently visible auxiliary axes are appended
     * to the array.
     */
    public Range[] calculateRanges( PointSelection pointSelection,
                                    Points points ) {
        PointSelectorSet pointSelectors = getPointSelectors();

        /* Work out the radial range. */
        Range radialRange;
        boolean hasRadial = false;
        for ( int i = 0; i < pointSelectors.getSelectorCount(); i++ ) {
            ValueInfo rInfo =
                getSphericalAxesSelector( pointSelectors.getSelector( i ) )
               .getRadialInfo();
            hasRadial = hasRadial || ( rInfo != null );
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
            radialRange = new Range( 0.0, rmax );
        }
        else {
            radialRange = new Range( 0.0, 1.0 );
        }

        /* Work out any auxiliary ranges. */
        Range[] auxRanges;
        int nVis = getVisibleAuxAxisCount();
        if ( nVis > 0 ) {
            Range[] allRanges = super.calculateRanges( pointSelection, points );
            assert allRanges.length == 3 + nVis;
            auxRanges = new Range[ nVis ];
            System.arraycopy( allRanges, 3, auxRanges, 0, nVis );
        }
        else {
            auxRanges = new Range[ 0 ];
        }

        /* Put them together. */
        Range[] ranges = new Range[ 1 + auxRanges.length ];
        ranges[ 0 ] = radialRange;
        System.arraycopy( auxRanges, 0, ranges, 1, auxRanges.length );
        return ranges;
    }

    /**
     * Returns the SphericalAxesSelector associated with a given PointSelector.
     *
     * @param  psel   point selector
     * @return   spherical axes selector
     */
    private static SphericalAxesSelector
                   getSphericalAxesSelector( PointSelector psel ) {
        AxesSelector axsel = psel.getAxesSelector();
        if ( axsel instanceof SphericalAxesSelector ) {
            return (SphericalAxesSelector) axsel;
        }
        else if ( axsel instanceof AugmentedAxesSelector ) {
            return (SphericalAxesSelector)
                   ((AugmentedAxesSelector) axsel).getBaseSelector();
        }
        else {
            throw new AssertionError();
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
