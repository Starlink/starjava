package uk.ac.starlink.ttools.taplint;

import java.util.HashMap;
import java.util.Map;

/**
 * Message types for use with a reporter.
 *
 * @author   Mark Taylor
 * @since    29 Jun 2011
 */
public enum ReportType {

    /** Compliance error. */
    ERROR( 'E', "Error", "Errors",
        "Error in operation or standard compliance of the service." ),

    /** Questionable or non-Recommended behaviour. */
    WARNING( 'W', "Warning", "Warnings",
        "Warning that service behaviour is questionable, "
      + "or contravenes a standard recommendation, "
      + "but is not in actual violation of the standard." ),

    /** Information about validator progress. */
    INFO( 'I', "Info", "Infos",
        "Information about progress, for instance details of queries made." ),

    /** Summary of previous reports. */
    SUMMARY( 'S', "Summary", "Summaries",
        "Summary of previous successful/unsuccessful reports." ),

    /** Unable to perform test (internal error or missing precondition). */
    FAILURE( 'F', "Failure", "Failures",
        "Failure of the validator to perform some testing. "
      + "The cause is either some error internal to the validator, "
      + "or some error or missing functionality in the service which "
      + "has already been reported." );

    private final char chr_;
    private final String name_;
    private final String names_;
    private final String description_;
    private static Map<Character,ReportType> charMap_;

    /**
     * Constructor.
     *
     * @param   chr  character distinguishing this type
     * @param  name  human-readable name
     * @param  names plural of <code>name</code>
     * @param  description  short description
     */
    private ReportType( char chr, String name, String names,
                        String description ) {
        chr_ = chr;
        name_ = name;
        names_ = names;
        description_ = description;
    }

    /**
     * Returns the single-character identifier for this type.
     *
     * @return   identifier character
     */
    public char getChar() {
        return chr_;
    }

    /**
     * Returns the human-readable name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns plural of human-readable name.
     *
     * @return  name plural
     */
    public String getNames() {
        return names_;
    }

    /**
     * Returns the description text for this type.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns the type instance corresponding to a given character.
     *
     * @param  chr  case-insensitive character
     * @return  type for which <code>type.getChar()==chr</code>
     */
    public static ReportType forChar( char chr ) {
        if ( charMap_ == null ) {
            Map map = new HashMap<Character,ReportType>();
            ReportType[] types = values();
            for ( int i = 0; i < types.length; i++ ) {
                ReportType type = types[ i ];
                map.put( Character.valueOf( Character
                                           .toUpperCase( type.getChar() ) ),
                         type );
            }
            charMap_ = map;
        }
        return charMap_.get( Character.valueOf( Character
                                               .toUpperCase( chr ) ) );
    }
}
