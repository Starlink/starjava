package uk.ac.starlink.ast;

import junit.framework.TestCase;

public class FitsChanTest extends TestCase {

    public FitsChanTest( String name ) {
        super( name );
    }

    public void testSetFits() {
        FitsChan fc = new FitsChan();
        fc.setFits( "CFVAL", Math.PI, Math.E, "Complex", true );
        fc.setFits( "CIVAL", 1234, 5678, null, false );
        fc.setFits( "IVAL", 23, null, true );
        fc.setFits( "FVAL", 0.25, "more commentary, but this time it's far "
                                + "too long to fit into a FITS header card",
                    false );
        fc.setFits( "LVAL", true, "Fishfinger", true );
        fc.setFits( "SVAL", "Captain Starlink", "R.I.P.", false );
        fc.setFitsContinue( "CNVAL", "Muon", null, true );

        fc.setCard( 0 );
        assertContains( fc.findFits( "CFVAL", false ), "3.1415" );
        assertContains( fc.findFits( "CFVAL", false ), "2.7182" );
        assertContains( fc.findFits( "CIVAL", false ), "1234" );
        assertContains( fc.findFits( "CIVAL", false ), "5678" );
        assertContains( fc.findFits( "IVAL", false ), "23" );
        assertContains( fc.findFits( "FVAL", false ), "0.25" );
        assertContains( fc.findFits( "LVAL", false ), "Fishfinger" );
        assertContains( fc.findFits( "SVAL", false ), "Starlink" );
        assertContains( fc.findFits( "CNVAL", false ), "Muon" );

        try {
            fc.setFits( null, false, "dummy", true );
            fail();
        }
        catch ( NullPointerException e ) {
        }
        try {
            fc.setFits( null, 0, 0, "dummy", false );
            fail();
        }
        catch ( NullPointerException e ) {
        }
        try {
            fc.setFits( null, "dummy", "dummy", false );
            fail();
        }
        catch ( NullPointerException e ) {
        }
    }

    private void assertContains( String val, String substr ) {
        if ( val.indexOf( substr ) < 0 ) {
            fail( "String \"" + val.trim() + "\" does not contain \"" 
                              + substr + "\"" );
        }
    }
}
