package uk.ac.starlink.topcat.soap;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Defines the SOAP services offered by the TOPCAT application.
 *
 * @author  Mark Taylor (Starlink)
 * @since   23 Mar 2005
 */
public class TopcatSOAPServices {

    /**
     * Displays a table in the TOPCAT application given its location.
     *
     * @param   cookie  cookie for this server
     * @param   location  table location (filename or URL)
     * @param   handler  name of table handler (or null for auto-detect)
     * @throws  IOException   if <tt>location</tt> doesn't name a table etc
     */
    public static void displayTableByLocation( String cookie, String location,
                                               String handler )
            throws IOException {
        TopcatSOAPServer.getInstance()
                        .displayTableByLocation( cookie, location, handler );
    }

    /**
     * Displays a table directly in the TOPCAT application.
     * The SOAP transport is done using custom VOTable serialization.
     *
     * @param   cookie  cookie for this server
     * @param   table   table to display
     * @param   location  string indicating the table's source
     */
    public static void displayTable( String cookie, StarTable table,
                                     String location ) throws IOException {
        TopcatSOAPServer.getInstance()
                        .displayTable( cookie, table, location );
    }
}
