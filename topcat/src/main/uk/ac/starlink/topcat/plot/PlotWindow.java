package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
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
import uk.ac.starlink.topcat.OptionsListModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Window which displays a scatter plot of two columns from a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    21 Jun 2004
 */
public class PlotWindow extends GraphicsWindow implements TopcatListener {

    private final ScatterPlot plot_;
    private final BlobPanel blobPanel_;
    private final Action blobAction_;
    private final Action fromVisibleAction_;
    private final CountsLabel plotStatus_;

    private boolean replotted_;
    private BitSet visibleRows_;
    private PointRegistry visiblePoints_;

    private static final StyleSet MARKERS1;
    private static final StyleSet MARKERS2;
    private static final StyleSet MARKERS3;
    private static final StyleSet MARKERS4;
    private static final StyleSet MARKERS5;
    static final StyleSet[] STYLE_SETS = new StyleSet[] {
        MARKERS1 =
        MarkStyles.points( "Pixels" ),
        MARKERS2 =
        MarkStyles.spots( "Dots", 1 ),
        MARKERS3 =
        MarkStyles.spots( "Spots", 2 ),
        MARKERS4 =
        MarkStyles.filledShapes( "Small Coloured Shapes", 3, null ),
        MARKERS5 =
        MarkStyles.filledShapes( "Medium Coloured Shapes", 4, null ),
        MarkStyles.filledShapes( "Large Coloured Shapes", 5, null ),
        MarkStyles.filledShapes( "Small Black Shapes", 3, Color.black ),
        MarkStyles.filledShapes( "Medium Black Shapes", 4, Color.black ),
        MarkStyles.filledShapes( "Large Black Shapes", 5, Color.black ),
        MarkStyles.openShapes( "Small Coloured Outlines", 3, null ),
        MarkStyles.openShapes( "Medium Coloured Outlines", 4, null ),
        MarkStyles.openShapes( "Large Coloured Outlines", 5, null ),
        MarkStyles.openShapes( "Small Black Outlines", 3, Color.black ),
        MarkStyles.openShapes( "Medium Black Outlines", 4, Color.black ),
        MarkStyles.openShapes( "Large Black Outlines", 5, Color.black ),
        MarkStyles.faded( "Faint Transparent Pixels", MARKERS1, 20 ),
        MarkStyles.faded( "Faint Transparent Dots", MARKERS2, 20 ),
        MarkStyles.faded( "Medium Transparent Pixels", MARKERS1, 5 ),
        MarkStyles.faded( "Medium Transparent Dots", MARKERS2, 5 ),
    };

    /**
     * Constructs a new PlotWindow.
     *
     * @param  tcModel  data model whose data the window will plot
     * @param  parent   parent component (may be used for positioning)
     */
    public PlotWindow( Component parent ) {
        super( "Scatter Plot", new String[] { "X", "Y" }, parent );

        /* Construct the plot component.  The paint method is
         * overridden so that when the points are replotted we maintain
         * a record of their current positions.  The Swing tutorial
         * generally recommends against overriding paint itself
         * (normally one should override paintComponent) but since all
         * we're doing here is invoking the superclass implementation
         * and some non-graphics-related stuff, it should be OK.
         * Overriding paintComponent would be no good, since it needs
         * to be called following paintChildren. */
        plot_ = new ScatterPlot( new PtPlotSurface( this ) ) {
            int lastHeight_;
            int lastWidth_;
            public void paint( Graphics g ) {
                super.paint( g );
                int height = getHeight();
                int width = getWidth();
                PlotState state = getState();
                if ( state.getValid() &&
                     ( replotted_ || height != lastHeight_
                                  || width != lastWidth_ ) ) {
                    recordVisiblePoints( state, getPoints(), getSurface() );
                    recordCorrelations( getPointSelection().getSetIds(),
                                        getCorrelations() );
                    lastHeight_ = height;
                    lastWidth_ = width;
                    replotted_ = false;
                }
            }
        };

        /* Construct and populate the plot panel with the plot itself
         * and a transparent layer for doodling blobs on. */
        JPanel plotPanel = new JPanel();
        blobPanel_ = new BlobPanel() {
            protected void blobCompleted( Shape blob ) {
                addNewSubsets( visiblePoints_.getContainedPoints( blob ) );
            }
        };
        blobAction_ = blobPanel_.getBlobAction();
        plotPanel.setLayout( new OverlayLayout( plotPanel ) );
        plotPanel.add( blobPanel_ );
        plotPanel.add( plot_ );
        plotPanel.setPreferredSize( new Dimension( 500, 400 ) );

        /* Listen for point-clicking events on the plot. */
        /* I have to reach right in to find the plot surface component to
         * add the mouse listener to it; it would be tidier to just add
         * the listener to the plot component itself, but that doesn't
         * receive the mouse events, since it's not the deepest visible
         * component.  Doing it this way is probably easier than mucking
         * about with glassPanes. */
        PlotSurface surface = plot_.getSurface();
        surface.getComponent().addMouseListener( new PointClickListener() );

        /* Listen for topcat actions. */
        getPointSelectors().addTopcatListener( this );

        /* Arrange the components in the top level window. */
        JPanel mainArea = getMainArea();
        mainArea.add( plotPanel, BorderLayout.CENTER );

        /* Add status lines for displaying the number of points plotted 
         * and the pointer position. */
        PositionLabel posStatus = new PositionLabel( surface );
        posStatus.setMaximumSize( new Dimension( Integer.MAX_VALUE,
                                                 posStatus.getMaximumSize()
                                                          .height ) );
        plotStatus_ = new CountsLabel( new String[] {
            "Potential", "Included", "Visible",
        } );
        getStatusBox().add( plotStatus_ );
        getStatusBox().add( Box.createHorizontalStrut( 5 ) );
        getStatusBox().add( posStatus );
        getStatusBox().add( Box.createHorizontalGlue() );

        /* Action for resizing the plot. */
        Action resizeAction = new BasicAction( "Rescale", ResourceIcon.RESIZE,
                                               "Rescale the plot to show " +
                                               "all points" ) {
            public void actionPerformed( ActionEvent evt ) {
                plot_.rescale();
                forceReplot();
            }
        };

        /* Construct a new menu for general plot operations. */
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( resizeAction );
        plotMenu.add( getGridModel().createMenuItem() );
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
                addNewSubsets( visibleRows_ );
            }
        };
        fromVisibleAction_.setEnabled( false );
        subsetMenu.add( blobAction_ );
        subsetMenu.add( fromVisibleAction_ );
        getJMenuBar().add( subsetMenu );

        /* Construct a new menu for marker style set selection. */
        JMenu styleMenu = new JMenu( "Marker Style" );
        styleMenu.setMnemonic( KeyEvent.VK_M );
        StyleSet[] styleSets = STYLE_SETS;
        for ( int i = 0; i < styleSets.length; i++ ) {
            final StyleSet styleSet = styleSets[ i ];
            String name = styleSet.getName();
            Icon icon = MarkStyles.getIcon( styleSet );
            Action stylesAct = new BasicAction( name, icon,
                                                "Set marker plotting style to "
                                                + name ) {
                public void actionPerformed( ActionEvent evt ) {
                    setStyles( styleSet );
                    replot();
                }
            };
            styleMenu.add( stylesAct );
        }
        getJMenuBar().add( styleMenu );

        /* Add actions to the toolbar. */
        getToolBar().add( resizeAction );
        getToolBar().add( getGridModel().createToolbarButton() );
        getToolBar().add( getReplotAction() );
        getToolBar().add( blobAction_ );
        getToolBar().add( fromVisibleAction_ );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "PlotWindow" );

        /* Perform an initial plot. */
        replot();
    }

    protected JComponent getPlot() {
        return plot_;
    }

    protected void doReplot( PlotState state, Points points ) {

        /* Cancel any active blob-drawing.  This is necesary since
         * the replot may put a different set of points inside it.
         * As a secondary consideration, forcing a replot by resizing
         * the window etc is an intuitive way for the user to escape
         * a blob-drawing session). */
        blobPanel_.setActive( false );

        /* Send the plot component the most up to date plotting state. */
        PlotState lastState = plot_.getState();
        plot_.setPoints( points );
        plot_.setState( state );

        /* If the axes are different from the last time we plotted,
         * fix it so that all the points are included. */
        /* This has the effect of rescaling even if the axes are just
         * flipped, which probably isn't what the user wants to see,
         * but implementation details mean that it's not easy to get
         * away without doing this, so if you want to change that
         * make sure you check it still works properly afterwards. */
        if ( ! state.sameAxes( lastState ) || ! state.sameData( lastState ) ) {
            plotStatus_.setValues( null );
            plot_.rescale();
        }

        /* Schedule for repainting so the changes can take effect.
         * It's tempting to call recordVisiblePoints here and have done
         * with it, but it has to be done from within the painting
         * system since the window geometry might not be correct here. */
        replotted_ = true;
        plot_.repaint();
    }

    protected StyleEditor createStyleEditor() {
        return new MarkStyleEditor( true, true );
    }

    /**
     * Works out which points are currently visible and stores this
     * information for possible later use.  Although this information
     * has to be calculcated by the ScatterPlot's paintComponent method,
     * it's not really possible to use that version of the information,
     * since it might be done in several calls of paintComponent
     * (with different clips) and it's not clear how to amalgamate all
     * these.  So we have to do this work more than once.
     *
     * <p>This method works out which points are visible in the current
     * plotting surface and stores the answer in a BitSet.  Elements of
     * this will be false if they either fall outside the range of the
     * current axes or do not appear in any of the plot state's
     * included subsets.
     *
     * @param  state  plotting state
     * @param  points  data points
     * @param  surface  plotting surface
     */
    private void recordVisiblePoints( PlotState state, final Points points,
                                      PlotSurface surface ) {
        if ( points == null || state == null ) {
            return;
        }
        int np = points.getCount();
        RowSubset[] sets = state.getPointSelection().getSubsets();
        int nset = sets.length;
        final BitSet visible = new BitSet();
        PointRegistry plotted = new PointRegistry();
        int nVisible = 0;
        int nInvisible = 0;
        double[] coords = new double[ 2 ];
        for ( int ip = 0; ip < np; ip++ ) {
            points.getCoords( ip, coords );
            Point point = surface.dataToGraphics( coords[ 0 ], coords[ 1 ],
                                                  true );
            if ( point != null ) {
                int xp = point.x;
                int yp = point.y;
                long lp = (long) ip;
                for ( int is = 0; is < nset; is++ ) {
                    if ( sets[ is ].isIncluded( lp ) ) {
                        visible.set( ip );
                        plotted.addPoint( ip, point );
                        nVisible++;
                        break;
                    }
                }
            }
            else {
                for ( int is = 0; is < nset; is++ ) {
                    if ( sets[ is ].isIncluded( ip ) ) {
                        nInvisible++;
                        break;
                    }
                }
            }
        }
        plotted.ready();
        visiblePoints_ = plotted;
        visibleRows_ = visible;
        final boolean someVisible = nVisible > 0;
        final int potentialCount = points.getCount();
        final int includedCount = visible.cardinality() + nInvisible;
        final int visibleCount = visible.cardinality();
        assert potentialCount >= includedCount;
        assert includedCount >= visibleCount;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                fromVisibleAction_.setEnabled( someVisible );
                blobAction_.setEnabled( someVisible );
                plotStatus_.setValues( new int[] { potentialCount,
                                                   includedCount,
                                                   visibleCount } );
            }
        } );
    }

    private void recordCorrelations( SetId[] setIds, XYStats[] stats ) {
        ((MarkStyleEditor) getPointSelectors().getStyleWindow().getEditor())
                          .setStats( setIds, stats );
    }

    /*
     * TopcatListener implementation.
     */
    public void modelChanged( TopcatEvent evt ) {
        if ( evt.getCode() == TopcatEvent.ROW ) {
            Object datum = evt.getDatum();
            if ( datum instanceof Long ) {
                TopcatModel tcModel = evt.getModel();
                PointSelection psel = plot_.getState().getPointSelection();
                long lrow = ((Long) datum).longValue();
                long[] lps = psel.getPointsForRow( tcModel, lrow );
                int[] ips = new int[ lps.length ];
                for ( int i = 0; i < lps.length; i++ ) {
                    ips[ i ] = Tables.checkedLongToInt( lps[ i ] );
                }
                plot_.setActivePoints( ips );
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
            int butt = evt.getButton();
            if ( butt == MouseEvent.BUTTON1 ) {
                int ip = visiblePoints_.getClosestPoint( evt.getPoint(), 4 );
                if ( ip >= 0 ) {
                    PointSelection psel = plot_.getState().getPointSelection();
                    psel.getPointTable( ip )
                        .highlightRow( psel.getPointRow( ip ) );
                }
                else {
                    plot_.setActivePoints( new int[ 0 ] );
                }
            }
        }
    }
}
