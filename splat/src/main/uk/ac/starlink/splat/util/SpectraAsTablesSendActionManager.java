/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     06-MAR-2009 (Mark Taylor):
 *        Original version.
 *     14-JUL-2009 (Peter Draper):
 *        Give up on 1D FITS and always transmit FITS tables.
 *     16-OCT-2009 (Peter Draper):
 *        Send SSA meta-data as required by HIPE (paul.balm@sciops.esi.int)
 *        More SSA 1.0 compatible.
 *     16-FEB-2016 (David Andresic):
 *        Send spectrum as table.
 */
package uk.ac.starlink.splat.util;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.gui.GuiHubConnector;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.GlobalSpecPlotList;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.vo.DataLinkParams;
import uk.ac.starlink.splat.vo.SSAQueryBrowser;
import uk.ac.starlink.util.URLUtils;

/**
 * Provides GUI actions for sending spectra by SAMP.
 *
 * @author Mark Taylor
 * @author David Andresic
 * @version $Id$
 */
public abstract class SpectraAsTablesSendActionManager
    extends SplatUniformCallActionManager
    implements EventEnabledTransmitter, ListSelectionListener
{
	protected static enum SOURCE_ENUM {
		JLIST,
		SSAP_BROWSER
	}
	
	private SOURCE_ENUM spectraSource;
	
	/**
	 * Message type
	 */
	private String mType;
	
	/**
	 * Send type
	 */
	private String sendType;
	
	/**
     * Global list of spectra.
     */
    private JList specList;
    
    /**
     * SSA Query Browser instance
     */
    private SSAQueryBrowser ssaBrowser;

    /**
     * Currently selected index in the global list of spectra.
     */
    private int selectedIndex = -1;
    
    /**
     * Map holding URL of each spectrum
     */
    private Map<SpecData, String> spectraUrls = new HashMap<SpecData, String>();

    /**
     * Constructor.
     *
     * @param  specList  global list of spectra
     * @param  hubConnector  controls connection with SAMP hub
     */
    public SpectraAsTablesSendActionManager( SSAQueryBrowser ssaBrowser,
                                      GuiHubConnector hubConnector, String mtype, String sendtype )
    {
        super( ssaBrowser, hubConnector, mtype,
        		sendtype );
        this.ssaBrowser = ssaBrowser;
        this.spectraSource = SOURCE_ENUM.SSAP_BROWSER;
        updateSpecState();
        this.mType = mtype;
        this.sendType = sendtype;
    }
    
    /**
     * Constructor.
     *
     * @param  specList  global list of spectra
     * @param  hubConnector  controls connection with SAMP hub
     */
    public SpectraAsTablesSendActionManager( JList specList,
                                      GuiHubConnector hubConnector, String mtype, String sendtype )
    {
        super( specList, hubConnector, mtype,
        		sendtype );
        this.specList = specList;
        this.spectraSource = SOURCE_ENUM.JLIST;
        specList.addListSelectionListener( this );
        updateSpecState();
        this.mType = mtype;
        this.sendType = sendtype;
    }

    /**
     * Implement ListSelectionListener interface to ensure that this object
     * keeps track of the current selection state in the global spectrum list.
     */
    public void valueChanged( ListSelectionEvent e ) {
        updateSpecState();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {updateSpecState();}
    @Override
    public void mouseEntered(MouseEvent arg0) {}
    @Override
    public void mouseExited(MouseEvent arg0) {}
    @Override
    public void mousePressed(MouseEvent arg0) {}
    @Override
    public void mouseReleased(MouseEvent arg0) {}
    
    /**
     * Invoked when the selection state of the global spectrum list
     * may have changed.
     */
    protected void updateSpecState() {
        
    	switch (getSpectraSource()) {
    		case JLIST:
    			int[] indices = specList.getSelectedIndices();
    	        selectedIndex = ( indices == null || indices.length != 1 )
    	                      ? -1
    	                      : indices[ 0 ];
    	        setEnabled( selectedIndex >= 0 );
    			break;
    		case SSAP_BROWSER:
    			List<Props> props = ssaBrowser.getSpectraAsList(true);
    			setEnabled(props != null && props.size() > 0);
    			break;
    		default:
    			throw new IllegalStateException("Unsupported source type.");
    	}
    	
    	
    }

    /**
     * Returns the currently-selected spectrum, if any.
     */
    protected List<SpecData> getSpecData()
    {
        if (getSpectraSource() == null) {
        	throw new IllegalStateException("There is no spectra source defined for SAMP send action manager.");
        }
        
        switch (getSpectraSource()) {
        	case JLIST:
        		return Arrays.asList(GlobalSpecPlotList.getInstance().getSpectrum( selectedIndex ));
        	case SSAP_BROWSER:
        		return getSpectraFromSSAQueryBrowser();
        	default:
        		throw new IllegalStateException("Unsupported spectra source.");
        }
        
    }

    /**
     * Constructs and returns a message for transmitting load of the
     * currently selected spectrum.
     */
    protected abstract List<Message> createMessages()
        throws IOException, SplatException;

    /**
     * Returns a URL corresponding to an existing resource given by a
     * location string, if possible.  If <code>loc</code> is an
     * <em>existing</em> file, a file-type URL is returned.
     * Otherwise, if <code>loc</code> can be parsed as a URL,
     * that is returned.  Otherwise, <code>null</code> is returned.
     * 
     * @param   loc  string pointing to resource (URL or filename)
     * @return   URL describing <code>loc</code>, or null
     */
    protected static URL getUrl( String loc )
    {
        if ( loc == null ) {
            return null;
        }
        File locFile = new File( loc );
        if ( locFile.exists() ) {
            return URLUtils.makeFileURL( locFile );
        }
        else {
            try {
                return new URL( loc );
            }
            catch ( MalformedURLException e ) {
                return null;
            }
        }
    }
    
    public SOURCE_ENUM getSpectraSource() {
		return spectraSource;
	}
    
    /**
     * Extracts the currently selected spectra from SSA Query Browser
     * 
     * @return
     */
    private List<SpecData> getSpectraFromSSAQueryBrowser() {
    	List<SpecData> spectra = new LinkedList<SpecData>();
    	List<Props> props = ssaBrowser.getSpectraAsList(true);
    	spectraUrls.clear();
    	
    	// Inspired by SplatBrowser.tryAddSpectrum() and simplified
    	
    	if (props != null) {
    		for (Props p : props) {
    			System.out.println("and146: props url: " + p.getDataLinkRequest() + " / " + p.getidValue() + " / " + p.getShortName() + " / " + p.getSpectrum());
    			if ( p.getType() == SpecDataFactory.SED || p.getType() == SpecDataFactory.TABLE ) {
    				try {
    					List<SpecData> sp = Arrays.asList(SpecDataFactory.getInstance().expandXMLSED( p.getSpectrum() ));
    					for (SpecData s : sp) {
    						spectra.add(s);
    						spectraUrls.put(s, p.getSpectrum());
    					}
//    					spectra.addAll(Arrays.asList(SpecDataFactory.getInstance().expandXMLSED( p.getSpectrum() )));
					} catch (SplatException e) {
						throw new RuntimeException("Unable to extract spectra from SSA Query Browser.", e);
					}
    			} else {
    				try {
	    				if (p.getType() == SpecDataFactory.DATALINK) {
	    					DataLinkParams dlparams = new DataLinkParams(p.getSpectrum());
	                        p.setSpectrum(dlparams.getQueryAccessURL(0)); // get the accessURL for the first service read
	                        String stype = null;
	                        if (p.getDataLinkFormat() != null ) { // see if user has changed the output format
	                        	stype = p.getDataLinkFormat();
	                        	p.setType(SpecDataFactory.mimeToSPLATType(stype));
	                            //props.setObjectType(SpecDataFactory.mimeToObjectType(stype));
	                        }
	                        else if ( dlparams.getQueryContentType(0) == null || dlparams.getQueryContentType(0).isEmpty()) //if not, use contenttype
	                            p.setType(SpecDataFactory.GUESS);
	                        else { 
	                            stype = dlparams.getQueryContentType(0);
	                        	p.setType(SpecDataFactory.mimeToSPLATType(stype));
	                        	//props.setObjectType(SpecDataFactory.mimeToObjectType(stype));
	                        }
	    				}
	    				List<SpecData> sp = SpecDataFactory.getInstance().get( p.getSpectrum(), p.getType() );
    					for (SpecData s : sp) {
    						spectra.add(s);
    						spectraUrls.put(s, p.getSpectrum());
    					}
//	    				spectra.addAll(SpecDataFactory.getInstance().get( p.getSpectrum(), p.getType() )); ///!!! IF it's a list???
    				} catch (Exception e) {
    					throw new RuntimeException("Unable to extract spectra from SSA Query Browser.", e);
    				}
    			}
    		}
    	}
    	
    	return spectra;
    }
    
    /**
     * Returns URL of the given spectrum, if exists
     * 
     * @param spec
     * @return
     */
    protected String getURLOfSpec(SpecData spec) {
    	return spectraUrls.get(spec);
    }
}
