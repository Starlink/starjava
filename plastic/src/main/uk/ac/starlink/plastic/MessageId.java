package uk.ac.starlink.plastic;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Convenience class containing URI constants for some of the standard
 * PLASTIC message definitions.
 * These are taken from the PLASTIC core message definitions list at
 * <a href="http://plastic.sourceforge.net/coremessages.html"/>
 * and possibly elsewhere.
 *
 * @author   Mark Taylor
 * @since    18 Jul 2006
 */
public class MessageId {

    public static final URI TEST_ECHO;

    public static final URI INFO_GETNAME;
    public static final URI INFO_GETIVORN;
    public static final URI INFO_GETDESCRIPTION;
    public static final URI INFO_GETVERSION;
    public static final URI INFO_GETICONURL;

    public static final URI HUB_APPREG;
    public static final URI HUB_APPUNREG;
    public static final URI HUB_STOPPING;

    public static final URI VOT_LOAD;
    public static final URI VOT_LOADURL;
    public static final URI VOT_SHOWOBJECTS;
    public static final URI VOT_HIGHLIGHTOBJECT;

    public static final URI FITS_LOADLINE;
    public static final URI FITS_LOADIMAGE;
    public static final URI FITS_LOADCUBE;

    public static final URI SKY_POINT;

    public static final URI SPECTRUM_LOADURL;

    /** Array of all messages known by this class.  */
    static final URI[] KNOWN_MESSAGES = new URI[] {
        TEST_ECHO = createURI( "ivo://votech.org/test/echo" ),

        INFO_GETNAME = createURI( "ivo://votech.org/info/getName" ),
        INFO_GETIVORN = createURI( "ivo://votech.org/info/getIVORN" ),
        INFO_GETDESCRIPTION =
             createURI( "ivo://votech.org/info/getDescription" ),
        INFO_GETVERSION = createURI( "ivo://votech.org/info/getVersion" ),
        INFO_GETICONURL = createURI( "ivo://votech.org/info/getIconURL" ),

        HUB_APPREG =
            createURI( "ivo://votech.org/hub/event/ApplicationRegistered" ),
        HUB_APPUNREG =
            createURI( "ivo://votech.org/hub/event/ApplicationUnregistered" ),
        HUB_STOPPING = createURI( "ivo://votech.org/hub/event/HubStopping" ),

        VOT_LOAD = createURI( "ivo://votech.org/votable/load" ),
        VOT_LOADURL = createURI( "ivo://votech.org/votable/loadFromURL" ),
        VOT_SHOWOBJECTS = createURI( "ivo://votech.org/votable/showObjects" ),
        VOT_HIGHLIGHTOBJECT =
            createURI( "ivo://votech.org/votable/highlightObject" ),

        FITS_LOADLINE = createURI( "ivo://votech.org/fits/line/loadFromURL" ),
        FITS_LOADIMAGE = createURI( "ivo://votech.org/fits/image/loadFromURL" ),
        FITS_LOADCUBE = createURI( "ivo://votech.org/fits/cube/loadFromURL" ),

        SKY_POINT = createURI( "ivo://votech.org/sky/pointAtCoords" ),

        SPECTRUM_LOADURL = createURI( "ivo://votech.org/spectrum/loadFromURL" ),
    };

    /**
     * Returns an array of known standard message IDs.
     * This comprises all the ones defined as public static members
     * of this class.  The returned object is a clone.
     *
     * @return   known messages list
     */
    public static URI[] getKnownMessages() {
        return (URI[]) KNOWN_MESSAGES.clone();
    }

    /**
     * Turns a string into a URI without throwing a checked exception.
     *
     * @param  id  text of URI
     * @return  URI 
     * @throws  IllegalArgumentException  in case of error
     */
    private static final URI createURI( String id ) {
        try {
            return new URI( id );
        }
        catch ( URISyntaxException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URI: " + id )
                 .initCause( e );
        }
    }
}
