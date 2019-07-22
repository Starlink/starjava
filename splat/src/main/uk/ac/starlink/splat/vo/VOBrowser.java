package uk.ac.starlink.splat.vo;

import java.io.IOException;

import javax.swing.JFrame;

import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.table.StarTable;

public interface VOBrowser  {
	
	void displaySpectra( Props[] propList, boolean display);

}
