package uk.ac.starlink.ttools.task;

import java.lang.reflect.Array;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.Tokenizer;

/**
 * Parameter which can split its value up into an array of words,
 * each parsed as a parameterised type.
 * Words are generally delimited by whitespace, but can be quoted using
 * single or double quotes as in the shell.
 * You can specify the required number of words.
 *
 * @param    <W> parsed type of each word
 * @author   Mark Taylor
 * @since    1 Sep 2005
 */
public class WordsParameter<W> extends Parameter<W[]> {

    private int nWords_ = -1;
    private final WordParser<W> parser_;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     * @param   arrayClazz   array output class of this parameter
     * @param   parser  converts each input word string to a typed value
     */
    public WordsParameter( String name, Class<W[]> arrayClazz,
                           WordParser<W> parser ) {
        super( name, arrayClazz, false );
        parser_ = parser;
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
     * Returns the parser which is being used to validate and to parse each
     * word in the supplied value string.
     *
     * @return   word parser
     */
    public WordParser<W> getWordParser() {
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
     * Returns the typed value of this parameter.
     * If the required word count value of this parameter is non-negative,
     * then the return value is guaranteed to contain that number of elements.
     *
     * <p>This is just an alias for {@link #objectValue}.
     *
     * @param   env  execution environment
     * @return  array of words constituting the value of this parameter
     */
    public W[] wordsValue( Environment env ) throws TaskException {
        return objectValue( env );
    }

    public W[] stringToObject( Environment env, String sval )
            throws TaskException {
        return parseWords( stringToWords( sval ) );
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
     * Invokes the word parser on each of an array of words.
     *
     * @param  words  string array
     * @return  array of objects matching <code>words</code>
     */
    private W[] parseWords( String[] words ) throws TaskException {
        if ( words == null ) {
            return null;
        }
        int nw = words.length;
        Class<W[]> arrayClazz = getValueClass();
        W[] parsedWords =
            arrayClazz.cast( Array.newInstance( arrayClazz.getComponentType(),
                                                nw ) );
        for ( int iw = 0; iw < nw; iw++ ) {
            try {
                parsedWords[ iw ] = parser_.parseWord( words[ iw ] );
            }
            catch ( TaskException e ) {
                StringBuffer msg = new StringBuffer( e.getMessage() )
                    .append( " in word \"" )
                    .append( words[ iw ] )
                    .append( '"' );
                throw new ParameterValueException( this, msg.toString(), e );
            }
        }
        return parsedWords;
    }

    /**
     * Returns an instance for which words are simply parsed as strings.
     *
     * @param  name  parameter name
     * @return  new WordsParameter
     */
    public static WordsParameter<String>
            createStringWordsParameter( String name ) {
        return new WordsParameter<String>( name, String[].class, s -> s );
    }

    /**
     * Returns an instance for which words are parsed as Integers.
     *
     * @param  name  parameter name
     * @return  new WordsParameter
     */
    public static WordsParameter<Integer>
            createIntegerWordsParameter( String name ) {
        return new WordsParameter<Integer>( name, Integer[].class,
                                            new WordParser<Integer>() {
            public Integer parseWord( String word ) throws TaskException {
                try {
                    return Integer.valueOf( word );
                }
                catch ( NumberFormatException e ) {
                    throw new UsageException( "Bad integer format"
                                            + " \"" + word + "\"",
                                              e );
                }
            }
        } );
    }

    /**
     * Returns an instance for which words are parsed as Doubles.
     *
     * @param  name  parameter name
     * @return  new WordsParameter
     */
    public static WordsParameter<Double>
            createDoubleWordsParameter( String name ) {
        return new WordsParameter<Double>( name, Double[].class,
                                           new WordParser<Double>() {
            public Double parseWord( String word ) throws TaskException {
                try {
                    return Double.valueOf( word );
                }
                catch ( NumberFormatException e ) {
                    throw new UsageException( "Bad number format"
                                            + " \"" + word + "\"",
                                            e );
                }
            }
        } );
    }
}
