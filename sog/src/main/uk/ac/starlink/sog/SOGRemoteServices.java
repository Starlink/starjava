package uk.ac.starlink.sog;

import org.w3c.dom.Element;

/**
 * Accessor class for all the SOAP web services offered by the SOG
 * application. 
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 27-MAY-2002
 */
public class SOGRemoteServices
{
    /**
     * Update the image display.
     */
    public static void updateImage()
    {
        System.out.println( "updateImage" );
        SOGRemoteControl.getInstance().updateImage();
    }

    /**
     * Display the given file or URL.
     */
    public static void showImage( String fileOrURL )
    {
        System.out.println( "showImage" );
        SOGRemoteControl.getInstance().showImage( fileOrURL );
    }

    /**
     * Display the given DOM Element as an NDX.
     */
    public static void showNDX( Element element )
    {
        System.out.println( "showImage" );
        SOGRemoteControl.getInstance().showNDX( element );
    }

    /**
     * Return the WCS center of the current image
     */
    public String wcsCenter()
    {
        System.out.println( "wcsCenter" );
        return SOGRemoteControl.getInstance().wcsCenter();
    }
}
