package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ListSelectionModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.OverlayLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.BitsRowSubset;
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

    private boolean replotted_;
    private MarkStyleProfile markers_;
    private boolean activeBlob_;
    private BitSet visibleRows_;
    private PointRegistry visiblePoints_;

    private static final MarkStyleProfile MARKERS1;
    private static final MarkStyleProfile MARKERS2;
    private static final MarkStyleProfile MARKERS3;
    private static final MarkStyleProfile MARKERS4;
    private static final MarkStyleProfile MARKERS5;
    static final MarkStyleProfile[] MARKER_PROFILES = new MarkStyleProfile[] {
        MARKERS1 =
        MarkStyleProfile.points( "Pixels" ),
        MARKERS2 =
        MarkStyleProfile.spots( "Dots", 1 ),
        MARKERS3 =
        MarkStyleProfile.spots( "Spots", 2 ),
        MARKERS4 =
        MarkStyleProfile.filledShapes( "Small Coloured Shapes", 3, null ),
        MARKERS5 =
        MarkStyleProfile.filledShapes( "Medium Coloured Shapes", 4, null ),
        MarkStyleProfile.filledShapes( "Large Coloured Shapes", 5, null ),
        MarkStyleProfile.filledShapes( "Small Black Shapes", 3, Color.black ),
        MarkStyleProfile.filledShapes( "Medium Black Shapes", 4, Color.black ),
        MarkStyleProfile.filledShapes( "Large Black Shapes", 5, Color.black ),
        MarkStyleProfile.openShapes( "Small Coloured Outlines", 3, null ),
        MarkStyleProfile.openShapes( "Medium Coloured Outlines", 4, null ),
        MarkStyleProfile.openShapes( "Large Coloured Outlines", 5, null ),
        MarkStyleProfile.openShapes( "Small Black Outlines", 3, Color.black ),
        MarkStyleProfile.openShapes( "Medium Black Outlines", 4, Color.black ),
        MarkStyleProfile.openShapes( "Large Black Outlines", 5, Color.black ),
        MarkStyleProfile.ghosts( "Faint Transparent Pixels", 0, 0.1f ),
        MarkStyleProfile.ghosts( "Medium Transparent Pixels", 0, 0.4f ),
        MarkStyleProfile.ghosts( "Faint Transparent Dots", 1, 0.1f ),
        MarkStyleProfile.ghosts( "Medium Transparent Dots", 1, 0.4f ),
    };

    /**
     * Constructs a new PlotWindow.
     *
     * @param  tcModel  data model whose data the window will plot
     * @param  parent   parent component (may be used for positioning)
     */
    public PlotWindow( TopcatModel tcModel, Component parent ) {
        super( tcModel, "Scatter Plot", new String[] { "X", "Y" }, parent );
        tcModel.addTopcatListener( this );

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
                if ( replotted_ || height != lastHeight_
                                || width != lastWidth_ ) {
                    recordVisiblePoints( getState(), getPoints(),
                                         getSurface() );
                    lastHeight_ = height;
                    lastWidth_ = width;
                    replotted_ = false;
                }
            }
        };

        /* Construct and populate the plot panel with the plot itself
         * and a transparent layer for doodling blobs on. */
        JPanel plotPanel = new JPanel();
        blobPanel_ = new BlobPanel();
        blobPanel_.setVisible( false );
        plotPanel.setLayout( new OverlayLayout( plotPanel ) );
        plotPanel.add( blobPanel_ );
        plotPanel.add( plot_ );
        plotPanel.setPreferredSize( new Dimension( 500, 300 ) );

        /* Listen for point-clicking events on the plot. */
        /* I have to reach right in to find the plot surface component to
         * add the mouse listener to it; it would be tidier to just add
         * the listener to the plot component itself, but that doesn't
         * receive the mouse events, since it's not the deepest visible
         * component.  Doing it this way is probably easier than mucking
         * about with glassPanes. */
        plot_.getSurface().getComponent()
             .addMouseListener( new PointClickListener() );

        /* Arrange the components in the top level window. */
        JPanel mainArea = getMainArea();
        mainArea.add( plotPanel, BorderLayout.CENTER );

        /* Set up the initial marker profile. */
        markers_ = getMarkStyleProfile( tcModel );

        /* Action for showing the grid. */
        JToggleButton gridButton = getGridButton();
        JCheckBoxMenuItem gridMenuItem =
            new JCheckBoxMenuItem( gridButton.getText(), gridButton.getIcon() );
        gridMenuItem.setModel( gridButton.getModel() );

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
        plotMenu.add( gridMenuItem );
        plotMenu.add( getReplotAction() );
        getJMenuBar().add( plotMenu );

        /* Construct a new menu for subset operations. */
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        blobAction_ = new AbstractAction() {
            public void actionPerformed( ActionEvent evt ) {
                if ( activeBlob_ ) {
                    useBlob();
                    setActiveBlob( false );
                }
                else {
                    setActiveBlob( true );
                }
            }
        };
        setActiveBlob( false );
        blobAction_.setEnabled( false );
        fromVisibleAction_ = new BasicAction( "New subset from visible",
                                              ResourceIcon.VISIBLE_SUBSET,
                                              "Define a new row subset " +
                                              "containing only " +
                                              "currently visible points" ) {
            public void actionPerformed( ActionEvent evt ) {
                addNewSubset( visibleRows_ );
            }
        };
        fromVisibleAction_.setEnabled( false );
        subsetMenu.add( blobAction_ );
        subsetMenu.add( fromVisibleAction_ );
        getJMenuBar().add( subsetMenu );

        /* Construct a new menu for marker profile selection. */
        JMenu markerMenu = new JMenu( "Marker Types" );
        markerMenu.setMnemonic( KeyEvent.VK_M );
        MarkStyleProfile[] profiles = MARKER_PROFILES;
        for ( int i = 0; i < profiles.length; i++ ) {
            final MarkStyleProfile profile = profiles[ i ];
            String name = profile.getName();
            Icon icon = profile.getIcon();
            Action profileAct = new BasicAction( name, icon,
                                                 "Set default marker types to "
                                                 + name ) {
                public void actionPerformed( ActionEvent evt ) {
                    markers_ = profile;
                    replot();
                }
            };
            markerMenu.add( profileAct );
        }
        getJMenuBar().add( markerMenu );

        /* Add actions to the toolbar. */
        getToolBar().add( resizeAction );
        getToolBar().add( gridButton );
        getToolBar().add( getReplotAction() );
        getToolBar().add( blobAction_ );
        getToolBar().add( fromVisibleAction_ );
        getToolBar().addSeparator();

        /* Add standard help actions. */
        addHelp( "PlotWindow" );

        /* Perform an initial plot. */
        replot();

        /* Render this component visible. */
        pack();
        setVisible( true );
    }

    protected JComponent getPlot() {
        return plot_;
    }

    protected MarkStyle getMarkStyle( int isub ) {
        return markers_.getStyle( isub );
    }

    protected PlotState createPlotState() {

        /* Create a new blank state for a 2-d plot. */
        Plot2State state = new Plot2State();
        assert ! state.getValid();
        if ( ! getPointSelector().isValid() ) {
            return state;
        }

        /* Collect and store the flags determining which regression lines
         * will be drawn. */
        int[] selection = getOrderedSubsetSelection();
        int nrsets = selection.length;
        boolean[] regressions = new boolean[ nrsets ];
        RowSubset[] usedSubsets = new RowSubset[ nrsets ];
        MarkStyle[] styles = new MarkStyle[ nrsets ];
        OptionsListModel subsets = getPointSelector().getTable().getSubsets();
        for ( int isel = 0; isel < nrsets; isel++ ) {
            int isub = selection[ isel ];
            usedSubsets[ isel ] = (RowSubset) subsets.get( isub );
            styles[ isel ] = getMarkStyle( isub );

            // Currently, regression selection is not working -
            // just set false here for now.
            regressions[ isel ] = false;
        }
        state.setRegressions( regressions );

        /* This step isn't necessary since the superclass getPlotState method
         * will fill these values in.  However currently it does an assertion
         * test that they are the same if it is filled in here. */
        state.setSubsets( usedSubsets, styles );

        /* Return the configured state object. */
        state.setValid( true );
        return state;
    }

    protected void doReplot( PlotState state, Points points ) {

        /* Cancel any active blob-drawing.  This is necesary since
         * the replot may put a different set of points inside it.
         * As a secondary consideration, forcing a replot by resizing
         * the window etc is an intuitive way for the user to escape
         * a blob-drawing session). */
        setActiveBlob( false );

        /* Send the plot component the most up to date plotting state. */
        PlotState lastState = plot_.getState();
        plot_.setPoints( points );
        plot_.setState( (Plot2State) state );

        /* If the axes are different from the last time we plotted,
         * fix it so that all the points are included. */
        /* This has the effect of rescaling even if the axes are just
         * flipped, which probably isn't what the user wants to see,
         * but implementation details mean that it's not easy to get
         * away without doing this, so if you want to change that
         * make sure you check it still works properly afterwards. */
        if ( ! state.sameAxes( lastState ) ) {
            plot_.rescale();
        }

        /* Schedule for repainting so the changes can take effect.
         * It's tempting to call recordVisiblePoints here and have done
         * with it, but it has to be done from within the painting
         * system since the window geometry might not be correct here. */
        replotted_ = true;
        plot_.repaint();
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
    private void recordVisiblePoints( PlotState state, Points points,
                                      PlotSurface surface ) {
        if ( points == null || state == null ) {
            return;
        }
        int np = points.getCount();
        RowSubset[] sets = state.getSubsets();
        int nset = sets.length;
        BitSet visible = new BitSet();
        PointRegistry plotted = new PointRegistry();
        int nVisible = 0;
        for ( int ip = 0; ip < np; ip++ ) {
            Point point = surface.dataToGraphics( points.getCoord( ip, 0 ),
                                                  points.getCoord( ip, 1 ),
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
        }
        plotted.ready();
        visiblePoints_ = plotted;
        visibleRows_ = visible;
        fromVisibleAction_.setEnabled( nVisible > 0 );
        blobAction_.setEnabled( nVisible > 0 );
    }

    /**
     * Determine whether we are currently in blob-drawing mode or not.
     *
     * @param  activeBlob true iff blob is being drawn
     */
    private void setActiveBlob( boolean active ) {
        activeBlob_ = active;
        blobPanel_.clear();
        blobPanel_.setVisible( active );
        blobAction_.putValue( Action.NAME,
                              active ? "Finish Drawing Region"
                                     : "Draw Subset Region" );
        blobAction_.putValue( Action.SMALL_ICON,
                              active ? ResourceIcon.BLOB_SUBSET_END
                                     : ResourceIcon.BLOB_SUBSET );
        blobAction_.putValue( Action.SHORT_DESCRIPTION,
                              active ? "Define subset from currently-drawn " +
                                       "region"
                                     : "Draw a region on the plot to define " +
                                       "a new row subset" );
    }

    /**
     * Called when the user has indicated that the blob he has drawn is
     * finished and ready to be turned into a subset.
     */
    private void useBlob() {
        Shape blob = blobPanel_.getBlob();
        addNewSubset( visiblePoints_.getContainedPoints( blob ) );
    }

    /**
     * Adds a new row subset to, er, at least zero of the associated 
     * tables for this window.  The subset is based on a bit vector
     * representing the points in this window's Points object.
     *
     * @param  mask  bit vector giving included points
     */
    private void addNewSubset( BitSet mask ) {
        if ( mask.cardinality() > 0 ) {
            PointSelector psel = getPointSelector();
            TopcatModel tcModel = psel.getTable();
            OptionsListModel subsets = tcModel.getSubsets();
            String name = tcModel.enquireSubsetName( PlotWindow.this );
            if ( name != null ) {
                int inew = subsets.size();
                subsets.add( new BitsRowSubset( name, mask ) );
                boolean[] flags = psel.getSubsetSelection();
                assert flags.length == inew + 1;
                flags[ inew ] = true;
                psel.setSubsetSelection( flags );
            }
        }
    }

    /*
     * TopcatListener implementation.
     */
    public void modelChanged( TopcatEvent evt ) {
        if ( evt.getCode() == TopcatEvent.ROW ) {
            Object datum = evt.getDatum();
            if ( datum instanceof Long ) {
                long lrow = ((Long) datum).longValue();
                plot_.setActivePoint( Tables.checkedLongToInt( lrow ) );
            }
            else {
                assert false;
            }
        }
    }

    /**
     * Returns a suitable marker profile for a given table.
     *
     * @param  tcModel  table 
     * @return   marker style profile
     */
    private static MarkStyleProfile getMarkStyleProfile( TopcatModel tcModel ) {
        long nRows = tcModel.getDataModel().getRowCount();
        if ( nRows > 20000 ) {
            return MARKERS1;
        }
        else if ( nRows > 2000 ) {
            return MARKERS2;
        }
        else if ( nRows > 200 ) {
            return MARKERS3;
        }
        else if ( nRows > 20 ) {
            return MARKERS4;
        }
        else {
            return MARKERS5;
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
                    getPointSelector().getTable().highlightRow( (long) ip );
                }
                else {
                    plot_.setActivePoint( -1 );
                }
            }
        }
    }

}
