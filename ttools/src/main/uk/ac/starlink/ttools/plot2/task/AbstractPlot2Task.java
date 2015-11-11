package uk.ac.starlink.ttools.plot2.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.func.Strings;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.LegendIcon;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.KeySet;
import uk.ac.starlink.ttools.plot2.config.LoggingConfigMap;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;
import uk.ac.starlink.ttools.plottask.PaintMode;
import uk.ac.starlink.ttools.plottask.PaintModeParameter;
import uk.ac.starlink.ttools.plottask.Painter;
import uk.ac.starlink.ttools.plottask.SwingPainter;
import uk.ac.starlink.ttools.task.AddEnvironment;
import uk.ac.starlink.ttools.task.ConsumerTask;
import uk.ac.starlink.ttools.task.DoubleArrayParameter;
import uk.ac.starlink.ttools.task.DynamicTask;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.StringMultiParameter;
import uk.ac.starlink.ttools.task.TableProducer;

/**
 * Abstract superclass for tasks performing plot2 plots using STILTS.
 * Concrete subclasses must supply the PlotType (perhaps from the
 * environment), and may customise the visible task parameter set.
 *
 * @author   Mark Taylor
 * @since    22 Aug 2014
 */
public abstract class AbstractPlot2Task implements Task, DynamicTask {

    private final boolean allowAnimate_;
    private final IntegerParameter xpixParam_;
    private final IntegerParameter ypixParam_;
    private final InsetsParameter insetsParam_;
    private final PaintModeParameter painterParam_;
    private final DataStoreParameter dstoreParam_;
    private final StringMultiParameter seqParam_;
    private final BooleanParameter legendParam_;
    private final BooleanParameter legborderParam_;
    private final BooleanParameter legopaqueParam_;
    private final DoubleArrayParameter legposParam_;
    private final StringMultiParameter legseqParam_;
    private final StringParameter titleParam_;
    private final StringParameter auxlabelParam_;
    private final DoubleParameter auxcrowdParam_;
    private final BooleanParameter auxvisibleParam_;
    private final BooleanParameter bitmapParam_;
    private final Parameter<Compositor> compositorParam_;
    private final InputTableParameter animateParam_;
    private final FilterParameter animateFilterParam_;
    private final IntegerParameter parallelParam_;
    private final Parameter[] basicParams_;

    public static final String LAYER_PREFIX = "layer";
    private static final String TABLE_PREFIX = "in";
    private static final String FILTER_PREFIX = "icmd";
    public static final String EXAMPLE_LAYER_SUFFIX = "N";
    private static final GraphicExporter[] EXPORTERS =
        GraphicExporter.getKnownExporters( PlotUtil.LATEX_PDF_EXPORTER );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.task" );

    /**
     * Constructor with explicit animation capability.
     *
     * @param  allowAnimate  true iff animation options should be provided
     */
    protected AbstractPlot2Task( boolean allowAnimate ) {
        allowAnimate_ = allowAnimate;
        List<Parameter> plist = new ArrayList<Parameter>();

        insetsParam_ = new InsetsParameter( "insets" );

        xpixParam_ = new IntegerParameter( "xpix" );
        xpixParam_.setPrompt( "Total horizontal size in pixels" );
        xpixParam_.setDescription( new String[] {
            "<p>Size of the output image in the X direction in pixels.",
            "This includes space for any axis labels, padding",
            "and other decoration outside the plot area itself.",
            "See also <code>" + insetsParam_.getName() + "</code>.",
            "</p>",
        } );
        xpixParam_.setIntDefault( 500 );
        xpixParam_.setMinimum( 1 );
        plist.add( xpixParam_ );

        ypixParam_ = new IntegerParameter( "ypix" );
        ypixParam_.setPrompt( "Total vertical size in pixels" );
        ypixParam_.setDescription( new String[] {
            "<p>Size of the output image in the Y direction in pixels.",
            "This includes space for any axis labels, padding",
            "and other decoration outside the plot area itself.",
            "See also <code>" + insetsParam_.getName() + "</code>.",
            "</p>",
        } );
        ypixParam_.setIntDefault( 400 );
        ypixParam_.setMinimum( 1 );
        plist.add( ypixParam_ );

        insetsParam_.setPrompt( "Space outside plotting area" );
        insetsParam_.setDescription( new String[] {
            "<p>Defines the amount of space in pixels around the",
            "actual plotting area.",
            "This space is used for axis labels, and other decorations",
            "and any left over forms an empty border.",
            "</p>",
            "<p>The size and position of the actual plotting area",
            "is determined by this parameter along with", 
            "<code>" + xpixParam_ + "</code> and",
            "<code>" + ypixParam_ + "</code>.",
            "If no value is set (the default), the insets will be determined",
            "automatically according to how much space is required for",
            "labels etc.",
            "</p>",
        } );
        plist.add( insetsParam_ );

        painterParam_ = createPaintModeParameter();
        plist.add( painterParam_ );

        dstoreParam_ = new DataStoreParameter( "storage" );
        dstoreParam_.setDescription( new String[] {
           dstoreParam_.getDescription(),
           "<p>The default value is",
           "<code>" + dstoreParam_.getName( dstoreParam_
                                           .getDefaultForCaching( true ) )
                    + "</code>",
           "if a live plot is being generated",
           "(<code>" + painterParam_.getName() + "="
                     + painterParam_.getName( PaintMode.SWING_MODE )
                     + "</code>),",
           "since in that case the plot needs to be redrawn every time",
           "the user performs plot navigation actions or resizes the window,",
           "or if animations are being produced.",
           "Otherwise (e.g. output to a graphics file) the default is",
           "<code>" + dstoreParam_.getName( dstoreParam_
                                           .getDefaultForCaching( false ) )
                    + "</code>.",
           "</p>",
        } );
        plist.add( dstoreParam_ );

        seqParam_ = new StringMultiParameter( "seq", ',' );
        seqParam_.setUsage( "<suffix>[,...]" );
        seqParam_.setPrompt( "Order in which to plot layers" );
        String osfix = "&lt;" + EXAMPLE_LAYER_SUFFIX + "&gt;";
        seqParam_.setDescription( new String[] {
            "<p>Contains a comma-separated list of layer suffixes",
            "to determine the order in which layers are drawn on the plot.",
            "This can affect which symbol are plotted on top of,",
            "and so potentially obscure, which other ones.",
            "</p>",
            "<p>When specifying a plot, multiple layers may be specified,",
            "each introduced by a parameter",
            "<code>" + LAYER_PREFIX + osfix + "</code>,",
            "where <code>" + osfix + "</code> is a different (arbitrary)",
            "suffix labelling the layer,",
            "and is appended to all the parameters",
            "specific to defining that layer.",
            "</p>",
            "<p>By default the layers are drawn on the plot in the order",
            "in which the <code>" + LAYER_PREFIX + "*</code> parameters",
            "appear on the command line.",
            "However if this parameter is specified, each comma-separated",
            "element is interpreted as a layer suffix,",
            "giving the ordered list of layers to plot.",
            "Every element of the list must be a suffix with a corresponding",
            "<code>" + LAYER_PREFIX + "</code> parameter,",
            "but missing or repeated elements are allowed.",
            "</p>",
        } );
        seqParam_.setNullPermitted( true );
        plist.add( seqParam_ );

        legendParam_ = new BooleanParameter( "legend" );
        legendParam_.setPrompt( "Show legend?" );
        legendParam_.setDescription( new String[] {
            "<p>Whether to draw a legend or not.",
            "If no value is supplied, the decision is made automatically:",
            "a legend is drawn only if it would have more than one entry.",
            "</p>",
        } );
        legendParam_.setNullPermitted( true );
        plist.add( legendParam_ );

        legborderParam_ = new BooleanParameter( "legborder" );
        legborderParam_.setPrompt( "Border around legend?" );
        legborderParam_.setDescription( new String[] {
            "<p>If true, a line border is drawn around the legend.",
            "</p>",
        } );
        legborderParam_.setBooleanDefault( true );
        plist.add( legborderParam_ );

        legopaqueParam_ = new BooleanParameter( "legopaque" );
        legopaqueParam_.setPrompt( "Legend background opaque?" );
        legopaqueParam_.setDescription( new String[] {
            "<p>If true, the background of the legend is opaque,",
            "and the legend obscures any plot components behind it.",
            "Otherwise, it's transparent.",
            "</p>",
        } );
        legopaqueParam_.setBooleanDefault( true );
        plist.add( legopaqueParam_ );

        legposParam_ = new DoubleArrayParameter( "legpos", 2 );
        legposParam_.setUsage( "<xfrac>,<yfrac>" );
        legposParam_.setPrompt( "X,Y fractional internal legend position" );
        legposParam_.setDescription( new String[] {
            "<p>Determines the position of the legend on the plot,",
            "if present.",
            "The value is a comma-separated pair of values giving the",
            "X and Y positions of the legend within the plotting bounds,",
            "so for instance \"<code>0.5,0.5</code>\" will put the legend",
            "right in the middle of the plot.",
            "If no value is supplied, the legend will appear outside",
            "the plot boundary.",
            "</p>",
        } );
        legposParam_.setNullPermitted( true );
        plist.add( legposParam_ );

        legseqParam_ = new StringMultiParameter( "legseq", ',' );
        legseqParam_.setUsage( "<suffix>[,...]" );
        legseqParam_.setPrompt( "Order in which to add layers to legend" );
        legseqParam_.setDescription( new String[] {
            "<p>Determines which layers are represented in the legend",
            "(if present) and in which order they appear.", 
            "The legend has a line for each layer label",
            "(as determined by the",
            "<code>" + createLabelParameter( EXAMPLE_LAYER_SUFFIX ) + "</code>",
            "parameter).",
            "If multiple layers have the same label,",
            "they will contribute to the same entry in the legend,",
            "with style icons plotted over each other.",
            "The value of this parameter is a sequence of layer suffixes,",
            "which determines the order in which the legend entries appear.",
            "Layers with suffixes missing from this list",
            "do not show up in the legend at all.",
            "</p>",
            "<p>If no value is supplied (the default),",
            "the sequence is the same as the layer plotting sequence",
            "(see <code>" + seqParam_.getName() + "</code>).",
            "</p>",
        } );
        legseqParam_.setNullPermitted( true );
        plist.add( legseqParam_ );

        titleParam_ = new StringParameter( "title" );
        titleParam_.setPrompt( "Title for plot" );
        titleParam_.setDescription( new String[] {
            "<p>Text of a title to be displayed at the top of the plot.",
            "If null, the default, no title is shown",
            "and there's more space for the graphics.",
            "</p>",
        } );
        titleParam_.setNullPermitted( true );
        plist.add( titleParam_ );

        plist.addAll( getKeyParams( StyleKeys.AUX_RAMP.getKeys() ) );
        plist.add( new ConfigParameter( StyleKeys.SHADE_LOW ) );
        plist.add( new ConfigParameter( StyleKeys.SHADE_HIGH ) );

        auxlabelParam_ = new StringParameter( "auxlabel" );
        auxlabelParam_.setUsage( "<text>" );
        auxlabelParam_.setPrompt( "Label for aux axis" );
        auxlabelParam_.setDescription( new String[] {
            "<p>Sets the label used to annotate the aux axis,",
            "if it is visible.",
            "</p>",
        } );
        auxlabelParam_.setNullPermitted( true );
        plist.add( auxlabelParam_ );

        auxcrowdParam_ = new DoubleParameter( "auxcrowd" );
        auxcrowdParam_.setUsage( "<factor>" );
        auxcrowdParam_.setPrompt( "Tick crowding on aux axis" );
        auxcrowdParam_.setDescription( new String[] {
            "<p>Determines how closely the tick marks are spaced",
            "on the Aux axis, if visible.",
            "The default value is 1, meaning normal crowding.",
            "Larger values result in more ticks,",
            "and smaller values fewer ticks.",
            "Tick marks will not however be spaced so closely that",
            "the labels overlap each other,",
            "so to get very closely spaced marks you may need to",
            "reduce the font size as well.",
            "</p>",
        } );
        auxcrowdParam_.setDoubleDefault( 1 );
        plist.add( auxcrowdParam_ );

        auxvisibleParam_ = new BooleanParameter( "auxvisible" );
        auxvisibleParam_.setPrompt( "Display aux colour ramp?" );
        auxvisibleParam_.setDescription( new String[] {
            "<p>Determines whether the aux axis colour ramp",
            "is displayed alongside the plot.",
            "</p>",
            "<p>If not supplied (the default),",
            "the aux axis will be visible when aux shading is used",
            "in any of the plotted layers.",
            "</p>",
        } );
        auxvisibleParam_.setNullPermitted( true );
        plist.add( auxvisibleParam_ );

        bitmapParam_ = new BooleanParameter( "forcebitmap" );
        bitmapParam_.setPrompt( "Force non-vector graphics output?" );
        bitmapParam_.setDescription( new String[] {
            "<p>This option only has an effect when writing output",
            "to vector graphics formats (PDF and PostScript).",
            "If set <code>true</code>, the data contents of the plot",
            "are drawn as a pixel map embedded into the output",
            "file rather than plotting each point in the output.",
            "This may make the output less beautiful",
            "(round markers will no longer be perfectly round),",
            "but it may result in a much smaller file",
            "if there are very many data points. Plot annotations such as",
            "axis labels will not be affected - they are still drawn as",
            "vector text.",
            "Note that in some cases",
            "(e.g. <code>" + ShapeFamilyLayerType.SHADING_PREFIX
                           + EXAMPLE_LAYER_SUFFIX + "="
                           + ShapeMode.AUTO.getModeName() + "</code>",
            "or    <code>" + ShapeFamilyLayerType.SHADING_PREFIX
                           + EXAMPLE_LAYER_SUFFIX + "="
                           + ShapeMode.DENSITY.getModeName() + "</code>)",
            "this kind of pixellisation will happen in any case.",
            "</p>",
        } );
        bitmapParam_.setBooleanDefault( false );
        plist.add( bitmapParam_ );

        compositorParam_ = new CompositorParameter( "compositor" );
        plist.add( compositorParam_ );

        if ( allowAnimate ) {
            animateParam_ = new InputTableParameter( "animate" );
            animateParam_.setNullPermitted( true );
            animateParam_.setTableDescription( "the animation control table" );
            animateParam_.setDescription( new String[] {
                "<p>If not null, this parameter causes the command",
                "to create a sequence of plots instead of just one.",
                "The parameter value is a table with one row for each",
                "frame to be produced.",
                "Columns in the table are interpreted as parameters",
                "which may take different values for each frame;",
                "the column name is the parameter name,",
                "and the value for a given frame is its value from that row.",
                "Animating like this is considerably more efficient",
                "than invoking the STILTS command in a loop.",
                "</p>",
                animateParam_.getDescription(),
            } );
            plist.add( animateParam_ );
            plist.add( animateParam_.getFormatParameter() );
            plist.add( animateParam_.getStreamParameter() );
            animateFilterParam_ = new FilterParameter( "acmd" );
            animateFilterParam_
               .setTableDescription( "the animation control table",
                                     animateParam_, Boolean.TRUE );
            plist.add( animateFilterParam_ );
            parallelParam_ = new IntegerParameter( "parallel" );
            parallelParam_.setPrompt( "Parallelism for animation frames" );
            parallelParam_.setDescription( new String[] {
                "<p>Determines how many threads will run in parallel",
                "if animation output is being produced.",
                "Only used if the <code>" + animateParam_.getName() + "</code>",
                "parameter is supplied.",
                "The default value is the number of processors apparently",
                "available to the JVM.",
                "</p>",
            } );
            parallelParam_.setMinimum( 1 );
            parallelParam_.setIntDefault( Runtime.getRuntime()
                                         .availableProcessors() );
            plist.add( parallelParam_ );
        }
        else {
            animateParam_ = null;
            animateFilterParam_ = null;
            parallelParam_ = null;
        }
        basicParams_ = plist.toArray( new Parameter[ 0 ] );
    }

    /**
     * Constructor with default animation capability.
     */
    protected AbstractPlot2Task() {
        this( true );
    }

    /**
     * Concrete subclasses must implement this method to provide
     * the PlotType and other information from the environment
     * that may not be available at construction time.
     *
     * @param  env  execution environment
     * @return  context
     */
    public abstract PlotContext getPlotContext( Environment env )
            throws TaskException;

    /**
     * Returns a config parameter for a given config key that may be
     * sensitive to the content of the execution environment.
     * This is here to provide a hook for subclasses to set up defaults
     * for some config parameters on the basis of what layers are present.
     *
     * @param   env  execution environment
     * @param   key  config key for which a parameter is required
     * @param   suffixes  ordered list of the plot layer suffixes
     *          in use for the plot being performed
     * @return   parameter to get the value of <code>key</code>
     */
    protected abstract <T> ConfigParameter
            createConfigParameter( Environment env, ConfigKey<T> key,
                                   String[] suffixes )
            throws TaskException;

    /**
     * Returns the list of parameters supplied by the AbstractPlot2Task
     * implementation.  Subclasses should include these alongside any
     * they want to add for presentation to the user.
     *
     * @return  basic parameter list
     */
    public final Parameter[] getBasicParameters() {
        return basicParams_;
    }

    public Executable createExecutable( final Environment env )
            throws TaskException {
        final PlotContext context = getPlotContext( env );
        final Painter painter = painterParam_.painterValue( env );
        final boolean isSwing = painter instanceof SwingPainter;
        final TableProducer animateProducer =
              allowAnimate_
            ? ConsumerTask.createProducer( env, animateFilterParam_,
                                           animateParam_ )
            : null;
        boolean isAnimate = animateProducer != null;
        dstoreParam_.setDefaultCaching( isSwing || isAnimate );

        /* Single frame: prepare operation and return an executable that
         * has no reference to the environment. */
        if ( ! isAnimate ) {
            final PlotExecutor executor = createPlotExecutor( env, context );
            return new Executable() {
                public void execute() throws IOException {
                    DataStore dataStore;
                    try {
                        dataStore = executor.createDataStore( null );
                    }
                    catch ( InterruptedException e ) {
                        Thread.currentThread().isInterrupted();
                        return;
                    }

                    /* For an active display, create and post a component
                     * which will draw the requested plot on demand,
                     * including resizing when appropriate. */
                    if ( isSwing ) {
                        final JComponent panel =
                            executor.createPlotComponent( dataStore,
                                                          true, true );
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                ((SwingPainter) painter).postComponent( panel );
                            }
                        } );
                    }

                    /* For a static plot, generate and plot
                     * the fixed icon here. */
                    else {
                        Icon plot = executor.createPlotIcon( dataStore );
                        long start = System.currentTimeMillis();
                        painter.paintPicture( PlotUtil.toPicture( plot ) );
                        PlotUtil.logTime( logger_, "Plot", start );
                    }
                }
            };
        }

        /* Animation.  This works by reading a row from a supplied table
         * for each output frame.  Each column of the table represents
         * (and is named as) one of the parameters of this task that
         * can change between frames.  This means the task operates
         * in a rather non-standard way: most of the execution
         * environment is kept, modified, and interrogated during the task
         * execution (Executable.execute() method) rather than being
         * interrogated and thrown away after the Executable is created. */
        else {

            /* First, read the animation table for later use. */
            final StarTable animateTable;
            final ColumnInfo[] infos;
            final long nrow;
            Object[] row0;
            Environment env0;
            try {
                Row0Table atable = new Row0Table( animateProducer.getTable() );
                animateTable = atable;
                infos = Tables.getColumnInfos( animateTable );
                nrow = animateTable.getRowCount();

                /* We also read the first row, for preparing a dummy frame. */
                row0 = atable.getRow0();
                env0 = createFrameEnvironment( env, infos, row0, 0, nrow );
            }
            catch ( IOException e ) {
                throw new ExecutionException( "Error reading animation table: "
                                            + e, e );
            }

            /* This line prepares to paint a dummy frame, but doesn't do it
             * (the created executor is just discarded).
             * The purpose of this is to read the variables from the execution
             * environment whose values are required to specify a frame of
             * the animation.  In this way, any parameter errors can be
             * identified now and passed back to the user, rather than
             * showing up during the actual execution.  For related reasons,
             * if we didn't do this, the parameter system would complain
             * that there are unused parameters in the environment. */
            createPlotExecutor( env0, context );

            /* Screen animation. */
            if ( isSwing ) {
                return new Executable() {
                    public void execute() throws IOException, TaskException {
                        try {
                            animateSwing( env, context, animateTable );
                        }
                        catch ( InterruptedException e ) {
                            Thread.currentThread().isInterrupted();
                            return;
                        }
                    }
                };
            }

            /* File output animation. */
            else {
                final String out0 = getPainterOutputName( env0 );
                final int parallel = parallelParam_.intValue( env );
                return new Executable() {
                    public void execute() throws IOException, TaskException {
                        try {
                            animateOutput( env, context, animateTable,
                                           parallel, out0 );
                        }
                        catch ( InterruptedException e ) {
                            Thread.currentThread().isInterrupted();
                            return;
                        }
                    }
                };
            }
        }
    }

    /**
     * Paints a sequence of animation frames under control of a parameter
     * table, outputting the result to a sequence of files.
     *
     * @param  baseEnv  base execution environment
     * @param  context  plot context
     * @param  animateTable  table providing per-frame adjustments
     *                       to environment
     * @param  parallel  thread count for calculations
     * @param  out0  name of first output frame
     */
    private void animateOutput( Environment baseEnv, PlotContext context,
                                StarTable animateTable, int parallel,
                                String out0 )
            throws TaskException, IOException, InterruptedException {
        ColumnInfo[] infos = Tables.getColumnInfos( animateTable );
        long nrow = animateTable.getRowCount();
        int nthr = parallel;
        ExecutorService paintService =
            new ThreadPoolExecutor( nthr, nthr, 60, TimeUnit.SECONDS,
                                    new ArrayBlockingQueue<Runnable>( nthr ),
                                    new ThreadPoolExecutor.CallerRunsPolicy() );
        RowSequence aseq = animateTable.getRowSequence();
        DataStore lastDataStore = null;
        String lastOutName = null;
        try {
            for ( long irow = 0; aseq.next(); irow++ ) {
                Environment frameEnv =
                    createFrameEnvironment( baseEnv, infos, aseq.getRow(),
                                            irow, nrow );
                final PlotExecutor executor =
                    createPlotExecutor( frameEnv, context );
                final Painter painter = getPainter( frameEnv );
                final DataStore dstore =
                    executor.createDataStore( lastDataStore );
                final String outName = getPainterOutputName( frameEnv );
                paintService.submit( new Callable<Void>() {
                    public Void call() throws IOException {
                        long start = System.currentTimeMillis();
                        Icon plot = executor.createPlotIcon( dstore );
                        painter.paintPicture( PlotUtil.toPicture( plot ) );
                        PlotUtil.logTime( logger_, "Plot " + outName, start );
                        return null;
                    }
                } );
                lastOutName = outName;
                lastDataStore = dstore;
            }
        }
        finally {
            aseq.close();
        }
        paintService.shutdown();
        paintService.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
        logger_.warning( "Wrote " + nrow + " frames, "
                       + out0 + " .. " + lastOutName );
    }

    /**
     * Paints a sequence of animation frames under control of a parameter
     * table, displaying the results in a screen component.
     *
     * @param  baseEnv  base execution environment
     * @param  context  plot context
     * @param  animateTable  table providing per-frame adjustments
     *                       to environment
     */
    private void animateSwing( Environment baseEnv, PlotContext context,
                               StarTable animateTable )
            throws TaskException, IOException, InterruptedException {
        final SwingPainter painter =
            (SwingPainter) createPaintModeParameter().painterValue( baseEnv );
        ColumnInfo[] infos = Tables.getColumnInfos( animateTable );
        long nrow = animateTable.getRowCount(); 
        RowSequence aseq = animateTable.getRowSequence();
        final JComponent holder = new JPanel( new BorderLayout() );
        DataStore dataStore = null;

        /* The swing animation is not parallelised, but should be.
         * It's like this.  The work is not (mostly) done by creating the
         * PlotDisplay component plot component, it's done when that
         * component paints itself, i.e. on the event dispatch thread,
         * so I can't easily do parallel plotting and feed the results
         * to the EDT to display.
         * To parallelise it, I should cause that painting work to be
         * done before the component is posted, and cached within the
         * PlotDisplay object ready for fast plotting when it becomes visible.
         * PlotDisplay is more or less set up to do this, but it would
         * need to know its dimensions to know how to do the plot,
         * which requires a bit of additional plumbing. */
        try {
            for ( long irow = 0; aseq.next(); irow++ ) {
                Environment frameEnv =
                    createFrameEnvironment( baseEnv, infos, aseq.getRow(),
                                            irow, nrow );
                PlotExecutor executor = createPlotExecutor( frameEnv, context );
                dataStore = executor.createDataStore( dataStore );
                final JComponent panel =
                    executor.createPlotComponent( dataStore, true, true );
                final boolean init = irow == 0;

                /* It's necessary to use invokeAndWait here, since the
                 * display painting is slow.  If invokeLater is used,
                 * most frames are never seen.  If I fix it so that
                 * most of the work is done outside the EDT,
                 * I can change this back to invokeLater. */
                try {
                    SwingUtilities.invokeAndWait( new Runnable() {
                        public void run() {
                            holder.removeAll();
                            holder.add( panel, BorderLayout.CENTER );
                            holder.revalidate();
                            holder.repaint();
                            if ( init ) {
                                painter.postComponent( holder );
                            }                   
                        }
                    } );
                }
                catch( InvocationTargetException e ) {
                    throw new TaskException( "Painting error: " + e, e );
                }
            }
        }
        finally {
            aseq.close();
        }
    }

    /**
     * Returns an execution environment based on a given static environment,
     * but augmented by values from a row of an animation table.
     *
     * @param  baseEnv  base environment
     * @param  colInfos  columns of animation table named as task parameters
     * @param  row    single row of animation table to augment environment
     * @param  irow   row index
     * @param  nrow   total row count (-1 if not known)
     * @return  environment for single animation frame
     */
    private Environment createFrameEnvironment( Environment baseEnv,
                                                ColumnInfo[] colInfos,
                                                Object[] row, long irow,
                                                long nrow )
            throws IOException, TaskException {
        OutputStreamParameter outParam = painterParam_.getOutputParameter();
        boolean hasPaintout = false;

        /* Prepare a map containing the values in the animation table row,
         * keyed by column name (=parameter name). */
        Map<String,String> map = new HashMap<String,String>();
        int ncol = colInfos.length;
        for ( int ic = 0; ic < ncol; ic++ ) {
            String pname = colInfos[ ic ].getName();
            hasPaintout = hasPaintout || pname.equals( outParam.getName() );
            Object cell = row[ ic ];
            if ( cell != null ) {
                map.put( pname, cell.toString() );
            }
        }

        /* Mangle the output filename if present and if explicit values are
         * not present in the animation table; append a frame number. */
        if ( ! hasPaintout &&
             ! ( painterParam_.painterValue( baseEnv )
                 instanceof SwingPainter ) ) {
            String baseOut = outParam.stringValue( baseEnv );
            int numpos = baseOut.lastIndexOf( '.' );
            if ( numpos < 0 ) {
                numpos = baseOut.length() - 1;
            }
            StringBuffer frameOut = new StringBuffer( baseOut );
            int ndigit = nrow > 0 ? (int) Math.ceil( Math.log10( nrow ) )
                                  : 3;
            String snum = "-" + Strings.padWithZeros( irow + 1, ndigit );
            frameOut.insert( numpos, snum );
            map.put( outParam.getName(), frameOut.toString() );
        }

        /* Return an environment with these additions. */
        return AddEnvironment.createAddEnvironment( baseEnv, map );
    }

    /**
     * Returns an Icon that paints the plot described
     * by a value-bearing execution environment.
     * This utility method is not used for executing this class.
     *
     * @param  env  execution environment
     * @return  plot icon
     */
    public Icon createPlotIcon( Environment env )
            throws TaskException, IOException, InterruptedException {
        dstoreParam_.setDefaultCaching( false );
        PlotExecutor executor =
            createPlotExecutor( env, getPlotContext( env ) );
        return executor.createPlotIcon( executor.createDataStore( null ) );
    }

    /**
     * Returns a graphical component that displays an interactive view of
     * the plot described by a value-bearing execution environment.
     * This utility method is not used for executing the task defined by
     * this class.
     *
     * @param  env  execution environment
     * @param  caching  whether data and plot should be cached or re-read
     *                  at every repaint
     * @return  active plot view component
     */
    public PlotDisplay createPlotComponent( Environment env, boolean caching )
            throws TaskException, IOException, InterruptedException {
        dstoreParam_.setDefaultCaching( caching );
        PlotExecutor executor =
            createPlotExecutor( env, getPlotContext( env ) );
        return executor.createPlotComponent( executor.createDataStore( null ),
                                             true, caching );
    }

    public Parameter[] getContextParameters( Environment env )
            throws TaskException {

        /* Initialise list with non-context-sensitive parameters. */
        List<Parameter> paramList = new ArrayList<Parameter>();
        paramList.addAll( Arrays.asList( getParameters() ) );

        /* Go through each layer that has been set in the environment
         * (by a layerN setting).  Get all the parameters associated
         * with that layer type and suffix. */
        PlotContext context = getPlotContext( env );
        for ( Map.Entry<String,LayerType> entry :
              getLayers( env, context ).entrySet() ) {
            String suffix = entry.getKey();
            LayerType layer = entry.getValue();

            /* Add an entry for the layer parameter itself, with a fixed
             * value. */
            LayerTypeParameter layerParam =
                createLayerTypeParameter( suffix, context );
            layerParam.setUsage( layerParam.stringifyOption( layer ) );
            paramList.add( layerParam );

            /* Add entries for the parameters associated with that
             * layer type. */
            for ( ParameterFinder finder :
                  getLayerParameterFinders( env, context, layer, suffix ) ) {
                paramList.add( finder.createParameter( suffix ) );
            }
        }
        return paramList.toArray( new Parameter[ 0 ] );
    }

    public Parameter getParameterByName( Environment env, String paramName )
            throws TaskException {

        /* Check if the parameter is a layer parameter itself. */
        if ( paramName.toLowerCase()
                      .startsWith( LAYER_PREFIX.toLowerCase() ) ) {
            String suffix = paramName.substring( LAYER_PREFIX.length() );
            PlotContext context = getPlotContext( env );
            return createLayerTypeParameter( suffix, context );
        }

        /* Otherwise, find each layer that has been set in the environment
         * (by a layerN setting).  Find its layer type and suffix.
         * Then it's a case of going through all the parameters that
         * come with that layer type to see if any of them match the
         * requested on by name. */
        PlotContext context = getPlotContext( env );
        for ( Map.Entry<String,LayerType> entry :
              getLayers( env, context ).entrySet() ) {
            String suffix = entry.getKey();
            LayerType layer = entry.getValue();
            for ( ParameterFinder finder :
                  getLayerParameterFinders( env, context, layer, suffix ) ) {
                Parameter p = finder.findParameterByName( paramName, suffix );
                if ( p != null ) {
                    return p;
                }
            }
        }

        /* No luck. */
        return null;
    }

    /**
     * Returns a list of parameter finders for parameters specific to
     * a given layer.  These can be used to find all the parameters
     * which are only present in virtue of the existence of a given
     * plot layer.
     *
     * @param   env  execution environment
     * @param   context  plot context
     * @param   layer   plot layer for which parameters are required
     * @param   suffix  suffix associated with layer
     * @return  array of plot finder for layer-specific parameters
     */
    private ParameterFinder[]
            getLayerParameterFinders( Environment env,
                                      final PlotContext context,
                                      final LayerType layer,
                                      final String suffix )
            throws TaskException {
        List<ParameterFinder> finderList = new ArrayList<ParameterFinder>();

        /* Layer type associated parameters. */
        int nassoc = layer.getAssociatedParameters( "dummy" ).length;
        for ( int ia = 0; ia < nassoc; ia++ ) {
            final int iassoc = ia;
            finderList.add( new ParameterFinder<Parameter>() {
                public Parameter createParameter( String sfix ) {
                    return layer.getAssociatedParameters( sfix )[ iassoc ];
                }
            } );
        }

        /* Layer positional parameters. */
        int npos = layer.getPositionCount();
        DataGeom geom = context.getGeom( env, suffix );
        Coord[] posCoords = geom.getPosCoords();
        for ( int ipos = 0; ipos < npos; ipos++ ) {
            final String posSuffix = npos > 1
                                   ? PlotUtil.getIndexSuffix( ipos )   
                                   : "";
            for ( Coord coord : posCoords ) {
                for ( final Input input : coord.getInputs() ) {
                    finderList.add( new ParameterFinder<Parameter>() {
                        public Parameter createParameter( String sfix ) {
                            return createDataParameter( input, posSuffix + sfix,
                                                        true );
                        }
                    } );
                }
            }
        }

        /* Layer geometry-specific parameters. */
        Parameter[] geomParams = context.getGeomParameters( suffix );
        for ( int igp = 0; igp < geomParams.length; igp++ ) {
            final int igp0 = igp;
            finderList.add( new ParameterFinder<Parameter>() {
                public Parameter createParameter( String sfix ) {
                    return context.getGeomParameters( sfix )[ igp0 ];
                }
            } );
        }

        /* Layer non-positional parameters. */
        Coord[] extraCoords = layer.getExtraCoords();
        for ( Coord coord : extraCoords ) {
            for ( final Input input : coord.getInputs() ) {
                finderList.add( new ParameterFinder<Parameter>() {
                    public Parameter createParameter( String sfix ) {
                        return createDataParameter( input, sfix, true );
                    }
                } );
            }
        }

        /* Layer style parameters. */
        ConfigKey[] styleKeys = layer.getStyleKeys();
        for ( final ConfigKey key : styleKeys ) {
            finderList.add( new ParameterFinder<Parameter>() {
                public Parameter createParameter( String sfix ) {
                    return ConfigParameter
                          .createSuffixedParameter( key, sfix, true );
                }
            } );
        }

        /* Now try shading parameters if appropriate. */
        if ( layer instanceof ShapeFamilyLayerType ) {
            final ShapeFamilyLayerType shadeLayer =
                (ShapeFamilyLayerType) layer;
            ShapeMode shapeMode = new ParameterFinder<Parameter<ShapeMode>>() {
                public Parameter<ShapeMode> createParameter( String sfix ) {
                    return shadeLayer.createShapeModeParameter( sfix );
                }
            }.getParameter( env, suffix )
             .objectValue( env );

            /* Shading coordinate parameters. */
            Coord[] shadeCoords = shapeMode.getExtraCoords();
            for ( Coord coord : shadeCoords ) {
                for ( final Input input : coord.getInputs() ) {
                    finderList.add( new ParameterFinder<Parameter>() {
                        public Parameter createParameter( String sfix ) {
                            return createDataParameter( input, sfix, true );
                        }
                    } );
                }
            }

            /* Shading config parameters. */
            ConfigKey[] shadeKeys = shapeMode.getConfigKeys();
            for ( final ConfigKey key : shadeKeys ) {
                finderList.add( new ParameterFinder<Parameter>() {
                    public Parameter createParameter( String sfix ) {
                        return ConfigParameter
                              .createSuffixedParameter( key, sfix, true );
                    }
                } ); 
            }
        }
        return finderList.toArray( new ParameterFinder[ 0 ] );
    }

    /**
     * Gets a painter value from an environment.
     *
     * The implementation should be trivial (paintModeParam.painterValue(env))
     * but instead it requires a hack.
     *
     * @param  env    execution environment
     * @return   painter object
     */
    private Painter getPainter( Environment env ) throws TaskException {
        PaintModeParameter paintModeParam = createPaintModeParameter();

        /* The following line is the ghastly hack.  We have to force the
         * output parameter associated with the paint mode parameter to
         * acquire its value from the given environment.  Because of the
         * opaque and nasty way that Environment is specified, it's not
         * possible to write an Environment implementation that properly
         * delegates to another one for resolving associated variables. */
        env.acquireValue( paintModeParam.getOutputParameter() );
        return paintModeParam.painterValue( env );
    }

    /**
     * Returns the filename associated with the graphics output file from
     * a given environment.
     *
     * @param  env    execution environment
     * @return  output graphics filename
     */
    private String getPainterOutputName( Environment env )
            throws TaskException {
        OutputStreamParameter outParam =
            createPaintModeParameter().getOutputParameter();

        /* Hack - see getPainter method. */
        env.acquireValue( outParam );
        return outParam.stringValue( env );
    }

    /**
     * Turns an execution environment (containing value-bearing parameters)
     * into an object capable of performing a static or interactive plot.
     *
     * @param  env  execution environment
     * @param  context   plot context
     * @return   plot executor
     */
    private PlotExecutor createPlotExecutor( Environment env,
                                             PlotContext context )
            throws TaskException {

        /* What kind of plot? */
        PlotType plotType = context.getPlotType();
        final PaperTypeSelector ptsel = plotType.getPaperTypeSelector();

        /* Set up generic configuration. */
        final int xpix = xpixParam_.intValue( env );
        final int ypix = ypixParam_.intValue( env );
        final Insets insets = insetsParam_.insetsValue( env );
        final boolean forceBitmap = bitmapParam_.booleanValue( env );
        final boolean surfaceAuxRange = false;
        final DataStoreFactory storeFact = dstoreParam_.objectValue( env );
        final Compositor compositor = compositorParam_.objectValue( env );

        /* Gather the defined plot layers from the environment. */
        Map<String,PlotLayer> layerMap = createLayerMap( env, context );

        /* Get the sequence of layers, then of legend entries, to plot. */
        String[] layerSeq = seqParam_.stringsValue( env );
        if ( layerSeq.length == 0 ) {
            layerSeq = layerMap.keySet().toArray( new String[ 0 ] );
        }
        String[] legendSeq = legseqParam_.stringsValue( env );
        if ( legendSeq.length == 0 ) {
            legendSeq = layerSeq;
        }

        /* Get an ordered list of layers to plot. */
        final PlotLayer[] layers = getLayerSequence( layerMap, layerSeq );

        /* Prepare to draw aux axis. */
        final ShadeAxisFactory shadeFact =
            createShadeAxisFactory( env, layers );

        /* Prepare to acquire parameters based on config keys.
         * This can use subclass functionality to default axis names
         * from data coodinate parameter values. */
        final String[] suffixes = layerSeq;
        ConfigParameterFactory surfCpFact = new ConfigParameterFactory() {
            public <T> ConfigParameter<T> getParameter( Environment env,
                                                        ConfigKey<T> key )
                    throws TaskException {
                return createConfigParameter( env, key, suffixes );
            }
        };

        /* Get surface factory. */
        final SurfaceFactory<?,?> surfFact = plotType.getSurfaceFactory();

        /* Get the surface configuration. */
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();
        keyList.addAll( Arrays.asList( surfFact.getProfileKeys() ) );
        keyList.addAll( Arrays.asList( surfFact.getAspectKeys() ) );
        keyList.addAll( Arrays.asList( surfFact.getNavigatorKeys() ) );
        keyList.add( StyleKeys.SHADE_LOW );
        keyList.add( StyleKeys.SHADE_HIGH );
        ConfigKey[] surfKeys = keyList.toArray( new ConfigKey[ 0 ] );
        final ConfigMap surfConfig =
            createConfigMap( env, surfKeys, surfCpFact );
        final Range shadeFixRange =
            new Range( surfConfig.get( StyleKeys.SHADE_LOW ),
                       surfConfig.get( StyleKeys.SHADE_HIGH ) );

        /* Assemble the list of DataSpecs required for the plot. */
        int nl = layers.length;
        final DataSpec[] dataSpecs = new DataSpec[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            dataSpecs[ il ] = layers[ il ] == null
                            ? null
                            : layers[ il ].getDataSpec();
        }

        /* Get the legend. */
        final Icon legend = createLegend( env, layerMap, legendSeq );
        final float[] legpos = legend == null
                             ? null
                             : legposParam_.floatsValue( env );
        final String title = titleParam_.stringValue( env );

        /* We have all we need.  Construct and return the object
         * that can do the plot. */
        return new PlotExecutor() {

            public DataStore createDataStore( DataStore prevStore )
                    throws IOException, InterruptedException {
                long t0 = System.currentTimeMillis();
                DataStore store =
                    storeFact.readDataStore( dataSpecs, prevStore );
                PlotUtil.logTime( logger_, "Data", t0 );
                return store;
            }

            public PlotDisplay createPlotComponent( DataStore dataStore,
                                                    boolean navigable,
                                                    boolean caching ) {
                PlotDisplay panel =
                    PlotDisplay
                   .createPlotDisplay( layers, surfFact, surfConfig, legend,
                                       legpos, title, shadeFact, shadeFixRange,
                                       ptsel, compositor, dataStore,
                                       surfaceAuxRange, navigable, caching );
                panel.setPreferredSize( new Dimension( xpix, ypix ) );
                panel.setDataInsets( insets );
                return panel;
            }

            public Icon createPlotIcon( DataStore dataStore ) {
                return AbstractPlot2Task
                      .createPlotIcon( layers, surfFact, surfConfig, legend,
                                       legpos, title, shadeFact, shadeFixRange,
                                       ptsel, compositor, dataStore,
                                       xpix, ypix, insets, forceBitmap );
            }
        };
    }

    /**
     * Obtains a list of the PlotLayers specified by parameters in
     * the execution environment for a given PlotContext.
     *
     * @param   env  execution environment
     * @param   context  plot context
     * @return   suffix->layer map for all the layers specified
     *           by the environment
     */
    private Map<String,PlotLayer> createLayerMap( Environment env,
                                                  PlotContext context )
            throws TaskException {

        /* Work out what plotters/layers are requested. */
        Map<String,Plotter> plotterMap = getPlotters( env, context );

        /* For each plotter, create a PlotLayer based on it using the
         * appropriately suffix-coded parameters in the environment.
         * In this step we deliberately create all the specified layers
         * though some might not be used in future.
         * It's important to do it that way, so that the parameter system
         * does not report specified but unplotted layer parameters as unused.
         * Creating layer objects is in any case cheap. */
        Map<String,PlotLayer> layerMap = new LinkedHashMap<String,PlotLayer>();
        for ( Map.Entry<String,Plotter> entry : plotterMap.entrySet() ) {
            String suffix = entry.getKey();
            Plotter plotter = entry.getValue();
            DataGeom geom = plotter.getCoordGroup().getPositionCount() > 0
                          ? context.getGeom( env, suffix )
                          : null;
            PlotLayer layer = createPlotLayer( env, suffix, plotter, geom );
            layerMap.put( suffix, layer );
        }
        return layerMap;
    }

    /**
     * Turns the map of defined layers into an ordered sequence of layers
     * to plot.
     *
     * @param  layerMap  suffix->layer map for all defined layers
     * @return   ordered list of layers to be actually plotted
     */
    private PlotLayer[] getLayerSequence( Map<String,PlotLayer> layerMap,
                                          String[] suffixSeq )
            throws TaskException {
        int nl = suffixSeq.length;
        PlotLayer[] layers = new PlotLayer[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            String suffix = suffixSeq[ il ];
            PlotLayer layer = layerMap.get( suffix );
            if ( layer == null ) {
                String msg = new StringBuffer()
                    .append( "No specification for layer \"" )
                    .append( suffix )
                    .append( "\"" )
                    .append( "; known layers: " )
                    .append( layerMap.keySet() )
                    .toString();
                throw new ParameterValueException( seqParam_, msg );
            }
            else {
                layers[ il ] = layer;
            }
        }
        return layers;
    }

    /**
     * Turns the map of defined layers into a legend icon.
     *
     * @param  env  execution environment
     * @param  layerMap  suffix->layer map for all defined layers
     * @param  suffixSeq  ordered array of suffixes for layers to be plotted
     * @return  legend icon, may be null
     */
    private Icon createLegend( Environment env, Map<String,PlotLayer> layerMap,                                String[] suffixSeq )
            throws TaskException {

        /* Make a map from layer labels to arrays of styles.
         * Each entry in this map will correspond to a legend entry. */
        Map<List<SubCloud>,String> cloudMap =
            new LinkedHashMap<List<SubCloud>,String>();
        Map<String,List<Style>> labelMap =
            new LinkedHashMap<String,List<Style>>();
        for ( String suffix : suffixSeq ) {
            PlotLayer layer = layerMap.get( suffix );
            if ( layer == null ) {
                String msg = new StringBuffer()
                    .append( "No specification for layer \"" )
                    .append( suffix )
                    .append( "\"" )
                    .append( "; known layers: " )
                    .append( layerMap.keySet() )
                    .toString();
                throw new ParameterValueException( legseqParam_, msg );
            }

            /* If a legend label has been explicitly given for this layer,
             * use that. */
            String label = new ParameterFinder<Parameter<String>>() {
                public Parameter<String> createParameter( String sfix ) {
                    return createLabelParameter( sfix );
                }
            }.getParameter( env, suffix )
             .objectValue( env );

            /* Otherwise, work one out.  We don't give every layer its own
             * entry, but instead group layers together by their data
             * coordinates, since you may well have multiple layers with the
             * same data overplotted to achieve some visual effect.
             * The SubCloud is an object which can tell (testing by equality)
             * whether layers have the same positional coordinates. */
            if ( label == null ) {
                List<SubCloud> dataClouds = getPointClouds( layer );
                if ( ! cloudMap.containsKey( dataClouds ) ) {
                    String suffixLabel = suffix.length() == 0 ? "data" : suffix;
                    cloudMap.put( dataClouds, suffixLabel );
                }
                label = cloudMap.get( dataClouds );
            }
            if ( ! labelMap.containsKey( label ) ) {
                labelMap.put( label, new ArrayList<Style>() );
            }
            labelMap.get( label ).add( layer.getStyle() );
        }

        /* Turn the map into a list of LegendEntry objects. */
        List<LegendEntry> entryList = new ArrayList<LegendEntry>();
        for ( Map.Entry<String,List<Style>> entry : labelMap.entrySet() ) {
            String label = entry.getKey();
            Style[] styles = entry.getValue().toArray( new Style[ 0 ] );
            entryList.add( new LegendEntry( label, styles ) );
        }
        LegendEntry[] legEntries = entryList.toArray( new LegendEntry[ 0 ] );
 
        /* Work out if we are actually going to use a legend. */
        Boolean hasLegObj = legendParam_.objectValue( env );
        boolean hasLegend = hasLegObj == null ? legEntries.length > 1
                                              : hasLegObj.booleanValue();

        /* Construct and return the legend, or return null, as required. */
        if ( hasLegend ) {
            Captioner captioner = getCaptioner( env );
            boolean hasBorder = legborderParam_.booleanValue( env );
            boolean isOpaque = legopaqueParam_.booleanValue( env );
            Color bgColor = isOpaque ? Color.WHITE : null;
            return new LegendIcon( legEntries, captioner, hasBorder, bgColor );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a list of point clouds representing the positional coordinates
     * used to plot a layer.  The result is an object that can be compared
     * for equality to test whether layers are representing the same
     * positional data set.
     *
     * @return  layer  plot layer
     * @return   list of positional dataset identifiers
     */
    private static List<SubCloud> getPointClouds( PlotLayer layer ) {
        DataSpec dataSpec = layer.getDataSpec();
        DataGeom geom = layer.getDataGeom();
        CoordGroup cgrp = layer.getPlotter().getCoordGroup();
        int npos = cgrp.getPositionCount();
        List<SubCloud> cloudList = new ArrayList<SubCloud>( npos );
        for ( int ipos = 0; ipos < npos; ipos++ ) {
            int iposCoord = cgrp.getPosCoordIndex( ipos, geom );
            cloudList.add( new SubCloud( geom, dataSpec, iposCoord ) );
        }
        return cloudList;
    }

    /**
     * Returns a map of suffix strings to LayerType objects.
     * Each suffix string is appended to parameters associated with the
     * relevant LayerType as a namespacing device on the command line.
     *
     * @param  env  execution environment
     * @param  context  plot context
     * @return  mapping from suffixes to layer types for the environment
     */
    private Map<String,LayerType> getLayers( Environment env,
                                             PlotContext context )
            throws TaskException {
        String prefix = LAYER_PREFIX;
        Map<String,LayerType> map = new LinkedHashMap<String,LayerType>();
        for ( String pname : env.getNames() ) {
            if ( pname != null &&
                 pname.toLowerCase().startsWith( prefix.toLowerCase() ) ) {
                String suffix = pname.substring( prefix.length() );
                LayerType ltype = createLayerTypeParameter( suffix, context )
                                 .objectValue( env );
                map.put( suffix, ltype );
            }
        }
        return map;
    }

    /**
     * Returns a map of suffix strings to Plotter objects.
     * Each suffix string is appended to parameters associated with the
     * relevant plotter as a namespacing device on the command line.
     *
     * @param  env  execution environment
     * @param  context  plot context
     * @return  mapping from suffixes to plotters for the environment
     */
    private Map<String,Plotter> getPlotters( Environment env,
                                             PlotContext context )
            throws TaskException {
        Map<String,Plotter> plotterMap = new LinkedHashMap<String,Plotter>();
        for ( Map.Entry<String,LayerType> entry :
              getLayers( env, context ).entrySet() ) {
            String suffix = entry.getKey();
            LayerType ltype = entry.getValue();
            plotterMap.put( suffix, ltype.getPlotter( env, suffix ) );
        }
        return plotterMap;
    }

    /**
     * Acquires a captioner from the environment.
     * At present, a single captioner is used for labelling the legend
     * and any axes; this is also the one that will be used by default
     * for any label-type plot layers.  However you could refine it to
     * use different parameter sets for different purposes.
     *
     * @param   env  execution environment
     * @return   captioner
     */
    private Captioner getCaptioner( Environment env ) throws TaskException {
        KeySet<Captioner> capKeys = StyleKeys.CAPTIONER;
        ConfigMap capConfig = createBasicConfigMap( env, capKeys.getKeys() );
        return capKeys.createValue( capConfig );
    }

    /**
     * Acquires a ShadeAxisFactory from the environment.
     *
     * @param  env  execution environment
     * @param  layers  layers that will be plotted
     * @return   shade axis factory, may be null
     */
    private ShadeAxisFactory createShadeAxisFactory( Environment env,
                                                     PlotLayer[] layers )
            throws TaskException {

        /* Locate the first layer that references the aux colour scale. */
        AuxScale scale = AuxScale.COLOR;
        PlotLayer scaleLayer = getFirstAuxLayer( layers, scale );

        /* Work out whether to display the colour ramp at all. */
        Boolean auxvis = auxvisibleParam_.objectValue( env );
        boolean hasAux = auxvis == null ? scaleLayer != null
                                        : auxvis.booleanValue();
        if ( ! hasAux ) {
            return null;
        }

        /* Find a suitable label for the colour ramp. */
        if ( scaleLayer != null ) {
            auxlabelParam_.setStringDefault( getAuxLabel( scaleLayer, scale ) );
        }
        String label = auxlabelParam_.objectValue( env );

        /* Configure and return a shade axis accordingly. */
        RampKeySet rampKeys = StyleKeys.AUX_RAMP;
        Captioner captioner = getCaptioner( env );
        ConfigMap auxConfig = createBasicConfigMap( env, rampKeys.getKeys() );
        RampKeySet.Ramp ramp = rampKeys.createValue( auxConfig );
        double crowd = auxcrowdParam_.doubleValue( env );
        return RampKeySet
              .createShadeAxisFactory( ramp, captioner, label, crowd );
    }

    /**
     * Returns a config map based on given keys with values derived from
     * the execution environment.  There no funny business with appending
     * suffixes etc.
     *
     * @param  env  execution environment
     * @param  keys  config keys
     * @return  config map
     */
    private ConfigMap createBasicConfigMap( Environment env, ConfigKey[] keys )
            throws TaskException {
        ConfigParameterFactory cpFact = new ConfigParameterFactory() {
            public <T> ConfigParameter<T> getParameter( Environment env,
                                                        ConfigKey<T> key ) {
                return new ConfigParameter<T>( key );
            }
        };
        return createConfigMap( env, keys, cpFact );
    }

    /**
     * Creates a PlotLayer from the environment given a plotter, suffix
     * and geom.
     *
     * @param   env  execution environment containing parameter assignments
     * @param   suffix  parameter suffix for this layer
     * @param   plotter  plotter object for this layer
     * @param   geom   data position geometry
     * @return  plot layer
     */
    private <S extends Style>
            PlotLayer createPlotLayer( Environment env, String suffix,
                                       Plotter plotter, DataGeom geom )
            throws TaskException {

        /* Get basic and additional coordinate specifications. */
        CoordGroup cgrp = plotter.getCoordGroup();
        DataSpec dataSpec =
            createDataSpec( env, suffix, geom, cgrp.getPositionCount(),
                            cgrp.getExtraCoords() );

        /* Prepare a config map with entries for all the config keys
         * required by the plotter.  All the config keys reported
         * by the plotter are included.  We also add keys for the
         * aux axis, if used, since these are global.
         * This is a bit questionable, the plotter is using unreported
         * config options, but if it didn't, it would report per-layer
         * aux axis options (colour maps etc) which the plot surface
         * decorations can't reflect (there's only one aux colour ramp
         * displayed).  Maybe look at this again one day. */
        ConfigMap config =
            createSuffixedConfigMap( env, plotter.getStyleKeys(), suffix );
        config.putAll( createBasicConfigMap( env,
                                             StyleKeys.AUX_RAMP.getKeys() ) );

        /* Work out the requested style. */
        @SuppressWarnings("unchecked")
        Plotter<S> splotter = (Plotter<S>) plotter;
        S style;
        try {
            style = splotter.createStyle( config );
        }
        catch ( ConfigException e ) {
            throw new UsageException( e.getConfigKey().getMeta().getShortName()
                                    + ": " + e.getMessage(), e );
        }

        /* Return a layer based on these. */
        return splotter.createLayer( geom, dataSpec, style );
    }

    /**
     * Generates a DataSpec for a given list of Coords from the environment.
     *
     * @param  env  execution environment
     * @param  suffix   parameter suffix of interest
     * @param  geom  defines positional coordinate groups
     * @param  npos  number of positional coordinate groups
     * @param  extraCoords   non-positional coordinates
     * @return   data spec from environment
     */
    private DataSpec createDataSpec( Environment env, String suffix,
                                     DataGeom geom, int npos,
                                     Coord[] extraCoords )
            throws TaskException {
        if ( npos == 0 && extraCoords.length == 0 ) {
            return null;
        }
        StarTable table = getInputTable( env, suffix );
        List<CoordValue> cvlist = new ArrayList<CoordValue>();
        Coord[] posCoords = geom == null ? new Coord[ 0 ] : geom.getPosCoords();
        for ( int ipos = 0; ipos < npos; ipos++ ) {
            String posSuffix = npos > 1 ? PlotUtil.getIndexSuffix( ipos ) : "";
            for ( int ic = 0; ic < posCoords.length; ic++ ) {
                cvlist.add( getCoordValue( env, posCoords[ ic ],
                                           posSuffix + suffix ) );
            }
        }
        for ( int ic = 0; ic < extraCoords.length; ic++ ) {
            cvlist.add( getCoordValue( env, extraCoords[ ic ], suffix ) );
        }
        CoordValue[] coordVals = cvlist.toArray( new CoordValue[ 0 ] );
        return new JELDataSpec( table, null, coordVals );
    }

    /**
     * Turns a coord into a CoordValue.
     *
     * @param   env  execution environment
     * @param   coord  coordinate definition
     * @param   suffix  suffix to append to parameter name
     * @return   coordinate with expression values acquired from environment
     */
    private CoordValue getCoordValue( Environment env, Coord coord,
                                      String suffix ) throws TaskException {
        Input[] inputs = coord.getInputs();
        int ni = inputs.length;
        String[] exprs = new String[ ni ];
        for ( int ii = 0; ii < ni; ii++ ) {
            final Input input = inputs[ ii ];
            Parameter param = new ParameterFinder<Parameter>() {
                public Parameter createParameter( String sfix ) {
                    return createDataParameter( input, sfix, true );
                }
            }.getParameter( env, suffix );
            param.setNullPermitted( ! coord.isRequired() );
            exprs[ ii ] = param.stringValue( env );
        }
        return new CoordValue( coord, exprs );
    }

    /**
     * Returns a ConfigMap derived from the assignments made in a given
     * execution environment.
     * Parameter names specified in the environment
     * are the config names plus a supplied suffix.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  configKeys  configuration keys to find values for
     * @param  suffix  trailer applied to config key shortnames to make
     *                 env parameter names
     * @return  config map with values for the supplied keys
     */
    private ConfigMap createSuffixedConfigMap( Environment env,
                                               ConfigKey[] configKeys,
                                               final String suffix )
            throws TaskException {
        ConfigParameterFactory cpFact = new ConfigParameterFactory() {
            public <T> ConfigParameter<T>
                    getParameter( Environment env, final ConfigKey<T> key ) {
                return new ParameterFinder<ConfigParameter<T>>() {
                    public ConfigParameter<T> createParameter( String sfix ) {
                        return ConfigParameter
                              .createSuffixedParameter( key, sfix, true );
                    }
                }.getParameter( env, suffix );
            }
        };
        return createConfigMap( env, configKeys, cpFact );
    }

    /**
     * Returns a ConfigMap derived from the assignments made in a given
     * execution environment.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  configKeys  configuration keys to find values for
     * @param  cpFact  turns config keys into Parameters
     * @return  config map with values for the supplied keys
     */
    private ConfigMap createConfigMap( Environment env, ConfigKey[] configKeys,
                                       ConfigParameterFactory cpFact )
            throws TaskException {
        Level level = Level.CONFIG;
        ConfigMap config = new ConfigMap();
        if ( Logger.getLogger( getClass().getName() ).isLoggable( level ) ) {
            config = new LoggingConfigMap( config, level );
        }
        for ( ConfigKey<?> key : configKeys ) {
            putConfigValue( env, key, cpFact, config );
        }
        return config;
    }

    /**
     * Extracts a parameter corresponding to a given config key from a
     * given execution environment, and puts the result into a config map.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  key   key to find value for
     * @param  cpFact  turns config keys into Parameters
     * @param  map   map into which key/value pair will be written
     */
    private <T> void putConfigValue( Environment env, ConfigKey<T> key,
                                     ConfigParameterFactory cpFact,
                                     ConfigMap map )
            throws TaskException {
        ConfigParameter<T> param = cpFact.getParameter( env, key );
        T value = param.objectValue( env );
        if ( key.getValueClass().equals( Double.class ) && value == null ) {
            value = key.cast( Double.NaN );
        }
        map.put( key, value );
    }

    /**
     * Returns a table from the environment.
     *
     * @param   env  execution environment
     * @param   suffix   parameter suffix
     * @return   table
     */
    private StarTable getInputTable( Environment env, String suffix )
            throws TaskException {
        FilterParameter filterParam = new ParameterFinder<FilterParameter>() {
            public FilterParameter createParameter( String sfix ) {
                return createFilterParameter( sfix, null );
            }
        }.getParameter( env, suffix );
        InputTableParameter tableParam =
                new ParameterFinder<InputTableParameter>() {
            public InputTableParameter createParameter( String sfix ) {
                return createTableParameter( sfix );
            }
        }.getParameter( env, suffix );

        /* Note that tables produced by this call which have the same
         * input specifications (text of input and filter parameters)
         * will be equal in the sense of Object.equals().  That's good,
         * since it means that in the common case where the same table
         * contributes to multiple layers, the DataStore only has to
         * scan the table once. */
        TableProducer producer =
            ConsumerTask.createProducer( env, filterParam, tableParam );
        try {
            return producer.getTable();
        }
        catch ( IOException e ) {
            throw new ExecutionException( "Table processing error", e );
        }
    }

    /**
     * Returns a parameter for acquiring a data table.
     *
     * @param  suffix  layer-specific suffix
     * @return   table parameter
     */
    public static InputTableParameter createTableParameter( String suffix ) {
        return new InputTableParameter( TABLE_PREFIX + suffix );
    }

    /**
     * Returns a parameter for acquiring a filter applied to the table input
     * for a given layer.
     *
     * @param  suffix  layer-specific suffix
     * @param  tableParam input table parameter associated with the layer
     * @return   filter parameter
     */
    public static FilterParameter
            createFilterParameter( String suffix,
                                   InputTableParameter tableParam ) {
        FilterParameter param = new FilterParameter( FILTER_PREFIX + suffix );
        param.setTableDescription( "the layer " + suffix + " input table",
                                   tableParam, null );
        return param;
    }

    /**
     * Returns a parameter to get a textual label corresponding to the layer
     * identified by a given layer suffix.  This label is displayed in
     * the legend.
     *
     * @param  suffix  layer suffix
     * @return  parameter to get legend label for layer
     */
    public static Parameter<String> createLabelParameter( String suffix ) {
        StringParameter param = new StringParameter( "leglabel" + suffix );
        param.setUsage( "<text>" );
        param.setPrompt( "Legend label for layer " + suffix );
        param.setDescription( new String[] { 
            "<p>Sets the presentation label for the layer with a given suffix.",
            "This is the text which is displayed in the legend, if present.",
            "Multiple layers may use the same label, in which case",
            "they will be combined to form a single legend entry.",
            "</p>",
            "<p>If no value is supplied (the default),",
            "the suffix itself is used as the label.",
            "</p>",
        } );
        param.setNullPermitted( true );
        return param;
    }

    /**
     * Returns a parameter for acquiring a plotter.
     *
     * @param   suffix  parameter name suffix
     * @param   context  plot context
     * @return   plotter parameter
     */
    public static LayerTypeParameter
            createLayerTypeParameter( String suffix, PlotContext context ) {
        return new LayerTypeParameter( LAYER_PREFIX, suffix, context );
    }

    /**
     * Returns a parameter for acquiring a column of data.
     *
     * @param  input  specifies input value required from user
     * @param  suffix  layer-specific suffix
     * @param  fullDetail  if true, extra detail is appended to the description
     * @return   data parameter
     */
    public static StringParameter createDataParameter( Input input,
                                                       String suffix,
                                                       boolean fullDetail ) {
        InputMeta meta = input.getMeta();
        boolean hasSuffix = suffix.length() > 0;
        String cName = meta.getShortName();
        Class cClazz = input.getValueClass();
        final String typeTxt;
        final String typeUsage;
        if ( cClazz.equals( String.class ) ) {
            typeTxt = "string";
            typeUsage = "txt";
        }
        else if ( cClazz.equals( Integer.class ) ||
                  cClazz.equals( Long.class ) ) {
            typeTxt = "integer";
            typeUsage = "int";
        }
        else if ( Number.class.isAssignableFrom( cClazz ) ) {
            typeTxt = "numeric";
            typeUsage = "num";
        }
        else {
            typeTxt = "<code>" + cClazz.getSimpleName() + "</code>";
            typeUsage = null;
        }
        StringParameter param = new StringParameter( cName + suffix );
        String prompt = meta.getShortDescription();
        if ( fullDetail ) {
            prompt += hasSuffix ? ( " for layer " + suffix )
                                : " for plot layers";
        }
        param.setPrompt( prompt );
        StringBuffer dbuf = new StringBuffer()
            .append( meta.getXmlDescription() );
        dbuf.append( "<p>" );
        if ( fullDetail ) {
            dbuf.append( "This parameter gives a column name, " )
                .append( "fixed value, or algebraic expression for the\n" )
                .append( "<code>" )
                .append( cName )
                .append( "</code> coordinate\n" );
            if ( hasSuffix ) {
                dbuf.append( "for layer <code>" )
                    .append( suffix )
                    .append( "</code>" );
            }
            else {
                dbuf.append( "for all plot layers" );
            }
            dbuf.append( ".\n" );
        }
        dbuf.append( "The value is a " )
            .append( typeTxt )
            .append( " algebraic expression based on column names\n" )
            .append( "as described in <ref id='jel'/>.\n" )
            .append( "</p>\n" );
        param.setDescription( dbuf.toString() );
        String vUsage = meta.getValueUsage();
        if ( vUsage == null ) {
            vUsage = typeUsage;
        }
        param.setUsage( vUsage == null ? "<expr>"
                                       : "<" + vUsage + "-expr>" );
        return param;
    }

    /**
     * Returns a parameter for specifying a paint mode.
     *
     * @return   paint mode parameter
     */
    private static PaintModeParameter createPaintModeParameter() {
        return new PaintModeParameter( "omode", EXPORTERS );
    }

    /**
     * Creates an icon which will paint the content of a plot.
     * This icon is expected to be painted once and then discarded,
     * so it's not cached.
     *
     * @param  layers   layers constituting plot content
     * @param  surfFact   surface factory
     * @param  config   map containing surface profile and initial aspect
     *                  configuration
     * @param  legend   legend icon, or null if none required
     * @param  legPos   2-element array giving x,y fractional legend placement
     *                  position within plot (elements in range 0..1),
     *                  or null for external legend
     * @param  title    plot title, or null
     * @param  shadeFact  gets shader axis from range, or null if not required
     * @param  shadeFixRange  fixed shader range,
     *                        or null for auto-range where required
     * @param  ptsel   paper type selector
     * @param  compositor   compositor for pixel composition
     * @param  dataStore   data storage object
     * @param  xpix    horizontal size of icon in pixels
     * @param  ypix    vertical size of icon in pixels
     * @param  insets  may supply the inset space to be used for
     *                 axis decoration etc; if null, this will be worked out
     *                 automatically
     * @param  forceBitmap   true to force bitmap output of vector graphics,
     *                       false to use default behaviour
     * @return  icon  icon for plotting
     */
    private static <P,A> Icon createPlotIcon( PlotLayer[] layers,
                                              SurfaceFactory<P,A> surfFact,
                                              ConfigMap config, Icon legend,
                                              float[] legPos, String title,
                                              ShadeAxisFactory shadeFact,
                                              Range shadeFixRange,
                                              PaperTypeSelector ptsel,
                                              Compositor compositor,
                                              DataStore dataStore,
                                              int xpix, int ypix, Insets insets,
                                              boolean forceBitmap ) {
        P profile = surfFact.createProfile( config );
        long t0 = System.currentTimeMillis();
        Range[] ranges = surfFact.useRanges( profile, config )
                       ? surfFact.readRanges( profile, layers, dataStore )
                       : null;
        PlotUtil.logTime( logger_, "Range", t0 );
        A aspect = surfFact.createAspect( profile, config, ranges );
        return createPlotIcon( layers, surfFact, profile, aspect,
                               legend, legPos, title, shadeFact, shadeFixRange,
                               ptsel, compositor, dataStore, xpix, ypix,
                               insets, forceBitmap );
    }

    /**
     * Creates an icon which will paint the content of a plot.
     * This icon is expected to be painted once and then discarded,
     * so it's not cached.
     *
     * @param  layers   layers constituting plot content
     * @param  surfFact   surface factory
     * @param  profile   surface profile
     * @param  aspect    surface aspect
     * @param  legend   legend icon, or null if none required
     * @param  legPos   2-element array giving x,y fractional legend placement
     *                  position within plot (elements in range 0..1),
     *                  or null for external legend
     * @param  title   plot title or null
     * @param  shadeFact  gets shader axis from range, or null if not required
     * @param  shadeFixRange  fixed shader range,
     *                        or null for auto-range where required
     * @param  ptsel   paper type selector
     * @param  compositor   compositor for pixel composition
     * @param  dataStore   data storage object
     * @param  xpix    horizontal size of icon in pixels
     * @param  ypix    vertical size of icon in pixels
     * @param  insets  may supply the inset space to be used for
     *                 axis decoration etc; if null, this will be worked out
     *                 automatically
     * @param  forceBitmap   true to force bitmap output of vector graphics,
     *                       false to use default behaviour
     * @return  icon  icon for plotting
     */
    public static <P,A> Icon createPlotIcon( PlotLayer[] layers,
                                             SurfaceFactory<P,A> surfFact,
                                             P profile, A aspect, Icon legend,
                                             float[] legPos, String title,
                                             ShadeAxisFactory shadeFact,
                                             Range shadeFixRange,
                                             PaperTypeSelector ptsel,
                                             Compositor compositor,
                                             DataStore dataStore,
                                             int xpix, int ypix, Insets insets,
                                             boolean forceBitmap ) {
        Rectangle extBounds = new Rectangle( 0, 0, xpix, ypix );
        Rectangle dataBounds = insets != null        
                             ? PlotUtil.subtractInsets( extBounds, insets )
                             : null;
        Rectangle approxBounds = dataBounds != null ? dataBounds : extBounds;
        Surface approxSurf =
            surfFact.createSurface( approxBounds, profile, aspect );
        Map<AuxScale,Range> auxRanges =
            PlotDisplay.getAuxRanges( layers, approxSurf, shadeFixRange,
                                      shadeFact, dataStore );
        Range shadeRange = auxRanges.get( AuxScale.COLOR );
        ShadeAxis shadeAxis = shadeFact != null && shadeRange != null
                            ? shadeFact.createShadeAxis( shadeRange )
                            : null;
        if ( dataBounds == null ) {
            boolean withScroll = false;
            dataBounds = PlotPlacement
                        .calculateDataBounds( extBounds, surfFact, profile,
                                              aspect, withScroll, legend,
                                              legPos, title, shadeAxis );
        }
        Surface surf = surfFact.createSurface( dataBounds, profile, aspect );
        Decoration[] decs =
            PlotPlacement.createPlotDecorations( surf, legend, legPos,
                                                 title, shadeAxis );
        PlotPlacement placer = new PlotPlacement( extBounds, surf, decs );
        LayerOpt[] opts = PaperTypeSelector.getOpts( layers );
        PaperType paperType = forceBitmap
                            ? ptsel.getPixelPaperType( opts, compositor, null )
                            : ptsel.getVectorPaperType( opts );
        boolean cached = false;
        return PlotDisplay.createIcon( placer, layers, auxRanges, dataStore,
                                       paperType, cached );
    }

    /**
     * Returns a list of non-suffixed parameters based on a list of
     * ConfigKeys.
     *
     * @param  keys  config keys
     * @return  parameters for acquiring config key values
     */
    public static List<Parameter> getKeyParams( ConfigKey[] keys ) {
        List<Parameter> plist = new ArrayList<Parameter>();
        for ( int ik = 0; ik < keys.length; ik++ ) {
            plist.add( new ConfigParameter( keys[ ik ] ) );
        }
        return plist;
    }

    /**
     * Identifies and returns the first layer in a given list that
     * appears to make use of a given AuxScale.
     * The test is whether it does ranging for the scale.
     *
     * @param   layers  list of known layers
     * @param   scale   target scale
     * @return  one of the layers in the given list that appears to use
     *          <code>scale</code>, or null if none do
     */
    private static PlotLayer getFirstAuxLayer( PlotLayer[] layers,
                                               AuxScale scale ) {
        for ( PlotLayer layer : layers ) {
            if ( layer.getAuxRangers().containsKey( scale ) ) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Tries to return the name of an input data quantity used in plotting
     * a given layer that corresponds to a given AuxScale.
     *
     * @param   layer   plot layer
     * @param   scale   scale
     * @return  user-readable name for data corresponding to
     *          <code>scale</code> in <code>layer</code>,
     *          or null if there's no obvious answer
     */
    private static String getAuxLabel( PlotLayer layer, AuxScale scale ) {
        AuxReader rdr = layer.getAuxRangers().get( scale );
        if ( rdr != null ) {
            int icAux = rdr.getCoordIndex();
            if ( icAux >= 0 ) {
                DataSpec dataSpec = layer.getDataSpec();
                assert dataSpec == null || dataSpec instanceof JELDataSpec;
                if ( dataSpec instanceof JELDataSpec ) {
                    String[] exprs =
                        ((JELDataSpec) dataSpec).getCoordExpressions( icAux );
                    if ( exprs.length == 1 ) {
                        return exprs[ 0 ];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Object that can turn a ConfigKey into a Parameter.
     */
    private interface ConfigParameterFactory {

        /**
         * Produces a parameter to find the value for a given config key.
         *
         * @param   key  config key
         * @param   env  execution environment
         * @return   parameter that can get a value for <code>key</code>
         */
        <T> ConfigParameter<T> getParameter( Environment env, ConfigKey<T> key )
                 throws TaskException;
    }

    /**
     * StarTable wrapper class which caches the first row of the table.
     * It uses a row sequence to do this, which it saves for later use.
     * This means that it doesn't need to use a separate row sequence
     * for the initial row, but it's only a good idea if you know you're
     * going to be calling getRowSequence at least once.
     */
    private static class Row0Table extends WrapperStarTable {
        final Object[] row0_;
        RowSequence rseq_;

        /**
         * Constructor.
         *
         * @param   base   table to which most calls are delegated
         */
        Row0Table( StarTable base ) throws IOException {
            super( base );
            rseq_ = base.getRowSequence();
            rseq_.next();
            row0_ = rseq_.getRow();
        }

        /**
         * Returns the first row.
         *
         * @return  row
         */
        public Object[] getRow0() {
            return row0_;
        }

        @Override
        public synchronized RowSequence getRowSequence() throws IOException {
            if ( rseq_ == null ) {
                return super.getRowSequence();
            }
            else {
                RowSequence baseSeq = rseq_;
                rseq_ = null;
                return new WrapperRowSequence( baseSeq ) {
                    long irow = -1;
                    @Override
                    public boolean next() throws IOException {
                        return ++irow == 0 || super.next();
                    }
                    @Override
                    public Object getCell( int icol ) throws IOException {
                        return irow == 0 ? row0_[ icol ]
                                         : super.getCell( icol );
                    }
                    @Override
                    public Object[] getRow() throws IOException {
                        return irow == 0 ? row0_ : super.getRow();
                    }
                };
            }
        }
    }

    /**
     * Object capable of executing a static or interactive plot.
     * All configuration options are contained.
     */
    private interface PlotExecutor {

        /**
         * Creates a data store suitable for use with this object.
         *
         * @param     prevStore  previously obtained data store, may be null
         * @return    object containing plot data
         */
        DataStore createDataStore( DataStore prevStore )
                throws IOException, InterruptedException;

        /**
         * Generates an interactive plot component.
         *
         * @param  dataStore  object containing plot data
         * @param  navigable  if true, standard pan/zoom mouse listeners
         *                   will be installed
         * @param  caching   if true, plot image will be cached where
         *                   applicable, if false it will be regenerated
         *                   from the data on every repaint
         */
        PlotDisplay createPlotComponent( DataStore dataStore,
                                         boolean navigable,
                                         boolean caching );

        /**
         * Generates an icon which will draw the plot.
         * This may be slow to paint.
         *
         * @param  dataStore  object containing plot data
         */
        Icon createPlotIcon( DataStore dataStore );
    }
}
