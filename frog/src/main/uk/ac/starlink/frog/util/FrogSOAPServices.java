package uk.ac.starlink.frog.util;

import org.w3c.dom.Element;

/**
 * Accessor class for all the SOAP web services offered by the FROG
 * application. 
 *
 * @author Peter W. Draper, Alasdair Allan
 * @version $Id$
 * @since 27-MAY-2002
 */
public class FrogSOAPServices
{
    /**
     * Display a spectrum.
     */
    public static boolean displaySeries( String series )
    {
        return FrogSOAPServer.getInstance().displaySeries( series );
    }

 
}
