package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.SubsetConsumer;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.NavigationListener;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.CachedDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.MemoryColumnFactory;
import uk.ac.starlink.ttools.plot2.data.SmartColumnFactory;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Window for all plots.
 * This is generic and currently not expected to have much behaviour
 * implemented by subclasses; plot-type-specific behaviour is
 * defined by supplied PlotType and PlotTypeGui objects.
 * It uses a PlotPanel to do the actual plotting work; this class
 * handles placing the PlotPanel, gathering and supplying user configuration
 * information to it, invoking replots at appropriate times, and
 * managing other user interactions.
 *
 * @author    Mark Taylor
 * @since     12 Mar 2013
 */
public class StackPlotWindow<P,A> extends AuxWindow {

    private final PlotType plotType_;
    private final AxisControl<P,A> axisControl_;
    private final SurfaceFactory<P,A> surfFact_;
    private final PlotPanel<P,A> plotPanel_;
    private final ControlStack stack_;
    private final ControlStackModel stackModel_;
    private final ToggleButtonModel showProgressModel_;
    private final JLabel posLabel_;
    private final JLabel countLabel_;
    private final BlobPanel2 blobPanel_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * Constructor.
     *
     * @param  name  window name
     * @param  parent  parent component
     * @param  plotType   defines basic plot type characteristics
     * @param  plotTypeGui  defines graphical user interface specifics
     *                      for plot type
     */
    public StackPlotWindow( String name, Component parent, PlotType plotType,
                            PlotTypeGui<P,A> plotTypeGui ) {
        super( name, parent );
        plotType_ = plotType;

        /* Set up user interface components in the window that can gather
         * all the information required to perform (re-)plots. */
        stack_ = new ControlStack();
        stackModel_ = stack_.getStackModel();
        MultiConfigger configger = new MultiConfigger();
        axisControl_ = plotTypeGui.createAxisControl( stack_ );
        ToggleButtonModel axlockModel = axisControl_.getAxisLockModel();
        surfFact_ = axisControl_.getSurfaceFactory();
        configger.addConfigger( axisControl_ );
        final ShaderControl shaderControl =
            new ShaderControl( stackModel_, configger );
        configger.addConfigger( shaderControl );
        DataStoreFactory storeFact =
            new CachedDataStoreFactory(
                new SmartColumnFactory( new MemoryColumnFactory() ) );
        Factory<PlotLayer[]> layerFact = new Factory<PlotLayer[]>() {
            public PlotLayer[] getItem() {
                return readPlotLayers();
            }
        };
        final LegendControl legendControl =
            new LegendControl( stackModel_, configger );
        Factory<Icon> legendFact = new Factory<Icon>() {
            public Icon getItem() {
                return legendControl.getLegendIcon();
            }
        };
        Factory<float[]> legendPosFact = new Factory<float[]>() {
            public float[] getItem() {
                return legendControl.getLegendPosition();
            }
        };
        ToggleButtonModel sketchModel =
            new ToggleButtonModel( "Sketch Frames", ResourceIcon.SKETCH,
                                   "Draw intermediate frames from subsampled "
                                 + "data when navigating very large plots" );
        sketchModel.setSelected( true );
        showProgressModel_ =
            new ToggleButtonModel( "Show Plot Progress", ResourceIcon.PROGRESS,
                                   "Report progress for slow plots in the "
                                 + "progress bar at the bottom of the window" );
        showProgressModel_.setSelected( false );

        /* Set up a plot panel with the objects it needs to gather plot
         * requirements from the GUI.  This does the actual plotting. */
        plotPanel_ =
            new PlotPanel<P,A>( storeFact, axisControl_, layerFact, legendFact,
                                legendPosFact, shaderControl, sketchModel,
                                plotType.getPaperTypeSelector(),
                                placeProgressBar().getModel(),
                                showProgressModel_ );

        /* Ensure that the plot panel is messaged when a GUI action occurs
         * that might change the plot appearance.  Each of these controls
         * is forwarding actions from all of its constituent controls. */
        stackModel_.addPlotActionListener( plotPanel_ );
        legendControl.addActionListener( plotPanel_ );
        axisControl_.addActionListener( plotPanel_ );
        shaderControl.addActionListener( plotPanel_ );

        /* Arrange for user navigation actions to adjust the view. */
        new GuiNavigationListener<A>( plotPanel_ ) {
            public Navigator<A> getNavigator() {
                return axisControl_.getNavigator();
            }
            public void setAspect( A aspect ) {
                axisControl_.setAspect( aspect );
                plotPanel_.replot();
            }
        }.addListeners( plotPanel_ );

        /* Arrange for user clicks to identify points. */
        plotPanel_.addMouseListener( new IdentifyListener() );

        /* Prepare a panel that reports current cursor position. */
        posLabel_ = new JLabel();
        JComponent posLine = new LineBox( "Position", posLabel_ );
        posLine.setBorder( BorderFactory.createEtchedBorder() );
        plotPanel_.addMouseListener( new MouseAdapter() {
            public void mouseEntered( MouseEvent evt ) {
                displayPosition( evt.getPoint() );
            }
            public void mouseExited( MouseEvent evt ) {
                displayPosition( null );
            }
        } );
        plotPanel_.addMouseMotionListener( new MouseMotionAdapter() {
            public void mouseMoved( MouseEvent evt ) {
                displayPosition( evt.getPoint() );
            }
        } );

        /* Prepare a panel that reports visible point count. */
        countLabel_ = new JLabel();
        JComponent countLine = new LineBox( "Count", countLabel_ );
        countLine.setBorder( BorderFactory.createEtchedBorder() );
        plotPanel_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                plotChanged();
            }
        } );

        /* Prepare the action that allows the user to select the currently
         * visible points. */
        Action fromVisibleAction =
                new BasicAction( "New subset from visible",
                                 ResourceIcon.VISIBLE_SUBSET,
                                 "Define a new row subset containing only "
                               + "currently visible points" ) {
            public void actionPerformed( ActionEvent evt ) {
                addMaskSubsets( createVisibleMasker() );
            }
        };

        /* Prepare the action that allows the user to select points by
         * hand-drawn region. */
        blobPanel_ = new BlobPanel2() {
            protected void blobCompleted( Shape blob ) {
                setListening( false );
                // will call setActive(false) later
                addMaskSubsets( createBlobMasker( blob ) );
            }
        };
        stackModel_.addPlotActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                blobPanel_.setActive( false );
            }
        } );
        Action blobAction = blobPanel_.getBlobAction();

        /* Prepare the plot export action. */
        final PlotExporter plotExporter = PlotExporter.getInstance();
        Action exportAction =
                new BasicAction( "Export plot to file", ResourceIcon.IMAGE,
                                 "Save the plot to a file"
                               + " in one of several graphics formats" ) {
            public void actionPerformed( ActionEvent evt ) {
                PlotPlacement placer = plotPanel_.getPlotPlacement();
                PlotLayer[] layers = plotPanel_.getPlotLayers();
                Map<AuxScale,Range> auxRanges = plotPanel_.getAuxClipRanges();
                DataStore dataStore = plotPanel_.getDataStore();
                plotExporter.exportPlot( StackPlotWindow.this,
                                         placer, layers, auxRanges, dataStore,
                                         plotType_.getPaperTypeSelector() );
            }
        };

        /* Prepare the plot rescale action. */
        Action resizeAction =
                new BasicAction( "Rescale", ResourceIcon.RESIZE,
                                 "Rescale plot to view all plotted data" ) {
            public void actionPerformed( ActionEvent evt ) {
                axisControl_.setAspect( null );
                axisControl_.setRanges( null );
                axisControl_.clearAspect();
                plotPanel_.replot();
            }
        };

        /* Prepare the replot action. */
        Action replotAction =
                new BasicAction( "Replot", ResourceIcon.REDO,
                                 "Redraw the plot" ) {
            public void actionPerformed( ActionEvent evt ) {
                plotPanel_.clearData();
                plotPanel_.replot();
            }
        };

        /* Prepare the actions that allow the user to populate the plot
         * with data layers appropriate to this window's plot type. */
        TopcatListener tcListener = new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                if ( evt.getCode() == TopcatEvent.ROW ) {
                    highlightRow( evt.getModel(),
                                  ((Long) evt.getDatum()).longValue() );
                }
            }
        };
        ControlManager controlManager =
            new GangControlManager( stack_, plotType, plotTypeGui, configger,
                                    tcListener );
        Action[] stackActions = controlManager.getStackActions();

        /* Action for deleting a control from the stack. */
        Action removeAction =
            stack_.createRemoveAction( "Remove Current Layer",
                                       "Delete the current layer control"
                                     + " from the stack" );

        /* Prepare the panel containing the user controls.  This may appear
         * either at the bottom of the plot window or floated into a
         * separate window. */
        final ControlStackPanel stackPanel = new ControlStackPanel( stack_ );
        stackPanel.addFixedControl( axisControl_ );
        stackPanel.addFixedControl( legendControl );

        /* The shader control is only visible in the stack when one of the
         * layers is making use of it. */
        stackModel_.addPlotActionListener( new ActionListener() {
            boolean hasShader;
            public void actionPerformed( ActionEvent evt ) {
                boolean requiresShader = hasShadedLayers( readPlotLayers() );
                if ( hasShader ^ requiresShader ) {
                    if ( requiresShader ) {
                        stackPanel.addFixedControl( shaderControl );
                    }
                    else {
                        stackPanel.removeFixedControl( shaderControl );
                    }
                    hasShader = requiresShader;
                }
            }
        } );

        /* Prepare the panel that holds the plot itself.  Blob drawing
         * is superimposed using an OverlayLayout. */
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout( new OverlayLayout( displayPanel ) );
        displayPanel.add( blobPanel_ );
        displayPanel.add( plotPanel_ );

        /* Prepare management of floating the control stack into a separate
         * window. */
        FloatManager floater =
            FloatManager
           .createFloatManager( getMainArea(), displayPanel, stackPanel );
        ToggleButtonModel floatModel = floater.getFloatToggle();
     
        /* Add actions etc to the toolbar. */
        if ( floatModel != null ) {
            getToolBar().add( floatModel.createToolbarButton() );
            getToolBar().addSeparator();
        }
        for ( int i = 0; i < stackActions.length; i++ ) {
            getToolBar().add( stackActions[ i ] );
        }
        getToolBar().add( removeAction );
        getToolBar().addSeparator();
        getToolBar().add( blobAction );
        getToolBar().add( fromVisibleAction );
        getToolBar().add( replotAction );
        getToolBar().add( resizeAction );
        if ( axlockModel != null ) {
            getToolBar().add( axlockModel.createToolbarButton() );
        }
        getToolBar().add( sketchModel.createToolbarButton() );
        getToolBar().add( showProgressModel_.createToolbarButton() );
        getToolBar().add( exportAction );
        getToolBar().addSeparator();

        /* Add actions etc to menus. */
        if ( floatModel != null ) {
            getFileMenu().insert( floatModel.createMenuItem(), 1 );
        }
        JMenu layerMenu = new JMenu( "Layers" );
        layerMenu.setMnemonic( KeyEvent.VK_L );
        for ( int i = 0; i < stackActions.length; i++ ) {
            layerMenu.add( stackActions[ i ] );
        }
        layerMenu.add( removeAction );
        getJMenuBar().add( layerMenu );
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        subsetMenu.add( blobAction );
        subsetMenu.add( fromVisibleAction );
        getJMenuBar().add( subsetMenu );
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( replotAction );
        plotMenu.add( resizeAction );
        if ( axlockModel != null ) {
            plotMenu.add( axlockModel.createMenuItem() );
        }
        plotMenu.add( sketchModel.createMenuItem() );
        plotMenu.add( showProgressModel_.createMenuItem() );
        getJMenuBar().add( plotMenu );
        JMenu exportMenu = new JMenu( "Export" );
        exportMenu.setMnemonic( KeyEvent.VK_E );
        exportMenu.add( exportAction );
        getJMenuBar().add( exportMenu );

        /* Place position and count status panels at the bottom of the
         * window. */
        JComponent statusLine = new JPanel( new GridLayout( 1, 2, 5, 0 ) );
        statusLine.setBorder( BorderFactory.createEmptyBorder( 4, 0, 0, 0 ) );
        statusLine.add( posLine );
        statusLine.add( countLine );
        JComponent cpanel = getControlPanel();
        cpanel.setLayout( new BoxLayout( cpanel, BoxLayout.X_AXIS ) );
        cpanel.add( statusLine );

        /* Try to set up a default control so that when the window opens
         * something gets plotted immediately. */
        Control dfltControl =
            controlManager.createDefaultControl( ControlWindow.getInstance()
                                                .getCurrentModel() );
        if ( dfltControl != null ) {
            stack_.addControl( dfltControl );
        }

        /* Set default component dimensions. */
        displayPanel.setMinimumSize( new Dimension( 150, 150 ) );
        displayPanel.setPreferredSize( new Dimension( 500, 400 ) );
        stackPanel.setMinimumSize( new Dimension( 200, 100 ) );
        stackPanel.setPreferredSize( new Dimension( 500, 240 ) );

        /* Place the plot and control components. */
        getMainArea().setLayout( new BorderLayout() );
        floater.init();
    }

    @Override
    public void dispose() {
        super.dispose();

        /* Ensure that the plot panel is not hanging on to expensive resources.
         * The plot panel, rather than this class, is where those things
         * are managed.
         * This is a hack: at time of writing, there is a memory leak 
         * preventing garbage collection of this window when it is disposed.
         * It applies to most or all of the topcat windows.  I think this is
         * through listeners to the application-wide lists of tables,
         * columns and subsets, possibly other things too.
         * It would be better to fix those leaks.
         * But in the mean time, clearing up here takes care of the
         * potentially large arrays etc. */
        plotPanel_.clearData();
    }

    /**
     * Acquires the list of requested plot layers from the GUI.
     *
     * @return  plot layer list
     */
    private PlotLayer[] readPlotLayers() {
        List<PlotLayer> layerList = new ArrayList<PlotLayer>();
        LayerControl[] controls = stackModel_.getActiveLayerControls();
        for ( int ic = 0; ic < controls.length; ic++ ) {
            PlotLayer[] layers = controls[ ic ].getPlotLayers();
            layerList.addAll( Arrays.asList( layers ) );
        }
        return layerList.toArray( new PlotLayer[ 0 ] );
    }

    /**
     * Returns the TopcatModel associated with a given DataSpec.
     *
     * @param  dataSpec   data spec
     * @return   topcat model supplying its table data
     */
    private TopcatModel getTopcatModel( DataSpec dataSpec ) {
        if ( dataSpec == null ) {
            return null;
        }
        LayerControl[] controls = stackModel_.getActiveLayerControls();
        for ( int ic = 0; ic < controls.length; ic++ ) {
            TopcatModel tcModel = controls[ ic ].getTopcatModel( dataSpec );
            if ( tcModel != null ) {
                return tcModel;
            }
        }
        return null;
    }

    /**
     * Returns the navigator currently in use for this window.
     *
     * @return  navigator
     */
    private Navigator<A> getNavigator() {
        return axisControl_.getNavigator();
    }

    /**
     * Highlights table points corresponding to a given graphics position.
     *
     * @param  point  reference graphics position, presumably indicated by user
     */
    private void identifyPoint( final Point point ) {
        final Factory<Map<TopcatModel,Long>> finder =
            createPointFinder( point );
        plotPanel_.submitPlotAnnotator( new Runnable() {
            public void run() {
                final Map<TopcatModel,Long> indexMap = finder.getItem();
                if ( indexMap != null ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            applyHighlights( indexMap );
                        }
                    } );
                }
            }
        } );
    }

    /**
     * Returns an object that can identify table row indices close to
     * a given screen position.
     *
     * @param  point  screen position to query
     * @return  factory that returns a map of topcat models to row indices
     *          giving rows whose markers are close to the point
     */
    private Factory<Map<TopcatModel,Long>>
            createPointFinder( final Point point ) {
        final Surface surface = plotPanel_.getSurface();
        final PlotLayer[] layers = getPointCloudLayers();
        final DataStore baseDataStore = plotPanel_.getDataStore();
        final boolean showProgress = showProgressModel_.isSelected();
        return new Factory<Map<TopcatModel,Long>>() {

            @Slow
            public Map<TopcatModel,Long> getItem() {

                /* Prepare a data store which will watch for interruptions
                 * and log progress. */
                final long nrow;
                if ( showProgress ) {
                    nrow = -1;
                }
                else {
                    long nr = 0;
                    for ( int il = 0; il < layers.length; il++ ) {
                        nr += ((GuiDataSpec) layers[ il ].getDataSpec())
                             .getRowCount();
                    }
                    nrow = nr;
                }
                DataStore dataStore =
                    plotPanel_.createGuiDataStore( nrow, baseDataStore );

                /* Prepare for iteration. */
                double[] dpos = new double[ surface.getDataDimCount() ];
                Point gp = new Point();
                double thresh2 = 4 * 4;
                Map<TopcatModel,Double> closeMap =
                    new HashMap<TopcatModel,Double>();
                Map<TopcatModel,Long> indexMap =
                    new HashMap<TopcatModel,Long>();

                /* Iterate over each usefully different layer. */
                for ( int il = 0; il < layers.length; il++ ) {
                    PlotLayer layer = layers[ il ];
                    DataGeom geom = layer.getDataGeom();
                    assert geom != null && geom.hasPosition();
                    DataSpec dataSpec = layer.getDataSpec();
                    TopcatModel tcModel = getTopcatModel( dataSpec );
                    TupleSequence tseq = dataStore.getTupleSequence( dataSpec );

                    /* Iterate over each visible point in the layer. */
                    while ( tseq.next() ) {
                        if ( geom.readDataPos( tseq, 0, dpos ) &&
                             surface.dataToGraphics( dpos, true, gp ) ) {

                            /* If the point is within a given threshold of our
                             * reference point, and it's closer than any other
                             * point we've encountered so far for the current
                             * table, record it. */
                            double dist2 = gp.distanceSq( point );
                            if ( dist2 < thresh2 ) {
                                Double c2 = closeMap.get( tcModel );
                                if ( c2 == null || c2.doubleValue() > dist2 ) {
                                    closeMap.put( tcModel, dist2 );
                                    indexMap.put( tcModel, tseq.getRowIndex() );
                                }
                            }
                        }
                    }
                    if ( Thread.currentThread().isInterrupted() ) {
                        return null;
                    }
                }

                /* Return a map of the closest row to the reference position
                 * for each visible table (only populated for each table if the
                 * point is within a given threshold - currently 4 pixels). */
                return indexMap;
            }
        };
    }

    /**
     * Takes a map indicating a row to highlight for zero or more tables,
     * and highlights the relevant rows.
     *
     * @param  indexMap  map from topcat models to row indices
     */
    private void applyHighlights( Map<TopcatModel,Long> indexMap ) {

        /* Message each the topcat model to highlight the relevant row.
         * This will in turn cause the plot panel to visually identify
         * these points (perhaps amongst other "activation" actions
         * unrelated to this plot). */
        int nHigh = 0;
        for ( Map.Entry<TopcatModel,Long> entry : indexMap.entrySet() ) {
            TopcatModel tcModel = entry.getKey();
            long irow = entry.getValue().longValue();
            assert tcModel != null;
            if ( tcModel != null ) {
                tcModel.highlightRow( irow );
                nHigh++;
            }
        }

        /* If no points were identified, clear the highlight list
         * for this plot. */
        if ( nHigh == 0 ) {
            plotPanel_.setHighlights( new HashMap<DataSpec,double[]>() );
        }
    }

    /**
     * Returns a list of plot layers which can be used as representatives
     * for the purpose of surveying point clouds.  If multiple layers
     * represent the same point cloud, only one of them will be returned.
     *
     * @return   representative point-cloud type layers for the current plot
     */
    private PlotLayer[] getPointCloudLayers() {
        PlotLayer[] layers = plotPanel_.getPlotLayers();
        Map<PointCloud,PlotLayer> cloudMap =
            new LinkedHashMap<PointCloud,PlotLayer>();
        for ( int il = 0; il < layers.length; il++ ) {
            PlotLayer layer = layers[ il ];
            DataGeom geom = layer.getDataGeom();
            DataSpec dataSpec = layer.getDataSpec();
            if ( dataSpec != null && geom != null && geom.hasPosition() ) {
                cloudMap.put( new PointCloud( geom, dataSpec ), layer );
            }
        }
        return cloudMap.values().toArray( new PlotLayer[ 0 ] );
    }

    /**
     * Highlights a given row for a given table in the currently displayed plot.
     * This method is called as a consequence of the TopcatEvent.ROW event.
     *
     * @param   tcModel   topcat model
     * @param   irow   row index
     */
    private void highlightRow( TopcatModel tcModel, long irow ) {
        StarTable highTable = tcModel.getDataModel();
        PlotLayer[] layers = plotPanel_.getPlotLayers();
        DataStore dataStore = plotPanel_.getDataStore();
        Surface surface = plotPanel_.getSurface();
        if ( surface == null ) {
            return;
        }
        Map<DataSpec,double[]> highMap =
            new LinkedHashMap<DataSpec,double[]>();
        for ( int il = 0; il < layers.length; il++ ) {
            PlotLayer layer = layers[ il ];
            DataSpec dataSpec = layer.getDataSpec();
            if ( dataSpec != null &&
                 dataSpec.getSourceTable() == highTable &&
                 ! highMap.containsKey( dataSpec ) ) {
                double[] dpos = getDataPos( layer, irow, dataStore );
                if ( dpos != null ) {
                    highMap.put( dataSpec, dpos );
                }
            }
        }

        /* Note at present each call to this method wipes out any previous
         * highlights.  Is that desired behaviour?  You might want it to
         * retain highlights for tables other than the one that is currently
         * being updated. */
        plotPanel_.setHighlights( highMap );
    }

    /**
     * Returns the data position for a given table row in a plot layer.
     *
     * @param  layer  plot layer
     * @param  irow   row index
     * @param  dataStore  data storage object
     * @return   data position if visible, else null
     */
    private static double[] getDataPos( PlotLayer layer, long irow,
                                        DataStore dataStore ) {
        DataGeom geom = layer.getDataGeom();
        if ( geom == null || ! geom.hasPosition() ) {
            return null;
        }
        double[] dpos = new double[ geom.getDataDimCount() ];
        TupleSequence tseq = dataStore.getTupleSequence( layer.getDataSpec() );

        /* Iterates over all rows, since random access is not defined for
         * tuple sequence.  Typical TupleSequence implementation means this
         * should be pretty fast though.  I think. */
        while ( tseq.next() ) {
            long ir = tseq.getRowIndex();
            if ( ir == irow && geom.readDataPos( tseq, 0, dpos ) ) {
                return dpos;
            }
            if ( ir > irow ) {
                break;
            }
        }
        return null;
    }

    /**
     * Returns an object to calculate a map of inclusion masks
     * for the currently visible points for each displayed table.
     *
     * @return   factory for table to subset map
     */
    private Factory<Map<TopcatModel,BitSet>> createVisibleMasker() {
        return createBlobMasker( null );
    }

    /**
     * Returns an object which can scan the current plot for table point
     * inclusion masks.
     * The actual scan may be slow for large data sets, so although this
     * method may be called on the event dispatch thread, the returned
     * factory's getItem method should not be.
     *
     * @param   blob  graphical region of interest; if null, corresponds to
     *                entire visible area
     * @return   factory for deferred calculation of bit masks
     */
    private Factory<Map<TopcatModel,BitSet>>
            createBlobMasker( final Shape blob ) {
        final Surface surface = plotPanel_.getSurface();
        final PlotLayer[] layers = plotPanel_.getPlotLayers();
        final TopcatModel[] tcModels = new TopcatModel[ layers.length ];
        final Map<TopcatModel,BitSet> maskMap =
            new LinkedHashMap<TopcatModel,BitSet>();
        long nrow = 0;
        for ( int il = 0; il < layers.length; il++ ) {
            PlotLayer layer = layers[ il ];
            DataGeom geom = layer.getDataGeom();
            TopcatModel tcModel = getTopcatModel( layer.getDataSpec() );
            tcModels[ il ] = tcModel;
            if ( tcModel != null && geom != null && geom.hasPosition() &&
                 ! maskMap.containsKey( tcModel ) ) {
                long nr = tcModel.getDataModel().getRowCount();
                nrow += nr;
                if ( ! maskMap.containsKey( tcModel ) ) {
                    maskMap.put( tcModel,
                                 new BitSet( Tables.checkedLongToInt( nr ) ) );
                }
            }
        }
        final DataStore dataStore = plotPanel_.createGuiDataStore( nrow );
        return new Factory<Map<TopcatModel,BitSet>>() {
            @Slow
            public Map<TopcatModel,BitSet> getItem() {
                double[] dpos = new double[ surface.getDataDimCount() ];
                Point gp = new Point();
                for ( int il = 0; il < layers.length; il++ ) {
                    PlotLayer layer = layers[ il ];
                    DataGeom geom = layer.getDataGeom();
                    DataSpec dataSpec = layer.getDataSpec();
                    TopcatModel tcModel = tcModels[ il ];
                    if ( tcModel != null &&
                         geom != null && geom.hasPosition() ) {
                        BitSet mask = maskMap.get( tcModel );
                        TupleSequence tseq =
                            dataStore.getTupleSequence( dataSpec );
                        while ( tseq.next() ) {
                            if ( geom.readDataPos( tseq, 0, dpos ) &&
                                 surface.dataToGraphics( dpos, true, gp ) &&
                                 ( blob == null || blob.contains( gp ) ) ) {
                                long ix = tseq.getRowIndex();
                                mask.set( Tables.checkedLongToInt( ix ) );
                            }
                        }
                    }
                }
                if ( Thread.currentThread().isInterrupted() ) {
                    return null;
                }
                assert ! maskMap.containsKey( null );
                maskMap.remove( null );
                return maskMap;
            }
        };
    }

    /**
     * Returns an object which can count the number of points visible in
     * the current plot.
     * The actual count may be slow for large data sets, so although this
     * method may be called on the event dispatch thread, the returned
     * factory's getItem method should not be.
     *
     * @return  factory for deferred calculation of formatted point count
     */
    private Factory<String> createCounter() {
        final PlotLayer[] layers = getPointCloudLayers();
        final DataStore dataStore = plotPanel_.getDataStore();
        final Surface surface = plotPanel_.getSurface();
        final int nl = layers.length;
        long nr = 0;
        TopcatModel[] tcModels = new TopcatModel[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            tcModels[ il ] = getTopcatModel( layers[ il ].getDataSpec() );
            assert tcModels[ il ] != null;
            nr += tcModels[ il ].getDataModel().getRowCount();
        }
        final long total = nr;
        return new Factory<String>() {
            @Slow
            public String getItem() {
                long start = System.currentTimeMillis();
                long count = 0;
                Point gp = new Point();
                double[] dpos = new double[ surface.getDataDimCount() ];
                for ( int il = 0; il < layers.length; il++ ) {
                    PlotLayer layer = layers[ il ];
                    DataGeom geom = layer.getDataGeom();
                    DataSpec dataSpec = layer.getDataSpec();
                    TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
                    while ( tseq.next() ) {
                        if ( geom.readDataPos( tseq, 0, dpos ) &&
                             surface.dataToGraphics( dpos, true, gp ) ) {
                            count++;
                        }
                        if ( Thread.currentThread().isInterrupted() ) {
                            return null;
                        }
                    }
                }
                PlotUtil.logTime( logger_, "Count", start );
                return TopcatUtils.formatLong( count ) + " / "
                     + TopcatUtils.formatLong( total );
            }
        };
    }

    /**
     * Adds subsets to topcat models given a map of tables to masks.
     *
     * @param   maskMap   map from topcat model to subset bit mask
     */
    private void addMaskSubsets( final Factory<Map<TopcatModel,BitSet>>
                                       maskMapFact ) {
        plotPanel_.submitPlotAnnotator( new Runnable() {
            public void run() {
                final Map<TopcatModel,BitSet> maskMap = maskMapFact.getItem();
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        if ( maskMap != null ) {
                            applyMasks( maskMap );
                        }
                        blobPanel_.setActive( false );
                    }
                } );
            }
        } );
    }

    /**
     * Takes a map from tables to new subset masks, presumably derived
     * from this plot, and passes them to the application.
     *
     * @param  maskMap   map from tables to associated row subset bitmasks
     */
    private void applyMasks( Map<TopcatModel,BitSet> maskMap ) {

        /* Purge empty masks. */
        for ( Iterator<BitSet> it = maskMap.values().iterator();
              it.hasNext(); ) {
            BitSet mask = it.next();
            if ( mask.cardinality() == 0 ) {
                it.remove();
            }
        }
        if ( maskMap.isEmpty() ) {
            return;
        }

        /* Pass masks to topcat models to incorporate. */
        TopcatModel tcModel0 = maskMap.keySet().iterator().next();
        SubsetConsumer subsetConsumer =
            tcModel0.enquireNewSubsetConsumer( this );
        if ( subsetConsumer == null ) {
            return;
        }
        for ( Map.Entry<TopcatModel,BitSet> entry : maskMap.entrySet() ) {
            TopcatModel tcModel = entry.getKey();
            BitSet mask = entry.getValue();
            assert tcModel != null;
            if ( tcModel != null ) {
                subsetConsumer.consumeSubset( tcModel, mask );
            }
        }
    }

    /**
     * Invoked when the plot changes.  Status panels are updated.
     */
    private void plotChanged() {

        /* Update position immediately. */
        displayPosition( plotPanel_.getMousePosition() );

        /* Initiate updating point count, which may be slow. */
        final Factory<String> counter = createCounter();
        plotPanel_.submitExtraAnnotator( new Runnable() {
            public void run() {
                final String txt = counter.getItem();
                if ( txt != null ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            countLabel_.setText( txt );
                        }
                    } );
                }
            }
        } );
    }

    /**
     * Displays the formatted position at a given point in the status panel.
     *
     * @param  point  cursor position
     */
    private void displayPosition( Point point ) {
        String pos = null;
        if ( point != null ) {
            Surface surface = plotPanel_.getSurface();
            if ( surface != null &&
                 surface.getPlotBounds().contains( point ) ) {
                double[] dataPos = surface.graphicsToData( point, null );
                if ( dataPos != null ) {
                    pos = surface.formatPosition( dataPos );
                }
            }
        }
        posLabel_.setText( pos );
    }

    /**
     * Indicates whether any of the submitted list of plot layers
     * makes use of a colour scale.
     *
     * @param  layers  plot layers
     * @return   true iff any uses an aux colour shader
     */
    private static boolean hasShadedLayers( PlotLayer[] layers ) {
        for ( int il = 0; il < layers.length; il++ ) {
            if ( layers[ il ].getAuxRangers().keySet()
                                             .contains( AuxScale.COLOR ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mouse listener which listens for click events that identify a point.
     */
    private class IdentifyListener extends MouseAdapter {
        @Override
        public void mouseClicked( MouseEvent evt ) {
            int iButt = evt.getButton();
            if ( iButt == MouseEvent.BUTTON1 &&
                 ! ( evt.isAltDown() ||
                     evt.isControlDown() ||
                     evt.isMetaDown() ||
                     evt.isShiftDown() ) ) {
                identifyPoint( evt.getPoint() );
            }
        }
    }
}
