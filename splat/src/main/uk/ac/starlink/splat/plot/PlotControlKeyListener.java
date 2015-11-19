/**
 * 
 */
package uk.ac.starlink.splat.plot;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * PlotControlKeyListener listens for PlotControl's KeyEvents.
 *
 * @author David Andresic
 * @version $Id$
 */
public class PlotControlKeyListener implements KeyListener {

	private PlotControl plotControl;
	
	public PlotControlKeyListener(PlotControl plotControl) {
		this.plotControl = plotControl;
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		if (e != null) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_DELETE:
					deleteSpectrum();
					break;
				default:
					// noop
					break;
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// so far no operation
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// so far no operation
	}
	
	/* Actual handlers */
	
	private void deleteSpectrum() {
		plotControl.removeCurrentSpectrumFromPlot();
	}
}
