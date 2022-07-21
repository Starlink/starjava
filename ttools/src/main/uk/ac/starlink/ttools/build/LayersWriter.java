package uk.ac.starlink.ttools.build;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.IOException;
import java.util.logging.Level;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.plot2.task.LayerType;
import uk.ac.starlink.ttools.plot2.task.LayerTypeParameter;
import uk.ac.starlink.ttools.plot2.task.TypedPlot2Task;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.ObjectFactory;

/**
 * Write XML text describing LayerTypes available to given TypedPlot2Tasks.
 * This class is designed to be used from its <code>main</code> method.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2014
 */
public class LayersWriter {

    private final TypedPlot2Task<?,?> task_;

    /**
     * Constructor.
     *
     * @param   task for which to write docs
     */
    public LayersWriter( TypedPlot2Task<?,?> task ) {
        task_ = task;
    }

    /** 
     * Returns XML text describing available layers for this writer's task.
     *
     * @return  &lt;p&gt; element(s)
     */
    public String getXml() {
        StringBuffer sbuf = new StringBuffer()
            .append( "<p>Content is added to the plot by specifying\n" )
            .append( "one or more <em>plot layers</em> using the\n" )
            .append( "<code>layerN</code> parameter.\n" )
            .append( "The <code>N</code> part is a suffix applied to\n" )
            .append( "all the parameters affecting a given layer;\n" )
            .append( "any suffix (including the empty string) may be used.\n" )
            .append( "Available layers for this plot type are:\n" );
        LayerType[] ltypes =
            LayerTypeParameter.getLayerTypes( task_.getPlotContext()
                                             .getPlotType().getPlotters() );
        int nt = ltypes.length;
        for ( int it = 0; it < nt; it++ ) {
            sbuf.append( LayerTypeDoc.layerTypeRef( ltypes[ it ] ) )
                .append( it < nt - 1 ? "," : "." )
                .append( "\n" );
        }
        sbuf.append( "</p>\n" );
        return sbuf.toString();
    }

    /**
     * With an arg: writes description for that TypedPlot2Task.
     * Without args: writes descriptions for all to current directory.
     */
    public static void main( String[] args ) throws LoadException, IOException {
        LogUtils.getLogger( "uk.ac.starlink.ttools.plot2" )
                .setLevel( Level.WARNING );
        ObjectFactory<Task> taskFact = Stilts.getTaskFactory();
        if ( args.length == 0 ) {
            File dir = new File( "." );
            for ( String nickname : taskFact.getNickNames() ) {
                Task task = taskFact.createObject( nickname );
                if ( task instanceof TypedPlot2Task ) {
                    TypedPlot2Task<?,?> p2task = (TypedPlot2Task<?,?>) task;
                    String fname = nickname + "-layers.xml";
                    File file = new File( dir, fname );
                    System.out.println( "Writing " + fname );
                    Writer out =
                        new BufferedWriter(
                            new OutputStreamWriter(
                                new FileOutputStream( file ) ) );
                    out.write( new LayersWriter( p2task ).getXml() );
                    out.close();
                }
            }
        }
        else {
            LayersWriter writer =
                new LayersWriter( (TypedPlot2Task<?,?>)
                                  taskFact.createObject( args[ 0 ] ) );
            System.out.println( writer.getXml() );
        }
    }
}
