package uk.ac.starlink.topcat.plot2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Slow;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.StepDataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Component which paints plot graphics for Topcat.
 * This is the throbbing heart of the plot classes.
 *
 * <p>It is supplied at construction time with various objects capable of
 * acquiring (presumably from a GUI) information required to specify a plot,
 * and its {@link #replot replot} method conceptually acquires
 * all that information and prepares a plot accordingly.
 * The plot is cached to an icon (probably an image) which is in
 * turn painted by <code>paintComponent</code>.
 * <code>replot</code> should therefore be called any time the plot
 * information has changed, or may have changed.
 *
 * <p>In actual fact <code>replot</code> additionally
 * spends a lot of effort to work out whether it can avoid doing some or
 * all of the work required for the plot on each occasion,
 * by caching and attempting to re-use the restults of various
 * computational steps if they have not become outdated.
 * The capability to do this as efficiently as possible drives quite a bit
 * of the design of the rest of the plotting framework, in particular
 * the requirement that a number of the objects determining plot content
 * can be assessed for equality to tell whether they have changed
 * materially since last time.
 *
 * <p>This component manages all the storage and caching of expensive
 * (memory-intensive) resources: layer plans and data stores.
 * Such resources should not be cached or otherwise held on to by
 * long-lived reference elsewhere in the application.
 *
 * <p>This component also manages threading to get computation done
 * in appropriate threads (and not on the EDT).  At time of writing
 * there are probably some improvements that can be made in that respect.
 *
 * <p>This component is an ActionListener - receiving any action will
 * prompt a (potential) replot.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2013
 */
public class PlotPanel<P,A> extends JComponent implements ActionListener {

    private final DataStoreFactory storeFact_;
    private final AxisController<P,A> axisController_;
    private final Factory<PlotLayer[]> layerFact_;
    private final Factory<PlotPosition> posFact_;
    private final Factory<Icon> legendFact_;
    private final Factory<float[]> legendPosFact_;
    private final Factory<String> titleFact_;
    private final ShaderControl shaderControl_;
    private final ToggleButtonModel sketchModel_;
    private final List<ChangeListener> changeListenerList_;
    private final PaperTypeSelector ptSel_;
    private final Compositor compositor_;
    private final BoundedRangeModel progModel_;
    private final ToggleButtonModel showProgressModel_;
    private final ExecutorService plotExec_;
    private final ExecutorService noteExec_;
    private PlotJob<P,A> plotJob_;
    private PlotJobRunner plotRunner_;
    private Cancellable plotNoteRunner_;
    private Cancellable extraNoteRunner_;
    private Workings<A> workings_;
    private Surface latestSurface_;
    private Map<SubCloud,double[]> highlightMap_;
    private Decoration navDecoration_;

    private static final Icon HIGHLIGHTER = new HighlightIcon();
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2" );

    /**
     * Constructor.  Factories to gather various information required
     * for the plot are passed in.
     * These are interrogated when a (possibly) new plot is triggered.
     *
     * <p>Information flow is, or should be, one way - this component
     * reads the data and the plot does not have side-effects on its
     * constituent components, since passing information both ways
     * generally leads to a lot of confusion.  In fact as currently
     * written a couple of GUI compoents, axisController and shaderControl
     * are passed in and can be affected.  It would be better to sanitize that.
     *
     * <p>A progress bar model is used so that progress can be logged
     * whenever a scan through the data of one or several tables is under way.
     * An alternative would be to pass a JProgressBar itself, so that a
     * new model could be inserted every time a new progress operation started.
     * That would actually be easier to use, but doing it this way makes it
     * more obvious if multiple progress operations are happening concurrently,
     * which as it stands they should not be.
     *
     * @param  storeFact   data store factory implementation
     * @param  axisController  axis control GUI component
     * @param  layerFact   supplier of plot layers
     * @param  posFact  supplier of plot position settings
     * @param  legendFact   supplier of legend icon
     * @param  legendPosFact    supplier of legend position
     *                          (2-element x,y fractional location in range 0-1,
     *                          or null for legend external/unused)
     * @param  titleFact    supplier of plot title text
     * @param  shaderControl   shader control GUI component
     * @param  sketchModel   model to decide whether intermediate sketch frames
     *                       are posted for slow plots
     * @param  ptSel   rendering policy
     * @param  compositor  compositor for composition of transparent pixels
     * @param  progModel  progress bar model for showing plot progress
     * @param  showProgressModel  model to decide whether data scan operations
     *                            are reported to the progress bar model
     */
    public PlotPanel( DataStoreFactory storeFact,
                      AxisController<P,A> axisController,
                      Factory<PlotLayer[]> layerFact,
                      Factory<PlotPosition> posFact,
                      Factory<Icon> legendFact, Factory<float[]> legendPosFact,
                      Factory<String> titleFact, ShaderControl shaderControl,
                      ToggleButtonModel sketchModel,
                      PaperTypeSelector ptSel, Compositor compositor,
                      BoundedRangeModel progModel,
                      ToggleButtonModel showProgressModel ) {
        storeFact_ = progModel == null
                   ? storeFact
                   : new ProgressDataStoreFactory( storeFact, progModel );
        axisController_ = axisController;
        layerFact_ = layerFact;
        posFact_ = posFact;
        legendFact_ = legendFact;
        legendPosFact_ = legendPosFact;
        titleFact_ = titleFact;
        shaderControl_ = shaderControl;
        sketchModel_ = sketchModel;
        ptSel_ = ptSel;
        compositor_ = compositor;
        progModel_ = progModel;
        showProgressModel_ = showProgressModel;
        changeListenerList_ = new ArrayList<ChangeListener>();
        plotExec_ = Executors.newSingleThreadExecutor();
        noteExec_ = Runtime.getRuntime().availableProcessors() > 1
                  ? Executors.newSingleThreadExecutor()
                  : plotExec_;
        plotRunner_ = new PlotJobRunner();
        plotNoteRunner_ = new Cancellable();
        extraNoteRunner_ = new Cancellable();
        setPreferredSize( new Dimension( 500, 400 ) );
        addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized( ComponentEvent evt ) {
                replot();
            }
        } );
        highlightMap_ = new HashMap<SubCloud,double[]>();
        clearData();
    }

    /**
     * Invokes replot.
     */
    public void actionPerformed( ActionEvent evt ) {
        replot();
    }

    /**
     * Call this on the event dispatch thread to indicate that the plot
     * inputs may have changed, to trigger a new plot.
     * The plot will be regenerated and painted at a later stage if required.
     * This method is fairly cheap to call if the plot has not in fact
     * changed.
     */
    public void replot() {

        /* We create the plot job here and queue it for (slightly)
         * later execution on the (same) event dispatch thread.
         * The point of this is that it is common for a single user
         * intervention (sliding a slider, selecting from a combo box)
         * to trigger not one but several ActionEvents in quick succession
         * (which may or may not be a consequence of sloppy coding of the GUI).
         * These actions probably all represent the same end state, or
         * even if they don't it's not desirable to plot the earlier ones.
         * Doing it like this gives a chance to ignore the earlier ones
         * in a quick sequence, and only bother to do the plot for the
         * last one. */

        /* Find out if we are already waiting for calculation of a replot. */
        boolean isJobPending = plotJob_ != null;

        /* Gather plot inputs and prepare a replot specification. */
        plotJob_ = createPlotJob();

        /* If a job is pending, there must already be a runnable in the
         * event queue which is ready to run the next plot job to appear.
         * In that case, resetting the value of plotJob_ as we have just
         * done without queueing a new runnable will cause the new replot
         * to get done (discarding the previously queued one). *./

        /* However if no job is pending, queue one now.  */
        if ( ! isJobPending ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {

                    /* Not expected to be be null, but might be if clearData()
                     * has been called (probably during window disposal). */
                    if ( plotJob_ != null ) {
                        executePlotJob( plotJob_ );
                        plotJob_ = null;
                    }
                }
            } );
        }
    }

    /**
     * Called from the event dispatch thread to schedule execution of a
     * plot job.
     *
     * @param  plotJob  plot job to run
     */
    private void executePlotJob( PlotJob<P,A> plotJob ) {

        /* Any annotations of the existing plot are out of date and should
         * be cancelled. */
        extraNoteRunner_.cancel( true );
        plotNoteRunner_.cancel( true );

        /* If the previously requested plot has not yet started, simply
         * cancel it.  If it has started, work out whether we want to let
         * it complete or to interrupt it directly.
         * This is not an easy call to make, but interactive response
         * seems to be best served by allowing the previous one to
         * complete if it was "on the way to" the new one - e.g. part of
         * the same (pan, zoom, style slider) drag gesture.  If not,
         * (e.g. different data, new layer) it's better to cancel the
         * previous one directly and start drawing the new one. 
         * Working out if one plot is on the way to another is itself
         * not easy, we delegate it here to the isSimilar method. */
        PlotJobRunner plotRunner = plotRunner_;
        boolean interruptPrevious = ! plotRunner.isSimilar( plotJob );
        plotRunner.cancel( interruptPrevious );

        /* Store the plot surface now if it can be done fast. */
        latestSurface_ = plotJob.getSurfaceQuickly();

        /* Schedule the plot job for execution. */
        plotRunner_ =
            new PlotJobRunner( plotJob, interruptPrevious ? null : plotRunner );
        plotRunner_.submit();
    }

    /**
     * Submits a runnable to run when the plot is not changing.
     * If the plot changes while it's in operation, it will be cancelled.
     * The supplied runnable should watch for thread interruptions.
     * Such runnables are notionally run on a different queue than the
     * one doing the plot.
     *
     * @param  annotator  runnable, typically for annotating the plot
     *                    in some sense
     */
    public void submitExtraAnnotator( Runnable annotator ) {
        extraNoteRunner_.cancel( true );
        extraNoteRunner_ = new Cancellable( noteExec_.submit( annotator ) );
    }

    /**
     * Submits a runnable to run on the same queue as the plot itself.
     * If the plot changes while it's in operation, it will be cancelled.
     * The supplied runnable should watch for thread interruptions.
     * Such runnables are notionally run on the same queue as the one
     * doing the plot, so will only run when a plot is complete.
     * They should use a GuiDataStore such as the one used by
     * {@link #createGuiPointCloud createGuiPointCloud} method
     * so that progress is logged as appropriate.
     *
     * @param  annotator  runnable to run on the plot queue
     */
    public void submitPlotAnnotator( Runnable annotator ) {
        plotNoteRunner_.cancel( true );
        plotNoteRunner_ = new Cancellable( plotExec_.submit( annotator ) );
    }

    /**
     * Returns placement for the most recent completed plot.
     *
     * @return   placement
     */
    public PlotPlacement getPlotPlacement() {
        return workings_.placer_;
    }

    /**
     * Returns the plot surface for the most recent completed plot.
     *
     * @return  plot surface
     */
    public Surface getSurface() {
        return workings_.placer_.getSurface();
    }

    /**
     * Returns the plot layers used in the most recent completed plot.
     *
     * @return  plot layers
     */
    public PlotLayer[] getPlotLayers() {
        return workings_.layers_;
    }

    /**
     * Returns the plot reports generated by the most recent completed plot.
     * The array elements correspond to those of the plot layers array.
     *
     * @return   plot reports
     */
    public ReportMap[] getReports() {
        return workings_.reports_;
    }

    /**
     * Returns the data store used in the most recent completed plot.
     *
     * @return  data store
     */
    public DataStore getDataStore() {
        return workings_.dataStore_;
    }

    /**
     * Returns a point cloud that describes all the point positions included
     * in the most recent plot.  This contains all the points from all the
     * subsets requested for plotting, including points not visible because
     * they fell outside the plot surface.
     * Iterating over the points described by the returned point cloud,
     * when using the DataStore available from it, takes care of progress
     * updates and thread interruptions.
     *
     * @return  positions in most recent plot
     */
    public GuiPointCloud createGuiPointCloud() {
        SubCloud[] subClouds =
            SubCloud.createSubClouds( workings_.layers_, true );
        return new GuiPointCloud( TableCloud.createTableClouds( subClouds ),
                                  getDataStore(),
                                  showProgressModel_.isSelected() ? progModel_
                                                                  : null );
    }

    /**
     * Returns a point cloud like that from {@link #createGuiPointCloud}
     * but for partial positions - ones for which data positions will have
     * one or more missing (NaN) coordinates.
     *
     * @return   partial positions in most recent plot
     * @see  uk.ac.starlink.ttools.plot2.SubCloud#createPartialSubClouds
     */
    public GuiPointCloud createPartialGuiPointCloud() {
        SubCloud[] subClouds =
            SubCloud.createPartialSubClouds( workings_.layers_, true );
        return new GuiPointCloud( TableCloud.createTableClouds( subClouds ),
                                  getDataStore(),
                                  showProgressModel_.isSelected() ? progModel_
                                                                  : null );
    }

    /**
     * Returns the map of layer-requested aux ranges sude in the most
     * recent completed plot.
     *
     * @return   actual aux range values for plot
     */
    public Map<AuxScale,Range> getAuxClipRanges() {
        return workings_.auxClipRanges_;
    }

    /**
     * Returns the best guess for the plot surface which will be displayed
     * next.  It may in fact be the surface for a plot which is
     * currently being calculated.
     *
     * @return   most up-to-date plot surface
     */
    public Surface getLatestSurface() {
        return latestSurface_ != null ? latestSurface_
                                      : getSurface();
    }

    /**
     * Clears state to initial values, cancels any plots in progress,
     * and disposes of potentially expensive memory assets.
     */
    public void clearData() {
        plotJob_ = null;
        plotRunner_.cancel( true );
        plotNoteRunner_.cancel( true );
        extraNoteRunner_.cancel( true );
        plotRunner_ = new PlotJobRunner();
        plotNoteRunner_ = new Cancellable();
        extraNoteRunner_ = new Cancellable();
        workings_ = new Workings<A>();
        navDecoration_ = null;
    }

    /**
     * Sets a list of points which should be highlighted in the plot.
     * This overwrites any previously set highlights map,
     * and triggers a replot.
     * These highlights will be retained for as long as the given
     * data specs are visible.
     *
     * @param  highlightMap  sequence of data positions labelled by SubCloud
     */
    public void setHighlights( Map<SubCloud,double[]> highlightMap ) {
        highlightMap_ = highlightMap;
        replot();
    }

    /**
     * Sets a decoration giving visual feedback for navigation gestures.
     * This decoration will overwrite any previously set value, and will be
     * retained until overwritten with a null value.
     * This method triggers a repaint, but not a replot; the data graphics
     * are assumed to be unaffected.
     *
     * @param  navDec  navigation decoration, or null to erase it
     */
    public void setNavDecoration( Decoration navDec ) {
        navDecoration_ = navDec;
        repaint();
    }

    /**
     * Acquires all the state necessary to define how to perform a plot,
     * and packages it up as an immutable object.
     *
     * @return   new plot job based on current state
     */
    private PlotJob<P,A> createPlotJob() {

        /* Acquire state. */
        PlotLayer[] layers = layerFact_.getItem();
        assert layerListEquals( layers, layerFact_.getItem() );
        assert layerSetEquals( layers, layerFact_.getItem() );
        SurfaceFactory<P,A> surfFact = axisController_.getSurfaceFactory();
        ConfigMap surfConfig = axisController_.getConfig();
        P profile = surfFact.createProfile( surfConfig );
        axisController_.configureForLayers( profile, layers );
        A fixAspect = axisController_.getAspect();
        Range[] geomFixRanges = axisController_.getRanges();
        ShadeAxisFactory shadeFact = shaderControl_.createShadeAxisFactory();
        Map<AuxScale,Range> auxFixRanges = new HashMap<AuxScale,Range>();
        Map<AuxScale,Subrange> auxSubranges = new HashMap<AuxScale,Subrange>();
        Map<AuxScale,Boolean> auxLogFlags = new HashMap<AuxScale,Boolean>();
        auxFixRanges.put( AuxScale.COLOR, shaderControl_.getFixRange() );
        auxSubranges.put( AuxScale.COLOR, shaderControl_.getSubrange() );
        auxLogFlags.put( AuxScale.COLOR, shaderControl_.isLog() );
        Icon legend = legendFact_.getItem();
        assert legend == null || legendFact_.getItem().equals( legend );
        float[] legpos = legendPosFact_.getItem();
        String title = titleFact_.getItem();
        PlotPosition plotpos = posFact_.getItem();
        Rectangle bounds = getOuterBounds( plotpos.getPlotSize() );
        Insets insets = plotpos.getPlotInsets();
        LayerOpt[] opts = PaperTypeSelector.getOpts( layers );
        PaperType paperType =
            ptSel_.getPixelPaperType( opts, compositor_, this );
        GraphicsConfiguration graphicsConfig = getGraphicsConfiguration();
        Color bgColor = getBackground();

        /* If the existing set of layers does not contain one of the
         * highlighted points, drop that highlight permanently.
         * You could argue that this should be done at plot time rather
         * than at plot job creation, since creating a plot job does
         * not entail that it will ever be plotted, but it's likely
         * that the effect will be the same. */
        SubCloud[] subClouds = SubCloud.createSubClouds( layers, true );
        highlightMap_.keySet().retainAll( Arrays.asList( subClouds ) );
        double[][] highlights = highlightMap_.values()
                                             .toArray( new double[ 0 ][] );

        /* Turn it into a plot job and return. */
        return new PlotJob<P,A>( workings_, layers, surfFact, profile,
                                 fixAspect, geomFixRanges, surfConfig,
                                 shadeFact, auxFixRanges, auxSubranges,
                                 auxLogFlags, legend, legpos, title, storeFact_,
                                 bounds, insets, paperType, graphicsConfig,
                                 bgColor, highlights );
    }

    /**
     * Paints the most recently cached plot icon.
     */
    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );
        Icon plotIcon = workings_.plotIcon_;
        if ( plotIcon != null ) {
            Insets insets = getInsets();
            plotIcon.paintIcon( this, g, insets.left, insets.top );

            /* Draw a border around the outside of the plot icon.
             * This will normally be invisible, since the plot icon is sized
             * to fit this component.  However, if the size has been set
             * explicitly (by supplying a PlotPosition object), it's useful
             * to be able to see where the outline is. */
            Color color0 = g.getColor();
            g.setColor( Color.GRAY );
            g.drawRect( insets.left - 1, insets.top - 1,
                        plotIcon.getIconWidth() + 1,
                        plotIcon.getIconHeight() + 1 );
            g.setColor( color0 );
        }
        Decoration navdec = navDecoration_;
        if ( navdec != null ) {
            navdec.paintDecoration( g );
        }
    }

    /**
     * Returns the bounds to use for the plot icon.
     * This includes axis decorations etc, but excludes component insets.
     *
     * @param  sizeSetting  explicit settings for icon size, or null;
     *                      negative members are ignored
     * @return   plot drawing bounds
     */
    private Rectangle getOuterBounds( Dimension sizeSetting ) {
        Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int width = getWidth() - insets.left - insets.right;;
        int height = getHeight() - insets.top - insets.bottom;
        if ( sizeSetting != null ) {
            if ( sizeSetting.width > 0 ) {
                width = sizeSetting.width;
            }
            if ( sizeSetting.height > 0 ) {
                height = sizeSetting.height;
            }
        }
        return new Rectangle( x, y, width, height );
    }

    /**
     * Adds a listener which will be messaged when the content of the
     * displayed plot actually changes.
     *
     * @param  listener   plot change listener
     */
    public void addChangeListener( ChangeListener listener ) {
        changeListenerList_.add( listener );
    }

    /**
     * Removes a listener previously added.
     *
     * @param  listener  plot change listener
     */
    public void removeChangeListener( ChangeListener listener ) {
        changeListenerList_.remove( listener );
    }

    /**
     * Messages change listeners.
     */
    private void fireChangeEvent() {
        ChangeEvent evt = new ChangeEvent( this );
        for ( ChangeListener listener : changeListenerList_ ) {
            listener.stateChanged( evt );
        }
    }

    /**
     * Utility method to return the list of non-null DataSpecs corresponding
     * to a given PlotLayer array.
     * Null dataspecs are ignored, so the output list may not be the same
     * length as the input array.
     *
     * @param   layers   plot layers
     * @return   data spec list
     */
    private static List<DataSpec> getDataSpecs( PlotLayer[] layers ) {
        List<DataSpec> list = new ArrayList<DataSpec>();
        for ( int il = 0; il < layers.length; il++ ) {
            DataSpec spec = layers[ il ].getDataSpec();
            if ( spec != null ) {
                list.add( spec );
            }
        }
        return list;
    }

    /**
     * Returns a list of LayerIds corresponding to an array of plot layers.
     *
     * @param  layers   plot layers
     * @return  list of layer ids
     */
    private static List<LayerId> layerList( PlotLayer[] layers ) {
        List<LayerId> llist = new ArrayList<LayerId>( layers.length );
        for ( int il = 0; il < layers.length; il++ ) {
            llist.add( LayerId.createLayerId( layers[ il ] ) );
        }
        return llist;
    }

    /**
     * Returns a list of LayerIds corresponding to an array of plot layers,
     * but in which the layer styles are not recorded (are set to null).
     *
     * @param  layers  plot layers
     * @return  list of style-less layer ids
     */
    private static List<LayerId> layerListNoStyle( PlotLayer[] layers ) {
        List<LayerId> llist = new ArrayList<LayerId>( layers.length );
        for ( int il = 0; il < layers.length; il++ ) {
            PlotLayer layer = layers[ il ];
            llist.add( new LayerId( layer.getPlotter(), layer.getDataSpec(),
                                    layer.getDataGeom(), null ) );
        }
        return llist;
    }

    /**
     * Determines whether two ordered lists of layers are effectively
     * identical.
     *
     * @param  layers1   first list
     * @param  layers2   second list
     * @return  true iff both lists are the same
     */
    private static boolean layerListEquals( PlotLayer[] layers1,
                                            PlotLayer[] layers2 ) {
        return layerList( layers1 ).equals( layerList( layers2 ) );
    }

    /**
     * Determines whether two unordered lists of layers contain the
     * equivalent sets of layers.
     *
     * @param  layers1   first list
     * @param  layers2   second list
     * @return   true iff both lists contain the same unique layers
     */
    private static boolean layerSetEquals( PlotLayer[] layers1,
                                          PlotLayer[] layers2 ) {
        return new HashSet<LayerId>( layerList( layers1 ) )
              .equals( new HashSet<LayerId>( layerList( layers2 ) ) );
    }

    /**
     * Immutable object representing the input to and result of a PlotJob.
     * If you've generated a Workings object you have done all the work
     * that can be done outside of the Event Dispatch Thread for making a plot.
     * The workings object also contains information that can be re-used
     * for subsequent plots if the input requirements are sufficiently
     * similar.
     */
    private static class Workings<A> {
        final PlotLayer[] layers_;
        final DataStore dataStore_;
        final Surface approxSurf_;
        final Range[] geomRanges_;
        final A aspect_;
        final Map<AuxScale,Range> auxDataRanges_;
        final Map<AuxScale,Range> auxClipRanges_;
        final PlotPlacement placer_;
        final Object[] plans_;
        final Icon dataIcon_;
        final Icon plotIcon_;
        final ReportMap[] reports_;
        final long plotMillis_;
        final int rowStep_;

        /**
         * Constructs a fully populated workings object.
         *
         * @param  layers   plot layers
         * @param  dataStore  data storage object
         * @param  approxSurf   approximation to plot surface (size etc may
         *                      be a bit out)
         * @param  geomRanges   ranges for the geometry coordinates
         * @param  aspect    surface aspect
         * @param  auxDataRanges  aux scale ranges derived from data
         * @param  auxClipRanges  aux scale ranges derived from
         *                        fixed constraints
         * @param  placer  plot placement
         * @param  plans   per-layer plot plan objects
         * @param  dataIcon   icon which will paint data part of plot
         * @param  plotIcon   icon which will paint the whole plot
         * @param  reports    reported info from plot layers
         * @param  plotMillis  wall-clock time in milliseconds taken for the
         *                     plot (plans+paint), but not data acquisition
         * @param  rowStep   row stride used for subsample in actual plots
         */
        Workings( PlotLayer[] layers, DataStore dataStore,
                  Surface approxSurf, Range[] geomRanges, A aspect,
                  Map<AuxScale,Range> auxDataRanges,
                  Map<AuxScale,Range> auxClipRanges, PlotPlacement placer,
                  Object[] plans, Icon dataIcon, Icon plotIcon,
                  ReportMap[] reports, long plotMillis, int rowStep ) {
            layers_ = layers;
            dataStore_ = dataStore;
            approxSurf_ = approxSurf;
            geomRanges_ = geomRanges;
            aspect_ = aspect;
            auxDataRanges_ = auxDataRanges;
            auxClipRanges_ = auxClipRanges;
            placer_ = placer;
            plans_ = plans;
            dataIcon_ = dataIcon;
            plotIcon_ = plotIcon;
            reports_ = reports;
            plotMillis_ = plotMillis;
            rowStep_ = rowStep;
        }

        /**
         * Constructs a dummy (contentless) workings object.
         */
        Workings() {
            this( new PlotLayer[ 0 ], null, null, null, null,
                  new HashMap<AuxScale,Range>(),
                  new HashMap<AuxScale,Range>(),
                  new PlotPlacement( new Rectangle( 0, 0 ), null ),
                  new Object[ 0 ], null, null, new ReportMap[ 0 ], 0L, 1 );
        }

        /**
         * Returns an object which characterises the data content of this
         * plot.  Two workings objects which have equal DataIconIds will
         * have equivalent dataIcon members.
         */
        @Equality
        DataIconId getDataIconId() {
            return new DataIconId( placer_.getSurface(), layers_,
                                   auxClipRanges_ );
        }
    }

    /**
     * Contains all the inputs required to perform a plot and methods to
     * generate a Workings object. 
     */
    private static class PlotJob<P,A> {
       
        private final Workings<A> oldWorkings_;
        private final PlotLayer[] layers_;
        private final SurfaceFactory<P,A> surfFact_;
        private final P profile_;
        private final A fixAspect_;
        private final Range[] geomFixRanges_;
        private final ConfigMap aspectConfig_;
        private final ShadeAxisFactory shadeFact_;
        private final Map<AuxScale,Range> auxFixRanges_;
        private final Map<AuxScale,Subrange> auxSubranges_;
        private final Map<AuxScale,Boolean> auxLogFlags_;
        private final Icon legend_;
        private final float[] legpos_;
        private final String title_;
        private final DataStoreFactory storeFact_;
        private final Rectangle bounds_;
        private final Insets insets_;
        private final PaperType paperType_;
        private final GraphicsConfiguration graphicsConfig_;
        private final Color bgColor_;
        private final double[][] highlights_;

        /**
         * Constructor.
         *
         * @param   oldWorkings  workings object from a previous run;
         *          parts of it may be re-used where appropriate
         * @param   layers  plot layer array
         * @param   surfFact  surface factory
         * @param   profile   surface profile
         * @param   fixAspect   exact surface aspect, or null if not known
         * @param   geomFixRanges  data ranges for geometry coordinates,
         *                         if known, else null
         * @param   aspectConfig  config map containing aspect keys
         * @param   shadeFact   shader axis factory
         * @param   auxFixRange  fixed ranges for aux scales, where known
         * @param   auxSubranges  subranges for aux scales, where present
         * @param   auxLogFlags  logarithmic slcae flags for aux scales
         *                       (either absent or false means linear)
         * @param   legend   legend icon, or null
         * @param   legpos   legend position as (x,y) array of relative
         *                   positions (0-1), or null if legend absent/external
         * @param   title    plot title, or null
         * @param   storeFact  data store factory implementation
         * @param   bounds   plot data bounds
         * @param   insets   space reserved for annotations between
         *                   the plot data bounds and external bounds
         * @param   paperType  rendering implementation
         * @param   graphicsConfig  graphics configuration
         * @param   bgColor   background colour
         * @param   highlights   array of highlight data positions
         */
        PlotJob( Workings<A> oldWorkings, PlotLayer[] layers,
                 SurfaceFactory<P,A> surfFact, P profile, A fixAspect,
                 Range[] geomFixRanges, ConfigMap aspectConfig,
                 ShadeAxisFactory shadeFact,
                 Map<AuxScale,Range> auxFixRanges,
                 Map<AuxScale,Subrange> auxSubranges,
                 Map<AuxScale,Boolean> auxLogFlags,
                 Icon legend, float[] legpos, String title,
                 DataStoreFactory storeFact, Rectangle bounds, Insets insets,
                 PaperType paperType, GraphicsConfiguration graphicsConfig,
                 Color bgColor, double[][] highlights ) {
            oldWorkings_ = oldWorkings;
            layers_ = layers;
            surfFact_ = surfFact;
            profile_ = profile;
            fixAspect_ = fixAspect;
            geomFixRanges_ = geomFixRanges;
            aspectConfig_ = aspectConfig;
            shadeFact_ = shadeFact;
            auxFixRanges_ = auxFixRanges;
            auxSubranges_ = auxSubranges;
            auxLogFlags_ = auxLogFlags;
            legend_ = legend;
            legpos_ = legpos;
            title_ = title;
            storeFact_ = storeFact;
            bounds_ = bounds;
            insets_ = insets;
            paperType_ = paperType;
            graphicsConfig_ = graphicsConfig;
            bgColor_ = bgColor;
            highlights_ = highlights;
        }

        /**
         * Calculates the workings object.
         * In case of error, or if the plot would have been just
         * the same as the previously calculated one (from oldWorkings),
         * null is returned
         *
         * @param  rowStep  stride for selecting row subsample; 1 means all rows
         * @param  progModel   progress bar model to be updated with progress;
         *                     if null, progress is not logged
         * @return  workings object or null
         */
        public Workings<A> calculateWorkings( int rowStep,
                                              BoundedRangeModel progModel ) {
            try {
                return attemptCalculateWorkings( rowStep, progModel );
            }
            catch ( InterruptedException e ) {
                Thread.currentThread().interrupt();
                return null;
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING, "Plot data error: " + e, e );
                return null;
            }
            catch ( OutOfMemoryError e ) {
                TopcatUtils.memoryErrorLater( e );
                return null;
            }
            catch ( Throwable e ) {
                logger_.log( Level.WARNING, "Plot data error: " + e, e );
                return null;
            }
        }

        /**
         * Attempts to calculate the workings object for this job.
         * If the plot would have been just the same as the one
         * previously calculated (from oldWorkings), null is returned
         *
         * @param  rowStep  stride for selecting row subsample; 1 means all rows
         * @param  progModel   progress bar model to be updated with progress;
         *                     if null, progress is not logged
         * @return   workings object, or null
         * @throws   IOException  in case of IO error
         * @throws   InterruptedException   if interrupted
         */
        private Workings<A>
                attemptCalculateWorkings( int rowStep,
                                          BoundedRangeModel progModel )
                throws IOException, InterruptedException {
            if ( bounds_.width > 0 && bounds_.height > 0 ) {
                DataStore dataStore = readDataStore();
                long ntuple = progModel == null
                            ? -1
                            : countTuples( dataStore, rowStep );
                return createWorkings( layers_, dataStore, rowStep,
                                       progModel, ntuple );
            }
            else {
                return null;
            }
        }

        /**
         * Counts how many tuples will be read in total when performing
         * this plot.
         *
         * @param   dataStore  contains the data for the plot
         * @param   rowStep  stride for subsampling rows
         * @return   total tuples expected to be read by a real plot,
         *           or -1 if not known
         */
        @Slow
        private long countTuples( DataStore dataStore, int rowStep ) {

            /* Set up a dummy data store based on this one, capable of
             * counting how many tuples are required. */
            CountDataStore countStore = new CountDataStore( dataStore, 8 );
            long cStart = System.currentTimeMillis();

            /* Do a dummy plot using that data store. */
            if ( createWorkings( layers_, countStore, rowStep, null, -1 )
                 != null ) {
                PlotUtil.logTime( logger_, "CountProgress", cStart );

                /* If successful, interrogate the data store for the number
                 * of tuples that [would have] got read. */
                return countStore.getTupleCount();
            }

            /* In case of trouble, return an indeterminate result. */
            else {
                return -1L;
            }
        }

        /**
         * Return a data store that can be used for performing this plot.
         * It may be possible to reuse one from last time (the cached
         * workings object), but if not, read a new one.
         *
         * @return  data store usable for this plot
         */
        @Slow
        private DataStore readDataStore()
                throws IOException, InterruptedException {

            /* Assess what data specs we will need. */
            DataSpec[] dataSpecs =
                getDataSpecs( layers_ ).toArray( new DataSpec[ 0 ] );

            /* If the oldWorkings data store contains the required data,
             * use that. */
            DataStore oldDataStore = oldWorkings_.dataStore_;
            if ( hasData( oldDataStore, dataSpecs ) ) {
                return oldDataStore;
            }

            /* Otherwise need a new data store. */
            else {
                long startData = System.currentTimeMillis();
                DataStore dataStore =
                    storeFact_.readDataStore( dataSpecs, oldDataStore );
                PlotUtil.logTime( logger_, "Data", startData );
                return dataStore;
            }
        }

        /**
         * Do the actual work for plotting.  This method
         * creates and returns a Workings object containing
         * the plot icon along with a load of intermediate information
         * calculated along the way that may be useful next time.
         * This method has no side-effects.
         *
         * <p>A null return indicates that there is no updated workings
         * object.  That could be either because the workings object
         * can be determined to be the same as the old one, or because
         * the calculations were interrupted.
         *
         * @param  layers   layers to plot
         * @param  dataStore  data store
         * @param  rowStep   stride for row subsampling, 1 for all rows
         * @param  progModel  progress bar model to update as tuples are read,
         *                    or null for no progress updates
         * @param  ntuple   total tuple count expected; used only for progress
         *                  updates, -1 if not known
         * @return  workings object representing completed plot, or null
         */
        @Slow
        private Workings<A> createWorkings( PlotLayer[] layers,
                                            DataStore dataStore, int rowStep,
                                            final BoundedRangeModel progModel,
                                            long ntuple ) {
            long startPlot = System.currentTimeMillis();
   
            /* Record the base data store which will be stored in the
             * output workings object, and prepare a data store decorated
             * with various wrappers to use for the actual processing. */
            DataStore dataStore0 = dataStore;
            DataStore dataStore1 = dataStore;
            dataStore = null;

            /* Pick subsample of rows if requested. */
            if ( rowStep > 1 ) {
                dataStore1 = new StepDataStore( dataStore1, rowStep );
            }

            /* Arrange for progress logging.  This also ensures that
             * if the thread is interrupted, tuples are no longer dispensed
             * from the data store.  We still have to check for thread
             * interruption status periodically, i.e. after every use of
             * the data store. */
            dataStore1 =
                new GuiDataStore( dataStore1, progModel, ntuple / rowStep );

            /* Ascertain the surface aspect.  If it has been set
             * explicitly, use that. */
            final A aspect;
            final Range[] geomRanges;
            if ( fixAspect_ != null ) {
                aspect = fixAspect_;
                geomRanges = geomFixRanges_;
            }

            /* Otherwise work them out from the supplied config and
             * by scanning the data if necessary. */
            else {
                if ( geomFixRanges_ != null ) {
                    geomRanges = geomFixRanges_;
                }
                else if ( ! surfFact_.useRanges( profile_, aspectConfig_ ) ) {
                    geomRanges = null;
                }
                else {
                    long startRange = System.currentTimeMillis();
                    geomRanges =
                        surfFact_.readRanges( profile_, layers_, dataStore1 );
                    if ( Thread.currentThread().isInterrupted() ) {
                        return null;
                    }
                    PlotUtil.logTime( logger_, "Range", startRange );
                    // could cache the ranges here by point cloud ID
                    // for possible later use.
                }
                aspect = surfFact_.createAspect( profile_, aspectConfig_,
                                                 geomRanges );
            }

            /* Work out the required aux scale ranges.
             * First find out which ones we need. */
            AuxScale[] scales = AuxScale.getAuxScales( layers );

            /* See if we can re-use the aux ranges from the oldWorkings.
             * This test isn't perfect, the layers may have changed
             * without requiring a recalculation of the Aux scales
             * (e.g. only colour map may have changed).  Oh well. */
            Surface approxSurf =
                surfFact_.createSurface( bounds_, profile_, aspect );
            Map<AuxScale,Range> auxDataRanges =
                  layerListEquals( layers, oldWorkings_.layers_ )
               && PlotUtil.equals( approxSurf, oldWorkings_.approxSurf_ )
                ? oldWorkings_.auxDataRanges_
                : new HashMap<AuxScale,Range>();

            /* Work out which scales we are going to have to calculate,
             * if any, and calculate them. */
            AuxScale[] calcScales =
                AuxScale.getMissingScales( scales, auxDataRanges,
                                           auxFixRanges_ );
            if ( calcScales.length > 0 ) {
                long startAux = System.currentTimeMillis();
                Map<AuxScale,Range> calcRanges =
                    AuxScale.calculateAuxRanges( calcScales, layers,
                                                 approxSurf, dataStore1 );
                if ( Thread.currentThread().isInterrupted() ) {
                    return null;
                }
                auxDataRanges.putAll( calcRanges );
                PlotUtil.logTime( logger_, "AuxRange", startAux );
            }

            /* Combine available aux scale information to get the
             * actual ranges for use in the plot. */
            Map<AuxScale,Range> auxClipRanges =
                AuxScale.getClippedRanges( scales, auxDataRanges,
                                           auxFixRanges_, auxSubranges_,
                                           auxLogFlags_ );

            /* Extract and use colour scale range for the shader. */
            Range shadeRange = auxClipRanges.get( AuxScale.COLOR );
            ShadeAxis shadeAxis = shadeFact_.createShadeAxis( shadeRange );

            /* Work out the graphics bounds of the data region. */
            final Rectangle dataBounds;
            if ( isFixedInsets( insets_ ) ) {
                dataBounds = PlotUtil.subtractInsets( bounds_, insets_ );
            }
            else {
                Rectangle autoDataBounds =
                    PlotPlacement
                   .calculateDataBounds( bounds_, surfFact_, profile_, aspect,
                                         true, legend_, legpos_, title_,
                                         shadeAxis );
                dataBounds = adjustDataBounds( bounds_, autoDataBounds,
                                               insets_ );
            }

            /* Get the plot surface. */
            Surface surface =
                surfFact_.createSurface( dataBounds, profile_, aspect );

            /* Get the basic plot decorations. */
            Decoration[] basicDecs =
                PlotPlacement.createPlotDecorations( surface, legend_, legpos_,
                                                     title_, shadeAxis );
            List<Decoration> decList = new ArrayList<Decoration>();
            decList.addAll( Arrays.asList( basicDecs ) );

            /* Place highlighted point icons as further plot decorations. */
            Icon highIcon = HIGHLIGHTER;
            int xoff = highIcon.getIconWidth() / 2;
            int yoff = highIcon.getIconHeight() / 2;
            Point2D.Double gp = new Point2D.Double();
            for ( int ih = 0; ih < highlights_.length; ih++ ) {
                if ( surface.dataToGraphics( highlights_[ ih ], true, gp ) ) {
                    int gx = PlotUtil.ifloor( gp.x - xoff );
                    int gy = PlotUtil.ifloor( gp.y - yoff );
                    decList.add( new Decoration( highIcon, gx, gy ) );
                }
            }

            /* Construct the plot placement. */
            Decoration[] decs = decList.toArray( new Decoration[ 0 ] );
            PlotPlacement placer = new PlotPlacement( bounds_, surface, decs );
            assert placer.equals( new PlotPlacement( bounds_, surface, decs ) );

            /* Determine whether first the data part, then the entire
             * graphics, of the plot is the same as for the oldWorkings.
             * If so, it's likely that we've got this far without any
             * expensive calculations (data scans), since the ranges
             * will have been picked up from the previous plot. */
            boolean sameDataIcon =
                new DataIconId( surface, layers, auxClipRanges )
               .equals( oldWorkings_.getDataIconId() );
            boolean samePlot =
                sameDataIcon &&
                placer.equals( oldWorkings_.placer_ );

            /* If the plot is identical to last time, return null as
             * an indication that no replot is required. */
            if ( samePlot ) {
                return null;
            }

            /* If the data part is the same as last time, no need to
             * redraw the data icon or recalculate the plans - carry
             * them forward from the oldWorkings for the result. */
            final Icon dataIcon;
            final long plotMillis;
            final Object[] plans;
            final ReportMap[] reports;
            if ( sameDataIcon ) {
                dataIcon = oldWorkings_.dataIcon_;
                plans = oldWorkings_.plans_;
                reports = oldWorkings_.reports_;
                plotMillis = 0;
            }

            /* Otherwise calculate plans and perform drawing to a new
             * cached data icon (image buffer). */
            else {
                int nl = layers.length;
                long startPlan = System.currentTimeMillis();
                Drawing[] drawings = new Drawing[ nl ];
                for ( int il = 0; il < nl; il++ ) {
                    drawings[ il ] =
                        layers[ il ].createDrawing( surface, auxClipRanges,
                                                    paperType_ );
                }
                plans = calculateDrawingPlans( drawings, dataStore1,
                                               oldWorkings_.plans_ );
                if ( Thread.currentThread().isInterrupted() ) {
                    return null;
                }
                PlotUtil.logTime( logger_, "Plan", startPlan );
                logger_.info( "Layers: " + layers_.length + ", "
                            + "Paper: " + paperType_ );
                reports = new ReportMap[ nl ];
                for ( int il = 0; il < nl; il++ ) {
                    reports[ il ] = drawings[ il ].getReport( plans[ il ] );
                }
                long startPaint = System.currentTimeMillis();
                dataIcon =
                    paperType_.createDataIcon( surface, drawings, plans,
                                               dataStore1, true );
                if ( Thread.currentThread().isInterrupted() ) {
                    return null;
                }
                PlotUtil.logTime( logger_, "Paint", startPaint );
                plotMillis = System.currentTimeMillis() - startPlot;
            }

            /* Create the final plot icon, and store the inputs and
             * outputs as a new Workings object for return. */
            Icon plotIcon = placer.createPlotIcon( dataIcon );
            return new Workings<A>( layers, dataStore0, approxSurf,
                                    geomRanges, aspect, auxDataRanges,
                                    auxClipRanges, placer, plans, dataIcon,
                                    plotIcon, reports, plotMillis, rowStep );
        }

        /**
         * Attempts to return the plot surface which the result of this
         * job will display.  If it cannot be determined quickly
         * (that is, if the data needs to be scanned), then null will be
         * returned.
         *
         * @return   plot surface used for this plot job, or null
         */
        public Surface getSurfaceQuickly() {

            /* Implementation follows that of the relevant parts of
             * attemptCalculateWorkings. */
            final A aspect;
            if ( fixAspect_ != null ) {
                aspect = fixAspect_;
            }
            else {
                final Range[] geomRanges;
                if ( geomFixRanges_ != null ) {
                    geomRanges = geomFixRanges_;
                }
                else if ( ! surfFact_.useRanges( profile_, aspectConfig_ ) ) {
                    geomRanges = null;
                }

                /* If it needs ranging, it's too slow. */
                else {
                    return null;
                }
                aspect = surfFact_.createAspect( profile_, aspectConfig_,
                                                 geomRanges );
            }

            AuxScale[] scales = AuxScale.getAuxScales( layers_ );
            Map<AuxScale,Range> auxDataRanges =
                  layerListEquals( layers_, oldWorkings_.layers_ )
                ? oldWorkings_.auxDataRanges_
                : new HashMap<AuxScale,Range>();
            AuxScale[] calcScales =
                AuxScale.getMissingScales( scales, auxDataRanges,
                                           auxFixRanges_ );
            if ( calcScales.length > 0 ) {
                return null;
            }

            Map<AuxScale,Range> auxClipRanges =
                AuxScale.getClippedRanges( scales, auxDataRanges,
                                           auxFixRanges_, auxSubranges_,
                                           auxLogFlags_ );
            Range shadeRange = auxClipRanges.get( AuxScale.COLOR );
            ShadeAxis shadeAxis = shadeFact_.createShadeAxis( shadeRange );
            PlotPlacement placer =
                PlotPlacement.createPlacement( bounds_, surfFact_, profile_,
                                               aspect, true, legend_, legpos_,
                                               title_, shadeAxis );
            return placer.getSurface();
        }

        /**
         * Determines whether a data store has data backing all of a
         * set of data specs.
         *
         * @param   dstore  data store, may be null
         * @param   dspecs  list of data specs
         * @return  true iff the data store has all the specified data
         */
        private static boolean hasData( DataStore dstore, DataSpec[] dspecs ) {
            if ( dstore == null ) {
                return dspecs.length == 0;
            }
            else {
                for ( int is = 0; is < dspecs.length; is++ ) {
                    DataSpec dspec = dspecs[ is ];
                    if ( dspec != null && ! dstore.hasData( dspec ) ) {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * Determines whether an Insets object contains full inset information.
         *
         * @param  insets   represents explicit inset settings
         * @return  true iff insets is not null and all its members are &gt;=0
         */
        private static boolean isFixedInsets( Insets insets ) {
            return insets != null
                && insets.top >= 0
                && insets.left >= 0
                && insets.bottom >= 0
                && insets.right >= 0;
        }

        /**
         * Returns a data bounds rectangle based on given external plot bounds,
         * automatically calculated data bounds, and an optional insets
         * object providing preferred settings.  The values in the insets
         * object are used to override those from the autoBounds input
         * where present.
         *
         * @param  extBounds  fixed external bounds 
         * @param  autoBounds   default data bounds
         * @param  insets   may contain required insets between external and
         *                  data bounds; members may be negative to indicate
         *                  no setting, or the whole thing can be null
         * @return   data bounds rectangle for actual use
         */
        private static Rectangle adjustDataBounds( Rectangle extBounds,
                                                   Rectangle autoBounds,
                                                   Insets insets ) {
            if ( insets == null ) {
                return new Rectangle( autoBounds );
            }
            int top =
                  insets.top >= 0
                ? insets.top
                : autoBounds.y - extBounds.y;
            int left =
                  insets.left >= 0
                ? insets.left
                : autoBounds.x - extBounds.x;
            int bottom =
                  insets.bottom >= 0
                ? insets.bottom
                : extBounds.y + extBounds.height
                - autoBounds.y - autoBounds.height;
            int right =
                  insets.right >= 0
                ? insets.right
                : extBounds.x + extBounds.width
                - autoBounds.x - autoBounds.width;
            return PlotUtil
                  .subtractInsets( extBounds,
                                   new Insets( top, left, bottom, right ) );
        }

        /**
         * Calculates plot plans for a set of drawings, attempting to re-use
         * previously calculated plans where possible.
         *
         * @param  drawings   drawings
         * @param  dataStore   data storage object
         * @param  oldPlans  unordered array of plan objects previously
         *                   calculated that may or may not be re-usable
         *                   for the current drawings
         * @return  array of per-drawing plans
         */
        private static Object[] calculateDrawingPlans( Drawing[] drawings,
                                                       DataStore dataStore,
                                                       Object[] oldPlans ) {
            int nl = drawings.length;
            Set<Object> oldPlanSet =
                new HashSet<Object>( Arrays.asList( oldPlans ) );
            Object[] plans = new Object[ nl ];
            for ( int il = 0; il < nl; il++ ) {
                Object plan =
                    drawings[ il ].calculatePlan( oldPlanSet.toArray(),
                                                  dataStore );
                plans[ il ] = plan;
                oldPlanSet.add( plan );
            }
            return plans;
        }
    }

    /**
     * Identifier object for data icon content.
     * Two Workings objects that have the same DataIconId will have
     * the same data icon.
     */
    @Equality
    private static class DataIconId {
        private final Surface surface_;
        private final PlotLayer[] layers_;
        private final Map<AuxScale,Range> auxClipRanges_;

        /**
         * Constructor.
         *
         * @param  surface  plot surface
         * @param  layers   plot layers
         * @param  auxClipRanges   actual ranges used for aux scales
         */
        DataIconId( Surface surface, PlotLayer[] layers,
                    Map<AuxScale,Range> auxClipRanges ) {
            surface_ = surface;
            layers_ = layers;
            auxClipRanges_ = auxClipRanges;
        }

        public boolean equals( Object o ) {
            if ( o instanceof DataIconId ) {
                DataIconId other = (DataIconId) o;
                return this.surface_.equals( other.surface_ )
                    && layerListEquals( this.layers_, other.layers_ )
                    && this.auxClipRanges_.equals( other.auxClipRanges_ );
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int code = 987;
            code = 23 * code + surface_.hashCode();
            code = 23 * code + layerList( layers_ ).hashCode();
            code = 23 * code + auxClipRanges_.hashCode();
            return code;
        }
    }


    /**
     * Stores a reference to a Future in such a way that it can be
     * cancelled if it still exists, but does not prevent it from being GCd.
     */
    private static class Cancellable {
        private final Reference<Future<?>> ref_;

        /**
         * Constructor.
         *
         * @param  future   future object to wrap
         */
        Cancellable( Future<?> future ) {
            ref_ = new WeakReference<Future<?>>( future );
        }

        /**
         * Constructs a dummy cancellable.
         */
        Cancellable() {
            this( null );
        }

        /**
         * Cancels this object's future task if it still exists.
         *
         * @param  mayInterruptIfRunning  whether interruption should take
         *         place if the thing has already started
         */
        public void cancel( boolean mayInterruptIfRunning ) {
            Future<?> future = ref_.get();
            if ( future != null ) {
                future.cancel( mayInterruptIfRunning );
            }
        }
    }

    /**
     * Handles submission and cancelling of plot jobs.
     */
    private class PlotJobRunner {
        private final Object simObj_;
        private final int rowStep_;
        private PlotJob plotJob_;
        private volatile Cancellable fullCanceler_;
        private volatile Cancellable stepCanceler_;
        private volatile long fullPlotMillis_;
        private static final int MAX_FULL_PLOT_MILLIS = 250;
        private static final int MAX_STEP_PLOT_MILLIS = 100;

        /**
         * Constructor.
         *
         * <p>A parameter supplies a previously submitted job
         * for reference; it should have been plotting about the same
         * amount of data and take about the same amount of time.  This is
         * used to work out what step to take for subsampled intermediate
         * plots.  The criteria are not actually the same as for the
         * similarity object, but it's an OK approximation.
         *
         * <p>Reference to the supplied <code>plotJob</code> object will
         * be released as soon as possible, so retaining a reference to
         * this PlotJobRunner is not harmful.
         *
         * @param   plotJob  plot job to be plotted
         * @param   referenceRunner   an instance for a previously submitted
         *          job related to this one; null if none is available
         */
        public PlotJobRunner( PlotJob plotJob, PlotJobRunner refRunner ) {
            plotJob_ = plotJob;
            simObj_ = getSimilarityObject( plotJob );
            rowStep_ = sketchModel_.isSelected() ? getRowStep( refRunner ) : 1;
        }

        /**
         * Dummy constructor for placeholder instance.
         */
        public PlotJobRunner() {
            this( null, null );
        }

        /**
         * Determines an appropriate subsample step to use given a
         * plot runner that has (maybe) already executed.
         * The subsample step is chosen so that the intermediate plots
         * ought to take a reasonable amount of time.  
         * If in doubt, 1 is returned, which means no subsampling
         * (full plot only).
         *
         * @param  other  runner supplied for reference, or null
         * @return   suitable step for a subsampled plot
         */
        private final int getRowStep( PlotJobRunner other ) {

            /* No reference plot, no subsampling. */
            if ( other == null ) {
                return 1;
            }

            /* If the reference plot has completed a full plot, use the
             * timing for that to work out what to do. */
            else if ( other.fullPlotMillis_ > 0 ) {

                /* If a full plot takes less than a given threshold, don't
                 * subsample. */
                long plotMillis = other.fullPlotMillis_;
                if ( plotMillis <= MAX_FULL_PLOT_MILLIS ) {
                    return 1;
                }

                /* If it takes longer, arrange for a subsample that should
                 * take around a given limit. */
                else {
                    return (int) Math.min( Integer.MAX_VALUE,
                                           plotMillis / MAX_STEP_PLOT_MILLIS );
                }
            }

            /* Otherwise, copy the step of the reference plot. */
            else if ( other.rowStep_ > 1 ) {
                return other.rowStep_;
            }
            else {
                return 1;
            }
        }

        /**
         * Submits this object's job for execution.
         */
        public void submit() {
            if ( fullCanceler_ != null ) {
                throw new IllegalStateException( "Don't call it twice" );
            }
            final PlotJob plotJob = plotJob_;

            /* Void the reference to the plot job so it can be GC'd as
             * early as possible. */
            plotJob_ = null;

            /* Set up runnables to execute the full plot or a subsample plot. */
            final BoundedRangeModel progModel =
                showProgressModel_.isSelected() ? progModel_ : null;
            Runnable fullJob = new Runnable() {
                public void run() {
                    Workings<A> workings =
                        plotJob.calculateWorkings( 1, progModel );
                    fullPlotMillis_ = workings.plotMillis_;
                    submitWorkings( workings );
                }
            };
            Runnable stepJob = new Runnable() {
                public void run() {
                    Workings<A> workings =
                        plotJob.calculateWorkings( rowStep_, null );
                    submitWorkings( workings );
                }
            };

            /* Submit one or both for execution. */
            if ( rowStep_ > 1 ) {
                logger_.info( "Intermediate plot with row step " + rowStep_ );
                stepCanceler_ = new Cancellable( plotExec_.submit( stepJob ) );
            }
            fullCanceler_ = new Cancellable( plotExec_.submit( fullJob ) );
        }

        /**
         * Cancels this object's job if applicable.
         * A parameter indicates whether the next job to be submitted
         * (the one in favour of which this one is being cancelled)
         * is similar to this one.  This may have implications for how
         * aggressively the cancellation is applied.
         *
         * @param  nextIsNotSimilar   false iff the next job will be similar
         */
        public void cancel( boolean nextIsNotSimilar ) {

            /* If the next job is quite like the old plot (e.g. pan or zoom)
             * it's a good idea to let the old one complete, so that
             * intermediate views are displayed rather than the screen
             * going blank until there are no more plots pending.
             * Pans and zooms typically come in a cascade of similar jobs.
             * If a subsample plot is happening, only let that one complete
             * and not the full plot, so that the screen refresh happens
             * reasonably quickly. 
             * If the plot is different (different layers or data) then
             * cancel the existing plot immediately and start work on
             * a new one. */
            boolean mayInterruptIfRunning = nextIsNotSimilar;
            if ( stepCanceler_ != null ) {
                fullCanceler_.cancel( true );
                stepCanceler_.cancel( mayInterruptIfRunning );
            }
            else if ( fullCanceler_ != null ) {
                fullCanceler_.cancel( mayInterruptIfRunning );
            }
        }

        /**
         * Accepts a workings object calculated for this job and applies
         * it to the parent panel.
         * May be called from any thread.
         *
         * @param  workings  workings object, may be null
         */
        private void submitWorkings( final Workings<A> workings ) {

            /* A null result may mean that the plot was interrupted or
             * that the result was the same as for the previously
             * calculated plot.  Either way, keep the same output
             * graphics as before.  If the return is non-null,
             * repaint it. */
            if ( workings != null ) {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        boolean plotChange =
                            ! workings.getDataIconId()
                             .equals( workings_.getDataIconId() );
                        workings_ = workings;
                        axisController_.setAspect( workings.aspect_ );
                        axisController_.setRanges( workings.geomRanges_ );
                        repaint();

                        /* If the plot changed materially, notify listeners. */
                        if ( plotChange ) {
                            fireChangeEvent();
                        }
                    }
                } );
            }
        }

        /**
         * Indicates whether the plot job which this object will execute
         * is similar to a given plot job, for the purposes of working out
         * whether to let an old job complete.
         *
         * @param  plotJob   other plot job
         * @return  true iff this object's job is like the other one
         */
        public boolean isSimilar( PlotJob otherJob ) {
            return simObj_.equals( getSimilarityObject( otherJob ) );
        }

        /**
         * Returns an identity object representing the parts of a plot job
         * that must be equal between two instances to confer similarity
         * for the purposes of working out whether to interrupt a previous job.
         * This notion of similarity is not very well defined, and may be
         * adjusted in the future; it's a case of trying to get something
         * which works well in the UI.
         *
         * <p>For now two jobs are characterised as similar if they have
         * the same list of layer types, in the same order, with the same
         * data.  However the plot surface and the layer styles may change.
         * This accommodates as similar for instance panned/zoomed versions
         * of the same plot or versions which differ by having the
         * transparency limit adjusted.
         *
         * @param  plotJob  job to identify
         * @return   list of layerIds
         */
        @Equality
        private Object getSimilarityObject( PlotJob plotJob ) {
            return layerListNoStyle( plotJob == null ? new PlotLayer[ 0 ]
                                                     : plotJob.layers_ );
        }
    }

    /**
     * Icon used for point highlighting.
     */
    private static class HighlightIcon implements Icon {
        private final int size_;
        private final int size2_;
        private final Stroke stroke_ =
            new BasicStroke( 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
        private final Map<RenderingHints.Key,Object> hints_;
        private final Color color1_ = new Color( 0xffffff );
        private final Color color2_ = new Color( 0x000000 );
        HighlightIcon() {
            size_ = 6;
            size2_ = size_ * 2 + 1;
            hints_ = new HashMap<RenderingHints.Key,Object>();
            hints_.put( RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY );
        }
        public int getIconWidth() {
            return size2_;
        }
        public int getIconHeight() {
            return size2_;
        }
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke stroke0 = g2.getStroke();
            Color color0 = g2.getColor();
            RenderingHints hints0 = g2.getRenderingHints();
            g2.setRenderingHints( hints_ );
            g2.setStroke( stroke_ );
            int xoff = x + size_;
            int yoff = y + size_;
            g2.translate( xoff, yoff );
            g2.setColor( color1_ );
            drawTarget( g2, size_ - 1 );
            g2.setColor( color2_ );
            drawTarget( g2, size_ );
            g2.translate( -xoff, -yoff );
            g2.setColor( color0 );
            g2.setStroke( stroke0 );
            g2.setRenderingHints( hints0 );
        }
        private static void drawTarget( Graphics g, int size ) {
            int size2 = size * 2 + 1;
            int s = size - 2;
            int s2 = s * 2;
            g.drawOval( -size, -size, size2, size2 );
            g.drawLine( 0, +s, 0, +s2 );
            g.drawLine( 0, -s, 0, -s2 );
            g.drawLine( +s, 0, +s2, 0 );
            g.drawLine( -s, 0, -s2, 0 );
        }
    }
}
