package uk.ac.starlink.ttools.plot2.task;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.filter.SelectFilter;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.SkySysConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.SpherePlotType;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.geom.SkySurfaceFactory;
import uk.ac.starlink.ttools.plot2.geom.TimePlotType;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapeModePlotter;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.task.Credibility;
import uk.ac.starlink.ttools.task.CredibleString;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.MapEnvironment;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.SettingGroup;
import uk.ac.starlink.ttools.task.TableNamer;
import uk.ac.starlink.util.LoadException;


/**
 * Represents an abstract model of a STILTS command line.
 * A list of parameter-value pairs along with basic parameter
 * grouping information is reprented.
 * There is no guarantee that the contents of this object
 * will correspond to a STILTS command that can actually be executed,
 * so care must be taken in assembling it.
 *
 * <p>Use a {@link StiltsPlotFormatter} instance to export this object into a
 * useful external form, such as a shell command line.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2017
 */
public class StiltsPlot {

    private final AbstractPlot2Task task_;
    private final String taskName_;
    private final SettingGroup[] groups_;

    private static final Collection<String> autoFormatNames_ =
        getAutoFormatNames();

    /**
     * Constructor.
     *
     * @param  task  plot task
     * @param  taskName  name of the plot task as used by stilts command line
     * @param  groups   all name-value pairs specifying the configuration
     *                  of the task, grouped for cosmetic purposes
     */
    public StiltsPlot( AbstractPlot2Task task, String taskName,
                       SettingGroup[] groups ) {
        task_ = task;
        taskName_ = taskName;
        groups_ = groups;
    }

    /**
     * Returns the plot task corresponding to this object.
     *
     * @return  plot task object
     */
    public AbstractPlot2Task getTask() {
        return task_;
    }

    /**
     * Returns the name of this object's plot task, as used by the
     * stilts command line.
     *
     * @return  task name
     */
    public String getTaskName() {
        return taskName_;
    }

    /**
     * Returns an array of objects that together contain all the parameter
     * settings required to specify this task to stilts.
     * They are grouped for cosmetic purposes.
     *
     * @return  settings
     */
    public SettingGroup[] getGroups() {
        return groups_;
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
        return (AbstractPlot2Task)
               Stilts.getTaskFactory().createObject( taskName );
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
     * @param  formatter  defines details of how formatting will take place
     */
    public static <P,A> StiltsPlot createPlot( PlotSpec<P,A> plotSpec,
                                               StiltsPlotFormatter formatter )
            throws LoadException {
        PlotType<P,A> plotType = plotSpec.getPlotType();
        Dimension extSize = plotSpec.getExtSize();
        ZoneSpec[] zoneSpecs = plotSpec.getZoneSpecs();
        LayerSpec[] layerSpecs = plotSpec.getLayerSpecs();
        Suffixer zoneSuffixer = formatter.getZoneSuffixer();
        Suffixer layerSuffixer = formatter.getLayerSuffixer();
        TableNamer namer = formatter.getTableNamer();

        /* Work out which plot command to use. */
        String taskName = getPlotTaskName( plotType );
        AbstractPlot2Task task = createTask( taskName );

        /* Global settings for the task. */
        List<Setting> taskSettings = new ArrayList<Setting>();
        if ( extSize != null ) {
            Integer xp = new Integer( extSize.width );
            Integer yp = new Integer( extSize.height );
            taskSettings.add( createParamSetting( task.getXpixParameter(),
                                                  xp ) );
            taskSettings.add( createParamSetting( task.getYpixParameter(),
                                                  yp ) );
        }
        taskSettings.addAll( Arrays.asList( new Setting[] {
            createParamSetting( task.getPaddingParameter(),
                                plotSpec.getPadding() ),
        } ) );

        /* Plot surface settings. */
        SurfaceFactory<P,A> sfact = plotType.getSurfaceFactory();
        ConfigKey<?>[] profileKeys = sfact.getProfileKeys();
        ConfigKey<?>[] aspectKeys = sfact.getAspectKeys();
        ConfigKey<?>[] auxKeys = PlotUtil.arrayConcat(
            StyleKeys.AUX_RAMP.getKeys(),
            new ConfigKey<?>[] { StyleKeys.SHADE_LOW, StyleKeys.SHADE_HIGH }
        );
        Map<String,List<Setting>> zoneSettings =
            new LinkedHashMap<String,List<Setting>>();

        /* Zone suffixes. */
        int nz = zoneSpecs.length;
        String[] zkeys =
            zoneSuffixer.createSuffixes( nz ).toArray( new String[ 0 ] );

        /* Set up zone adjusters to deal with zone-layer interactions. */
        ZoneAdjuster[] zadjusters = new ZoneAdjuster[ nz ];
        for ( int iz = 0; iz < nz; iz++ ) {
            List<LayerSpec> lspecs = new ArrayList<LayerSpec>();
            for ( LayerSpec ls : layerSpecs ) {
                if ( ls.getZoneIndex() == iz ) {
                    lspecs.add( ls );
                }
            }
            zadjusters[ iz ] =
                createZoneAdjuster( zoneSpecs[ iz ], lspecs, plotType );
        }

        /* Per-zone settings. */
        for ( int iz = 0; iz < nz; iz++ ) {
            ZoneSpec zoneSpec = zoneSpecs[ iz ];
            ConfigMap zoneConfig = zoneSpec.getConfig();
            List<Setting> settings = new ArrayList<Setting>();
            settings.addAll( getConfigSettings( zoneConfig, profileKeys ) );
            settings.add( null );
            settings.addAll( getConfigSettings( zoneConfig, aspectKeys ) );
            settings.add( null );
            if ( zoneSpec.getHasAux() ) {
                ZoneSpec.RampSpec auxSpec = zoneSpec.getAuxSpec();
                settings.addAll( getConfigSettings( zoneConfig, auxKeys ) );
                settings.add( null );
                settings.add( createParamSetting(
                                  task.createAuxVisibleParameter( null),
                                  Boolean.valueOf( auxSpec != null ) ) );
                if ( auxSpec != null ) {
                    settings.add( createParamSetting(
                                      task.createAuxLabelParameter( null ),
                                      auxSpec.getLabel() ) );
                    settings.add( createParamSetting(
                                      task.createAuxCrowdParameter( null ),
                                      auxSpec.getCrowding() ) );
                }
                settings.add( null );
            }
            settings.add( createParamSetting( task.createTitleParameter( null ),
                                              zoneSpec.getTitle() ) );
            ZoneSpec.LegendSpec legSpec = zoneSpec.getLegendSpec();
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
            zadjusters[ iz ].adjustZoneSettings( settings );
            zoneSettings.put( zkeys[ iz ], settings );
        }

        /* Per-layer settings. */
        Map<String,String> layerTypes = new LinkedHashMap<String,String>();
        Map<String,List<Setting>> layerSettings =
            new LinkedHashMap<String,List<Setting>>();
        int nl = layerSpecs.length;
        Collection<String> legKeys = new ArrayList<String>( nl );
        List<String> lkeys = layerSuffixer.createSuffixes( nl );
        boolean excludeLegend = false;
        for ( int il = 0; il < nl; il++ ) {
            LayerSpec lspec = layerSpecs[ il ];
            int iz = lspec.getZoneIndex();
            ZoneSpec zoneSpec = zoneSpecs[ iz ];
            List<Setting> lsettings = new ArrayList<Setting>();

            /* Assign a layer suffix. */
            String lkey = lkeys.get( il );

            /* Zone identifier. */
            lsettings.add( new Setting( AbstractPlot2Task.ZONE_PREFIX,
                                        zkeys[ iz ], "" ) );
            lsettings.add( null );

            /* Work out the layer type and possibly associated shading type. */
            Plotter<?> plotter = lspec.getPlotter();
            final String ltype;
            List<Setting> modeSettings = new ArrayList<Setting>();
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
            lsettings.addAll( createInputTableSettings( lspec, namer ) );
            lsettings.add( null );

            /* Input data coordinate settings, if any. */
            lsettings.addAll( createCoordSettings( lspec ) );
            lsettings.add( null );

            /* Layer style configuration. */
            lsettings.addAll( modeSettings );
            lsettings.addAll( getConfigSettings( lspec.getConfig(),
                                                 plotter.getStyleKeys() ) );
            zadjusters[ iz ].adjustLayerSettings( lspec, lsettings );
            lsettings.add( null );

            /* Legend label, if any. */
            if ( zoneSpec.getLegendSpec() != null ) {
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
        List<Setting> trailSettings = new ArrayList<Setting>();
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
        List<SettingGroup> groups = new ArrayList<SettingGroup>();
        groups.addAll( toGroups( 1, taskSettings ) );
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
        return new StiltsPlot( task, taskName,
                               groups.toArray( new SettingGroup[ 0 ] ) );
    }

    /**
     * Returns the STILTS task name corresponding to a given plot type.
     *
     * @param  ptype  plot type
     * @return  stilts task name
     */
    private static String getPlotTaskName( PlotType<?,?> ptype ) {
        if ( ptype instanceof PlanePlotType ) {
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
     * For each non-null setting in a supplied list, appends a given
     * suffix to the key part.  Null entries are left untouched.
     *
     * @param  settings   input setting list
     * @param  suffix     suffix to append to each setting key
     * @return  output setting list
     */
    private static List<Setting> addSuffixes( List<Setting> settings,
                                              String suffix ) {
        List<Setting> outList = new ArrayList<Setting>( settings.size() );
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
        List<Setting> common = new ArrayList<Setting>();
        if ( lists.size() > 1 ) {
            Set<String> allkeys = new LinkedHashSet<String>();
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
     * Quotes a string as required for use as one of the arguments within
     * a STILTS filter command.
     *
     * @param  txt  string to quote
     * @return   text suitable for use within a stilts parameter value;
     *           some quoting may have been added if required
     */
    private static String argQuote( String txt ) {
        boolean hasSquot = txt.indexOf( '\'' ) >= 0;
        boolean hasDquot = txt.indexOf( '"' ) >= 0;
        boolean hasSpace = txt.indexOf( ' ' ) >= 0;
        if ( hasSquot || hasDquot || hasSpace ) {
            return "\"" + txt.replaceAll( "\"", "\\\\\"" ) + "\"";
        }
        else {
            return txt;
        }
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
        List<Setting> settings = new ArrayList<Setting>( nk );
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
        return new Setting( getSettingKey( key ),
                            key.valueToString( map.get( key ) ),
                            key.valueToString( key.getDefaultValue() ) );
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
     * Creates a Setting corresponding to a given task parameter.
     *
     * @param   param  task parameter
     * @param   tval   typed value for parameter
     * @return   setting object
     */
    private static <T> Setting createParamSetting( Parameter<T> param,
                                                   T tval ) {
        String key = param.getName();
        String value;
        try {
            value = param.objectToString( new MapEnvironment(), tval );
        }
        catch ( TaskException e ) {
            assert false;
            throw new RuntimeException();
        }
        String dflt = param.getStringDefault();
        return new Setting( key, value, dflt );
    }

    /**
     * Creates a Setting corresponding to a given task parameter,
     * set to its default value.
     *
     * @param   param  task parameter
     * @return   setting object
     */
    private static Setting createDefaultParamSetting( Parameter<?> param ) {
        String dflt = param.getStringDefault();
        return new Setting( param.getName(), dflt, dflt );
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
        List<Setting> settings = new ArrayList<Setting>();
        StarTable table = lspec.getTable();
        String suffix = "";
        if ( table != null ) {

            /* Table name. */
            InputTableParameter inParam =
                AbstractPlot2Task.createTableParameter( suffix );
            CredibleString naming = namer.nameTable( table );
            Credibility nameCred = naming.getCredibility();
            Setting tableSetting =
                new Setting( inParam.getName(), naming.getValue(), null );
            tableSetting.setObjectValue( table );
            tableSetting.setCredibility( nameCred );
            settings.add( tableSetting );

            /* Input format. */
            if ( nameCred == Credibility.YES ||
                 nameCred == Credibility.MAYBE ) {
                Parameter<String> fmtParam = inParam.getFormatParameter();
                TableBuilder tfmt = namer.getTableFormat( table );
                final Setting tfmtSetting;
                if ( tfmt != null ) {
                    String fmtName = tfmt.getFormatName();
                    tfmtSetting = autoFormatNames_.contains( fmtName )
                                ? createDefaultParamSetting( fmtParam )
                                : createParamSetting( fmtParam, fmtName );
                }
                else {
                    tfmtSetting = createDefaultParamSetting( fmtParam );
                    tfmtSetting.setCredibility( Credibility.MAYBE );
                }
                settings.add( tfmtSetting );
            }

            /* Row selection. */
            CredibleString selection = lspec.getSelectExpr();
            if ( selection != null ) {
                FilterParameter filterParam =
                    AbstractPlot2Task.createFilterParameter( suffix, inParam );
                String filterCmd = new SelectFilter().getName()
                                 + " "
                                 + argQuote( selection.getValue() );
                Setting selectSetting =
                    new Setting( filterParam.getName(), filterCmd, null );
                selectSetting.setCredibility( selection.getCredibility() );
                settings.add( selectSetting );
            }
        }
        return settings;
    }

    /**
     * Returns a list of settings corresponding to the coordinates
     * used by a given layer specification.
     *
     * @param   lspec  layer specification
     * @return   list of zero or more setting objects
     */
    private static List<Setting> createCoordSettings( LayerSpec lspec ) {
        List<Setting> settings = new ArrayList<Setting>();

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
     * @param   zspec   specification for a zone
     * @param   lspecs  array of specifications for all the layers that
     *                  appear within the given zone
     * @param  plotType  plot type
     * @return  appropriate zone adjuster, not null (but may be a noop)
     */
    private static ZoneAdjuster createZoneAdjuster( ZoneSpec zspec,
                                                    List<LayerSpec> lspecs,
                                                    PlotType<?,?> plotType ) {
        if ( plotType instanceof SkyPlotType ) {
            return new SkySysZoneAdjuster( zspec, lspecs );
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
     * Groups a list of settings into zero or more SettingGroups.
     * A group is created for each non-empty run of Settings in the
     * input list that does not contain a null value; nulls are
     * effectively recognised as group terminators.
     *
     * @param  level  level for all returned groups
     * @param  settings  input list of settings
     * @return   list of groups containing all the input settings
     */
    private static List<SettingGroup> toGroups( int level,
                                                List<Setting> settings ) {
        List<Setting> inList = new ArrayList<Setting>( settings );
        inList.add( null );
        List<SettingGroup> glist = new ArrayList<SettingGroup>();
        List<Setting> slist = new ArrayList<Setting>();
        for ( Setting s : inList ) {
            if ( s != null ) {
                slist.add( s );
            }
            else if ( slist.size() > 0 ) {
                Setting[] line = slist.toArray( new Setting[ 0 ] );
                glist.add( new SettingGroup( level, line ) );
                slist = new ArrayList<Setting>();
            }
        }
        return glist;
    }

    /**
     * Returns the list of table input handlers that correspond to
     * input formats which can be auto-detected.
     * For these, the default "ifmt=(auto)" setting will work.
     *
     * @return  auto-detected format name list
     */
    private static Collection<String> getAutoFormatNames() {
        Collection<String> list = new HashSet<String>();
        for ( Object obj : new StarTableFactory().getDefaultBuilders() ) {
            if ( obj instanceof TableBuilder ) {
                list.add( ((TableBuilder) obj).getFormatName() );
            }
            else {
                assert false;
            }
        }
        return list;
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
         * @param  zspec  zone specifier
         * @param  lspecs  layer specifiers
         */
        SkySysZoneAdjuster( ZoneSpec zspec, List<LayerSpec> lspecs ) {
            ConfigMap zconfig = zspec.getConfig();
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
