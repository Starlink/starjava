package uk.ac.starlink.ttools.build;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.geom.HealpixDataGeom;
import uk.ac.starlink.ttools.plot2.layer.HealpixPlotter;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.plot2.task.LayerType;
import uk.ac.starlink.ttools.plot2.task.LayerTypeParameter;
import uk.ac.starlink.ttools.plot2.task.SimpleLayerType;
import uk.ac.starlink.ttools.plot2.task.ShapeFamilyLayerType;
import uk.ac.starlink.ttools.plot2.task.TypedPlot2Task;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Writes XML text documenting known plot2 LayerTypes.
 * Output is to standard output.
 * This class is designed to be used from its <code>main</code> method.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2014
 */
public class LayerTypeDoc {

    private final boolean basicXml_;
    private final String suffix_;
    private final Map<String,String> examplesMap_;
    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.build" );

    /**
     * Constructor.
     *
     * @param  basicXml  avoid XML constructs that won't show up in text output
     */
    public LayerTypeDoc( boolean basicXml ) {
        basicXml_ = basicXml;
        suffix_ = AbstractPlot2Task.EXAMPLE_LAYER_SUFFIX;
        examplesMap_ = Plot2Example.getExamplesXml();
    }

    /**
     * Returns an XML element giving full user documentation for a given
     * layer type.
     *
     * @param  layerType  layer type
     * @param  plotType   plot type with which this layer type is associated
     *                    if unique; otherwise null
     * @return  text of &lt;subsect&gt; element
     */
    public String getXmlDoc( LayerType layerType, PlotType<?,?> plotType ) {

        /* Get basic layer type information. */
        String lname = layerType.getName().toLowerCase();
        String layerId = "layer-" + lname;
        int npos = layerType.getPositionCount();
        Coord[] extraCoords = layerType.getExtraCoords();
        ConfigKey<?>[] styleKeys = layerType.getStyleKeys();

        /* Work out what groups of auxiliary parameters it has. */
        boolean hasPos = npos > 0;
        boolean hasExtra = extraCoords.length > 0;
        boolean hasStyle = styleKeys.length > 0;
        boolean hasShade = layerType instanceof ShapeFamilyLayerType;
        boolean hasTable = hasPos | hasExtra;

        /* Start output. */
        StringBuffer sbuf = new StringBuffer()
            .append( "<subsubsect id='" )
            .append( layerId )
            .append( "'>\n" )
            .append( "<subhead><title><code>" )
            .append( lname )
            .append( "</code></title></subhead>\n" );

        /* Description text. */
        sbuf.append( layerType.getXmlDescription() );

        /* Prepare lists of usage words and parameters. */
        List<String> usageWords = new ArrayList<String>();
        List<Parameter<?>> paramList = new ArrayList<Parameter<?>>();
        usageWords.add( AbstractPlot2Task.LAYER_PREFIX + suffix_
                      + "=" + lname );
        if ( hasStyle ) {
            Parameter<?>[] styleParams =
                LayerTypeParameter
               .getLayerConfigParams( styleKeys, suffix_, false );
            paramList.addAll( Arrays.asList( styleParams ) );
            usageWords.addAll( LayerTypeParameter.usageWords( styleParams ) );
        }
        if ( hasShade ) {
            Parameter<ShapeMode> shapeModeParam =
                ((ShapeFamilyLayerType) layerType)
               .createShapeModeParameter( suffix_ );
            paramList.add( shapeModeParam );
            usageWords.add( LayerTypeParameter.usageWord( shapeModeParam ) );
        }
        final String[] posPlaceholderWords;
        if ( hasPos ) {
            
            /* If we know what kind of plot it is, we can be specific about
             * the positional coordinates. */ 
            if ( plotType != null ) {
                final DataGeom geom;
                if ( layerType instanceof SimpleLayerType &&
                     (((SimpleLayerType) layerType).getPlotter())
                                         instanceof HealpixPlotter ) {
                    geom = HealpixDataGeom.DUMMY_INSTANCE;
                }
                else {
                    DataGeom[] geoms = plotType.getPointDataGeoms();
                    assert geoms.length == 1;
                    geom = geoms[ 0 ];
                }
                List<Parameter<?>> posParamList = new ArrayList<Parameter<?>>();
                for ( int ipos = 0; ipos < npos; ipos++ ) {
                    String posSuffix = npos == 1
                                     ? ""
                                     : PlotUtil.getIndexSuffix( ipos );
                    for ( Coord posCoord : geom.getPosCoords() ) {
                        String sfix = posSuffix + suffix_;
                        for ( Input input : posCoord.getInputs() ) {
                            posParamList.add( AbstractPlot2Task
                                             .createDataParameter( input, sfix,
                                                                   false ) );
                            if ( AbstractPlot2Task.hasDomainMappers( input ) ) {
                                posParamList.add( AbstractPlot2Task
                                                 .createDomainMapperParameter(
                                                      input, sfix ) );
                            }
                        }
                    }
                }
                paramList.addAll( posParamList );
                Parameter<?>[] posParams =
                    posParamList.toArray( new Parameter<?>[ 0 ] );
                usageWords.addAll( LayerTypeParameter.usageWords( posParams ) );
                posPlaceholderWords = new String[ 0 ];
            }

            /* Otherwise, just add placeholders to the usage list. */
            else {
                posPlaceholderWords = new String[ npos ];
                for ( int ipos = 0; ipos < npos; ipos++ ) {
                    String posSuffix = npos == 1
                                     ? ""
                                     : PlotUtil.getIndexSuffix( ipos );
                    posPlaceholderWords[ ipos ] = 
                        "<pos-coord-params" + posSuffix + suffix_ + ">";
                }
                usageWords.addAll( Arrays.asList( posPlaceholderWords ) );
            }
        }
        else {
            posPlaceholderWords = new String[ 0 ];
        }
        if ( hasExtra ) {
            Parameter<?>[] extraParams =
                LayerTypeParameter
               .getCoordParams( extraCoords, suffix_, false );
            paramList.addAll( Arrays.asList( extraParams ) );
            usageWords.addAll( LayerTypeParameter.usageWords( extraParams ) );
        }
        if ( hasTable ) {
            Parameter<?>[] tableParams =
                LayerTypeParameter.getInputParams( suffix_ );
            paramList.addAll( Arrays.asList( tableParams ) );
            usageWords.addAll( LayerTypeParameter.usageWords( tableParams ) );
        }

        /* Write usage summary. */
        sbuf.append( "<p>\n" )
            .append( "<strong>Usage Overview:</strong>\n" )
            .append( "<verbatim><![CDATA[\n" )
            .append( Formatter.formatWords( usageWords, 3 ) )
            .append( "]]></verbatim>\n" )
            .append( "</p>\n" )
            .append( "<p>All the parameters listed here\n" )
            .append( "affect only the relevant layer,\n" )
            .append( "identified by the suffix\n" )
            .append( "<code>" )
            .append( suffix_ )
            .append( "</code>.\n" )
            .append( "</p>\n" );

        /* If we have not been able to give the positional parameters
         * explicitly, explain how to supply them here. */
        int nppw = posPlaceholderWords.length;
        if ( nppw > 0 ) {
            sbuf.append( "<p><dl>\n" )
                .append( "<dt>Positional Coordinate Parameters:</dt>\n" );
            sbuf.append( "<dd>" )
                .append( "<p>The positional coordinates\n" );
            for ( int i = 0; i < nppw; i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( "<code><![CDATA[" )
                    .append( posPlaceholderWords[ i ] )
                    .append( "]]></code>\n" );
            }
            sbuf.append( "give " )
                .append( npos == 1 ? "a position " : ( npos + " positions " ) )
                .append( "for each row of the input table.\n" )
                .append( "Their form depends on the plot geometry,\n" )
                .append( "i.e. which plotting command is used.\n" )
                .append( "For a plane plot " )
                .append( "(<ref id='plot2plane'><code>plot2plane"
                                             + "</code></ref>)\n" )
                .append( "the parameters would be\n" );
            for ( int ipos = 0; ipos < npos; ipos++ ) {
                String posSuffix = npos == 1
                                 ? ""
                                 : PlotUtil.getIndexSuffix( ipos );
                String sfix = posSuffix + suffix_;
                boolean isMid = ipos < npos - 1;
                sbuf.append( "<code>x" )
                    .append( sfix )
                    .append( "</code>" )
                    .append( isMid ? ", " : " and " )
                    .append( "<code>y" )
                    .append( sfix )
                    .append( "</code>" )
                    .append( isMid ? "," : "." )
                    .append( "\n" );
            }
            sbuf.append( PlotUtil.concatLines( new String[] {
                "The coordinate parameter values are in all cases strings",
                "interpreted as numeric expressions based on column names.",
                "These can be column names, fixed values or algebraic",
                "expressions as described in <ref id='jel'/>.",
                "</p>",
            } ) );
            sbuf.append( "</dd>\n" )
                .append( "</dl>\n" )
                .append( "</p>\n" );
        }

        /* Add example. */
        String exname = "layer-" + lname;
        String exXml = examplesMap_.get( exname );
        if ( exXml != null ) {
            sbuf.append( "<p>\n" )
                .append( "<strong>Example:</strong>\n" )
                .append( "</p>\n" )
                .append( "<figure>\n" )
                .append( "<figureimage src='&FIG.plot2-" )
                .append( exname )
                .append( ";'/>\n" )
                .append( "</figure>\n" )
                .append( "<p>" )
                .append( exXml )
                .append( "</p>\n" );
        }
        else {
            logger_.severe( "No example figure for layer type " + lname );
        }

        /* Add detailed per-parameter usage list. */
        Collections.sort( paramList, Parameter.BY_NAME );
        if ( paramList.size() > 0 ) {
            sbuf.append( "<p><dl>\n" );
            for ( Parameter<?> param : paramList ) {
                sbuf.append( UsageWriter.xmlItem( param, layerId, basicXml_ ) )
                    .append( "\n" );
            }
            sbuf.append( "</dl></p>\n" );
        }

        /* End top-level element and return. */
        sbuf.append( "</subsubsect>\n" );
        return sbuf.toString();
    }

    /**
     * Returns a list of all the TypedPlot2Tasks known to Stilts.
     *
     * @return   plot tasks
     */
    public static TypedPlot2Task<?,?>[] getPlot2Tasks() throws LoadException {
        List<TypedPlot2Task<?,?>> plot2Tasks =
            new ArrayList<TypedPlot2Task<?,?>>();
        ObjectFactory<Task> taskFact = Stilts.getTaskFactory();
        for ( String nickname : taskFact.getNickNames() ) {
            Task task = taskFact.createObject( nickname );
            if ( task instanceof TypedPlot2Task ) {
                plot2Tasks.add( (TypedPlot2Task<?,?>) task );
            }
        }
        return plot2Tasks.toArray( new TypedPlot2Task<?,?>[ 0 ] );
    }

    /**
     * Returns an ordered list of all the LayerTypes used by known plot types.
     *
     * @return   known layer types
     */
    public static LayerType[] getLayerTypes() throws LoadException {
        Set<LayerType> ltypes = new LinkedHashSet<LayerType>();
        for ( TypedPlot2Task<?,?> task : getPlot2Tasks() ) {
            ltypes.addAll( getLayerTypes( task ).values() );
        }
        return ltypes.toArray( new LayerType[ 0 ] );
    }

    /**
     * Returns a map (keyed by name) of all the LayerTypes used by a given
     * task.
     *
     * @param   task  geometry-specific plot task
     * @return  name->layer type map for given task
     */
    private static Map<String,LayerType>
            getLayerTypes( TypedPlot2Task<?,?> task ) {
        LayerTypeParameter ltParam =
            AbstractPlot2Task
           .createLayerTypeParameter( "", task.getPlotContext() );
        Map<String,LayerType> typeMap = new LinkedHashMap<String,LayerType>();
        for ( LayerType ltype : ltParam.getOptions() ) {
            typeMap.put( ltype.getName(), ltype );
        }
        return typeMap;
    }

    /**
     * Returns XML text that can be used to reference a LayerType
     * description in the user document.
     *
     * @param   ltype  layer type
     * @return  &lt;ref&gt; element
     */
    public static String layerTypeRef( LayerType ltype ) {
        String ltname = ltype.getName().toLowerCase();
        return new StringBuffer()
            .append( "<ref id='layer-" )
            .append( ltname )
            .append( "' plaintextref='yes'><code>" )
            .append( ltname )
            .append( "</code></ref>" )
            .toString();
    }

    /**
     * Main method.  Try <code>-help</code>.
     */
    public static void main( String[] args ) throws LoadException {
        String usage = new StringBuffer()
           .append( "\n   " )
           .append( "Usage: " )
           .append( LayerTypeDoc.class.getName() )
           .append( " [-doc]" )
           .append( " [-basicxml]" )
           .append( " [<layer-name> ...]" )
           .append( "\n" )
           .toString();

        /* Parse arguments. */
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        boolean doc = false;
        boolean basicXml = false;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.equals( "-doc" ) ) {
                it.remove();
                doc = true;
            }
            if ( arg.equals( "-basicxml" ) ) {
                it.remove();
                basicXml = true;
            }
            if ( arg.startsWith( "-h" ) || arg.startsWith( "--h" ) ) {
                it.remove();
                System.err.println( usage );
                return;
            }
        }

        /* Get known layer types. */
        TypedPlot2Task<?,?>[] tasks = getPlot2Tasks();
        Map<String,LayerType> typeMap =
            new LinkedHashMap<String,LayerType>();
        Map<String,Set<TypedPlot2Task<?,?>>> taskMap =
            new LinkedHashMap<String,Set<TypedPlot2Task<?,?>>>();
        for ( TypedPlot2Task<?,?> task : tasks ) {
            for ( Map.Entry<String,LayerType> entry :
                  getLayerTypes( task ).entrySet() ) {
                String lname = entry.getKey();
                LayerType ltype = entry.getValue();

                /* Make a list of the tasks that can be used for each
                 * layer type.  This is so we can identify which layer
                 * types are specific to a single TypedPlot2Task. */
                if ( ! taskMap.containsKey( lname ) ) {
                    taskMap.put( lname, new HashSet<TypedPlot2Task<?,?>>() );
                }
                taskMap.get( lname ).add( task );

               /* Only insert into the name->type map if we haven't seen
                * name before; that doesn't affect the order of the
                * entries in the LinkedHashMap, but it does mean that
                * earlier-listed LayerTypes with the same name end
                * up in the map.  These are generally better choices
                * for generating generic documentation. */
                if ( ! typeMap.containsKey( lname ) ) {
                    typeMap.put( lname, ltype );
                }
            }
        }

        if ( argList.size() > 0 ) {
            typeMap.keySet().retainAll( argList );
        }

        /* Generate and print output. */
        PrintStream out = System.out;
        if ( doc ) {
            out.println( "<doc>" );
        }
        LayerTypeDoc doccer = new LayerTypeDoc( basicXml );
        for ( Map.Entry<String,LayerType> entry : typeMap.entrySet() ) {
            String lname = entry.getKey();
            LayerType ltype = entry.getValue();
            Set<TypedPlot2Task<?,?>> ltasks = taskMap.get( lname );
            PlotType<?,?> plotType = ltasks != null && ltasks.size() == 1
                                   ? ltasks.iterator().next().getPlotType()
                                   : null;
            out.println( doccer.getXmlDoc( ltype, plotType ) );
        }
        if ( doc ) {
            out.println( "</doc>" );
        }
    }
}
