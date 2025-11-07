// The doc comments in this class are processed to produce user-visible
// documentation as part of the package build process.  For this reason
// care should be taken to make the doc comment style comprehensible,
// consistent, concise, and not over-technical.

package uk.ac.starlink.ttools.func;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.starlink.ttools.calc.WebMapper;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.util.CgiQuery;

/**
 * Functions that construct URLs for external services.
 * Most of the functions here just do string manipulation to build up
 * URL strings, using knowledge of the parameters required for
 * various services.
 *
 * @author   Mark Taylor
 * @since    18 Oct 2019
 */
public class URLs {

    /** Legal characters for the data part of a URI - see RFC 3986. */
    private static final String URI_DATA_CHARS =
        "abcdefghijklmnopqrstuvwxyz" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        "0123456789" +
        "-_.~";

    /** Base URL for CDS hips2fits service. */
    private static final String HIPS2FITS_BASE =
        "https://alasky.cds.unistra.fr/hips-image-services/hips2fits";

    /** Alternative base URL for CDS hips2fits service. */
    private static final String HIPS2FITS_BASE2 =
        "https://alaskybis.cds.unistra.fr/hips-image-services/hips2fits";

    /**
     * Private constructor prevents instantiation.
     */
    private URLs() {
    }

    /**
     * Performs necessary quoting of a string for it to be included
     * safely in the data part of a URL.
     * Alphanumeric characters and the characters
     * underscore ("<code>_</code>"),
     * minus sign ("<code>-</code>"),
     * period ("<code>.</code>") and
     * tilde ("<code>~</code>") are passed through unchanged,
     * and any other 7-bit ASCII character is represented by a percent sign
     * ("<code>%</code>") followed by its 2-digit hexadecimal code.
     * Characters with values of 128 or greater are simply dropped.
     *
     * @example  <code>urlEncode("RR Lyr") = "RR%20Lyr"</code>
     *
     * @param  txt  input (unencoded) string
     * @return  output (encoded) string
     * @see  <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>
     */
    public static String urlEncode( String txt ) {
        if ( txt == null ) {
            return null;
        }
        else {
            int nc = txt.length();
            StringBuffer sbuf = new StringBuffer( nc );
            for ( int i = 0; i < nc; i++ ) {
                char c = txt.charAt( i );
                if ( URI_DATA_CHARS.indexOf( c ) >= 0 ) {
                    sbuf.append( c );
                }
                else if ( c >= 0x10 && c <= 0x7f ) {
                    sbuf.append( '%' )
                        .append( Integer.toHexString( (int) c ) );
                }
            }
            return sbuf.toString();
        }
    }

    /**
     * Reverses the quoting performed by <code>urlEncode</code>.
     * Percent-encoded sequences (<code>%xx</code>) are replaced
     * by the ASCII character with the hexadecimal code <code>xx</code>.
     *
     * @example  <code>urlDecode("RR%20Lyr") = "RR Lyr"</code>
     *
     * @param  txt  input (encoded) string
     * @return  output (unencoded) string
     * @see  <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>
     */
    public static String urlDecode( String txt ) {
        if ( txt == null ) {
            return null;
        }
        else {
            int nc = txt.length();
            StringBuffer sbuf = new StringBuffer( nc );
            for ( int i = 0; i < nc; i++ ) {
                char c = txt.charAt( i );
                if ( c == '%' ) {
                    try {
                        String hexPair = txt.substring( i + 1, i + 3 );
                        c = (char) Integer.parseInt( hexPair, 16 );
                        i += 2;
                    }
                    catch ( RuntimeException e ) {
                        // failed for some reason (bad hex, short string) -
                        // just copy input characters as usual instead
                    }
                }
                sbuf.append( c );
            }
            return sbuf.toString();
        }
    }

    /**
     * Builds a query-type URL string given a base URL and a list of
     * name, value pairs.
     *
     * <p>The parameters are encoded on the command line according to the
     * "<code>application/x-www-form-urlencoded</code>" convention,
     * which appends a "?" to the base URL, and then adds name=value pairs
     * separated by "&amp;" characters, with percent-encoding of
     * non-URL-friendly characters.
     * This format is used by many services that require a list of parameters
     * to be conveyed on the URL.
     *
     * @example
     *  <code>paramsUrl("http://x.org/", "a", "1", "b", "two", "c", "3&amp;4")
     *      = "http://x.org/?a=1&amp;b=two&amp;c=3%264"</code>
     *
     * @param  baseUrl  basic URL (may or may not already contain a "?")
     * @param  nameValuePairs   an even number of arguments
     *         (or an even-length string array) giving
     *         parameter name1,value1,name2,value2,...nameN,valueN
     * @return  form-encoded URL
     */
    public static String paramsUrl( String baseUrl, String... nameValuePairs ) {
        return paramsUrl( baseUrl, toStringMap( nameValuePairs ) );
    }

    /**
     * Builds a query-type URL string given a base URL and a map giving
     * name-value pairs.
     * The output is in the <code>application/x-www-form-urlencoded</code>
     * format.
     * 
     * @param  baseUrl  basic URL (may or may not already contain a "?")
     * @return  form-encoded URL
     */
    private static String paramsUrl( String baseUrl, Map<String,String> pmap ) {
        CgiQuery query = new CgiQuery( baseUrl );
        for ( Map.Entry<String,String> entry : pmap.entrySet() ) {
            query.addArgument( entry.getKey(), entry.getValue() );
        }
        return query.toURL().toString();
    }

    /**
     * Turns an even-length string array into a Map, by interpreting
     * each pair of elements as a name-value pair.
     *
     * @param  nameValuePairs   an even number of arguments
     *         (or an even-length string array) giving
     *         parameter name1,value1,name2,value2,...nameN,valueN
     * @return  map
     */
    private static Map<String,String> toStringMap( String... nameValuePairs ) {
        int n2 = nameValuePairs.length;
        if ( n2 % 2 == 0 ) {
            Map<String,String> pmap = new LinkedHashMap<String,String>();
            for ( int i2 = 0; i2 < n2; i2 += 2 ) {
                pmap.put( nameValuePairs[ i2 ], nameValuePairs[ i2 + 1 ] );
            }
            return pmap;
        }
        else {
            return null;
        }
    }

    /**
     * Maps a bibcode to the URL that will display the relevant entry in
     * <a href="https://ui.adsabs.harvard.edu/">ADS</a>.
     * If the supplied string does not appear to be a bibcode,
     * null will be returned.
     *
     * <p>If the supplied string appears to be a bibcode,
     * it just prepends the string
     * "<code>https://ui.adsabs.harvard.edu/abs/</code>"
     * and performs any character escaping that is required.
     *
     * @example <code>bibcodeUrl("2018A&amp;A...616A...2L") =
     *          "https://ui.adsabs.harvard.edu/abs/2018A%26A...616A...2L"</code>
     *
     * @param  bibcode  ADS-style bibcode string
     * @return  display URL pointing at bibcode record,
     *          or null if it doesn't look like a bibcode
     * @see  <a href="http://adsabs.harvard.edu/abs_doc/help_pages/data.html"
     *               >http://adsabs.harvard.edu/abs_doc/help_pages/data.html</a>
     */
    public static String bibcodeUrl( String bibcode ) {
        return webMap( WebMapper.BIBCODE, bibcode );
    }

    /**
     * Maps a DOI (Digital Object Identifier) to its display URL.
     * If the supplied string does not appear to be a DOI,
     * null will be returned. 
     * 
     * <p>If the supplied string appears to be a DOI,
     * it strips any "<code>doi:</code>" prefix if present,
     * prepends the string "<code>https://doi.org/</code>",
     * and performs any character escaping that is required.
     * 
     * @example <code>doiUrl("10.3390/informatics4030018") =
     *          "https://doi.org/10.3390/informatics4030018"</code>
     *
     * @param  doi  DOI string, with or without "doi:" prefix
     * @return  display URL pointing at DOI content,
     *          or null if it doesn't look like a DOI
     * @see  <a href="https://www.doi.org/">https://www.doi.org/</a>
     */
    public static String doiUrl( String doi ) {
        return webMap( WebMapper.DOI, doi );
    }

    /**
     * Maps an arXiv identifier to the URL that will display its
     * <a href="https://arxiv.org/">arXiv</a> web page.
     * If the supplied string does not appear to be an arXiv identifier,
     * null will be returned.
     *
     * <p>If the supplied string appears to be an arXiv identifier,
     * it strips any "<code>arXiv:</code> prefix
     * and prepends the string "<code>https://arxiv.org/abs/</code>".
     *
     * @example  <code>arxivUrl("arXiv:1804.09379") =
     *           "https://arxiv.org/abs/1804.09381"</code>
     *
     * @param  arxivId  arXiv identifier, with or without "arXiv:" prefix
     * @return  display URL pointing at bibcode record,
     *          or null if it doesn't look like a bibcode
     * @see  <a href="https://arxiv.org/help/arxiv_identifier"
     *               >https://arxiv.org/help/arxiv_identifier</a>
     */
    public static String arxivUrl( String arxivId ) {
        return webMap( WebMapper.ARXIV, arxivId );
    }

    /**
     * Maps a source identifier to the URL of its
     * <a href="https://simbad.cds.unistra.fr/simbad/">SIMBAD</a> web page.
     * SIMBAD is the astronomical source information service run by
     * the Centre de Donn&#x00e9;es astronomiques de Strasbourg.
     *
     * <p>The string
     * "<code>https://simbad.cds.unistra.fr/simbad/sim-id?Ident=</code>"
     * is prepended to the given id string, and any necessary character
     * escaping is applied.
     * No attempt is made to validate whether the supplied string is
     * a real source identifier, so there is no guarantee that the
     * returned URL will contain actual results.
     * 
     * @example  <code>simbadUrl("Beta Pictoris") =
     *   "https://simbad.cds.unistra.fr/simbad/sim-id?Ident=Beta%20Pictoris"</code>
     * 
     * @param  sourceId   free text assumed to represent a source identifier
     *                    known by SIMBAD
     * @return  URL of the Simbad web page describing the identified source
     */
    public static String simbadUrl( String sourceId ) {
        return webMap( WebMapper.SIMBAD, sourceId );
    }

    /**
     * Maps a source identifier to the URL of its
     * <a href="http://ned.ipac.caltech.edu/">NED</a> web page.
     * NED is the NASA/IPAC Extragalactic Database.
     *
     * <p>The string
     * "<code>http://ned.ipac.caltech.edu/byname?objname=</code>"
     * is prepended to the given id string, and any necessary character
     * escaping is applied.
     * No attempt is made to validate whether the supplied string is
     * a real source identifier, so there is no guarantee that the
     * returned URL will contain actual results.
     *
     * @example  <code>nedUrl("NGC 3952") =
     *           "http://ned.ipac.caltech.edu/byname?objname=NGC%203952"</code>
     *
     * @param  sourceId   free text assumed to represent a source identifier
     *                    known by NED
     * @return  URL of the NED web page describing the identified source
     */
    public static String nedUrl( String sourceId ) {
        return webMap( WebMapper.NED, sourceId );
    }

    /**
     * Returns the URL of a cutout from the Hips2Fits service operated
     * by CDS.  The result will be the URL of a FITS or image file
     * resampled to order from one of the HiPS surveys available at CDS.
     *
     * <p>This function requests a square cutout using the SIN projection,
     * which is suitable for small cutouts.
     * If the details of this function don't suit your purposes,
     * you can construct the URL yourself.
     *
     * @see
     *  <a href="https://alasky.cds.unistra.fr/hips-image-services/hips2fits"
     *          >https://alasky.cds.unistra.fr/hips-image-services/hips2fits</a>
     * @param  hipsId  identifier or partial identifier for the HiPS survey
     * @param  fmt    required output format, for instance
     *                "<code>fits</code>", "<code>png</code>",
     *                "<code>jpg</code>"
     * @param  raDeg  central Right Ascension (longitude) in degrees
     * @param  decDeg  central Declination (latitude) in degrees
     * @param  fovDeg  field of view; extent of the cutout in degrees
     * @param  npix    extent of the cutout in pixels (width=height=npix)
     * @return   URL of the required cutout
     */
    public static String hips2fitsUrl( String hipsId, String fmt,
                                       double raDeg, double decDeg,
                                       double fovDeg, int npix ) {
        double pixSizeDeg = fovDeg / npix;
        int decPlaces = - (int) Math.floor( Maths.log10( pixSizeDeg ) );
        int ndp = Math.max( decPlaces + 1, 0 );
        double fovEps = fovDeg / npix;

        /* Assemble this by hand because the URL-encoding of HIPS ID
         * is not necessary for the Hips2fits service and it makes
         * the URL less human-readable.  The other fields are not
         * going to need URL-encoding anyway. */
        return new StringBuffer()
           .append( HIPS2FITS_BASE )
           .append( "?hips=" )
           .append( hipsId )
           .append( "&format=" )
           .append( fmt )
           .append( "&ra=" )
           .append( Formats.formatDecimal( raDeg, ndp )
                           .replaceFirst( "0+$", "" ) )
           .append( "&dec=" )
           .append( Formats.formatDecimal( decDeg, ndp )
                           .replaceFirst( "0+$", "" ) )
           .append( "&fov=" )
           .append( PlotUtil.formatNumber( fovDeg, fovEps ) )
           .append( "&width=" )
           .append( npix )
           .append( "&height=" )
           .append( npix )
           .append( "&projection=SIN" )
           .toString();
    }

    /**
     * Uses a WebMapper to turn an input reference to a URL string.
     * 
     * @param  mapper  mapper
     * @param  txt  input string 
     * @return  output string, may be null
     */
    private static String webMap( WebMapper mapper, String txt ) {
        if ( txt == null ) {
            return null;  
        }
        else {  
            URL url = mapper.toUrl( txt );
            return url == null ? null : url.toString();
        }
     }
}
