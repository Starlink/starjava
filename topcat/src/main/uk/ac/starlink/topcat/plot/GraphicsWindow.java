package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultButtonModel;
import javax.swing.Icon;
import javax.swing.ListModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.SubsetConsumer;
import uk.ac.starlink.topcat.SuffixFileFilter;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.plot.AuxLegend;
import uk.ac.starlink.ttools.plot.ErrorMarkStyleSet;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Legend;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.MarkStyles;
import uk.ac.starlink.ttools.plot.PdfGraphicExporter;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.StyleSet;
import uk.ac.starlink.ttools.plot.TablePlot;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.util.WrapUtils;
import uk.ac.starlink.util.gui.ChangingComboBoxModel;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Abstract superclass for windows doing N-dimensional plots of table data.
 *
 * <p>The basic way that plotting works is as follows.  Almost all the
 * controls visible on the GraphicsWindow do nothing except trigger 
 * the replot action {@link #getReplotListener} when their state changes,
 * which schedules a replot to occur later on the event dispatch thread. 
 * When the replot is executed, the {@link #getPlotState} method is 
 * called which goes through all the controls and assembles a 
 * {@link PlotState} object (of a class which is probably specific to
 * the window implementation).  <em>Only if</em> this PlotState differs
 * from the last gathered PlotState will any actual plotting action take
 * place.  This means that we don't worry about triggering loads of
 * replot actions - as long as the state doesn't change materially 
 * between one and the next, they're cheap.  If the state does change
 * materially, then a new plot is required.  The work done for plotting
 * depends on the details of how the PlotState has changed - in some cases
 * new data will be acquired (<code>PointSelection.readPoints</code> 
 * is called - possibly expensive),
 * but if the data is the same as before, the plot just
 * needs to be redrawn (usually quite fast, since the various plotting
 * classes are written as efficiently as possible).
 * It is therefore very important for performance reasons that you can
 * tell whether one plot state differs from the last one.  Since the
 * PlotState is a newly created object each time, its <code>equals()</code>
 * method is used - so <code>PlotState.equals()</code> must be written 
 * with great care.  There's an assertion in this class which tests that
 * two PlotStates gathered at the same time are equal, so you should find
 * out if your equals() method is calling two equal states unequal.
 * If it's calling two unequal states equal, then you'll find that the
 * plot doesn't get updated when state changes.
 *
 * @author   Mark Taylor
 * @since    26 Oct 2005
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class GraphicsWindow extends AuxWindow {

    private final TablePlot plot_;
    private final int ndim_;
    private final int naux_;
    private final boolean hasLabels_;
    private final PointSelectorSet pointSelectors_;

    private final ReplotListener replotListener_;
    private final Action replotAction_;
    private final Action axisEditAction_;
    private final Action rescaleAction_;
    private final Action incAuxAction_;
    private final Action decAuxAction_;
    private final String[] axisNames_;
    private final ToggleButtonModel gridModel_;
    private final ToggleButtonModel[] flipModels_;
    private final ToggleButtonModel[] logModels_;
    private final ErrorModeSelectionModel[] errorModeModels_;
    private final JMenu exportMenu_;
    private final JProgressBar progBar_;
    private final BoundedRangeModel noProgress_;
    private final BoundedRangeModel auxVisibleModel_;
    private final ComboBoxModel[] auxShaderModels_;
    private final ToggleButtonModel labelsModel_;
    private final JComponent plotArea_;
    private final JComponent controlArea_;
    private final Legend legend_;
    private final AuxLegend[] auxLegends_;
    private final JToolBar pselToolbar_;
    private final JComponent extrasPanel_;
    private final JComponent legendBox_;
    private final JLabel titleLabel_;
    private final ToggleButtonModel legendModel_;

    private StyleSet styleSet_;
    private BitSet usedStyles_;
    private Points points_;
    private PointSelection lastPointSelection_;
    private PlotState lastState_;
    private Points lastPoints_;
    private Box statusBox_;
    private boolean initialised_;
    private int guidePointCount_;
    private AxisWindow axisWindow_;
    private Range[] dataRanges_;
    private Range[] viewRanges_;
    private boolean forceReread_;
    private double padRatio_ = 0.02;
    private PointsReader pointsReader_;
    private int nPlot_;
    private int nRead_;
    private boolean legendEverVisible_;

    private static JFileChooser exportSaver_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot" );
    private static final Shader[] SHADERS = new Shader[] {
        Shaders.NULL,
        Shaders.LUT_RAINBOW,
        Shaders.TRANSPARENCY,
        Shaders.LUT_PASTEL,
        Shaders.LUT_STANDARD,
        Shaders.LUT_HEAT,
        Shaders.LUT_COLOR,
        Shaders.FIX_HUE,
        Shaders.WHITE_BLACK,
        Shaders.RED_BLUE,
        Shaders.FIX_INTENSITY,
        Shaders.FIX_RED,
        Shaders.FIX_GREEN,
        Shaders.FIX_BLUE,
        Shaders.HSV_H,
        Shaders.HSV_S,
        Shaders.HSV_V,
        Shaders.FIX_Y,
        Shaders.FIX_U,
        Shaders.FIX_V,
    };
    private static final StyleSet MARKERS1;
    private static final StyleSet MARKERS2;
    private static final StyleSet[] MARK_STYLE_SETS = new StyleSet[] {
        MARKERS1 =
        MarkStyles.points( "Pixels" ),
        MARKERS2 =
        MarkStyles.spots( "Dots", 1 ),
        MarkStyles.spots( "Spots", 2 ),
        MarkStyles.filledShapes( "Small Coloured Shapes", 3, null ),
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
     * Constructor.  A number of main axes are defined by the 
     * <code>axisNames</code> array, which also defines the dimensionality.
     * The <code>naux</code> variable gives a maximum number of auxiliary
     * axes which will be managed by this window - these give extra
     * dimensions which can be mapped to, for instance, colour changes in
     * plotted points.  If no auxiliary axes are required, supply
     * <code>naux=0</code>.
     *
     * @param   viewName  name of the view window
     * @param   plot    component which draws the plot
     * @param   axisNames  array of labels by which each main axis is known
     * @param   naux   number of auxiliary axes
     * @param   errorModeModels   array of selecction models for error modes
     * @param   parent   parent window - may be used for positioning
     */
    public GraphicsWindow( String viewName, TablePlot plot, String[] axisNames,
                           int naux, boolean hasLabels,
                           ErrorModeSelectionModel[] errorModeModels,
                           Component parent ) {
        super( viewName, parent );
        plot_ = plot;
        axisNames_ = axisNames;
        ndim_ = axisNames.length;
        naux_ = naux;
        hasLabels_ = hasLabels;
        replotListener_ = new ReplotListener();

        /* Axis flags. */
        flipModels_ = new ToggleButtonModel[ ndim_ + naux_ ];
        logModels_ = new ToggleButtonModel[ ndim_ + naux_ ];
        for ( int i = 0; i < ndim_ + naux_; i++ ) {
            String ax = i < ndim_ ? axisNames[ i ]
                                  : "Aux " + ( i - ndim_ + 1 );
            flipModels_[ i ] = new ToggleButtonModel( "Flip " + ax + " Axis",
                null, "Reverse the sense of the " + ax + " axis" );
            logModels_[ i ] = new ToggleButtonModel( "Log " + ax + " Axis",
                null, "Logarithmic scale for the " + ax + " axis" );
            flipModels_[ i ].addActionListener( replotListener_ );
            logModels_[ i ].addActionListener( replotListener_ );
        }
        if ( ndim_ > 0 ) {
            flipModels_[ 0 ].setIcon( ResourceIcon.XFLIP );
            logModels_[ 0 ].setIcon( ResourceIcon.XLOG );
            if ( ndim_ > 1 ) {
                flipModels_[ 1 ].setIcon( ResourceIcon.YFLIP );
                logModels_[ 1 ].setIcon( ResourceIcon.YLOG );
            }
        }

        /* Error mode selectors. */
        errorModeModels_ = errorModeModels.clone();
        for ( int ierr = 0; ierr < errorModeModels_.length; ierr++ ) {
            errorModeModels_[ ierr ].addActionListener( replotListener_ );
        }

        /* Auxiliary axis visible model - controls how many of the auxiliary
         * axes are visible to the user. */
        auxVisibleModel_ = new DefaultBoundedRangeModel( 0, 0, 0, naux_ );
        incAuxAction_ =
            new GraphicsAction( "Add Aux Axis", ResourceIcon.ADD_COLORS,
                                "Add an auxiliary axis" );
        decAuxAction_ =
            new GraphicsAction( "Remove Aux Axis", ResourceIcon.REMOVE_COLORS,
                                "Remove the highest-numbered auxiliary axis" );
        auxVisibleModel_.addChangeListener( replotListener_ );
        ChangeListener auxEnabler = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                int val = auxVisibleModel_.getValue();
                incAuxAction_.setEnabled( val < auxVisibleModel_.getMaximum() );
                decAuxAction_.setEnabled( val > auxVisibleModel_.getMinimum() );
            }
        };
        auxVisibleModel_.addChangeListener( auxEnabler );
        auxEnabler.stateChanged( null );

        /* Model to control whether a label selection column can be used. */
        labelsModel_ =
            new ToggleButtonModel( "Draw Labels", ResourceIcon.LABEL,
                                   "Draw text labels for plotted points" );
        labelsModel_.addChangeListener( replotListener_ );

        /* Shader selection models for each auxiliary axis. */
        auxShaderModels_ = new ComboBoxModel[ naux ];
        Shader[] customShaders = Shaders.getCustomShaders();
        for ( int i = 0; i < naux; i++ ) {
            ChangingComboBoxModel shaderModel =
                new ChangingComboBoxModel( SHADERS );
            for ( int is = 0; is < customShaders.length; is++ ) {
                shaderModel.insertElementAt( customShaders[ is ], 1 );
            }
            shaderModel.setSelectedItem( shaderModel.getElementAt( 1 ) );
            shaderModel.addChangeListener( replotListener_ );
            auxShaderModels_[ i ] = shaderModel;
        }

        /* Create a legend object. */
        legend_ = new Legend();

        /* Model for whether the legend is visible or not. */
        legendModel_ =
            new ToggleButtonModel( "Show Legend", ResourceIcon.LEGEND,
                                   "Display legend at right of plot" );
        legendModel_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                boolean selected = legendModel_.isSelected();
                legendEverVisible_ = legendEverVisible_ || selected;
                boolean contained = Arrays.asList( legendBox_.getComponents() )
                                   .contains( legend_ );
                if ( selected && ! contained ) {
                    legendBox_.add( legend_, 0 );
                    legend_.resetWidth();
                    plotArea_.revalidate();
                    legend_.repaint();
                }
                else if ( ! selected && contained ) {
                    legendBox_.remove( legend_ );
                    plotArea_.revalidate();
                    legend_.repaint();
                }
                else {
                    assert selected == contained;
                }
            }
        } );
        legendModel_.addActionListener( replotListener_ );

        /* Set up point selector component. */
        pointSelectors_ = new PointSelectorSet() {
            protected PointSelector createSelector() {
                return GraphicsWindow.this.createPointSelector();
            }
            protected StyleEditor createStyleEditor() {
                return GraphicsWindow.this.createStyleEditor();
            }
        };
        JComponent pselBox = new JPanel( new BorderLayout() );
        pselBox.add( pointSelectors_, BorderLayout.CENTER );
        controlArea_ = new ScrollableBox();
        extrasPanel_ = new JPanel();
        extrasPanel_.setLayout( new BoxLayout( extrasPanel_,
                                               BoxLayout.Y_AXIS ) );
        controlArea_.add( extrasPanel_ );
        controlArea_.add( new SizeWrapper( pselBox ) );

        /* Construct the component which will form the actual plot graphics.
         * This is what will be drawn if the plot is printed/exported to
         * some external graphical format.  It will consist of the 
         * plotting area itself, as drawn by the window subclass, 
         * and optionally a legend and a title. */
        plotArea_ = new JPanel( new BorderLayout() );
        plotArea_.setOpaque( false );
        legendBox_ = Box.createVerticalBox();
        plotArea_.add( legendBox_, BorderLayout.EAST );
        Box titleBox = Box.createHorizontalBox();
        titleLabel_ = new JLabel();
        titleBox.add( Box.createHorizontalGlue() );
        titleBox.add( titleLabel_ );
        titleBox.add( Box.createHorizontalGlue() );
        plotArea_.add( titleBox, BorderLayout.NORTH );

        /* Set up a container for auxiliary axis legends. */
        auxLegends_ = new AuxLegend[ naux_ ];
        if ( naux_ > 0 ) {
            for ( int iaux = 0; iaux < naux_; iaux++ ) {

                /* Construct an auxililary axis legend. */
                AuxLegend auxLegend = new AuxLegend( false, 16 );
                auxLegends_[ iaux ] = auxLegend;
                int xpad = iaux > 0 ? 10 : 0;
                auxLegend.setBorder( BorderFactory
                                    .createEmptyBorder( 0, xpad, 0, 0 ) );

                /* Configure it for zooming. */
                final int idim = iaux + getMainRangeCount();
                ZoomRegion zoomRegion = new AuxLegendZoomRegion( auxLegend ) {
                    protected void dataZoomed( double lo, double hi ) {
                        getAxisWindow().getEditors()[ idim ].clearBounds();
                        getViewRanges()[ idim ]
                            .setBounds( new double[] { lo, hi } );
                        replot();
                    }
                };
                Zoomer zoomer = new Zoomer();
                zoomer.setCursorComponent( auxLegend );
                zoomer.setRegions( Collections.singletonList( zoomRegion ) );
                auxLegend.addMouseListener( zoomer );
                auxLegend.addMouseMotionListener( zoomer );
            }
            final JComponent auxLegendBox = Box.createHorizontalBox();
            legendBox_.add( auxLegendBox );
            ChangeListener auxVisListener = new ChangeListener() {
                public void stateChanged( ChangeEvent evt ) {
                    if ( ! auxVisibleModel_.getValueIsAdjusting() ) {
                        auxLegendBox.removeAll();
                        int nvis = auxVisibleModel_.getValue();
                        for ( int i = 0; i < nvis; i++ ) {
                            auxLegendBox.add( auxLegends_[ i ] );
                        }
                        auxLegendBox.add( Box.createHorizontalGlue() );
                    }
                }
            };
            auxVisListener.stateChanged( null );
            auxVisibleModel_.addChangeListener( auxVisListener );
        }

        /* Set up and populate a toolbar for controls relating specifically
         * to the point selectors. */
        pselToolbar_ = new JToolBar( JToolBar.HORIZONTAL );
        pselToolbar_.setFloatable( false );
        pselBox.add( pselToolbar_, BorderLayout.NORTH );
        pselToolbar_.add( pointSelectors_.getAddSelectorAction() );
        pselToolbar_.add( pointSelectors_.getRemoveSelectorAction() );
        if ( naux_ > 0 ) {
            pselToolbar_.addSeparator();
            pselToolbar_.add( incAuxAction_ );
            pselToolbar_.add( decAuxAction_ );
        }
        if ( hasLabels ) {
            pselToolbar_.addSeparator();
            pselToolbar_.add( labelsModel_.createToolbarButton() );
        }

        /* Ensure that changes to the point selection trigger a replot. */
        pointSelectors_.addActionListener( replotListener_ );

        /* Action to reconfigure main component placement (in a split window
         * or not). */
        final ToggleButtonModel splitModel =
            new ToggleButtonModel( "Split Window", ResourceIcon.SPLIT,
                                   "Make the data control panel at the bottom "
                                 + "of this window resizable" );
        splitModel.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                placeMainComponents( splitModel.isSelected() );
            }
        } );
        getWindowMenu().insert( splitModel.createMenuItem(), 1 );

        /* Add a progress bar. */
        progBar_ = placeProgressBar();
        noProgress_ = new DefaultBoundedRangeModel();

        /* Actions for exporting the plot. */
        Action gifAction =
            new GraphicExportAction( GraphicExporter.GIF, ResourceIcon.IMAGE,
                                     "Save plot as a GIF file" );
        Action pdfAction =
            new GraphicExportAction( PdfGraphicExporter.BASIC, ResourceIcon.PDF,
                                     "Save plot as a PDF file" );
        Action epsAction =
            new GraphicExportAction( GraphicExporter.EPS, ResourceIcon.PRINT,
                                     "Export to Encapsulated Postscript file" );
        Action epsgzAction =
            new GraphicExportAction( GraphicExporter.EPS_GZIP,
                                     ResourceIcon.PRINT_ZIP,
                                     "Export to Gzipped Encapsulated Postscript"
                                   + " file" );
        Action jpegAction =
            new GraphicExportAction( GraphicExporter.JPEG, ResourceIcon.JPEG,
                                     "Save plot as a JPEG file" );
        Action pngAction =
            new GraphicExportAction( GraphicExporter.PNG, ResourceIcon.IMAGE,
                                     "Save plot as a PNG file" );
        exportMenu_ = new JMenu( "Export" );
        exportMenu_.setMnemonic( KeyEvent.VK_E );
        exportMenu_.add( pdfAction );
        exportMenu_.add( gifAction );
        exportMenu_.add( epsAction );
        exportMenu_.add( epsgzAction );
        exportMenu_.add( jpegAction );
        exportMenu_.add( pngAction );
        getJMenuBar().add( exportMenu_ );

        /* Other actions. */
        replotAction_ =
            new GraphicsAction( "Replot", ResourceIcon.REDO,
                                "Redraw the plot" );
        rescaleAction_ =
            new GraphicsAction( "Rescale", ResourceIcon.RESIZE,
                                "Rescale the plot to show all points" );

        /* Action for showing grid. */
        gridModel_ = new ToggleButtonModel( "Show Grid", ResourceIcon.GRID_ON,
                                            "Select whether grid lines are " +
                                            "drawn" );
        gridModel_.setSelected( true );
        gridModel_.addActionListener( replotListener_ );

        /* Action for performing user configuration of axes. */
        axisEditAction_ = new GraphicsAction( "Configure Axes and Title",
                                              ResourceIcon.AXIS_EDIT,
                                              "Set axis labels and ranges" );

        /* Set up standard toolbar buttons. */
        getToolBar().add( splitModel.createToolbarButton() );
        getToolBar().add( replotAction_ );
        getToolBar().add( axisEditAction_ );
        getToolBar().add( pdfAction );
        getToolBar().add( gifAction );
    }

    /**
     * Arranges the plot and control panels in the main area of this window.
     * According to the argument, they will either be inserted into a 
     * JSplitPane or just placed in a JPanel with a BorderLayout.
     *
     * @param  split  true to use a split pane
     */
    private void placeMainComponents( boolean split ) {
        JComponent mainArea = getMainArea();
        mainArea.removeAll();
        if ( split ) {
            JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
            splitter.setTopComponent( plotArea_ );
            splitter.setOneTouchExpandable( true );
            splitter.setBottomComponent(
                new JScrollPane( controlArea_,
                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ) );
            splitter.setResizeWeight( 1.0 );
            splitter.setDividerLocation( plotArea_.getHeight() );
            mainArea.add( splitter, BorderLayout.CENTER );
        }
        else {
            mainArea.add( plotArea_, BorderLayout.CENTER );
            mainArea.add( controlArea_, BorderLayout.SOUTH );
        }
        mainArea.revalidate();
    }

    public void setVisible( boolean visible ) {
        if ( visible ) {
            ensureInitialised();
            if ( lastState_ == null ) {
                lastState_ = getPlotState();
                lastState_.setValid( false );
            }
        }
        super.setVisible( visible );
    }

    /**
     * Check that initialisations have been performed.
     */
    private void ensureInitialised() {
        if ( ! initialised_ ) {
            init();
            initialised_ = true;
            replot();
        }
    }
    
    /**
     * Perform initialisation which can't be done in the constructor
     * (typically because it calls potentially overridden methods).
     */
    protected void init() {

        /* Place the plot area and control panel. */
        placeMainComponents( false );

        /* Insert the plot component itself into the plotting component. */
        plotArea_.add( getPlotPanel(), BorderLayout.CENTER );

        /* Add a starter point selector. */
        PointSelector mainSel = createPointSelector();
        pointSelectors_.addNewSelector( mainSel );
        pointSelectors_.revalidate();

        /* Add axis editors and corresponding data and view range arrays. */
        final AxisEditor[] axeds =
            mainSel.getAxesSelector().createAxisEditors();
        int nax = axeds.length;
        dataRanges_ = new Range[ nax ];
        viewRanges_ = new Range[ nax ];
        for ( int i = 0; i < nax; i++ ) {
            viewRanges_[ i ] = new Range();
            axeds[ i ].addMaintainedRange( viewRanges_[ i ] );
            axeds[ i ].addActionListener( replotListener_ );
        }
        axisWindow_ = new AxisWindow( this );
        axisWindow_.addActionListener( replotListener_ );
        axisWindow_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                titleLabel_.setFont( plotArea_.getFont() );
                titleLabel_.setText( axisWindow_.getPlotTitle() );
            }
        } );

        /* If there are auxiliary axes arrange for the editors
         * in the axis window to keep track of which are visible. */
        if ( naux_ > 0 ) {
            ChangeListener auxVisListener = new ChangeListener() {
                public void stateChanged( ChangeEvent evt ) {
                    if ( ! auxVisibleModel_.getValueIsAdjusting() ) {
                        int nvis = getMainRangeCount()
                                 + auxVisibleModel_.getValue();
                        AxisEditor[] eds = new AxisEditor[ nvis ];
                        System.arraycopy( axeds, 0, eds, 0, nvis );
                        axisWindow_.setEditors( eds );
                    }
                }
            };
            auxVisibleModel_.addChangeListener( auxVisListener );
            auxVisListener.stateChanged( null );
        }
        else {
            axisWindow_.setEditors( axeds );
        }

        /* Set a suitable default style set. */
        long npoint = 0;
        if ( guidePointCount_ > 0 ) {
            npoint = guidePointCount_;
        }
        else {
            TopcatModel selectedTable =
                getPointSelectors().getMainSelector().getTable();
            if ( selectedTable != null ) {
                npoint = selectedTable.getDataModel().getRowCount();
            }
            else {
                ListModel tablesList = ControlWindow.getInstance()
                                                    .getTablesListModel();
                npoint = 10000;
                for ( int i = 0; i < tablesList.getSize(); i++ ) {
                    npoint = Math.min( npoint,
                                       ((TopcatModel)
                                        tablesList.getElementAt( i ))
                                      .getDataModel().getRowCount() );
                }
            }
        }
        setStyles( getDefaultStyles( (int) Math.min( npoint,
                                                     Integer.MAX_VALUE ) ) );
        mainSel.setStyles( getStyles() );

        /* Configure legend. */
        legend_.setErrorModeSelections( getErrorModeModels() );
    }

    /**
     * Returns a panel into which additional window-specific control 
     * components can be added.  By default it is empty.
     *
     * @return  extra control panel
     */
    public JComponent getExtrasPanel() {
        return extrasPanel_;
    }

    /**
     * Returns the number of axes whose ranges can be reset excluding any
     * auxiliary axes.
     */
    public int getMainRangeCount() {
        return ndim_;
    }

    /**
     * Provides a hint to this window how many points it's likely to be
     * plotting.  This should be called before the window is first 
     * displayed, and may influence the default plotting style.
     *
     * @param   npoint  approximate number of data points that may be plotted
     */
    public void setGuidePointCount( int npoint ) {
        guidePointCount_ = npoint;
    }

    /**
     * Sets the ratio by which the data ranges calculated by the
     * GraphicsWindow implementation of {@link #calculateRanges} are
     * padded.
     *
     * @param  pad  padding ratio (typically a few percent)
     */
    public void setPadRatio( double pad ) {
        padRatio_ = pad;
    }

    /**
     * Returns the ratio by which the data ranges calculated by the
     * GraphicsWindow implememetation of {@link #calculateRanges} are
     * padded.
     *
     * @return  padding ratio (by default a few percent)
     */
    public double getPadRatio() {
        return padRatio_;
    }

    /**
     * Returns the toolbar used for controls specific to the PointSelector
     * component.
     *
     * @return  point selector toolbar
     */
    public JToolBar getPointSelectorToolBar() {
        return pselToolbar_;
    }

    /**
     * Returns the menu which contains export actions.
     *
     * @return  export menu
     */
    public JMenu getExportMenu() {
        return exportMenu_;
    }

    /**
     * Constructs and returns a menu for selecting marker styles.
     * Selecting an item from this list resets all plotting styles in this
     * window according to the selected StyleSet.
     *
     * @param   styleSets  style sets to be presented in the menu
     * @return  menu
     */
    public JMenu createMarkerStyleMenu( StyleSet[] styleSets ) {
        JMenu styleMenu = new JMenu( "Marker Style" );
        styleMenu.setMnemonic( KeyEvent.VK_M );
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
        return styleMenu;
    }

    /**
     * Constructs and returns a menu which can be used to select error modes
     * for this window.
     *
     * @return  new error mode selection menu
     */
    public JMenu createErrorModeMenu() {

        /* Create a new menu. */
        JMenu errorMenu = new JMenu( "Errors" );

        /* For each dimension add menu items corresponding to what mode of
         * error bar it requires. */
        ErrorModeSelectionModel[] errorModeModels = getErrorModeModels();
        for ( int ierr = 0; ierr < errorModeModels.length; ierr++ ) {
            if ( ierr > 0 ) {
                errorMenu.addSeparator();
            }
            JMenuItem[] errItems = errorModeModels[ ierr ].createMenuItems();
            for ( int imode = 0; imode < errItems.length; imode++ ) {
                errorMenu.add( errItems[ imode ] );
            }
        }
        return errorMenu;
    }

    /**
     * Constructs and returns a menu which can be used to select error styles
     * to be imposed at once on all subsets.
     *
     * @param   renderers  full list of renderers which may be used to 
     *          draw errors
     */
    public JMenu createErrorRendererMenu( final ErrorRenderer[] renderers ) {
        final ErrorModeSelectionModel[] modeModels = getErrorModeModels();

        /* Create a new menu. */
        final JMenu styleMenu = new JMenu( "Error Style" );

        /* Prepare to add items at the end of the menu for selecting error
         * rendering style.  This is not a fixed list, because it depends
         * on what the current error mode selection is.  So we need and
         * error mode selection listener which deletes and re-adds suitable
         * error renderer items each time the mode selection changes. */
        ActionListener errStyleListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                updateErrorRendererMenu( styleMenu, 0, renderers,
                                         modeModels );
            }
        };
        for ( int ierr = 0; ierr < modeModels.length; ierr++ ) {
            modeModels[ ierr ].addActionListener( errStyleListener );
        }

        /* Invoke the update now to set it up for the current state. */
        errStyleListener.actionPerformed( null );

        /* Return the configured menu. */
        return styleMenu;
    }

    /**
     * Updates an error menu to reflect the current state of the 
     * error mode selectors.  First, all items after <code>nfixed</code>
     * are removed (presumed added on by previous invocations of this method).
     * Then a number of items corresponding to the supplied 
     * <code>renderers</code> array are added.
     * Wouldn't it be nice if JMenu had a model?
     *
     * @param  errorMenu  menu to modify
     * @param  nfixed     number of initial menu items to leave alone
     * @param  renderers  array of renderers for which menu actions may be
     *                    added
     * @param  modeModels error mode models
     */
    private void updateErrorRendererMenu( JMenu errorMenu, int nfixed,
                                 ErrorRenderer[] renderers,
                                 ErrorModeSelectionModel[] modeModels ) {

        /* Delete any of the menu items which we added last time. */
        while ( errorMenu.getItemCount() > nfixed ) {
            errorMenu.remove( errorMenu.getItemCount() - 1 );
        }
        if ( nfixed > 0 ) {
            errorMenu.addSeparator();
        }

        /* Count the number of non-blank dimensions for error bars. */
        int ndim = 0;
        ErrorMode[] modes = new ErrorMode[ modeModels.length ];
        for ( int idim = 0; idim < modeModels.length; idim++ ) {
            modes[ idim ] = modeModels[ idim ].getErrorMode();
            if ( ! ErrorMode.NONE.equals( modes[ idim ] ) ) {
                ndim++;
            }
        }

        /* For each known renderer, add an action to the menu if it makes
         * sense for the current error bar dimensionality. */
        int nAdded = 0;
        for ( int ir = 0; ir < renderers.length; ir++ ) {
            final ErrorRenderer erend = renderers[ ir ];
            if ( erend.supportsDimensionality( ndim ) ) {
                Icon icon = erend.getLegendIcon( modes, 30, 20, 1, 1 );
                String name = erend.getName();
                Action rendAct = new BasicAction( name, icon,
                                                  "Reset all error styles" ) {
                    public void actionPerformed( ActionEvent evt ) {
                        setStyles( new ErrorMarkStyleSet( styleSet_, erend ) );
                        replot();
                    }
                };
                errorMenu.add( rendAct );
                nAdded++;
            }
        }

        /* If no errors are in use, just add a disabled entry to that effect. */
        if ( nAdded == 0 ) {
            errorMenu.add( "No error bars" ).setEnabled( false );
        }
    }

    /**
     * Returns the PointSelectorSet component used by this window.
     *
     * @return  point selector set
     */
    public PointSelectorSet getPointSelectors() {
        return pointSelectors_;
    }

    /**
     * Returns the most recently calculated data range objects.
     * These were calculated by invocation of {@link #calculateRanges},
     * which probably occurred during the last data read or rescale
     * operation.  They describe the natural ranges of the data,
     * which typically means that they defibe an N-dimensional region
     * into which all the current data points fall.
     * 
     * @return  array of data ranges, one for each axis
     */
    public Range[] getDataRanges() {
        return dataRanges_;
    }

    /**
     * Returns an array of ranges which may be set to determine the
     * actual range of visible data displayed by this plot.  The 
     * dimensions should generally match those returned by 
     * {@link #getDataRanges}.  The actual range of visible data will
     * generally be got by combining the data range with the visible
     * range on each axis.  Elements of the returned array may have
     * their states altered, but should not be replaced, since 
     * these elements are kept up to date by the editors in the axis window.
     *
     * @return   array of visible ranges, one for each axis
     */
    public Range[] getViewRanges() {
        return viewRanges_;
    }

    /**
     * Returns an array of button models representing the inversion state
     * for each axis.  Selected state for each model indicates that that
     * axis has been flipped.
     *
     * @return   button models for flip state
     */
    public ToggleButtonModel[] getFlipModels() {
        return flipModels_;
    }

    /**
     * Returns an array of button models representing the log/linear state
     * for each axis.  Selected state for each model indicates that that
     * axis is logarithmic, unselected means linear.
     *
     * @return  button models for log state
     */
    public ToggleButtonModel[] getLogModels() {
        return logModels_;
    }

    /**
     * Returns the models for selecting error modes.
     *
     * @return  error mode models
     */
    public ErrorModeSelectionModel[] getErrorModeModels() {
        return errorModeModels_;
    }

    /**
     * Returns the model which indicates whether the legend is visible or not.
     *
     * @return  legend visibility model
     */
    public ToggleButtonModel getLegendModel() {
        return legendModel_;
    }

    /**
     * Returns the most recently read Points object.
     *
     * @return  points object
     */
    public Points getPoints() {
        return points_;
    }

    /**
     * Returns a line suitable for putting status information into.
     *
     * @return  status  component
     */
    public Box getStatusBox() {
        if ( statusBox_ == null ) {
            statusBox_ = Box.createHorizontalBox();
            JComponent panel = getControlPanel();
            panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
            panel.add( Box.createVerticalStrut( 5 ) );
            panel.add( statusBox_ );
        }
        return statusBox_;
    }

    /**
     * Returns the total (maximum) number of auxiliary axes used by this
     * window.
     *
     * @return  maximum number of aux axes
     */
    public int getAuxAxisCount() {
        return naux_;
    }

    /**
     * Returns the number of currently visible auxiliary axes for this window.
     *
     * @return  number of visible aux axes
     */
    public int getVisibleAuxAxisCount() {
        return auxVisibleModel_.getValue();
    }

    /**
     * Returns the plot object for this window.
     *
     * @return  plot
     */
    public TablePlot getPlot() {
        return plot_;
    }

    /**
     * Returns the component containing the graphics output of this 
     * window.  This is the component which is exported or printed etc
     * alongside the legend which is managed by GraphicsWindow.
     * It should therefore contain only the output data, not any user 
     * interface decoration.
     *
     * @return   plot container
     */
    protected abstract JComponent getPlotPanel();

    /**
     * Performs an actual plot.
     *
     * @param  state  plot state determining details of plot configuration
     */
    protected void doReplot( PlotState state ) {

        /* Send the plot component the most up to date plotting state. */
        plot_.setState( state );

        /* Schedule a replot. */
        plot_.repaint();
    }

    /**
     * Returns a new PointSelector instance to be used for selecting
     * points to be plotted.
     *
     * @return   new point selector component
     */
    protected PointSelector createPointSelector() {

        /* Default implementation uses Cartesian axes according to the
         * nominal dimensionality of this window. */
        ErrorModeSelectionModel[] errorModeModels = getErrorModeModels();
        AxesSelector axsel =
            new CartesianAxesSelector( axisNames_, logModels_, flipModels_,
                                       errorModeModels );

        /* If there are additional axes, construct a composite AxesSelector
         * which can keep track of them. */
        axsel = addExtraAxes( axsel );

        /* Create, configure and return the point selector. */
        PointSelector psel = new PointSelector( axsel, getStyles() );
        ActionListener errorModeListener = psel.getErrorModeListener();
        for ( int i = 0; i < errorModeModels.length; i++ ) {
            errorModeModels[ i ].addActionListener( errorModeListener );
        }
        return psel;
    }

    /**
     * Adds additional axes to a given AxesSelector as appropriate for this
     * window.  The returned value may or may not be the same as the input 
     * <code>axsel</code> object.
     *
     * <p>This method is called by <code>GraphicsWindow</code>'s implementation
     * of {@link #createPointSelector}.
     *
     * @param   axsel  basic axes selector
     * @return  axes selector containing additional auxiliary axes as
     *          appropriate
     */
    protected AxesSelector addExtraAxes( AxesSelector axsel ) {
        if ( naux_ > 0 ) {
            ToggleButtonModel[] auxLogModels = new ToggleButtonModel[ naux_ ];
            ToggleButtonModel[] auxFlipModels = new ToggleButtonModel[ naux_ ];
            System.arraycopy( logModels_, ndim_, auxLogModels, 0, naux_ );
            System.arraycopy( flipModels_, ndim_, auxFlipModels, 0, naux_ );
            final AugmentedAxesSelector augsel =
                new AugmentedAxesSelector( axsel, naux_, auxLogModels,
                                           auxFlipModels, auxShaderModels_ );
            auxVisibleModel_.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent evt ) {
                    if ( ! auxVisibleModel_.getValueIsAdjusting() ) {
                        augsel.setAuxVisible( auxVisibleModel_.getValue() );
                    }
                }
            } );
            augsel.setAuxVisible( auxVisibleModel_.getValue() );
            axsel = augsel;
        }
        if ( hasLabels_ ) {
            final LabelledAxesSelector labsel =
                new LabelledAxesSelector( axsel );
            labelsModel_.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent evt ) {
                    labsel.enableLabels( labelsModel_.isSelected() );
                }
            } );
            labsel.enableLabels( labelsModel_.isSelected() );
            axsel = labsel;
        }
        return axsel;
    }

    /**
     * Creates a style editor suitable for this window.
     *
     * @return   new style editor
     */
    protected abstract StyleEditor createStyleEditor();

    /**
     * Returns a StyleSet which can supply markers.
     * The <code>npoint</code> may be used as a hint for how many 
     * points are expected to be drawn with it.
     *
     * @param    npoint  approximate number of points - use -1 for unknown
     * @return   style factory
     */
    public abstract StyleSet getDefaultStyles( int npoint );

    /**
     * Sets the style set to use for this window.
     * 
     * @param   styleSet  new style set
     */
    public void setStyles( StyleSet styleSet ) {
        styleSet_ = styleSet;
        usedStyles_ = new BitSet();
        int nsel = pointSelectors_.getSelectorCount();
        for ( int isel = 0; isel < nsel; isel++ ) {
            pointSelectors_.getSelector( isel ).setStyles( getStyles() );
        }
    }

    /**
     * Returns a style set suitable for use with a new PointSelector.
     * Note this is not the same object as was set by {@link #setStyles},
     * but it is based on it - it will dispense styles from the same set,
     * but avoid styles already dispensed to other selectors.
     *
     * @return   style set suitable for a new selector
     */
    public MutableStyleSet getStyles() {
        return new PoolStyleSet( styleSet_, usedStyles_ );
    }

    /**
     * Constructs a new PlotState.  This is called by {@link #getPlotState}
     * prior to the PlotState configuration done there.  Thus if a 
     * subclass wants to provide and configure a particular state
     * (for instance one of a specialised subclass of PlotState) it can
     * override this method to do so.
     * The default implementation just invokes <code>new PlotState()</code>.
     *
     * @return   returns a new PlotState object ready for generic
     *           configuration
     */
    protected PlotState createPlotState() {
        return new PlotState();
    }

    /**
     * Returns an object which characterises the choices the user has
     * made in the GUI to indicate the plot that s/he wants to see.
     *
     * <p>The <code>GraphicsWindow</code> implementation of this method
     * as well as populating the state with standard information
     * also calls {@link PointSelection#readPoints}
     * and {@link #calculateRanges} if necessary.
     *
     * @return  snapshot of the currently-selected plot request
     */
    public PlotState getPlotState() {

        /* Create a plot state as delegated to the current instance. */
        PlotState state = createPlotState();

        /* Can't plot, won't plot. */
        if ( ! pointSelectors_.getMainSelector().isReady() ) {
            state.setValid( false );
            return state;
        }

        /* Set the number of main (geometric) dimensions.  This may need to
         * be adjusted by subclasses which define these differently. */
        state.setMainNdim( getMainRangeCount() );

        /* Set per-axis characteristics. */
        StarTable mainData =
            pointSelectors_.getMainSelector().getAxesSelector().getData();
        int nd = mainData.getColumnCount();
        ColumnInfo[] axinfos = new ColumnInfo[ nd ];
        boolean[] flipFlags = new boolean[ nd ];
        boolean[] logFlags = new boolean[ nd ];
        ValueConverter[] converters = new ValueConverter[ nd ];
        for ( int i = 0; i < nd; i++ ) {
            ColumnInfo cinfo = mainData.getColumnInfo( i );
            axinfos[ i ] = cinfo;
            converters[ i ] =
                cinfo.getAuxDatumValue( TopcatUtils.NUMERIC_CONVERTER_INFO,
                                        ValueConverter.class );
            if ( flipModels_.length > i ) {
                flipFlags[ i ] = flipModels_[ i ].isSelected();
            }
            if ( logModels_.length > i ) {
                logFlags[ i ] = logModels_[ i ].isSelected();
            }
        }
        state.setAxes( axinfos );
        state.setConverters( converters );
        state.setLogFlags( logFlags );
        state.setFlipFlags( flipFlags );

        /* Set array of shader objects.  Shaders which have no associated
         * data are filled in as nulls, since they have no effect on the 
         * plot.  The array is truncated so that the final element is 
         * non-null. */
        BitSet activeShaders = new BitSet();
        int nvis = auxVisibleModel_.getValue();
        if ( nvis > 0 ) {
            int nsel = pointSelectors_.getSelectorCount();
            for ( int ivis = 0; ivis < nvis; ivis++ ) {
                boolean isActive = false;
                for ( int isel = 0; isel < nsel && ! isActive; isel++ ) {
                    AugmentedAxesSelector augSel =
                        (AugmentedAxesSelector) WrapUtils
                       .getWrapped( pointSelectors_.getSelector( isel )
                                                   .getAxesSelector(),
                                    AugmentedAxesSelector.class );
                    CartesianAxesSelector auxSel = augSel.getAuxSelector();
                    Object auxItem = auxSel.getColumnSelector( ivis )
                                           .getSelectedItem();
                    isActive = isActive || ( auxItem instanceof ColumnData );
                }
                activeShaders.set( ivis, isActive );
            }
        }
        int nShader = activeShaders.length();
        assert nShader <= nvis;
        Shader[] shaders = new Shader[ nShader ];
        for ( int i = 0; i < nShader; i++ ) {
            if ( activeShaders.get( i ) ) {
                shaders[ i ] = (Shader) auxShaderModels_[ i ].getSelectedItem();
            }
        }
        state.setShaders( shaders );

        /* Set grid status. */
        state.setGrid( gridModel_.isSelected() );

        /* Set point selection, reading the points data if necessary
         * (that is if the point selection has changed since last time
         * it was read). */
        PointSelection pointSelection = pointSelectors_.getPointSelection();
        state.setPlotData( pointSelection );
        boolean sameData = pointSelection.sameData( lastPointSelection_ );
        if ( ( ! sameData ) || forceReread_ ) {
            forceReread_ = false;

            /* If we're looking at what is effectively a new graph,
             * reset the viewing limits to null, so the visible range
             * will be defined only by the data. */
            if ( ! pointSelection.sameAxes( lastPointSelection_ ) ) {
                for ( int i = 0; i < viewRanges_.length; i++ ) {
                    viewRanges_[ i ].clear();
                }
            }

            /* If we're already doing an asynchronous data read, cancel it. */
            if ( pointsReader_ != null ) {
                PointsReader pr = pointsReader_;
                pointsReader_ = null;
                pr.interrupt();
            }
            assert pointsReader_ == null;

            /* Start an asynchronous data read to get the new dataset. */
            pointsReader_ = new PointsReader( pointSelection, state );
            pointsReader_.start();

            /* In the mean time, install a blank dataset. */
            points_ = pointSelection.getEmptyPoints();
            dataRanges_ = calculateRanges( pointSelection, points_, state );

            /* Remember point selection for comparison next time. */
            lastPointSelection_ = pointSelection;
        }

        /* Set axis labels configured in the axis editor window. */
        AxisEditor[] eds = axisWindow_.getEditors();
        String[] labels = new String[ eds.length ];
        for ( int i = 0; i < eds.length; i++ ) {
            AxisEditor ed = eds[ i ];
            labels[ i ] = ed.getLabel();
        }
        state.setAxisLabels( labels );

        /* Set visible ranges, based on data range and ranges explicitly
         * selected by the user. */
        double[][] bounds = new double[ dataRanges_.length ][];
        for ( int i = 0; i < dataRanges_.length; i++ ) {
            Range range = new Range( dataRanges_[ i ] );
            range.limit( viewRanges_[ i ] );
            bounds[ i ] = range.getFiniteBounds( logFlags[ i ] );
        }
        state.setRanges( bounds );

        /* Return the configured state for use. */
        state.setValid( true );
        return state;
    }

    /**
     * Returns the button model used to select whether a grid will be
     * drawn or not.
     *
     * @return   grid toggle model
     */
    public ToggleButtonModel getGridModel() {
        return gridModel_;
    }

    /**
     * Returns an action which can be used to force a replot of the plot.
     *
     * @return   replot action
     */
    public Action getReplotAction() {
        return replotAction_;
    }

    /**
     * Returns an action which will recalculate data ranges, clear 
     * view ranges, and replot the data.
     *
     * @return  rescale action
     */
    public Action getRescaleAction() {
        return rescaleAction_;
    }

    /**
     * Returns an action which can be used to configure axes manually.
     *
     * @return   axis configuration action
     */
    public Action getAxisEditAction() {
        return axisEditAction_;
    }

    /**
     * Sets the main table in the point selector component.
     *
     * @param  tcModel  new table
     */
    public void setMainTable( TopcatModel tcModel ) {
        PointSelector mainSel = pointSelectors_.getMainSelector();
        mainSel.setTable( tcModel, true );
    }

    /**
     * Redraws the plot if any of the characteristics indicated by the
     * currently-requested plot state have changed since the last time
     * it was done.  If no changes have been made, no work is done, in
     * which case it will be cheap to call.
     *
     * <p>This method schedules a replot on the event dispatch thread,
     * so it may be called from any thread.
     */
    public void replot() {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                performReplot();
            }
        } );
    }

    /**
     * Redraws the plot in-thread, perhaps taking account of whether 
     * the plot state has changed since last time it was done.
     */
    private void performReplot() {

        /* If called before this window is ready, do nothing. */
        if ( ! initialised_ ) {
            return;
        }

        /* Interrogate this window for the details of the plot to be done. */
        PlotState state = getPlotState();

        /* Check that two plot state objects obtained one after another
         * satisfy the equals() relationship.  This is not required for 
         * correctness, but it is important for performance.  If you're
         * getting an assertion error here, find out why the two 
         * PlotStates are unequal and fix it (probably by providing 
         * suitable equals() implementations for plotstate constituent
         * objects).  getPlotState() itself ought to be cheap, so this 
         * assertion should not take much time. */
        assert state.equals( getPlotState() ) : state.compare( getPlotState() );

        /* Only if the plot will differ from last time we did it, do we
         * do the actual drawing.  This can be true in one of two ways:
         * either the PlotState differs (as the result of the controls
         * in this window having changed) or the Points object differs
         * (the asynchronous data read might have completed since last
         * time). */
        if ( ! state.equals( lastState_ ) || points_ != lastPoints_ ) {

            /* Make sure that the legend displays are up to date. */
            configureLegends( state );

            /* Do the actual painting. */
            PointSelection psel = (PointSelection) state.getPlotData();
            if ( psel != null ) {
                psel.setPoints( points_ );
            }
            doReplot( state );

            /* Log and store state for future use. */
            logger_.info( "Replot " + ++nPlot_ );
            lastState_ = state;
            lastPoints_ = points_;
        }
    }

    /**
     * Ensures that legends are configured and aligned correctly for a
     * given plot state.
     *
     * @param  state  plot state
     */
    private void configureLegends( PlotState state ) {

        /* Work out the space available above and below the actual plot
         * region within the plot component. */
        Rectangle containerRegion = plotArea_.getBounds();
        Rectangle plotRegion = plot_.getPlotBounds();
        int topgap = plotRegion.y;
        int botgap = containerRegion.height - plotRegion.height - topgap;

        /* Set the border on the legend so that its top edge aligns with
         * the top edge of the plot region. */
        Border border = BorderFactory.createEmptyBorder( topgap, 0, 10, 0 );
        border = BorderFactory.createCompoundBorder( border,
                    BorderFactory.createLineBorder( Color.BLACK ) );
        border = BorderFactory.createCompoundBorder( border,
                    BorderFactory.createEmptyBorder( 5, 5, 5, 10 ) );
        legend_.setBorder( border );

        /* Update plot style legend contents. */
        PlotData data = state.getPlotData();
        if ( data == null ) {
            legend_.setStyles( new Style[ 0 ], new String[ 0 ] );
        }
        else {
            int nset = data.getSetCount();
            Style[] styles = new Style[ nset ];
            String[] labels = new String[ nset ];
            for ( int is = 0; is < nset; is++ ) {
                styles[ is ] = data.getSetStyle( is );
                labels[ is ] = data.getSetName( is );
            }
            legend_.setStyles( styles, labels );

            /* If the legend has never been seen before and is worth looking
             * at, display it.  If it has ever been seen before then do 
             * nothing, since either it is still visible, or it's hidden 
             * at the user's explicit request. */
            if ( ! legendEverVisible_ && isLegendInteresting( state ) ) {
                legendModel_.setSelected( true );
                assert legendEverVisible_;
            }
        }

        /* Update legends for auxiliary axes. */
        int nvis = getVisibleAuxAxisCount();
        for ( int i = 0; i < nvis; i++ ) {
            auxLegends_[ i ].setLengthPadding( topgap, botgap );
            auxLegends_[ i ].configure( state, i );
        }
    }

    /**
     * Indicates whether the legend is worth showing for a given plot state.
     *
     * @param   state  plot state
     * @return  true iff the legend would show non-trivial information
     */
    protected boolean isLegendInteresting( PlotState state ) {

        /* If there is more than one labelled subset for display, the legend
         * conveys some useful information. */
        PlotData data = state.getPlotData();
        int nLabel = 0;
        if ( data != null ) {
            int nset = data.getSetCount();
            for ( int is = 0; is < nset; is++ ) {
                String label = data.getSetName( is );
                if ( label != null && label.length() > 0 ) {
                    nLabel++;
                }
            }
        }
        return nLabel > 1;
    }

    /**
     * Returns a listener which will perform a replot when any event occurs.
     *
     * @return   replot listener
     */
    protected ReplotListener getReplotListener() {
        return replotListener_;
    }

    /**
     * Returns the axis configuration window associated with this window.
     *
     * @return  axis editor dialogue
     */
    public AxisWindow getAxisWindow() {
        return axisWindow_;
    }

    /**
     * Adds a new row subset to tables associated with this window as
     * appropriate.  The subset is based on a bit vector
     * representing the points in this window's Points object.
     *
     * @param  pointsMask  bit vector giving included points
     */
    protected void addNewSubsets( BitSet pointsMask ) {

        /* If the subset is empty, just warn the user and return. */
        if ( pointsMask.cardinality() == 0 ) {
            JOptionPane.showMessageDialog( this, "Empty subset",
                                           "Blank Selection",
                                           JOptionPane.ERROR_MESSAGE );
            return;                                  
        }
  
        /* Get the name for the new subset(s). */
        SubsetConsumer consumer = getPointSelectors().getMainSelector()
                                 .getTable().enquireNewSubsetConsumer( this );
        if ( consumer == null ) {
            return;
        }

        /* For the given mask, which corresponds to all the plotted points,
         * deconvolve it into individual masks for any of the tables
         * that our point selection is currently dealing with. */
        PointSelection.TableMask[] tableMasks =
            ((PointSelection) lastState_.getPlotData())
           .getTableMasks( pointsMask );

        /* Handle each of the affected tables separately. */
        for ( int i = 0; i < tableMasks.length; i++ ) {
            TopcatModel tcModel = tableMasks[ i ].getTable();
            BitSet tmask = tableMasks[ i ].getMask();

            /* Try adding a new subset to the table. */
            if ( tmask.cardinality() > 0 ) {
                consumer.consumeSubset( tcModel, tmask );
            }
        }
    }

    /**
     * Calculates data ranges based on a PointSelection and a Points object.
     *
     * @param  pointSelection  point selection
     * @param  points  points data
     * @param  state  plot state
     * @return   array of per-axis data ranges 
     */
    private Range[] calculateRanges( final PointSelection pointSelection,
                                     final Points points, PlotState state ) {
        return calculateRanges( pointSelection.createPlotData( points ),
                                state );
    }

    /**
     * Calculates data ranges for a given data set.
     * The returned Range array is the one which will be returned from
     * future calls of {@link #getDataRanges}.
     *
     * @param  data  point data for the plot
     * @param  state  plot state
     * @return  ranges
     */
    public Range[] calculateRanges( PlotData data, PlotState state ) {

        /* Actual calculations are delegated to the plot object, which 
         * understands the data well enough to do it. */
        Range[] ranges = plot_.calculateBounds( data, state ).getRanges();

        /* Add some padding at each end. */
        for ( int idim = 0; idim < ranges.length; idim++ ) {
            ranges[ idim ].pad( getPadRatio() );
        }

        /* Return the range array. */
        return ranges;
    }

    /**
     * Rescales the data acoording to the most recently read data and current
     * point selection.  The data ranges are recalculated and the view
     * ranges are cleared.
     */
    private void rescale() {
        PlotState state = getPlotState();
        Points points = points_;
        if ( state.getValid() && points != null ) {
            getAxisWindow().clearRanges();
            dataRanges_ = calculateRanges( (PointSelection) state.getPlotData(),
                                           points, state );
            for ( int i = 0; i < viewRanges_.length; i++ ) {
                viewRanges_[ i ].clear();
            }
        }
    }

    /**
     * Returns a file chooser widget with which the user can select a
     * file to output the currently plotted graph to in some serialized form.
     *
     * @return   a file chooser
     */
    private JFileChooser getExportSaver() {
        if ( exportSaver_ == null ) {
            exportSaver_ = new JFileChooser( "." );
            exportSaver_.setAcceptAllFileFilterUsed( true );
        }
        return exportSaver_;
    }

    /**
     * Returns the index in the TableModel (not the TableColumnModel) of
     * the given TableColumn.
     *
     * @param   tcol   the column whose index is to be found
     * @return  the index of <code>tcol</code> in the table model
     */
    public int getColumnIndex( TableColumn tcol ) {
        return tcol.getModelIndex();
    }

    public void dispose() {
        super.dispose();

        /* Interrupt any active asynchronous data reads. */
        if ( pointsReader_ != null ) {
            PointsReader pr = pointsReader_;
            pointsReader_ = null;
            pr.interrupt();
        }

        /* Configure all the point selectors to use a new, dummy TopcatModel
         * instead of the one they were using before.  The main purpose of 
         * this is to give the selectors a chance to unregister themselves
         * as listeners to the old TopcatModel.  This is important so that
         * no references exist in listener lists to this window, so that
         * it can be garbage collected (once disposed, this window can't
         * become visible again). */
        TopcatModel dummyModel = TopcatModel.createDummyModel();
        PointSelectorSet psels = getPointSelectors();
        for ( int i = 0; i < psels.getSelectorCount(); i++ ) {
            psels.getSelector( i ).configureForTable( dummyModel );
        }
    }

//  public void finalize() throws Throwable {
//      super.finalize();
//      logger_.fine( "Finalize " + this.getClass().getName() );
//  }

    /**
     * Creates a default set of ErrorModeSelectionModels given a list of
     * axis names.
     *
     * @param   axisNames   array of axis names
     * @return  array of error model selection models, one for each axis
     */
    public static ErrorModeSelectionModel[] createErrorModeModels(
                                                  String[] axisNames ) {
        int nerror = axisNames.length;
        ErrorModeSelectionModel[] errorModeModels =
            new ErrorModeSelectionModel[ nerror ];
        for ( int ierr = 0; ierr < nerror; ierr++ ) {
            errorModeModels[ ierr ] =
                new ErrorModeSelectionModel( ierr, axisNames[ ierr ] );
        }
        return errorModeModels;
    }

    /**
     * Returns an array of StyleSets which dispense {@link MarkStyle} objects,
     * suitable for general purpose scatter plots.
     *
     * @return   styleset library
     */
    public static StyleSet[] getStandardMarkStyleSets() {
        return MARK_STYLE_SETS.clone();
    }

    /**
     * Utility method to adjust an array of style sets so that all its 
     * members use a given error renderer by default.
     *
     * @param   erend  desired default error renderer
     * @param   styleSets  input style set array
     * @return  output array of modified style sets
     */
    public static StyleSet[] fixDefaultErrorRenderers( ErrorRenderer erend,
                                                       StyleSet[] styleSets ) {
        styleSets = styleSets.clone();
        for ( int i = 0; i < styleSets.length; i++ ) {
            styleSets[ i ] = new ErrorMarkStyleSet( styleSets[ i ], erend );
        }
        return styleSets;
    }

    /**
     * Actions for exporting the plot to a file.
     */
    protected abstract class ExportAction extends BasicAction {
        final String formatName_;
        final FileFilter filter_;

        /**
         * Constructs an export action given a list of file extensions.
         * 
         * @param   formatName  short name for format
         * @param   icon   icon for action
         * @param   descrip  description for action
         * @param   extensions  array of standard file extensions for format
         */
        ExportAction( String formatName, Icon icon, String descrip,
                      String[] extensions ) {
            this( formatName, icon, descrip,
                  new SuffixFileFilter( extensions ) );
        }

        /**
         * Constructs an export action given a file filter.
         * 
         * @param   formatName  short name for format
         * @param   icon   icon for action
         * @param   descrip  description for action
         * @param   filter   file filter appropriate for export files
         */
        ExportAction( String formatName, Icon icon, String descrip, 
                      FileFilter filter ) {
            super( "Export as " + formatName, icon, descrip );
            formatName_ = formatName;
            filter_ = filter;
        }

        /**
         * Performs the export by writing bytes to a given stream.
         * Implementations should not close the stream after writing.
         *
         * @param  out  destination stream
         */
        public abstract void exportTo( OutputStream out ) throws IOException;

        public void actionPerformed( ActionEvent evt ) {
            Component parent = GraphicsWindow.this;

            /* Acquire and configure the file chooser. */
            JFileChooser chooser = getExportSaver();
            chooser.setDialogTitle( "Export Plot As " + formatName_ );
            chooser.setFileFilter( filter_ );

            /* Prompt the user to select a file for output. */
            if ( chooser.showDialog( parent, "Write " + formatName_ ) ==
                 JFileChooser.APPROVE_OPTION ) {
                OutputStream ostrm = null;
                try {

                    /* Construct the output stream. */
                    File file = chooser.getSelectedFile();
                    ostrm = new BufferedOutputStream(
                                    new FileOutputStream( file ) );

                    /* Write output to it. */
                    exportTo( ostrm );
                }
                catch ( IOException e ) {
                    ErrorDialog.showError( parent, "Write Error", e );
                }
                finally {
                    if ( ostrm != null ) {
                        try {
                            ostrm.close();
                        }
                        catch ( IOException e ) {
                            // no action
                        }
                    }
                }
            }
        }
    }

    /**
     * Action which exports the currently displayed plot component 
     * as a graphics file.
     */
    private class GraphicExportAction extends ExportAction {

        private final GraphicExporter gExporter_;

        /**
         * Constructor.
         *
         * @param   gExporter  format-specific graphics exporter
         * @param   icon   icon for action
         * @param   descrip  description for action
         * @param   extensions  array of standard file extensions for format
         */
        GraphicExportAction( GraphicExporter gExporter, Icon icon,
                             String descrip ) {
            super( gExporter.getName(), icon, descrip,
                   gExporter.getFileSuffixes() );
            gExporter_ = gExporter;
        }

        public void exportTo( OutputStream out ) throws IOException {
            gExporter_.exportGraphic( GraphicExporter.toPicture( plotArea_ ),
                                      out );
        }
    }

    /**
     * Thread class which will read the data into a Points object.
     */
    private class PointsReader extends Thread {
        final PointSelection pointSelection_;
        final PlotState prState_;
        final long start_;

        /**
         * Constructs a new reader ready to read data as defined by 
         * a given point selection object.
         *
         * @param  pointSelection  point selection
         * @param  state  plot state
         */
        PointsReader( PointSelection pointSelection, final PlotState state ) {
            super( "Point Reader" );
            pointSelection_ = pointSelection;
            prState_ = state;
            start_ = System.currentTimeMillis();
        }

        /**
         * Determines whether this thread is still active - that is whether
         * it is currently installed as this window's reader.  If it is not,
         * then no modifications to the state or GUI of this window should
         * be performed.  This method should be checked every time an
         * action is about to be performed on the event dispatch thread.
         * Its return value will only be changed by events on the event
         * dispatch thread.
         *
         * @return   true  iff this reader is installed
         */
        private boolean isActive() {
            return PointsReader.this == pointsReader_;
        }

        public void run() {
            if ( ! isActive() ) {
                return;
            }

            /* Construct and install a new progress bar model which will
             * be associated with this read. */
            final BoundedRangeModel progModel = new DefaultBoundedRangeModel();
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( isActive() ) {
                        progBar_.setModel( progModel );
                    }
                }
            } );

            /* Perform the read. */
            Points points = null;
            Throwable error = null;
            boolean success = false;
            try {
                points = pointSelection_.readPoints( progModel );
                error = null;
                success = true;
                nPlot_ = 0;
                logger_.info( "Data read " + ++nRead_ + " ("
                            + points.getCount() + " in "
                            + ( System.currentTimeMillis() - start_ ) + "ms)" );
            }

            /* In the case of a thread interruption, just bail out. */
            catch ( InterruptedException e ) {
                return;
            }
            catch ( Throwable e ) {
                points = null;
                error = e;
                success = false;
            }
            if ( ! isActive() ) {
                return;
            }

            /* Schedule an action to notify the Graphics window of the newly
             * arrived data, or failure to obtain it. */
            final Points points1 = points;
            final Throwable error1 = error;
            final boolean success1 = success;
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    if ( ! isActive() ) {
                        return;
                    }

                    /* Success: install the new data points object,
                     * update range information, and schedule a replot. */
                    if ( success1 ) {
                        points_ = points1;
                        dataRanges_ = calculateRanges( pointSelection_, points1,
                                                       prState_ );
                        replot();
                    }

                    /* In case of failure, inform the user. */
                    else {
                        if ( error1 instanceof OutOfMemoryError ) {
                            TopcatUtils.memoryError( (OutOfMemoryError)
                                                     error1 );
                        }
                        else {
                            ErrorDialog.showError( GraphicsWindow.this,
                                                   "Read Error", error1 );
                        }
                    }

                    /* Uninstall this reader and its associated progress bar. */
                    progBar_.setModel( noProgress_ );
                    pointsReader_ = null;
                }
            } );
        }
    }

    /**
     * Miscellaneous actions.
     */
    private class GraphicsAction extends BasicAction {
        GraphicsAction( String name, Icon icon, String desc ) {
            super( name, icon, desc );
        }
        public void actionPerformed( ActionEvent evt ) {
            if ( this == replotAction_ ) {

                /* Force a re-read of the data. */
                forceReread_ = true;
                lastState_ = null;
                replot();
            }
            else if ( this == axisEditAction_ ) {
                axisWindow_.setVisible( true );
            }
            else if ( this == rescaleAction_ ) {
                rescale();
                replot();
            }
            else if ( this == incAuxAction_ ) {
                auxVisibleModel_.setValue( auxVisibleModel_.getValue() + 1 );
            }
            else if ( this == decAuxAction_ ) {
                auxVisibleModel_.setValue( auxVisibleModel_.getValue() - 1 );
            }
        }
    }

    /**
     * General purpose listener which replots given an event.
     */
    protected class ReplotListener implements ActionListener, ItemListener,
                                              ListSelectionListener,
                                              ChangeListener {
        public void actionPerformed( ActionEvent evt ) {
            replot();
        }
        public void itemStateChanged( ItemEvent evt ) {
            replot();
        }
        public void valueChanged( ListSelectionEvent evt ) {
            replot();
        }
        public void stateChanged( ChangeEvent evt ) {
            replot();
        }
    }

    /**
     * JComponent which implements the Scrollable interface in a fashion
     * which makes it suitable for holding the controls of this window.
     * The main important points are the tracksViewportHeight/Width methods,
     * which indicate that the component should squash/stretch itself
     * sideways if necessary, but should keep its proper vertical height.
     */
    private static class ScrollableBox extends Box implements Scrollable {
        ScrollableBox() {
            super( BoxLayout.Y_AXIS );
        }
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        public int getScrollableBlockIncrement( Rectangle visibleRect,
                                                int orientation,
                                                int direction ) {
            return orientation == SwingConstants.VERTICAL
                 ? visibleRect.height
                 : visibleRect.width;
        }
        public int getScrollableUnitIncrement( Rectangle visibleRect,
                                               int orientation,
                                               int direction ) {
            return 24;
        }
    }
}
