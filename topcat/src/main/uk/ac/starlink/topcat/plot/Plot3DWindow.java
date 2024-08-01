package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Matrices;
import uk.ac.starlink.ttools.plot.Plot3D;
import uk.ac.starlink.ttools.plot.Plot3DState;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PointIterator;
import uk.ac.starlink.ttools.plot.PointPlacer;
import uk.ac.starlink.ttools.plot.StyleSet;
import uk.ac.starlink.ttools.plot.TablePlot;

/**
 * Graphics window for viewing 3D scatter plots.
 *
 * @author   Mark Taylor
 * @since    22 Nov 2005
 */
public abstract class Plot3DWindow extends GraphicsWindow
                                   implements TopcatListener {

    private final JComponent plotPanel_;
    private final ToggleButtonModel fogModel_;
    private final ToggleButtonModel antialiasModel_;
    private final ToggleButtonModel northModel_;
    private final BlobPanel blobPanel_;
    private final AnnotationPanel annotations_;
    private final Action blobAction_;
    private final Action fromVisibleAction_;
    private double[] rotation_;
    private boolean isRotating_;
    private double zoom_ = 1.0;

    private static final StyleSet[] STYLE_SETS = getStandardMarkStyleSets();
    private static final StyleSet MARKERS1 = STYLE_SETS[ 0 ];
    private static final StyleSet MARKERS2 = STYLE_SETS[ 1 ];
    private static final StyleSet MARKERS3 = STYLE_SETS[ 2 ];
    private static final StyleSet MARKERS4 = STYLE_SETS[ 3 ];
    private static final StyleSet MARKERS5 = STYLE_SETS[ 4 ];
    static {
        assert MARKERS1.getName().equals( "Pixels" );
        assert MARKERS2.getName().equals( "Dots" );
        assert MARKERS3.getName().equals( "Spots" );
        assert MARKERS4.getName().startsWith( "Small" );
        assert MARKERS5.getName().startsWith( "Medium" );
    }

    private static final double[] INITIAL_ROTATION = 
        Plot3D.rotateXY( Plot3D.rotateXY( new double[] { 1, 0, 0,
                                                         0, 1, 0,
                                                         0, 0, -1 },
                                          0.5, 0.5 * Math.PI ),
                         0, -0.1 * Math.PI );
    private static final double CLICK_ZOOM_UNIT = 1.2;
    private static final boolean CAN_ZOOM = true;

    /**
     * Constructor.
     *
     * @param   viewName  name of the view window
     * @param   axisNames  array of labels by which each axis is known;
     *          the length of this array defines the dimensionality of the plot
     * @param   naux  number of auxiliary axes
     * @param   parent   parent window - may be used for positioning
     * @param   errorModeModels   array of selecction models for error modes
     * @param   plot   the Plot3D object on which plotting is done
     */
    @SuppressWarnings("this-escape")
    public Plot3DWindow( String viewName, String[] axisNames, int naux,
                         Component parent,
                         ErrorModeSelectionModel[] errorModeModels,
                         final Plot3D plot ) {
        super( viewName, plot, axisNames, naux, true, errorModeModels, parent );

        /* Set a suitable border on the plot.  The left part of this is
         * where the central zoom target live.  There's a zoom target
         * on the right as well (since this is where it was in 
         * previous versions) but it's very thin - this is mainly padding
         * between the plot and the legend region.  The top and bottom 
         * parts sometimes serve as overspills for auxiliary axis text. */
        plot.setBorder( BorderFactory.createEmptyBorder( 10, 32, 10, 10 ) );

        /* Zooming. */
        Zoomer zoomer = new Zoomer();
        zoomer.setRegions( Arrays.asList( createZoomRegions() ) );
        zoomer.setCursorComponent( plot );
        plot.addMouseListener( zoomer );
        plot.addMouseMotionListener( zoomer );
        plot.addMouseWheelListener( new MouseWheelListener() {
            public void mouseWheelMoved( MouseWheelEvent evt ) {
                int nclick = evt.getWheelRotation();
                if ( nclick != 0 ) {
                    double factor = Math.pow( CLICK_ZOOM_UNIT, -nclick );
                    doZoom( ((Plot3DState) plot.getState()).getZoomScale()
                            * factor );
                }
            }
        } );

        /* Annotations panel. */
        annotations_ = new AnnotationPanel() {
            public PointPlacer getPlacer() {
                return plot.getPointPlacer();
            }
        };

        /* Construct and populate the plot panel with the 3D plot itself
         * and a transparent layer for doodling blobs on. */
        plotPanel_ = new JPanel();
        plotPanel_.setOpaque( false );
        blobPanel_ = new BlobPanel() {
            protected void blobCompleted( Shape blob ) {
                addNewSubsets( plot.getPlottedPointIterator()
                                   .getContainedPoints( blob ) );
            }
        };
        blobAction_ = blobPanel_.getBlobAction();

        /* Overlay components of display. */
        plotPanel_.setLayout( new OverlayLayout( plotPanel_ ) );
        plotPanel_.add( blobPanel_ );
        plotPanel_.add( annotations_ );
        plotPanel_.add( plot );

        /* Listen for topcat actions. */
        getPointSelectors().addTopcatListener( this );

        /* Arrange that mouse dragging on the plot component will rotate
         * the view. */
        DragListener rotListener = new DragListener();
        plot.addMouseMotionListener( rotListener );
        plot.addMouseListener( rotListener );

        /* Arrange that clicking on a point will activate it. */
        plot.addMouseListener( new PointClickListener() );

        /* Add a status line. */
        PlotStatsLabel plotStatus = new PlotStatsLabel();
        plotStatus.setMaximumSize( new Dimension( Integer.MAX_VALUE,
                                                  plotStatus
                                                 .getMaximumSize().height ) );
        plot.addPlotListener( plotStatus );
        getStatusBox().add( plotStatus );

        /* Action for reorienting the plot. */
        Action reorientAction = new BasicAction( "Reorient", ResourceIcon.XYZ,
                                                 "Reorient the plot to initial"
                                               + " position" ) {
            public void actionPerformed( ActionEvent evt ) {
                setRotation( INITIAL_ROTATION );
                zoom_ = 1.0;
                replot();
            }
        };

        /* Action for selecting subset from visible points. */
        fromVisibleAction_ = new BasicAction( "New subset from visible",
                                              ResourceIcon.VISIBLE_SUBSET,
                                              "Define a new row subset " +
                                              "containing only currently " +
                                              "visible points" ) {
            public void actionPerformed( ActionEvent evt ) {
                addNewSubsets( plot.getPlottedPointIterator().getAllPoints() );
            }
        };

        /* Model to toggle fogged rendering. */
        fogModel_ = new ToggleButtonModel( "Fog", ResourceIcon.FOG,
                                           "Select whether fog obscures " +
                                           "distant points" );
        fogModel_.setSelected( true );
        fogModel_.addActionListener( getReplotListener() );

        /* Model to toggle antialiasing. */
        antialiasModel_ = new ToggleButtonModel( "Antialias",
                                                 ResourceIcon.ANTIALIAS,
                                                 "Select whether text is " +
                                                 "antialiased" );
        antialiasModel_.setSelected( false );
        antialiasModel_.addActionListener( getReplotListener() );

        /* Model to keep the Y axis facing upwards. */
        northModel_ = new ToggleButtonModel( "Stay Upright",
                                             ResourceIcon.NORTH,
                                             "Select whether the Z axis is "
                                           + "always vertical on the screen" );
        northModel_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                setRotation( rotation_ );
                getReplotListener().actionPerformed( evt );
            }
        } );
        northModel_.setSelected( false );

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( getRescaleAction() );
        plotMenu.add( reorientAction );
        plotMenu.add( northModel_.createMenuItem() );
        plotMenu.add( getAxisEditAction() );
        plotMenu.add( getGridModel().createMenuItem() );
        plotMenu.add( getLegendModel().createMenuItem() );
        plotMenu.add( getReplotAction() );
        getJMenuBar().add( plotMenu );

        /* Construct a new menu for rendering options. */
        JMenu renderMenu = new JMenu( "Rendering" );
        renderMenu.setMnemonic( KeyEvent.VK_R );
        renderMenu.add( fogModel_.createMenuItem() );
        renderMenu.add( antialiasModel_.createMenuItem() );
        getJMenuBar().add( renderMenu );

        /* Construct a new menu for subset options. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        subsetMenu.add( blobAction_ );
        if ( CAN_ZOOM ) {
            subsetMenu.add( fromVisibleAction_ );
        }
        getJMenuBar().add( subsetMenu );

        /* Add actions to the toolbar. */
        getToolBar().add( getRescaleAction() );
        getToolBar().add( reorientAction );
        getToolBar().add( northModel_.createToolbarButton() );
        getToolBar().add( getGridModel().createToolbarButton() );
        getToolBar().add( getLegendModel().createToolbarButton() );
        getToolBar().add( fogModel_.createToolbarButton() );
        getToolBar().add( blobAction_ );
        if ( CAN_ZOOM ) {
            getToolBar().add( fromVisibleAction_ );
        }

        /* Set initial rotation. */
        setRotation( INITIAL_ROTATION );
        replot();
    }

    /**
     * Sets the viewing angle.
     *
     * @param   matrix  9-element array giving rotation of data space
     */
    public void setRotation( double[] matrix ) {
        double[] rot = matrix.clone();
        if ( northModel_.isSelected() ) {
            double theta = Math.atan2( rot[ 2 ], rot[ 5 ] );
            double[] correction = 
                Plot3D.rotate( rot, new double[] { 0., 0., 1., }, theta );
            rot = Matrices.mmMult( rot, correction );
        }
        rotation_ = rot;
    }

    protected JComponent getPlotPanel() {
        return plotPanel_;
    }

    protected PlotState createPlotState() {
        return new Plot3DState();
    }

    public PlotState getPlotState() {
        Plot3DState state = (Plot3DState) super.getPlotState();

        /* Configure the state with this window's current viewing angles
         * and zoom state. */
        state.setRotation( rotation_ );
        state.setRotating( isRotating_ );
        state.setZoomScale( zoom_ );

        /* Configure rendering options. */
        state.setFogginess( fogModel_.isSelected() ? 2.0 : 0.0 );
        state.setAntialias( antialiasModel_.isSelected() );

        /* Return. */
        return state;
    }

    public StyleSet getDefaultStyles( int npoint ) {
        if ( npoint > 20000 ) {
            return MARKERS1;
        }
        else if ( npoint > 2000 ) {
            return MARKERS2;
        }
        else if ( npoint > 200 ) {
            return MARKERS3;
        }
        else if ( npoint > 20 ) {
            return MARKERS4;
        }
        else if ( npoint >= 1 ) {
            return MARKERS5;
        }
        else {
            return MARKERS2;
        }
    }

    protected void doReplot( PlotState state ) {
        blobPanel_.setActive( false );
        annotations_.setPlotData( state.getPlotData() );
        super.doReplot( state );
    }

    /**
     * Returns the model which toggles whether the orientation of the plot
     * always points up on the screen.
     *
     * @return  keep north action
     */
    public ToggleButtonModel getNorthModel() {
        return northModel_;
    }

    /*
     * TopcatListener implementation.
     */
    public void modelChanged( TopcatEvent evt ) {
        if ( evt.getCode() == TopcatEvent.ROW ) {
            Object datum = evt.getDatum();
            if ( datum instanceof Long ) {
                Plot3D plot = (Plot3D) getPlot();
                TopcatModel tcModel = evt.getModel();
                PointSelection psel =
                    (PointSelection) plot.getState().getPlotData();
                long lrow = ((Long) datum).longValue();
                long[] lps = psel.getPointsForRow( tcModel, lrow );
                int[] ips = new int[ lps.length ];
                for ( int i = 0; i < lps.length; i++ ) {
                    ips[ i ] = Tables.checkedLongToInt( lps[ i ] );
                }
                annotations_.setActivePoints( ips );
            }
            else {
                assert false;
            }
        }
    }

    /**
     * Performs a zoom.
     * Zoom factors greater than unity will not be honoured.
     *
     * @param  zoom  requested absolute zoom factor
     */
    private void doZoom( double zoom ) {
        zoom_ = Math.max( 1.0, zoom );
        replot();
    }

    /**
     * Returns an array of zoom region objects for use on this window's plot.
     *
     * @return  zoom region array
     */
    private final ZoomRegion[] createZoomRegions() {
        return new ZoomRegion[] {

            /* Left hand side of plot. */
            new ZoomRegion3D( false ) {
                protected Rectangle getTarget( Rectangle display,
                                               Rectangle bounds ) {
                    int x = display.x + display.width;
                    int width = bounds.width - x;
                    int y = display.y;
                    int height = display.height;
                    return new Rectangle( x, y, width, height );
                }
            },

            /* Right hand side of plot. */
            new ZoomRegion3D( false ) {
                protected Rectangle getTarget( Rectangle display,
                                               Rectangle bounds ) {
                    return new Rectangle( 0, display.y,
                                          display.x, display.height );
                }
            },
        };
    }

    /**
     * Zoom Region abstract superclass which handles central zooming of
     * this plot.  Different concrete subclasses will have different
     * target regions.
     */
    private abstract class ZoomRegion3D extends CentreZoomRegion {

        /**
         * Constructor.
         *
         * @param   isX  true for horizontal target, false for vertical
         */
        ZoomRegion3D( boolean isX ) {
            super( isX );
        }

        /**
         * Defines the target region given the component and plot regions.
         *
         * @param   display  bounds of the plot region
         * @param   bounds   bounds of the entire component
         */
        protected abstract Rectangle getTarget( Rectangle display,
                                                Rectangle bounds );

        public Rectangle getDisplay() {
            return ((Plot3D) getPlot()).getDisplayBounds();
        }

        public Rectangle getTarget() {
            TablePlot plot = getPlot();
            return getTarget( plot.getPlotBounds(), plot.getBounds() );
        }

        public void zoomed( double[][] bounds ) {
            doZoom( ((Plot3DState) getPlot().getState()).getZoomScale()
                    / bounds[ 0 ][ 0 ] );
        }
    }

    /**
     * Listener which interprets drag gestures on the plotting surface 
     * as requests to rotate the viewing angles.
     */
    private class DragListener extends MouseAdapter
                               implements MouseMotionListener {

        private Point posBase_;
        private double[] rotBase_;
        private boolean relevant_;

        public void mousePressed( MouseEvent evt ) {
            relevant_ = getPlot().getPlotBounds().contains( evt.getPoint() );
        }

        public void mouseDragged( MouseEvent evt ) {
            if ( ! relevant_ ) {
                return;
            }
            isRotating_ = true;
            Point pos = evt.getPoint(); 
            if ( posBase_ == null ) {
                posBase_ = pos;
                rotBase_ = Plot3DWindow.this.rotation_;
            }
            else {

                /* Work out the amounts by which the user wants to rotate
                 * in the 'horizontal' and 'vertical' directions respectively
                 * (these directions are relative to the current orientation
                 * of the view). */
                Plot3D plot = (Plot3D) getPlot();
                double scale = Math.min( plot.getWidth(), plot.getHeight() );
                double xf = - ( pos.x - posBase_.x ) / scale / zoom_;
                double yf = - ( pos.y - posBase_.y ) / scale / zoom_;

                /* Turn these into angles.  Phi and Psi are the rotation
                 * angles around the screen vertical and horizontal axes
                 * respectively. */
                double phi = xf * Math.PI / 2.;
                double psi = yf * Math.PI / 2.;
                setRotation( Plot3D.rotateXY( rotBase_, phi, psi ) );
                replot();
            }
        }

        public void mouseMoved( MouseEvent evt ) {
            posBase_ = null;
            rotBase_ = null;
        }

        public void mouseReleased( MouseEvent evt ) {
            if ( isRotating_ ) {
                isRotating_ = false;
                replot();
            }
        }
    }

    /**
     * Watches for points clicked and activates the corresponding row(s) 
     * if they are.
     */
    private class PointClickListener extends MouseAdapter {
        public void mouseClicked( MouseEvent evt ) {
            int butt = evt.getButton();
            if ( butt == MouseEvent.BUTTON1 ) {

                /* Get the position in plot coordinates. */
                Plot3D plot = (Plot3D) getPlot();
                Point point = evt.getPoint();

                /* Get the closest plotted point to this. */
                PointIterator pointIt = plot.getPlottedPointIterator();
                int ip = pointIt == null
                       ? -1
                       : pointIt.getClosestPoint( point, 4 );

                /* Highlight if there is one. */
                if ( ip >= 0 ) {
                    PointSelection psel =
                        (PointSelection) plot.getState().getPlotData();
                    psel.getPointTable( ip )
                        .highlightRow( psel.getPointRow( ip ) );
                }
                else {
                    annotations_.setActivePoints( new int[ 0 ] );
                }
            }
        }
    }
}
