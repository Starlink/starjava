/*
 * Copyright (C) 2006 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     18-AUG-2006 (Mark Taylor):
 *       Original version.
 *     14-JUL-200(Peter Draper):
 *       Give up on 1D FITS and always transmit FITS tables.
 */
package uk.ac.starlink.splat.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
 * <p>The implementation of this class is currently somewhat complicated
 * because it provides options for transmitting spectra using
 * a number of different PLASTIC messages.  If it is decided that only
 * one of these is required, it would be possible to simplify the
 * implementation a bit.
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
    protected URI msgId;

    /**
     * Constructor.
     *
     * @param   hubman   object controlling connection to a PLASTIC hub
     * @param   specList global list of spectra; the current selection
     *          determines what spectrum is transmitted (transmission will
     *          only be enabled if there is a unique selection)
     * @param   msgId  PLASTIC identifier for the message that this
     *          transmitter deals with
     * @param   sendType  short string representing the type of object
     *          which is transmitted
     */
    protected SpecTransmitter( HubManager hubman, JList specList, URI msgId,
                               String sendType )
    {
        super( hubman, msgId, sendType );
        this.specList = specList;
        this.msgId = msgId;
        specList.addListSelectionListener( this );
        updateState();
    }

    /**
     * Implements message transmission.
     */
    protected void transmit( PlasticHubListener hub, URI clientId,
                             ApplicationItem app )
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

        //  Transmit it.
        transmitSpectrum( hub, clientId, app, spec );
    }

    /**
     * Given a spectrum to transmit, transmits it in the appropriate way
     * for this Transmitter.
     *
     * @param  hub  hub
     * @param  clientId  registered ID for this application
     * @param  app    target for the transmssion; if null broadcast to all
     * @param  spec  the spectrum to transmit
     */
    protected abstract void transmitSpectrum( PlasticHubListener hub,
                                              URI clientId, ApplicationItem app,
                                              SpecData spec )
        throws IOException;

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
     * using the format-neutral PLASTIC spectrum transmission message.
     *
     * @param   hubman   object controlling connection to a PLASTIC hub
     * @param   specList global list of spectra; the current selection
     *          determines what spectrum is transmitted (transmission will
     *          only be enabled if there is a unique selection)
     * @return  new transmitter
     */
    public static SpecTransmitter createSpectrumTransmitter( HubManager hubman,
                                                             JList specList )
    {
        return new SpectrumSpecTransmitter( hubman, specList );
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

    /**
     * SpecTransmitter implementation which sends spectra using the
     * format-neutral PLASTIC spectrum transmission message.
     */
    private static class SpectrumSpecTransmitter
        extends SpecTransmitter
    {
        SpectrumSpecTransmitter( HubManager hubman, JList specList )
        {
            super( hubman, specList, MessageId.SPECTRUM_LOADURL, "spectrum" );
        }

        protected void transmitSpectrum( final PlasticHubListener hub,
                                         final URI clientId,
                                         final ApplicationItem app,
                                         SpecData spec )
            throws IOException
        {
            String fmt = spec.getDataFormat();
            String mime = null;
            URL locUrl = null;
            File tmpFile = null;

            //  See if we already have a VOTable spectrum ready to send.
            if ( "VOTable".equals( fmt ) ) {
                tmpFile = new File( spec.getFullName() );
                if ( tmpFile.exists() ) {
                    mime = "application/x-votable+xml";
                    locUrl = getUrl( spec.getFullName() );
                }
                tmpFile = null;
            }

            //  Otherwise, write it as a FITS table and use that. Note
            //  we cannot find out if a FITS table already exists as 
            //  StarTables are anonymous. Use "fits-basic" as SPLAT
            //  gets distracted by the primary array.
            if ( locUrl == null ) {
                tmpFile = File.createTempFile( "spec", ".fits" );
                tmpFile.deleteOnExit();
                locUrl = URLUtils.makeFileURL( tmpFile );
                mime = "application/fits";
                try {
                    spec = SpecDataFactory.getInstance()
                        .getTableClone( spec, tmpFile.toString(), 
                                        "fits-basic" );
                    spec.save();
                    assert tmpFile.exists() : tmpFile;
                }
                catch ( Throwable e ) {
                    throw (IOException) new IOException( e.getMessage() )
                                       .initCause( e );
                }
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
                if ( coordUnits.equals( "" ) ) {
                    meta.put( "vox:spectrum_units", 
                              coordUnits + " " + dataUnits );
                }
            }

            //  Columns. Note may not contain white space.
            String xColName = spec.getXDataColumnName();
            String yColName = spec.getYDataColumnName();
            if ( xColName != null && yColName != null ) {
                meta.put( "vox:spectrum_axes", xColName + " " + yColName );
            }

            //  Prepare message argument list.
            final List argList = new ArrayList();
            argList.add( locUrl.toString() );
            argList.add( locUrl.toString() );
            argList.add( meta );

            //  Send the message to the hub.
            final File tmpFile0 = tmpFile;
            new Thread( "PLASTIC spectrum transmitter" ) {
                public void run() {
                    Map responses = app == null
                        ? hub.request( clientId, msgId, argList )
                        : hub.requestToSubset( clientId, msgId, argList,
                                               Collections
                                              .singletonList( app.getId() ) );
                    if ( tmpFile0 != null ) {
                        tmpFile0.delete();
                    }
                }
            }.start();
        }
    }

    /**
     * SpecTransmitter implementation which sends spectra using a message
     * that requires a particular serialization type.
     */
    private abstract static class TypedSpecTransmitter
        extends SpecTransmitter
    {

        /**
         * Constructor.
         *
         * @param   hubman   object controlling connection to a PLASTIC hub
         * @param   specList global list of spectra; the current selection
         *          determines what spectrum is transmitted (transmission will
         *          only be enabled if there is a unique selection)
         * @param   msgId  PLASTIC identifier for the message that this
         *          transmitter deals with
         * @param   formatName   human-readable name for the spectrum
         *          transmission format (used in action names etc)
         */
        TypedSpecTransmitter( HubManager hubman, JList specList, URI msgId,
                              String formatName )
        {
            super( hubman, specList, msgId, "spectrum as " + formatName );
        }

        /**
         * Returns the location (URL or filename) of existing storage for
         * a given spectrum.  This must reference the data in a format
         * appropriate for the data type used by this transmitter.
         * If no such correctly-typed storage exists, null is returned.
         *
         * @param  spec   spectrum data
         * @return  file or URL where <code>spec</code>'s typed data
         *          can be found
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

        protected void transmitSpectrum( final PlasticHubListener hub,
                                         final URI clientId,
                                         final ApplicationItem app,
                                         SpecData spec )
            throws IOException
        {

            //  Set up a URL and (possibly) a temporary file which we will use
            //  for the transmission.
            final String url;
            final File tmpFile;

            //  See if there is a suitable ready-made URL for transmission.
            URL locUrl = getUrl( getTypedLocation( spec ) );
            if ( locUrl != null ) {
                url = locUrl.toString();
                tmpFile = null;
            }

            //  Otherwise write to a temporary file and the the URL of that.
            else {
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
    }
}
