package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.BitSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ListSelectionModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.CheckBoxMenu;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.MarkStyles;
import uk.ac.starlink.ttools.plot.PlotEvent;
import uk.ac.starlink.ttools.plot.PlotListener;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.PlotSurface;
import uk.ac.starlink.ttools.plot.PtPlotSurface;
import uk.ac.starlink.ttools.plot.ScatterPlot;
import uk.ac.starlink.ttools.plot.ScatterPlotEvent;
import uk.ac.starlink.ttools.plot.StyleSet;
import uk.ac.starlink.ttools.plot.SurfacePlot;
import uk.ac.starlink.ttools.plot.XYStats;

/**
 * Window which displays a scatter plot of two columns from a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Jun 2004
 */
public class PlotWindow extends GraphicsWindow implements TopcatListener {

    private final JComponent plotPanel_;
    private final BlobPanel blobPanel_;
    private final AnnotationPanel annotations_;
    private final Action blobAction_;
    private final Action fromVisibleAction_;
    private final SurfaceZoomRegionList zoomRegionList_;

    private static final ErrorRenderer DEFAULT_ERROR_RENDERER =
        ErrorRenderer.EXAMPLE;
    private static final StyleSet[] STYLE_SETS =
        fixDefaultErrorRenderers( DEFAULT_ERROR_RENDERER,
                                  getStandardMarkStyleSets() );
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
    private static final String[] AXIS_NAMES = new String[] { "X", "Y", };
    private static final ErrorRenderer[] ERROR_RENDERERS = 
        ErrorRenderer.getOptions2d();

    /**
     * Constructs a new PlotWindow.
     *
     * @param  parent   parent component (may be used for positioning)
     */
    @SuppressWarnings("this-escape")
    public PlotWindow( Component parent ) {
        super( "Scatter Plot (old)", new ScatterPlot( new PtPlotSurface() ),
               AXIS_NAMES, 3, true, createErrorModeModels( AXIS_NAMES ),
               parent );
        final ScatterPlot plot = (ScatterPlot) getPlot();
        final PlotSurface surface = plot.getSurface();

        plot.addPlotListener( new PlotListener() {
            public void plotChanged( PlotEvent evt ) {
                ScatterPlotEvent sevt = (ScatterPlotEvent) evt;
                zoomRegionList_.reconfigure();
                boolean someVisible = evt.getVisiblePointCount() > 0;
                fromVisibleAction_.setEnabled( someVisible );
                blobAction_.setEnabled( someVisible );
                XYStats[] xyStats = sevt.getXYStats();
                SetId[] setIds =
                    ((PointSelection) evt.getPlotState().getPlotData())
                                                        .getSetIds();
                ((MarkStyleEditor) getPointSelectors()
                                  .getStyleWindow().getEditor())
                                  .setStats( setIds, xyStats );
            }
        } );

        /* Annotations panel. */
        annotations_ = new AnnotationPanel();

        /* Zooming. */
        zoomRegionList_ = new SurfaceZoomRegionList( plot ) {
            protected void requestZoom( double[][] bounds ) {
                for ( int idim = 0; idim < 2; idim++ ) {
                    if ( bounds[ idim ] != null ) {
                        getAxisWindow().getEditors()[ idim ].clearBounds();
                        getViewRanges()[ idim ].setBounds( bounds[ idim ] );
                    }
                }
                replot();
            }
        };
        Zoomer zoomer = new Zoomer();
        zoomer.setRegions( zoomRegionList_ );
        zoomer.setCursorComponent( plot );
        Component scomp = plot.getSurface().getComponent();
        scomp.addMouseListener( zoomer );
        scomp.addMouseMotionListener( zoomer );

        /* Construct and populate the plot panel with the plot itself
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
        plotPanel_.setPreferredSize( new Dimension( 500, 400 ) );

        /* Listen for point-clicking events on the plot. */
        /* I have to reach right in to find the plot surface component to
         * add the mouse listener to it; it would be tidier to just add
         * the listener to the plot component itself, but that doesn't
         * receive the mouse events, since it's not the deepest visible
         * component.  Doing it this way is probably easier than mucking
         * about with glassPanes. */
        surface.getComponent().addMouseListener( new PointClickListener() );

        /* Listen for topcat actions. */
        getPointSelectors().addTopcatListener( this );

        /* Add status lines for displaying the number of points plotted 
         * and the pointer position. */
        PositionLabel posStatus = new PositionLabel( surface );
        posStatus.setMaximumSize( new Dimension( Integer.MAX_VALUE,
                                                 posStatus.getMaximumSize()
                                                          .height ) );
        PlotStatsLabel plotStatus = new PlotStatsLabel();
        plot.addPlotListener( plotStatus );
        getStatusBox().add( plotStatus );
        getStatusBox().add( Box.createHorizontalStrut( 5 ) );
        getStatusBox().add( posStatus );
        getStatusBox().add( Box.createHorizontalGlue() );

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( getRescaleAction() );
        plotMenu.add( getAxisEditAction() );
        plotMenu.add( getGridModel().createMenuItem() );
        plotMenu.add( getLegendModel().createMenuItem() );
        plotMenu.add( getReplotAction() );
        getJMenuBar().add( plotMenu );

        /* Construct a new menu for axis operations. */
        JMenu axisMenu = new JMenu( "Axes" );
        axisMenu.setMnemonic( KeyEvent.VK_A );
        axisMenu.add( getFlipModels()[ 0 ].createMenuItem() );
        axisMenu.add( getFlipModels()[ 1 ].createMenuItem() );
        axisMenu.addSeparator();
        axisMenu.add( getLogModels()[ 0 ].createMenuItem() );
        axisMenu.add( getLogModels()[ 1 ].createMenuItem() );
        getJMenuBar().add( axisMenu );

        /* Construct a new menu for subset operations. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        blobAction_.setEnabled( false );
        fromVisibleAction_ = new BasicAction( "New subset from visible",
                                              ResourceIcon.VISIBLE_SUBSET,
                                              "Define a new row subset " +
                                              "containing only " +
                                              "currently visible points" ) {
            public void actionPerformed( ActionEvent evt ) {
                addNewSubsets( plot.getPlottedPointIterator().getAllPoints() );
            }
        };
        fromVisibleAction_.setEnabled( false );
        subsetMenu.add( blobAction_ );
        subsetMenu.add( fromVisibleAction_ );
        getJMenuBar().add( subsetMenu );

        /* Construct a new menu for error modes. */
        getJMenuBar().add( createErrorModeMenu() );

        /* Construct a new menu for marker style set selection. */
        getJMenuBar().add( createMarkerStyleMenu( STYLE_SETS ) );

        /* Add a new menu for error bar style selection. */
        getJMenuBar().add( createErrorRendererMenu( ERROR_RENDERERS ) );

        /* Add actions to the toolbar. */
        getPointSelectorToolBar().addSeparator();
        getPointSelectorToolBar().add( getErrorModeModels()[ 0 ]
                                      .createOnOffToolbarButton() );
        getPointSelectorToolBar().add( getErrorModeModels()[ 1 ]
                                      .createOnOffToolbarButton() );
        getToolBar().add( getRescaleAction() );
        getToolBar().add( getGridModel().createToolbarButton() );
        getToolBar().add( getLegendModel().createToolbarButton() );
        getToolBar().add( blobAction_ );
        getToolBar().add( fromVisibleAction_ );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "PlotWindow" );

        /* Perform an initial plot. */
        replot();
    }

    protected JComponent getPlotPanel() {
        return plotPanel_;
    }

    protected void doReplot( PlotState state ) {
        annotations_.setPlotData( state.getPlotData() );
        annotations_.setPlacer( ((ScatterPlot) getPlot()).getPointPlacer() );

        /* Cancel any active blob-drawing.  This is necesary since
         * the replot may put a different set of points inside it.
         * As a secondary consideration, forcing a replot by resizing
         * the window etc is an intuitive way for the user to escape
         * a blob-drawing session). */
        blobPanel_.setActive( false );

        /* Send the plot component the most up to date plotting state. */
        super.doReplot( state );
    }

    protected StyleEditor createStyleEditor() {
        return new MarkStyleEditor( true, true, ERROR_RENDERERS,
                                    DEFAULT_ERROR_RENDERER,
                                    getErrorModeModels() );
    }

    /*
     * TopcatListener implementation.
     */
    public void modelChanged( TopcatEvent evt ) {
        if ( evt.getCode() == TopcatEvent.ROW ) {
            ScatterPlot plot = (ScatterPlot) getPlot();
            Object datum = evt.getDatum();
            if ( datum instanceof Long ) {
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

    public StyleSet getDefaultStyles( int npoint ) {
        if ( npoint > 100000 ) {
            int opaqueLimit = npoint / 50000;
            return MarkStyles.faded( "Ghost " + opaqueLimit, MARKERS1,
                                     opaqueLimit );
        }
        if ( npoint > 10000 ) {
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

    /**
     * Mouse listener to handle clicks on given points.
     */
    private class PointClickListener extends MouseAdapter {
        public void mouseClicked( MouseEvent evt ) {
            ScatterPlot plot = (ScatterPlot) getPlot();
            int butt = evt.getButton();
            if ( butt == MouseEvent.BUTTON1 ) {
                int ip = plot.getPlottedPointIterator()
                             .getClosestPoint( evt.getPoint(), 4 );
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
