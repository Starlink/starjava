package uk.ac.starlink.ttools.build;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.task.LineInvoker;
import uk.ac.starlink.util.LoadException;

/**
 * Writes a usage paragraph specific to one of the STILTS tasks.
 * Output is to standard output.  This class is designed to be used
 * from its {@link #main} method.
 *
 * @author   Mark Taylor
 * @since    17 Aug 2005
 */
public class UsageWriter {

    private final OutputStream out_;
    private final String taskName_;
    private final Task task_;

    private UsageWriter( String taskName, OutputStream out ) 
            throws LoadException {
        out_ = out;
        taskName_ = taskName;
        task_ = (Task) Stilts.getTaskFactory().createObject( taskName_ );
    }

    private void writeXml() throws IOException {
        String prefix = "   stilts <stilts-flags> " + taskName_;
        outln( "<subsubsect id=\"" + taskName_ + "-usage\">" );
        outln( "<subhead><title>Usage</title></subhead>" );
        outln( "<p>The usage of <code>" + taskName_ + "</code> is" );
        outln( "<verbatim><![CDATA[" );
        outln( LineInvoker.getPrefixedTaskUsage( task_, prefix )
                          .replaceFirst( "\n+$", "" ) );
        outln( "]]></verbatim>" );
        outln( "If you don't have the <code>stilts</code> script installed," );
        outln( "write \"<code>java -jar stilts.jar</code>\" instead of" );
        outln( "\"<code>stilts</code>\" - see <ref id=\"invoke\"/>." );
        outln( "The available <code>&lt;stilts-flags&gt;</code> are listed" );
        outln( "in <ref id=\"stilts-flags\"/>." );
        outln( "</p>" );
        outln();

        Parameter[] params = task_.getParameters();
        if ( params.length > 0 ) {
            outln( "<p>Parameter values are assigned on the command line" );
            outln( "as explained in <ref id=\"task-args\"/>." );
            outln( "They are as follows:" );
            outln( "</p>" );
            outln( "<p>" );
            outln( "<dl>" );
            Arrays.sort( params, new Comparator() {
                public int compare( Object o1, Object o2 ) {
                    Parameter p1 = (Parameter) o1;
                    Parameter p2 = (Parameter) o2;
                    return ((Parameter) o1).getName()
                          .compareTo( ((Parameter) o2).getName() );
                }
            } );
            for ( int i = 0; i < params.length; i++ ) {
                outln( xmlItem( params[ i ] ) );
            }
            outln( "</dl>" );
            outln( "</p>" );
        }
        else {
            outln( "<p>This task has no parameters." );
            outln( "</p>" );
        }
        outln( "</subsubsect>" );
    }

    /**
     * Returns a list item (dt/dd pair) for a parameter giving its usage
     * and description.
     * 
     * @param  param  parameter
     * @return   XML snippet for <code>param</code>
     */
    public static String xmlItem( Parameter param ) {
        String usage = ( param.getName() + " = " + param.getUsage() )
                      .replaceAll( "<", "&lt;" )
                      .replaceAll( ">", "&gt;" );
        String descrip = param.getDescription();
        if ( descrip == null ) {
            throw new NullPointerException( "No description for parameter "
                                          + param );
        }
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<dt><code>" )
            .append( ( param.getName() + " = " + param.getUsage() )
                    .replaceAll( "<", "&lt;" )
                    .replaceAll( ">", "&gt;" ) )
            .append( "</code></dt>\n" )
            .append( "<dd>" )
            .append( param.getDescription().toString() );
        String dflt = param.getDefault();
        if ( dflt != null && dflt.length() > 0 ) {
            sbuf.append( "<p>[Default: <code>" )
                .append( dflt.replaceAll( "&", "&amp;" )
                             .replaceAll( "<", "&lt;" )
                             .replaceAll( ">", "&gt;" ) )
                .append( "</code>]</p>" );
        }
        sbuf.append( "</dd>" );
        return sbuf.toString();
    }

    private void out( String text ) throws IOException {
        out_.write( text.getBytes() );
    }

    private void outln( String line ) throws IOException {
        out( line + "\n" );
    }

    private void outln() throws IOException {
        out( "\n" );
    }

    /**
     * Writes a file called <i>taskname</i>-summary.xml for each of the
     * tasks in STILTS.  This contains a basic usage summary with some
     * surrounding XML boilerplate.
     */
    public static void main( String[] args ) throws IOException, LoadException {
        if ( args.length == 0 ) {
            String[] taskNames = Stilts.getTaskFactory().getNickNames();
            File dir = new File( "." );
            for ( int i = 0; i < taskNames.length; i++ ) {
                String taskName = taskNames[ i ];
                String fname = taskName + "-summary.xml";
                File file = new File( dir, fname );
                System.out.println( "Writing " + fname );
                OutputStream out = new FileOutputStream( file );
                out = new BufferedOutputStream( out );
                new UsageWriter( taskName, out ).writeXml();
                out.close();
            }
        }
        else {
            new UsageWriter( args[ 0 ], System.out ).writeXml();
        }
    }
}
