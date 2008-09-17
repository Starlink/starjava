package uk.ac.starlink.topcat.interop;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.gui.DefaultSendActionManager;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * SendActionManager which will transmit a table.
 * The table sent is the currently selected one in the control window.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2008
 */
public class TableSendActionManager extends DefaultSendActionManager {

    private final TopcatSampConnector connector_;
    private final ControlWindow control_;
    private final TableResourceFactory votResourceFactory_;

    /**
     * Constructor.
     *
     * @param   connector   hub connector
     */
    public TableSendActionManager( TopcatSampConnector connector )
            throws IOException {
        super( connector.getControlWindow(), connector, "table.load.votable",
               "VOTable" );
        connector_ = connector;
        control_ = connector.getControlWindow();
        StarTableOutput sto = control_.getTableOutput();
        votResourceFactory_ =
            new TableResourceFactory( TopcatServer.getInstance(),
                                      sto.getHandler( "votable" ), ".xml" );
    }

    protected Map createMessage() throws IOException {
        TopcatModel tcModel = control_.getCurrentModel();
        if ( tcModel != null ) {
            URL turl = votResourceFactory_.addResource( tcModel );
            String tid = connector_.getTableId( tcModel );
            return new Message( "table.load.votable" )
                  .addParam( "url", turl.toString() )
                  .addParam( "table-id", tid );
        }
        else {
            throw new IllegalStateException( "No table selected" );
        }
    }

    /**
     * Arranges to make tables available by URL.
     */
    private static class TableResourceFactory {
        private final TopcatServer server_;
        private final StarTableWriter writer_;
        private final String extension_;

        /**
         * Constructor.
         *
         * @param   server   HTTP server object capable of hosting resources
         * @param   writer   StarTable output handler
         * @param   extension  file extension used for cosmetic purposes
         */
        TableResourceFactory( TopcatServer server, StarTableWriter writer,
                              String extension ) {
            server_ = server;
            writer_ = writer;
            extension_ = extension;
        }

        /**
         * Makes a table available to external processes via a URL.
         *
         * @param  tcModel   table to publicise
         * @return  URL location of table resource
         */
        public URL addResource( TopcatModel tcModel ) {
            StarTable table = tcModel.getApparentStarTable();
            String name = "t" + tcModel.getID() + extension_;
            return server_.addResource( name, createResource( table ) );
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
    }
}
