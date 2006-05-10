package uk.ac.starlink.ttools.mode;

import java.io.IOException;
import java.io.PrintStream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.task.TableEnvironment;
import uk.ac.starlink.ttools.task.OutputFormatParameter;

/**
 * Output mode for writing the table as a stream with CGI headers.
 *
 * @author   Mark Taylor
 * @since    10 May 2006
 */
public class CgiMode implements ProcessingMode {

    private final Parameter formatParam_;

    public CgiMode() {
        formatParam_ = new OutputFormatParameter( "ofmt" );
        formatParam_.setDefault( "votable" );
        formatParam_.setNullPermitted( false );
        formatParam_.setDescription( new String[] {
            "Specifies the format in which the output table will be written",
            "(one of the ones in <ref id='outFormats'/> - matching is",
            "case-insensitive and you can use just the first few letters).",
        } );
    }

    public Parameter[] getAssociatedParameters() {
        return new Parameter[] {
            formatParam_,
        };
    }

    public String getDescription() {
        return new StringBuffer()
       .append( "Writes a table to standard output in a way suitable for\n" )
       .append( "use as output from a CGI (Common Gateway Interface) " )
       .append( "program.\n" )
       .append( "This is very much like <code>out</code> mode\n" )
       .append( "but a short CGI header giving the MIME Content-Type\n" )
       .append( "is prepended to the output." )
       .toString();
    }

    public TableConsumer createConsumer( Environment env )
            throws TaskException {
        String fmt = formatParam_.stringValue( env );
        StarTableOutput sto = TableEnvironment.getTableOutput( env );
        final StarTableWriter handler; 
        try {
             handler = sto.getHandler( fmt );
        }
        catch ( TableFormatException e ) {
             throw new ParameterValueException( formatParam_,
                                                "No such output format", e );
        }
        final PrintStream out = System.out;
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                out.print( "Content-Type: " + handler.getMimeType() + "\n" );
                out.print( "\n" );
                handler.writeStarTable( table, out );
                out.flush();
            }
        };
    }
}
