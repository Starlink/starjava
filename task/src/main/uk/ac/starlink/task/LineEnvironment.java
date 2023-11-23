package uk.ac.starlink.task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import uk.ac.starlink.util.LineReader;

/**
 * Execution environment for use from the command line.
 * Arguments are supplied as {@link LineWord} objects.
 *
 * @author   Mark Taylor
 * @since    27 Nov 2006
 */
public class LineEnvironment implements Environment {

    private boolean promptAll_;
    private boolean interactive_ = true;
    private int numberedArgs_;
    private Argument[] arguments_;
    private PrintStream out_ = System.out;
    private PrintStream err_ = System.err;
    private final Set<Parameter<?>> clearedParams_ =
        new HashSet<Parameter<?>>();
    private final List<String> acquiredValues_ = new ArrayList<String>();

    static final String NULL_STRING = "";
    public static final char INDIRECTION_CHAR = '@';
    public static final int NUM_TRIES = 3;

    /**
     * Constructs a LineEnvironment without any additional initialisation.
     * A subsequent call of {@link #setWords} must be made before 
     * it is interrogated for values.
     */
    public LineEnvironment() {
    }

    /**
     * Constructs a new LineEnvironment, initialising it with a set of command
     * line arguments.  These are checked against a set of parameters.
     * The arguments must be of the form <code>name=value</code>, or 
     * just <code>value</code> for positional arguments.
     * If any of these refer to parameters not in the <code>params</code>
     * list, an exception will be thrown.
     *
     * @param    args  command-line arguments
     * @param   params  parameters that <code>args</code> provide values for
     */
    public LineEnvironment( String[] args, Parameter<?>[] params )
            throws UsageException {
        this();
        LineWord[] words = new LineWord[ args.length ];
        for ( int i = 0; i < args.length; i++ ) {
            words[ i ] = new LineWord( args[ i ] );
        }
        setWords( words );
        checkParameters( params );
    }

    /**
     * Sets the words supplying the parameter values for this environment.
     *
     * @param   words  list of words
     */
    public void setWords( LineWord[] words ) {
        if ( arguments_ != null ) {
            throw new IllegalStateException( "Arguments already set" );
        }
        arguments_ = new Argument[ words.length ];
        for ( int i = 0; i < words.length; i++ ) {
            arguments_[ i ] = new Argument( words[ i ] );
        }
    }

    /**
     * Returns the help string for a given parameter.
     * This should be supplied in ready-to-use form, that is formatted for a
     * suitable screen width etc.
     *
     * @param   param  parameter
     * @return   formatted documentation for <code>param</code>
     */
    public String getParamHelp( Parameter<?> param ) {
        return param.getPrompt();
    }

    /**
     * Determines whether a parameter is "hidden", that is its value
     * should not be revealed to prying eyes.
     * The default implementation returns false.
     *
     * @param  param  param
     * @return  true if param is hidden type
     */
    public boolean isHidden( Parameter<?> param ) {
        return false;
    }

    /**
     * Checks that this environment's values are compatible with the given
     * list of parameters.  If any of the values does not appear to be a
     * spcecification of one of the listed parameters a UsageException will
     * be thrown.
     *
     * @param  params   parameter list
     */
    public void checkParameters( Parameter<?>[] params ) throws UsageException {
        int narg = arguments_.length;
        Set<Parameter<?>> paramSet =
            new HashSet<Parameter<?>>( Arrays.asList( params ) );
        int position = 1;
        for ( int ia = 0; ia < narg; ia++ ) {
            LineWord word = arguments_[ ia ].word_;
            boolean found = false;
            for ( Iterator<Parameter<?>> it = paramSet.iterator();
                  ! found && it.hasNext(); ) {
                Parameter<?> param = it.next();
                if ( ! found && paramNameMatches( word.getName(), param ) ) {
                    found = true;
                    if ( ! ( param instanceof MultiParameter ) ) {
                        it.remove();
                    }
                }
                if ( ! found && param.getName() == null ) {
                    if ( param.getPosition() == position ) {
                        found = true;
                        it.remove();
                        position++;
                    }
                }
            }
            if ( ! found ) {
                throw new UsageException( "Unrecognised parameter "
                                        + word.getText() );
            }
        }
    }

    public String[] getNames() {
        int narg = arguments_.length;
        String[] names = new String[ narg ];
        for ( int iarg = 0; iarg < narg; iarg++ ) {
            names[ iarg ] = arguments_[ iarg ].word_.getName();
        }
        return names;
    }

    /**
     * Sets whether we are running interactively or not.
     * Only if this attribute is true will any attempt be made to prompt
     * the user for responses etc.  Default is true.
     *
     * @param  interactive  whether we are running interactively
     */
    public void setInteractive( boolean interactive ) {
        interactive_ = interactive;
    }

    /**
     * Determines whether we are running interactively.
     * Only if this attribute is true will any attempt be made to prompt
     * the user for responses etc.  Default is true.
     *
     * @return   whether we are running interactively
     */
    public boolean getInteractive() {
        return interactive_;
    }

    /**
     * Sets whether all parameters which haven't received explicit values
     * on the command line should be prompted for.
     * Default is false.  Only makes sense if we're running interatively.
     *
     * @param  prompt  whether to prompt for everything
     */
    public void setPromptAll( boolean prompt ) {
        if ( interactive_ ) {
            promptAll_ = prompt;
        }
        else {
            throw new IllegalStateException( "Not interactive" );
        }
    }

    /**
     * Determines whether all parameters which haven't received explicit
     * values on the command line should be prompted for.
     * Default is false.
     *
     * @return   whether to prompt for everything
     */
    public boolean getPromptAll() {
        return promptAll_;
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
    private String findValue( Parameter<?> param ) throws TaskException {

        /* If it's a multiparameter, concatenate all the appearances
         * on the command line. */
        if ( param instanceof MultiParameter ) {
            char separator = ((MultiParameter) param).getValueSeparator();
            List<String> valueList = new ArrayList<String>();
            for ( int i = 0; i < arguments_.length; i++ ) {
                Argument arg = arguments_[ i ];
                LineWord word = arg.word_;
                if ( paramNameMatches( word.getName(), param ) ||
                     ( arg.pos_ > 0 && param.getPosition() == arg.pos_ ) ) {
                    arg.used_ = true;
                    valueList.add( word.getValue() );
                }
            }
            if ( valueList.size() == 1 ) {
                String value = valueList.get( 0 );
                if ( value.length() > 0 &&
                     value.charAt( 0 ) == INDIRECTION_CHAR ) {
                    String[] lines;
                    try {
                        lines = readLines( value.substring( 1 ) );
                    }
                    catch ( IOException e ) {
                        throw new TaskException( e.getMessage(), e );
                    }
                    StringBuffer val = new StringBuffer();
                    for ( int i = 0; i < lines.length; i++ ) {
                        if ( i > 0 ) {
                            val.append( separator );
                        }
                        val.append( lines[ i ].trim() );
                    }
                    return val.toString();
                }
                else {
                    return value;
                }
            }
            else if ( valueList.size() > 1 ) {
                StringBuffer val = new StringBuffer();
                for ( Iterator<String> it = valueList.iterator();
                      it.hasNext(); ) {
                    val.append( it.next() );
                    if ( it.hasNext() ) {
                        val.append( separator );
                    }
                }
                return val.toString();
            }
        }

        /* Otherwise, just take the first one with a matching name or
         * the right position. */
        else {
            for ( int i = 0; i < arguments_.length; i++ ) {
                Argument arg = arguments_[ i ];
                LineWord word = arg.word_;
                if ( paramNameMatches( word.getName(), param ) ||
                     ( arg.pos_ > 0 && param.getPosition() == arg.pos_ ) ) {
                    arg.used_ = true;
                    return word.getValue();
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
     * If the value was set successfully, the value string is returned.
     * However, note that this is a byproduct, it is not necessary
     * to use this value to set the value of the parameter following
     * this call.
     *
     * @param   param  parameter
     * @return   the value to which the parameter was set
     */
    private String promptForValue( Parameter<?> param ) throws TaskException {

        /* Assemble a prompt string. */
        String name = param.getName();
        String prompt = param.getPrompt();
        String strDflt = param.getStringDefault();
        StringBuffer obuf = new StringBuffer( param.getName() );
        if ( prompt != null ) {
            obuf.append( " - " )
                .append( prompt );
        }
        if ( strDflt != null || param.isNullPermitted() ) {
            obuf.append( " [" )
                .append( strDflt )
                .append( "]" );
        }
        obuf.append( ": " );
        String promptLine = obuf.toString();

        /* Try to get a sensible response. */
        for ( int ntry = 0; ntry < NUM_TRIES; ntry++ ) {

            /* Elicit a response string from the user. */
            String sval;
            try {
                PrintStream err = getErrorStream();
                sval = isHidden( param )
                     ? LineReader.readMaskedString( promptLine, err )
                     : LineReader.readString( promptLine, err );
            }
            catch ( IOException e ) {
                throw new TaskException( "Prompt for value failed" );
            }

            /* If it's a request for help, issue help. */
            if ( "?".equals( sval ) || "help".equalsIgnoreCase( sval ) ) {
                getErrorStream().println();
                getErrorStream().println( getParamHelp( param ) );
            }

            /* Otherwise, try to set the parameter value. */
            else {

                /* Massage it if necessary. */
                String stringVal = sval.length() == 0
                                 ? param.getStringDefault()
                                 : readValue( sval );
                if ( NULL_STRING.equals( stringVal ) ) {
                    stringVal = null;
                }

                /* Attempt to set the parameter's value. */
                try {
                    setValueFromString( param, stringVal );

                    /* If successful, return. */
                    return stringVal;
                }

                /* Otherwise, display the error and try again. */
                catch ( TaskException e ) {
                    getErrorStream().println( e.getMessage() + "\n" );
                }
            }
        }
        throw new ParameterValueException( param, "Too many tries" );
    }

    public void clearValue( Parameter<?> par ) {
        clearedParams_.add( par );
    }

    /**
     * Sets the destination stream for standard out.
     *
     * @param   out   output stream
     */
    public void setOutputStream( PrintStream out ) {
        out_ = out;
    }

    /**
     * Sets the destination stream for standard error.
     *
     * @param   err  error stream
     */
    public void setErrorStream( PrintStream err ) {
        err_ = err;
    }

    public PrintStream getOutputStream() {
        return out_;
    }

    public PrintStream getErrorStream() {
        return err_;
    }

    public void acquireValue( Parameter<?> param ) throws TaskException {

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
            if ( stringVal.equals( NULL_STRING ) ) {
                stringVal = null;
            }
            setValueFromString( param, stringVal );
        }

        /* If no explicit value has been given, we may wish to prompt
         * the user. */
        else if ( interactive_ &&
                  ( promptAll_
                 || ( param.getStringDefault() == null &&
                      ! param.isNullPermitted() )
                 || param.getPreferExplicit() ) ) {
            stringVal = promptForValue( param );
        }

        /* Otherwise, use the default. */
        else {
            stringVal = param.getStringDefault();
            setValueFromString( param, stringVal );
        }

        /* Store the value for logging purposes. */
        String word = formatAssignment( param, stringVal );
        if ( ! acquiredValues_.contains( word ) ) {
            acquiredValues_.add( word );
        }
    }

    /**
     * Invokes <code>setValueFromString</code> on a parameter, checking
     * whether an illegal <code>null</code> is being set.
     *
     * @param   param   parameter whose value is to be set
     * @param   sval    string representation of <code>param</code>'s value
     */
    private void setValueFromString( Parameter<?> param, String sval )
            throws TaskException {
        if ( sval == null && ! param.isNullPermitted() ) {
            throw new ParameterValueException( param,
                                               "null value not allowed" );
        }
        param.setValueFromString( this, sval );
    }

    /**
     * Returns a "name=value" type assignment word.
     *
     * @param  param  parameter
     * @param  value  parameter value
     * @return  formatted assignment string
     */
    private String formatAssignment( Parameter<?> param, String value ) {
        if ( value != null && value.indexOf( ' ' ) >= 0 ) {
            if ( value.indexOf( '\'' ) < 0 ) {
                value = '\'' + value + '\'';
            }
            else if ( value.indexOf( '"' ) < 0 ) {
                value = '"' + value + '"';
            }
        }
        if ( isHidden( param ) ) {
            value = "*";
        }
        return param.getName() + "=" + value;
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
        List<String> unusedList = new ArrayList<String>();
        for ( int i = 0; i < arguments_.length; i++ ) {
            Argument arg = arguments_[ i ];
            if ( ! arg.used_ ) {
                unusedList.add( arg.word_.getText() );
            }
        }
        return unusedList.toArray( new String[ 0 ] );
    }

    /**
     * Returns an array of strings, one for each parameter assignment
     * which was actually used (via {@link #acquireValue})
     * for this environment.
     *
     * @return   array of parameter assignment strings
     */
    public String[] getAssignments() {
        return acquiredValues_.toArray( new String[ 0 ] );
    }

    /**
     * Indicates whether a parameter name supplied from the environment
     * is a reference to a given parameter.
     *
     * <p>The implementation in the <code>LineEnvironment</code> class
     * performs case-insensitive matching against the 
     * <code>param.getName()</code>.  This behaviour may be overridden
     * by subclasses to change the environment's behaviour.
     *
     * @param  envName  parameter name from environment
     * @param  param  parameter
     * @return   true iff <code>envName</code> is considered to name 
     *           <code>param</code>
     */
    public boolean paramNameMatches( String envName, Parameter<?> param ) {
        return param.getName().equalsIgnoreCase( envName );
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
     * Reads a file, providing the result as an array of lines.
     * If the final character in a line is a backslash,
     * the following line is considered as a continuation.
     *
     * @param   location  filename
     * @return   array of lines from <code>location</code>
     */
    private static String[] readLines( String location ) throws IOException {
        InputStream istrm = new FileInputStream( new File( location ) );
        BufferedReader rdr =
           new BufferedReader( new InputStreamReader( istrm ) );
        List<String> lineList = new ArrayList<String>();
        try {
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                int nl = lineList.size();
                String prevLine = nl > 0 ? lineList.get( nl - 1 ) : "";
                if ( prevLine.endsWith( "\\" ) ) {
                    String prevBase =
                        prevLine.substring( 0, prevLine.length() - 1 );
                    lineList.set( nl - 1, prevBase + line );
                }
                else {
                    lineList.add( line );
                }
            }
        }
        finally {
            rdr.close();
        }
        return lineList.toArray( new String[ 0 ] );
    }

    /**
     * Helper class to encapsulate a word found on the command line.
     */
    private class Argument {
        final LineWord word_;
        int pos_;
        boolean used_;
        Argument( LineWord word ) {
            word_ = word;
            pos_ = word.getName() == null
                 ? ++numberedArgs_
                 : -1;
        }
    }
}
