package uk.ac.starlink.sog.photom;

import org.w3c.dom.Element;
import java.io.IOException;

/**
 * Accessor class for test SOAP web services
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 27-MAY-2002
 */
public class PhotomWSServices
{
    /**
     * Perform the calculations
     */
    public static Element autophotom( Element ndxElement, 
                                      Element photomListElement,
                                      Element globalsElement )
        throws IOException
    {
        return PhotomWS.getInstance().autophotom( ndxElement, 
                                                  photomListElement,
                                                  globalsElement );
    }

    /**
     * Perform the calculations
     */
    public static void simpleAutophotom( Element element, 
                                         String positionsFile,
                                         String resultsFile )
        throws IOException
    {
        PhotomWS.getInstance().autophotom( element, positionsFile, 
                                           resultsFile );
    }

}
