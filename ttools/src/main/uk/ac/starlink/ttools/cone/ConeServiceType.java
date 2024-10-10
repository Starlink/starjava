package uk.ac.starlink.ttools.cone;

import java.net.URL;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.task.TableCone;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.ConeSearch;
import uk.ac.starlink.vo.SiaFormatOption;
import uk.ac.starlink.vo.SiaVersion;

/**
 * Characterises a simple DAL service based on sky position.
 *
 * @author   Mark Taylor
 * @since    10 Oct 2024
 */
public abstract class ConeServiceType {

    private final String name_;

    /** Simple cone search type. */
    public static final ConeServiceType CONE =
        new SimpleConeServiceType( "cone" );

    /** Simple Spectral Access type. */
    public static final ConeServiceType SSA =
        new SsaServiceType( "ssa" );

    /** Simple Image Access v1 type. */
    public static final ConeServiceType SIA1 =
        new SiaServiceType( "sia1", SiaVersion.V10 );

    /** Simple Image Access v2 type. */
    public static final ConeServiceType SIA2 =
        new SiaServiceType( "sia2", SiaVersion.V20 );

    /** Alias for SIA1. */
    public static final ConeServiceType SIA =
        new SiaServiceType( "sia", SiaVersion.V10 ) {
            @Override
            public String getDescription() {
                return "alias for <code>" + super.toString() + "</code>";
            }
        };

    private static final String BELIEVE_EMPTY_NAME = "emptyok";
    private static final String INCONSISTENT_EMPTY_ADVICE =
            BELIEVE_EMPTY_NAME + "=false";

    /**
     * Constructor.
     *
     * @param  name  informal, short name
     */
    protected ConeServiceType( String name ) {
        name_ = name;
    }

    /**
     * Returns XML description of this service type.
     *
     * @return  description
     */
    public abstract String getDescription();

    /**
     * Returns XML documentation of the use of the format parameter
     * for this service type.
     *
     * @return  formats info
     */
    public abstract String getFormatDescription();

    /**
     * Provides this object with a chance to perform custom configuration
     * on general cone search parameters.
     *
     * @param  srParam   search radius parameter
     */
    public abstract void configureRadiusParam( Parameter<Double> srParam );

    /**
     * Constructs a ConeSearcher instance suitable for single cone searches
     * using this service type.
     *
     * @param  env  execution environment
     * @param  coneTask  task instance
     * @param  url  service URL
     * @param  tfact  table factory
     * @param  coding  controls HTTP-level byte stream compression;
     *                 implementations may choose to ignore this hint
     * @return  cone searcher object
     */
    public abstract ConeSearcher
            createSingleSearcher( Environment env, TableCone coneTask,
                                  String url, StarTableFactory tfact,
                                  ContentCoding coding ) throws TaskException;

    /**
     * Constructs a ConeSearcher instance suitable for this service type.
     *
     * @param  env  execution environment
     * @param  conerTask   task instance
     * @param  url  service URL
     * @param  believeEmpty  whether to take seriously metadata from
     *         zero-length tables
     * @param  tfact  table factory
     * @param  coding  controls HTTP-level byte stream compression;
     *                 implementations may choose to ignore this hint
     * @return  cone searcher object
     */
    public abstract ConeSearcher
            createMultiSearcher( Environment env, ConeSearchConer conerTask,
                                 String url, boolean believeEmpty,
                                 StarTableFactory tfact, ContentCoding coding )
            throws TaskException;

    /**
     * Indicates whether the result table should be subjected
     * to additional filtering to ensure that only rows in the
     * specified search radius are included in the final output.
     *
     * @return  true iff post-query filtering on distance is to be performed
     */
    public abstract boolean useDistanceFilter();

    /**
     * Returns a coverage footprint for use with the service specified.
     *
     * @param  url  cone search service URL
     * @param  nside  MOC nside parameter
     * @return  coverage footprint, or null
     */
    public abstract Coverage getCoverage( URL url, int nside );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of distinct instances of this class.
     *
     * @return  instance list
     */
    public static ConeServiceType[] getDistinctTypes() {
        return new ConeServiceType[] { CONE, SIA1, SIA2, SSA };
    }

    /**
     * Returns a list of instances of this class that may include aliases.
     *
     * @return  instance list
     */
    public static ConeServiceType[] getAllTypes() {
        return new ConeServiceType[] { CONE, SSA, SIA1, SIA2, SIA };
    }

    /**
     * Utility method to parse the verbosity level parameter.
     *
     * @param  env  execution environment
     * @param  verbosity parameter
     * @return  verbosity level, -1 for unknown
     */
    private static int getVerbosity( Environment env,
                                     Parameter<String> verbParam )
            throws TaskException {
        String sverb = verbParam.stringValue( env );
        if ( sverb == null ) {
            return -1;
        }
        else {
            try {
                return Integer.parseInt( sverb );
            }
            catch ( NumberFormatException e ) {
                assert false;
                throw new ParameterValueException( verbParam,
                                                   e.getMessage(), e );
            }
        }
    }

    /**
     * ServiceType implementation for Cone Search protocol.
     */
    private static class SimpleConeServiceType extends ConeServiceType {

        /**
         * Constructor.
         *
         * @param  name   type name
         */
        SimpleConeServiceType( String name ) {
            super( name );
        }

        public String getDescription() {
            return new StringBuffer()
               .append( "Cone Search protocol " )
               .append( "- returns a table of objects found " )
               .append( "near each location.\n" )
               .append( "See <webref url='" )
               .append( "http://www.ivoa.net/Documents/latest/ConeSearch.html" )
               .append( "'>Cone Search standard</webref>." )
               .toString();
        }

        public String getFormatDescription() {
            return "not used";
        }

        public void configureRadiusParam( Parameter<Double> srParam ) {
            srParam.setNullPermitted( false );
        }

        public ConeSearcher createSingleSearcher( Environment env,
                                                  TableCone coneTask,
                                                  String url,
                                                  StarTableFactory tfact,
                                                  ContentCoding coding )
                throws TaskException {
            int verb = getVerbosity( env, coneTask.getVerbosityParameter() );
            return new ServiceConeSearcher( new ConeSearch( url, coding ),
                                            verb, true, tfact );
        }

        public ConeSearcher createMultiSearcher( Environment env,
                                                 ConeSearchConer conerTask,
                                                 String url,
                                                 final boolean believeEmpty,
                                                 StarTableFactory tfact,
                                                 ContentCoding coding )
                throws TaskException {
            int verb = getVerbosity( env, conerTask.getVerbosityParameter() );
            return new ServiceConeSearcher( new ConeSearch( url, coding ),
                                            verb, believeEmpty, tfact ) {
                @Override
                protected String getInconsistentEmptyAdvice() {
                    return INCONSISTENT_EMPTY_ADVICE;
                }
            };
        }

        public boolean useDistanceFilter() {
            return true;
        }

        public Coverage getCoverage( URL url, int nside ) {
            return UrlMocCoverage.getServiceMoc( url, nside );
        }
    }

    /**
     * ServiceType implementation for Simple Image Access.
     */
    private static class SiaServiceType extends ConeServiceType {

        final SiaVersion version_;

        /**
         * Constructor.
         *
         * @param  name   type name
         * @param  version   version of SIA protocol to use
         */
        SiaServiceType( String name, SiaVersion version ) {
            super( name );
            version_ = version;
        }

        public String getDescription() {
            return new StringBuffer()
               .append( "Simple Image Access protocol version " )
               .append( version_.getMajorVersion() )
               .append( " - returns a table of images near each location.\n" )
               .append( "See <webref url='" )
               .append( version_.getDocumentUrl() )
               .append( "'>SIA " )
               .append( version_ )
               .append( " standard</webref>." )
               .toString();
        }

        public String getFormatDescription() {
            StringBuffer sbuf = new StringBuffer()
               .append( "gives the MIME type required for images/resources\n" )
               .append( "referenced in the output table,\n" )
               .append( "corresponding to the SIA FORMAT parameter.\n" )
               .append( "The special values " )
               .append( "\"<code>GRAPHIC</code>\" (all graphics formats) and " )
               .append( "\"<code>ALL</code>\" (no restriction)\n" )
               .append( "as defined by SIAv1 are also permissible.\n" );
            if ( version_.getMajorVersion() == 1 ) {
                sbuf.append( "For SIA version 1 only, this defaults to" )
                    .append( "<code>\"image/fits\"</code>." );
            }
            return sbuf.toString();
        }

        public void configureRadiusParam( Parameter<Double> srParam ) {

            /* SIZE = 0 has a special meaning for SIA: it means any image
             * containing the given image.  This is a sensible default in
             * most cases. */
            srParam.setStringDefault( "0" );
            srParam.setNullPermitted( false );
        }

        public ConeSearcher createSingleSearcher( Environment env,
                                                  TableCone coneTask,
                                                  String url,
                                                  StarTableFactory tfact,
                                                  ContentCoding coding )
                throws TaskException {
            Parameter<String> formatParam = coneTask.getFormatParameter();
            if ( version_.getMajorVersion() == 1 ) {
                formatParam.setStringDefault( "image/fits" );
            }
            String formatTxt = formatParam.stringValue( env );
            SiaFormatOption format = SiaFormatOption.fromObject( formatTxt );
            return new SiaConeSearcher( url, version_, format, true,
                                        tfact, coding );
        }

        public ConeSearcher createMultiSearcher( Environment env,
                                                 ConeSearchConer conerTask,
                                                 String url,
                                                 boolean believeEmpty,
                                                 StarTableFactory tfact,
                                                 ContentCoding coding )
                throws TaskException {
            Parameter<String> formatParam = conerTask.getFormatParameter();
            if ( version_.getMajorVersion() == 1 ) {
                formatParam.setStringDefault( "image/fits" );
            }
            String formatTxt = formatParam.stringValue( env );
            SiaFormatOption format = SiaFormatOption.fromObject( formatTxt );
            return new SiaConeSearcher( url, version_, format, believeEmpty,
                                        tfact, coding ) {
                @Override
                protected String getInconsistentEmptyAdvice() {
                    return INCONSISTENT_EMPTY_ADVICE;
                }
            };
        }

        public boolean useDistanceFilter() {
            return false;
        }

        public Coverage getCoverage( URL url, int nside ) {
            return null;
        }
    }

    /**
     * ServiceType implementation for Simple Spectral Access.
     */
    private static class SsaServiceType extends ConeServiceType {

        /**
         * Constructor.
         *
         * @param  name   type name
         */
        SsaServiceType( String name ) {
            super( name );
        }

        public String getDescription() {
            return new StringBuffer()
               .append( "Simple Spectral Access protocol " )
               .append( " - returns a table of spectra near each location.\n" )
               .append( "See <webref url='" )
               .append( "http://www.ivoa.net/Documents/latest/SSA.html" )
               .append( "'>SSA standard</webref>." )
               .toString();
        }

        public String getFormatDescription() {
            return new StringBuffer()
               .append( "gives the MIME type of spectra referenced in the " )
               .append( "output table, also special values " )
               .append( "\"<code>votable</code>\", " )
               .append( "\"<code>fits</code>\", " )
               .append( "\"<code>compliant</code>\", " )
               .append( "\"<code>graphic</code>\", " )
               .append( "\"<code>all</code>\", and others\n" )
               .append( "(value of the SSA FORMAT parameter)." )
               .toString();
        }

        public void configureRadiusParam( Parameter<Double> srParam ) {

            /* SIZE param may be omitted in an SSA query; the service should
             * use some appropriate default value. */
            srParam.setNullPermitted( true );
        }

        public ConeSearcher createSingleSearcher( Environment env,
                                                  TableCone coneTask,
                                                  String url,
                                                  StarTableFactory tfact,
                                                  ContentCoding coding )
                throws TaskException {
            String format = coneTask.getFormatParameter().stringValue( env );
            return new SsaConeSearcher( url, format, true, tfact, coding );
        }

        public ConeSearcher createMultiSearcher( Environment env,
                                                 ConeSearchConer conerTask,
                                                 String url,
                                                 final boolean believeEmpty,
                                                 StarTableFactory tfact,
                                                 ContentCoding coding )
                throws TaskException {
            String format = conerTask.getFormatParameter().stringValue( env );
            return new SsaConeSearcher( url, format, believeEmpty, tfact,
                                        coding ) {
                @Override
                protected String getInconsistentEmptyAdvice() {
                    return INCONSISTENT_EMPTY_ADVICE;
                };
            };
        }

        public boolean useDistanceFilter() {
            return true;
        }

        public Coverage getCoverage( URL url, int nside ) {
            return null;
        }
    }
}
