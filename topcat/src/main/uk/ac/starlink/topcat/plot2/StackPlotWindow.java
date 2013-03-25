package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.OverlayLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
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
import uk.ac.starlink.topcat.plot.BlobPanel;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.PointCloud;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.ZoomListener;
import uk.ac.starlink.ttools.plot2.data.CachedDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.MemoryColumnFactory;
import uk.ac.starlink.ttools.plot2.data.SmartColumnFactory;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Window for all plots.
 * This is generic and currently not expected to be subclassed;
 * plot-type-specific behaviour is defined by supplied PlotType and
 * PlotTypeGui objects.
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
    private final JComponent stackPanel_;
    private final JComponent displayPanel_;
    private final JLabel posLabel_;
    private final JLabel countLabel_;
    private final ToggleButtonModel floatModel_;
    private JDialog floater_;
    private static final double CLICK_ZOOM_UNIT = 1.2;
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
        ShaderControl shaderControl =
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

        /* Set up a plot panel with the objects it needs to gather plot
         * requirements from the GUI.  This does the actual plotting. */
        plotPanel_ =
            new PlotPanel<P,A>( storeFact, axisControl_, layerFact, legendFact,
                                legendPosFact, shaderControl,
                                plotType.getPaperTypeSelector() );

        /* Ensure that the plot panel is messaged when a GUI action occurs
         * that might change the plot appearance.  Each of these controls
         * is forwarding actions from all of its constituent controls. */
        stackModel_.addPlotActionListener( plotPanel_ );
        legendControl.addActionListener( plotPanel_ );
        axisControl_.addActionListener( plotPanel_ );
        if ( shaderControl != null ) {
            shaderControl.addActionListener( plotPanel_ );
        }

        /* Arrange for user gestures (zoom, pan, click) on the plot panel
         * itself to result in appropriate actions. */
        plotPanel_.setFocusable( true );
        new ZoomListener() {
            @Override public void zoom( int nZoom, Point point ) {
                StackPlotWindow.this.zoom( nZoom, point );
            };
        }.install( plotPanel_ );
        addMouseInputListener( plotPanel_, new PanListener() );

        /* Prepare a panel that reports current cursor position. */
        posLabel_ = new JLabel();
        JComponent posLine = new LineBox( "Position", posLabel_ );
        posLine.setBorder( BorderFactory.createEtchedBorder() );
        addMouseInputListener( plotPanel_, new PosListener() );

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
                addMaskSubsets( getVisibleMasks() );
            }
        };

        /* Prepare the action that allows the user to select points by
         * hand-drawn region. */
        final BlobPanel blobPanel = new BlobPanel() {
            protected void blobCompleted( Shape blob ) {
                addMaskSubsets( createBlobMasker( blob ).getItem() );
            }
        };
        blobPanel.setFocusable( true ); // necessary to transmit focus to plot
        stackModel_.addPlotActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                blobPanel.setActive( false );
            }
        } );
        Action blobAction = blobPanel.getBlobAction();

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

        /* Prepare non-contextual zoom actions. */
        Action zoomInAction = new ZoomAction( true );
        Action zoomOutAction = new ZoomAction( false );
 
        /* Prepare the action for floating the control panel. */
        floatModel_ =
            new ToggleButtonModel( "Float Controls", ResourceIcon.FLOAT,
                                   "Present plot controls in a floating window "
                                 + "rather than below the plot" );
        floatModel_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                placeControls();
            }
        } );

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
        Action[] stackActions = controlManager.createStackActions();

        /* Action for deleting a control from the stack. */
        Action removeAction =
            stack_.createRemoveAction( "Remove Current Layer",
                                       "Delete the current layer control"
                                     + " from the stack" );
     
        /* Add actions etc to the toolbar. */
        getToolBar().add( floatModel_.createToolbarButton() );
        getToolBar().addSeparator();
        getToolBar().add( exportAction );
        getToolBar().add( replotAction );
        getToolBar().add( resizeAction );
        getToolBar().add( zoomInAction );
        getToolBar().add( zoomOutAction );
        if ( axlockModel != null ) {
            getToolBar().add( axlockModel.createToolbarButton() );
        }
        getToolBar().add( fromVisibleAction );
        getToolBar().add( blobAction );
        getToolBar().addSeparator();
        for ( int i = 0; i < stackActions.length; i++ ) {
            getToolBar().add( stackActions[ i ] );
        }
        getToolBar().add( removeAction );
        getToolBar().addSeparator();

        /* Add actions etc to menus. */
        JMenu exportMenu = new JMenu( "Export" );
        exportMenu.setMnemonic( KeyEvent.VK_E );
        exportMenu.add( exportAction );
        getJMenuBar().add( exportMenu );
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( replotAction );
        plotMenu.add( resizeAction );
        plotMenu.add( zoomInAction );
        plotMenu.add( zoomOutAction );
        if ( axlockModel != null ) {
            plotMenu.add( axlockModel.createMenuItem() );
        }
        getJMenuBar().add( plotMenu );
        JMenu subsetMenu = new JMenu( "Subsets" );
        subsetMenu.setMnemonic( KeyEvent.VK_S );
        subsetMenu.add( fromVisibleAction );
        subsetMenu.add( blobAction );
        getJMenuBar().add( subsetMenu );
        JMenu layerMenu = new JMenu( "Layers" );
        layerMenu.setMnemonic( KeyEvent.VK_L );
        for ( int i = 0; i < stackActions.length; i++ ) {
            layerMenu.add( stackActions[ i ] );
        }
        layerMenu.add( removeAction );
        getJMenuBar().add( layerMenu );

        /* Add standard help actions. */
        addHelp( name );

        /* Prepare the panel containing the user controls.  This may appear
         * either at the bottom of the plot window or floated into a
         * separate window. */
        Control[] fixedControls = new Control[] {
            axisControl_,
            shaderControl,
            legendControl,
        };
        stackPanel_ = new ControlStackPanel( fixedControls, stack_ );

        /* Prepare the panel that holds the plot itself.  Blob drawing
         * is superimposed using an OverlayLayout. */
        displayPanel_ = new JPanel();
        displayPanel_.setLayout( new OverlayLayout( displayPanel_ ) );
        displayPanel_.add( blobPanel );
        displayPanel_.add( plotPanel_ );

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
        displayPanel_.setMinimumSize( new Dimension( 150, 150 ) );
        displayPanel_.setPreferredSize( new Dimension( 500, 400 ) );
        stackPanel_.setMinimumSize( new Dimension( 200, 100 ) );
        stackPanel_.setPreferredSize( new Dimension( 500, 240 ) );

        /* Place the plot and control components. */
        getMainArea().setLayout( new BorderLayout() );
        placeControls();
   }

   /**
    * Places the controls in appropriate windows.
    * They may go in the same window or in two separate windows according
    * to whether the control panel is currently requested to be floating.
    */
   private void placeControls() {
        boolean external = floatModel_.isSelected();
        JComponent main = getMainArea();
        main.removeAll();
        if ( floater_ != null ) {
            floater_.getContentPane().removeAll();
            floater_.dispose();
            floater_ = null;
        }
        if ( external ) {
            main.add( displayPanel_ );

            /* This should possibly be a JFrame rather than a JDialog.
             * If it was a JFrame it could go under its controlling window,
             * which might be useful for screen management.
             * If so, I'd need to add a WindowListener to make sure that
             * the floater closes and iconifies when the parent does.
             * Any other Dialog behaviour I'd need to add by hand? */
            floater_ = new JDialog( this );
            floater_.getContentPane().setLayout( new BorderLayout() );
            floater_.getContentPane().add( stackPanel_ );
            floater_.pack();
            floater_.setVisible( true );
            floater_.addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent evt ) {
                    floatModel_.setSelected( false );
                    placeControls();
                }
            } );
        }
        else {
            JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT,
                                                  displayPanel_, stackPanel_ );
            splitter.setResizeWeight( 0.75 );
            splitter.setOneTouchExpandable( true );
            main.add( splitter, BorderLayout.CENTER );
        }
        main.validate();
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
     * Sets the plot surface aspect to a given new value.
     *
     * @param   aspect  new aspect
     */
    private void fixAspect( A aspect ) {
        if ( aspect != null ) {
            axisControl_.setAspect( aspect );
            plotPanel_.replot();
        }
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
     * Highlights table points corresponding to a given graphics position.
     *
     * @param  point  reference graphics position, presumably indicated by user
     */
    private void identifyPoint( Point point ) {

        /* Acquire a list of data layers that we need to examine.
         * We go through all the visible layers and hash them by the value
         * of the corresponding PointCloud.  The point of this is that we
         * may have several layers with the same point cloud (position
         * sequence) and there's only any point in treating each position
         * sequence once, rather than doing it once for each layer. */
        PlotLayer[] visibleLayers = plotPanel_.getPlotLayers();
        Map<PointCloud,PlotLayer> cloudMap =
            new HashMap<PointCloud,PlotLayer>();
        for ( int il = 0; il < visibleLayers.length; il++ ) {
            PlotLayer layer = visibleLayers[ il ];
            DataGeom geom = layer.getDataGeom();
            DataSpec dataSpec = layer.getDataSpec();
            if ( dataSpec != null && geom != null ) {
                cloudMap.put( new PointCloud( geom, dataSpec ), layer );
            }
        }
        Collection<PlotLayer> uniqueLayers = cloudMap.values();

        /* Iterate over each usefully different layer. */
        Surface surface = plotPanel_.getSurface();
        DataStore dataStore = plotPanel_.getDataStore();
        double[] dpos = new double[ surface.getDataDimCount() ];
        Point gp = new Point();
        double thresh2 = 4 * 4;
        Map<TopcatModel,Double> closeMap = new HashMap<TopcatModel,Double>();
        Map<TopcatModel,Long> indexMap = new HashMap<TopcatModel,Long>();
        for ( PlotLayer layer : uniqueLayers ) {

            /* Iterate over each visible point in the layer. */
            DataGeom geom = layer.getDataGeom();
            DataSpec dataSpec = layer.getDataSpec();
            TopcatModel tcModel = getTopcatModel( dataSpec );
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec );
            while ( tseq.next() ) {
                if ( geom.readDataPos( tseq, 0, dpos ) &&
                     surface.dataToGraphics( dpos, true, gp ) ) {

                    /* If the point is within a given threshold of our
                     * reference point, and it's closer than any other
                     * point we've encountered so far for the current table,
                     * record it. */
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
        }

        /* We now have a map of the closest point to the reference position
         * for each visible table (only populated for each table if the
         * point is within a given threshold - currently 4 pixels).
         * Message the topcat model in each case to highlight that row.
         * This will in turn cause the plot panel to visually identify
         * these points (perhaps amongst other actions unrelated to
         * this plot). */
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

        /* If no points were identified, clear the highlight list for this
         * plot. */
        if ( nHigh == 0 ) {
            plotPanel_.setHighlights( new HashMap<DataSpec,double[]>() );
        }
    }

    /**
     * Returns a map of inclusion masks for the currently visible points
     * for each displayed table.
     *
     * @return   table to subset map
     */
    private Map<TopcatModel,BitSet> getVisibleMasks() {
        return createBlobMasker( null ).getItem();
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
        final DataStore dataStore = plotPanel_.getDataStore();
        final PlotLayer[] layers = plotPanel_.getPlotLayers();
        final TopcatModel[] tcModels = new TopcatModel[ layers.length ];
        for ( int il = 0; il < layers.length; il++ ) {
            tcModels[ il ] = getTopcatModel( layers[ il ].getDataSpec() );
        }
        return new Factory<Map<TopcatModel,BitSet>>() {
            @Slow
            public Map<TopcatModel,BitSet> getItem() {
                Map<TopcatModel,BitSet> maskMap =
                    new LinkedHashMap<TopcatModel,BitSet>();
                double[] dpos = new double[ surface.getDataDimCount() ];
                Point gp = new Point();
                for ( int il = 0; il < layers.length; il++ ) {
                    PlotLayer layer = layers[ il ];
                    DataGeom geom = layer.getDataGeom();
                    DataSpec dataSpec = layer.getDataSpec();
                    TopcatModel tcModel = tcModels[ il ];
                    if ( tcModel != null && geom != null ) {
                        if ( ! maskMap.containsKey( tcModel ) ) {
                            int nrow =
                                Tables.checkedLongToInt( tcModel.getDataModel()
                                                        .getRowCount() );
                            maskMap.put( tcModel, new BitSet( nrow ) );
                        }
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
                            if ( Thread.currentThread().isInterrupted() ) {
                                return null;
                            }
                        }
                    }
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
        final Factory<Map<TopcatModel,BitSet>> masker =
            createBlobMasker( null );
        return new Factory<String>() {
            @Slow
            public String getItem() {
                long start = System.currentTimeMillis();

                /* Do the scan. */
                Map<TopcatModel,BitSet> maskMap = masker.getItem();
                if ( Thread.currentThread().isInterrupted() ) {
                    return null;
                }

                /* Analyse the calculated masks to generate a
                 * formatted count string. */
                if ( maskMap == null ) {
                    return null;
                }
                else {
                    long total = 0;
                    long count = 0;
                    for ( TopcatModel tcModel : maskMap.keySet() ) {
                        BitSet mask = maskMap.get( tcModel );
                        total += tcModel.getDataModel().getRowCount();
                        count += mask.cardinality();
                    }
                    PlotUtil.logTime( logger_, "Count", start );
                    return TopcatUtils.formatLong( count ) + " / "
                         + TopcatUtils.formatLong( total );
                }
            }
        };
    }

    /**
     * Adds subsets to topcat models given a map of tables to masks.
     *
     * @param   maskMap   map from topcat model to subset bit mask
     */
    private void addMaskSubsets( Map<TopcatModel,BitSet> maskMap ) {

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
        plotPanel_.submitAnnotator( new GuiFuture<String>( createCounter() ) {
            protected void acceptValue( String txt, boolean success ) {
                countLabel_.setText( success ? txt : null );
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
     * Performs a zoom around a given point.
     *
     * @param   nZoom  number of zoom increments (negative means zoom out)
     * @param   point  zoom reference point
     */
    private void zoom( int nZoom, Point point ) {
        if ( nZoom != 0 ) {
            double factor = Math.pow( CLICK_ZOOM_UNIT, nZoom );
            Surface surface = plotPanel_.getLatestSurface();
            if ( surface != null &&
                 surface.getPlotBounds().contains( point ) ) {
                fixAspect( surfFact_.zoom( surface, point, factor ) );
            }
        }
    }

    /**
     * Utility method to add a mouse input listener as both a MouseListener
     * and a MouseMotionListener to a component.
     *
     * @param   comp  target component
     * @param   lnr   listener for mouse button and motion events
     */
    private static void addMouseInputListener( JComponent comp,
                                               MouseInputListener lnr ) {
        comp.addMouseListener( lnr );
        comp.addMouseMotionListener( lnr );
    }

    /**
     * Action to zoom about the center of the plot.
     */
    private class ZoomAction extends BasicAction {
        private final int nZoom_;

        /**
         * Constructor.
         *
         * @param  in   true to zoom in, false to zoom out
         */
        ZoomAction( boolean in ) {
            super( "Zoom " + ( in ? "In" : "Out" ),
                   in ? ResourceIcon.ZOOM_IN : ResourceIcon.ZOOM_OUT,
                   "Zoom " + ( in ? "in" : "out" )
                           + " around the center of the plot" );
            nZoom_ = in ? +1 : -1;
        }
        public void actionPerformed( ActionEvent evt ) {
            Surface surf = plotPanel_.getSurface();
            if ( surf != null ) {
                Rectangle bounds = surf.getPlotBounds();
                Point p = new Point( bounds.x + bounds.width / 2,
                                     bounds.y + bounds.height / 2 );
                zoom( nZoom_, p );
            }
        }
    }

    /**
     * Mouse listener which implements dragging the plot around,
     * clicking to identify points, and re-centering (right click).
     */
    private class PanListener extends MouseInputAdapter {
        private Surface dragSurface_;
        private Point startPoint_;

        @Override
        public void mousePressed( MouseEvent evt ) {
            Surface surface = plotPanel_.getLatestSurface();
            Point point = evt.getPoint();
            if ( surface != null &&
                 surface.getPlotBounds().contains( point ) ) {
                dragSurface_ = surface;
                startPoint_ = point;
            }
        }

        @Override
        public void mouseDragged( MouseEvent evt ) {
            if ( dragSurface_ != null ) {
                fixAspect( surfFact_.pan( dragSurface_, startPoint_,
                                          evt.getPoint() ) );
            }
        }

        @Override
        public void mouseReleased( MouseEvent evt ) {
            dragSurface_ = null;
            startPoint_ = null;
        }

        @Override
        public void mouseClicked( MouseEvent evt ) {
            int iButt = evt.getButton();
            if ( iButt == MouseEvent.BUTTON1 ) {
                identifyPoint( evt.getPoint() );
            }
            else if ( iButt == MouseEvent.BUTTON3 ) {
                Surface surface = plotPanel_.getSurface();
                Iterable<double[]> dpIt =
                    new PointCloud( plotPanel_.getPlotLayers(), true )
                   .createDataPosIterable( plotPanel_.getDataStore() );
                double[] dpos = surface.graphicsToData( evt.getPoint(), dpIt );
                if ( dpos != null ) {
                    fixAspect( surfFact_.center( surface, dpos ) );
                }
            }
        }
    }

    /**
     * Mouse listener which causes update of the cursor position status
     * panel whenever the mouse is moved.
     */
    private class PosListener extends MouseInputAdapter {
        @Override
        public void mouseEntered( MouseEvent evt ) {
            displayPosition( evt.getPoint() );
        }

        @Override
        public void mouseMoved( MouseEvent evt ) {
            displayPosition( evt.getPoint() );
        }

        @Override
        public void mouseExited( MouseEvent evt ) {
            displayPosition( null );
        }
    }
}
