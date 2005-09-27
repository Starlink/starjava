package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uk.ac.starlink.table.jdbc.TerminalAuthenticator;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Table execution environment which gets parameter variables from
 * command-line arguments.
 * Currently the arguments are of the form "param-name=param-value".
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class LineEnvironment extends TableEnvironment {

    private Argument[] arguments_;
    private int narg_;
    private int numberedArgs_;
    private boolean interactive_ = true;
    private boolean promptUnspecified_;
    private Set clearedParams_ = new HashSet();

    private static final String NULL_STRING = "";

    /**
     * Initializes the input data for this environment from a list of
     * words presumably got from a command line of some description.
     *
     * @param  args  array of words from the command line
     */
    public void setArgs( String[] args ) {
        if ( arguments_ != null ) {
            throw new IllegalStateException( "Arguments already set" );
        }
        arguments_ = new Argument[ args.length ];
        for ( int i = 0; i < args.length; i++ ) {
            arguments_[ i ] = new Argument( args[ i ] );
        }
    }

    /**
     * From the command line arguments finds the string value of a selected
     * parameter.  If the parameter has been given in such a way as
     * to represent a null value, the special value
     * {@link #NULL_STRING} (the empty string) will be returned.
     * If the environment has no value for the parameter (the user has
     * not specified it explicitly), then <code>null</code> will be returned.
     *
     * @param   param  parameter to locate
     * @return  string value for <tt>param</tt>
     */
    private String findValue( Parameter param ) {

        /* If it's a multiparameter, concatenate all the appearances
         * on the command line. */
        if ( param instanceof MultiParameter ) {
            StringBuffer val = new StringBuffer();
            int igot = 0;
            for ( int i = 0; i < arguments_.length; i++ ) {
                Argument arg = arguments_[ i ];
                if ( param.getName().equals( arg.name_ ) ||
                     ( arg.pos_ > 0 && param.getPosition() == arg.pos_ ) ) {
                    arg.used_ = true;
                    if ( igot++ > 0 ) {
                        val.append( ';' );
                    }
                    val.append( arg.value_ );
                }
            }
            if ( igot > 0 ) {
                return val.toString();
            }
        }

        /* Otherwise, just take the first one with a matching name or
         * the right position. */
        else {
            for ( int i = 0; i < arguments_.length; i++ ) {
                Argument arg = arguments_[ i ];
                if ( param.getName().equals( arg.name_ ) ||
                     ( arg.pos_ > 0 && param.getPosition() == arg.pos_ ) ) {
                    arg.used_ = true;
                    return arg.value_;
                }
            }
        }

        /* Not found. */
        return null;
    }

    /**
     * Prompts the user for the value of a parameter, and sets its
     * value from the users's input.  Should only be called if we are
     * running interactively.
     *
     * @param   param  parameter
     */
    private void promptForValue( Parameter param ) throws TaskException {

        /* Assemble a prompt string. */
        String name = param.getName();
        String prompt = param.getPrompt();
        String dflt = param.getDefault();
        StringBuffer obuf = new StringBuffer( param.getName() );
        if ( prompt != null ) {
            obuf.append( " - " )
                .append( prompt );
        }
        if ( dflt != null || param.isNullPermitted() ) {
            obuf.append( " /" )
                .append( dflt )
                .append( "/" );
        }
        obuf.append( ": " );
        String promptLine = obuf.toString();

        /* Elicit a response string from the user. */
        String sval;
        try {
            PrintStream err = getErrorStream();
            sval = name.equals( "password" )
                 ? TerminalAuthenticator.readMaskedString( promptLine, err )
                 : TerminalAuthenticator.readString( promptLine, err );
        }
        catch ( IOException e ) {
            throw new TaskException( "Prompt for value failed" );
        }

        /* Massage it if necessary. */
        String stringVal = sval.length() == 0 
                         ? param.getDefault()
                         : readValue( sval );
        if ( NULL_STRING.equals( stringVal ) ) {
            stringVal = null;
        }

        /* Attempt to set the parameter's value. */
        setValueFromString( param, stringVal );
    }

    /**
     * Reads a string from standard input.
     *
     * @param  prompt  short prompt for the user
     * @return  string entered by user
     */
    private String readString( String prompt ) throws IOException {
        getErrorStream().print( prompt );
        getErrorStream().flush();

        /* Read a result line from standard input. */
        StringBuffer ibuf = new StringBuffer();
        for ( boolean done = false; ! done; ) {
            int c = System.in.read();
            switch ( c ) {
                case -1:
                case '\n':
                case '\r':
                    done = true;
                    break;
                default:
                    ibuf.append( (char) c );
            }
        }
        return ibuf.toString();
    }

    /**
     * Clears the value of a given parameter.
     *
     * @throws   UnsupportedOperationException
     */
    public void clearValue( Parameter par ) {
        clearedParams_.add( par );
    }

    public PrintStream getOutputStream() {
        return System.out;
    }

    public PrintStream getErrorStream() {
        return System.err;
    }

    public void acquireValue( Parameter param ) throws TaskException {

        /* Unless the value has been explicitly cleared, search for it
         * in the values specified on the command line. */
        String stringVal;
        if ( clearedParams_.contains( param ) ) {
            stringVal = null;
            clearedParams_.remove( param );
        }
        else {
            stringVal = findValue( param );
        }

        /* If an explicit value has been given, attempt to set the parameter
         * value from it. */
        if ( stringVal != null ) {
            String sval = stringVal.equals( NULL_STRING ) ? null : stringVal;
            setValueFromString( param, sval );
        }

        /* If no explicit value has been given, we may wish to prompt
         * the user. */
        else if ( interactive_ &&
                  ( promptUnspecified_
                 || ( param.getDefault() == null && ! param.isNullPermitted() )
                 || param.getPreferExplicit() ) ) {
            promptForValue( param );
        }

        /* Otherwise, use the default. */
        else {
            setValueFromString( param, param.getDefault() );
        }
    }

    /**
     * Invokes <code>setValueFromString</code> on a parameter, checking
     * whether an illegal <code>null</code> is being set.
     *
     * @param   param   parameter whose value is to be set
     * @param   sval    string representation of <code>param</code>'s value
     */
    private void setValueFromString( Parameter param, String sval )
            throws TaskException {
        if ( sval == null && ! param.isNullPermitted() ) {
            throw new ParameterValueException( param, 
                                               "null value not allowed" );
        }
        param.setValueFromString( this, sval );
    }

    /**
     * Returns a string containing any words of the input argument list 
     * which were never queried by the application to find their 
     * value.  Such unused words probably merit a warning, since they 
     * may for instance be misspelled versions of real parameters.
     *
     * @return   array of unused words
     */
    public String[] getUnused() {
        List unusedList = new ArrayList();
        for ( int i = 0; i < arguments_.length; i++ ) {
            Argument arg = arguments_[ i ];
            if ( ! arg.used_ ) {
                unusedList.add( arg.orig_ );
            }
        }
        return (String[]) unusedList.toArray( new String[ 0 ] );
    }

    /**
     * Turns a string value from the input word into a string value suitable
     * for presentation to the parameter system.  If a string representing
     * the null value is presented, the result will be {@link #NULL_STRING}
     * (the empty string).
     *
     * @param   sval  raw string value
     * @return  processed string value
     */
    private static String readValue( String sval ) {
        if ( sval.length() == 0 || sval.equals( "null" ) ) {
            return NULL_STRING;
        }
        else {
            return sval;
        }
    }

    /**
     * Helper class to encapsulate a word found on the command line.
     */
    private class Argument {

        final String orig_;
        final String name_;
        final int pos_;
        final String value_;
        boolean used_;

        /**
         * Constructor.
         *
         * @param  arg  name=value word
         */
        Argument( String arg ) {
            orig_ = arg;
            int epos = arg.indexOf( '=' );
            if ( epos > 0 ) {
                name_ = arg.substring( 0, epos );
                pos_ = -1;
                value_ = readValue( arg.substring( epos + 1 ) );
            }
            else {
                pos_ = ++numberedArgs_;
                name_ = null;
                value_ = readValue( arg );
            }
        }
    }

}
