/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     06-MAR-2009 (Mark Taylor):
 *        Original version.
 */
package uk.ac.starlink.splat.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JMenu;

import org.astrogrid.samp.Client;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.SampUtils;
import org.astrogrid.samp.Subscriptions;
import org.astrogrid.samp.gui.GuiHubConnector;
import org.astrogrid.samp.gui.IndividualCallActionManager;
import org.astrogrid.samp.gui.SubscribedClientListModel;
import org.astrogrid.samp.httpd.ServerResource;

import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.splat.vo.SSAQueryBrowser;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Provides GUI actions for sending tables by SAMP.
 *
 * @author Mark Taylor
 * @version $Id$
 */
public class TableSendActionManager
    extends IndividualCallActionManager
    implements Transmitter
{
    /** SSA window from which this object sends tables. */
    protected SSAQueryBrowser ssaBrowser;

    /** Supported table send formats. */
    private static final Sender[] SENDERS = new Sender[] {
        new Sender( "table.load.votable", new VOTableWriter(), ".vot" ),
        new Sender( "table.load.fits", new FitsTableWriter(), ".fits" ),
    };

    /**
     * Constructor.
     *
     * @param  ssaBrowser  SSA window from which to send tables
     * @param  hubConnector  handles SAMP hub communications
     */
    public TableSendActionManager( SSAQueryBrowser ssaBrowser,
                                   GuiHubConnector hubConnector )
    {
        super( ssaBrowser, hubConnector,
               new SubscribedClientListModel( hubConnector, getSendMtypes() ) );
        this.ssaBrowser = ssaBrowser;
    }

    /**
     * Get the table for transmission.
     */
    protected StarTable getTable()
    {
        return ssaBrowser.getCurrentTable();
    }

    /**
     * Returns a message which will send the currently selected table
     * to a given client.
     *
     * @param  client  recipient of message
     */
    protected Map createMessage( Client client )
        throws IOException
    {
        Sender sender = getSender( client );
        StarTable table = getTable();
        if ( sender != null && table != null ) {
            return sender.createMessage( table, "" );
        }
        else {
            return null;
        }
    }

    /**
     * Decorates default action with more specific annotations.
     */
    public Action createBroadcastAction()
    {
        Action action = super.createBroadcastAction();
        action.putValue( Action.NAME, "Broadcast Result Table" );
        action.putValue( Action.SHORT_DESCRIPTION,
                         "Transmit table result of SSA query to all "
                       + "applications using SAMP" );
        return action;
    }

    /**
     * Decorates default action with more specific annotations.
     */
    public Action getSendAction( Client client )
    {
        Action action = super.getSendAction( client );
        action.putValue( Action.SHORT_DESCRIPTION,
                         "Send table result of SSA query to "
                       + SampUtils.toString( client ) );
        return action;
    }

    /**
     * Decorates default menu with more specific annotation.
     */
    public JMenu createSendMenu()
    {
        JMenu menu = super.createSendMenu( "Send Result Table to ..." );
        menu.setToolTipText( "Send table result of SSA query"
                           + " to a single other application using SAMP" );
        return menu;
    }

    /**
     * Returns a Sender object which can send a table to a given client.
     *
     * @param  client  target client
     * @return   sender
     */
    private static Sender getSender( Client client )
    {
        Subscriptions subs = client.getSubscriptions();
        for ( int i = 0; i < SENDERS.length; i++ ) {
            Sender sender = SENDERS[ i ];
            if ( subs.isSubscribed( sender.getMtype() ) ) {
                return sender;
            }
        }
        return null;
    }

    /**
     * Returns the array of MTypes which this sender can use to send tables.
     *
     * @return  mtype list
     */
    private static String[] getSendMtypes()
    {
        String[] mtypes = new String[ SENDERS.length ];
        for ( int i = 0; i < SENDERS.length; i++ ) {
            mtypes[ i ] = SENDERS[ i ].getMtype();
        }
        return mtypes;
    }

    /**
     * Encapsulates format-specific details of how a table is sent over SAMP.
     */
    private static class Sender
    {
        /** MType of message to send. */
        private String mtype;

        /** Table writer which generates serialized table representation. */
        private StarTableWriter writer;

        /** Informative file extension used for filename. */
        private String extension;

        /**
         * Constructor.
         */
        Sender( String mtype, StarTableWriter writer, String extension )
        {
            this.mtype = mtype;
            this.writer = writer;
            this.extension = extension;
        }

        /**
         * Returns this sender's MType.
         */
        public String getMtype()
        {
            return mtype;
        }

        /**
         * Returns a message suitable for sending a table.
         *
         * @param   table  table to send
         * @param   informative label (uniqueness not essential)
         * @return  send message
         */
        public Message createMessage( StarTable table, String label )
            throws IOException
        {
           String name = "t" + label + extension;
           ServerResource resource = new TableResource( table, writer );
           URL turl =
               SplatHTTPServer.getInstance().addResource( name, resource );
           return new Message( getMtype() )
                 .addParam( "url", turl.toString() );
        }
    }

    /**
     * ServerResource implementation representing a table serialization.
     */
    private static class TableResource
        implements ServerResource
    {
        private StarTable table;
        private StarTableWriter writer;

        /**
         * Constructor.
         *
         * @param  table  table to serialize
         * @param  writer  table output format handler
         */
        TableResource( StarTable table, StarTableWriter writer )
        {
            this.table = table;
            this.writer = writer;
        }

        public long getContentLength()
        {
            return -1L;
        }

        public String getContentType()
        {
            return writer.getMimeType();
        }

        public void writeBody( OutputStream out )
            throws IOException
        {
            writer.writeStarTable( table, out );
        }
    }
}
