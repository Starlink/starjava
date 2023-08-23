package uk.ac.starlink.ttools.plot2.task;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.Icon;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.GangContext;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotCaching;
import uk.ac.starlink.ttools.plot2.PlotScene;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.ShadeAxisKit;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Subrange;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.Trimming;
import uk.ac.starlink.ttools.plot2.ZoneContent;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.geom.MatrixFormat;
import uk.ac.starlink.ttools.plot2.geom.MatrixGanger;
import uk.ac.starlink.ttools.plot2.geom.MatrixPlotType;
import uk.ac.starlink.ttools.plot2.geom.MatrixShape;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.XyKeyPair;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;

/**
 * Plot task for matrix plot.
 *
 * @author   Mark Taylor
 * @since    22 Aug 2023
 */
public class MatrixPlot2Task extends
        TypedPlot2Task<PlaneSurfaceFactory.Profile,PlaneAspect> {

    private final Parameter<?>[] params_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.task" );

    private static final MatrixPlotType PLOT_TYPE =
        MatrixPlotType.getInstance();
    private static final boolean XDIAG = MatrixGanger.XDIAG;
    private static final Map<ConfigKey<?>,XyKeyPair<?>> XYKEY_MAP =
        Collections.unmodifiableMap( createXyKeyPairMap() );

    /**
     * Constructor.
     */
    public MatrixPlot2Task() {
        super( PLOT_TYPE, (Map<ConfigKey<String>,Input>) null );

        /* Adjust the parameter list declared by the superclass. */
        Map<String,XyKeyPair<?>> xyNameMap =
            XYKEY_MAP.entrySet().stream()
           .collect( Collectors.toMap( e -> e.getKey().getMeta().getShortName(),
                                       e -> e.getValue() ) );
        List<Parameter<?>> paramList = new ArrayList<>();
        for ( Parameter<?> param : super.getParameters() ) {
            String pname = param.getName();

            /* Parameters corresponding to the PlaneSurfaceFactory
             * X and Y aspect and profile config keys are replaced
             * in the matrix plot by N keys for an nvar-variable plot. */
            XyKeyPair<?> xyKey = xyNameMap.get( pname );
            if ( xyKey != null ) {
                if ( pname.equals( xyKey.getKeyX().getMeta().getShortName() ) ){
                    paramList.add( createExampleXyParameter( xyKey ) );
                }
                else {
                    assert pname.equals( xyKey.getKeyY()
                                        .getMeta().getShortName() );
                }
            }

            /* The aspect lock key doesn't work on a per-surface basis,
             * so exclude it from consideration.
             * Something similar could be used to affect off-diagonal
             * cells only, but that would require some additional
             * implementation. */
            else if ( pname.equals( PlaneSurfaceFactory.XYFACTOR_KEY
                                   .getMeta().getShortName() ) ) {
                // no action
            }

            /* Other keys can be added as for the Plane plot. */
            else {
                paramList.add( param );
            }
        }
        params_ = paramList.toArray( new Parameter<?>[ 0 ] );
    }

    @Override
    public String getPurpose() {
        return "Draws a matrix of plane plots";
    }

    @Override
    public Parameter<?>[] getParameters() {
        return params_;
    }

    /**
     * The implementation of this method simply adapts the
     * {@link #createPlanePlotConfiguration} method to the required signature.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected <P,A> PlotConfiguration<P,A>
                    createPlotConfiguration( Environment env,
                                             PlotContext<P,A> context )
            throws TaskException {
        PlotContext<PlaneSurfaceFactory.Profile,PlaneAspect> planeContext =
            (PlotContext<PlaneSurfaceFactory.Profile,PlaneAspect>) context;
        PlotConfiguration<PlaneSurfaceFactory.Profile,PlaneAspect> planeConfig =
            createPlanePlotConfiguration( env, planeContext );
        PlotConfiguration<P,A> config = (PlotConfiguration<P,A>) planeConfig;
        return config;
    }

    /**
     * Does the work for creating the PlotConfiguration from the environment.
     *
     * @param  env  execution environment
     * @param  context   plot context
     */
    private PlotConfiguration<PlaneSurfaceFactory.Profile,PlaneAspect>
            createPlanePlotConfiguration(
                    Environment env,
                    PlotContext<PlaneSurfaceFactory.Profile,PlaneAspect>
                                context )
            throws TaskException {

        // There is quite a lot of boilerplate in this method.
        // I could push some of it back into TypedPlot2Task if I tried.
        final SurfaceFactory<PlaneSurfaceFactory.Profile,PlaneAspect> surfFact =
            PLOT_TYPE.getSurfaceFactory();
        final PaperTypeSelector ptSel = PLOT_TYPE.getPaperTypeSelector();
        final GangerFactory<PlaneSurfaceFactory.Profile,PlaneAspect>
            gangerFact = PLOT_TYPE.getGangerFactory();

        /* Identify plotters requested from the environment. */
        Map<String,Plotter<?>> plotterMap = getPlotters( env, context );

        /* Get the sequence of layers, then of legend entries, to plot. */
        String[] layerSeq = getSequenceParameter().objectValue( env );
        if ( layerSeq == null || layerSeq.length == 0 ) {
            layerSeq = plotterMap.keySet().toArray( new String[ 0 ] );
        }
        String[] legendSeq = getLegendSequenceParameter().objectValue( env );
        if ( legendSeq == null || legendSeq.length == 0 ) {
            legendSeq = layerSeq;
        }

        /* Identify plotters that will actually be used. */
        Map<String,Plotter<?>> activePlotterMap =
            new LinkedHashMap<>( plotterMap );
        activePlotterMap.keySet().retainAll( Arrays.asList( layerSeq ) );

        /* Prepare a Ganger. */
        final Plotter<?>[] activePlotters =
            activePlotterMap.values().toArray( new Plotter<?>[ 0 ] );
        GangContext gangContext = new GangContext() {
            public Plotter<?>[] getPlotters() {
                return activePlotters;
            }
            public String[] getRequestedZoneNames() {
                return new String[ 0 ];
            }
        };
        Padding padding = getPaddingParameter().objectValue( env );
        ConfigMap gangConfig =
            createBasicConfigMap( env, gangerFact.getGangerKeys() );
        MatrixGanger ganger =
            (MatrixGanger) gangerFact.createGanger( padding, gangConfig,
                                                    gangContext );
        MatrixShape shape = ganger.getShape();
        int nz = ganger.getZoneCount();

        /* Set up generic configuration. */
        final int xpix = getXpixParameter().intValue( env );
        final int ypix = getYpixParameter().intValue( env );
        final boolean forceBitmap = getBitmapParameter().booleanValue( env );
        final DataStoreFactory storeFact =
            getDataStoreParameter().objectValue( env );
        final Compositor compositor =
            getCompositorParameter().objectValue( env );

        /* Gather the defined plot layers from the environment. */
        Map<String,PlotLayer[]> layersMap =
            createLayersMap( env, context, shape );
        PlotLayer[] allLayers =
            layersMap.values().stream()
                     .flatMap( Arrays::stream )
                     .filter( layer -> layer != null )
                     .toArray( n -> new PlotLayer[ n ] );

        /* Assemble the list of DataSpecs required for the plot. */
        DataSpec[] dataSpecs =
            Arrays.stream( allLayers )
                  .map( PlotLayer::getDataSpec )
                  .filter( d -> d != null )
                  .toArray( n -> new DataSpec[ n ] );

        /* Prepare lists of config keys and config maps based on them. */
        ConfigKey<?>[] profileKeys = getMatrixProfileKeys( surfFact );
        ConfigKey<?>[] aspectKeys = surfFact.getAspectKeys();
        ConfigKey<?>[] navKeys = surfFact.getNavigatorKeys();
        ConfigKey<?>[] shadeKeys = { StyleKeys.SHADE_LOW, StyleKeys.SHADE_HIGH};
        ConfigKey<?>[] auxKeys = StyleKeys.AUX_RAMP.getKeys();
        ConfigMap aspectConfig0 = createBasicConfigMap( env, aspectKeys );
        ConfigMap navConfig = createBasicConfigMap( env, navKeys );
        ConfigMap shadeConfig = createBasicConfigMap( env, shadeKeys );
        ConfigMap auxConfig = createBasicConfigMap( env, auxKeys );

        /* Prepare the legend. */
        Map<String,PlotLayer> layerMap = new LinkedHashMap<>();
        for ( Map.Entry<String,PlotLayer[]> entry : layersMap.entrySet() ) {
            PlotLayer layer1 = Arrays.stream( entry.getValue() )
                              .filter( l -> l != null )
                              .findFirst()
                              .orElse( null );
            if ( layer1 != null ) {
                layerMap.put( entry.getKey(), layer1 );
            }
        }
        Icon legend = createLegend( env, layerMap, legendSeq );

        /* Prepare the global trimmings object. */
        String title = createTitleParameter( null ).stringValue( env );
        float[] legPos =
            createLegendPositionParameter( null ).floatsValue( env );
        final Trimming[] trimmings = new Trimming[] {
            new Trimming( legend, legPos, title ),
        };

        /* Prepare the global ShadeAxisKit object. */
        Span shadeFixSpan =
            PlotUtil.createSpan( shadeConfig.get( StyleKeys.SHADE_LOW ),
                                 shadeConfig.get( StyleKeys.SHADE_HIGH ) );
        RampKeySet.Ramp ramp = StyleKeys.AUX_RAMP.createValue( auxConfig );
        ShadeAxisFactory shadeFact =
            createShadeAxisFactory( env, allLayers, "" );
        final ShadeAxisKit[] shadeKits =
            { new ShadeAxisKit( shadeFact, shadeFixSpan, (Subrange) null ) };

        /* Prepare the per-zone profiles and aspect config maps. */
        final PlaneSurfaceFactory.Profile[] initialProfiles =
            PlotUtil.createProfileArray( surfFact, nz );
        final ConfigMap[] aspectConfigs = new ConfigMap[ nz ];
        for ( int iz = 0; iz < nz; iz++ ) {
            MatrixShape.Cell cell = shape.getCell( iz );
            int ix = cell.getX();
            int iy = cell.getY();
            ConfigMap profileConfig =
                createCellConfigMap( env, profileKeys, cell, layerSeq );
            PlaneSurfaceFactory.Profile profile =
                surfFact.createProfile( profileConfig );
            ConfigMap aspectConfig =
                createCellConfigMap( env, aspectKeys, cell, layerSeq );
            initialProfiles[ iz ] = profile;
            aspectConfigs[ iz ] = aspectConfig;
        }
        PlaneSurfaceFactory.Profile[] profiles =
            ganger.adjustProfiles( initialProfiles );

        /* Prepare an nzone-element array of non-null PlotLayer arrays,
         * one for each zone. */
        List<List<PlotLayer>> layerLists = new ArrayList<>( nz );
        for ( int iz = 0; iz < nz; iz++ ) {
            layerLists.add( new ArrayList<PlotLayer>() );
        }
        for ( String suffix : layerSeq ) {
            PlotLayer[] layers = layersMap.get( suffix );
            for ( int iz = 0; iz < nz; iz++ ) {
                PlotLayer layer = layers[ iz ];
                if ( layer != null ) {
                    layerLists.get( iz ).add( layer );
                }
            }
        }
        final PlotLayer[][] layerArrays = new PlotLayer[ nz ][];
        for ( int iz = 0; iz < nz; iz++ ) {
            layerArrays[ iz ] =
                layerLists.get( iz ).toArray( new PlotLayer[ 0 ] );
        }

        /* Returns a PlotConfiguration object containing all the
         * information required to perform the plot. */
        return new PlotConfiguration<PlaneSurfaceFactory.Profile,PlaneAspect>(){

            public DataStore createDataStore( DataStore prevStore )
                    throws IOException, InterruptedException {
                long t0 = System.currentTimeMillis();
                DataStore store =
                    storeFact.readDataStore( dataSpecs, prevStore );
                PlotUtil.logTimeFromStart( logger_, "Data", t0 );
                return store;
            }

            public Dimension getPlotSize() {
                return new Dimension( xpix, ypix );
            }

            public Navigator<PlaneAspect> createNavigator() {
                return surfFact.createNavigator( navConfig );
            }

            public PlotScene<PlaneSurfaceFactory.Profile,PlaneAspect>
                   createPlotScene( DataStore dataStore, PlotCaching caching ) {
                return PlotScene
                      .createGangScene( ganger, surfFact, layerArrays,
                                        profiles, aspectConfigs, trimmings,
                                        shadeKits, ptSel, compositor,
                                        dataStore, caching );
            }

            public Icon createPlotIcon( DataStore dataStore ) {
                ZoneContent<PlaneSurfaceFactory.Profile,PlaneAspect>[]
                    contents = PlotUtil.createZoneContentArray( surfFact, nz );
                long t0 = System.currentTimeMillis();
                for ( int iz = 0; iz < nz; iz++ ) {
                    PlaneSurfaceFactory.Profile profile = profiles[ iz ];
                    ConfigMap config = aspectConfigs[ iz ];
                    PlotLayer[] layers = layerArrays[ iz ];
                    Range[] ranges =
                          surfFact.useRanges( profile, config )
                        ? surfFact.readRanges( profile, layers, dataStore )
                        : null;
                    PlaneAspect aspect =
                        surfFact.createAspect( profile, config, ranges );
                    contents[ iz ] =
                        new ZoneContent<PlaneSurfaceFactory.Profile,PlaneAspect>
                                       ( profile, aspect, layers );
                }
                return AbstractPlot2Task
                      .createPlotIcon( ganger, surfFact,
                                       contents, trimmings, shadeKits,
                                       ptSel, compositor, dataStore,
                                       xpix, ypix, forceBitmap );
            }
        };
    }

    /**
     * Returns a config map based on a given set of keys,
     * but specific to a given cell of the matrix.
     * For keys that correspond to the X/Y coordinates of a Plane plot,
     * values specified for the appropriate numbered coordinates of the
     * matrix plot are used, according to the matrix cell specified.
     *
     * @param   env  execution environment
     * @param   keys   config keys
     * @param   cell   matrix cell of interest
     * @param   suffixes   list of layer suffixes that will be plotted
     *                     in the cell; used to come up with default
     *                     coordinate labels
     * @return   config map
     */
    private ConfigMap createCellConfigMap( Environment env, ConfigKey<?>[] keys,
                                           MatrixShape.Cell cell,
                                           String[] suffixes )
            throws TaskException {
        int ix = cell.getX();
        int iy = cell.getY();
        Map<ConfigKey<?>,Integer> icMap = new HashMap<>();
        for ( ConfigKey<?> key : keys ) {
            XyKeyPair<?> xyKey = XYKEY_MAP.get( key );
            ConfigKey<?> xKey = xyKey == null ? null : xyKey.getKeyX();
            ConfigKey<?> yKey = xyKey == null ? null : xyKey.getKeyY();
            final int icoord;
            if ( key.equals( xKey ) && ( ix != iy || XDIAG ) ) {
                icoord = ix;
            }
            else if ( key.equals( yKey ) && ( ix != iy || !XDIAG ) ) {
                icoord = iy;
            }
            else {
                icoord = -1;
            }
            if ( icoord >= 0 ) {
                icMap.put( key, Integer.valueOf( icoord ) );
            }
        }
        ConfigParameterFactory cpFact = new ConfigParameterFactory() {
            public <T> ConfigParameter<T> getParameter( Environment env,
                                                        ConfigKey<T> key )
                    throws TaskException {
                if ( icMap.containsKey( key ) ) {
                    int icoord = icMap.get( key ).intValue();
                    String coordName =
                         MatrixPlotType.getCoordMeta( icoord ).getShortName();
                    @SuppressWarnings("unchecked")
                    XyKeyPair<T> keyPair = (XyKeyPair<T>) XYKEY_MAP.get( key );
                    ConfigParameter<T> param =
                        new ConfigParameter<T>( keyPair.createKey( coordName ));

                    /* Special handling for X/Y labels, they are defaulted to
                     * the values of the parameters giving their values. */
                    if ( key == PlaneSurfaceFactory.XLABEL_KEY ||
                         key == PlaneSurfaceFactory.YLABEL_KEY ) {
                        String dflt =
                            getPosCoordExpression( env, icoord, suffixes );
                        if ( dflt != null ) {
                            param.setStringDefault( dflt );
                        }
                    }
                    return param;
                }
                else {
                    return new ConfigParameter<T>( key );
                }
            }
        };
        return createConfigMap( env, keys, cpFact );
    }

    /**
     * Returns a map from layer name to per-zone array of layers.
     *
     * @param   env  execution environment
     * @param   context  plot context
     * @param   shape  matrix shape
     * @return  ordered map giving nzone-element PlotLayer array
     *          for each requested layer
     */
    private Map<String,PlotLayer[]>
            createLayersMap( Environment env, PlotContext<?,?> context,
                             MatrixShape shape )
            throws TaskException {
        Map<String,Plotter<?>> plotterMap = getPlotters( env, context );
        Map<String,PlotLayer[]> layersMap = new LinkedHashMap<>();
        SurfaceFactory<?,?> surfFact = PLOT_TYPE.getSurfaceFactory();
        for ( Map.Entry<String,Plotter<?>> entry : plotterMap.entrySet() ) {
            String suffix = entry.getKey();
            Plotter<?> plotter = entry.getValue();
            layersMap.put( suffix,
                           createPlotLayers( env, suffix, plotter, surfFact,
                                             shape ) );
        }
        return layersMap;
    }

    /**
     * Returns an nzone-element array of plot layers for a given requested
     * layer.
     *
     * @param  env  execution environment
     * @param  layerSuffix  suffix associated with requested layer
     * @param  plotter   layer plotter
     * @param  surfFact  surface factory
     * @param  shape   matrix shape
     * @return   nzone-element array of plot layers, some elements may be null
     */
    private <S extends Style>
            PlotLayer[] createPlotLayers( Environment env, String layerSuffix,
                                          Plotter<S> plotter,
                                          SurfaceFactory<?,?> surfFact,
                                          MatrixShape shape )
            throws TaskException {

        /* Get style from configuration keys in environment for this layer. */
        ConfigMap profileConfig =
            createBasicConfigMap( env, getMatrixProfileKeys( surfFact ) );
        ConfigMap captionConfig =
            createBasicConfigMap( env, StyleKeys.CAPTIONER.getKeys() );
        ConfigMap layerConfig =
            createLayerSuffixedConfigMap( env, plotter.getStyleKeys(),
                                          layerSuffix );
        ConfigMap auxConfig =
            createBasicConfigMap( env, StyleKeys.AUX_RAMP.getKeys() );
        ConfigMap config = new ConfigMap();
        config.putAll( profileConfig );
        config.putAll( captionConfig );
        config.putAll( auxConfig );
        config.putAll( layerConfig );
        final S style;
        try {
            style = plotter.createStyle( config );
            assert style.equals( plotter.createStyle( config ) );
        }
        catch ( ConfigException e ) {
            throw new UsageException( e.getConfigKey().getMeta().getShortName()
                                    + ": " + e.getMessage(), e );
        }

        /* Populate PlotLayer array with per-zone elements. */
        CoordGroup cgrp = plotter.getCoordGroup();
        boolean isOnDiag = MatrixFormat.isOnDiagonal( cgrp );
        boolean isOffDiag = MatrixFormat.isOffDiagonal( cgrp );
        int npos = cgrp.getBasicPositionCount();
        Coord[] extraCoords = cgrp.getExtraCoords();
        boolean hasPos = npos + cgrp.getExtraPositionCount() > 0;
        DataGeom geom = hasPos ? PlaneDataGeom.INSTANCE : null;
        StarTable table = getInputTable( env, layerSuffix );
        int ncell = shape.getCellCount();
        PlotLayer[] layers = new PlotLayer[ ncell ];
        for ( int icell = 0; icell < ncell; icell++ ) {
            MatrixShape.Cell cell = shape.getCell( icell );
            int ix = cell.getX();
            int iy = cell.getY();
            Coord[] posCoords = hasPos ? geom.getPosCoords() : new Coord[ 0 ];
            List<CoordValue> cvlist = new ArrayList<>();
            final boolean hasCell;

            /* The positional coordinates, where required, have to be
             * got in the context of the X and Y locations of this cell
             * in the matrix. */
            if ( isOffDiag ) {
                if ( ix != iy ) {
                    hasCell = true;
                    for ( int ipos = 0; ipos < npos; ipos++ ) {
                        String posSuffix = npos > 1
                                         ? PlotUtil.getIndexSuffix( ipos )
                                         : "";
                        cvlist.add( getPosCoordValue( env, ix, posSuffix,
                                                      layerSuffix, true ) );
                        cvlist.add( getPosCoordValue( env, iy, posSuffix,
                                                      layerSuffix, true ) );
                    }
                }
                else {
                    hasCell = false;
                }
            }
            else if ( isOnDiag ) {
                if ( ix == iy ) {
                    hasCell = true;
                    cvlist.add( getPosCoordValue( env, ix, "", layerSuffix,
                                                  true ) );
                }
                else {
                    hasCell = false;
                }
            }
            else {
                hasCell = true;
            }
            if ( hasCell ) {

                /* Add any remaining non-positional coordinates to the
                 * positional ones we have assembled. */
                int iex0 = isOnDiag ? 1 : 0;
                for ( int iex = iex0; iex < extraCoords.length; iex++ ) {
                    cvlist.add( getCoordValue( env, extraCoords[ iex ],
                                               layerSuffix ) );
                }

                /* Create and store the layer for this cell if there is one. */
                CoordValue[] coordVals = cvlist.toArray( new CoordValue[ 0 ] );
                DataSpec dataSpec = new JELDataSpec( table, null, coordVals );
                layers[ icell ] = plotter.createLayer( geom, dataSpec, style );
            }
        }
        return layers;
    }

    /**
     * Returns a mapping of PlaneSurfaceFactory keys to their corresponding
     * XyKeyPair objects.
     *
     * @return   map from X/Y-type keys to XyKeyPairs they comem from
     */
    private static Map<ConfigKey<?>,XyKeyPair<?>> createXyKeyPairMap() {
        Map<ConfigKey<?>,XyKeyPair<?>> map = new HashMap<>();
        for ( XyKeyPair<?> xy :
              PLOT_TYPE.getSurfaceFactory().getXyKeyPairs() ) {
            map.put( xy.getKeyX(), xy );
            map.put( xy.getKeyY(), xy );
        }
        return map;
    }

    /**
     * Returns a doctored list of Profile keys from a given surface factory.
     * At present it just excludes XYFACTOR_KEY which doesn't work well
     * with the multi-panel plot defined here: for instance the histogram-like
     * plots on the diagonal should not have a fixed aspect ratio,
     * and MatrixGanger.adjustAspects doesn't cope with it.
     *
     * @param  surfFact  surface factory (presumably a PlaneSurfaceFactory)
     * @return   list of profile keys suitable for matrix plot
     */
    private static ConfigKey<?>[]
            getMatrixProfileKeys( SurfaceFactory<?,?> surfFact ) {
        return Arrays.stream( surfFact.getProfileKeys() )
                     .filter( k -> k != PlaneSurfaceFactory.XYFACTOR_KEY )
                     .toArray( n -> new ConfigKey<?>[ n ] );
    }

    /**
     * Creates a parameter to acquire a positional coordinate value.
     *
     * @param   icoord   dimension index of positional coordinate
     * @param   posSuffix  parameter suffix giving index of the position
     *                     in a multi-position coord group;
     *                     empty string for single-position coord group
     * @param   layerSuffix  suffix identifying requested layer
     * @param  fullDetail  if true, extra detail is appended to the description
     * @return  new parameter
     */
    private static StringParameter
            createPosCoordParameter( int icoord, String posSuffix,
                                     String layerSuffix, boolean fullDetail ) {
        int ic1 = icoord + 1;
        boolean hasLayerSuffix = layerSuffix.length() > 0;
        boolean hasPosSuffix = posSuffix.length() > 0;
        Input templateInput = PlaneDataGeom.X_COORD.getInputs()[ 0 ];
        InputMeta templateMeta = templateInput.getMeta();
        String cName = templateMeta.getShortName() + posSuffix + ic1;
        Domain<?> domain = templateInput.getDomain();
        StringParameter param = new StringParameter( cName + layerSuffix );
        String prompt = "Value " + ( hasPosSuffix ? ( posSuffix + " " ) : "" )
                                 + "for plot coordinate " + ic1;
        if ( fullDetail ) {
            prompt += hasLayerSuffix ? ( ", for layer " + layerSuffix ) : "";
        }
        StringBuffer dbuf = new StringBuffer()
            .append( "<p>Numeric value for coordinate #" )
            .append( ic1 )
            .append( " in the matrix plot.\n" )
            .append( "N such values must be supplied for an NxN grid " )
            .append( "of scatter plots.\n" )
            .append( "</p>\n" );
        dbuf.append( "<p>" );
        if ( fullDetail ) {
            dbuf.append( "This parameter gives a column name, " )
                .append( "fixed value, or algebraic expression for the\n" )
                .append( "<code>" )
                .append( cName )
                .append( "</code> coordinate\n" );
            if ( hasLayerSuffix ) {
                dbuf.append( "for layer <code>" )
                    .append( layerSuffix )
                    .append( "</code>" );
            }
            dbuf.append( ".\n" );
        }
        dbuf.append( "The value is a numeric algebraic expression " )
            .append( "based on column names\n" )
            .append( "as described in <ref id='jel'/>.\n" )
            .append( "</p>\n" );
        param.setDescription( dbuf.toString() );
        param.setUsage( "<expr>" );
        return param;
    }

    /**
     * Returns the value of a positional coordinate specified by the user
     * for a given requested layer.
     *
     * @param   env  execution environment
     * @param   icoord   dimension index of positional coordinate
     * @param   posSuffix  parameter suffix giving index of the position
     *                     in a multi-position coord group;
     *                     empty string for single-position coord group
     * @param   layerSuffix  suffix identifying requested layer
     * @param   requireValue  whether value must be supplied
     * @return   coord value
     */
    private static CoordValue getPosCoordValue( Environment env, int icoord,
                                                String posSuffix,
                                                String layerSuffix,
                                                boolean requireValue )
            throws TaskException {
        Coord templateCoord = PlaneDataGeom.X_COORD;
        Parameter<String> exprParam = new ParameterFinder<Parameter<String>>() {
            public Parameter<String> createParameter( String sfix ) {
                return createPosCoordParameter( icoord, posSuffix, sfix, true );
            }
        }.getParameter( env, layerSuffix );
        exprParam.setNullPermitted( ! requireValue );
        String expr = exprParam.stringValue( env );
        return new CoordValue( templateCoord, new String[] { expr },
                               new DomainMapper[] { null } );
    }

    /**
     * Attempts to get an expression entered by the user for a given
     * positional coordinate.  The supplied layers are examined in turn,
     * and the first time a value is found it is returned.
     *
     * @param   env  execution environment
     * @param   icoord   dimension index of positional coordinate
     * @param   layerSuffixes   suffixes of layers in which to look
     * @return   representative expression, or null
     */
    private static String getPosCoordExpression( Environment env, int icoord,
                                                 String[] layerSuffixes )
            throws TaskException {
        String posSuffix = "";
        for ( String layerSuffix : layerSuffixes ) {
            CoordValue cval =
                getPosCoordValue( env, icoord, posSuffix, layerSuffix, false );
            String[] exprs = cval.getExpressions();
            if ( exprs.length > 0 ) {
                String expr = exprs[ 0 ];
                if ( expr != null && expr.trim().length() > 0 ) {
                    return expr;
                }
            }
        }
        return null;
    }

    /**
     * Returns a parameter that can be used for documentation purposes
     * that has indexed values in the matrix plot, but corresponds to
     * a given XyKeyPair.
     *
     * @param  xyKey  template key pair
     * @return  parameter suitable for generic documentation
     */
    private static <T> Parameter<T>
            createExampleXyParameter( XyKeyPair<T> xyKey ) {
        ConfigKey<T> key = xyKey.createKey( "xK" );
        Parameter<T> param = new ConfigParameter<T>( key );
        param.setName( param.getName().replaceFirst( "[xX][kK]", "xK" ) );
        String prompt = param.getPrompt();
        if ( prompt != null ) {
            param.setPrompt( prompt.replaceAll( " [xX][kK] ", " xK " ) );
        }
        String descrip = param.getDescription();
        if ( descrip != null ) {
            descrip = descrip.replaceAll( " [xX][kK] ", " xK " );
            descrip = descrip + String.join( "\n",
                "<p>The xK axis refers to any axes in the matrix",
                "on which input coordinate #K is plotted.",
                "Hence",
                "<code>" + xyKey.createKey( "x2" ).getMeta().getShortName()
                         + "</code>",
                "affects the axes on which the <code>x2</code> coordinates",
                "are plotted.",
                "</p>",
                "" );
            param.setDescription( descrip );
        }
        return param;
    }
}
