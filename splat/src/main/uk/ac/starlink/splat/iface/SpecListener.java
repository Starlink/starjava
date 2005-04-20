/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     29-SEP-2000(Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.util.EventListener;
/**
 * SpecListener defines an interface used when listening for
 * SpecChangedEvent events.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface SpecListener 
    extends EventListener 
{
    /**
     *  Sent when a new spectrum is added.
     */
    public void spectrumAdded( SpecChangedEvent e );

    /**
     *  Send when a spectrum is removed.
     */
    public void spectrumRemoved( SpecChangedEvent e );

    /**
     *  Send when a spectrum property is changed.
     */
    public void spectrumChanged( SpecChangedEvent e );

    /**
     *  Send when a spectrum has its data, units or coordinates changed.
     */
    public void spectrumModified( SpecChangedEvent e );

    /**
     *  Send when a spectrum becomes "current".
     */
    public void spectrumCurrent( SpecChangedEvent e );
}
