package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
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
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * SendActionManager which will transmit a table.
 * A selection of table formats (table.load.*) is available as required -
 * currently only votable and fits are offered, but it is a one-liner
 * to add other supported formats.
 *
 * @author   Mark Taylor
 * @since    4 Dec 2008
 */
public class TableSendActionManager extends IndividualCallActionManager
                                    implements Transmitter {

    private final TopcatSampControl sampControl_;
    private final ControlWindow controlWindow_;

    /** Supported table send formats. */
    private static final Sender[] SENDERS = new Sender[] {
        new Sender( "table.load.votable", new VOTableWriter(), ".vot" ),
        new Sender( "table.load.fits", new FitsTableWriter(), ".fits" ),
    };

    /**
     * Constructor.
     *
     * @param   connector  hub connector
     * @param   sampControl  TOPCAT SAMP control object
     */
    public TableSendActionManager( GuiHubConnector connector,
                                   TopcatSampControl sampControl ) {
        super( sampControl.getControlWindow(), connector,
               new SubscribedClientListModel( connector, getSendMtypes() ) );
        sampControl_ = sampControl;
        controlWindow_ = sampControl.getControlWindow();
    }

    protected Map createMessage( Client client ) throws IOException {
        Sender sender = getSender( client );
        TopcatModel tcModel = controlWindow_.getCurrentModel();
        if ( sender != null && tcModel != null ) {
            StarTable table = tcModel.getApparentStarTable();
            String ident = Integer.toString( tcModel.getID() );
            String name = tcModel.getLabel();
            String sampId = sampControl_.getTableId( tcModel );
            return sender.createMessage( table, ident, name, sampId );
        }
        else {
            return null;
        }
    }

    public Action createBroadcastAction() {
        Action action = super.createBroadcastAction();
        action.putValue( Action.NAME, "Broadcast table" );
        action.putValue( Action.SHORT_DESCRIPTION,
                         "Transmit table to all applications using SAMP" );
        action.putValue( Action.SMALL_ICON, ResourceIcon.BROADCAST );
        return action;
    }

    public Action getSendAction( Client client ) {
        Action action = super.getSendAction( client );
        action.putValue( Action.SHORT_DESCRIPTION,
                         "Send table to " + SampUtils.toString( client ) );
        return action;
    }

    public JMenu createSendMenu() {
        JMenu menu = super.createSendMenu( "Send table to..." );
        menu.setToolTipText( "Send table to a single other registered client"
                           + " using SAMP" );
        menu.setIcon( ResourceIcon.SEND );
        return menu;
    }

    /**
     * Returns a Sender object which can send a table to a given client.
     *
     * @param  client  target client
     * @return   sender
     */
    private static Sender getSender( Client client ) {
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
    private static String[] getSendMtypes() {
        String[] mtypes = new String[ SENDERS.length ];
        for ( int i = 0; i < SENDERS.length; i++ ) {
            mtypes[ i ] = SENDERS[ i ].getMtype();
        }
        return mtypes;
    }

    /**
     * Encapsulates format-specific details of how a table is sent over SAMP.
     */
    private static class Sender {
        private final String mtype_;
        private final StarTableWriter writer_;
        private final String extension_;

        /**
         * Constructor.
         *
         * @param   mtype   MType of table send message
         * @param   writer  serializer for table
         * @param   extension  suggested file extension (including dot) 
         *                     for table URL
         */
        Sender( String mtype, StarTableWriter writer, String extension ) {
            mtype_ = mtype;
            writer_ = writer;
            extension_ = extension;
        }

        /**
         * Returns the MType used by this sender.
         *
         * @return  MType
         */
        public String getMtype() {
            return mtype_;
        }

        /**
         * Returns a message suitable for sending a table.
         *
         * @param   table  table to send
         * @param   ident  informal table identifier used for constructing URL;
         *                 should not contain weird characters,
         *                 but uniqueness is not essential
         * @param   name   informal table name for human consumption;
         *                 free form text, uniqueness is not essential,
         *                 may be null
         * @param   sampId  table ID, unique to relevant parts of table state
         * @return  send message
         */
        public Message createMessage( StarTable table, String ident,
                                      String name, String sampId )
                throws IOException {
            String fname = "t" + ident + extension_;
            URL turl = TopcatServer.getInstance()
                      .addResource( fname, createResource( table ) );
            Message msg = new Message( getMtype() );
            msg.addParam( "url", turl.toString() );
            msg.addParam( "table-id", sampId );
            if ( name != null && name.trim().length() > 0 ) {
                msg.addParam( "name", name );
            }
            return msg;
        }

        /**
         * Obtains a (somewhat) persistent resource object via which
         * a table can be made available to external processes.
         *
         * @param   table  table
         * @return   servable resource
         */
        private ServerResource createResource( final StarTable table ) {
            return new ServerResource() {
                public long getContentLength() {
                    return -1L;
                }
                public String getContentType() {
                    return writer_.getMimeType();
                }
                public void writeBody( OutputStream out ) throws IOException {
                    writer_.writeStarTable( table, out );
                }
            };
        }

        public String toString() {
            return mtype_;
        }
    }
}
