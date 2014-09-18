package uk.ac.starlink.ttools.build;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.task.ChoiceParameter;
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
import uk.ac.starlink.ttools.plot2.geom.CubePlotType;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.geom.SkyPlotType;
import uk.ac.starlink.ttools.plot2.geom.TimePlotType;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.plot2.task.LayerType;
import uk.ac.starlink.ttools.plot2.task.LayerTypeParameter;
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
    private final String posUsage_;
    private final String extraUsage_;
    private final String styleUsage_;
    private final String shadeUsage_;
    private final String tableUsage_;

    /**
     * Constructor.
     *
     * @param  basicXml  avoid XML constructs that won't show up in text output
     */
    public LayerTypeDoc( boolean basicXml ) {
        basicXml_ = basicXml;
        suffix_ = AbstractPlot2Task.EXAMPLE_LAYER_SUFFIX;
        posUsage_ = "<pos-coord-params" + suffix_ + ">";
        extraUsage_ = "<extra-coord-params" + suffix_ + ">";
        styleUsage_ = "<style-params" + suffix_ + ">";
        shadeUsage_ = "<shade-params" + suffix_ + ">";
        tableUsage_ = "<table-params" + suffix_ + ">";
    }

    /**
     * Returns an XML element giving full user documentation for a given
     * layer type.
     *
     * @param  layerType  layer type
     * @return  text of &lt;subsect&gt; element
     */
    public String getXmlDoc( LayerType layerType ) {

        /* Get basic layer type information. */
        String lname = layerType.getName().toLowerCase();
        int npos = layerType.getPositionCount();
        Coord[] extraCoords = layerType.getExtraCoords();
        ConfigKey[] styleKeys = layerType.getStyleKeys();

        /* Find out if this layer type looks to be associated with a particular
         * plot type (ad-hoc). */
        final PlotType plotType;
        if ( lname.startsWith( "xyz" ) ) {
            plotType = CubePlotType.getInstance();
        }
        else if ( lname.startsWith( "xy" ) ) {
            plotType = PlanePlotType.getInstance();
        }
        else if ( lname.startsWith( "sky" ) ) {
            plotType = SkyPlotType.getInstance();
        }
        else if ( lname.equals( "yerror" ) ) {
            plotType = TimePlotType.getInstance();
        }
        else {
            plotType = null;
        }

        /* Work out what groups of auxiliary parameters it has. */
        boolean hasPos = npos > 0;
        boolean hasExtra = extraCoords.length > 0;
        boolean hasStyle = styleKeys.length > 0;
        boolean hasShade = layerType instanceof ShapeFamilyLayerType;
        boolean hasTable = hasPos | hasExtra;

        /* Start output. */
        StringBuffer sbuf = new StringBuffer()
            .append( "<subsubsect id='layer-" )
            .append( lname )
            .append( "'>\n" )
            .append( "<subhead><title><code>" )
            .append( lname )
            .append( "</code></title></subhead>\n" );

        /* Description text. */
        sbuf.append( layerType.getXmlDescription() );

        /* Usage overview. */
        List<String> usageWords = new ArrayList<String>();
        usageWords.add( AbstractPlot2Task.PLOTTER_PREFIX + suffix_
                      + "=" + lname );
        if ( hasPos ) {
            String mult = npos > 1 ? ( "*" + npos ) : "";
            usageWords.add( posUsage_ + mult );
        }
        if ( hasExtra ) {
            usageWords.add( extraUsage_ );
        }
        if ( hasStyle ) {
            usageWords.add( styleUsage_ );
        }
        if ( hasShade ) {
            usageWords.add( shadeUsage_ );
        }
        if ( hasTable ) {
            usageWords.add( tableUsage_ );
        }
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

        /* Start list of stanzas for each parameter group. */
        sbuf.append( "<p><dl>\n" );

        /* Positional coordinate parameters. */
        if ( hasPos ) {
            final String usageWord;
            final Parameter[] posParams;
            StringBuffer cbuf = new StringBuffer();
            cbuf.append( "<p>" );
            if ( npos == 1 ) {
                usageWord = posUsage_;
                cbuf.append( "Positional coordinates give a position\n" )
                    .append( "for each row of the input table.\n" );
            }
            else {
                usageWord = posUsage_ + "*" + npos;
                cbuf.append( "The positional coordinates give " )
                    .append( npos )
                    .append( " positions for each row of the input table.\n" );
            }

            /* If we know what kind of plot it is, we can be specific about
             * the positional coorinates. */
            if ( plotType != null ) {
                DataGeom[] geoms = plotType.getPointDataGeoms();
                assert geoms.length == 1;
                DataGeom geom = geoms[ 0 ];
                List<Parameter> paramList = new ArrayList<Parameter>();
                for ( int ipos = 0; ipos < npos; ipos++ ) {
                    String posSuffix = npos == 1
                                     ? ""
                                     : PlotUtil.getIndexSuffix( ipos );
                    for ( Coord posCoord : geom.getPosCoords() ) {
                        String sfix = posSuffix + suffix_;
                        for ( Input input : posCoord.getInputs() ) {
                            paramList.add( AbstractPlot2Task
                                          .createDataParameter( input, sfix,
                                                                false ) );
                        }
                    }
                }
                posParams = paramList.toArray( new Parameter[ 0 ] );
            }

            /* Otherwise, just give an example. */
            else {
                cbuf.append( "Their form depends on the plot geometry\n" )
                    .append( "(which plotting command is used).\n" )
                    .append( "For a plane plot\n" )
                    .append( "(<ref id='plot2plane'><code>plot2plane"
                                                       + "</code></ref>)\n" )
                    .append( "the parameters would be\n" );
                for ( int ipos = 0; ipos < npos; ipos++ ) {
                    String posSuffix = npos == 1
                                     ? ""
                                     : PlotUtil.getIndexSuffix( ipos );
                    String sfix = posSuffix + suffix_;
                    boolean isMid = ipos < npos - 1;
                    cbuf.append( "<code>x" )
                        .append( sfix )
                        .append( "</code>" )
                        .append( isMid ? ", " : " and " )
                        .append( "<code>y" )
                        .append( sfix )
                        .append( "</code>" )
                        .append( isMid ? ", " : "." );
                }
                cbuf.append( ".\n" );
                posParams = new Parameter[ 0 ];
            }

            cbuf.append( "</p>\n" );
            cbuf.append( PlotUtil.concatLines( new String[] {
                "<p>The parameter values are in all cases strings interpreted",
                "as numeric expressions based on column names.",
                "These can be column names, fixed values or algebraic",
                "expressions as described in <ref id='jel'/>.",
                "</p>",
            } ) );
            String posComment = cbuf.toString();
            sbuf.append( usageStanza( usageWord,
                                      "Positional coordinate parameters",
                                      posComment, posParams,
                                      new String[ 0 ] ) );
        }

        /* Non-positional coordinate parameters. */
        if ( hasExtra ) {
            Parameter[] extraParams =
                LayerTypeParameter
               .getCoordParams( extraCoords, suffix_, false );
            String extraComment = PlotUtil.concatLines( new String[] {
                "<p>Coordinate values other than the actual point positions.",
                "The parameter values are in all cases strings interpreted",
                "as expressions based on column names.",
                "These can be column names, fixed values or algebraic",
                "expressions as described in <ref id='jel'/>.",
                "</p>",
            } );
            sbuf.append( usageStanza( extraUsage_,
                                      "Non-positional coordinate parameters",
                                      extraComment, extraParams,
                                      new String[ 0 ] ) );
        }

        /* Style parameters. */
        if ( hasStyle ) {
            Parameter[] styleParams =
                LayerTypeParameter.getConfigParams( styleKeys, suffix_, false );
            String styleComment = PlotUtil.concatLines( new String[] {
                "<p>Gives configuration values for the options",
                "that affect the details of the plot layer's appearance.",
                "</p>",
            } );
            sbuf.append( usageStanza( styleUsage_, "Style parameters",
                                      styleComment, styleParams,
                                      new String[ 0 ] ) );
        }

        /* Shading parameters. */
        if ( hasShade ) {
            ChoiceParameter<ShapeMode> shapeModeParam =
                ((ShapeFamilyLayerType) layerType)
               .createShapeModeParameter( suffix_ );
            Parameter[] shadeParams = new Parameter[] { shapeModeParam };
            String shadeComment = PlotUtil.concatLines( new String[] {
                "<p>Shading parameters determine how the plotted markers",
                "are coloured.",
                "The value supplied for",
                "<code>" + shapeModeParam.getName() + "</code>",
                "determines what other configuration parameters",
                "if any, are required for the shading.",
                "The details are given in the relevant shading subsections",
                "in <ref id='ShapeMode'/>.",
                "</p>",
            } );
            String[] moreUsageWords = new String[] {
                "<shade-params" + suffix_ + ">",
            };
            sbuf.append( usageStanza( shadeUsage_, "Shading parameters",
                                      shadeComment, shadeParams,
                                      moreUsageWords ) );
        }

        /* Input table parameters. */
        if ( hasTable ) {
            Parameter[] tableParams =
                LayerTypeParameter.getInputParams( suffix_ );
            String tableComment = PlotUtil.concatLines( new String[] {
                "<p>The table parameters define the table from which",
                "the input coordinate values will be obtained.",
                "They have the same form as table input parameters",
                "elsewhere in STILTS.",
                "</p>",
            } );
            sbuf.append( usageStanza( tableUsage_, "Table parameters",
                                      tableComment, tableParams,
                                      new String[ 0 ] ) );
        }

        /* End parameter group list. */
        sbuf.append( "</dl></p>\n" );

        /* End top-level element. */
        sbuf.append( "</subsubsect>\n" );
        return sbuf.toString();
    }

    /**
     * Constructs XML text describing usage of a group of parameters.
     *
     * @param  usageForm   formal usage template string
     * @param  txt    short description of parameter group
     * @param  comment   XML text to serve as a textual commentary
     *                   on these parameters
     * @param  params   list of parameters for which to provide detailed
     *                  usage information
     * @return   &lt;dt&gt;&lt;dd&gt; element pair
     */
    private String usageStanza( String usageForm, String txt,
                                String comment, Parameter[] params,
                                String[] moreUsageWords ) {
        StringBuffer sbuf = new StringBuffer();

        /* <dt>. */
        sbuf.append( "<dt>" )
            .append( txt )
            .append( ":" )
            .append( "</dt>\n" )

        /* <dd>. */
            .append( "<dd>\n" );
        List<String> words = new ArrayList<String>();

        /* Add usage summary. */
        words.add( usageForm + ( params.length > 0 ? ":" : "" ) );
        for ( Parameter param : params ) {
            words.add( LayerTypeParameter.usageWord( param ) );
        }
        words.addAll( Arrays.asList( moreUsageWords ) );
        sbuf.append( "<p><verbatim><![CDATA[\n" )
            .append( Formatter.formatWords( words, 3 ) )
            .append( "]]></verbatim></p>\n" );

        /* Add text comments. */
        sbuf.append( comment );

        /* Sort parameter list. */
        params = params.clone();
        Arrays.sort( params, Parameter.BY_NAME );

        /* Add detailed per-parameter usage. */
        if ( params.length > 0 ) {
            sbuf.append( "<p><dl>\n" );
            for ( Parameter param : params ) {
                sbuf.append( UsageWriter.xmlItem( param, basicXml_ ) )
                    .append( "\n" );
            }
            sbuf.append( "</dl></p>\n" );
        }
        sbuf.append( "</dd>" );
        return sbuf.toString();
    }

    /**
     * Returns a list of all the TypedPlot2Tasks known to Stilts.
     *
     * @return   plot tasks
     */
    public static TypedPlot2Task[] getPlot2Tasks() throws LoadException {
        List<TypedPlot2Task> plot2Tasks = new ArrayList<TypedPlot2Task>();
        ObjectFactory<Task> taskFact = Stilts.getTaskFactory();
        for ( String nickname : taskFact.getNickNames() ) {
            Task task = taskFact.createObject( nickname );
            if ( task instanceof TypedPlot2Task ) {
                plot2Tasks.add( (TypedPlot2Task) task );
            }
        }
        return plot2Tasks.toArray( new TypedPlot2Task[ 0 ] );
    }

    /**
     * Returns a map (keyed by name) of all the LayerTypes used by a given
     * list of tasks.
     *
     * @param   known task list
     * @return   known layer types
     */
    public static Map<String,LayerType>
            getLayerTypes( TypedPlot2Task[] tasks ) {
        Map<String,LayerType> typeMap = new LinkedHashMap<String,LayerType>();
        for ( TypedPlot2Task task : tasks ) {
            LayerTypeParameter ltParam =
                AbstractPlot2Task
               .createLayerTypeParameter( "", task.getPlotContext() );
            for ( LayerType ltype : ltParam.getOptions() ) {
                String name = ltype.getName();
                if ( ! typeMap.containsKey( name ) ) {
                    typeMap.put( ltype.getName(), ltype );
                }
            }
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
        List<String> argList = new ArrayList( Arrays.asList( args ) );
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
        Map<String,LayerType> typeMap = getLayerTypes( getPlot2Tasks() );
        if ( argList.size() > 0 ) {
            typeMap.keySet().retainAll( argList );
        }

        /* Generate and print output. */
        PrintStream out = System.out;
        if ( doc ) {
            out.println( "<doc>" );
        }
        LayerTypeDoc doccer = new LayerTypeDoc( basicXml );
        for ( LayerType ltype : typeMap.values() ) {
            out.println( doccer.getXmlDoc( ltype ) );
        }
        if ( doc ) {
            out.println( "</doc>" );
        }
    }
}
