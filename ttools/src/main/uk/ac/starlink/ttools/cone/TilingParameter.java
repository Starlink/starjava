package uk.ac.starlink.ttools.cone;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for specifying SkyTiling values.
 *
 * @author   Mark Taylor
 * @since    13 Dec 2007
 */
public class TilingParameter extends Parameter<SkyTiling> {

    private static final String HEALPIX_NEST_PREFIX2 = "hpx";
    private static final String HEALPIX_NEST_PREFIX = "healpixnest";
    private static final String HEALPIX_RING_PREFIX = "healpixring";
    private static final String HTM_PREFIX = "htm";

    /**
     * Constructor.
     *
     * @param  name   parameter name
     */
    @SuppressWarnings("this-escape")
    public TilingParameter( String name ) {
        super( name, SkyTiling.class, true );
        setPrompt( "Sky tiling scheme" );
        setUsage( HEALPIX_NEST_PREFIX2 + "<K>" + "|"
                + HEALPIX_NEST_PREFIX + "<K>" + "|"
                + HEALPIX_RING_PREFIX + "<K>" + "|"
                + HTM_PREFIX + "<K>" );
        setNullPermitted( true );
        setDescription( new String[] {
            "<p>Describes the sky tiling scheme that is in use.",
            "One of the following values may be used:",
            "<ul>",
            "<li><code>" + HEALPIX_NEST_PREFIX2 + "K</code>:",
                "alias for <code>" + HEALPIX_NEST_PREFIX
                                   + "K</code></li>",
            "<li><code>" + HEALPIX_NEST_PREFIX + "K</code>:",
                "HEALPix using the Nest scheme at order <code>K</code></li>",
            "<li><code>" + HEALPIX_RING_PREFIX + "K</code>:",
                "HEALPix using the Ring scheme at order <code>K</code></li>",
            "<li><code>" + HTM_PREFIX + "K</code>:",
                "Hierarchical Triangular Mesh at level <code>K</code></li>",
            "</ul>",
            "So for instance",
            "<code>" + HEALPIX_NEST_PREFIX2 + "5</code> or",
            "<code>" + HEALPIX_NEST_PREFIX + "5</code>",
            "would both indicate the HEALPix NEST tiling scheme at order 5.",
            "</p>",
            "<p>At level K, there are 12*4^K HEALPix pixels,",
            "or 8*4^K HTM pixels on the sky.",
            "More information about these tiling schemes can be found at",
            "the <webref url='https://healpix.jpl.nasa.gov/'>HEALPix</webref>",
            "and <webref url='http://www.skyserver.org/htm/'>HTM</webref>",
            "web sites.",
            "</p>",
        } );
    }

    public SkyTiling stringToObject( Environment env, String svalue )
            throws TaskException {
        String lvalue = svalue.toLowerCase();
        if ( lvalue.startsWith( HEALPIX_NEST_PREFIX2 ) ) {
            int k = getNumberSuffix( svalue, HEALPIX_NEST_PREFIX2 );
            return new HealpixTiling( k, true );
        }
        else if ( lvalue.startsWith( HEALPIX_NEST_PREFIX ) ) {
            int k = getNumberSuffix( svalue, HEALPIX_NEST_PREFIX );
            return new HealpixTiling( k, true );
        }
        else if ( lvalue.startsWith( HEALPIX_RING_PREFIX ) ) {
            int k = getNumberSuffix( svalue, HEALPIX_RING_PREFIX );
            return new HealpixTiling( k, false );
        }
        else if ( lvalue.startsWith( HTM_PREFIX ) ) {
            int level = getNumberSuffix( svalue, HTM_PREFIX );
            return new HtmTiling( level );
        }
        else {
            throw new ParameterValueException( this, "Unknown tiling scheme \""
                                                   + svalue + "\"" );
        }
    }

    /**
     * Returns the value of this parameter as a SkyTiling object.
     *
     * @param  env  execution environment
     * @return   tiling value
     */
    public SkyTiling tilingValue( Environment env ) throws TaskException {
        return objectValue( env );
    }

    /**
     * Sets the default value of this parameter to a HEALPix NEST instance
     * of a given order.
     *
     * @param  k  healpix order, or -1 for no default
     */
    public void setHealpixNestDefault( int k ) {
        setStringDefault( k >= 0 ? ( HEALPIX_NEST_PREFIX2 + k ) : null );
    }

    /**
     * Parses a string of the form &lt;prefix&gt;&lt;num&gt;, returning
     * the integer value of the &lt;num&gt; part.
     *
     * @param   value   string to parse
     * @param   prefix  prefix part
     * @return  integer value of suffix part
     * @throws  TaskException  if the parsing fails
     */
    private int getNumberSuffix( String value, String prefix ) 
            throws TaskException {
        String svalue = value.substring( prefix.length() );
        try {
            return Integer.parseInt( svalue );
        }
        catch ( NumberFormatException e ) {
            throw new ParameterValueException( this, "\"" + svalue + "\" "
                                             + "not numeric" );
        }
    }
}
