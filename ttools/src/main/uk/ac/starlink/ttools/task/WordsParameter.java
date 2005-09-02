package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
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
public class WordsParameter extends Parameter {

    private int nWords_ = -1;

    /**
     * Constructor.
     *
     * @param   name  parameter name
     */
    public WordsParameter( String name ) {
        super( name );
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
     * Returns the value of this parameter as an array of words.
     * If the required word count value of this parameter is non-negative,
     * then the return value is guaranteed to contain that number of elements.
     *
     * @param   env  execution environment
     * @return  array of words constituting the value of this parameter
     */
    public String[] wordsValue( Environment env ) throws TaskException {
        checkGotValue( env );
        String sval = stringValue( env );
        String[] words = Tokenizer.tokenizeWords( sval );
        if ( nWords_ >= 0 && nWords_ != words.length ) {
            StringBuffer sbuf = new StringBuffer();
            sbuf.append( "Wrong number of words in parameter " )
                .append( getName() )
                .append( " - wanted " )
                .append( nWords_ )
                .append( ", got " )
                .append( words.length )
                .append( '\n' )
                .append( "Value was: " )
                .append( sval )
                .append( '\n' )
                .append( "Words containing spaces must be 'quoted' " )
                .append( "or \"quoted\"\n" ); 
            throw new UsageException( sbuf.toString() );
        }
        return words;
    }
}
