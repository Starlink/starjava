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
public class OutputStreamParameter extends Parameter {

    private Destination destination_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public OutputStreamParameter( String name ) {
        super( name );
        setUsage( "<out-file>" );
        setPrompt( "Location of output file" );
        setDefault( "-" );

        setDescription( new String[] {
            "<p>The location of the output file.  This is usually a filename",
            "to write to.",
            "If it is equal to the special value \"-\"",
            "the output will be written to standard output.",
            "</p>",
        } );
    }

    public void setValueFromString( Environment env, String sval )
            throws TaskException {
        if ( sval == null || sval.trim().length() == 0 ) {
            destination_ = null;
        }
        else if ( "-".equals( sval ) ) {
            final PrintStream out = env.getOutputStream();
            destination_ = new Destination() {
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
            destination_ = new Destination() {
                public OutputStream createStream() throws IOException {
                    return new FileOutputStream( file );
                }
            };
        }
        super.setValueFromString( env, sval );
    }

    /**
     * Returns a Destination object representing the value of this parameter.
     *
     * @param  env  execution environment
     */
    public Destination destinationValue( Environment env )
            throws TaskException {
        checkGotValue( env );
        return destination_;
    }
}
