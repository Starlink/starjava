/*
 * Copyright (C) 2007 Science and Technology Facilities Council.
 *
 *  History:
 *     02-APR-2007 (Peter W. Draper):
 *       Original version, based on SpecTransmitter by Mark Taylor.
 */
package uk.ac.starlink.splat.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.votech.plastic.PlasticHubListener;
import uk.ac.starlink.plastic.ApplicationItem;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.plastic.MessageId;
import uk.ac.starlink.plastic.PlasticTransmitter;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.splat.vo.SSAQueryBrowser;
import uk.ac.starlink.votable.VOTableWriter;


/**
 * Transmit a {@link StarTable} as a VOTable over PLASTIC.  
 *
 * @author   Peter W. Draper
 * @version  $Id$
 */
public class StarTableTransmitter
    extends PlasticTransmitter
{
    /**
     * PLASTIC message identifier that this transmitter deals with.
     */
    private final static URI msgId = MessageId.VOT_LOADURL;
    
    /** The SSAQueryBrowser containing the tables to transmit
     *  XXX use a general interface XXX */
    private SSAQueryBrowser browser = null;

    /**
     * Constructor.
     *
     * @param   hubman   object controlling connection to a PLASTIC hub
     * @param   browser  query browser from which table is selected
     */
    public StarTableTransmitter( HubManager hubman, 
                                 SSAQueryBrowser browser )
    {
        super( hubman, msgId, "SSAP query result" );
        this.browser = browser;
    }

    /**
     * Get the table for transmission.
     */
    protected StarTable getTable()
    {
        return browser.getCurrentTable();
    }

    protected void transmit( final PlasticHubListener hub,
                             final URI clientId,
                             final ApplicationItem app )
        throws IOException
    {
        StarTable table = getTable();
        if ( table == null ) {
            throw new IOException( "No table available for tranmission" );
        }
        
        //  Set up a URL for a temporary file which we will use
        //  for the transmission.
        final String url;
        final File tmpFile = File.createTempFile( "SPLAT", ".xml" );

        try {
            //  Save table to disk.
            VOTableWriter writer = new VOTableWriter();
            FileOutputStream fs = new FileOutputStream( tmpFile );
            writer.writeStarTable( table, fs, null);
            tmpFile.deleteOnExit();
        }
        catch ( IOException e ) {
            throw e;
        }
        catch ( Throwable e ) {
            throw (IOException) new IOException().initCause( e );
        }
        url = URLUtils.makeFileURL( tmpFile ).toString();
       
        //  Send the message to the hub.
        //  This is done in a separate thread so as not to block the UI.
        new Thread( "PLASTIC table transmitter" ) 
        {
            public void run() 
            {
                List argList = Arrays.asList( new Object[] { url, url } );
                Map res;
                if ( app == null ) {
                    res = hub.request( clientId, msgId, argList );
                }
                else {
                    res = hub.requestToSubset( clientId, msgId, argList, 
                                 Collections.singletonList( app.getId() ) );
                }
                if ( tmpFile != null ) {
                    tmpFile.delete();
                }
            }
        }.start();
    }
}
