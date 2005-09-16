package uk.ac.starlink.ttools;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.task.UsageException;

/**
 * Utility class to help with tokenizing strings.
 *
 * @author   Mark Taylor
 * @since    1 Sep 2005
 */
public class Tokenizer {

    /**
     * Private sole constructor.
     */
    private Tokenizer() {
    }

    /**
     * Splits a string up into lines, separated by semicolons or newlines.
     * Semicolons may appear inside quoted strings without terminating
     * a line.
     *
     * @param  text  input string
     * @return  array of lines
     */
    public static String[] tokenizeLines( String text ) throws UsageException {
        List tokenList = new ArrayList();
        String text1 = text + ";";
        char delim = 0;
        StringBuffer token = new StringBuffer();
        for ( int i = 0; i < text1.length(); i++ ) {
            char chr = text1.charAt( i );
            switch ( chr ) {
                case ';':
                case '\n':
                    if ( delim == 0 ) {
                        tokenList.add( token.toString() );
                        token = new StringBuffer();
                    }
                    else {
                        token.append( chr );
                    }
                    break;
                case '\'':
                case '"':
                    if ( delim == chr ) {
                        delim = 0;
                    }
                    else if ( delim == 0 ) {
                        delim = chr;
                    }
                    token.append( chr );
                    break;
                default:
                    token.append( chr );
            }
        }
        if ( token.length() > 0 || delim != 0 ) {
            throw new UsageException( "Badly formed input: " + text );
        }
        return (String[]) tokenList.toArray( new String[ 0 ] );
    }

    /**
     * Chops up a line of text into tokens.
     * Works roughly like the shell, as regards quotes, whitespace and
     * comments.
     *
     * @param  line  line of text
     * @return  array of words corresponding to <code>line</code>
     */
    public static String[] tokenizeWords( String line ) throws UsageException {
        String line1 = line + '\n';
        List tokenList = new ArrayList();
        StringBuffer token = null;
        char delim = 0;
        boolean done = false;
        for ( int i = 0; i < line1.length() && ! done; i++ ) {
            char chr = line1.charAt( i );
            switch ( chr ) {
                case '#':
                    if ( delim == 0 ) {
                        done = true;
                    }
                    else {
                        token.append( chr );
                    }
                    break;
                case ' ':
                case '\t':
                case '\n':
                    if ( token != null ) {
                        if ( delim == 0 ) {
                            tokenList.add( token.toString() );
                            token = null;
                        }
                        else {
                            token.append( chr );
                        }
                    }
                    break;
                case '\'':
                case '"':
                    if ( token == null ) {
                        token = new StringBuffer();
                        delim = chr;
                    }
                    else if ( delim == chr ) {
                        tokenList.add( token.toString() );
                        token = null;
                        delim = 0;
                    }
                    else if ( delim == 0 ) {
                        delim = chr;
                    }
                    else {
                        token.append( chr );
                    }
                    break;
                case '\\':
                    if ( i == line1.length() - 1 ) {
                        throw new UsageException( "Backslash illegal " +
                                                  "at end of line" );
                    }
                    if ( token == null ) {
                        token = new StringBuffer();
                    }
                    token.append( line1.charAt( ++i ) );
                    break;
                default:
                    if ( token == null ) {
                        token = new StringBuffer();
                    }
                    token.append( chr );
            }
        }
        if ( token != null || delim != 0 ) {
            throw new UsageException( "Badly formed line: " + line );
        }
        return (String[]) tokenList.toArray( new String[ 0 ] );
    }
}
