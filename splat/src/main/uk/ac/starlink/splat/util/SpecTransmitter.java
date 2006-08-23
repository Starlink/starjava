/*
 * Copyright (C) 2006 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-AUG-2006 (Mark Taylor):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.ApplicationItem;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.plastic.PlasticTransmitter;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.GlobalSpecPlotList;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.util.URLUtils;

/**
 * Transmits spectra over PLASTIC.  This abstract class provides the 
 * common functionality for performing transmissions using different
 * data formats.  Static factory methods are provided to construct
 * instances for particular formats.
 *
 * @author   Mark Taylor
 * @version  $Id$
 */
public abstract class SpecTransmitter
    extends PlasticTransmitter
    implements ListSelectionListener
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
     * PLASTIC message identifier that this transmitter deals with.
     */
    private URI msgId;

    /**
     * Constructor.
     *
     * @param   hubman   object controlling connection to a PLASTIC hub
     * @param   specList global list of spectra; the current selection 
     *          determines what spectrum is transmitted (transmission will
     *          only be enabled if there is a unique selection)
     * @param   formatName   human-readable name for the spectrum transmission
     *          format (used in action names etc)
     * @param   msgId  PLASTIC identifier for the message that this 
     *          transmitter deals with
     */
    protected SpecTransmitter( HubManager hubman, JList specList, 
                               String formatName, URI msgId )
    {
        super( hubman, msgId, "spectrum as " + formatName );
        this.specList = specList;
        this.msgId = msgId;
        specList.addListSelectionListener( this );
        updateState();
    }

    /**
     * Returns the location (URL or filename) of existing storage for
     * a given spectrum.  This must reference the data in a format 
     * appropriate for the data type used by this transmitter.
     * If no such correctly-typed storage exists, null is returned.
     *
     * @param  spec   spectrum data
     * @return  file or URL where <code>spec</code>'s typed data can be found
     */
    protected abstract String getTypedLocation( SpecData spec );

    /**
     * Creates and returns a new SpecData object with data obtained from
     * an existing one which can be serialized using its <code>save</code>
     * method into the data type used by this transmitter.
     *
     * @param  spec   spectrum data
     * @return   typed clone of <code>spec</code>
     */
    protected abstract SpecData createTypedClone( SpecData spec )
        throws SplatException, IOException;

    /**
     * Implements actual transmission of the message.
     */
    protected void transmit( final PlasticHubListener hub,
                             final URI clientId,
                             final ApplicationItem app )
        throws IOException
    {

        //  Check we have a uniquely selected spectrum.  This error shouldn't
        //  happen in normal use because actions which call this method 
        //  should be enabled only if there is a single unique selection.
        if ( selectedIndex < 0 ) {
            throw new IllegalStateException( "No unique spectrum selected" );
        }

        //  Get the SpecData to be transmitted.
        SpecData spec = GlobalSpecPlotList.getInstance()
                                          .getSpectrum( selectedIndex );

        //  See if there is a suitable ready-made URL for transmission.
        String location = getTypedLocation( spec );
        URL locUrl = null;
        File locFile = null;
        if ( location != null ) {
            locFile = new File( location );
            if ( locFile.exists() ) {
                locUrl = URLUtils.makeFileURL( new File( location ) );
            }
            else {
                try {
                    locUrl = new URL( location );
                }
                catch ( MalformedURLException e ) {
                    locUrl = null;
                }
            }
        }

        //  Set up a URL and (possibly) a temporary file which we will use
        //  for the transmission.
        final String url;
        final File tmpFile;
        if ( locUrl != null ) {

            //  Use a URL pointing to existing data if available.
            url = locUrl.toString();
            tmpFile = null;
        }
        else {

            //  Otherwise write to a temporary file and the the URL of that.
            File tf;
            try {
                SpecData target = createTypedClone( spec );
                tf = new File( target.getFullName() );
                tf.deleteOnExit();
                target.save();
                assert tf.exists() : tf;
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( Throwable e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
            tmpFile = tf;
            url = URLUtils.makeFileURL( tmpFile ).toString();
        }
       
        //  Send the message to the hub.
        //  This is done in a separate thread so as not to block the GUI.
        new Thread( "PLASTIC spectrum transmitter" ) {
            public void run() {
                List argList = Arrays.asList( new Object[] { url, url } );
                Map responses = app == null
                    ? hub.request( clientId, msgId, argList )
                    : hub.requestToSubset( clientId, msgId, argList,
                                           Collections
                                          .singletonList( app.getId() ) );
                if ( tmpFile != null ) {
                    tmpFile.delete();
                }
            }
        }.start();
    }

    /**
     * Implement ListSelectionListener interface to ensure that this object
     * keeps track of the current selection state in the global spectrum list.
     */
    public void valueChanged( ListSelectionEvent e ) {
        updateState();
    }

    /**
     * Invoked when the selection state of the global spectrum list
     * may have changed.
     */
    private void updateState() {
        int[] indices = specList.getSelectedIndices();
        selectedIndex = ( indices == null || indices.length != 1 )
                      ? -1
                      : indices[ 0 ];
        setEnabled( selectedIndex >= 0 );
    }

    /**
     * Constructs and returns a SpecTransmitter which transmits spectra 
     * as 1-d FITS files.
     *
     * @param   hubman   object controlling connection to a PLASTIC hub
     * @param   specList global list of spectra; the current selection 
     *          determines what spectrum is transmitted (transmission will
     *          only be enabled if there is a unique selection)
     * @return  new transmitter using FITS
     */
    public static SpecTransmitter createFitsTransmitter( HubManager hubman,
                                                         JList specList )
    {
        return new SpecTransmitter( hubman, specList, "FITS",
                                    MessageId.FITS_LOADLINE ) {

            protected String getTypedLocation( SpecData spec ) 
            {
                return "FITS".equals( spec.getDataFormat() )
                     ? spec.getFullName()
                     : null;
            }

            protected SpecData createTypedClone( SpecData spec )
                throws SplatException, IOException
            {
                String tmploc = File.createTempFile( "spec", ".fits" )
                                    .toString();
                return SpecDataFactory.getInstance()
                      .getClone( spec, tmploc, SpecDataFactory.FITS, "" );
            }
        };
    }

    /**
     * Constructs and returns a SpecTransmitter which transmits spectra
     * as VOTables.
     *
     * @param   hubman   object controlling connection to a PLASTIC hub
     * @param   specList global list of spectra; the current selection 
     *          determines what spectrum is transmitted (transmission will
     *          only be enabled if there is a unique selection)
     * @return  new transmitter for using VOTable
     */
    public static SpecTransmitter createVOTableTransmitter( HubManager hubman,
                                                            JList specList )
    {
        return new SpecTransmitter( hubman, specList, "VOTable",
                                    MessageId.VOT_LOADURL ) {

            protected String getTypedLocation( SpecData spec )
            {
                //  Could tell if it was a table, but can't tell if it's
                //  a VOTable or not, so just return null.
                return null;
            }

            protected SpecData createTypedClone( SpecData spec )
                throws SplatException, IOException
            {
                String tmploc = File.createTempFile( "spec", ".xml" )
                                    .toString();
                return SpecDataFactory.getInstance()
                      .getClone( spec, tmploc, SpecDataFactory.TABLE,
                                 "votable" );
            }
        };
    }
}
