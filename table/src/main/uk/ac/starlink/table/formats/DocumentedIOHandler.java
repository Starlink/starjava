package uk.ac.starlink.table.formats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;
import uk.ac.starlink.table.Documented;

/**
 * Marker interface providing some behaviour that should be implemented
 * by table I/O handlers to assist with auto-generating XML user documentation.
 *
 * <p>Handlers implementing this class should moreover provide the
 * {@link uk.ac.starlink.util.ConfigMethod} annotation on all mutator
 * methods that ought to be documented in the user documentation.
 *
 * <p>{@link uk.ac.starlink.table.TableBuilder}s and
 * {@link uk.ac.starlink.table.StarTableWriter}s don't have to implement
 * this class in order to be used for reading/writing tables,
 * but they are expected to if they are going to be one of the handlers
 * installed by default in the I/O controller classes and hence
 * documented in the STIL/STILTS/TOPCAT user documentation.
 *
 * @author   Mark Taylor
 * @since    21 Sept 2020
 */
public interface DocumentedIOHandler extends Documented {

    /**
     * Returns the list of filename extensions recognised by this handler.
     *
     * @return  lower-cased filename extension strings, no "." characters
     */
    String[] getExtensions();

    /**
     * Indicates whether the serialization of some (short) example table
     * should be added to the user documentation for this handler.
     * Binary formats, or instances for which the {@link #getXmlDescription}
     * method already includes some example output, should return false.
     *
     * @return   true if the user documentation would benefit from
     *           the addition of an example serialization
     */
    boolean docIncludesExample();

    /**
     * Utility method to read text from a resource file.
     * UTF-8 encoding is assumed.
     * IOExceptions are rethrown for convenience as RuntimeExceptions.
     *
     * @param  resourceName  name of resource relative to the class
     *                       of this object
     * @return   content of resource file as a string
     * @throws  RuntimeException  if the resource doesn't exist or
     *          something else goes wrong
     */
    default String readText( String resourceName ) {
        try {
            URL url = getClass().getResource( resourceName );
            try ( BufferedReader rdr =
                      new BufferedReader(
                          new InputStreamReader(
                              url.openStream(), "UTF-8" ) ) ) {
                return rdr.lines().collect( Collectors.joining( "\n" ) );
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Failed to read resource "
                                      + resourceName, e );
        }
    }

    /**
     * Utility method that returns true if the supplied filename
     * has one of the extensions associated with a given handler
     * (ends with a "." plus the extension), matched case-insensitively.
     *
     * @param  handler   handler with extensions
     * @param  filename   name to test
     * @return   true iff filename has a recognised extension
     */
    static boolean matchesExtension( DocumentedIOHandler handler,
                                     String filename ) {
        int iext = filename.lastIndexOf( '.' );
        return iext > 0
            && Arrays.asList( handler.getExtensions() )
                     .contains( filename.substring( iext + 1 ).toLowerCase() );
    }

    /**
     * Utility method that returns the text of an XML &lt;A&gt; element
     * whose href and content are both given by a supplied URL.
     *
     * @param  url  URL text
     * @return  &lt;a href='url'&gt;url&lt;/a&gt;
     */
    static String toLink( String url ) {
        return new StringBuffer()
              .append( "<a href='" )
              .append( url )
              .append( "'>" )
              .append( url )
              .append( "</a>" )
              .toString();
    }
}
