package uk.ac.starlink.ttools;

import uk.ac.starlink.table.ValueInfo;
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

    /**
     * Returns an string listing the supplied array of metadata objects.
     * The returned string should be suitable for inserting into XML text.
     *
     * @param  infos  array of infos
     * @return  string listing <code>infos</code> by name
     */
    public static String listInfos( ValueInfo[] infos ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "<ul>\n" );
        for ( int i = 0; i < infos.length; i++ ) {
            sbuf.append( "<li>" )
                .append( "<code>" )
                .append( infos[ i ].getName() )
                .append( "</code>" )
                .append( ": " )
                .append( infos[ i ].getDescription() )
                .append( "</li>" );
        }
        sbuf.append( "</ul>\n" );
        return sbuf.toString();
    }
}
