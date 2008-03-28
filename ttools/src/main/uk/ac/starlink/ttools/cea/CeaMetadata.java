package uk.ac.starlink.ttools.cea;

import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.Stilts;

/**
 * Encapsulates the metadata required for a CEA application description.
 *
 * @author   Mark Taylor
 * @since    20-MAR-2008
 */
public class CeaMetadata {

    private final String ivorn_;
    private final String shortName_;
    private final String longName_;
    private final String refUrl_;
    private final String description_;

    private static final String BASE_IVORN = "ivo://uk.ac.starlink/stilts";
    private static final String MANUAL_URL =
        "http://www.starlink.ac.uk/stilts/sun256/";

    /**
     * Constructor.
     *
     * @param  ivorn   application ID
     * @param  shortName  short name
     * @param  longName  long name
     * @param  refUrl   URL for reference documentation
     * @param  description  textual description
     */
    public CeaMetadata( String ivorn, String shortName, String longName, 
                        String refUrl, String description ) {
        ivorn_ = ivorn;
        shortName_ = shortName;
        longName_ = longName;
        refUrl_ = refUrl;
        description_ = description;
    }

    /**
     * Returns the application ID.
     *
     * @return  ivorn
     */
    public String getIvorn() {
        return ivorn_;
    }

    /**
     * Returns the short name of the application.
     *
     * @return  short name
     */
    public String getShortName() {
        return shortName_;
    }

    /**
     * Returns the long name of the application.
     *
     * @return  long name
     */
    public String getLongName() {
        return longName_;
    }

    /**
     * Returns the URL for reference documentation.
     *
     * @return   reference URL
     */
    public String getRefUrl() {
        return refUrl_;
    }

    /**
     * Returns description text for this application.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Creates a metadata object for the STILTS application with a given
     * list of tasks.
     *
     * @param   tasks   tasks to include in this deployment
     * @return   new metadata object
     */
    public static CeaMetadata createStiltsMetadata( CeaTask[] tasks ) {
        String ivorn = BASE_IVORN;
        String shortName = "STILTS";
        String longName =
            "STILTS - Starlink Tables Infrastructure Library Tool Set"
          + " v" + Stilts.getVersion();
        String refUrl = MANUAL_URL;
        StringBuffer dbuf = new StringBuffer();
        dbuf.append( "STILTS is a package which provides a number of " )
            .append( "table manipulation functions.\n" )
            .append( "The following tasks (profiles) are provided:\n" );
        for ( int i = 0; i < tasks.length; i++ ) {
            CeaTask task = tasks[ i ];
            dbuf.append( "   " ) 
                .append( task.getName() )
                .append( ": " )
                .append( "      " )
                .append( task.getPurpose() )
                .append( '\n' );
        }
        String desc = dbuf.toString();
        return new CeaMetadata( ivorn, shortName, longName, refUrl, desc );
    }

    /**
     * Creates a metadata object for a CEA application providing only a
     * single STILTS task.
     *
     * @param   task   single task
     * @return   new metadata object
     */
    public static CeaMetadata createTaskMetadata( CeaTask task ) {
        String ivorn = BASE_IVORN + "/" + task.getName();
        String shortName = task.getName();
        String longName = task.getName() + " from STILTS"
                        + " v" + Stilts.getVersion();
        String refUrl = MANUAL_URL + task.getName() + ".html";
        String desc = task.getPurpose();
        return new CeaMetadata( ivorn, shortName, longName, refUrl, desc );
    }
}
