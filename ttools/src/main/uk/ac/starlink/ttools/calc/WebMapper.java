package uk.ac.starlink.ttools.calc;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.ttools.func.URLs;
import uk.ac.starlink.util.URLUtils;

/**
 * Object that can map some particular sort of string to a URL
 * referencing a web page.
 * "Web page" here is generally intended to mean a resource that it's
 * reasonable to try viewing in a web browser.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2019
 */
public abstract class WebMapper {

    private final String name_;

    /** Mapper for pathname of a file in the local filesystem. */
    public static final WebMapper FILE = new WebMapper( "Filename" ) {
        public URL toUrl( String txt ) {
            File file = new File( txt );
            return file.exists() ? URLUtils.makeFileURL( file ) : null;
        }
    };

    /** Mapper for a string that's already a URL. */
    public static final WebMapper URL = new WebMapper( "URL" ) {
        public URL toUrl( String txt ) {
            try {
                return URLUtils.newURL( txt );
            }
            catch ( MalformedURLException e ) {
                return null;
            }
        }
    };

    /** Mapper for a Bibcode. */
    public static final WebMapper BIBCODE =
        createBibcodeMapper( "Bibcode", false );

    /** Mapper for a DOI (Digital Object Identifier). */
    public static final WebMapper DOI = createDoiMapper( "DOI" );

    /** Mapper for an arXiv identifier. */
    public static final WebMapper ARXIV = createArxivMapper( "arXiv" );

    /** Maps a source identifier to its Simbad web page. */
    public static final WebMapper SIMBAD =
        createPrefixMapper( "SIMBAD",
                            "http://simbad.u-strasbg.fr/simbad/sim-id?Ident=" );

    /** Maps a source identifier to its NED web page. */
    public static final WebMapper NED =
        createPrefixMapper( "NED",
                            "http://ned.ipac.caltech.edu/byname?objname=" );

    private static final WebMapper[] SELECTIVE_MAPPERS = new WebMapper[] {
        URL, FILE, BIBCODE, DOI, ARXIV,
    };

    private static final WebMapper[] UNSELECTIVE_MAPPERS = new WebMapper[] {
        SIMBAD, NED,
    };

    /** Mapper for a Bibcode, using the Classic web pages (deprecated by ADS).*/
    public static final WebMapper BIBCODE_OLD =
        createBibcodeMapper( "BibcodeClassic", true );

    /** Maps a source identifier to its classic NED web page. */
    public static final WebMapper NED_CLASSIC =
        createPrefixMapper( "NED",
                            "http://ned.ipac.caltech.edu/cgi-bin/objsearch"
                          + "?extend=no&objname=" );

    /** Mapper that tries various strategies to turn a string into a URL. */
    public static final WebMapper AUTO =
        createMultiMapper( "auto", SELECTIVE_MAPPERS );
 
    /**
     * Constructor.
     *
     * @param  name  user-visible name for this mapper
     */
    protected WebMapper( String name ) {
        name_ = name;
    }

    /**
     * Returns a URL constructed from the given text according to the
     * knowledge of this WebMapper, if it looks suitable.
     * If the supplied string does not look like the kind of input
     * this mapper is expecting, it should return null.
     *
     * <p>Where possible, implementations should do enough checking
     * of the format to tell whether the result is likely to be a
     * resolvable URL (though without taking significant time to do it),
     * rather than just making a best effort to come up with a URL
     * that's unlikely to work.
     *
     * @param  txt  location string
     * @return   URL, or null
     */
    public abstract URL toUrl( String txt );

    /**
     * Returns the user-visible name for this mapper.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of all the known useful mappers.
     * The {@link #AUTO} mapper will be first in the list.
     *
     * @return  mapper list
     */
    public static WebMapper[] getMappers() {
        List<WebMapper> list = new ArrayList<WebMapper>();
        list.add( AUTO );
        list.addAll( Arrays.asList( SELECTIVE_MAPPERS ) );
        list.addAll( Arrays.asList( UNSELECTIVE_MAPPERS ) );
        return list.toArray( new WebMapper[ 0 ] );
    }

    /**
     * Turns a string into a URL where possible.
     *
     * @param  url  string representation of URL
     * @return  URL object, or null for malformed input
     */
    private static URL stringToUrl( String url ) {
        try {
            return URLUtils.newURL( url );
        }
        catch ( MalformedURLException e ) {
            assert false;
            return null;
        }
    }

    /**
     * Returns a WebMapper implementation for Bibcodes.
     *
     * @param  name  mapper name
     * @param  isClassic  false for current ADS,
     *                    true for classic (deprecated) ADS
     * @return  new mapper
     * @see  <a href="http://adsabs.harvard.edu/abs_doc/help_pages/data.html"
     *               >http://adsabs.harvard.edu/abs_doc/help_pages/data.html</a>
     */
    private static WebMapper createBibcodeMapper( String name,
                                                  boolean isClassic ) {
        final String urlPrefix = isClassic
                               ? "http://adsabs.harvard.edu/abs/"
                               : "https://ui.adsabs.harvard.edu/abs/";
        final Pattern bibcodeRegex = Pattern.compile(
                "[12][0-9]{3}"   // YYYY
              + "[A-Za-z]\\S{4}" // JJJJJ
              + "\\S{4}"         // VVVV
              + "\\S"            // M
              + "[0-9.]{4}"      // PPPP
              + "[A-Z]"          // A
        );
        return new WebMapper( name ) {
            public URL toUrl( String txt ) {
                return bibcodeRegex.matcher( txt ).matches()
                     ? stringToUrl( urlPrefix + URLs.urlEncode( txt ) )
                     : null;
            }
        };
    }

    /**
     * Returns a WebMapper implementation for DOIs.
     *
     * @param  name  mapper name
     * @return   new mapper
     * @see <a href="https://www.doi.org/doi_handbook/2_Numbering.html"
     *              >https://www.doi.org/doi_handbook/2_Numbering.html</a>
     */
    private static WebMapper createDoiMapper( String name ) {
        final String urlPrefix = "https://doi.org/";
        final Pattern doiRegex = Pattern.compile(
            "(?:doi:)?(10[.][0-9]{4,}(?:[.][0-9]+)*)"   // prefix
          + "/"
          + "(.+)"                                      // suffix
        );
        return new WebMapper( name ) {
            public URL toUrl( String txt ) {
                Matcher matcher = doiRegex.matcher( txt );
                if ( matcher.matches() ) {
                    String doiPrefix = matcher.group( 1 );
                    String doiSuffix = matcher.group( 2 );
                    return stringToUrl( new StringBuffer()
                       .append( urlPrefix )
                       .append( doiPrefix )
                       .append( '/' )
                       .append( URLs.urlEncode( doiSuffix ) )
                       .toString() );
                }
                else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a WebMapper implementation for arXiv identifiers.
     *
     * @param  name  mapper name
     * @return   new mapper
     * @see  <a href="https://arxiv.org/help/arxiv_identifier"
     *               >https://arxiv.org/help/arxiv_identifier</a>
     */
    private static WebMapper createArxivMapper( String name ) {
        final String urlPrefix = "https://arxiv.org/abs/";
        int year = new GregorianCalendar().get( GregorianCalendar.YEAR );
        int maxDecade = ( year > 2007 && year < 2097 )
                      ? ( year - 2000 + 2 ) / 10
                      : 9;
        assert Integer.toString( maxDecade ).length() == 1;
        final Pattern post07Regex = Pattern.compile(
            "(?:ar[xX]iv:)?"
          + "[0-" + maxDecade + "][0-9]"  // YY
          + "(0[1-9]|1[0-2])"             // MM
          + "[.]"
          + "[0-9]{4,5}"                  // number
          + "(v[1-9][0-9]*)?"             // vV
        );
        final Pattern pre07Regex = Pattern.compile(
            "(?:ar[xX]iv:)?"
          + "(astro-ph|cond-mat|gr-qc|hep-ex|hep-lat|hep-ph|hep-th|math-ph"
          + "|nlin|nucl-ex|nucl-th|physics|quant-ph"
          + "|math|CoRR|q-bio|q-fin|stat|eess)"  // archive
          + "([.][A-Z][A-Z])?"                   // subject-class
          + "/"
          + "[09][0-9]"                          // year
          + "(0[1-9]|1[0-2])"                    // month
          + "[0-9][0-9][0-9]"                    // number
        );
        return new WebMapper( name ) {
            public URL toUrl( String txt ) {
                if ( pre07Regex.matcher( txt ).matches() ||
                     post07Regex.matcher( txt ).matches() ) {
                    String id = txt.toLowerCase().startsWith( "arxiv:" )
                              ? txt.substring( 6 )
                              : txt;
                    return stringToUrl( urlPrefix + id );
                }
                else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns a mapper that blindly appends the given location string
     * to a supplied prefix URL, pausing only to URL-encode it.
     * This implementation does not attempt to assess the supplied URL
     * for suitability, so it never returns null except in case of a
     * blank input string.
     *
     * @param  name  mapper name
     * @return   new mapper
     */
    public static WebMapper createPrefixMapper( String name,
                                                final String urlPrefix ) {
        return new WebMapper( name ) {
            public URL toUrl( String txt ) {
                return txt == null || txt.trim().length() == 0
                     ? null
                     : stringToUrl( urlPrefix + URLs.urlEncode( txt ) );
            } 
        };
    }

    /**
     * Returns a mapper that combines others to come up with a result
     * if any of them can turn a string into a URL.
     *
     * @param  name   mapper name
     * @param  others   list of WebMappers to which this one will
     *                  try delegating in turn
     * @return  new mapper
     */
    public static WebMapper createMultiMapper( String name,
                                               final WebMapper[] others ) {
        return new WebMapper( name ) {
            public URL toUrl( String txt ) {
                for ( WebMapper other : others ) {
                    URL url = other.toUrl( txt );
                    if ( url != null ) {
                        return url;
                    }
                }
                return null;
            }
        };
    }
}
