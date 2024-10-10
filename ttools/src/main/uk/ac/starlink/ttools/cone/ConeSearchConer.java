package uk.ac.starlink.ttools.cone;

import java.net.URL;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.URLParameter;
import uk.ac.starlink.ttools.task.ContentCodingParameter;
import uk.ac.starlink.ttools.task.LineTableEnvironment;
import uk.ac.starlink.util.ContentCoding;

/**
 * Coner implementation which uses remote 
 * <a href="http://www.ivoa.net/Documents/latest/ConeSearch.html"
 *    >Cone Search</a> services or similar.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2007
 */
public class ConeSearchConer implements Coner {

    private final URLParameter urlParam_;
    private final ChoiceParameter<String> verbParam_;
    private final ChoiceParameter<ConeServiceType> serviceParam_;
    private final BooleanParameter believeemptyParam_;
    private final ContentCodingParameter codingParam_;
    private final StringParameter formatParam_;
    private int nside_;
    private static final String BELIEVE_EMPTY_NAME = "emptyok";
    private static final String INCONSISTENT_EMPTY_ADVICE =
            BELIEVE_EMPTY_NAME + "=false";

    /**
     * Constructor.
     */
    public ConeSearchConer() {
        nside_ = -1;
        ConeServiceType[] serviceTypes = ConeServiceType.getAllTypes();

        urlParam_ = new URLParameter( "serviceurl" );
        urlParam_.setPrompt( "Base URL for query returning VOTable" );
        urlParam_.setDescription( new String[] {
            "<p>The base part of a URL which defines the queries to be made.",
            "Additional parameters will be appended to this using CGI syntax",
            "(\"<code>name=value</code>\", separated by '&amp;' characters).",
            "If this value does not end in either a '?' or a '&amp;',",
            "one will be added as appropriate.",
            "</p>",
            "<p>See <ref id='coneService'/> for discussion of how to locate",
            "service URLs corresponding to given datasets.",
            "</p>",
        } );

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

        believeemptyParam_ = new BooleanParameter( BELIEVE_EMPTY_NAME );
        believeemptyParam_.setBooleanDefault( true );
        believeemptyParam_.setPrompt( "Believe metadata from empty results?" );
        believeemptyParam_.setDescription( new String[] {
            "<p>Whether the table metadata which is returned from a search",
            "result with zero rows is to be believed.",
            "According to the spirit, though not the letter, of the",
            "cone search standard, a cone search service which returns no data",
            "ought nevertheless to return the correct column headings.",
            "Unfortunately this is not always the case.",
            "If this parameter is set <code>true</code>, it is assumed",
            "that the service behaves properly in this respect; if it does not",
            "an error may result.  In that case, set this parameter",
            "<code>false</code>.  A consequence of setting it false is that",
            "in the event of no results being returned, the task will",
            "return no table at all, rather than an empty one.",
            "</p>",
        } );

        codingParam_ = new ContentCodingParameter();

        formatParam_ = new StringParameter( "dataformat" );
        formatParam_.setPrompt( "Data format type for DAL outputs" );
        formatParam_.setNullPermitted( true );
        StringBuffer formatsDescrip = new StringBuffer();
        for ( ConeServiceType stype : serviceTypes ) {
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

    /**
     * Returns "ICRS", which is the system defined to be used by the
     * Cone Search specification.
     */
    public String getSkySystem() {
        return "ICRS";
    }

    public Parameter<?>[] getParameters() {
        return new Parameter<?>[] {
            serviceParam_,
            urlParam_,
            verbParam_,
            formatParam_,
            believeemptyParam_,
            codingParam_,
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

    public void configureParams( Environment env, Parameter<?> srParam )
            throws TaskException {
        if ( Double.class.isAssignableFrom( srParam.getValueClass() ) ) {
            @SuppressWarnings("unchecked")
            Parameter<Double> dsrParam = (Parameter<Double>) srParam;
            serviceParam_.objectValue( env ).configureRadiusParam( dsrParam );
        }
        else {
            assert false;
        }
    }

    public boolean useDistanceFilter( Environment env )
            throws TaskException {
        return serviceParam_.objectValue( env ).useDistanceFilter();
    }

    public ConeSearcher createSearcher( Environment env, boolean bestOnly )
            throws TaskException {
        ConeServiceType serviceType = serviceParam_.objectValue( env );
        URL url = urlParam_.objectValue( env );
        boolean believeEmpty = believeemptyParam_.booleanValue( env );
        StarTableFactory tfact = LineTableEnvironment.getTableFactory( env );
        ContentCoding coding = codingParam_.codingValue( env );
        return serviceType.createMultiSearcher( env, this, url.toString(),
                                                believeEmpty, tfact, coding );
    }

    public Coverage getCoverage( Environment env ) throws TaskException {
        ConeServiceType serviceType = serviceParam_.objectValue( env );
        URL url = urlParam_.objectValue( env );
        return serviceType.getCoverage( url, nside_ );
    }

    /**
     * Sets the NSIDE parameter for MOC coverage maps.
     * Defaults to -1, which means no settting (up to service).
     *
     * @param  nside  HEALPix NSIDE parameter for MOCs
     */
    public void setNside( int nside ) {
        nside_ = nside;
    }
}
