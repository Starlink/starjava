package uk.ac.starlink.ttools.plot2.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter.ShapeModePlotter;
import uk.ac.starlink.ttools.task.ExtraParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.TableEnvironment;

/**
 * Parameter that specifies the Plotter to be used for a plot layer.
 *
 * <p>For general plotters, the string value of the parameter is used
 * to identify a Plotter known by this parameter in a straightforward way.
 * Plotters that subclass
 * {@link uk.ac.starlink.ttools.plot2.layer.ShapePlotter.ShapeModePlotter}
 * are treated specially.  In this case the string value of the
 * parameter gives the ShapeForm, and the ShapeMode is given by
 * an auxiliary parameter (shading).
 * This is somewhat confusing from the point of view of how this
 * parameter relates to Plotter values, but it makes it more
 * straightforward for users (and in particular parameter documentation),
 * since it keeps the number of possible string values of this parameter
 * to a manageable size.
 *
 * <p>However, if this parameter is used programmatically, you can
 * assign a Plotter instance to it.
 *
 * <p>Most of the complication here is generating the auto-documentation.
 * It's not 100% obvious that code belongs here, but I can't think of
 * a better place to put it.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2014
 */
public class PlotterParameter extends Parameter<Plotter>
                              implements ExtraParameter {

    private final String layerSuffix_;
    private final DataGeom[] geoms_;
    private final Parameter[] geomParams_;
    private final List<Plotter> singlePlotterList_;
    private final Map<ShapeForm,List<ShapeModePlotter>> shapePlotterMap_;
    private final ShapeModeParameter shapemodeParam_;
    public static final String SHADING_PREFIX = "shading";

    /**
     * Constructor.
     *
     * @param   prefix  non-suffix part of this parameter's name
     * @param   suffix  layer-specific part of this parameter's name
     * @param   context  plot context
     */
    public PlotterParameter( String prefix, String suffix,
                             PlotContext context ) {
        super( prefix + suffix, Plotter.class, true );
        layerSuffix_ = suffix;
        geoms_ = context.getExampleGeoms();
        geomParams_ = context.getGeomParameters( suffix );
        Plotter[] plotters = context.getPlotType().getPlotters();
        List<String> names = new ArrayList<String>();

        /* Divide up the supplied plotters into those which constitute
         * mode/form families, and standalone ones. */
        singlePlotterList_ = new ArrayList<Plotter>();
        shapePlotterMap_ =
            new LinkedHashMap<ShapeForm,List<ShapeModePlotter>>();
        for ( int ip = 0; ip < plotters.length; ip++ ) { 
            Plotter plotter = plotters[ ip ];
            final String name;
            if ( plotter instanceof ShapeModePlotter ) {
                ShapeModePlotter shapePlotter = (ShapeModePlotter) plotter;
                ShapeForm form = shapePlotter.getForm();
                if ( ! shapePlotterMap_.containsKey( form ) ) {
                    shapePlotterMap_.put( form,
                                          new ArrayList<ShapeModePlotter>() );
                }
                shapePlotterMap_.get( form ).add( shapePlotter );
                name = form.getFormName().toLowerCase();
            }
            else {
                singlePlotterList_.add( plotter );
                name = plotter.getPlotterName().toLowerCase();
            }
            if ( ! names.contains( name ) ) {
                names.add( name );
            }
        }

        /* If we have some shape plotters, use an associated parameter to
         * select the ShapeMode (shading), while this parameter will select
         * the ShapeForm. */
        if ( shapePlotterMap_.keySet().size() > 0 ) {
            shapemodeParam_ = new ShapeModeParameter( SHADING_PREFIX + suffix );
            shapemodeParam_.configurePlotters( shapePlotterMap_.entrySet()
                                              .iterator().next().getValue() );
        }
        else {
            shapemodeParam_ = null;
        }

        /* Construct usage string. */
        StringBuffer usage = new StringBuffer()
             .append( "<layer-type>" )
             .append( " " )
             .append( "<layer" )
             .append( layerSuffix_ )
             .append( "-specific-params>" );
        setUsage( usage.toString() );

        setPrompt( "Plot type for layer " + suffix );
        String osfix = "&lt;" + AbstractPlot2Task.EXAMPLE_LAYER_SUFFIX + "&gt;";
        StringBuffer obuf = new StringBuffer();
        for ( ShapeForm form : shapePlotterMap_.keySet() ) {
            obuf.append( "<li><code>" )
                .append( form.getFormName().toLowerCase() )
                .append( "</code></li>\n" );
        }
        for ( Plotter plotter : singlePlotterList_ ) {
            obuf.append( "<li><code>" )
                .append( plotter.getPlotterName().toLowerCase() )
                .append( "</code></li>\n" );
        }
        String optlist = obuf.toString();
        setDescription( new String[] {
            "<p>Selects one of the available plot types",
            "for layer" + suffix + ".",
            "A plot consists of a plotting surface,",
            "set up using the various unsuffixed parameters",
            "of the plotting command,",
            "and zero or more plot layers.",
            "Each layer is introduced by a parameter with the name",
            "<code>" + prefix + osfix + "</code>",
            "where the suffix \"<code>" + osfix + "</code>\"",
            "is a label identifying the layer",
            "and is appended to all the parameter names",
            "which configure that layer.",
            "Suffixes may be any string, including the empty string.",
            "</p>",
            "<p>This parameter may take one of the following values:",
            "<ul>",
            optlist,
            "</ul>",
            "</p>",
            "<p>Each of these layer types comes with a list of type-specific",
            "parameters to define the details of that layer,",
            "including some or all of the following groups:",
            "<ul>",
            "<li>input table parameters",
                "(e.g. <code>in" + suffix + "</code>,",
                      "<code>icmd" + suffix + "</code>)</li>",
            "<li>coordinate params referring to input table columns",
                "(e.g. <code>x" + suffix + "</code>,",
                      "<code>y" + suffix + "</code>)</li>",
            "<li>layer style parameters",
                "(e.g. <code>" + SHADING_PREFIX + suffix + "</code>,",
                      "<code>color" + suffix + "</code>)</li>",
            "</ul>",
            "</p>",
            "<p>Every parameter notionally carries the same suffix",
            "<code>" + suffix + "</code>.",
            "However, if the suffix is not present,",
            "the application will try looking for a parameter with the",
            "same name with no suffix instead.",
            "In this way, if several layers have the same value for a given",
            "parameter (for instance input table),",
            "you can supply it using one unsuffixed parameter",
            "to save having to supply several parameters with the same",
            "value but different suffixes.",
            "</p>",
        } );
    }

    public Plotter stringToObject( Environment env, String sval )
            throws TaskException {

        /* Check if it's the name of a shape form. */
        for ( ShapeForm form : shapePlotterMap_.keySet() ) {
            if ( sval.equalsIgnoreCase( form.getFormName() ) ) {
                shapemodeParam_.configurePlotters( shapePlotterMap_
                                                  .get( form ) );
                return shapemodeParam_.plotterValue( env );
            }
        }

        /* Check if it's the name of a non-shape plotter. */
        for ( Plotter plotter : singlePlotterList_ ) {
            if ( sval.equalsIgnoreCase( plotter.getPlotterName() ) ) {
                if ( shapemodeParam_ != null ) {
                    shapemodeParam_.configurePlotters( null );
                }
                return plotter;
            }
        }

        /* If not, it's unknown. */
        throw new ParameterValueException( this, "Unknown layer type" );
    }

    public String objectToString( Environment env, Plotter plotter )
            throws TaskException {

        /* If it's a ShapeModePlotter, use the ShapeForm name and
         * set the value of the associted shading parameter to the plotter's
         * ShapeMode. */
        if ( plotter instanceof ShapeModePlotter ) {
            ShapeModePlotter shapePlotter = (ShapeModePlotter) plotter;
            String fname = shapePlotter.getForm().getFormName();
            String mname = shapePlotter.getMode().getModeName();
            for ( ShapeForm sf : shapePlotterMap_.keySet() ) {
                if ( sf.getFormName().equalsIgnoreCase( fname ) ) {
                    for ( ShapeModePlotter sp : shapePlotterMap_.get( sf ) ) {
                        if ( sp.getMode().getModeName()
                               .equalsIgnoreCase( mname ) ) {
                            shapemodeParam_.configurePlotters( shapePlotterMap_
                                                              .get( sf ) );
                            shapemodeParam_.setValueFromString( env, mname );
                            return fname;
                        }
                    }
                }
            }
        }

        /* Otherwise, just set use the plotter name. */
        else {
            String pname = plotter.getPlotterName();
            for ( Plotter sp : singlePlotterList_ ) {
                if ( sp.getPlotterName().equalsIgnoreCase( pname ) ) {
                    return pname;
                }
            }
        }

        /* If we haven't seen this plotter try just returning its class. */
        return plotter.getClass().getName();
    }

    public String getExtraUsage( TableEnvironment env ) {
        boolean hasShades = shapePlotterMap_.keySet().size() > 0;

        /* Generic usage. */
        String tableUsage = "<table-params" + layerSuffix_ + ">";
        String coordUsage = "<coord-params" + layerSuffix_ + ">";
        String styleUsage = "<style-params" + layerSuffix_ + ">";
        String shadeUsage = "<shade-params" + layerSuffix_ + ">";
        StringBuffer sbuf = new StringBuffer();
        List<String> uwords = new ArrayList<String>();
        uwords.add( getName() + "=<layer-type>" );
        uwords.add( tableUsage );
        uwords.add( coordUsage );
        uwords.add( styleUsage );
        if ( hasShades ) {
            uwords.add( shadeUsage );
        }
        sbuf.append( "   Specification of layer N" )
            .append( " (with N any string)" )
            .append( " takes the form:" )
            .append( "\n" )
            .append( Formatter.formatWords( uwords, 6 ) );

        /* To show parameter usage we need to use some DataGeom, since that
         * affects what coordinate parameters are used.  Just pick the first
         * one (defined to be exemplary) but it does mean in general that
         * the usage generated here may not be representative for using
         * alternative DataGeoms, if any are permitted. */
        DataGeom geom = geoms_[ 0 ];

        /* Add detail for each known plotter. */
        sbuf.append( "\n   Available layer types" )
            .append( " with associated parameters:\n" );
        List<Plotter> plotterList = new ArrayList<Plotter>();
        for ( List<ShapeModePlotter> plist : shapePlotterMap_.values() ) {
            plotterList.add( plist.get( 0 ) );
        }
        plotterList.addAll( singlePlotterList_ );
        for ( Plotter plotter : plotterList ) {
            boolean hasShape = plotter instanceof ShapeModePlotter;
            String sval = hasShape 
                        ? ((ShapeModePlotter) plotter).getForm().getFormName()
                        : plotter.getPlotterName();
            sbuf.append( '\n' );
            List<String> usageWords = new ArrayList<String>();
            usageWords.add( getName() + "=" + sval.toLowerCase() );
            int npos = plotter.getCoordGroup().getPositionCount();
            Coord[] extraCoords = getNonModeExtraCoords( plotter );
            boolean hasData = npos > 0 || extraCoords.length > 0;
            ConfigKey[] styleKeys = getNonModeStyleKeys( plotter );
            boolean hasStyle = styleKeys.length > 0;
            if ( hasData ) {
                usageWords.add( tableUsage );
                usageWords.add( coordUsage );
            }
            if ( hasStyle ) {
                usageWords.add( styleUsage );
            }
            if ( hasShape ) {
                usageWords.add( shadeUsage );
            }
            sbuf.append( Formatter.formatWords( usageWords, 6 ) );

            /* Positional and extra coordinate parameter usage. */
            if ( hasData ) {
                List<String> coordWords = new ArrayList<String>();
                coordWords.add( coordUsage + ":" );
                Coord[] posCoords = geom.getPosCoords();
                for ( int ipos = 0; ipos < npos; ipos++ ) {
                    String suffix = layerSuffix_;
                    if ( npos > 1 ) {
                        suffix = PlotUtil.getIndexSuffix( ipos ) + suffix;
                    }
                    coordWords.addAll( getCoordsUsage( posCoords, suffix ) );
                }
                if ( npos > 0 ) {
                    for ( int i = 0; i < geomParams_.length; i++ ) {
                        coordWords.add( usageWord( geomParams_[ i ] ) );
                    }
                }
                coordWords.addAll( getCoordsUsage( extraCoords,
                                                   layerSuffix_ ) );
                sbuf.append( Formatter.formatWords( coordWords, 9 ) );
            }

            /* Style parameter usage. */
            if ( hasStyle ) {
                List<String> styleWords = new ArrayList<String>();
                styleWords.add( styleUsage + ":" );
                if ( hasShape ) {
                    ShapeModePlotter sp = (ShapeModePlotter) plotter;
                    shapemodeParam_.configurePlotters( shapePlotterMap_
                                                      .get( sp.getForm() ) );
                    styleWords.add( usageWord( shapemodeParam_ ) );
                }
                styleWords.addAll( getConfigUsage( styleKeys, layerSuffix_ ) );
                sbuf.append( Formatter.formatWords( styleWords, 9 ) );
            }
        }

        /* Table parameters. */
        sbuf.append( "\n   " )
            .append( tableUsage )
            .append( " (Table parameters):" )
            .append( "\n" );
        List<String> tuWords = new ArrayList<String>();
        tuWords.addAll( getInputUsage( layerSuffix_ ) );
        sbuf.append( Formatter.formatWords( tuWords, 6 ) );

        /* Shading (ShapeMode) parameters. */
        if ( hasShades ) {
            sbuf.append( "\n   " )
                .append( shadeUsage )
                .append( " (Available shading types " )
                .append( "with associated parameters):" )
                .append( "\n" );
            ShapeMode[] modes = getShapeModes();
            for ( int im = 0; im < modes.length; im++ ) {
                ShapeMode mode = modes[ im ];
                List<String> modeWords = new ArrayList<String>();
                modeWords.add( shapemodeParam_.getName() + "="
                             + mode.getModeName() );
                modeWords.addAll( getCoordsUsage( mode.getExtraCoords(),
                                                  layerSuffix_ ) );
                modeWords.addAll( getConfigUsage( mode.getConfigKeys(),
                                                  layerSuffix_ ) );
                sbuf.append( Formatter.formatWords( modeWords, 6 ) );
            }
        }

        /* Note that if there is a choice of geoms for this plot type,
         * the positional parameters might not look quite like their
         * usages as listed above.  This is not entirely satisfactory. */
        if ( geoms_.length > 1 ) {
            sbuf.append( "Coordinate variables may differ by geometry.\n" );
        }
        return sbuf.toString();
    }

    /**
     * Gets the ShapeModes known by this parameter.
     *
     * <p>Note this assumes that there is only one set of ShapeModes,
     * i.e. the list of modes is the same for all known ShapeModePlotters.
     * That's not guaranteed (or checked) to be the case, but at present
     * it is.
     *
     * @return  known shape modes for use with shape mode plotters
     */
    private ShapeMode[] getShapeModes() {
        ShapeModeParameter modeParam = new ShapeModeParameter( "dummy" );

        /* Just pick the list from the first ShapeModePlotter we have,
         * and hope it's representative. */
        modeParam.configurePlotters( shapePlotterMap_.entrySet()
                                    .iterator().next().getValue() );
        return modeParam.getOptions();
    }

    /**
     * Generates parameter usage for specifying a table with
     * input data for a plot.
     *
     * @param  suffix  layer suffix
     * @param  words giving input table parameter usage
     */
    private static List<String> getInputUsage( String suffix ) {
        InputTableParameter inParam =
            AbstractPlot2Task.createTableParameter( suffix );
        Parameter fmtParam = inParam.getFormatParameter();
        Parameter istrmParam = inParam.getStreamParameter();
        Parameter filterParam =
            AbstractPlot2Task.createFilterParameter( suffix );
        List<String> wordList = new ArrayList<String>();
        wordList.add( usageWord( inParam ) );
        wordList.add( usageWord( fmtParam ) );
        wordList.add( usageWord( istrmParam ) );
        wordList.add( usageWord( filterParam ) );
        return wordList;
    }

    /**
     * Generates parameter usage for specifying coordinate values for a plot.
     * 
     * @param  coords  coordinates required
     * @param  suffix  layer suffix
     * @return  words giving coord parameter usage
     */
    private static List<String> getCoordsUsage( Coord[] coords,
                                                String suffix ) {
        List<String> wordList = new ArrayList<String>();
        for ( int ic = 0; ic < coords.length; ic++ ) {
            Coord coord = coords[ ic ];
            ValueInfo[] infos = coord.getUserInfos();
            int nuc = infos.length;
            for ( int iuc = 0; iuc < nuc; iuc++ ) {
                Parameter param = AbstractPlot2Task
                                 .createDataParameter( infos[ iuc ], suffix );
                param.setNullPermitted( ! coord.isRequired() );
                wordList.add( usageWord( param ) );
            }
        }
        return wordList;
    }

    /**
     * Generates parameter usage for speciyfing ConfigKey-based parameters
     * for a plot.
     *
     * @param  configKeys  configuration keys
     * @param  suffix   layer suffix
     * @return  words giving config parameter usage
     */
    private static List<String> getConfigUsage( ConfigKey[] configKeys,
                                                String suffix ) {
        List<String> wordList = new ArrayList<String>();
        for ( int ik = 0; ik < configKeys.length; ik++ ) {
            ConfigParameter param = ConfigParameter
                                   .createSuffixedParameter( configKeys[ ik ],
                                                             suffix );
            wordList.add( usageWord( param ) );
        }
        return wordList;
    }

    /**
     * Returns a "name=<usage>" string for a parameter.
     *
     * @param   param  parameter
     * @return  usage string
     */
    private static String usageWord( Parameter param ) {
        return param.getName() + "=" + param.getUsage();
    }

    /**
     * Returns the style keys associated with a plotter excluding any
     * associated with a ShapeMode.
     *
     * @param  plotter  plotter
     * @return  config keys
     */
    private ConfigKey[] getNonModeStyleKeys( Plotter plotter ) {
        List<ConfigKey> keyList =
            new ArrayList<ConfigKey>( Arrays.asList( plotter.getStyleKeys() ) );
        if ( plotter instanceof ShapeModePlotter ) {
            ShapeMode mode = ((ShapeModePlotter) plotter).getMode();
            keyList.removeAll( Arrays.asList( mode.getConfigKeys() ) );
        }
        return keyList.toArray( new ConfigKey[ 0 ] );
    }

    /**
     * Returns the non-positional coordinate specifiers associated with
     * a plotter excluding any associated with a ShapeMode.
     *
     * @param  plotter  plotter
     * @return  coords
     */
    private Coord[] getNonModeExtraCoords( Plotter plotter ) {
        List<Coord> coordList = 
            new ArrayList<Coord>( Arrays.asList( plotter.getCoordGroup()
                                                        .getExtraCoords() ) );
        if ( plotter instanceof ShapeModePlotter ) {
            ShapeMode mode = ((ShapeModePlotter) plotter).getMode();
            coordList.removeAll( Arrays.asList( mode.getExtraCoords() ) );
        }
        return coordList.toArray( new Coord[ 0 ] );
    }

    /**
     * Parameter used to specify shading (ShapeMode) for a ShapeModePlotter.
     */
    private static class ShapeModeParameter extends ChoiceParameter<ShapeMode> {
        private Map<ShapeMode,ShapeModePlotter> plotterMap_;

        /**
         * Constructor.
         *
         * @param  name  parameter name
         */
        ShapeModeParameter( String name ) {
            super( name, ShapeMode.class );
        }

        /**
         * Sets the options for this parameter from a list of ShapeModePlotters.
         * The ShapeMode is extracted from each one.
         *
         * @param  plotters  shapemodeplotter list
         */
        void configurePlotters( List<ShapeModePlotter> plotters ) {
            clearOptions();
            if ( plotters == null || plotters.size() == 0 ) {
                plotterMap_ = null;
                setNullPermitted( true );
                setDefaultOption( null );
            }
            else {
                plotterMap_ = new LinkedHashMap<ShapeMode,ShapeModePlotter>();
                for ( ShapeModePlotter plotter : plotters ) {
                    ShapeMode mode = plotter.getMode();
                    addOption( mode );
                    plotterMap_.put( mode, plotter );
                }
                setNullPermitted( false );
                setDefaultOption( getOptions()[ 0 ] );
            }
        }

        /**
         * Returns the value of this parameter as a Plotter.
         * It requires that the configurePlotters was called with a suitable
         * list earlier.
         * 
         * @param  env  execution environment
         * @return  plotter  
         */
        ShapeModePlotter plotterValue( Environment env ) throws TaskException {
            return plotterMap_.get( objectValue( env ) );
        }   

        @Override
        public String stringifyOption( ShapeMode mode ) {
            return mode.getModeName().toLowerCase();
        }
    }
}
