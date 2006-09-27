package uk.ac.starlink.ttools;

import uk.ac.starlink.ttools.filter.BasicFilter;

/**
 * Utilities used for automatically-generated documentation.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2006
 */
public class DocUtils {

    /**
     * Private sole constructor prevents instantiation.
     */
    private DocUtils() {
    }


    /**
     * Concatenates an array of strings, appending a carriage return
     * to each one.
     *
     * @param   lines  array of input strings
     * @return  one long output string
     */
    public static String join( String[] lines ) {
        StringBuffer sbuf = new StringBuffer();
        for ( int i = 0; i < lines.length; i++ ) {
            sbuf.append( lines[ i ] )
                .append( '\n' );
        }
        return sbuf.toString();
    }

    /**
     * Provides a snippet of XML which references a processing filter.
     *
     * @param  filter  processing filter
     * @return  filter reference
     */
    public static String filterRef( BasicFilter filter ) {
        String name = filter.getName();
        return new StringBuffer()
            .append( "<code>" )
            .append( "<ref id=\"" )
            .append( name )
            .append( "\">" )
            .append( name )
            .append( "</ref>" )
            .append( "</code>" )
            .toString();
    }

    /**
     * Provides a snippet of XML which references a named
     * {@link uk.ac.starlink.ttools.mode.ProcessingMode}.
     *
     * @param name  mode name
     * @return  mode reference
     */
    public static String modeRef( String name ) {
        return new StringBuffer()
            .append( "<code>" )
            .append( "<ref id=\"mode-" )
            .append( name )
            .append( "\">" )
            .append( name )
            .append( "</ref>" )
            .append( "</code>" )
            .toString();
    }
}
