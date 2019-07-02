package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.net.URL;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.ServiceConeSearcher;
import uk.ac.starlink.ttools.cone.SiaConeSearcher;
import uk.ac.starlink.ttools.cone.SsaConeSearcher;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.ConeSearch;

/**
 * Executes a single cone-search-like query to an external DAL service.
 *
 * @author   Mark Taylor
 * @since    18 Jun 2019
 */
public class TableCone extends ConsumerTask {

    private final URLParameter urlParam_;
    private final DoubleParameter lonParam_;
    private final DoubleParameter latParam_;
    private final DoubleParameter radiusParam_;
    private final ChoiceParameter<String> verbParam_;
    private final ChoiceParameter<ServiceType> serviceParam_;
    private final ContentCodingParameter codingParam_;
    private final ChoiceParameter<SkySystem> sysParam_;
    private final StringParameter formatParam_;

    public TableCone() {
        super( "Executes a Cone Search-like query", new ChoiceMode(), true );

        final String sysParamName = "skysys";
        ServiceType[] serviceTypes = new ServiceType[] {
            new ConeServiceType(),
            new SiaServiceType(),
            new SsaServiceType(),
        };

        urlParam_ = new URLParameter( "serviceurl" );
        urlParam_.setPrompt( "Base URL for query returning VOTable" );
        urlParam_.setDescription( new String[] {
            "<p>The base part of a URL which defines the query to be made.",
            "Additional parameters will be appended to this using CGI syntax",
            "(\"<code>name=value</code>\", separated by '&amp;' characters).",
            "If this value does not end in either a '?' or a '&amp;',",
            "one will be added as appropriate.",
            "</p>",
        } );

        lonParam_ = new DoubleParameter( "lon" );
        lonParam_.setPrompt( "Longitude in degrees" );
        lonParam_.setUsage( "<degrees>" );
        lonParam_.setDescription( new String[] {
            "<p>Central longitude position for cone search.",
            "By default this is the Right Ascension,",
            "but depending on the value of the",
            "<code>" + sysParamName + "</code> parameter",
            "it may be in a different sky system.",
            "</p>",
        } );

        latParam_ = new DoubleParameter( "lat" );
        latParam_.setPrompt( "Latitude in degrees" );
        latParam_.setUsage( "<degrees>" );
        latParam_.setDescription( new String[] {
            "<p>Central latitude position for cone search.",
            "By default this is the Declination,",
            "but depending on the value of the",
            "<code>" + sysParamName + "</code> parameter",
            "it may be in a different sky system.",
            "</p>",
        } );

        radiusParam_ = new DoubleParameter( "radius" );
        radiusParam_.setPrompt( "Radius in degrees" );
        radiusParam_.setUsage( "<degrees>" );
        radiusParam_.setDescription( new String[] {
            "<p>Search radius in degrees.",
            "</p>",
        } );

        sysParam_ =
            new ChoiceParameter<SkySystem>( sysParamName,
                                            SkySystem.getKnownSystems() );
        sysParam_.setPrompt( "Sky coordinate system for central position" );
        sysParam_.setDescription( new String[] {
            "<p>Sky coordinate system used to interpret the",
            "<code>" + lonParam_.getName() + "</code> and",
            "<code>" + latParam_.getName() + "</code> parameters.",
            "If the value is ICRS (the default)",
            "the provided values are assumed to be",
            "Right Ascension and Declination and",
            "are sent unchanged; for other values they will be",
            "converted from the named system into RA and Dec first.",
            "</p>",
        } );
        sysParam_.setDefaultOption( SkySystem.ICRS );

        serviceParam_ =
            new ChoiceParameter<ServiceType>( "servicetype", serviceTypes );
        serviceParam_.setPrompt( "Search service type" );
        StringBuffer typesDescrip = new StringBuffer();
        for ( int i = 0; i < serviceTypes.length; i++ ) {
            ServiceType stype = serviceTypes[ i ];
            typesDescrip.append( "<li>" )
                        .append( "<code>" )
                        .append( stype )
                        .append( "</code>:\n" )
                        .append( stype.getDescription() )
                        .append( "</li>" )
                        .append( "\n" );
        }
        serviceParam_.setDescription( new String[] {
            "<p>Selects the type of data access service to contact.",
            "Most commonly this will be the Cone Search service itself,",
            "but there are one or two other possibilities:",
            "<ul>",
            typesDescrip.toString(),
            "</ul>",
            "</p>",
        } );
        serviceParam_.setDefaultOption( serviceTypes[ 0 ] );

        verbParam_ =
            new ChoiceParameter<String>( "verb",
                                         new String[] { "1", "2", "3", } );
        verbParam_.setNullPermitted( true );
        verbParam_.setPrompt( "Verbosity level of search responses (1..3)" );
        verbParam_.setDescription( new String[] {
            "<p>Verbosity level of the tables returned by the query service.",
            "A value of 1 indicates the bare minimum and",
            "3 indicates all available information.",
            "</p>",
        } );

        codingParam_ = new ContentCodingParameter();

        formatParam_ = new StringParameter( "dataformat" );
        formatParam_.setPrompt( "Data format type for DAL outputs" );
        formatParam_.setNullPermitted( true );
        StringBuffer formatsDescrip = new StringBuffer();
        for ( int i = 0; i < serviceTypes.length; i++ ) {
            ServiceType stype = serviceTypes[ i ];
            formatsDescrip
               .append( "<li>" )
               .append( "<code>" )
               .append( serviceParam_.getName() )
               .append( "=" )
               .append( stype )
               .append( "</code>:\n" )
               .append( stype.getFormatDescription() )
               .append( "</li>" )
               .append( "\n" );
        }
        formatParam_.setDescription( new String[] {
            "<p>Indicates the format of data objects described in the",
            "returned table.",
            "The meaning of this is dependent on the value of the",
            "<code>" + serviceParam_.getName() + "</code>",
            "parameter:",
            "<ul>",
            formatsDescrip.toString(),
            "</ul>",
            "</p>",
        } );
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {    
            urlParam_,
            lonParam_,
            latParam_,
            radiusParam_,
            sysParam_,
            serviceParam_,
            verbParam_,
            codingParam_,
            formatParam_,
        };
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        URL url = urlParam_.objectValue( env );
        ServiceType serviceType = serviceParam_.objectValue( env );
        serviceType.configureParams( radiusParam_ );
        double lon = lonParam_.doubleValue( env );
        double lat = latParam_.doubleValue( env );
        final double radius = radiusParam_.doubleValue( env );
        SkySystem skysys = sysParam_.objectValue( env );
        ContentCoding coding = codingParam_.codingValue( env );
        final double raDeg;
        final double decDeg;
        if ( SkySystem.ICRS.equals( skysys ) ) {
            raDeg = lon;
            decDeg = lat;
        }
        else {
            double epoch = 2000.0;
            double[] posFk5Rad =
                skysys.toFK5( Math.toRadians( lon ), Math.toRadians( lat ),
                              epoch );
            double[] posIcrsRad =
                SkySystem.ICRS.fromFK5( posFk5Rad[ 0 ], posFk5Rad[ 1 ], epoch );
            raDeg = Math.toDegrees( posIcrsRad[ 0 ] );
            decDeg = Math.toDegrees( posIcrsRad[ 1 ] );
        }
        StarTableFactory tfact = LineTableEnvironment.getTableFactory( env );
        final ConeSearcher searcher =
            serviceType.createSearcher( env, url.toString(), tfact, coding );
        return new TableProducer() {
            public StarTable getTable() throws IOException {
                return searcher.performSearch( raDeg, decDeg, radius );
            }
        };
    }

    /**
     * Describes the type of DAL service which is the basis of the cone.
     */
    private abstract class ServiceType {
        private final String name_;

        /**
         * Constructor.
         *
         * @param  informal, short name
         */
        ServiceType( String name ) {
            name_ = name;
        }

        /**
         * Returns XML description of this service type.
         *
         * @return  description
         */
        abstract String getDescription();

        /**
         * Returns XML documentation of the use of the format parameter
         * for this service type.
         *
         * @return  formats info
         */
        abstract String getFormatDescription();

        /**
         * Provides this object with a chance to perform custom configuration
         * on general cone search parameters.
         *
         * @param  srParam   search radius parameter
         */
        abstract void configureParams( Parameter<?> srParam );

        /**
         * Constructs a ConeSearcher instance suitable for this service type.
         *
         * @param  env  execution environment
         * @param  url  service URL
         * @param  tfact  table factory
         * @param  coding  controls HTTP-level byte stream compression;
         *                 implementations may choose to ignore this hint
         * @return  cone searcher object
         */
        abstract ConeSearcher createSearcher( Environment env, String url,
                                              StarTableFactory tfact,
                                              ContentCoding coding )
                throws TaskException;

        @Override
        public String toString() {
            return name_;
        }

        /**
         * Utility method to parse the verbosity level parameter.
         *
         * @param  env  execution environment
         * @return  verbosity level
         */
        int getVerbosity( Environment env ) throws TaskException {
            String sverb = verbParam_.stringValue( env );
            if ( sverb == null ) {
                return -1;
            }
            else {
                try {
                    return Integer.parseInt( sverb );
                }
                catch ( NumberFormatException e ) {
                    assert false;
                    throw new ParameterValueException( verbParam_,
                                                       e.getMessage(), e );
                }
            }
        }
    }

    /**
     * ServiceType implementation for Cone Search protocol.
     */
    private class ConeServiceType extends ServiceType {
        ConeServiceType() {
            super( "cone" );
        }

        String getDescription() {
            return new StringBuffer()
               .append( "Cone Search protocol " )
               .append( "- returns a table of objects found " )
               .append( "near each location.\n" )
               .append( "See <webref url='" )
               .append( "http://www.ivoa.net/Documents/latest/ConeSearch.html" )
               .append( "'>Cone Search standard</webref>." )
               .toString();
        }

        String getFormatDescription() {
            return "not used";
        }

        public void configureParams( Parameter<?> srParam ) {
            srParam.setNullPermitted( false );
        }

        public ConeSearcher createSearcher( Environment env, String url,
                                            StarTableFactory tfact,
                                            ContentCoding coding )
                throws TaskException {
            return new ServiceConeSearcher( new ConeSearch( url, coding ),
                                            getVerbosity( env ), true, tfact );
        }
    }

    /**
     * ServiceType implementation for Simple Image Access.
     */
    private class SiaServiceType extends ServiceType {
        SiaServiceType() {
            super( "sia" );
        }

        String getDescription() {
            return new StringBuffer()
               .append( "Simple Image Access protocol " )
               .append( "- returns a table of images near each location.\n" )
               .append( "See <webref url='" )
               .append( "http://www.ivoa.net/Documents/latest/SIA.html" )
               .append( "'>SIA standard</webref>." )
               .toString();
        }

        String getFormatDescription() {
            return new StringBuffer()
               .append( "gives the MIME type of images referenced in the " )
               .append( "output table, also special values " )
               .append( "\"<code>GRAPHIC</code>\" and \"<code>ALL</code>\"." )
               .append( "(value of the SIA FORMAT parameter)" )
               .toString();
        }

        public void configureParams( Parameter<?> srParam ) {

            /* SIZE = 0 has a special meaning for SIA: it means any image
             * containing the given image.  This is a sensible default in
             * most cases. */
            srParam.setStringDefault( "0" );
            srParam.setNullPermitted( false );
        }

        public ConeSearcher createSearcher( Environment env, String url,
                                            StarTableFactory tfact,
                                            ContentCoding coding )
                throws TaskException {
            formatParam_.setStringDefault( "image/fits" );
            String format = formatParam_.stringValue( env );
            return new SiaConeSearcher( url, format, true, tfact, coding );
        }
    }

    /**
     * ServiceType implementation for Simple Spectral Access.
     */
    private class SsaServiceType extends ServiceType {
        SsaServiceType() {
            super( "ssa" );
        }

        String getDescription() {
            return new StringBuffer()
               .append( "Simple Spectral Access protocol " )
               .append( " - returns a table of spectra near each location.\n" )
               .append( "See <webref url='" )
               .append( "http://www.ivoa.net/Documents/latest/SSA.html" )
               .append( "'>SSA standard</webref>." )
               .toString();
        }

        String getFormatDescription() {
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

        public void configureParams( Parameter<?> srParam ) {

            /* SIZE param may be omitted in an SSA query; the service should
             * use some appropriate default value. */
            srParam.setNullPermitted( true );
        }

        public ConeSearcher createSearcher( Environment env, String url,
                                            StarTableFactory tfact,
                                            ContentCoding coding )
                throws TaskException {
            String format = formatParam_.stringValue( env );
            return new SsaConeSearcher( url, format, true, tfact, coding );
        }
    }
}
