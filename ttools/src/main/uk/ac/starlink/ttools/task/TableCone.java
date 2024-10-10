package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.ttools.cone.ConeSearcher;
import uk.ac.starlink.ttools.cone.ConeServiceType;
import uk.ac.starlink.ttools.cone.ServiceConeSearcher;
import uk.ac.starlink.ttools.cone.SiaConeSearcher;
import uk.ac.starlink.ttools.cone.SsaConeSearcher;
import uk.ac.starlink.ttools.convert.SkySystem;
import uk.ac.starlink.util.ContentCoding;

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
    private final ChoiceParameter<ConeServiceType> serviceParam_;
    private final ContentCodingParameter codingParam_;
    private final ChoiceParameter<SkySystem> sysParam_;
    private final StringParameter formatParam_;

    @SuppressWarnings("this-escape")
    public TableCone() {
        super( "Executes a Cone Search-like query", new ChoiceMode(), true );
        List<Parameter<?>> coneParams = new ArrayList<>();

        final String sysParamName = "skysys";

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
        coneParams.add( urlParam_ );

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
        coneParams.add( lonParam_ );

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
        coneParams.add( latParam_ );

        radiusParam_ = new DoubleParameter( "radius" );
        radiusParam_.setPrompt( "Radius in degrees" );
        radiusParam_.setUsage( "<degrees>" );
        radiusParam_.setDescription( new String[] {
            "<p>Search radius in degrees.",
            "</p>",
        } );
        coneParams.add( radiusParam_ );

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
        coneParams.add( sysParam_ );

        ConeServiceType[] serviceTypes = ConeServiceType.getAllTypes();
        serviceParam_ =
            new ChoiceParameter<ConeServiceType>( "servicetype", serviceTypes );
        serviceParam_.setPrompt( "Search service type" );
        StringBuffer typesDescrip = new StringBuffer();
        for ( ConeServiceType stype : serviceTypes ) {
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
        coneParams.add( serviceParam_ );

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
        coneParams.add( verbParam_ );

        codingParam_ = new ContentCodingParameter();
        coneParams.add( codingParam_ );

        formatParam_ = new StringParameter( "dataformat" );
        formatParam_.setPrompt( "Data format type for DAL outputs" );
        formatParam_.setNullPermitted( true );
        StringBuffer formatsDescrip = new StringBuffer();
        for ( int i = 0; i < serviceTypes.length; i++ ) {
            ConeServiceType stype = serviceTypes[ i ];
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
        coneParams.add( formatParam_ );

        getParameterList().addAll( 0, coneParams );
    }

    public TableProducer createProducer( Environment env )
            throws TaskException {
        URL url = urlParam_.objectValue( env );
        ConeServiceType serviceType = serviceParam_.objectValue( env );
        serviceType.configureRadiusParam( radiusParam_ );
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
            serviceType.createSingleSearcher( env, this, url.toString(), tfact,
                                              coding );
        return new TableProducer() {
            public StarTable getTable() throws IOException {
                return searcher.performSearch( raDeg, decDeg, radius );
            }
        };
    }

    /**
     * Returns the parameter used to acquire the DAL requested data format.
     *
     * @return  format parameter
     */
    public Parameter<String> getFormatParameter() {
        return formatParam_;
    }

    /**
     * Returns the parameter used to acquire the requested verbosity.
     *
     * @return   verbosity parameter
     */
    public Parameter<String> getVerbosityParameter() {
        return verbParam_;
    }

    /**
     * Returns the parameter used to acquire the service URL.
     *
     * @return  service URL parameter
     */
    public Parameter<URL> getServiceUrlParameter() {
        return urlParam_;
    }

    /**
     * Returns the parameter used to acquire the service type.
     *
     * @return  service type parameter
     */
    public Parameter<ConeServiceType> getServiceTypeParameter() {
        return serviceParam_;
    }

    /**
     * Returns the parameter used to acquire the longitude in degrees.
     *
     * @return  longitude parameter
     */
    public Parameter<Double> getLongitudeParameter() {
        return lonParam_;
    }

    /**
     * Returns the parameter used to acquire the latitude in degrees.
     *
     * @return  latitude parameter
     */
    public Parameter<Double> getLatitudeParameter() {
        return latParam_;
    }

    /**
     * Returns the parameter used to acquire the search radius in degrees.
     *
     * @return  radius parameter
     */
    public Parameter<Double> getRadiusDegParameter() {
        return radiusParam_;
    }

    /**
     * Returns the parameter used to acquire the sky system.
     *
     * @return  skysys parameter
     */
    public Parameter<SkySystem> getSkySystemParameter() {
        return sysParam_;
    }
}
