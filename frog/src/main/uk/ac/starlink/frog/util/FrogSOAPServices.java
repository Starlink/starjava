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
     * Display a spectrum in the current FROG window.
     */
    public static boolean displaySeries( String series ) 
    {
        return FrogSOAPServer.getInstance().displaySeries( series );
    }
    
    /**
     * Display a spectrum in the current FROG window. Security enabled version.
     */
    public static boolean displaySeries( String cookie, String series ) 
    {
        return FrogSOAPServer.getInstance().displaySeries( cookie, series );
    }
    
    /**
     * Display a spectrum.
     */
    public static String getFourierTransform( String series, double minFreq, 
                                              double maxFreq, 
                                              double freqInterval ) 
    {
        return FrogSOAPServer.getInstance()
            .getFourierTransform( series, minFreq, maxFreq, freqInterval );
    }
 
    /**
     * Display a spectrum. Security enabled version.
     */
    public static String getFourierTransform( String cookie, 
                                              String series, double minFreq, 
                                              double maxFreq, 
                                              double freqInterval ) 
    {
        return FrogSOAPServer.getInstance()
            .getFourierTransform( cookie, series, minFreq, maxFreq, 
                                  freqInterval );
    }
}
