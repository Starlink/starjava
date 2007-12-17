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
public class TilingParameter extends Parameter {

    private SkyTiling tiling_;

    private static final String HTM_PREFIX = "htm";
    private static final String HEALPIX_RING_PREFIX = "healpixring";
    private static final String HEALPIX_NEST_PREFIX = "healpixnest";

    /**
     * Constructor.
     *
     * @param  name   parameter name
     */
    public TilingParameter( String name ) {
        super( name );
        setPrompt( "Sky tiling scheme" );
        setUsage( HTM_PREFIX + "<level>" + "|"
                + HEALPIX_NEST_PREFIX + "<nside>" + "|"
                + HEALPIX_RING_PREFIX + "<nside>" );
        setNullPermitted( true );
        setDescription( new String[] {
            "<p>Describes the sky tiling scheme that is in use.",
            "One of the following values may be used:",
            "<ul>",
            "<li><code>" + HTM_PREFIX + "&lt;level&gt;</code>:",
                "Hierarchical Triangular Mesh with a level value of",
                "<code>level</code>.</li>",
            "<li><code>" + HEALPIX_NEST_PREFIX + "&lt;nside&gt;</code>:",
                "HEALPix using the Nest scheme with an nside value of",
                "<code>nside</code>.</li>",
            "<li><code>" + HEALPIX_RING_PREFIX + "&lt;nside&gt;</code>:",
                "HEALPix using the Ring scheme with an nside value of",
                "<code>nside</code>.</li>",
            "</ul>",
            "</p>",
        } );
    }

    public void setValueFromString( Environment env, String value )
            throws TaskException {
        String lvalue = value.toLowerCase();
        if ( lvalue.startsWith( HTM_PREFIX ) ) {
            int level = getNumberSuffix( value, HTM_PREFIX );
            tiling_ = new HtmTiling( level );
        }
        else if ( lvalue.startsWith( HEALPIX_NEST_PREFIX ) ) {
            int nside = getNumberSuffix( value, HEALPIX_NEST_PREFIX );
            tiling_ = new HealpixTiling( nside, true );
        }
        else if ( lvalue.startsWith( HEALPIX_RING_PREFIX ) ) {
            int nside = getNumberSuffix( value, HEALPIX_RING_PREFIX );
            tiling_ = new HealpixTiling( nside, false );
        }
        else {
            throw new ParameterValueException( this, "Unknown tiling scheme \""
                                                   + value + "\"" );
        }
        super.setValueFromString( env, value );
    }

    /**
     * Returns the value of this parameter as a SkyTiling object.
     *
     * @param  env  execution environment
     * @return   tiling value
     */
    public SkyTiling tilingValue( Environment env ) throws TaskException {
        checkGotValue( env );
        return tiling_;
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
