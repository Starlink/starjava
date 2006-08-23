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
import java.util.List;
import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.plastic.MessageId;

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
        MessageId.VOT_LOADURL,
        MessageId.INFO_GETDESCRIPTION,
        MessageId.INFO_GETICONURL,
    };

    /**
     * Controlling browser object.
     */
    protected SplatBrowser browser;

    /**
     * Model controlling whether spectra received as VOTables are accepted.
     */
    protected ButtonModel acceptVOTableModel = new DefaultButtonModel();

    /**
     * Model controlling whether spectra received as FITS are accepted.
     */
    protected ButtonModel acceptFITSModel = new DefaultButtonModel();

    /**
     * Create a new plastic listener for SPLAT.
     */
    public SplatPlastic( SplatBrowser browser )
    {
        super( "splat-vo", SUPPORTED_MESSAGES );
        this.browser = browser;
        acceptVOTableModel.setSelected( true );
        acceptFITSModel.setSelected( true );
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

        //  Load a spectrum from a FITS URL.
        else if ( MessageId.FITS_LOADLINE.equals( message ) &&
                  checkArgs( args, new Class[] { Object.class } ) ) {
            if ( acceptFITSModel.isSelected() ) {
                String location = args.get( 0 ).toString();
                boolean success =
                    browser.addSpectrum( location, SpecDataFactory.FITS ); 
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
    public void setAcceptFITSModel( ButtonModel model )
    {
        acceptFITSModel = model;
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
    public ButtonModel getAcceptFITSModel()
    {
        return acceptFITSModel;
    }
}
