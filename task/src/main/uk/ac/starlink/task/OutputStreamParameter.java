package uk.ac.starlink.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import uk.ac.starlink.util.Destination;

/**
 * Parameter for selecting an output stream to write to.
 *
 * @author   Mark Taylor
 * @since    9 May 2006
 */
public class OutputStreamParameter extends Parameter<Destination> {

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    @SuppressWarnings("this-escape")
    public OutputStreamParameter( String name ) {
        super( name, Destination.class, true );
        setUsage( "<out-file>" );
        setPrompt( "Location of output file" );
        setStringDefault( "-" );

        setDescription( new String[] {
            "<p>The location of the output file.  This is usually a filename",
            "to write to.",
            "If it is equal to the special value \"-\"",
            "the output will be written to standard output.",
            "</p>",
        } );
    }

    public Destination stringToObject( Environment env, String sval )
            throws ParameterValueException {
        if ( "-".equals( sval ) ) {
            final PrintStream out = env.getOutputStream();
            return new Destination() {
                public OutputStream createStream() {
                    return out;
                }
            };
        }
        else {
            final File file = new File( sval );
            File parentDir = new File( sval ).getAbsoluteFile().getParentFile();
            if ( ! parentDir.exists() ) {
                throw new ParameterValueException( this,
                                                   "Bad pathname (no dir?)" );
            }
            return new Destination() {
                public OutputStream createStream() throws IOException {
                    return new FileOutputStream( file );
                }
            };
        }
    }
}
