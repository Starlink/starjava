package uk.ac.starlink.hapi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describes basic information about a HAPI server.
 *
 * @author   Mark Taylor
 * @since    12 Jan 2024
 */
public abstract class ServerMeta {

    private static ServerMeta[] servers_;

    /**
     * Location of list of known HAPI servers.
     *
     * @see <a href="https://github.com/hapi-server/servers"
     *              >https://github.com/hapi-server/servers</a>
     */
    public static final String SERVER_LIST_URL =
        "https://raw.githubusercontent.com/hapi-server/servers/master/all_.txt";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    /**
     * Returns the base URL of the HAPI service.
     *
     * @return  base URL, legal URL, not null
     */
    public abstract String getUrl();

    /**
     * Returns the name of this service.
     *
     * @return  service name, may be null
     */
    public abstract String getName();

    /**
     * Returns the title of this service.
     *
     * @return  service title, may be null
     */
    public abstract String getTitle();

    /**
     * Returns the name of a person who can be contacted about this service.
     *
     * @return  contact person name, may be null
     */
    public abstract String getContact();

    /**
     * Returns the email address for contact regarding this service.
     *
     * @return  contact email, may be null
     */
    public abstract String getEmail();

    /**
     * Returns a list of known servers.
     *
     * @return  server list
     */
    public static ServerMeta[] getServers() {
        if ( servers_ == null ) {
            servers_ = readServers();
        }
        return servers_.clone();
    }

    /**
     * Obtains the list of known servers from an external source.
     *
     * @return  server list
     */
    private static ServerMeta[] readServers() {
        String[] lines;
        logger_.info( "Reading HAPI servers from " + SERVER_LIST_URL );
        try ( BufferedReader in =
                  new BufferedReader(
                      new InputStreamReader(
                          new URL( SERVER_LIST_URL ).openStream() ) ) ) {
            lines = in.lines().toArray( n -> new String[ n ] );
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "HAPI server list read failed", e );
            lines = new String[] {
                "https://cdaweb.gsfc.nasa.gov/hapi",
                "https://imag-data.bgs.ac.uk/GIN_V1/hapi",
                "http://hapi-server.org/servers/SSCWeb/hapi",
                "https://iswa.gsfc.nasa.gov/IswaSystemWebApp/hapi",
                "http://lasp.colorado.edu/lisird/hapi",
                "http://hapi-server.org/servers/TestData2.0/hapi",
                "https://amda.irap.omp.eu/service/hapi",
                "https://vires.services/hapi",
                "https://api.helioviewer.org/hapi/Helioviewer/hapi",
            };
        }
        return parseServerLines( lines );
    }

    /**
     * Decodes a list of servers in the ad hoc CSV format used at time
     * of writing by the file at
     * https://github.com/hapi-server/servers/blob/master/all_.txt
     *
     * @param  lines of text
     * @return  list of servers
     */
    private static ServerMeta[] parseServerLines( String[] lines ) {
        List<ServerMeta> list = new ArrayList<>();
        for ( String line : lines ) {
            String[] words = line.split( " *, *", -1 );
            int nw = words.length;
            if ( nw > 0 ) {
                String url = words[ 0 ];
                boolean urlOk;
                try {
                    new URL( url );
                    urlOk = true;
                }
                catch ( MalformedURLException e ) {
                    urlOk = false;
                }
                if ( urlOk ) {
                    String name = nw >= 5 ? words[ 1 ] : null;
                    String title = nw >= 5 ? words[ 2 ] : null;
                    String contact = nw >= 5 ? words[ 3 ] : null;
                    String email = nw >= 5 ? words[ 4 ] : null;
                    list.add( new ServerMeta() {
                        public String getUrl() {
                            return url;
                        }
                        public String getName() {
                            return name;
                        }
                        public String getTitle() {
                            return title;
                        }
                        public String getContact() {
                            return contact;
                        }
                        public String getEmail() {
                            return email;
                        }
                    } );
                }
            }
        }
        return list.toArray( new ServerMeta[ 0 ] );
    }
}
