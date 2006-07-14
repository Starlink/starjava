package uk.ac.starlink.plastic;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Basic implementation of the PlasticApplication interface.
 * Could serve as a superclass for other concrete implementations.
 *
 * @author   Mark Taylor
 * @since    14 Jul 2006
 */
public class BasicApplication implements PlasticApplication {

    private static final URI GET_VERSION =
        PlasticUtils.createURI( "ivo://votech.org/info/getVersion" );
    private static final URI GET_NAME = 
        PlasticUtils.createURI( "ivo://votech.org/info/getName" );
    private static final URI GET_DESCRIPTION =
        PlasticUtils.createURI( "ivo://votech.org/info/getDescription" );
    private static final URI GET_ICON =
        PlasticUtils.createURI( "ivo://votech.org/info/getIconURL" );
    private static final URI GET_IVORN =
        PlasticUtils.createURI( "ivo://votech.org/info/getIVORN" );
    private static final URI ECHO =
        PlasticUtils.createURI( "ivo://votech.org/test/echo" );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.plastic" );

    private final String name_;
    private String description_;
    private String iconUrl_;
    private String ivorn_;
    private String version_ = PlasticUtils.PLASTIC_VERSION;

    /**
     * Constructor.
     *
     * @param   name  registered application name
     */
    public BasicApplication( String name ) {
        name_ = name;
    }

    public String getName() {
        return name_;
    }

    /**
     * Sets the application description text.
     *
     * @param  description  description text
     */
    public void setDescription( String description ) {
        description_ = description;
    }

    /**
     * Sets the PLASTIC version string.
     * Default is {@link PlasticUtils#PLASTIC_VERSION}.
     *
     * @param  version  plastic version string
     */
    public void setVersion( String version ) {
        version_ = version;
    }

    /** 
     * Sets the icon URL for the application.
     *
     * @param  iconUrl   icon URL
     */
    public void setIconUrl( String iconUrl ) {
        if ( iconUrl != null ) {
            try {
                new URL( iconUrl );
            }
            catch ( MalformedURLException e ) {
                throw (IllegalArgumentException)
                      new IllegalArgumentException( "Bad URL: " + iconUrl )
                     .initCause( e );
            }
        }
        iconUrl_ = iconUrl;
    }

    /**
     * Sets the IVORN for the application.
     *
     * @param  ivorn IVORN
     */
    public void setIvorn( String ivorn ) {
        ivorn_ = ivorn;
    }

    public URI[] getSupportedMessages() {
        List msgList = new ArrayList();
        msgList.add( GET_NAME );
        if ( description_ != null ) {
            msgList.add( GET_DESCRIPTION );
        }
        if ( version_ != null ) {
            msgList.add( GET_VERSION );
        }
        if ( iconUrl_ != null ) {
            msgList.add( GET_ICON );
        }
        if ( ivorn_ != null ) {
            msgList.add( GET_IVORN );
        }
        msgList.add( ECHO );
        return (URI[]) msgList.toArray( new URI[ 0 ] );
    }

    public Object perform( URI sender, URI msg, List args ) {
        if ( ECHO.equals( msg ) ) {
            return args.get( 0 );
        }
        else if ( GET_NAME.equals( msg ) ) {
            return name_;
        }
        else if ( GET_DESCRIPTION.equals( msg ) ) {
            return description_;
        }
        else if ( GET_VERSION.equals( msg ) ) {
            return version_;
        }
        else if ( GET_ICON.equals( msg ) ) {
            return iconUrl_;
        }
        else if ( GET_IVORN.equals( msg ) ) {
            return ivorn_;
        }
        else {
            logger_.warning( "Unexpected unknown message from hub: " + msg );
            return new ArrayList();
        }
    }
}
