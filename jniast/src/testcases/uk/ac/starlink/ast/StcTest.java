package uk.ac.starlink.ast;

import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.util.TestCase;

public class StcTest extends TestCase {

    public StcTest( String name ) {
        super( name );
    }

    public void testConstructors() {
        Region region = box( -100, -100, 100, 100 );
        AstroCoords ac1 = new AstroCoords( new String[] { "X", "Y" } );
        ac1.setValue( box( 1, 1, 5, 10 ) );
        AstroCoords ac2 = new AstroCoords( new String[] { "RA", "DEC" } );
        ac2.setError( box( -1, -1, 1, 1 ) );
        AstroCoords[] acs = new AstroCoords[] { ac1, ac2 };

        checkStc( new StcCatalogEntryLocation( region, acs ), region, acs );
        checkStc( new StcObsDataLocation( region, acs ), region, acs );
        checkStc( new StcResourceProfile( region, acs ), region, acs );
        checkStc( new StcSearchLocation( region, acs ), region, acs );
    }

    private void checkStc( Stc stc, Region region, AstroCoords[] acs ) {
        assertSameShape( region, stc.getStcRegion() );
        assertEquals( acs.length, stc.getStcNCoord() );
        for ( int i = 0; i < acs.length; i++ ) {
            AstroCoords ac = stc.getStcCoord( i + 1 );
            if ( acs[ i ].getName() == null ) {
                assertNull( ac.getName() );
            }
            else {
                assertArrayEquals( acs[ i ].getName(), ac.getName() );
            }
        }
    }

    public void testReads() throws IOException {
        /* Test with example STC files acquired from
         * http://hea-www.harvard.edu/~arots/nvometa/ */
        Stc chandra = (StcResourceProfile) readStc( "ChandraSTCResource.xml" );
        Stc kpno = (StcObsDataLocation) readStc( "M81KPNO.xml" );

        /* For one reason and another, AST doesn't yet support the features
         * in the following XML files. */
        // Stc cat = (StcCatalogEntryLocation) readStc( "GalCatalog.xml" );
        // Stc rosat = (StcObsDataLocation) readStc( "RosatObs.xml" );
        // Stc query = (StcSearchLocation) readStc( "Query.xml" );
    }

    public void testChandra() throws IOException {
        StcResourceProfile chandra = 
            (StcResourceProfile) readStc( "ChandraSTCResource.xml" );
        assertEquals( Interval.class, chandra.getStcRegion().getClass() );
        assertEquals( 1, chandra.getStcNCoord() );
        AstroCoords ac = chandra.getStcCoord( 1 );

        // Stc.astroCoordsToKeyMap( ac ).show();

        String[] axisNames = ac.getName();
        assertEquals( 4, axisNames.length );
        assertArrayEquals( new String[] { "Position", "Position",
                                          "Time", "Energy" },
                           ac.getName() );

        Region error = ac.getError();
        assertEquals( 4, error.getNaxes() );

        assertNull( ac.getPixSize() );
    }

    private static Stc readStc( String filename ) throws IOException {
        InputStream in = StcTest.class.getResourceAsStream( filename );
        XmlChan chan = new XmlChan( in, null );
        Stc stc = (Stc) chan.read();
        assertEquals( "IVOA", chan.getXmlFormat() );
        in.close();

        assertTrue( stc.getStcRegion() instanceof Region );

        int ncoord = stc.getStcNCoord();
        for ( int i = 0; i < ncoord; i++ ) {
            assertNotNull( stc.getStcCoord( i + 1 ) );
        }
        
        return stc;
    }

    private static Box box( double x1, double y1, double x2, double y2 ) {
        return new Box( new Frame( 2 ), 1, new double[] { x1, y1 },
                        new double[] { x2, y2 }, null );
    }

    private void assertSameShape( Region region1, Region region2 ) {
        int ov = region1.overlap( region2 );
        assertEquals( "Regions don't match (" + ov + ")",
                      Region.OVERLAP_SAME, ov );
    }

}
