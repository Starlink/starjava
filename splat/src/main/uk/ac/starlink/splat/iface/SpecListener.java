package uk.ac.starlink.splat.iface;

import java.util.EventListener;
/**
 * SpecListener defines an interface used when listening for
 * SpecChangedEvent events.
 *
 * @since $Date$
 * @since 29-SEP-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public interface SpecListener extends EventListener {

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
     *  Send when a spectrum becomes "current".
     */
    public void spectrumCurrent( SpecChangedEvent e );
}
