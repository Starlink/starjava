package uk.ac.starlink.table;

import junit.framework.TestCase;

public class StarTableChooserTest extends TestCase {

    public StarTableChooserTest( String name ) {
        super( name );
    }

    /**
     * This tests the availability of the StarTableChooser class for use.
     */
    public void testChooserAvailability() throws Throwable {

       /* This works using reflection on the class 
        * StarTableChooser.CHOOSER_CLASS (somewhere in Treeview, probably), 
        * which may not be available when this package is built.  Hence this 
        * test may fail if the whole starjava set has not yet been installed. */
        StarTableChooser.reflect();

        String msg = "Wrong signatures on " + StarTableChooser.CHOOSER_CLASS;
        assertTrue( msg, StarTableChooser.isAvailable() );
        assertNotNull( msg, StarTableChooser.newInstance() );
    }
}
