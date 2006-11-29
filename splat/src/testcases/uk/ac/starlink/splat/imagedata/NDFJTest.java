package uk.ac.starlink.splat.imagedata;

import java.io.IOException;

import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.Loader;

public class NDFJTest 
    extends TestCase 
{
    public NDFJTest( String name ) 
    {
        super( name );
    }

    protected void setUp() 
        throws IOException 
    {
        // Replicate the loader section from NDFJ.java so that we can diagnose
        // any issues with loading the shareable library. Normally the
        // exceptions are handled and JNIHDS is used instead.
        try {
            Loader.loadLibrary( "splat" );
        }
        catch (SecurityException se) {
            se.printStackTrace();
            assertTrue( "SecurityException", false );
        }
        catch (UnsatisfiedLinkError ue) {
            ue.printStackTrace();
            assertTrue( "UnsatisfiedLinkError", false );
        }
        catch (Exception ge) {
            ge.printStackTrace();
            assertTrue( "Unexpected loader error", false );
        }
    }

    public void testLoading() 
    {
        NDFJ ndfJ = new NDFJ();
    }
}
