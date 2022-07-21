package uk.ac.starlink.topcat.interop;

import java.awt.HeadlessException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import junit.framework.TestCase;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.client.HubConnection;
import org.astrogrid.samp.client.HubConnector;
import org.astrogrid.samp.client.MessageHandler;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadDialog;
import uk.ac.starlink.vo.DalLoader;
import uk.ac.starlink.vo.TapTableLoadDialog;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.LoadWindow;
import uk.ac.starlink.topcat.join.DalMultiWindow;
import uk.ac.starlink.util.LogUtils;

public class SampControlTest extends TestCase {

    private final ControlWindow controlWindow_;

    public SampControlTest() {
        LogUtils.getLogger( "org.astrogrid.samp" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.topcat" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.vo" ).setLevel( Level.WARNING );
        ControlWindow cwin;
        try {
            cwin = ControlWindow.getInstance( false );
        }
        catch ( HeadlessException e ) {
            cwin = null;
        }
        controlWindow_ = cwin;
    }

    /**
     * Some of the handling when accepting resource lists relies on
     * identifying the classes of dialogues listed in the TableLoadChooser's
     * list.  This is a somewhat dangerous practice, since if the 
     * classes are changed (polymorphism) the code could stop working
     * properly.  The purpose of this test is to check that the classes
     * can still be identified.
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
        TableLoadDialog[] tlds =
            new LoadWindow( null, new StarTableFactory( true ) )
           .getKnownDialogs();
        DalMultiWindow[] mws = new DalMultiWindow[] {
            controlWindow_.getConeMultiWindow(),
            controlWindow_.getSiaMultiWindow(),
            controlWindow_.getSsaMultiWindow(),
        };

        /* For each resource handler, make sure it is matched by an
         * appropriate number of the known dialogue types. */
        for ( TopcatSampControl.ResourceListHandler rh : rhlist ) {
            Class ldClazz = rh.dalLoaderClass_;
            Class mwClazz = rh.dalMultiWindowClass_;
            boolean isGeneral = ldClazz == DalLoader.class;
            boolean isTap = ldClazz == TapTableLoadDialog.class;
            int gotTld = 0;
            for ( int id = 0; id < tlds.length; id++ ) {
                if ( controlWindow_.loadDialogMatches( tlds[ id ], ldClazz ) ) {
                    gotTld++;
                }
            }
            int gotMw = 0;
            for ( int im = 0; im < mws.length; im++ ) {
                if ( controlWindow_.multiWindowMatches( mws[ im ], mwClazz ) ) {
                    gotMw++;
                }
            }
            if ( isGeneral ) {
                assertTrue( gotTld > 0 );
                assertTrue( gotMw > 0 );
            }
            else if ( isTap ) {
                assertEquals( 1, gotTld );
                assertEquals( 0, gotMw );
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
