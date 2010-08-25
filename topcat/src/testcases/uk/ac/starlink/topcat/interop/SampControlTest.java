package uk.ac.starlink.topcat.interop;

import java.awt.HeadlessException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.MessageHandler;
import uk.ac.starlink.table.gui.TableLoadChooser;
import uk.ac.starlink.table.gui.TableLoadDialog;
import uk.ac.starlink.vo.DalTableLoadDialog;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.Driver;

public class SampControlTest extends TestCase {

    private final ControlWindow controlWindow_;

    public SampControlTest() {
        ControlWindow cwin;
        try {
            cwin = ControlWindow.getInstance();
        }
        catch ( HeadlessException e ) {
            cwin = null;
        }
        controlWindow_ = cwin;
        Logger.getLogger( "org.astrogrid.samp" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.topcat" ).setLevel( Level.WARNING );
    }

    /**
     * Some of the handling when accepting resource lists relies on
     * identifying the classes of dialogues listed in the TableLoadChooser's
     * list.  This is a somewhat dangerous practice, since if the 
     * classes are changed (polymorphism) the code could stop working
     * properly.  The purpose of this test is to check that the classes
     * can still be identified - see ControlWindow.acceptResourceIdList.
     * To do an interactive runtime test, send the SAMP message(s)
     * "voresource.loadlist{,.cone,.siap,.ssap}".
     */
    public void testDialogs() throws IOException {
        if ( controlWindow_ == null ) {
            System.err.println( "headless: no SampControlTest" );
            return;
        }

        /* Get the list of resource accept message handlers. */
        HubConnector connector =
            new HubConnector( new DummyClientProfile() );
        TopcatSampControl samper =
            new TopcatSampControl( connector, controlWindow_ );
        MessageHandler[] handlers = samper.createMessageHandlers();
        List<TopcatSampControl.ResourceListHandler> rhlist =
            new ArrayList<TopcatSampControl.ResourceListHandler>();
        for ( int ih = 0; ih < handlers.length; ih++ ) {
            if ( handlers[ ih ] instanceof
                 TopcatSampControl.ResourceListHandler ) {
                rhlist.add( (TopcatSampControl.ResourceListHandler)
                            handlers[ ih ] );
            }
        }

        /* Get the list of table load dialogues and multiwindows as used
         * by ControlWindow for matching against the classes held in
         * resourcehandler instances. */
        TableLoadChooser chooser =
            new TableLoadChooser( controlWindow_.getTableFactory(),
                                  TableLoadChooser.makeDefaultLoadDialogs(),
                                  Driver.KNOWN_DIALOGS );
        TableLoadDialog[] tlds = chooser.getKnownDialogs();
        Collection<Class> tldClassList = new HashSet<Class>();
        for ( int i = 0; i < tlds.length; i++ ) {
            tldClassList.add( tlds[ i ].getClass() );
        }
        Collection<Class> multiClassList = new HashSet<Class>();
        multiClassList.add( controlWindow_.getConeMultiWindow().getClass() );
        multiClassList.add( controlWindow_.getSiaMultiWindow().getClass() );
        multiClassList.add( controlWindow_.getSsaMultiWindow().getClass() );

        /* For each resource handler, make sure it is matched by an
         * appropriate number of the known dialogue types. */
        for ( TopcatSampControl.ResourceListHandler rh : rhlist ) {
            Class ldClazz = rh.dalLoadDialogClass_;
            Class mwClazz = rh.dalMultiWindowClass_;
            boolean isGeneral = ldClazz == DalTableLoadDialog.class;
            int gotTld = 0;
            for ( Class ldc : tldClassList ) {
                if ( ldClazz.isAssignableFrom( ldc ) ) {
                    gotTld++;
                }
            }
            int gotMw = 0;
            for ( Class mwc : multiClassList ) {
                if ( mwClazz.isAssignableFrom( mwc ) ) {
                    gotMw++;
                }
            }
            if ( isGeneral ) {
                assertTrue( gotTld > 0 );
                assertTrue( gotMw > 0 );
            }
            else {
                assertEquals( 1, gotTld );
                assertEquals( 1, gotMw );
            }
        }
    }

    private static class DummyClientProfile implements ClientProfile {
        public boolean isHubRunning() {
            return false;
        }
        public HubConnection register() {
            return null;
        }
    }
}
