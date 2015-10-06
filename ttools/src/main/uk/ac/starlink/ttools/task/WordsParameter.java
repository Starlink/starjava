package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.Tokenizer;

/**
 * Parameter which can split its value up into an array of words.
 * Words are generally delimited by whitespace, but can be quoted using
 * single or double quotes as in the shell.
 * You can specify the required number of words.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2005
 */
public class WordsParameter extends Parameter<String[]> {

    private int nWords_ = -1;
    private WordParser parser_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public WordsParameter( String name ) {
        super( name, String[].class, false );
    }

    /**
     * Sets the number of words required from this parameter.
     * If it is -1 (the default), then no restrictions are placed on
     * the number of words.  Otherwise, attempting to set the value
     * to the wrong number of words is an error.
     *
     * @param   nWords  required number of words
     */
    public void setRequiredWordCount( int nWords ) {
        nWords_ = nWords;
    }

    /**
     * Returns the number of words required from this parameter.
     * If it is -1 (the default), then no restrictions are placed on
     * the number of words.  Otherwise, attempting to set the value
     * to the wrong number of words is an error.
     *
     * @return  required number of words
     */
    public int getRequiredWordCount() {
        return nWords_;
    }

    /**
     * Sets a parser which will be used to validate and to parse each
     * word in the supplied value string.  If null, no validation is 
     * performed and the parsed words are just the tokenized strings.
     *
     * @param  parser   word parser to install
     */
    public void setWordParser( WordParser parser ) {
        parser_ = parser;
    }

    /**
     * Returns the parser which is being used to validate and to parse each
     * word in the supplied value string.  If null, no validation is being
     * performed and the parsed words are just the tokenized strings.
     *
     * @return   word parser
     */
    public WordParser getWordParser() {
        return parser_;
    }

    /**
     * Configures the usage of this parameter from usages for given words.
     *
     * @param  wordUsage  per-word usage
     */
    public void setWordUsage( String wordUsage ) {
        StringBuffer ubuf = new StringBuffer( wordUsage );
        if ( nWords_ > 0 ) {
            for ( int i = 1; i < nWords_; i++ ) {
                ubuf.append( ' ' ).append( wordUsage );
            }
        }
        else {
            ubuf.append( " ..." );
        }
        setUsage( ubuf.toString() );
    }

    /**
     * If the required word count value of this parameter is non-negative,
     * then the return value is guaranteed to contain that number of elements.
     *
     * @param   env  execution environment
     * @return  array of words constituting the value of this parameter
     */
    public String[] wordsValue( Environment env ) throws TaskException {
        return objectValue( env );
    }

    /**
     * Returns the value of this parameter as an array of objects which have
     * resulted from the parsing of the {@link #wordsValue} using the
     * currently installed <code>WordParser</code>.  If no word parser is
     * installed, this will have the same contents (Strings) as the words.
     * If the required word count value of this parameter is non-negative,
     * then the return value is guaranteed to contain that number of elements.
     *
     * @param   env  execution environment
     * @return  array of objects representing the value of this parameter
     */
    public Object[] parsedWordsValue( Environment env ) throws TaskException {
        return parseWords( objectValue( env ) );
    }

    public String[] stringToObject( Environment env, String sval )
            throws TaskException {
        String[] svals = stringToWords( sval );
        parseWords( svals );  // validation
        return svals;
    }

    /**
     * Splits a given string value into words.
     * A TaskException is thrown if the number of words does not match
     * the required constraints.
     *
     * @param  sval  string value
     * @return   word array
     */
    private String[] stringToWords( String sval ) throws TaskException {
        String[] words = Tokenizer.tokenizeWords( sval );
        if ( nWords_ >= 0 && nWords_ != words.length ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "Wrong number of words - wanted " )
                .append( nWords_ )
                .append( ", got " )
                .append( words.length );
            throw new ParameterValueException( this, sbuf.toString() );
        }
        return words;
    }

    /**
     * Invokes the installed word parser, if any, on each of an array of words.
     *
     * @param  words  string array
     * @return  array of objects matching <code>words</code>
     */
    private Object[] parseWords( String[] words ) throws TaskException {
        if ( words == null ) {
            return null;
        }
        WordParser parser = getWordParser();
        if ( parser != null ) {
            Object[] parsedWords = new Object[ words.length ];
            for ( int i = 0; i < words.length; i++ ) {
                try {
                    parsedWords[ i ] = parser.parseWord( words[ i ] );
                }
                catch ( TaskException e ) {
                    StringBuffer msg = new StringBuffer( e.getMessage() )
                        .append( " in word \"" )
                        .append( words[ i ] )
                        .append( '"' );
                    throw new ParameterValueException( this, msg.toString(),
                                                       e );
                }
            }
            return parsedWords;
        }
        else {
            return words;
        }
    }
}
