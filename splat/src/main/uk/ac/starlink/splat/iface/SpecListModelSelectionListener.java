/**
 * 
 */
package uk.ac.starlink.splat.iface;

import java.util.EventListener;

/**
 * SpecListModelSelectionListener defines an interface used when listening for
 * SpecListModelSelectionEvent events.
 *
 * @author David Andresic
 * @version $Id$
 *
 */
public interface SpecListModelSelectionListener extends EventListener {
	
	public void selectionChanged(SpecListModelSelectionEvent e);
	
}
