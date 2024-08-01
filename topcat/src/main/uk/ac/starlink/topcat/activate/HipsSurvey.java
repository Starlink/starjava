package uk.ac.starlink.topcat.activate;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.ac.starlink.util.ContentCoding;

/**
 * This class characterises a HiPS survey corresponding to an existing
 * service, and provides static methods for acquiring a list of
 * such services.
 *
 * @see  <a href="http://www.ivoa.net/documents/HiPS/">HiPS 1.0</a>
 */
public class HipsSurvey {

    public static final String MOC_SERVER =
        "http://alasky.unistra.fr/MocServer/query";
    public static final String MOC_SERVER2 =
        "http://alaskybis.unistra.fr/MocServer/query";

    private static final String KEY_CREATOR_DID;
    private static final String KEY_HIPS_TILE_FORMAT;
    private static final String KEY_HIPS_FRAME;
    private static final String KEY_OBS_TITLE;
    private static final String KEY_OBS_REGIME;
    private static final String KEY_CLIENT_CATEGORY;
    private static final String KEY_CLIENT_SORT_KEY;
    private static final String KEY_MOC_SKY_FRACTION;

    private static final String[] READ_FIELDS = {
        KEY_CREATOR_DID = "creator_did",
        KEY_HIPS_TILE_FORMAT = "hips_tile_format",
        KEY_HIPS_FRAME = "hips_frame",
        KEY_OBS_TITLE = "obs_title",
        KEY_OBS_REGIME = "obs_regime",
        KEY_CLIENT_CATEGORY = "client_category",
        KEY_CLIENT_SORT_KEY = "client_sort_key",
        KEY_MOC_SKY_FRACTION = "moc_sky_fraction",
    };
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.misc" );
    private static final Downloader<HipsSurvey[]> imageHipsListDownloader_ =
        createImageHipsListDownloader();

    private final String creatorDid_;
    private final String obsTitle_;
    private final Set<String> tileFormats_;
    private final String hipsFrame_;
    private final String obsRegime_;
    private final String clientCategory_;
    private final String clientSortKey_;
    private final double mocSkyFraction_;
    private final ObsRegime obsRegimeCategory_;
    private final String shortName_;

    /**
     * Constructs a HipsSurvey object based on a map representing
     * (some or all of) the HiPS properties.
     * These properties are defined in the HiPS 1.0 standard.
     *
     * @param   map  HiPS properties as a map
     */
    @SuppressWarnings("this-escape")
    public HipsSurvey( Map<String,String> map ) {
        creatorDid_ = map.get( KEY_CREATOR_DID );
        obsTitle_ = map.get( KEY_OBS_TITLE );
        String tftxt = map.get( KEY_HIPS_TILE_FORMAT );
        tileFormats_ = new LinkedHashSet<String>();
        if ( tftxt != null ) {
            tileFormats_.addAll( Arrays.asList( tftxt.split( " +" ) ) );
        }
        hipsFrame_ = map.get( KEY_HIPS_FRAME );
        clientCategory_ = map.get( KEY_CLIENT_CATEGORY );
        clientSortKey_ = map.get( KEY_CLIENT_SORT_KEY );
        obsRegime_ = map.get( KEY_OBS_REGIME );
        String fractxt = map.get( KEY_MOC_SKY_FRACTION );
        double frac;
        if ( fractxt != null ) {
            try {
                frac = Double.parseDouble( fractxt );
            }
            catch ( NumberFormatException e ) {
                frac = Double.NaN;
            }
        }
        else {
            frac = Double.NaN;
        }
        mocSkyFraction_ = frac;
        obsRegimeCategory_ = parseObsRegime( obsRegime_ );
        final String shortName;
        if ( creatorDid_ == null ) {
            shortName = "HipsSurvey" + System.identityHashCode( this );
        }
        else {
            int pIndex = creatorDid_.indexOf( "/P/" );
            if ( pIndex >= 0 ) {
                shortName = creatorDid_.substring( pIndex + 3 );
            }
            else if ( creatorDid_.startsWith( "ivo://" ) ) {
                shortName = creatorDid_.substring( 6 );
            }
            else {
                shortName = creatorDid_;
            }
        }
        shortName_ = shortName;
    }

    /**
     * Value of creator_did property.
     *
     * @return  creator_did
     */
    public String getCreatorDid() {
        return creatorDid_;
    }

    /**
     * Value of obs_title property.
     *
     * @return  obs_title
     */
    public String getObsTitle() {
        return obsTitle_;
    }

    /**
     * Value of obs_regime property.
     *
     * @return  obs_regime
     */
    public String getObsRegime() {
        return obsRegime_;
    }

    /**
     * Value of client_category property.
     *
     * @return  client_category
     */
    public String getClientCategory() {
        return clientCategory_;
    }

    /**
     * Value of client_sort_key property.
     *
     * @return  client_sort_key
     */
    public String getClientSortKey() {
        return clientSortKey_;
    }

    /**
     * Value of the hips_tile_formats property as a set of words.
     *
     * @return  hips_tile_formats as a set
     */
    public Set<String> getTileFormats() {
        return tileFormats_;
    }

    /**
     * Value of the hips_frame property.
     *
     * @return  hips_frame
     */
    public String getHipsFrame() {
        return hipsFrame_;
    }

    /**
     * Value of the moc_sky_fraction property as a double.
     *
     * @return   moc_sky_fraction in range 0..1, or NaN if not known
     */
    public double getMocSkyFraction() {
        return mocSkyFraction_;
    }

    /**
     * Indicates whether this survey has FITS data.
     *
     * @return  true iff <code>getTileFormats().contains("fits")</code>
     */
    public boolean hasFits() {
        return tileFormats_.contains( "fits" );
    }

    /**
     * Indicates whether this survey apparently contains sky data.
     * If not, it probably represents some kind of solar system object.
     *
     * @return  true iff getHipsFrame() looks like a sky frame
     */
    public boolean isSky() {
        return "equatorial".equals( hipsFrame_ )
            || "galactic".equals( hipsFrame_ )
            || "ecliptic".equals( hipsFrame_ );
    }

    /**
     * Returns an ObsRegime enum entry describing the obs_regime of this
     * survey.  If none of the known obs_regimes has been named,
     * null is returned.
     *
     * @return  known observation regime, or null
     */
    public ObsRegime getObsRegimeCategory() {
        return obsRegimeCategory_;
    }

    /**
     * Returns a hierarchical form of this survey's identifier.
     * This is somewhat ad-hoc, but it is intended to form the basis
     * for a hierarchical representation of a list of HiPS surveys.
     * The first element of the path is the root, more specific
     * items later on in the list.
     *
     * @return  hierarchical representation of HiPS identifier
     */
    public String[] getPath() {
        List<String> path = new ArrayList<String>();

        /* First divide into sky and other based on hips_frame. */
        if ( "equatorial".equals( hipsFrame_ ) ||
             "galactic".equals( hipsFrame_ ) ) {
            path.add( "Sky" );
            ObsRegime regime = obsRegimeCategory_;
            path.add( regime == null ? "??" : regime.toString() );
            if ( obsRegime_ != null && obsRegime_.trim().length() > 0 ) {
                path.add( obsRegime_ );
            }
        }
        else {
            path.add( "Other" );
            path.add( hipsFrame_ == null ? "??" : hipsFrame_ );
        }

        /* Add hierachical elements based on the supplied client_category
         * if present, otherwise using the creator_did. */
        if ( clientCategory_ != null && clientCategory_.indexOf( '/' ) >= 0 ) {
            path.addAll( Arrays.asList( clientCategory_.split( "/" ) ) );
            if ( creatorDid_ != null && creatorDid_.trim().length() > 0 ) {
                path.add( creatorDid_ );
            }
        }
        else {
            String cid = creatorDid_;
            if ( cid == null ) {
                return null;
            }
            final String ident;
            int pIndex = cid.indexOf( "/P/" );
            if ( pIndex >= 0 ) {
                ident = cid.substring( pIndex + 3 );
            }
            else if ( cid.startsWith( "ivo://" ) ) {
                ident = cid.substring( 6 );
            }
            else {
                ident = cid;
            }
            path.addAll( Arrays.asList( ident.split( "/" ) ) );
        }

        /* Remove ad hoc some known not-very-informative elements. */
        for ( Iterator<String> it = path.iterator(); it.hasNext(); ) {
            String el = it.next();
            if ( "image".equalsIgnoreCase( el ) ||
                 "solar system".equalsIgnoreCase( el ) ) {
                it.remove();
            }
        }

        /* Remove any element that is repeated in the path. */
        List<String> plist = new ArrayList<String>();
        Set<String> elSet = new HashSet<String>();
        for ( String el : path ) {
            if ( elSet.add( el.toLowerCase() ) ) {
                plist.add( el );
            }
        }

        /* Return path. */
        return plist.toArray( new String[ 0 ] );
    }

    /**
     * Returns an abbreviated name for this hips survey.
     * This is usually a shortened form of the creator_did that should be
     * somewhat human readable and should also be recognisable by
     * services.
     *
     * @return  service name
     */
    public String getShortName() {
        return shortName_;
    }

    @Override
    public String toString() {
        return shortName_;
    }

    /**
     * Returns a downloader for a list of all the HiPS image surveys.
     * The downloader is not started by this method.
     *
     * @return  downloader for all surveys suitable for hips2fits
     */
    public static Downloader<HipsSurvey[]> getImageHipsListDownloader() {
        return imageHipsListDownloader_;
    }

    /**
     * Creates a downloader for all HiPS image surveys.
     *
     * @param  new downloader
     */
    private static Downloader<HipsSurvey[]> createImageHipsListDownloader() {

        /* This URL follows advice from Pierre Fernique (CDS) and is also
         * a result of some of my experimentation.  I hope it's stable.
         * Note some useful queries for exploring available fields are
         *    http://alasky.unistra.fr/MocServer/query?get=example
         *    http://alasky.unistra.fr/MocServer/query?<id>&fields=*
         */
        final Map<String,String> params = new LinkedHashMap<String,String>();
        params.put( "dataproduct_type", "image" );
        return new Downloader<HipsSurvey[]>( "HiPS survey list",
                                             new Callable<HipsSurvey[]>() {
            public HipsSurvey[] call() throws IOException {
                return readSurveys( MOC_SERVER, params, ContentCoding.GZIP );
            }
        } );
    }

    /**
     * Reads a list of HiPS surveys for which hips2fits works from a MocServer.
     * The MocServer interface does not appear to be documented anywhere,
     * but I believe it's used internally by Aladin, so is hopefully
     * reasonably stable.  One day this should be replaced by
     * registry queries, but the registry content is not sufficiently
     * complete at time of writing.
     *
     * @param  mocServerUrl  base URL of MocServer
     * @param  extraParams   name-&gt;value map giving custom query parameters
     * @param  coding   content-coding
     * @return   survey list
     */
    public static HipsSurvey[] readSurveys( String mocServerUrl,
                                            Map<String,String> extraParams,
                                            ContentCoding coding )
            throws IOException {

        /* Construct query URL. */
        StringBuffer fieldsBuf = new StringBuffer();
        for ( String key : READ_FIELDS ) {
            if ( fieldsBuf.length() > 0 ) {
                fieldsBuf.append( "," );
            }
            fieldsBuf.append( key );
        }
        Map<String,String> params = new LinkedHashMap<String,String>();
        params.putAll( extraParams );
        params.put( "fmt", "json" );
        params.put( "fields", fieldsBuf.toString() );
        StringBuffer qbuf = new StringBuffer();
        for ( Map.Entry<String,String> entry : params.entrySet() ) {

            /* I'm not URL-encoding characters here because mocServer
             * doesn't seem to need it, and it makes the URL look messy.
             * But careful what goes in here. */
            qbuf.append( qbuf.length() == 0 ? "?" : "&" )
                .append( entry.getKey() )
                .append( "=" )
                .append( entry.getValue() );
        }
        URL url = new URL( mocServerUrl + qbuf.toString() );

        /* Read JSON content and parse into a list of HipsSurvey objects. */
        logger_.info( "Loading HiPS list from " + url );
        InputStream in = new BufferedInputStream( coding.openStream( url ) );
        try {
            JSONTokener jt = new JSONTokener( in );
            Object next = jt.nextValue();
            if ( next instanceof JSONArray ) {
                List<HipsSurvey> list = new ArrayList<HipsSurvey>();
                JSONArray array = (JSONArray) next;
                int n = array.length();
                for ( int i = 0; i < n; i++ ) {
                    Object item = array.get( i );
                    if ( item instanceof JSONObject ) {
                        list
                       .add( new HipsSurvey( toMap( (JSONObject) item ) ) );
                    }
                }
                logger_.info( "Loaded " + n + " HiPS" );
                return list.toArray( new HipsSurvey[ 0 ] );
            }
            else {
                throw new IOException( "Unexpected JSON object from " + url );
            }
        }
        finally {
            in.close();
        }
    }

    /**
     * Utility method to turn a JSONObject into a String-&gt;String map
     * on a best-efforts basis.
     *
     * @param  jobj   JSON object
     * @return  map containing scalar content of jobj
     */
    private static final Map<String,String> toMap( JSONObject jobj ) {
        Map<String,String> map = new LinkedHashMap<String,String>();
        for ( String key : JSONObject.getNames( jobj ) ) {
            Object val = jobj.get( key );
            if ( val instanceof String ) {
                map.put( key, (String) val ); 
            }
        }
        return map;
    }

    /**
     * Attempts to work out what ObsRegime corresponds to a given string.
     * Exact match is not required.
     *
     * @param  txt  regime description
     * @return  regime, or null if not known
     */
    private static ObsRegime parseObsRegime( String txt ) {
        if ( txt != null ) {
            txt = txt.trim().toLowerCase();
            for ( ObsRegime regime : ObsRegime.values() ) {
                String rname = regime.toString().toLowerCase();
                if ( txt.startsWith( rname ) || txt.endsWith( rname ) ) {
                    return regime;
                }
            }
        }
        return null;
    }

    /**
     * Known obs_regime values.
     * The toString values of these items are as documented under
     * the "obs_regime" keyword in the HiPS specification.
     */
    public enum ObsRegime {

        RADIO("Radio"),
        MILLIMETER("Millimeter"),
        INFRARED("Infrared"),
        OPTICAL("Optical"),
        UV("UV"),
        EUV("EUV"),
        X_RAY("X-ray"),
        GAMMA_RAY("Gamma-ray");

        private final String txt_;

        /**
         * Constructor.
         *
         * @param  txt  string value as specified in HiPS doc
         */
        ObsRegime( String txt ) {
            txt_ = txt;
        }

        @Override
        public String toString() {
            return txt_;
        }

        /**
         * Returns the ObsRegime instance corresponding exactly to
         * a given string.  This string is as specified in the HiPS
         * specification.
         *
         * @param   txt  regime name
         * @return  regime, or null
         */
        public static ObsRegime fromName( String txt ) {
            for ( ObsRegime regime : values() ) {
                if ( txt.equals( regime.toString() ) ) {
                    return regime;
                }
            }
            return null;
        }
    }
}
