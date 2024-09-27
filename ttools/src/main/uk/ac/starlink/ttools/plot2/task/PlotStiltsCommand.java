package uk.ac.starlink.ttools.plot2.task;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.filter.SelectFilter;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.SkySysConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.MatrixGangerFactory;
import uk.ac.starlink.ttools.plot2.geom.MatrixPlotType;
import uk.ac.starlink.ttools.plot2.geom.MatrixShape;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.SpherePlotType;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.TimePlotType;
import uk.ac.starlink.ttools.plot2.geom.XyKeyPair;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapeModePlotter;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.task.CommandFormatter;
import uk.ac.starlink.ttools.task.CredibleString;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.SettingGroup;
import uk.ac.starlink.ttools.task.StiltsCommand;
import uk.ac.starlink.ttools.task.TableNamer;
import uk.ac.starlink.util.LoadException;

/**
 * StiltsCommand subclass for plot2 commands.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2017
 */
public class PlotStiltsCommand extends StiltsCommand {

    private final AbstractPlot2Task plotTask_;

    private static final ConfigKey<?>[] AUX_KEYS = getAuxKeys(); 

    /**
     * Constructor.
     *
     * @param  task  plot task
     * @param  taskName  name of the plot task as used by stilts command line
     * @param  groups   all name-value pairs specifying the configuration
     *                  of the task, grouped for cosmetic purposes
     */
    public PlotStiltsCommand( AbstractPlot2Task task, String taskName,
                              SettingGroup[] groups ) {
        super( task, taskName, groups );
        plotTask_ = task;
    }

    /**
     * Returns the plot task corresponding to this object.
     *
     * @return  plot task object
     */
    public AbstractPlot2Task getTask() {
        return plotTask_;
    }

    /**
     * Returns a task object corresponding to a given plot task name
     * known by the STILTS application.
     *
     * @param  taskName  stilts plot2 task name
     * @return plot task object
     */
    private static AbstractPlot2Task createTask( String taskName )
            throws LoadException {
        return Stilts.getPlot2TaskFactory().createObject( taskName );
    }

    /**
     * Turns a PlotSpec into an abstract model of a STILTS command line
     * to regenerate the same plot.  Various methods are provided to
     * export this in a way that can be presented to the user or executed.
     *
     * <p>This is the method that does the work of mapping the internal
     * plot representation to a STILTS command line.  It has to understand
     * how the AbstractPlot2Task parameters are specified, which is
     * a complicated job. So (1) the output is not bulletproof, and may get
     * broken by implementation or interface changes elsewhere in the
     * code base, and therefore (2) lots of tests are a good idea.
     * It works on a best efforts basis.
     *
     * @param  plotSpec  programmatic representation of a plot
     * @param  tableNamer  controls table naming
     * @param  layerSuffixer  controls suffixes for layers
     * @param  zoneSuffixer  cotrols suffixes for zones
     * @return   new command
     */
    public static <P,A> PlotStiltsCommand
            createPlotCommand( PlotSpec<P,A> plotSpec, TableNamer tableNamer,
                               Suffixer layerSuffixer, Suffixer zoneSuffixer )
            throws LoadException {
        PlotType<P,A> plotType = plotSpec.getPlotType();
        Dimension extSize = plotSpec.getExtSize();
        ConfigMap globalConfig = plotSpec.getGlobalConfig();
        ConfigMap[] zoneConfigs = plotSpec.getZoneConfigs();
        TrimmingSpec[] trimSpecs = plotSpec.getTrimmingSpecs();
        ShadeSpec[] shadeSpecs = plotSpec.getShadeSpecs();
        LayerSpec[] layerSpecs = plotSpec.getLayerSpecs();
        GangerFactory<P,A> gangerFact = plotType.getGangerFactory();
        boolean hasIndependentZones = gangerFact.hasIndependentZones();
        int nz = zoneConfigs.length;
        boolean isShadeGlobal = nz > 1 && shadeSpecs.length == 1;
        boolean isTrimGlobal = nz > 1 && trimSpecs.length == 1;

        /* Work out which plot command to use. */
        String taskName = getPlotTaskName( plotType );
        AbstractPlot2Task task = createTask( taskName );

        /* Global settings for the task. */
        List<Setting> taskSettings = new ArrayList<>();
        if ( extSize != null ) {
            Integer xp = Integer.valueOf( extSize.width );
            Integer yp = Integer.valueOf( extSize.height );
            taskSettings.add( createParamSetting( task.getXpixParameter(),
                                                  xp ) );
            taskSettings.add( createParamSetting( task.getYpixParameter(),
                                                  yp ) );
        }
        taskSettings.addAll( Arrays.asList( new Setting[] {
            createParamSetting( task.getPaddingParameter(),
                                plotSpec.getPadding() ),
        } ) );
        ConfigKey<?>[] gangKeys = gangerFact.getGangerKeys();
        List<Setting> globalSettings = new ArrayList<>();
        globalSettings.addAll( getConfigSettings( globalConfig, gangKeys ) );
        globalSettings.add( null );
        globalSettings.addAll( getGlobalSettings( plotSpec ) );

        /* Plot surface settings. */
        SurfaceFactory<P,A> sfact = plotType.getSurfaceFactory();
        ConfigKey<?>[] profileKeys = sfact.getProfileKeys();
        ConfigKey<?>[] aspectKeys = sfact.getAspectKeys();
        Map<String,List<Setting>> zoneSettings = new LinkedHashMap<>();

        /* Zone suffixes. */
        String[] zkeys =
            zoneSuffixer.createSuffixes( nz ).toArray( new String[ 0 ] );

        /* Set up adjusters to deal with plot-type specific situations. */
        ZoneAdjuster[] zadjusters = new ZoneAdjuster[ nz ];
        for ( int iz = 0; iz < nz; iz++ ) {
            List<LayerSpec> lspecs = new ArrayList<>();
            for ( LayerSpec ls : layerSpecs ) {
                if ( ls.getZoneIndex() == iz ) {
                    lspecs.add( ls );
                }
            }
            zadjusters[ iz ] =
                createZoneAdjuster( zoneConfigs[ iz ], lspecs, plotType );
        }

        /* Per-zone settings. */
        if ( hasIndependentZones || nz == 1 ) {
            assert ! ( plotSpec.getPlotType() instanceof MatrixPlotType );
            for ( int iz = 0; iz < nz; iz++ ) {
                ConfigMap zoneConfig = zoneConfigs[ iz ];
                List<Setting> settings = new ArrayList<>();
                settings.addAll( getConfigSettings( zoneConfig, profileKeys ) );
                settings.add( null );
                settings.addAll( getConfigSettings( zoneConfig, aspectKeys ) );
                settings.add( null );
                TrimmingSpec trimSpec = isTrimGlobal ? null : trimSpecs[ iz ];
                settings.addAll( getTrimSettings( task, trimSpec ) );
                ShadeSpec shadeSpec = isShadeGlobal ? null : shadeSpecs[ iz ];
                settings.addAll( getShadeSettings( task, shadeSpec ) );
                zadjusters[ iz ].adjustZoneSettings( settings );
                zoneSettings.put( zkeys[ iz ], settings );
            }
        }

        /* Global shader and trimming settings if applicable. */
        TrimmingSpec trimSpec = isTrimGlobal ? trimSpecs[ 0 ] : null;
        globalSettings.addAll( getTrimSettings( task, trimSpec ) );
        ShadeSpec shadeSpec = isShadeGlobal ? shadeSpecs[ 0 ] : null;
        globalSettings.addAll( getShadeSettings( task, shadeSpec ) );

        /* Per-layer settings. */
        Map<String,String> layerTypes = new LinkedHashMap<>();
        Map<String,List<Setting>> layerSettings = new LinkedHashMap<>();
        int nl = layerSpecs.length;
        Collection<String> legKeys = new ArrayList<>( nl );
        List<String> lkeys = layerSuffixer.createSuffixes( nl );
        boolean excludeLegend = false;
        for ( int il = 0; il < nl; il++ ) {
            LayerSpec lspec = layerSpecs[ il ];
            int iz = lspec.getZoneIndex();
            List<Setting> lsettings = new ArrayList<>();

            /* Assign a layer suffix. */
            String lkey = lkeys.get( il );

            /* Zone identifier. */
            if ( iz >= 0 ) {
                lsettings.add( new Setting( AbstractPlot2Task.ZONE_PREFIX,
                                            zkeys[ iz ], "" ) );
                lsettings.add( null );
            }

            /* Work out the layer type and possibly associated shading type. */
            Plotter<?> plotter = lspec.getPlotter();
            final String ltype;
            List<Setting> modeSettings = new ArrayList<>();
            if ( plotter instanceof ShapeModePlotter ) {
                ShapeModePlotter sPlotter = (ShapeModePlotter) plotter;
                ShapeForm form = sPlotter.getForm();
                ShapeMode mode = sPlotter.getMode();
                ltype = form.getFormName();
                modeSettings.add( new Setting( ShapeFamilyLayerType
                                              .SHADING_PREFIX,
                                               mode.getModeName(), null ) );
            }
            else {
                ltype = plotter.getPlotterName();
            }

            /* Input table setting, if any. */
            lsettings.addAll( createInputTableSettings( lspec, tableNamer ) );
            lsettings.add( null );

            /* DataGeom setting, if any. */
            lsettings.addAll( createGeomSettings( lspec, task ) );

            /* Input data coordinate settings, if any. */
            lsettings.addAll( createCoordSettings( lspec ) );
            lsettings.add( null );

            /* Layer style configuration. */
            lsettings.addAll( modeSettings );
            lsettings.addAll( getConfigSettings( lspec.getConfig(),
                                                 plotter.getStyleKeys() ) );
            if ( iz >= 0 ) {
                zadjusters[ iz ].adjustLayerSettings( lspec, lsettings );
            }
            lsettings.add( null );

            /* Legend label, if any. */
            TrimmingSpec.LegendSpec legSpec =
                  iz >= 0 && iz < trimSpecs.length && trimSpecs[ iz ] != null
                ? trimSpecs[ iz ].getLegendSpec()
                : null;
            if ( legSpec != null ) {
                String leglabel = lspec.getLegendLabel();
                if ( leglabel != null ) {
                    lsettings.add(
                        createParamSetting( AbstractPlot2Task
                                           .createLabelParameter( "" ),
                        leglabel ) );
                    legKeys.add( lkey );
                }
                else {
                    excludeLegend = true;
                }
                lsettings.add( null );
            }

            /* Store layer definition in a map. */
            layerTypes.put( lkey, ltype );
            layerSettings.put( lkey, lsettings );
        }

        /* Factorise out settings that are common to all zones or to
         * all layers. */
        List<Setting> commonZoneSettings =
            extractCommonSettings( zoneSettings.values() );
        List<Setting> commonLayerSettings =
            extractCommonSettings( layerSettings.values() );

        /* Miscellaneous settings at the end. */
        List<Setting> trailSettings = new ArrayList<>();
        if ( lkeys.size() > 1 ) {
            Setting seqSetting =
                createParamSetting( task.getSequenceParameter(),
                                    lkeys.toArray( new String[ 0 ] ) );
            trailSettings.add( asDefaultSetting( seqSetting ) );
        }
        if ( excludeLegend ) {
            trailSettings
               .add( createParamSetting( task.getLegendSequenceParameter(),
                                         legKeys.toArray( new String[ 0 ] ) ) );
        }
        trailSettings.add( null );
        trailSettings.add( createParamSetting( AbstractPlot2Task
                                              .createPaintModeParameter()
                                              .getOutputParameter(),
                                               null ) );

        /* Collect all the settings together and return them as a
         * plot object. */
        List<SettingGroup> groups = new ArrayList<>();
        groups.addAll( toGroups( 1, taskSettings ) );
        groups.addAll( toGroups( 1, globalSettings ) );
        groups.addAll( toGroups( 1, commonZoneSettings ) );
        for ( String zkey : zoneSettings.keySet() ) {
            List<Setting> zsettings = zoneSettings.get( zkey );
            groups.addAll( toGroups( 1, addSuffixes( zsettings, zkey ) ) );
        }
        groups.addAll( toGroups( 1, commonLayerSettings ) );
        for ( String lkey : layerTypes.keySet() ) {
            String ltype = layerTypes.get( lkey );
            List<Setting> lsettings = layerSettings.get( lkey );
            groups.add( new SettingGroup( 1, new Setting[] {
                new Setting( AbstractPlot2Task.LAYER_PREFIX + lkey, ltype,
                             null ),
            } ) );
            groups.addAll( toGroups( 2, addSuffixes( lsettings, lkey ) ) );
        }
        groups.addAll( toGroups( 1, trailSettings ) );
        return new PlotStiltsCommand( task, taskName,
                                      groups.toArray( new SettingGroup[ 0 ] ) );
    }

    /**
     * Returns the STILTS task name corresponding to a given plot type.
     *
     * @param  ptype  plot type
     * @return  stilts task name
     */
    private static String getPlotTaskName( PlotType<?,?> ptype ) {
        if ( ptype instanceof MatrixPlotType ) {
            return "plot2corner";
        }
        else if ( ptype instanceof PlanePlotType ) {
            return "plot2plane";
        }
        else if ( ptype instanceof SkyPlotType ) {
            return "plot2sky";
        }
        else if ( ptype instanceof CubePlotType ) {
            return "plot2cube";
        }
        else if ( ptype instanceof SpherePlotType ) {
            return "plot2sphere";
        }
        else if ( ptype instanceof TimePlotType ) {
            return "plot2time";
        }
        else {
            throw new IllegalArgumentException( "Unknown plot type " + ptype );
        }
    }

    /**
     * Returns a list of settings relevant to the aux shade axis.
     *
     * @param  task   plot task
     * @param  shadeSpec  shading specification
     * @return  settings
     */
    private static List<Setting> getShadeSettings( AbstractPlot2Task task,
                                                   ShadeSpec shadeSpec ) {
        List<Setting> settings = new ArrayList<>();
        if ( shadeSpec != null ) {
            settings.addAll( getConfigSettings( shadeSpec.getConfig(),
                                                AUX_KEYS ) );
            settings.add( null );
            boolean isVisible = shadeSpec.isVisible();
            settings.add( createParamSetting(
                              task.createAuxVisibleParameter( null ),
                              Boolean.valueOf( isVisible ) ) );
            if ( isVisible ) {
                settings.addAll( Arrays.asList( new Setting[] {
                    createParamSetting( task.createAuxLabelParameter( null ),
                                        shadeSpec.getLabel() ),
                    createParamSetting( task.createAuxCrowdParameter( null ),
                                        shadeSpec.getCrowding() ),
                } ) );
            }
            settings.add( null );
        }
        return settings;
    }

    /**
     * Returns a list of settings relevant to plot trimmings.
     *
     * @param  task  plot task
     * @param  trimSpec  trimming specification
     * @return  settings
     */
    private static List<Setting> getTrimSettings( AbstractPlot2Task task,
                                                  TrimmingSpec trimSpec ) {
        List<Setting> settings = new ArrayList<>();
        if ( trimSpec != null ) {
            settings.add( createParamSetting( task.createTitleParameter( null ),
                                              trimSpec.getTitle() ) );
            TrimmingSpec.LegendSpec legSpec = trimSpec.getLegendSpec();
            boolean hasLegend = legSpec != null;
            settings.add( createParamSetting( task.getLegendParameter(),
                                              hasLegend ) );
            if ( hasLegend ) {
                settings.addAll( Arrays.asList( new Setting[] {
                    createParamSetting( task.getLegendBorderParameter(),
                                        legSpec.hasBorder() ),
                    createParamSetting( task.getLegendOpaqueParameter(),
                                        legSpec.isOpaque() ),
                    createParamSetting( task
                                       .createLegendPositionParameter( null ),
                                        toDoubles( legSpec.getPosition() ) ),
                } ) );
            }
            settings.add( null );
        }
        return settings;
    }

    /**
     * For each non-null setting in a supplied list, appends a given
     * suffix to the key part.  Null entries are left untouched.
     *
     * @param  settings   input setting list
     * @param  suffix     suffix to append to each setting key
     * @return  output setting list
     */
    private static List<Setting> addSuffixes( List<Setting> settings,
                                              String suffix ) {
        List<Setting> outList = new ArrayList<>( settings.size() );
        for ( Setting s : settings ) {
            outList.add( s == null ? null : s.appendSuffix( suffix ) );
        }
        return outList;
    }

    /**
     * Factorises the settings in a collection of lists.
     * Settings for keys that appear in multiple elements of the collection
     * are examined.  For each key, if it appears in more than one list,
     * and has the same value in all the lists in which it appears,
     * it is added to a list of common settings and removed from each
     * of the individual lists.
     * This allows repeated specifications for keys appearing in
     * multiple lists to be 'factorised out', so a shorter parameter list
     * is required for the task.
     *
     * @param   lists   collection of mutable lists of settings
     *                  from which to factor out items;
     *                  any of these lists may be modified in place
     *                  (comment entries are removed)
     * @return  list of settings which have the same value in all of the
     *          supplied lists, and appear in more than one of them
     */
    private static List<Setting>
            extractCommonSettings( Collection<List<Setting>> lists ) {
        List<Setting> common = new ArrayList<>();
        if ( lists.size() > 1 ) {
            Set<String> allkeys = new LinkedHashSet<>();
            for ( List<Setting> list : lists ) {
                for ( Setting s : list ) {
                    if ( s != null ) {
                        allkeys.add( s.getKey() );
                    }
                }
            }
            for ( String key : allkeys ) {
                int nsame = 0;
                int ndiff = 0;
                Setting s0 = null;
                for ( List<Setting> list : lists ) {
                    Setting s1 = findSetting( list, key );
                    if ( s1 != null ) {
                        if ( s0 == null ) {
                            s0 = s1;
                        }
                        if ( s1.equals( s0 ) ) {
                            nsame++;
                        }
                        else {
                            ndiff++;
                        }
                    }
                }
                if ( nsame > 1 && ndiff == 0 ) {
                    common.add( s0 );
                    for ( List<Setting> list : lists ) {
                        list.remove( s0 );
                    }
                }
            }
        }
        return common;
    }

    /**
     * Identifies a setting from a given list that has a given key.
     *
     * @param   settings  list of settings
     * @param   key    key to look for
     * @return  the first setting from the list with the given key,
     *          or null if none have it
     */
    private static Setting findSetting( List<Setting> settings, String key ) {
        for ( Setting s : settings ) {
            if ( s != null && s.getKey().equals( key ) ) {
                return s;
            }
        }
        return null;
    }

    /**
     * Extracts a list of settings corresponding to a given list of
     * config keys.  The entries appearing in the output depend
     * on the supplied list of keys, not the entries that have been
     * explicitly added to the config map.
     *
     * @param  config   config map containing configuration data
     * @param  keys   list of keys defining which config entries to use
     * @return   list of settings, one for each supplied key
     */
    private static List<Setting> getConfigSettings( ConfigMap config,
                                                    ConfigKey<?>[] keys ) {
        int nk = keys.length;
        List<Setting> settings = new ArrayList<>( nk );
        for ( int ik = 0; ik < nk; ik++ ) {
            settings.add( createConfigSetting( keys[ ik ], config ) );
        }
        return settings;
    }

    /**
     * Creates a Setting corresponding to a given config option.
     *
     * @param  key   config key
     * @param  config   config map that may contain information on value
     * @return  new setting object
     */
    private static <T> Setting createConfigSetting( ConfigKey<T> key,
                                                    ConfigMap map ) {
        return createNamedConfigSetting( key, map, getSettingKey( key ) );
    }

    /**
     * Creates a setting based on a given config option with an explicitly
     * supplied setting key.
     *
     * @param  key   config key
     * @param  config   config map that may contain information on value
     * @param  settingKey  key for created setting object
     * @return  new setting object
     */
    private static <T> Setting createNamedConfigSetting( ConfigKey<T> configKey,
                                                         ConfigMap config,
                                                         String settingKey ) {
        return new Setting( settingKey,
                            configKey.valueToString( config.get( configKey ) ),
                            configKey.valueToString( configKey
                                                    .getDefaultValue() ) );
    }

    /**
     * Returns the setting key name when the setting is based on a
     * ConfigKey object.
     *
     * @param  key   config key
     * @return  setting key name
     */
    private static String getSettingKey( ConfigKey<?> key ) {
        return key.getMeta().getShortName();
    }

    /**
     * Sets the default value of a setting to its current value.
     * This has the effect of making it hidden in the (usual) case
     * where only non-default setting values are exported.
     *
     * @param  setting
     * @return   new setting with default equal to current value
     */
    private static Setting asDefaultSetting( Setting setting ) {
        return setting.resetDefault( setting.getStringValue() );
    }

    /**
     * Returns a list of settings corresponding to the input table
     * implied by a given layer specification.
     *
     * @param   lspec  layer specification
     * @param   namer  file naming policy
     * @return   list of zero or more setting objects
     */
    private static List<Setting>
            createInputTableSettings( LayerSpec lspec, TableNamer namer ) {
        List<Setting> settings = new ArrayList<>();
        StarTable table = lspec.getTable();
        if ( table != null ) {
            CredibleString selection = lspec.getSelectExpr();
            String suffix = "";
            InputTableParameter inParam =
                AbstractPlot2Task.createTableParameter( suffix );
            FilterParameter filterParam =
                AbstractPlot2Task.createFilterParameter( suffix, inParam );
            return StiltsCommand
                  .createInputTableSettings( inParam, table, namer,
                                             filterParam, selection );
        }
        else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns a list of settings corresponding to the coordinates
     * used by a given layer specification.
     *
     * @param   lspec  layer specification
     * @return   list of zero or more setting objects
     */
    private static List<Setting> createCoordSettings( LayerSpec lspec ) {
        List<Setting> settings = new ArrayList<>();

        /* Add coordinate values if any. */
        for ( CoordSpec cspec : lspec.getCoordSpecs() ) {
            String inName = cspec.getInputName();
            settings.add( new Setting( inName, cspec.getValueExpr(), null ) );
            DomainMapper dm = cspec.getDomainMapper();
            DomainMapper dfltDm = cspec.getDefaultDomainMapper();
            if ( dm != null ) {
                Setting setting =
                    new Setting( inName + AbstractPlot2Task.DOMAINMAPPER_SUFFIX,
                                 dm.getSourceName(),
                                 dfltDm == null ? null
                                                : dfltDm.getSourceName() );
                settings.add( setting );
            }
        }
        return settings;
    }

    /**
     * Returns a list of settings corresponding to the DataGeom selected
     * for a given layer specification.
     *
     * @param   lspec  layer specification
     * @param   task   task on behalf of which layer is working
     * @return   list of zero or more setting objects
     */
    private static List<Setting> createGeomSettings( LayerSpec lspec,
                                                     AbstractPlot2Task task ) {
        PlotContext<?,?> context = task instanceof TypedPlot2Task
                                 ? ((TypedPlot2Task) task).getPlotContext()
                                 : null;
        Parameter<?> geomParam = context == null
                               ? null
                               : context.getGeomParameter( "" );
        List<Setting> settings = new ArrayList<>();
        if ( geomParam != null &&
             DataGeom.class.isAssignableFrom( geomParam.getValueClass() ) ) {
            @SuppressWarnings("unchecked")
            Parameter<DataGeom> typedGeomParam =
                (Parameter<DataGeom>) geomParam;
            DataGeom geom = lspec.getDataGeom();
            if ( geom != null ) {
                settings.add( createParamSetting( typedGeomParam, geom ) );
            }
        }
        return settings;
    }

    /**
     * Returns the key's value if it is specified in the given config map,
     * or null if it is not.  Note this differs from just calling
     * <code>config.get(key)</code>, which will return the default value
     * if no key is present.
     *
     * @param  key   config key
     * @param  config   config map
     * @return   value explicitly present in map, or null
     */
    private static <T> T getExplicitValue( ConfigKey<T> key,
                                           ConfigMap config ) {
        return config.keySet().contains( key ) ? config.get( key ) : null;
    }

    /**
     * Converts a float array to a double array.
     *
     * @param  farray  array of floats
     * @return  equivalent array of doubles
     */
    private static double[] toDoubles( float[] farray ) {
        if ( farray == null ) {
            return null;
        }
        else {
            int n = farray.length;
            double[] darray = new double[ n ];
            for ( int i = 0; i < n; i++ ) {
                double dval = farray[ i ];
                darray[ i ] = PlotUtil.roundNumber( dval, 1e-5 * dval );
            }
            return darray;
        }
    }

    /**
     * Creates a zone adjuster for a given zone and the layers within it.
     * This is a special measure that may be required to cope with some
     * zone/layer interactions not otherwise handled by the configuration
     * logic.
     *
     * @param   config   configuration in effect for the zone
     * @param   lspecs  array of specifications for all the layers that
     *                  appear within the given zone
     * @param  plotType  plot type
     * @return  appropriate zone adjuster, not null (but may be a noop)
     */
    private static ZoneAdjuster createZoneAdjuster( ConfigMap config,
                                                    List<LayerSpec> lspecs,
                                                    PlotType<?,?> plotType ) {
        if ( plotType instanceof SkyPlotType ) {
            return new SkySysZoneAdjuster( config, lspecs );
        }
        else {
            return new ZoneAdjuster() {
                public void adjustZoneSettings( List<Setting> zsettings ) {
                }
                public void adjustLayerSettings( LayerSpec lspec,
                                                 List<Setting> lsettings ) {
                }
            };
        }
    }

    /**
     * Hook for returning special settings relating to a given PlotSpec.
     *
     * @param  plotSpec  plot specification
     * @return   additional settings to add to the global list
     */
    private static List<Setting> getGlobalSettings( PlotSpec<?,?> plotSpec ) {
        if ( plotSpec.getPlotType() instanceof MatrixPlotType ) {
            return getMatrixGlobalSettings( plotSpec );
        }
        else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the list of config keys used for configuring the
     * Aux shade axis.
     *
     * @return  shade axis config keys
     */
    private static ConfigKey<?>[] getAuxKeys() {
        List<ConfigKey<?>> keys = new ArrayList<>();
        keys.addAll( Arrays.asList( StyleKeys.AUX_RAMP.getKeys() ) );
        keys.add( StyleKeys.SHADE_LOW );
        keys.add( StyleKeys.SHADE_HIGH );
        return keys.toArray( new ConfigKey<?>[ 0 ] );
    }

    /**
     * Interface for an object that can tweak settings in a way
     * that depends on on the way that both a plot zone and the
     * layers within that zone are configured.  Most of the rest of
     * the setup is done either within a zone or within a layer.
     */
    private static interface ZoneAdjuster {

        /**
         * Takes an existing list of settings corresponding to this
         * adjuster's plot zone, and modifies it in place as required.
         *
         * @param  zsettings  mutable list of settings objects
         *                    corresponding to the zone that this
         *                    adjuster is configured for
         */
        void adjustZoneSettings( List<Setting> zsettings );

        /**
         * Returns a list of additional settings for one of this adjuster's
         * layers.
         *
         * @param  lspec  specification for one of the layers that this
         *                adjuster is configured for
         * @param  lsettings  mutable list of settings objects
         *                    for the layer to be adjusted
         */
        void adjustLayerSettings( LayerSpec lspec, List<Setting> lsettings );
    }

    /**
     * Performs special processing to provide MatrixPlot-specific setttings.
     *
     * @param  plotSpec  plot specification
     * @return  global settings
     */
    private static List<Setting>
            getMatrixGlobalSettings( PlotSpec<?,?> plotSpec ) {

        /* Prepare required information from the plotspec. */
        PlotType<?,?> plotType = plotSpec.getPlotType();
        GangerFactory<?,?> gangerFact0 = plotType.getGangerFactory();
        SurfaceFactory<?,?> surfFact0 = plotType.getSurfaceFactory();
        if ( ! ( gangerFact0 instanceof MatrixGangerFactory ) ||
             ! ( surfFact0 instanceof PlaneSurfaceFactory ) ) {
            assert false;
            return Collections.emptyList();
        }
        MatrixGangerFactory gangerFact = (MatrixGangerFactory) gangerFact0;
        PlaneSurfaceFactory surfFact = (PlaneSurfaceFactory) surfFact0;
        Plotter<?>[] plotters = Arrays.stream( plotSpec.getLayerSpecs() )
                                      .map( LayerSpec::getPlotter )
                                      .toArray( n -> new Plotter<?>[ n ] );
        ConfigMap globalConfig = plotSpec.getGlobalConfig();
        ConfigMap[] zoneConfigs = plotSpec.getZoneConfigs();
        MatrixShape shape = gangerFact.getShape( globalConfig, plotters );
        int nc = shape.getWidth();
        int nz = shape.getCellCount();
        assert zoneConfigs.length == nz;
        String[] coordNames =
            IntStream.range( 0, nc )
                     .mapToObj( MatrixPlotType::getCoordName )
                     .toArray( n -> new String[ n ] );
        XyKeyPair<?>[] xyKeyPairs = surfFact.getXyKeyPairs();

        /* Go through each zone, extracting configuration information
         * specific to the X and Y axes.  These have to be converted to
         * settings with different names corresponding to the numbered
         * axes (X1, X2, ...) used by the matrix plot UI.
         * Each numbered axis with all its settings is encountered
         * multiple times; because of the way that the plots are set up,
         * the setting values ought to be the same each time,
         * so the settings (which in any case have to be single-valued)
         * are only stored the first time round is stored (in maps). */
        List<Map<String,Setting>> settingMaps = new ArrayList<>( nc );
        for ( int ic = 0; ic < nc; ic++ ) {
            settingMaps.add( new LinkedHashMap<String,Setting>() );
        }
        for ( int iz = 0; iz < nz; iz++ ) {
            ConfigMap zconfig = zoneConfigs[ iz ];
            MatrixShape.Cell cell = shape.getCell( iz );
            int ix = cell.getX();
            int iy = cell.getY();

            /* Avoid diagonal cells, which are histogram like.
             * Although one of the axes is a numbered coordinate in this case,
             * the other one is something else (like histogram count),
             * and its configuration shouldn't be applied to a numbered axis.
             * This does mean that the configuration of those histogram
             * axis is simply not recorded; there is currently no way
             * to specify these configuration items in the stilts UI.
             * We could try to identify the non-weird axis and use that only,
             * but the config for this should get picked up when looking at
             * one of the other cells.  Unless it's a very small matrix. */
            if ( ix != iy ) {
                for ( XyKeyPair<?> xyPair : xyKeyPairs ) {
                    String xname =
                        getSettingKey( xyPair.createKey( coordNames[ ix ] ) );
                    String yname =
                        getSettingKey( xyPair.createKey( coordNames[ iy ] ) );
                    Map<String,Setting> xmap = settingMaps.get( ix );
                    Map<String,Setting> ymap = settingMaps.get( iy );
                    if ( ! xmap.containsKey( xname ) ) {
                        xmap.put( xname,
                                  createNamedConfigSetting( xyPair.getKeyX(),
                                                            zconfig, xname ) );
                    }
                    if ( ! ymap.containsKey( yname ) ) {
                        ymap.put( yname,
                                  createNamedConfigSetting( xyPair.getKeyY(),
                                                            zconfig, yname ) );
                    }
                }
            }
        }

        /* Prepare result for return, grouped by coord index for
         * more comprehensible presentation. */
        List<Setting> settings = new ArrayList<>();
        for ( Map<String,Setting> settingMap : settingMaps ) {
            settings.addAll( settingMap.values() );
            settings.add( null );
        }

        /* Add global items from the profile keys, apart from the X/Y-specific
         * ones which we have taken care of in the previous step.
         * By special arrangement these are not extracted in other parts
         * of the configuration in the special case of the Matrix Plot. */
        List<ConfigKey<?>> profileKeyList =
            new ArrayList<>( Arrays.asList( surfFact.getProfileKeys() ) );
        for ( XyKeyPair<?> xyk : xyKeyPairs ) {
            profileKeyList.remove( xyk.getKeyX() );
            profileKeyList.remove( xyk.getKeyY() );
        }
        ConfigKey<?>[] profileKeys =
            profileKeyList.toArray( new ConfigKey<?>[ 0 ] );
        settings.add( null );
        settings.addAll( getConfigSettings( globalConfig, profileKeys ) );

        /* Return the result. */
        return settings;
    }

    /**
     * ZoneAdjuster that handles the interaction between sky systems
     * in sky plots; the per-zone view system and per-layer data systems.
     * The datasys settings should only be explicit (visible) if they
     * differ from the viewsys.
     * Only intended for use with SkyPlotType.
     */
    private static class SkySysZoneAdjuster implements ZoneAdjuster {

        private static final ConfigKey<SkySys> VIEWSYS_KEY =
            SkySurfaceFactory.VIEWSYS_KEY;
        private final SkySys viewsys_;
        private final boolean sysDiffers_;

        /**
         * Constructor.
         *
         * @param  zconfig  zone configuration
         * @param  lspecs  layer specifiers
         */
        SkySysZoneAdjuster( ConfigMap zconfig, List<LayerSpec> lspecs ) {
            viewsys_ = getExplicitValue( VIEWSYS_KEY, zconfig );
            boolean differs = false;
            for ( LayerSpec lspec : lspecs ) {
                ConfigMap lconfig = lspec.getConfig();
                for ( ConfigKey<?> key : lconfig.keySet() ) {
                    if ( key instanceof SkySysConfigKey ) {
                        SkySysConfigKey sysKey = (SkySysConfigKey) key;
                        if ( sysKey.isViewComparison() ) {
                            SkySys cmpsys = lconfig.get( sysKey );
                            if ( cmpsys != null &&
                                 ! cmpsys.equals( viewsys_ ) ) {
                                differs = true;
                            }
                        }
                    }
                }
            }
            sysDiffers_ = differs;
        }

        public void adjustZoneSettings( List<Setting> zsettings ) {
            for ( int i = 0; i < zsettings.size(); i++ ) {
                Setting zs = zsettings.get( i );
                if ( zs != null &&
                     zs.getKey().equals( getSettingKey( VIEWSYS_KEY ) ) ) {
                    String dflt = sysDiffers_ ? null : zs.getStringValue();
                    zsettings.set( i, zs.resetDefault( dflt ) );
                }
            }
        }

        public void adjustLayerSettings( LayerSpec lspec,
                                         List<Setting> lsettings ) {
            ConfigMap lconfig = lspec.getConfig();
            for ( ConfigKey<?> key : lconfig.keySet() ) {
                if ( key instanceof SkySysConfigKey ) {
                    SkySysConfigKey sysKey = (SkySysConfigKey) key;
                    if ( sysKey.isViewComparison() ) {
                        int isys = -1;
                        for ( int i = 0; i < lsettings.size(); i++ ) {
                            Setting ls = lsettings.get( i );
                            if ( ls != null &&
                                 ls.getKey().equals( getSettingKey( sysKey ))) {
                                isys = i;
                            }
                        }
                        if ( isys < 0 ) {
                            lsettings.add( createConfigSetting( sysKey,
                                                                lconfig ) );
                            isys = lsettings.size() - 1;
                        }
                        Setting sysSetting = lsettings.get( isys );
                        String dflt = sysDiffers_ ? null
                                                  : sysSetting.getStringValue();
                        lsettings.set( isys, sysSetting.resetDefault( dflt ) );
                    }
                }
            }
        }
    }
}
