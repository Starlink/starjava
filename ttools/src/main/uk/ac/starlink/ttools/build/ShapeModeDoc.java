package uk.ac.starlink.ttools.build;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.Formatter;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapeModePlotter;
import uk.ac.starlink.ttools.plot2.task.AbstractPlot2Task;
import uk.ac.starlink.ttools.plot2.task.LayerType;
import uk.ac.starlink.ttools.plot2.task.LayerTypeParameter;
import uk.ac.starlink.ttools.plot2.task.ShapeFamilyLayerType;
import uk.ac.starlink.util.LoadException;

/**
 * Writes XML text documenting known plot2 ShapeModes (shading modes).
 * Output is to standard output.
 * This class is designed to be used from its <code>main</code> method.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2014
 */
public class ShapeModeDoc {

    private final boolean basicXml_;
    private final String suffix_;
    private final String paramPrefix_;
    private final Map<String,String> examplesMap_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.build" );

    /**
     * Constructor.
     *
     * @param  basicXml  avoid XML constructs that won't show up in text output
     */
    public ShapeModeDoc( boolean basicXml ) {
        basicXml_ = basicXml;
        suffix_ = AbstractPlot2Task.EXAMPLE_LAYER_SUFFIX;
        paramPrefix_ = ShapeFamilyLayerType.SHADING_PREFIX;
        examplesMap_ = Plot2Example.getExamplesXml();
    }

    /**
     * Returns XML element giving full user documentation for a given
     * shading mode.
     *
     * @param  mode  shapemode
     * @return  text of a &lt;subsect&gt; element
     */
    public String getXmlDoc( ShapeMode mode ) {
        String mname = mode.getModeName();
        Parameter<?>[] coordParams =
            LayerTypeParameter
           .getCoordParams( mode.getExtraCoords(), suffix_, true );
        Parameter<?>[] styleParams =
            LayerTypeParameter
           .getLayerConfigParams( mode.getConfigKeys(), suffix_, false );
        Parameter<?>[] params =
            PlotUtil.arrayConcat( coordParams, styleParams );
        StringBuffer sbuf = new StringBuffer();

        /* Start section. */
        String modeId = "shading-" + mname;
        sbuf.append( "<subsubsect id='" )
            .append( modeId )
            .append( "'>\n" )
            .append( "<subhead><title><code>" )
            .append( mname )
            .append( "</code></title></subhead>\n" );

        /* Description. */
        sbuf.append( mode.getModeDescription() );

        /* Usage overview. */
        List<String> usageWords = new ArrayList<String>();
        usageWords.add( paramPrefix_ + suffix_ + "=" + mname );
        for ( Parameter<?> param : params ) {
            usageWords.add( LayerTypeParameter.usageWord( param ) );
        }
        sbuf.append( "<p>\n" )
            .append( "<strong>Usage:</strong>\n" )
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

        /* Add example figure. */
        String exname = "shading-" + mname;
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
            logger_.severe( "No example figure for shading type " + mname );
        }

        /* Parameter details. */
        if ( params.length > 0 ) {
            sbuf.append( "<p>Associated parameters are as follows:\n" );
            sbuf.append( "<dl>\n" );
            Arrays.sort( params, Parameter.BY_NAME );
            for ( Parameter<?> param : params ) {
                sbuf.append( UsageWriter.xmlItem( param, modeId, basicXml_ ) );
            }
            sbuf.append( "</dl>\n" );
            sbuf.append( "</p>\n" );
        }

        /* End section. */
        sbuf.append( "</subsubsect>\n" );
        return sbuf.toString();
    }

    /**
     * Returns a map (keyed by name) of all the ShapeModes known to Stilts.
     *
     * @return  name->mode map
     */
    public static Map<String,ShapeMode> getShapeModes() throws LoadException {
        Map<String,ShapeMode> modeMap = new LinkedHashMap<String,ShapeMode>();
        for ( LayerType ltype : LayerTypeDoc.getLayerTypes() ) {
            if ( ltype instanceof ShapeFamilyLayerType ) {
                ShapeModePlotter[] plotters =
                    ((ShapeFamilyLayerType) ltype).getShapeModePlotters();
                for ( ShapeModePlotter plotter : plotters ) {
                    ShapeMode mode = plotter.getMode();
                    modeMap.put( mode.getModeName(), mode );
                }
            }
        }
        return modeMap;
    }

    /**
     * Main method.  Try <code>-help</code>.
     */
    public static void main( String[] args ) throws LoadException {
        String usage = new StringBuffer()
           .append( "\n   " )
           .append( "Usage: " )
           .append( ShapeModeDoc.class.getName() )
           .append( " [-doc]" )
           .append( " [-basicxml]" )
           .append( " [<mode-name> ...]" )
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

        /* Get known shape modes. */
        Map<String,ShapeMode> modeMap = getShapeModes();
        if ( argList.size() > 0 ) {
            modeMap.keySet().retainAll( argList );
        }

        /* Generate and print output. */
        PrintStream out = System.out;
        if ( doc ) {
            out.println( "<doc>" );
        }
        ShapeModeDoc doccer = new ShapeModeDoc( basicXml );
        for ( ShapeMode mode : modeMap.values() ) {
            out.println( doccer.getXmlDoc( mode ) );
        }
        if ( doc ) {
            out.println( "</doc>" );
        }
    }
}
