package uk.ac.starlink.topcat;

import java.io.FileNotFoundException;
import junit.framework.TestCase;

public class ResourceTest extends TestCase {

    public ResourceTest( String name ) {
        super( name );
    }

    public void testResourceIcon() throws FileNotFoundException {
        ResourceIcon.checkResourcesPresent();
    }
}
