package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.SphericalPlot3D;
import uk.ac.starlink.ttools.plot.SphericalPlotState;
import uk.ac.starlink.util.WrapUtils;

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
    private final ToggleButtonModel radialToggler_;
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
    @SuppressWarnings("this-escape")
    public SphereWindow( Component parent ) {
        super( "Spherical Plot (old)",
               new String[] { "Longitude", "Latitude", "Radius" }, 3, parent, 
               new ErrorModeSelectionModel[ 0 ], createPlot() );

        /* Set up toggle button model for whether the radial axis is used. */
        radialToggler_ =
            new ToggleButtonModel( "Radial Coordinates",
                                   ResourceIcon.RADIAL,
                                   "Plot points with radial "
                                 + "as well as angular coordinates" );
        radialToggler_.addActionListener( getReplotListener() );

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
                if ( tangentErrorModeModels_[ 0 ].getErrorMode() != mode ) {
                    assert tangentErrorModeModels_[ 1 ].getErrorMode() != mode;
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
        getJMenuBar()
            .add( createMarkerStyleMenu( getStandardMarkStyleSets() ) );
        getJMenuBar().add( createErrorRendererMenu( ERROR_RENDERERS ) );

        /* Add toolbar buttons. */
        getPointSelectorToolBar().addSeparator();
        getPointSelectorToolBar().add( radialToggler_
                                      .createToolbarButton() );
        getPointSelectorToolBar().addSeparator();
        getPointSelectorToolBar().add( tangentErrorToggler_
                                      .createToolbarButton() );
        getPointSelectorToolBar().add( radialErrorModeModel_
                                      .createOnOffToolbarButton() );
        getToolBar().addSeparator();
        addHelp( "SphereWindow" );
    }

    public int getMainRangeCount() {
        return 1;
    }

    protected PlotState createPlotState() {
        SphericalPlotState state = new SphericalPlotState() {
            public PlotData getPlotData() {
                PlotData data = super.getPlotData();
                adjustPlotData( this, data );
                return data;
            }
        };
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

        /* Modify ranges.  This is required because of confusion about what
         * axes mean between GraphicsWindow and SphereWindow. */
        if ( state.getValid() ) {
            double[][] bounds = state.getRanges();
            int naux = state.getShaders().length;
            Range[] viewRanges = getViewRanges();
            Range[] dataRanges = getDataRanges();
            boolean[] logFlags = state.getLogFlags();
            boolean[] flipFlags = state.getFlipFlags();
            int mainNdim = getMainRangeCount();
            for ( int i = 0; i < naux; i++ ) {
                logFlags[ mainNdim + i ] = logFlags[ 3 + i ];
                flipFlags[ mainNdim + i ] = flipFlags[ 3 + i ];
                Range range = new Range( dataRanges[ mainNdim + i ] );
                range.limit( viewRanges[ mainNdim + i ] );
                bounds[ mainNdim + i ] = 
                    range.getFiniteBounds( logFlags[ mainNdim + i ] );
            }
        }
        return state;
    }

    protected PointSelector createPointSelector() {
        final SphericalAxesSelector sphaxsel =
            new SphericalAxesSelector( logToggler_, tangentErrorToggler_,
                                       radialErrorModeModel_ );
        radialToggler_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                sphaxsel.setRadialVisible( radialToggler_.isSelected() );
            }
        } );
        sphaxsel.setRadialVisible( radialToggler_.isSelected() );
        AxesSelector axsel = addExtraAxes( sphaxsel );
        PointSelector psel = new PointSelector( axsel, getStyles() );
        ActionListener errorModeListener = psel.getErrorModeListener();
        tangentErrorToggler_.addActionListener( errorModeListener );
        radialErrorModeModel_.addActionListener( errorModeListener );
        return psel;
    }

    protected StyleEditor createStyleEditor() {
        return new MarkStyleEditor( false, true, ERROR_RENDERERS,
                                    ErrorRenderer.DEFAULT,
                                    errorModeModels3d_ );
    }

    public ErrorModeSelectionModel[] getErrorModeModels() {
        return errorModeModels3d_;
    }

    /**
     * Returns the SphericalAxesSelector associated with a given PointSelector.
     *
     * @param  psel   point selector
     * @return   spherical axes selector
     */
    private static SphericalAxesSelector
                   getSphericalAxesSelector( PointSelector psel ) {
        return (SphericalAxesSelector)
               WrapUtils.getWrapped( psel.getAxesSelector() );
    }

    /**
     * Returns an icon for the button which toggles whether tangential errors
     * will be drawn.
     *
     * @return   error icon
     */
    public static Icon createTangentErrorIcon() {
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

    /**
     * Generates a plot to be used with this window.
     *
     * @return  3D plot
     */
    private static SphericalPlot3D createPlot() {
        return new SphericalPlot3D() {
            protected boolean paintMemoryError( OutOfMemoryError e ) {
                TopcatUtils.memoryErrorLater( e );
                return true;
            }
        };
    }

    /**
     * Modifies a PlotData object used by this window.
     * This is an optimisation to do with current pixel size.
     *
     * @param  state  plot state
     * @param  data   plot data object - may be modified
     */
    private void adjustPlotData( SphericalPlotState state, PlotData data ) {
        if ( data instanceof PointSelection ) {
            Points points = ((PointSelection) data).getPoints();
            if ( points != null ) {
                Object basePoints = WrapUtils.getWrapped( points );
                if ( basePoints instanceof SphericalPolarPointStore ) {

                    /* If the data is a spherical polar point store, tell it
                     * roughly what the display pixel size is for this plot.
                     * This enables skipping some expensive calculaations of
                     * error points where it is known that they will end up in
                     * the same pixel as the error point. */
                    SphericalPolarPointStore sphPoints =
                        (SphericalPolarPointStore) basePoints;
                    Dimension size = getPlot().getSize();
                    int scale = Math.max( size.width, size.height );

                    /* This is a rough estimate, but it should be an
                     * underestimate, which is safe though perhaps not
                     * maximally efficient. */
                    double minTanErr = 1.0 / ( scale * state.getZoomScale() );
                    sphPoints.setMinimumTanError( minTanErr );
                }
            }
        }
    }
}
