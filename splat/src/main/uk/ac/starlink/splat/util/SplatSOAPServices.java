package uk.ac.starlink.splat.util;

import org.w3c.dom.Element;

/**
 * Accessor class for all the SOAP web services offered by the SOG
 * application. 
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 27-MAY-2002
 */
public class SplatSOAPServices
{
    /**
     * Display a spectrum.
     */
    public static boolean displaySpectrum( String specspec )
    {
        return SplatSOAPServer.getInstance().displaySpectrum( specspec );
    }

    /**
     * Accept an NDX as Element description.
     */
    public static void displayNDX( Element ndxElement )
    {
        SplatSOAPServer.getInstance().displayNDX( ndxElement );
    }
}
