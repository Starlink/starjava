package uk.ac.starlink.hapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.util.IOSupplier;

/**
 * Listing for HAPI services.
 *
 * @author   Mark Taylor
 * @since    15 Jan 2024
 */
public class ServerListing {

    private final String name_;
    private final IOSupplier<String[]> lineSupplier_;
    private ServerMeta[] servers_;

    private static final String SERVERS_URL =
        "https://raw.githubusercontent.com/hapi-server/servers/master/";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.hapi" );

    /** Central list of production servers without ad-hoc metadata. */
    public static final ServerListing ALL;

    /** Central list of production servers with ad-hoc metadata. */
    public static final ServerListing ALL_;

    /** Central list of development servers. */
    public static final ServerListing DEV;

    /** Fallback hard-coded list, does not rely on a service. */
    public static final ServerListing FALLBACK;

    /** Known listings. */
    public static final ServerListing[] LISTINGS = new ServerListing[] {
        ALL_ = createUrlListing( "All + Metadata",
                                    SERVERS_URL + "all_.txt" ),
        ALL = createUrlListing( "All", SERVERS_URL + "all.txt" ),
        DEV = createUrlListing( "Dev", SERVERS_URL + "dev.txt" ),
        FALLBACK = createFixedListing( "Fallback" ),
    };

    /**
     * Constructor.
     * A supplier for lines is required.
     * At minimum, this can be a list of service base URLs.
     * If the additional 6-field metadata as used by all_.txt is available,
     * some attempt will be made to use it.
     *
     * @param   name  listing name
     * @param   lineSupplier  list of text lines describing servers
     */
    private ServerListing( String name, IOSupplier<String[]> lineSupplier ) {
        name_ = name;
        lineSupplier_ = lineSupplier;
    }

    /**
     * Returns the list of servers held by this object.
     * In case of a read error, it may be an empty array.
     *
     * @return  server list
     */
    public ServerMeta[] getServers() {
        if ( servers_ == null ) {
            String[] lines;
            try {
                lines = lineSupplier_.get();
            }
            catch ( IOException e ) {
                logger_.log( Level.WARNING, "Failed to load HAPI server list",
                             e );
                lines = new String[ 0 ];
            }
            servers_ = parseServerLines( lines );
        }
        return servers_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a listing got from reading data from a given URL.
     *
     * @param  name  listing name
     * @param  url   URL at which server listing can be found
     * @return  new listing
     */
    private static ServerListing createUrlListing( String name, String url ) {
        return new ServerListing( name, () -> {
            logger_.info( "Reading HAPI servers from " + url );
            try ( BufferedReader in =
                      new BufferedReader(
                          new InputStreamReader(
                              new URL( url ).openStream() ) ) ) {
                return in.lines().toArray( n -> new String[ n ] );
            }
        } );
    }

    /**
     * Returns a fallback list from hard-coded data.
     *
     * @param  name  listing name
     * @return  new listing
     */
    private static ServerListing createFixedListing( String name ) {
        String[] lines = new String[] {
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
        return new ServerListing( name, () -> lines );
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
                    String name = nw >= 2 ? words[ 1 ] : words[ 0 ];
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
