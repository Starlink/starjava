/*
 * Copyright (C) 2006 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-AUG-2006 (Mark Taylor):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;

import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.SpectrumIO;

/**
 * Implements the PlasticListener interface on behalf of the SPLAT
 * application.
 *
 * @author   Mark Taylor
 * @version $Id$
 */
public class SplatPlastic
     extends HubManager
{

    /**
     * List of application-specific PLASTIC messages supported by this class.
     */
    protected static final URI[] SUPPORTED_MESSAGES = new URI[] {
        MessageId.FITS_LOADLINE,
        MessageId.FITS_LOADIMAGE,
        MessageId.VOT_LOADURL,
        MessageId.INFO_GETDESCRIPTION,
        MessageId.INFO_GETICONURL,
    };

    /**
     * Controlling browser object.
     */
    protected SplatBrowser browser;

    /**
     * SpecDataFactory instance.
     */
    protected SpecDataFactory specDataFactory = SpecDataFactory.getInstance();

    /**
     * Model controlling whether spectra received as VOTables are accepted.
     */
    protected ButtonModel acceptVOTableModel = new DefaultButtonModel();

    /**
     * Model controlling whether spectra received as FITS lines are accepted.
     */
    protected ButtonModel acceptFITSLineModel = new DefaultButtonModel();

    /**
     * Model controlling whether spectra received as FITS images are accepted.
     */
    protected ButtonModel acceptFITSTableModel = new DefaultButtonModel();

    /**
     * Create a new plastic listener for SPLAT.
     */
    public SplatPlastic( SplatBrowser browser )
    {
        super( "splat-vo", SUPPORTED_MESSAGES );
        this.browser = browser;
        acceptVOTableModel.setSelected( true );
        acceptFITSLineModel.setSelected( true );
        acceptFITSTableModel.setSelected( false );
    }

    /**
     * Does the work for processing a request from the hub.
     *
     * @param   sender  sender ID
     * @param   message message ID
     * @param   args    message argument list
     * @return  return value requested by message
     */
    public Object doPerform( URI sender, URI message, List args )
        throws IOException
    {

        //  Return a description of this application.
        if ( MessageId.INFO_GETDESCRIPTION.equals( message ) ) {
            return Utilities.getFullDescription();
        }

        //  Return the URL of the SPLAT icon.
        else if ( MessageId.INFO_GETICONURL.equals( message ) ) {
            return "http://star-www.dur.ac.uk/~pdraper/splat/splat.gif";
        }

        //  Load a spectrum from a FITS 1-d array.
        else if ( MessageId.FITS_LOADLINE.equals( message ) &&
                  checkArgs( args, new Class[] { Object.class } ) ) {
            if ( acceptFITSLineModel.isSelected() ) {
                String location = args.get( 0 ).toString();
                boolean success =
                    browser.addSpectrum( location, SpecDataFactory.FITS );
                return Boolean.valueOf( success );
            }
            else {
                return Boolean.FALSE;
            }
        }

        //  Load a spectrum from a FITS table.
        else if ( MessageId.FITS_LOADIMAGE.equals( message ) &&
                  checkArgs( args, new Class[] { Object.class } ) ) {
            if ( acceptFITSTableModel.isSelected() ) {
                String location = args.get( 0 ).toString();
                boolean success =
                    browser.addSpectrum( location, SpecDataFactory.TABLE );
                return Boolean.valueOf( success );
            }
            else {
                return Boolean.FALSE;
            }
        }

        //  Load a spectrum from a VOTable.
        else if ( MessageId.VOT_LOADURL.equals( message ) &&
                  checkArgs( args, new Class[] { Object.class } ) ) {
            if ( acceptVOTableModel.isSelected() ) {
                String location = args.get( 0 ).toString();
                boolean success =
                    browser.addSpectrum( location, SpecDataFactory.TABLE );
                return Boolean.valueOf( success );
            }
            else {
                return Boolean.FALSE;
            }
        }

        //  Unknown message (the superclass shouldn't hand one of these down).
        else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Sets the button model which determines whether incoming messages
     * requesting load of a VOTable will be acted on or ignored.
     *
     * @param  model  button model
     */
    public void setAcceptVOTableModel( ButtonModel model )
    {
        acceptVOTableModel = model;
    }

    /**
     * Sets the button model which determines whether incoming messages
     * requesting load of a 1-d FITS file will be acted on or ignored.
     *
     * @param  model  button model
     */
    public void setAcceptFITSLineModel( ButtonModel model )
    {
        acceptFITSLineModel = model;
    }

    /**
     * Sets the button model which determines whether incoming messages
     * requesting load of a FITS table file will be acted on or ignored.
     *
     * @param  model  button model
     */
    public void setAcceptFITSTableModel( ButtonModel model )
    {
        acceptFITSTableModel = model;
    }

    /**
     * Returns the button model which determines whether incoming messages
     * requesting load of a VOTable will be acted on or ignored.
     *
     * @return  button model
     */
    public ButtonModel getAcceptVOTableModel()
    {
        return acceptVOTableModel;
    }

    /**
     * Returns the button model which determines whether incoming messages
     * requesting load of a 1-d FITS file will be acted on or ignored.
     *
     * @return  button model
     */
    public ButtonModel getAcceptFITSLineModel()
    {
        return acceptFITSLineModel;
    }

    /**
     * Returns the button model which determines whether incoming messages
     * requesting load of a FITS table will be acted on or ignored.
     *
     * @return  button model
     */
    public ButtonModel getAcceptFITSTableModel()
    {
        return acceptFITSTableModel;
    }

    /**
     * Load a spectrum from a URL with the given Map of properties.
     */
    protected void loadSpectrum( String location, Map meta )
    {
        SpectrumIO.Props[] propList = new SpectrumIO.Props[1];
        propList[0] = getProps( location, meta );
        browser.threadLoadSpectra( propList );
    }

    /**
     * Convert a spectrum specification (a URL and Map of SSAP metadata) into
     * a SpectrumIO.Prop instance.
     *
     * @param location URL of spectrum.
     * @param meta a key-value map of SSAP metadata that describes the
     *             spectrum to be accessed.
     */
    protected SpectrumIO.Props getProps( String location, Map meta )
    {
        SpectrumIO.Props props = new SpectrumIO.Props( location );
        if ( meta != null && meta.size() > 0 ) {
            Set keys = meta.keySet();
            Iterator i = keys.iterator();
            String key;
            String value;
            String axes[];
            String units[];
            while( i.hasNext() ) {
                key = (String) i.next();
                key = key.toLowerCase();
                value = (String) meta.get( key ); // ?is this always String?

                //  UTYPEs and UCDs, maybe UTYPES should be sdm:ssa.xxx.
                //  Many of the SSAP response UTYPEs don't seem documented yet.
                if ( key.equals( "vox:spectrum_format" ) ||
                     key.equals( "access.format" ) ) {
                    props.setType( specDataFactory.mimeToSPLATType( value ) );
                }
                else if ( key.equals( "vox:image_title" ) ||
                          key.equals( "target.name" ) ) {
                    props.setShortName( value );
                }
                //  Suspect these will be separate UTYPEs,
                //  data.spectralaxis.value and data.fluxaxis.value.
                else if ( key.equals( "vox:spectrum_axes" ) ) {
                    axes = value.split( "\\s" );
                    props.setCoordColumn( axes[0] );
                    props.setDataColumn( axes[1] );
                    if ( axes.length == 3 ) {
                        props.setErrorColumn( axes[2] );
                    }
                }
                //  Suspect these will be separate UTYPEs,
                //  data.spectralaxis.unit and data.fluxaxis.unit.
                else if ( key.equals( "vox:spectrum_units" ) ) {
                    units = value.split("\\s");
                    props.setCoordUnits( units[0] );
                    props.setDataUnits( units[1] );
                }
            }
        }
        return props;
    }
}
