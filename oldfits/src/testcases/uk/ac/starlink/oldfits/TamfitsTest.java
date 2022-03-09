package uk.ac.starlink.oldfits;

import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.URLDataSource;

/**
 * These tests test bugfixes made to the starjava fork
 * of nom.tam.fits.  If they fail on other nom.tam.fits
 * versions, those versions should be fixed.
 */
public class TamfitsTest extends TestCase {

    public void testString() throws Exception {

        String name = "DUMMY";
        String txt = repeat( "1234567890", 8 );
        String comm = null;
        for ( int i = 0; i < 69; i++ ) {
            HeaderCard card =
                new HeaderCard( name, txt.substring( 0, i ), comm );
            checkStringCard( card );
        }
        assertEquals( '\'', new HeaderCard( name, txt.substring( 0, 68 ), comm )
                           .toString().charAt( 79 ) );
        try {
            new HeaderCard( name, txt.substring( 0, 69 ), comm );
            fail();
        }
        catch ( HeaderCardException e ) {
            // ok
        }

        {
            String qtxt = repeat( "C'thalpa ", 6 ) + "12345678";
            HeaderCard qcard = new HeaderCard( name, qtxt, comm );
            checkStringCard( qcard );
            assertEquals( 80, qcard.toString().length() );
            try {
                new HeaderCard( name, qtxt + "X", comm );
                fail();
            }
            catch ( HeaderCardException e ) {
                // ok
            }
        }

        String atxt = "Cy\u00e4egha\u2207";
        {
            HeaderCard acard = new HeaderCard( name, atxt, comm );
            checkStringCard( acard );
        }

        {
            String ltxt = repeat( "1234567890", 6 ) + atxt;
            HeaderCard lcard = new HeaderCard( name, ltxt, comm );
            checkStringCard( lcard );
            assertEquals( 80, lcard.toString().length() );
        }
    }


    private void checkStringCard( HeaderCard card ) {
        String image = card.toString();
        assertEquals( 80, image.length() );
        assertEquals( '=', image.charAt( 8 ) );
        assertEquals( ' ', image.charAt( 9 ) );
        assertEquals( '\'', image.charAt( 10 ) );
        assertEquals( '\'', image.charAt( image.trim().length() - 1 ) );
        for ( int ic = 0; ic < 80; ic++ ) {
            char c = image.charAt( ic );
            assertTrue( "Disallowed character: " + c + " in " + image,
                        c >= 0x20 && c <= 0x7e );
        }
    }

    public void testStringValues() throws Exception {
        checkStringValue( "electron'.s**-1" );
        checkStringValue( "x'electron'.s**-1" );

        // This one fails for nom.tam.fits 1.15.2;
        // the HeaderCard constructor checks for a quote at the start of
        // the string and assumes that you have pre-quoted the value,
        // tries to strip quotes from start and end, and fails if
        // it can't.  That is horrible behaviour.
        // See https://github.com/nom-tam-fits/nom-tam-fits/issues/148
        checkStringValue( "'electron'.s**-1" );
    }

    private void checkStringValue( String sval ) throws Exception {
        HeaderCard card = new HeaderCard( "CARD", sval, "comment" );
        assertEquals( sval, card.getValue() );
    }

    private static String repeat( String txt, int count ) {
        StringBuffer sbuf = new StringBuffer( txt.length() * count );
        for ( int i = 0; i < count; i++ ) {
            sbuf.append( txt );
        }
        return sbuf.toString();
    }
}
