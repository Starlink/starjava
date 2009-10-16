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
 */
package uk.ac.starlink.splat.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.astrogrid.samp.Message;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.UniformCallActionManager;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.GlobalSpecPlotList;
import uk.ac.starlink.util.URLUtils;

/**
 * Provides GUI actions for sending spectra by SAMP.
 *
 * @author Mark Taylor
 * @version $Id$
 */
public class SpectrumSendActionManager
    extends UniformCallActionManager
    implements Transmitter, ListSelectionListener
{
    /**
     * Global list of spectra.
     */
    private JList specList;

    /**
     * Currently selected index in the global list of spectra.
     */
    private int selectedIndex = -1;

    /**
     * Constructor.
     *
     * @param  specList  global list of spectra
     * @param  hubConnector  controls connection with SAMP hub
     */
    public SpectrumSendActionManager( JList specList,
                                      GuiHubConnector hubConnector )
    {
        super( specList, hubConnector, "spectrum.load.ssa-generic",
               "spectrum" );
        this.specList = specList;
        specList.addListSelectionListener( this );
        updateSpecState();
    }

    /**
     * Implement ListSelectionListener interface to ensure that this object
     * keeps track of the current selection state in the global spectrum list.
     */
    public void valueChanged( ListSelectionEvent e ) {
        updateSpecState();
    }

    /**
     * Invoked when the selection state of the global spectrum list
     * may have changed.
     */
    private void updateSpecState() {
        int[] indices = specList.getSelectedIndices();
        selectedIndex = ( indices == null || indices.length != 1 )
                      ? -1
                      : indices[ 0 ];
        setEnabled( selectedIndex >= 0 );
    }

    /**
     * Returns the currently-selected spectrum, if any.
     */
    private SpecData getSpecData()
    {
        return GlobalSpecPlotList.getInstance().getSpectrum( selectedIndex );
    }

    /**
     * Constructs and returns a message for transmitting load of the
     * currently selected spectrum.
     */
    protected Map createMessage()
        throws IOException, SplatException
    {
        SpecData spec = getSpecData();
        String fmt = spec.getDataFormat();
        String mime = null;
        URL locUrl = null;
        File tmpFile = null;

        //  See if we already have a VOTable spectrum ready to use.
        if ( "VOTable".equals( fmt ) ) {
            if ( new File( spec.getFullName() ).exists() ) {
                mime = "application/x-votable+xml";
                locUrl = getUrl( spec.getFullName() );
            }
        }

        //  Otherwise, write it as a FITS table and use that. Note we cannot
        //  find out if a FITS table already exists as StarTables are
        //  anonymous. Use "fits-basic" as SPLAT gets distracted by the
        //  primary array.
        if ( locUrl == null ) {
            tmpFile = File.createTempFile( "spec", ".fits");
            tmpFile.deleteOnExit();
            locUrl = URLUtils.makeFileURL( tmpFile );
            mime = "application/fits";
            spec = SpecDataFactory.getInstance()
                .getTableClone( spec, tmpFile.toString(),
                                "fits-basic" );
            spec.save();
            assert tmpFile.exists() : tmpFile;
        }
        assert mime != null;
        assert locUrl != null;

        //  Prepare a metadata map describing the spectrum.
        //  There should probably be more items in here.
        Map meta = new HashMap();
        meta.put( "Access.Reference", locUrl.toString() );
        meta.put( "Access.Format", mime );
        String shortName = spec.getShortName();
        if ( shortName != null && shortName.trim().length() > 0 ) {
            meta.put( "vox:image_title", shortName );
            meta.put( "Target.Name", shortName );
        }

        //  Units.
        String dataUnits = spec.getDataUnits();
        String coordUnits = spec.getFrameSet().getUnit( 1 );
        if ( dataUnits != null && coordUnits != null ) {
            if ( ! coordUnits.equals( "" ) ) {
                meta.put( "vox:spectrum_units",
                          coordUnits + " " + dataUnits );
                meta.put( "Spectrum.Char.SpectralAxis.unit", coordUnits );
                meta.put( "Spectrum.Char.FluxAxis.unit", dataUnits );
            }
        }

        //  Columns.
        String xColName = spec.getXDataColumnName();
        String yColName = spec.getYDataColumnName();
        if ( xColName != null && yColName != null ) {
            meta.put( "vox:spectrum_axes", xColName + " " + yColName );
            meta.put( "Spectrum.Char.SpectralAxis.Name", xColName );
            meta.put( "Spectrum.Char.FluxAxis.Name", yColName );
        }

        //  Prepare and return the actual message.
        Message msg = new Message( "spectrum.load.ssa-generic" );
        msg.addParam( "url", locUrl.toString() );
        msg.addParam( "meta", meta );
        if ( shortName != null && shortName.trim().length() > 0 ) {
            msg.addParam( "name", shortName );
        }
        return msg;
    }

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
    private static URL getUrl( String loc )
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
}
