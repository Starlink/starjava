package uk.ac.starlink.topcat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import javax.swing.SwingUtilities;
import uk.ac.starlink.plastic.HubManager;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.DataSource;

/**
 * Implements the PlasticListener interface on behalf of the TOPCAT application.
 * Will attempt to unregister with the hub on finalization or JVM shutdown.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2006
 * @see      <a href="http://plastic.sourceforge.net/">PLASTIC</a>
 */
public class TopcatPlasticListener extends HubManager {

    private final ControlWindow controlWindow_;

    private static final URI VOT_LOAD;
    private static final URI VOT_LOADURL;
    private static final URI[] SUPPORTED_MESSAGES = new URI[] {
        VOT_LOAD =    createURI( "ivo://votech.org/votable/load" ),
        VOT_LOADURL = createURI( "ivo://votech.org/votable/loadFromURL" ),
    };

    /**
     * Constructs a new listener which will react appropriately to 
     * messages from the hub.
     *
     * @param   controlWindow   control window into which accepted tables
     *          will be loaded etc
     */
    public TopcatPlasticListener( ControlWindow controlWindow ) {
        super( "topcat", SUPPORTED_MESSAGES );
        controlWindow_ = controlWindow;
    }

    /**
     * Does the work for processing a hub message.
     *
     * @param  sender   sender ID
     * @param  message  message ID (determines the action required)
     * @param  args     message argument list
     * @return  return value requested by message
     */
    public Object doPerform( URI sender, URI message, List args )
            throws IOException {

        /* Load VOTable passed as text in an argument. */
        if ( VOT_LOAD.equals( message ) &&
                  checkArgs( args, new Class[] { String.class } ) ) {
            votableLoad( sender, (String) args.get( 0 ) );
            return null;
        }

        /* Load VOTable by URL. */
        else if ( VOT_LOADURL.equals( message ) &&
                  checkArgs( args, new Class[] { Object.class } ) ) {
            String url = args.get( 0 ) instanceof String
                       ? (String) args.get( 0 )
                       : args.get( 0 ).toString();
            votableLoadFromURL( sender, url );
            return null;
        }

        /* Unknown message. */
        else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Does the work for the load-from-string VOTable message.
     *
     * @param  sender  sender ID
     * @param  votText   VOTable text contained in a string, assumed UTF-8
     *                   encoded
     */
    private void votableLoad( URI sender, String votText ) throws IOException {
        final byte[] votBytes;
        try {
            votBytes = votText.getBytes( "UTF-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            throw (AssertionError)
                  new AssertionError( "JVMs are required to support UTF-8" )
                 .initCause( e );
        }
        votText = null;
        DataSource datsrc = new DataSource() {
            public InputStream getRawInputStream() {
                return new ByteArrayInputStream( votBytes );
            }
        };
        loadTable( controlWindow_.getTableFactory()
                  .makeStarTable( datsrc, "votable" ) );
    }

    /**
     * Does the work for the load-from-URL VOTable load message.
     *
     * @param   sender  sender ID
     * @param   url  location of table
     */
    private void votableLoadFromURL( URI sender, String url )
            throws IOException {
        loadTable( controlWindow_.getTableFactory()
                  .makeStarTable( url, "votable" ) );
    }

    /**
     * Loads a StarTable into TOPCAT.
     *
     * @param  table  table to load
     */
    private void loadTable( final StarTable table ) {

        /* Best do it asynchronously since this may not be called from the
         * event dispatch thread. */
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                controlWindow_.addTable( table, "plastic", true );
            }
        } );
    }
}
