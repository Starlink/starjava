/**
 * 
 */
package uk.ac.starlink.splat.iface;

import java.util.EventObject;

/**
 * @author David Andresic
 *
 */
public class SpecListModelSelectionEvent extends EventObject {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Holds the index of newly selected spectra.
	 */
	int[] index;
	
	public SpecListModelSelectionEvent(Object source, int... index) {
		super(source);
		this.index = index;
	}

	public int[] getIndex() {
		return index;
	}
	
}
