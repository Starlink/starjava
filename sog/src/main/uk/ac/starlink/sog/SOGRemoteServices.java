package uk.ac.starlink.sog;

import org.w3c.dom.Element;
import org.apache.axis.AxisFault;

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
        throws AxisFault
    {
        try {
            SOGRemoteControl.getInstance().updateImage();
        }
        catch (Exception e) {
            throw new AxisFault( "Failed to update SoG image display", e  );
        }
    }

    /**
     * Update the image display.
     */
    public static void updateImage( String cookie )
        throws AxisFault
    {
        try {
            SOGRemoteControl.getInstance().updateImage( cookie );
        }
        catch (Exception e) {
            throw new AxisFault( "Failed to update SoG image display", e  );
        }
    }

    /**
     * Display the given file or URL.
     */
    public static void showImage( String fileOrURL )
        throws AxisFault
    {
        try {
            SOGRemoteControl.getInstance().showImage( fileOrURL );
        }
        catch (Exception e) {
            throw new AxisFault( "Failed to display:" + fileOrURL + 
                                 " in SoG", e );
        }
    }

    /**
     * Display the given file or URL.
     */
    public static void showImage( String cookie, String fileOrURL )
        throws AxisFault
    {
        try {
            SOGRemoteControl.getInstance().showImage( cookie, fileOrURL );
        }
        catch (Exception e) {
            throw new AxisFault( "Failed to display:" + fileOrURL + 
                                 " in SoG", e );
        }
    }

    /**
     * Display the given DOM Element as an NDX.
     */
    public static void showNDX( Element element )
        throws AxisFault
    {
        try {
            SOGRemoteControl.getInstance().showNDX( element );
        }
        catch (Exception e) {
            throw new AxisFault( "Failed to display remote NDX", e );
        }
    }

    /**
     * Display the given DOM Element as an NDX.
     */
    public static void showNDX( String cookie, Element element )
        throws AxisFault
    {
        try {
            SOGRemoteControl.getInstance().showNDX( cookie, element );
        }
        catch (Exception e) {
            throw new AxisFault( "Failed to display remote NDX", e );
        }
    }

    /**
     * Return the WCS center of the current image
     */
    public String wcsCenter()
        throws AxisFault
    {
        try {
            return SOGRemoteControl.getInstance().wcsCenter();
        }
        catch (Exception e) {
            throw new AxisFault( "Failed to get centre of WCS", e );
        }
    }

    /**
     * Return the WCS center of the current image
     */
    public String wcsCenter( String cookie )
        throws AxisFault
    {
        try {
            return SOGRemoteControl.getInstance().wcsCenter( cookie );
        }
        catch (Exception e) {
            throw new AxisFault( "Failed to get centre of WCS", e );
        }
    }
}
