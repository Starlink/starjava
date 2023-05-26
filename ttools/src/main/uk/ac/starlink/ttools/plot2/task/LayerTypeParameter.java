package uk.ac.starlink.ttools.plot2.task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.layer.ShapeForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapeModePlotter;
import uk.ac.starlink.ttools.task.ExtraParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.TableEnvironment;

/**
 * Parameter that specifies a LayerType to be used for a plot layer.
 * LayerTypes in some cases correspond to Plotters, and in some cases
 * to families of Plotters.
 *
 * <p>Most of the complication here is generating the auto-documentation.
 * It's not 100% obvious that code belongs here, but I can't think of
 * a better place to put it.
 *
 * @author   Mark Taylor
 * @since    3 Sep 2014
 */
public class LayerTypeParameter extends ChoiceParameter<LayerType>
                                implements ExtraParameter {

    private final String layerSuffix_;
    private final DataGeom[] geoms_;
    private final Parameter<?>[] geomParams_;

    /**
     * Constructor.
     *
     * @param   prefix  non-suffix part of this parameter's name
     * @param   suffix  layer-specific part of this parameter's name
     * @param   context  plot context
     */
    public LayerTypeParameter( String prefix, String suffix,
                               PlotContext<?,?> context ) {
        super( prefix + suffix,
               getLayerTypes( context.getPlotType().getPlotters() ) );
        layerSuffix_ = suffix;
        geoms_ = context.getExampleGeoms();
        geomParams_ = context.getGeomParameters( suffix );

        /* Construct usage string. */
        StringBuffer usage = new StringBuffer()
             .append( "<layer-type>" )
             .append( " " )
             .append( "<layer" )
             .append( layerSuffix_ )
             .append( "-specific-params>" );
        setUsage( usage.toString() );

        /* Other parameter documentation. */
        setPrompt( "Plot type for layer " + suffix );
        String osfix = "&lt;" + AbstractPlot2Task.EXAMPLE_LAYER_SUFFIX + "&gt;";
        StringBuffer obuf = new StringBuffer();
        for ( LayerType ltype : getOptions() ) {
            String lname = stringifyOption( ltype );
            obuf.append( "<li><code>" )
                .append( "<ref id='layer-" )
                .append( lname )
                .append( "' plaintextref='yes'>" )
                .append( lname )
                .append( "</ref>" )
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
            "<p>This parameter may take one of the following values,",
            "described in more detail in <ref id='LayerType'/>:",
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
                "(e.g. <code>" + ShapeFamilyLayerType.SHADING_PREFIX
                               + suffix + "</code>,",
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

    @Override
    public String stringifyOption( LayerType ltype ) {
        return ltype.getName().toLowerCase();
    }

    public String getExtraUsage( TableEnvironment env ) {
        boolean hasShades = getShapeLayerTypes().length > 0;

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
        for ( LayerType ltype : getOptions() ) {
            sbuf.append( '\n' );
            List<String> usageWords = new ArrayList<String>();
            usageWords.add( getName() + "=" + ltype.getName() );
            int npos = ltype.getCoordGroup().getBasicPositionCount();
            Coord[] extraCoords = ltype.getExtraCoords();
            boolean hasData = npos > 0 || extraCoords.length > 0;
            ConfigKey<?>[] styleKeys = ltype.getStyleKeys();
            boolean hasStyle = styleKeys.length > 0;
            if ( hasData ) {
                usageWords.add( tableUsage );
                usageWords.add( coordUsage );
            }
            if ( hasStyle ) {
                usageWords.add( styleUsage );
            }
            if ( hasShades ) {
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
                    coordWords.addAll(
                        usageWords( getCoordParams( posCoords, suffix,
                                                    false ) ) );
                }
                if ( npos > 0 ) {
                    for ( int i = 0; i < geomParams_.length; i++ ) {
                        coordWords.add( usageWord( geomParams_[ i ] ) );
                    }
                }
                coordWords.addAll(
                    usageWords( getCoordParams( extraCoords, layerSuffix_,
                                                false ) ) );
                sbuf.append( Formatter.formatWords( coordWords, 9 ) );
            }

            /* Style parameter usage. */
            if ( hasStyle ) {
                List<String> styleWords = new ArrayList<String>();
                styleWords.add( styleUsage + ":" );
                for ( Parameter<?> param :
                      ltype.getAssociatedParameters( layerSuffix_ ) ) {
                     styleWords.add( usageWord( param ) );
                }
                styleWords.addAll(
                    usageWords( getLayerConfigParams( styleKeys, layerSuffix_,
                                                      false ) ) );
                sbuf.append( Formatter.formatWords( styleWords, 9 ) );
            }
        }

        /* Table parameters. */
        sbuf.append( "\n   " )
            .append( tableUsage )
            .append( " (Table parameters):" )
            .append( "\n" );
        List<String> tuWords = new ArrayList<String>();
        tuWords.addAll( usageWords( getInputParams( layerSuffix_ ) ) );
        sbuf.append( Formatter.formatWords( tuWords, 6 ) );

        /* Shading (ShapeMode) parameters. */
        if ( hasShades ) {
            ShapeFamilyLayerType shapeType = getShapeLayerTypes()[ 0 ];
            ChoiceParameter<ShapeMode> shapemodeParam =
                shapeType.createShapeModeParameter( layerSuffix_ );
            sbuf.append( "\n   " )
                .append( shadeUsage )
                .append( " (Available shading types " )
                .append( "with associated parameters):" )
                .append( "\n" );

            /* <p>Note this assumes that there is only one set of ShapeModes,
             * i.e. the list of modes is the same for all known
             * ShapeModePlotters.  That's not guaranteed (or checked)
             * to be the case, but at time of writing it is. */
            for ( ShapeMode mode : shapemodeParam.getOptions() ) {
                List<String> modeWords = new ArrayList<String>();
                modeWords.add( shapemodeParam.getName() + "="
                             + mode.getModeName() );
                modeWords.addAll(
                    usageWords( getCoordParams( mode.getExtraCoords(),
                                                layerSuffix_, false ) ) );
                modeWords.addAll(
                    usageWords( getLayerConfigParams( mode.getConfigKeys(),
                                                      layerSuffix_, false ) ) );
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
     * Returns the list of shape-variant layer types avaiable from
     * this parameter.
     *
     * @return  list of ShapeFamily layer types, may be empty
     */
    private ShapeFamilyLayerType[] getShapeLayerTypes() {
        List<ShapeFamilyLayerType> shapeList =
            new ArrayList<ShapeFamilyLayerType>();
        for ( LayerType ltype : getOptions() ) {
            if ( ltype instanceof ShapeFamilyLayerType ) {
                shapeList.add( (ShapeFamilyLayerType) ltype );
            }
        }
        return shapeList.toArray( new ShapeFamilyLayerType[ 0 ] );
    }

    /**
     * Gets parameters used for specifying a table with
     * input data for a plot.
     *
     * @param  suffix  layer suffix
     * @return  input table parameters
     */
    public static Parameter<?>[] getInputParams( String suffix ) {
        InputTableParameter inParam =
            AbstractPlot2Task.createTableParameter( suffix );
        return new Parameter<?>[] {
            inParam,
            inParam.getFormatParameter(),
            inParam.getStreamParameter(),
            AbstractPlot2Task.createFilterParameter( suffix, inParam ),
        };
    }
            
    /**     
     * Gets parameters used for specifying coordinate values for a plot.
     *  
     * @param  coords  coordinates required
     * @param  suffix  layer suffix
     * @param  fullDetail  if true, extra detail is appended to the parameter
     *                     descriptions
     * @return  coord parameters
     */
    public static Parameter<?>[] getCoordParams( Coord[] coords, String suffix,
                                                 boolean fullDetail ) {
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
        for ( Coord coord : coords ) {
            for ( Input input : coord.getInputs() ) {
                Parameter<?> param =
                    AbstractPlot2Task
                   .createDataParameter( input, suffix, fullDetail );
                param.setNullPermitted( ! coord.isRequired() );
                paramList.add( param );
                if ( AbstractPlot2Task.hasDomainMappers( input ) ) {
                    paramList.add( AbstractPlot2Task
                                  .createDomainMapperParameter( input,
                                                                suffix ) );
                }
            }
        }
        return paramList.toArray( new Parameter<?>[ 0 ] );
    }

    /**
     * Gets parameters used for speciyfing ConfigKey-based values
     * for a plot.
     *
     * @param  configKeys  configuration keys
     * @param  suffix   layer suffix
     * @param  fullDetail  if true, extra detail is appended to the parameter
     *                     descriptions
     * @return  config parameters
     */
    public static Parameter<?>[]
            getLayerConfigParams( ConfigKey<?>[] configKeys, String suffix,
                                  boolean fullDetail ) {
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
        for ( ConfigKey<?> key : configKeys ) {
            paramList.add( ConfigParameter
                          .createLayerSuffixedParameter( key, suffix,
                                                         fullDetail ) );
        }
        return paramList.toArray( new Parameter<?>[ 0 ] );
    }

    /**
     * List of name=usage strings for a given set of parameters.
     *
     * @param  params  parameter list
     * @return  list of name=usage strings, one for each param
     */
    public static List<String> usageWords( Parameter<?>[] params ) {
        List<String> wordList = new ArrayList<String>( params.length );
        for ( Parameter<?> param : params ) {
            wordList.add( usageWord( param ) );
        }
        return wordList;
    }

    /**
     * Returns a "name=&lt;usage&gt;" string for a parameter.
     *
     * @param   param  parameter
     * @return  usage string
     */
    public static String usageWord( Parameter<?> param ) {
        return new StringBuffer()
              .append( param.getName() )
              .append( '=' )
              .append( param.getUsage() )
              .toString();
    }

    /**
     * Returns the list of LayerTypes represented by a given list of Plotters.
     *
     * @param  plotters  plotter list
     * @return  layer type list
     */
    public static LayerType[] getLayerTypes( Plotter<?>[] plotters ) {
        Map<ShapeForm,List<ShapeModePlotter>> shapePlotterMap =
            new LinkedHashMap<ShapeForm,List<ShapeModePlotter>>();
        List<LayerType> typeList = new ArrayList<LayerType>();
        for ( Plotter<?> plotter : plotters ) {
            if ( plotter instanceof ShapeModePlotter ) {
                ShapeModePlotter shapePlotter = (ShapeModePlotter) plotter;
                ShapeForm form = shapePlotter.getForm();
                if ( ! shapePlotterMap.containsKey( form ) ) {
                    List<ShapeModePlotter> modeList =
                        new ArrayList<ShapeModePlotter>();
                    typeList.add( new ShapeFamilyLayerType( form, modeList ) );
                    shapePlotterMap.put( form, modeList );
                }
                shapePlotterMap.get( form ).add( shapePlotter );
            }
            else {
                typeList.add( new SimpleLayerType( plotter ) );
            }
        }
        return typeList.toArray( new LayerType[ 0 ] );
    }
}
