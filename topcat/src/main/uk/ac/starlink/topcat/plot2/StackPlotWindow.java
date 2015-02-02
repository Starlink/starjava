package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
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
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.HelpAction;
import uk.ac.starlink.topcat.LineBox;
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
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Gesture;
import uk.ac.starlink.ttools.plot2.IndicatedRow;
import uk.ac.starlink.ttools.plot2.NavigationListener;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.CachedDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.MemoryColumnFactory;
import uk.ac.starlink.ttools.plot2.data.SmartColumnFactory;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Compositor;

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
    private final PlotTypeGui<P,A> plotTypeGui_;
    private final AxisController<P,A> axisController_;
    private final SurfaceFactory<P,A> surfFact_;
    private final PlotPanel<P,A> plotPanel_;
    private final ControlStack stack_;
    private final ControlStackModel stackModel_;
    private final ControlManager controlManager_;
    private final ToggleButtonModel showProgressModel_;
    private final JLabel posLabel_;
    private final JLabel countLabel_;
    private final NavigationHelpPanel navPanel_;
    private final BlobPanel2 blobPanel_;
    private final Action blobAction_;
    private final Action fromVisibleAction_;
    private final Action resizeAction_;
    private final boolean canSelectPoints_;
    private final JMenu exportMenu_;
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
        plotTypeGui_ = plotTypeGui;
        canSelectPoints_ = plotTypeGui.hasPositions();

        /* Use a compositor with a fixed boost.  Maybe make the compositor
         * implementation controllable from the GUI at some point, but
         * the replot machinery currently assumes that PaperType is fixed
         * (does not check whether it's changed between replot requests)
         * so a bit of re-engineering would be required. */
        final Compositor compositor = new Compositor.BoostCompositor( 0.05f );

        /* Set up user interface components in the window that can gather
         * all the information required to perform (re-)plots. */
        stack_ = new ControlStack();
        stackModel_ = stack_.getStackModel();
        MultiConfigger configger = new MultiConfigger();
        axisController_ = plotTypeGui.createAxisController( stack_ );
        final FrameControl frameControl = new FrameControl();
        Factory<PlotPosition> posFact = new Factory<PlotPosition>() {
            public PlotPosition getItem() {
                return frameControl.getPlotPosition();
            }
        };
        Factory<String> titleFact = new Factory<String>() {
            public String getItem() {
                return frameControl.getPlotTitle();
            }
        };
        ToggleButtonModel axlockModel = axisController_.getAxisLockModel();
        surfFact_ = axisController_.getSurfaceFactory();
        configger.addConfigger( axisController_ );
        final ShaderControl shaderControl =
            new ShaderControl( stackModel_, configger );
        configger.addConfigger( shaderControl );
        DataStoreFactory storeFact =
            new CachedDataStoreFactory(
                new SmartColumnFactory( new MemoryColumnFactory() ) );
        Factory<PlotLayer[]> layerFact = new Factory<PlotLayer[]>() {
            public PlotLayer[] getItem() {
                return readPlotLayers( true );
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
        showProgressModel_.setSelected( true );
        final ToggleButtonModel navdecModel =
            new ToggleButtonModel( "Show Navigation Graphics",
                                   ResourceIcon.NAV_DEC,
                                   "Give visual feedback for plot navigation "
                                 + "gestures" );
        navdecModel.setSelected( true );

        /* Set up a plot panel with the objects it needs to gather plot
         * requirements from the GUI.  This does the actual plotting. */
        plotPanel_ =
            new PlotPanel<P,A>( storeFact, axisController_, layerFact,
                                posFact, legendFact, legendPosFact, titleFact,
                                shaderControl, sketchModel,
                                plotType.getPaperTypeSelector(), compositor,
                                placeProgressBar().getModel(),
                                showProgressModel_ );

        /* Ensure that the plot panel is messaged when a GUI action occurs
         * that might change the plot appearance.  Each of these controls
         * is forwarding actions from all of its constituent controls. */
        stackModel_.addPlotActionListener( plotPanel_ );
        frameControl.addActionListener( plotPanel_ );
        legendControl.addActionListener( plotPanel_ );
        axisController_.addActionListener( plotPanel_ );
        shaderControl.addActionListener( plotPanel_ );
        navdecModel.addActionListener( plotPanel_ );

        /* Arrange for user navigation actions to adjust the view. */
        new GuiNavigationListener<A>( plotPanel_ ) {
            public Navigator<A> getNavigator() {
                return axisController_.getNavigator();
            }
            public void setAspect( A aspect ) {
                axisController_.setAspect( aspect );
                plotPanel_.replot();
            }
            public void setDecoration( Decoration navDec ) {
                if ( navdecModel.isSelected() ) {
                    plotPanel_.setNavDecoration( navDec );
                }
            }
        }.addListeners( plotPanel_ );

        /* Arrange to update the GUI appropriately when the user moves the
         * mouse around. */
        axisController_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updatePositionDisplay( plotPanel_.getMousePosition() );
            }
        } );

        /* Arrange for user clicks to identify points. */
        if ( canSelectPoints_ ) {
            plotPanel_.addMouseListener( new IdentifyListener() );
        }

        /* Prepare a panel that reports current cursor position. */
        posLabel_ = new JLabel();
        JComponent posLine = new LineBox( "Position", posLabel_ );
        posLine.setBorder( BorderFactory.createEtchedBorder() );
        plotPanel_.addMouseListener( new MouseAdapter() {
            public void mouseEntered( MouseEvent evt ) {
                updatePositionDisplay( evt.getPoint() );
            }
            public void mouseExited( MouseEvent evt ) {
                updatePositionDisplay( null );
            }
        } );
        plotPanel_.addMouseMotionListener( new MouseMotionAdapter() {
            public void mouseMoved( MouseEvent evt ) {
                updatePositionDisplay( evt.getPoint() );
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
        fromVisibleAction_ =
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
        blobAction_ = blobPanel_.getBlobAction();

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
                                         plotType_.getPaperTypeSelector(),
                                         compositor );
            }
        };

        /* Prepare the plot rescale action. */
        resizeAction_ =
                new BasicAction( "Rescale", ResourceIcon.RESIZE,
                                 "Rescale plot to view all plotted data" ) {
            public void actionPerformed( ActionEvent evt ) {
                axisController_.setAspect( null );
                axisController_.setRanges( null );
                axisController_.clearAspect();
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
        controlManager_ =
            new GangControlManager( stack_, plotType, plotTypeGui, configger,
                                    tcListener );

        /* Prepare actions for adding and removing stack controls. */
        Action[] stackActions = controlManager_.getStackActions();
        Action removeAction =
            stack_.createRemoveAction( "Remove Current Control",
                                       "Delete the current layer control"
                                     + " from the stack" );

        /* Prepare the panel containing the user controls.  This may appear
         * either at the bottom of the plot window or floated into a
         * separate window. */
        JToolBar stackToolbar = new JToolBar();
        final ControlStackPanel stackPanel =
            new ControlStackPanel( stack_, stackToolbar );
        stackPanel.addFixedControl( frameControl );
        Control[] axisControls = axisController_.getControls();
        for ( int i = 0; i < axisControls.length; i++ ) {
            stackPanel.addFixedControl( axisControls[ i ] );
        }
        stackPanel.addFixedControl( legendControl );

        /* The shader control is only visible in the stack when one of the
         * layers is making use of it. */
        stackModel_.addPlotActionListener( new ActionListener() {
            boolean hasShader;
            public void actionPerformed( ActionEvent evt ) {
                boolean requiresShader =
                    hasShadedLayers( readPlotLayers( false ) );
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

        /* Place position and count status panels at the bottom of the
         * window. */
        JComponent cpanel = getControlPanel();
        cpanel.setLayout( new BoxLayout( cpanel, BoxLayout.Y_AXIS ) );
        JComponent statusLine = new JPanel( new GridLayout( 1, 2, 5, 0 ) );
        statusLine.setBorder( BorderFactory.createEmptyBorder( 4, 0, 0, 0 ) );
        statusLine.add( posLine );
        statusLine.add( countLine );
        cpanel.add( statusLine );

        /* Get action to provide plot-specific navigation help. */
        String navHelpId = axisController_.getNavigatorHelpId();
        Action navHelpAction =
              navHelpId != null && HelpAction.helpIdExists( navHelpId )
            ? new HelpAction( navHelpId, this )
            : null;

        /* Place mouse hints panel at the bottom of the window, with
         * actions to hide it. */
        final JComponent navhelpHolder = Box.createVerticalBox();
        navPanel_ = new NavigationHelpPanel();
        navPanel_.setBorder( BorderFactory.createEmptyBorder( 2, 0, 0, 0 ) );
        final JComponent navhelpLine = Box.createHorizontalBox();
        final ToggleButtonModel navhelpModel =
            new ToggleButtonModel( "Show Navigation Help",
                                   ResourceIcon.NAV_HELP,
                                   "Display mouse action hints"
                                 + " at the bottom of the window" );
        navhelpModel.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                if ( navhelpModel.isSelected() ) {
                    navhelpHolder.add( navhelpLine );
                }
                else {
                    navhelpHolder.remove( navhelpLine );
                }
                navhelpHolder.revalidate();
            }
        } );
        navPanel_.setAlignmentY( 0.5f );
        JButton hideNavButt = new JButton( null, ResourceIcon.SMALL_CLOSE );
        hideNavButt.setMargin( new Insets( 0, 0, 0, 0 ) );
        hideNavButt.setModel( navhelpModel );
        hideNavButt.setAlignmentY( 0.5f );
        navhelpLine.add( hideNavButt );
        navhelpLine.add( Box.createHorizontalStrut( 5 ) );
        if ( navHelpAction != null ) {
            JButton helpNavButt = new JButton( null, ResourceIcon.SMALL_HELP );
            helpNavButt.setMargin( new Insets( 0, 0, 0, 0 ) );
            helpNavButt.addActionListener( navHelpAction );
            helpNavButt.setAlignmentY( 0.5f );
            navhelpLine.add( helpNavButt );
            navhelpLine.add( Box.createHorizontalStrut( 5 ) );
        }
        navhelpLine.add( navPanel_ );
        navhelpHolder.add( navhelpLine );
        cpanel.add( navhelpHolder );
        navhelpModel.setSelected( true );
        updatePositionDisplay( null );

        /* Prepare management of floating the control stack into a separate
         * window. */
        FloatManager floater =
            FloatManager
           .createFloatManager( getMainArea(), displayPanel, stackPanel );
        ToggleButtonModel floatModel = floater.getFloatToggle();
     
        /* Add actions etc to the toolbars. */
        if ( floatModel != null ) {
            getToolBar().add( floatModel.createToolbarButton() );
            getToolBar().addSeparator();
            stackToolbar.add( floatModel.createToolbarButton() );
            stackToolbar.addSeparator();
        }
        if ( canSelectPoints_ ) {
            getToolBar().add( blobAction_ );
        }
        getToolBar().add( fromVisibleAction_ );
        getToolBar().add( replotAction );
        getToolBar().add( resizeAction_ );
        if ( axlockModel != null ) {
            getToolBar().add( axlockModel.createToolbarButton() );
        }
        getToolBar().add( sketchModel.createToolbarButton() );
        getToolBar().add( showProgressModel_.createToolbarButton() );
        getToolBar().add( exportAction );
        for ( int i = 0; i < stackActions.length; i++ ) {
            stackToolbar.add( stackActions[ i ] );
        }
        stackToolbar.addSeparator();
        stackToolbar.add( removeAction );

        /* Add actions etc to menus. */
        getWindowMenu().insert( navhelpModel.createMenuItem(), 1 );
        if ( floatModel != null ) {
            getWindowMenu().insert( floatModel.createMenuItem(), 1 );
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
        if ( canSelectPoints_ ) {
            subsetMenu.add( blobAction_ );
        }
        subsetMenu.add( fromVisibleAction_ );
        getJMenuBar().add( subsetMenu );
        JMenu plotMenu = new JMenu( "Plot" );
        plotMenu.setMnemonic( KeyEvent.VK_P );
        plotMenu.add( replotAction );
        plotMenu.add( resizeAction_ );
        if ( axlockModel != null ) {
            plotMenu.add( axlockModel.createMenuItem() );
        }
        plotMenu.add( sketchModel.createMenuItem() );
        plotMenu.add( showProgressModel_.createMenuItem() );
        plotMenu.add( navdecModel.createMenuItem() );
        getJMenuBar().add( plotMenu );
        exportMenu_ = new JMenu( "Export" );
        exportMenu_.setMnemonic( KeyEvent.VK_E );
        exportMenu_.add( exportAction );
        getJMenuBar().add( exportMenu_ );

        /* Set default component dimensions. */
        displayPanel.setMinimumSize( new Dimension( 150, 150 ) );
        displayPanel.setPreferredSize( new Dimension( 500, 400 ) );
        stackPanel.setMinimumSize( new Dimension( 200, 100 ) );
        stackPanel.setPreferredSize( new Dimension( 500, 240 ) );
        getBodyPanel().setBorder( BorderFactory
                                 .createEmptyBorder( 10, 10, 2, 10 ) );

        /* Place the plot and control components. */
        getMainArea().setLayout( new BorderLayout() );
        floater.init();

        /* The following focus magic is a workaround for some obscure
         * (buggy?) Swing behaviour.
         * It seems that if there are two toggle buttons with the same
         * button model, and one of them has initial focus, then the other
         * one does not receive an action the first time it's clicked.
         * That situation applies to this window as currently arranged:
         * by default, the first toolbar button gets the initial focus,
         * and the first toolbar button here uses floatModel which is
         * also used for a button in the stack panel.
         * By grabbing the focus for some other component, the problem
         * goes away.
         * Experiments suggest that the problem exists in Sun/Oracle
         * Java 1.5.0_22 but not 1.6.0_41, so probably this workaround
         * could be removed.  But I think it's harmless. */
        pack();
        plotPanel_.requestFocusInWindow();
    }

    /**
     * Returns the plot type used by this window.
     *
     * @return  GUI plot type
     */
    public PlotTypeGui<P,A> getPlotTypeGui() {
        return plotTypeGui_;
    }

    /**
     * Returns the stack containing controls which define what this
     * window is displaying.
     *
     * @return   control stack
     */
    public ControlStack getControlStack() {
        return stack_;
    }

    /**
     * Returns the manager object that controls this window's stack.
     *
     * @return   control manager
     */
    public ControlManager getControlManager() {
        return controlManager_;
    }

    /**
     * Returns this window's PlotPanel.
     *
     * @return  plot panel
     */
    public PlotPanel getPlotPanel() {
        return plotPanel_;
    }

    /**
     * Returns this window's AxisController.
     *
     * @return  axis controller
     */
    public AxisController getAxisController() {
        return axisController_;
    }

    /**
     * Returns this window's Export menu.
     *
     * @return  export menu
     */
    public JMenu getExportMenu() {
        return exportMenu_;
    }

    /**
     * Adds an action that is logically associated with rescaling the plot.
     * This takes the given action and inserts it into the toolbar and
     * menus in appropriate places.
     *
     * @param  act  action to add
     */
    public void insertRescaleAction( Action act ) {

        /* Insert into the toolbar.  Try to put it after the existing
         * Resize action, but if for some reason that doesn't exist,
         * the new one will just get appended at the end. */
        JToolBar toolbar = getToolBar();
        JButton actButton = toolbar.add( act );
        List<Component> comps =
            new ArrayList<Component>( Arrays
                                     .asList( toolbar.getComponents() ) );
        int iresize = -1;
        for ( int i = 0; i < comps.size() && iresize < 0; i++ ) {
            Component comp = comps.get( i );
            if ( comp instanceof JButton &&
                 isResizeAction( ((JButton) comp).getAction() ) ) {
                iresize = i;
            }
        }
        if ( iresize >= 0 ) {
            comps.remove( actButton );
            comps.add( iresize + 1, actButton );
            toolbar.removeAll();
            for ( Component c : comps ) {
                toolbar.add( c );
            }
        }

        /* Insert into menus as appropriate.  Any place the resize action
         * is found, place the new one after it. */
        JMenuBar menuBar = getJMenuBar();
        for ( int im = 0; im < menuBar.getMenuCount(); im++ ) {
            JMenu menu = menuBar.getMenu( im );
            for ( int ii = 0; ii < menu.getItemCount(); ii++ ) {
                JMenuItem item = menu.getItem( ii );
                if ( item != null && isResizeAction( item.getAction() ) ) {
                    menu.insert( act, ii + 1 );
                }
            }
        }
    }

    /**
     * Tests whether a given action corresponds to the Resize action.
     *
     * @return   true iff act is the Resize action
     */
    private boolean isResizeAction( Action act ) {
        return act == resizeAction_;
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
     * @param  activeOnly   if true, return only layers from active controls
     *                      (those which will actually be plotted)
     *                      if false, return even inactive ones
     * @return  plot layer list
     */
    private PlotLayer[] readPlotLayers( boolean activeOnly ) {
        List<PlotLayer> layerList = new ArrayList<PlotLayer>();
        LayerControl[] controls = stackModel_.getLayerControls( activeOnly );
        for ( int ic = 0; ic < controls.length; ic++ ) {
            PlotLayer[] layers = controls[ ic ].getPlotLayers();
            layerList.addAll( Arrays.asList( layers ) );
        }
        return layerList.toArray( new PlotLayer[ 0 ] );
    }

    /**
     * Returns the navigator currently in use for this window.
     *
     * @return  navigator
     */
    private Navigator<A> getNavigator() {
        return axisController_.getNavigator();
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
        final GuiPointCloud pointCloud = plotPanel_.createGuiPointCloud();

        /* Create and return the object that will do the work. */
        return new Factory<Map<TopcatModel,Long>>() {
            @Slow
            public Map<TopcatModel,Long> getItem() {

                /* Prepare a datastore which will watch for interruptions
                 * and possibly log progress. */
                DataStore dataStore = pointCloud.createGuiDataStore();

                /* Prepare for iteration. */
                TableCloud[] tclouds = pointCloud.getTableClouds();
                Map<TopcatModel,Double> closeMap =
                    new HashMap<TopcatModel,Double>();
                Map<TopcatModel,Long> indexMap =
                    new HashMap<TopcatModel,Long>();

                /* Iterate over each sub point cloud distinct positions. */
                for ( int ic = 0; ic < tclouds.length; ic++ ) {
                    TableCloud tcloud = tclouds[ ic ];
                    DataGeom geom = tcloud.getDataGeom();
                    int iPosCoord = tcloud.getPosCoordIndex();
                    TupleSequence tseq =
                        tcloud.createTupleSequence( dataStore );
                    IndicatedRow indicated =
                        PlotUtil.getClosestRow( surface, geom, iPosCoord, tseq,
                                                point );
                    if ( indicated != null ) {
                        long index = indicated.getIndex();
                        double distance = indicated.getDistance();
                        if ( distance <= PlotUtil.NEAR_PIXELS ) {
                            TopcatModel tcModel = tcloud.getTopcatModel();
                            Double closest = closeMap.get( tcModel );
                            if ( closest == null || distance < closest ) {
                                closeMap.put( tcModel, distance );
                                indexMap.put( tcModel, index );
                            }
                        }
                    }
                    if ( Thread.currentThread().isInterrupted() ) {
                        return null;
                    }
                }

                /* Return a map of the closest row to the reference position
                 * for each visible table (only populated for each table if the
                 * point is within a given threshold, NEAR_PIXELS. */
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
            plotPanel_.setHighlights( new HashMap<SubCloud,double[]>() );
        }
    }

    /**
     * Highlights a given row for a given table in the currently displayed plot.
     * This method is called as a consequence of the TopcatEvent.ROW event.
     *
     * @param   tcModel   topcat model
     * @param   irow   row index
     */
    private void highlightRow( TopcatModel tcModel, long irow ) {
        Surface surface = plotPanel_.getSurface();
        if ( surface == null ) {
            return;
        }
        Map<SubCloud,double[]> highMap = new LinkedHashMap<SubCloud,double[]>();
        SubCloud[] subClouds =
            SubCloud.createSubClouds( plotPanel_.getPlotLayers(), true );
        DataStore dataStore = plotPanel_.getDataStore();
        for ( int ic = 0; ic < subClouds.length; ic++ ) {
            SubCloud subCloud = subClouds[ ic ];
            if ( getTopcatModel( subCloud.getDataSpec() ) == tcModel ) {
                double[] dpos = getDataPos( subCloud, irow, dataStore );
                if ( dpos != null ) {
                    highMap.put( subCloud, dpos );
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
     * Returns the TopcatModel associated with a given DataSpec.
     *
     * @param  dataSpec   data spec
     * @return   topcat model supplying its table data
     */
    private static TopcatModel getTopcatModel( DataSpec dataSpec ) {
        return GuiDataSpec.getTopcatModel( dataSpec );
    }

    /**
     * Returns the data position for a given table row in a point cloud.
     *
     * @param  subCloud  point subcloud
     * @param  irow   row index
     * @param  dataStore  data storage object
     * @return   data position if visible, else null
     */
    private static double[] getDataPos( SubCloud subCloud, long irow,
                                        DataStore dataStore ) {
        DataGeom geom = subCloud.getDataGeom();
        int iPosCoord = subCloud.getPosCoordIndex();
        if ( geom == null ) {
            return null;
        }
        double[] dpos = new double[ geom.getDataDimCount() ];
        TupleSequence tseq =
            dataStore.getTupleSequence( subCloud.getDataSpec() );

        /* Iterates over all rows, since random access is not defined for
         * tuple sequence.  Typical TupleSequence implementation means this
         * should be pretty fast though.  I think. */
        while ( tseq.next() ) {
            long ir = tseq.getRowIndex();
            if ( ir == irow && geom.readDataPos( tseq, iPosCoord, dpos ) ) {
                return dpos;
            }
        }
        return null;
    }

    /**
     * Returns an object to calculate a map of masks giving inclusion
     * of currently plotted points in a given graphics shape on the screen.
     * The actual scan may be slow for large data sets, so although this
     * method may be called on the event dispatch thread, the returned
     * factory's getItem method should not be.
     * The object returned by the factory gives a row index inclusion mask
     * for each visible table.
     *
     * @param   blob  graphical region of interest
     * @return   factory for deferred calculation of bit masks
     */
    private Factory<Map<TopcatModel,BitSet>>
            createBlobMasker( final Shape blob ) {
        final GuiPointCloud pointCloud = plotPanel_.createGuiPointCloud();
        final PositionCriterion blobCriterion =
            PositionCriterion.createBlobCriterion( plotPanel_.getSurface(),
                                                   blob );
        return new Factory<Map<TopcatModel,BitSet>>() {
            @Slow
            public Map<TopcatModel,BitSet> getItem() {
                Map<TopcatModel,BitSet> maskMap =
                    new LinkedHashMap<TopcatModel,BitSet>();
                updateMasks( maskMap, pointCloud, blobCriterion );
                return Thread.currentThread().isInterrupted() ? null : maskMap;
            }
        };
    }

    /**
     * Returns an object to calculate a map of masks showing currently
     * visible points.
     * The actual scan may be slow for large data sets, so although this
     * method may be called on the event dispatch thread, the returned
     * factory's getItem method should not be.
     * The object returned by the factory gives a row index inclusion mask
     * for each visible table.
     *
     * @return   factory for deferred calculation of bit masks
     */
    private Factory<Map<TopcatModel,BitSet>> createVisibleMasker() {
        Surface surface = plotPanel_.getSurface();

        /* Prepare a criterion for inclusion of normal data positions
         * in the bounds of the plotting surface. */
        final GuiPointCloud pointCloud = plotPanel_.createGuiPointCloud();
        final PositionCriterion fullCriterion =
            PositionCriterion.createBoundsCriterion( surface );

        /* Prepare a criterion for appearance of partial data positions
         * in the bounds.
         * These partial positions are things like histogram data,
         * which have an X graphics position but not a Y graphics position.
         * For this case, either X or Y position within the plot bounds
         * counts as visibility. */
        final GuiPointCloud partialPointCloud =
            plotPanel_.createPartialGuiPointCloud();
        final PositionCriterion partialCriterion =
            PositionCriterion.createPartialBoundsCriterion( surface );

        /* Return an object that will scan the data given these criteria.
         * This isn't perfect, because this constitutes two scans through
         * the data (full and partial), and each one of those will update
         * the progress bar separately.  In practice, most plots will have
         * either full or partial points, but in the uncommon case where
         * there are both, you may see the progress bar scan through twice
         * for this operation. */
        return new Factory<Map<TopcatModel,BitSet>>() {
            @Slow
            public Map<TopcatModel,BitSet> getItem() {
                Map<TopcatModel,BitSet> maskMap =
                    new LinkedHashMap<TopcatModel,BitSet>();
                updateMasks( maskMap, pointCloud, fullCriterion );
                updateMasks( maskMap, partialPointCloud, partialCriterion );
                return Thread.currentThread().isInterrupted() ? null : maskMap;
            }
        };
    }

    /**
     * Scans through points in a point cloud, and updates
     * table->row_inclusion_mask map to indicate which rows in the tables
     * are included.
     *
     * @param   maskMap  map to update; will be populated with blank entries
     *                   as required
     * @param   pointCloud  sequence of points to assess
     * @param   criterion  condition for inclusion of a point
     */
    @Slow
    private static void updateMasks( Map<TopcatModel,BitSet> maskMap,
                                     GuiPointCloud pointCloud,
                                     PositionCriterion criterion ) {
        TableCloud[] tclouds = pointCloud.getTableClouds();
        DataStore dataStore = pointCloud.createGuiDataStore();
        int nc = tclouds.length;
        for ( int ic = 0;
              ic < nc && ! Thread.currentThread().isInterrupted();
              ic++ ) {
            TableCloud tcloud = tclouds[ ic ];
            TopcatModel tcModel = tcloud.getTopcatModel();
            DataGeom geom = tcloud.getDataGeom();
            int icPos = tcloud.getPosCoordIndex();

            /* Get the row mask for the table correspoinding to the current
             * table cloud.  If no such entry is present in the map, add one. */
            if ( ! maskMap.containsKey( tcModel ) ) {
                long nr = tcModel.getDataModel().getRowCount();
                maskMap.put( tcModel,
                             new BitSet( Tables.checkedLongToInt( nr ) ) );
            }
            BitSet mask = maskMap.get( tcModel );

            /* Iterate over the points in the cloud, testing inclusion and
             * updating this table's mask accordingly. */
            double[] dpos = new double[ geom.getDataDimCount() ];
            TupleSequence tseq = tcloud.createTupleSequence( dataStore );
            while ( tseq.next() ) {
                if ( geom.readDataPos( tseq, icPos, dpos ) &&
                     criterion.isIncluded( dpos ) ) {
                    long ix = tseq.getRowIndex();
                    mask.set( Tables.checkedLongToInt( ix ) );
                }
            }
        }
    }

    /**
     * Returns an object which can count the number of points visible in
     * the current plot.
     *
     * <p>The actual count may be slow for large data sets, so although this
     * method may be called on the event dispatch thread, the returned
     * factory's getItem method should not be.
     *
     * <p>The count is implicitly determined while making the plots,
     * since the various layers have to see which points are in bounds
     * to work out whether to plot them.  But the count information is not
     * recorded or passed back to this window.  A future improvement 
     * might do that somehow, in which case it wouldn't be necessary to
     * re-do the count here.  It wouldn't be straightforward though,
     * because you need to worry about double counting of points that
     * appear in multiple plots.
     *
     * @return  factory for deferred calculation of formatted point count
     */
    private Factory<String> createCounter() {
        Surface surface = plotPanel_.getSurface();
        final GuiPointCloud pointCloud = plotPanel_.createGuiPointCloud();
        final GuiPointCloud partialCloud =
            plotPanel_.createPartialGuiPointCloud();
        final PositionCriterion pointCriterion =
            PositionCriterion.createBoundsCriterion( surface );
        final PositionCriterion partialCriterion =
            PositionCriterion.createPartialBoundsCriterion( surface );
        final DataStore dataStore = plotPanel_.getDataStore();
        return new Factory<String>() {
            @Slow
            public String getItem() {
                long start = System.currentTimeMillis();
                long[] pointCounts =
                    countPoints( pointCloud, pointCriterion, dataStore );
                long[] partialCounts =
                    countPoints( partialCloud, partialCriterion, dataStore );
                if ( Thread.currentThread().isInterrupted() ) {
                    return null;
                }
                else {
                    PlotUtil.logTime( logger_, "Count", start );
                    long count = pointCounts[ 0 ] + partialCounts[ 0 ];
                    long total = pointCounts[ 1 ] + partialCounts[ 1 ];
                    return TopcatUtils.formatLong( count ) + " / "
                         + TopcatUtils.formatLong( total );
                }
            }
        };
    }

    /**
     * Counts the actual and potential number of points in a point cloud.
     * The potential number is the sum of the row counts of all the
     * tables contributing to the cloud.  The actual number is determined
     * by applying a supplied position criterion to each of the positions
     * in the cloud.
     *
     * @param   pointCloud  aggregation of points contributed by multiple tables
     * @param   criterion   test to define what counts as an included point
     * @param   dataStore   data storage object
     * @return  2-element array giving (actual,potential) counts
     */
    @Slow
    private static long[] countPoints( GuiPointCloud pointCloud,
                                       PositionCriterion criterion,
                                       DataStore dataStore ) {
        TableCloud[] tclouds = pointCloud.getTableClouds();
        long count = 0;
        long total = 0;
        for ( int ic = 0; ic < tclouds.length; ic++ ) {
            TableCloud tcloud = tclouds[ ic ];
            DataGeom geom = tcloud.getDataGeom();
            int iPosCoord = tcloud.getPosCoordIndex();
            double[] dpos = new double[ geom.getDataDimCount() ];
            TupleSequence tseq = tcloud.createTupleSequence( dataStore );
            while ( tseq.next() ) {
                if ( geom.readDataPos( tseq, iPosCoord, dpos ) &&
                     criterion.isIncluded( dpos ) ) {
                    count++;
                }
            }
            total += tcloud.getTopcatModel().getDataModel().getRowCount();
        }
        return new long[] { count, total };
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
        updatePositionDisplay( plotPanel_.getMousePosition() );

        /* Work out if it makes any sense to do a blob or visibility
         * selection. */
        boolean hasPoints =
            plotPanel_.createGuiPointCloud().getTableClouds().length > 0;
        boolean hasPartialPoints =
            plotPanel_.createPartialGuiPointCloud().getTableClouds().length > 0;
        blobAction_.setEnabled( hasPoints );
        fromVisibleAction_.setEnabled( hasPoints || hasPartialPoints );

        /* Update plot reports. */
        PlotLayer[] layers = plotPanel_.getPlotLayers();
        ReportMap[] reports = plotPanel_.getReports();
        int nl = layers.length;
        assert nl == reports.length;
        Map<LayerId,ReportMap> rmap = new HashMap<LayerId,ReportMap>();
        for ( int il = 0; il < nl; il++ ) {
            rmap.put( LayerId.createLayerId( layers[ il ] ), reports[ il ] );
        }
        for ( LayerControl control : stackModel_.getLayerControls( false ) ) {
            control.submitReports( rmap );
        }

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
     * This method is called when the mouse position changes in the plot panel.
     *
     *
     * @param  pos  current (new) mouse position, or null if the mouse
     *              is positioned outside the bounds of the PlotPanel
     */
    private void updatePositionDisplay( Point pos ) {
        displayPosition( pos );
        displayNavHelp( pos );
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
     * Ensures that navigation help is displayed correctly for the current
     * cursor position.
     *
     * @param  pos  cursor position, or null if outside the plot panel
     */
    private void displayNavHelp( Point pos ) {
        Surface surface = plotPanel_.getSurface();
        final Map<Gesture,String> navOpts;
        final boolean active;
        if ( surface != null ) {
            Navigator<A> navigator = getNavigator();

            /* Get a notional position for the navigation help to refer to.
             * If the reported position is within the plot panel, use that.
             * If it's outside the component altogether, use a position which
             * is within the actual bounds of the plot (inside the axes).
             * Navigation options may be different outside the plot bounds
             * (e.g. below the X axis or left of the Y axis). */
            boolean inBounds = pos != null
                            && plotPanel_.getBounds().contains( pos );
            final Point pos1;
            if ( inBounds ) {
                pos1 = pos;
            }
            else {
                Point origin = surface.getPlotBounds().getLocation();
                pos1 = new Point( origin.x + 1, origin.y + 1 );
            }
            active = inBounds;

            /* Add an item referring to the point selection provided by the
             * mouse listener added by this window. */
            navOpts = new LinkedHashMap<Gesture,String>();
            if ( canSelectPoints_ ) {
                navOpts.put( Gesture.CLICK_1, "Select" );
            }
            navOpts.putAll( navigator.getNavOptions( surface, pos1 ) );
        }
        else {
            active = false;

            /* Add at least one option in case of no surface.
             * This is just to make sure that the component has its nominal
             * size on initial window view, otherwise it gets resized as
             * soon as the surface shows up which is slightly visually
             * annoying. */
            navOpts = new HashMap<Gesture,String>();
            navOpts.put( Gesture.CLICK_1, "Select" );
        }

        /* Update the panel. */
        navPanel_.setOptions( navOpts );
        navPanel_.setEnabled( active );
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
            if ( PlotUtil.getButtonChangedIndex( evt ) == 1 ) {
                identifyPoint( evt.getPoint() );
            }
        }
    }
}
