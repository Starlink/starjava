package uk.ac.starlink.table.gui;

import javax.swing.SwingUtilities;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class StarTableNodeChooserTest extends TestCase {

    Throwable storedThrowable;

    public StarTableNodeChooserTest( String name ) {
        super( name );
    }

    /**
     * This tests the availability of the StarTableNodeChooser class for use.
     */
    public void testChooserAvailability() throws Throwable {

        SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {
                try {
                    /* This works using reflection on the class 
                     * StarTableNodeChooser.CHOOSER_CLASS 
                     * (somewhere in Treeview, probably), 
                     * which may not be available when this package is built.
                     * Hence this test may fail if the whole starjava 
                     * set has not yet been installed. */
                    StarTableNodeChooser.reflect();

                    String msg = "Wrong signatures on " 
                               + StarTableNodeChooser.CHOOSER_CLASS;
                    assertTrue( msg, StarTableNodeChooser.isAvailable() );
                    assertNotNull( msg, StarTableNodeChooser.newInstance() );
                }
                catch ( Throwable e ) {
                    storedThrowable = e;
                }
            }
        } );

        if ( storedThrowable != null ) {
            throw storedThrowable;
        }
    }
}
