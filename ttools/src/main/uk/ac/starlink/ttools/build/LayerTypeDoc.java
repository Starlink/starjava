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
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.data.Coord;
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
            .append( "</p>\n" );

        /* Start list of stanzas for each parameter group. */
        sbuf.append( "<p><dl>\n" );

        /* Positional coordinate parameters. */
        if ( hasPos ) {
            String[] posComments = new String[] {
                "<p>Positional coordinates give a position for each row",
                "of the input table.",
                "Their form depends on the plot geometry",
                "(which plotting command is used).",
                "For a plane plot",
                "(<ref id='plot2plane'><code>plot2plane</code></ref>)",
                "the parameters would be",
                "<code>x" + suffix_ + "</code> and",
                "<code>y" + suffix_ + "</code>,",
                "for the horizontal and vertical coordinates respectively.",
                "</p>",
                "<p>The parameter values are in all cases strings interpreted",
                "as numeric expressions based on column names.",
                "These can be column names, fixed values or algebraic",
                "expressions as described in <ref id='jel'/>.",
                "</p>",
            };
            sbuf.append( usageStanza( posUsage_,
                                      "Positional coordinate parameters",
                                      posComments, new Parameter[ 0 ],
                                      new String[ 0 ] ) );
        }

        /* Non-positional coordinate parameters. */
        if ( hasExtra ) {
            Parameter[] extraParams =
                LayerTypeParameter.getCoordParams( extraCoords, suffix_ );
            String[] extraComments = new String[] {
                "<p>Coordinate values other than the actual point positions.",
                "The parameter values are in all cases strings interpreted",
                "as expressions based on column names.",
                "These can be column names, fixed values or algebraic",
                "expressions as described in <ref id='jel'/>.",
                "</p>",
            };
            sbuf.append( usageStanza( extraUsage_,
                                      "Non-positional coordinate parameters",
                                      extraComments, extraParams,
                                      new String[ 0 ] ) );
        }

        /* Style parameters. */
        if ( hasStyle ) {
            Parameter[] styleParams =
                LayerTypeParameter.getConfigParams( styleKeys, suffix_ );
            String[] styleComments = new String[] {
                "<p>Gives configuration values for the options",
                "that affect the details of the plot layer's appearance.",
                "</p>",
            };
            sbuf.append( usageStanza( styleUsage_, "Style parameters",
                                      styleComments, styleParams,
                                      new String[ 0 ] ) );
        }

        /* Shading parameters. */
        if ( hasShade ) {
            ChoiceParameter<ShapeMode> shapeModeParam =
                ((ShapeFamilyLayerType) layerType)
               .createShapeModeParameter( suffix_ );
            Parameter[] shadeParams = new Parameter[] { shapeModeParam };
            String[] shadeComments = new String[] {
                "<p>Shading parameters determine how the plotted markers",
                "are coloured.",
                "The value supplied for",
                "<code>" + shapeModeParam.getName() + "</code>",
                "determines what other configuration parameters",
                "if any, are required for the shading.",
                "The details are given in the relevant shading subsections",
                "in <ref id='ShapeMode'/>.",
                "</p>",
            };
            String[] moreUsageWords = new String[] {
                "<shade-params" + suffix_ + ">",
            };
            sbuf.append( usageStanza( shadeUsage_, "Shading parameters",
                                      shadeComments, shadeParams,
                                      moreUsageWords ) );
        }

        /* Input table parameters. */
        if ( hasTable ) {
            Parameter[] tableParams =
                LayerTypeParameter.getInputParams( suffix_ );
            String[] tableComments = new String[] {
                "<p>The table parameters define the table from which",
                "the input coordinate values will be obtained.",
                "They have the same form as table input parameters",
                "elsewhere in STILTS.",
                "</p>",
            };
            sbuf.append( usageStanza( tableUsage_, "Table parameters",
                                      tableComments, tableParams,
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
     * @param  commentLines  array of lines of XML text to serve as a
     *                       textual commentary on these parameters
     * @param  params   list of parameters for which to provide detailed
     *                  usage information
     * @return   &lt;dt&gt;&lt;dd&gt; element pair
     */
    private String usageStanza( String usageForm, String txt,
                                String[] commentLines, Parameter[] params,
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
        for ( String line : commentLines ) {
            sbuf.append( line )
                .append( "\n" );
        }

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
