package uk.ac.starlink.ttools.plot2.task;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Picture;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.LoggingConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.SpherePlotType;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.TimePlotType;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;
import uk.ac.starlink.ttools.plottask.ColorParameter;
import uk.ac.starlink.ttools.plottask.PaintModeParameter;
import uk.ac.starlink.ttools.plottask.Painter;
import uk.ac.starlink.ttools.plottask.SwingPainter;
import uk.ac.starlink.ttools.task.ConsumerTask;
import uk.ac.starlink.ttools.task.DefaultMultiParameter;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.TableProducer;

/**
 * STILTS Task for generic layer plots.
 *
 * @author   Mark Taylor
 * @since    1 Mar 2013
 */
public class Plot2Task implements Task {

    private final ChoiceParameter<PlotType> typeParam_;
    private final ChoiceParameter<DataGeom> geomParam_;
    private final IntegerParameter xpixParam_;
    private final IntegerParameter ypixParam_;
    private final InsetsParameter insetsParam_;
    private final PaintModeParameter painterParam_;
    private final DataStoreParameter dstoreParam_;
    private final DefaultMultiParameter orderParam_;
    private final BooleanParameter bitmapParam_;

    private static final String PLOTTER_PREFIX = "layer";
    private static final String TABLE_PREFIX = "in";
    private static final String FILTER_PREFIX = "cmd";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.task" );

    /**
     * Constructor.
     */
    public Plot2Task() {
        typeParam_ = new ChoiceParameter<PlotType>( "type", new PlotType[] {
            PlanePlotType.getInstance(),
            SkyPlotType.getInstance(),
            CubePlotType.getInstance(),
            SpherePlotType.getInstance(),
            TimePlotType.getInstance(),
        } );
        geomParam_ = new ChoiceParameter<DataGeom>( "geom" );
        xpixParam_ = new IntegerParameter( "xpix" );
        xpixParam_.setDefault( "500" );
        ypixParam_ = new IntegerParameter( "ypix" );
        ypixParam_.setDefault( "400" );
        insetsParam_ = new InsetsParameter( "insets" );
        GraphicExporter[] exporters =
            GraphicExporter.getKnownExporters( PlotUtil.LATEX_PDF_EXPORTER );
        painterParam_ = new PaintModeParameter( "omode", exporters );
        dstoreParam_ = new DataStoreParameter( "storage" );
        orderParam_ = new DefaultMultiParameter( "order", ',' );
        orderParam_.setNullPermitted( true );
        bitmapParam_ = new BooleanParameter( "forcebitmap" );
        bitmapParam_.setDefault( Boolean.FALSE.toString() );
    }

    public String getPurpose() {
        return "Draws a generic plot";
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            typeParam_,
            geomParam_,
            xpixParam_,
            ypixParam_,
            insetsParam_,
            painterParam_,
            dstoreParam_,
            orderParam_,
        };
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        final Painter painter = painterParam_.painterValue( env );
        final boolean isSwing = painter instanceof SwingPainter;
        dstoreParam_.setDefaultCaching( isSwing );
        final PlotExecutor executor = createPlotExecutor( env );
        return new Executable() {
            public void execute() throws IOException {
                DataStore dataStore;
                try {
                    dataStore = executor.createDataStore();
                }
                catch ( InterruptedException e ) {
                    Thread.currentThread().isInterrupted();
                    return;
                }

                /* For an active display, create and post a component which
                 * will draw the requested plot on demand, including resizing
                 * when appropriate. */
                if ( isSwing ) {
                    JComponent panel =
                        executor.createPlotComponent( dataStore, true, true );
                    ((SwingPainter) painter).postComponent( panel );
                }

                /* For a static plot, generate and plot the fixed icon here. */
                else {
                    Icon plot = executor.createPlotIcon( dataStore );
                    long start = System.currentTimeMillis();
                    painter.paintPicture( PlotUtil.toPicture( plot ) );
                    PlotUtil.logTime( logger_, "Plot", start );
                }
            }
        };
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
        PlotExecutor executor = createPlotExecutor( env );
        return executor.createPlotIcon( executor.createDataStore() );
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
    public JComponent createPlotComponent( Environment env, boolean caching )
            throws TaskException, IOException, InterruptedException {
        dstoreParam_.setDefaultCaching( caching );
        PlotExecutor executor = createPlotExecutor( env );
        return executor.createPlotComponent( executor.createDataStore(),
                                             true, caching );
    }

    /**
     * Turns an execution environment (containing value-bearing parameters)
     * into an object capable of performing a static or interactive plot.
     *
     * @param  env  execution environment
     * @return   plot executor
     */
    private PlotExecutor createPlotExecutor( Environment env )
            throws TaskException {

        /* What kind of plot? */
        final PlotType plotType = typeParam_.objectValue( env );

        /* Set up generic configuration. */
        final int xpix = xpixParam_.intValue( env );
        final int ypix = ypixParam_.intValue( env );
        final Insets insets = insetsParam_.insetsValue( env );
        final boolean forceBitmap = bitmapParam_.booleanValue( env );
        final boolean surfaceAuxRange = false;
        final DataStoreFactory storeFact = dstoreParam_.objectValue( env );

        /* Leave out some optional extras for now. */
        final Icon legend = null;
        final float[] legpos = null;
        final ShadeAxis shadeAxis = null;

        /* Get surface factory and configuration. */
        final SurfaceFactory<?,?> surfFact = plotType.getSurfaceFactory();
        List<ConfigKey> keyList = new ArrayList<ConfigKey>();
        keyList.addAll( Arrays.asList( surfFact.getProfileKeys() ) );
        keyList.addAll( Arrays.asList( surfFact.getAspectKeys() ) );
        keyList.addAll( Arrays.asList( surfFact.getNavigatorKeys() ) );
        keyList.add( StyleKeys.SHADE_LOW );
        keyList.add( StyleKeys.SHADE_HIGH );
        ConfigKey[] surfKeys = keyList.toArray( new ConfigKey[ 0 ] );
        final ConfigMap surfConfig = createConfigMap( env, "", surfKeys );
        final Range shadeFixRange =
            new Range( surfConfig.get( StyleKeys.SHADE_LOW ),
                       surfConfig.get( StyleKeys.SHADE_HIGH ) );

        /* Gather the requested plot layers from the environment. */
        final PlotLayer[] layers = createLayers( env, plotType );
        int nl = layers.length;
        final DataSpec[] dataSpecs = new DataSpec[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            dataSpecs[ il ] = layers[ il ].getDataSpec();
        }
        return new PlotExecutor() {

            public DataStore createDataStore()
                    throws IOException, InterruptedException {
                long t0 = System.currentTimeMillis();
                DataStore store = storeFact.readDataStore( dataSpecs, null );
                PlotUtil.logTime( logger_, "Data", t0 );
                return store;
            }

            public JComponent createPlotComponent( DataStore dataStore,
                                                   boolean navigable,
                                                   boolean caching ) {
                PlotDisplay panel =
                    PlotDisplay
                   .createPlotDisplay( plotType, layers, surfFact,
                                       surfConfig, legend, legpos,
                                       shadeAxis, shadeFixRange,
                                       dataStore, surfaceAuxRange,
                                       navigable, caching );
                panel.setPreferredSize( new Dimension( xpix, ypix ) );
                return panel;
            }

            public Icon createPlotIcon( DataStore dataStore ) {
                PaperTypeSelector ptsel = plotType.getPaperTypeSelector();
                return Plot2Task
                      .createPlotIcon( layers, surfFact, surfConfig,
                                       legend, legpos,
                                       shadeAxis, shadeFixRange, dataStore,
                                       xpix, ypix, insets,
                                       ptsel, forceBitmap );
            }
        };
    }

    /**
     * Obtains a list of the PlotLayers specified by parameters in
     * the execution environment for a given PlotType.
     *
     * @param   env  execution environment
     * @param   plotType  plot type
     */
    private PlotLayer[] createLayers( Environment env, PlotType plotType )
            throws TaskException {

        /* Determine the data position coordinate geometry. */
        DataGeom[] geoms = plotType.getPointDataGeoms();
        for ( int ig = 0; ig < geoms.length; ig++ ) {
            geomParam_.addOption( geoms[ ig ], geoms[ ig ].getVariantName() );
        }
        geomParam_.setDefaultOption( geoms[ 0 ] );
        DataGeom geom = geomParam_.objectValue( env );

        /* Work out what plotters/layers are requested. */
        Map<String,Plotter> plotterMap = getPlotters( env, plotType );

        /* For each plotter, create a PlotLayer based on it using the
         * appropriately suffix-coded parameters in the environment. 
         * In this step we deliberately create all the specified layers
         * and possibly discard some rather than only creating the
         * required layers.  This is to make sure that the parameter
         * system does not report specified but unplotted layer parameters
         * as unused.  Creating layer objects is in any case cheap. */
        Map<String,PlotLayer> layerMap = new LinkedHashMap<String,PlotLayer>();
        for ( Map.Entry<String,Plotter> entry : plotterMap.entrySet() ) {
            String suffix = entry.getKey();
            Plotter plotter = entry.getValue();
            PlotLayer layer = createPlotLayer( env, suffix, plotter, geom );
            layerMap.put( suffix, layer );
        }

        /* Get the order of layers to plot as implicitly or
         * explicitly supplied. */
        String[] suffixOrder = orderParam_.stringsValue( env );
        final Collection<PlotLayer> layers;
        if ( suffixOrder.length == 0 ) {
            layers = layerMap.values();
        }
        else {
            layers = new ArrayList<PlotLayer>();
            for ( int il = 0; il < suffixOrder.length; il++ ) {
                PlotLayer layer = layerMap.get( suffixOrder[ il ] );
                if ( layer == null ) {
                    String msg = new StringBuffer()
                        .append( "No specification for layer \"" )
                        .append( suffixOrder[ il ] )
                        .append( "\"" )
                        .append( "; known layers: " )
                        .append( layerMap.keySet() )
                        .toString();
                    throw new ParameterValueException( orderParam_, msg );
                }
                else {
                    layers.add( layer );
                }
            }
        }

        /* Return the plot layers in the chosen order. */
        return layers.toArray( new PlotLayer[ 0 ] );
    }

    /**
     * Returns a map of suffix strings to Plotter objects.
     * Each suffix string is appended to parameters associated with the
     * relevant plotter as a namespacing device on the command line.
     *
     * @param  env  execution environment
     * @param  plotType  plot type
     * @return  mapping from suffixes to plotters for the environment
     */
    private Map<String,Plotter> getPlotters( Environment env,
                                             PlotType plotType )
            throws TaskException {
        String prefix = PLOTTER_PREFIX;
        Map<String,Plotter> map = new LinkedHashMap<String,Plotter>();
        String[] pnames = env.getNames();
        for ( int in = 0; in < pnames.length; in++ ) {
            String name = pnames[ in ];
            if ( name.toLowerCase().startsWith( prefix.toLowerCase() ) ) {
                String suffix = name.substring( prefix.length() );
                Plotter plotter =
                    createPlotterParameter( prefix + suffix, plotType )
                   .objectValue( env );
                map.put( suffix, plotter );
            }
        }
        return map;
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
        List<Coord> coordList = new ArrayList<Coord>();
        if ( plotter.hasPosition() ) {
            coordList.addAll( Arrays.asList( geom.getPosCoords() ) );
        }
        coordList.addAll( Arrays.asList( plotter.getExtraCoords() ) );
        DataSpec dataSpec =
            createDataSpec( env, suffix, coordList.toArray( new Coord[ 0 ] ) );

        /* Work out the requested Style. */
        ConfigMap config =
            createConfigMap( env, suffix, plotter.getStyleKeys() );
        @SuppressWarnings("unchecked")
        Plotter<S> splotter = (Plotter<S>) plotter;
        S style = splotter.createStyle( config );

        /* Return a layer based on these. */
        return splotter.createLayer( geom, dataSpec, style );
    }

    /**
     * Generates a DataSpec for a given list of Coords from the environment.
     *
     * @param  env  execution environment
     * @param  suffix   parameter suffix of interest
     * @param  coords  coordinates for which values are required
     * @return   data spec from environment
     */
    private DataSpec createDataSpec( Environment env, String suffix,
                                     Coord[] coords )
            throws TaskException {
        if ( coords.length == 0 ) {
            return null;
        }
        StarTable table = getInputTable( env, suffix );
        String[][] exprs = new String[ coords.length ][];
        for ( int ic = 0; ic < coords.length; ic++ ) {
            Coord coord = coords[ ic ];
            ValueInfo[] infos = coord.getUserInfos();
            exprs[ ic ] = new String[ infos.length ];
            for ( int iuc = 0; iuc < infos.length; iuc++ ) {
                Parameter param = createDataParameter( infos[ iuc ], suffix );
                param.setNullPermitted( ! coord.isRequired() );
                exprs[ ic ][ iuc ] = param.stringValue( env );
            }
        }
        return new JELDataSpec( table, null, coords, exprs );
    }

    /**
     * Returns a ConfigMap derived from the assignments made in a given
     * execution environment.
     * Parameter names specified in the environment
     * are the config names plus a supplied suffix.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  suffix  trailer applied to config key shortnames to make
     *                 env parameter names
     * @param  configKeys  configuration keys to find values for
     * @return  config map with values for the supplied keys
     */
    private ConfigMap createConfigMap( Environment env, String suffix,
                                       ConfigKey[] configKeys )
            throws TaskException {
        Level level = Level.INFO;
        ConfigMap config = new ConfigMap();
        if ( Logger.getLogger( getClass().getName() ).isLoggable( level ) ) {
            config = new LoggingConfigMap( config, level );
        }
        for ( int ic = 0; ic < configKeys.length; ic++ ) {
            ConfigKey<?> key = configKeys[ ic ];
            putConfigValue( env, suffix, key, config );
        }
        return config;
    }

    /**
     * Extracts a parameter corresponding to a given config key from a
     * given execution environment, and puts the result into a config map.
     * Parameter names specified in the environment
     * are the config names plus a supplied suffix.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  suffix  trailer applied to config key shortnames to make
     *                 env parameter names
     * @param  key   key to find value for
     * @param  map   map into which key/value pair will be written
     */
    private <T> void putConfigValue( Environment env, String suffix,
                                     ConfigKey<T> key, ConfigMap map )
            throws TaskException {
        String pname = key.getMeta().getShortName() + suffix;
        ConfigParameter<T> param = new ConfigParameter<T>( pname, key );
        T value = param.configValue( env );
        map.put( key, value );
    }

    /**
     * Returns a table from the environment.
     *
     * <p>This should be improved in a couple of respects.
     * <ol>
     * <li>It should be possible to use the same table for several or all
     *     layers by using the name without the suffix
     * <li>The returned table should be identifiably equal to others
     *     specified with the same text so that the data system does not
     *     read the same data multiple times for different layers.
     * </ol>
     *
     * @param   env  execution environment
     * @param   suffix   parameter suffix
     * @return   table
     */
    private StarTable getInputTable( Environment env, String suffix )
            throws TaskException {
        TableProducer producer =
            ConsumerTask.createProducer( env, createFilterParameter( suffix ),
                                              createTableParameter( suffix ) );
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
    private InputTableParameter createTableParameter( String suffix ) {
        return new InputTableParameter( TABLE_PREFIX + suffix );
    }

    /**
     * Returns a parameter for acquiring a data filter.
     *
     * @param  suffix  layer-specific suffix
     * @return   fileter parameter
     */
    private FilterParameter createFilterParameter( String suffix ) {
        return new FilterParameter( FILTER_PREFIX + suffix );
    }

    /**
     * Returns a parameter for acquiring a plotter.
     *
     * @param   pname  parameter name
     * @param   plotType  plot type
     * @return   plotter parameter
     */
    private ChoiceParameter<Plotter>
            createPlotterParameter( String pname, PlotType plotType ) {
        return new ChoiceParameter<Plotter>( pname, Plotter.class,
                                             plotType.getPlotters() ) {
            @Override
            public String getName( Plotter option ) {
                return option.getPlotterName();
            }
        };
    }

    /**
     * Returns a parameter for acquiring a column of data.
     *
     * @param  info  metadata for column
     * @param  suffix  layer-specific suffix
     * @return   data parameter
     */
    private Parameter createDataParameter( ValueInfo info, String suffix ) {
        return new Parameter( info.getName().toLowerCase() + suffix );
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
     * @param  shadeAxis  shader axis, or null if not required
     * @param  shadeFixRange  fixed shader range,
     *                        or null for auto-range where required
     * @param  dataStore   data storage object
     * @param  xpix    horizontal size of icon in pixels
     * @param  ypix    vertical size of icon in pixels
     * @param  insets  may supply the inset space to be used for
     *                 axis decoration etc; if null, this will be worked out
     *                 automatically
     * @param  ptsel   paper type selector
     * @param  forceBitmap   true to force bitmap output of vector graphics,
     *                       false to use default behaviour
     * @return  icon  icon for plotting
     */
    public static <P,A> Icon createPlotIcon( PlotLayer[] layers,
                                             SurfaceFactory<P,A> surfFact,
                                             ConfigMap config,
                                             Icon legend, float[] legPos,
                                             ShadeAxis shadeAxis,
                                             Range shadeFixRange,
                                             DataStore dataStore,
                                             int xpix, int ypix, Insets insets,
                                             PaperTypeSelector ptsel,
                                             boolean forceBitmap ) {
        P profile = surfFact.createProfile( config );
        long t0 = System.currentTimeMillis();
        Range[] ranges = surfFact.useRanges( profile, config )
                       ? surfFact.readRanges( layers, dataStore )
                       : null;
        PlotUtil.logTime( logger_, "Range", t0 );
        A aspect = surfFact.createAspect( profile, config, ranges );

        Rectangle extBounds = new Rectangle( 0, 0, xpix, ypix );
        final Rectangle dataBounds;
        if ( insets != null ) {
            dataBounds =
                new Rectangle( extBounds.x + insets.left,
                               extBounds.y + insets.top,
                               extBounds.width - insets.left - insets.right,
                               extBounds.height - insets.top - insets.bottom );
        }
        else {
            dataBounds = PlotPlacement
                        .calculateDataBounds( extBounds, surfFact, profile,
                                              aspect, false, legend, legPos,
                                              shadeAxis );
            dataBounds.x += 2;
            dataBounds.y += 2;
            dataBounds.width -= 4;
            dataBounds.height -= 4;
            int top = dataBounds.y - extBounds.y;
            int left = dataBounds.x - extBounds.x;
            int bottom = extBounds.height - dataBounds.height - top;
            int right = extBounds.width - dataBounds.width - left;
            logger_.info( "Calculate plot insets: "
                        + new Insets( top, left, bottom, right ) );
        }
        Surface surf = surfFact.createSurface( dataBounds, profile, aspect );
        Decoration[] decs =
            PlotPlacement.createPlotDecorations( dataBounds, legend, legPos,
                                                 shadeAxis );
        PlotPlacement placer = new PlotPlacement( extBounds, surf, decs );
        Map<AuxScale,Range> auxRanges =
            PlotDisplay.getAuxRanges( layers, surf, shadeFixRange, shadeAxis,
                                      dataStore );

        LayerOpt[] opts = PaperTypeSelector.getOpts( layers );
        PaperType paperType = forceBitmap
                            ? ptsel.getPixelPaperType( opts, null )
                            : ptsel.getVectorPaperType( opts );
        return PlotDisplay.createIcon( placer, layers, auxRanges, dataStore,
                                       paperType, false );
    }

    /**
     * Object capable of executing a static or interactive plot.
     * All configuration options are contained.
     */
    private interface PlotExecutor {

        /**
         * Creates a data store suitable for use with this object.
         *
         * @return    object containing plot data
         */
        DataStore createDataStore() throws IOException, InterruptedException;

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
        JComponent createPlotComponent( DataStore dataStore,
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
