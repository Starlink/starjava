package uk.ac.starlink.ast;

import java.awt.Rectangle;
import uk.ac.starlink.ast.grf.GrfEscape;
import uk.ac.starlink.util.TestCase;

public class EscapeTest extends TestCase {

    final String h2so4 = "H%v50+2%v+SO%v50+4%v+";

    public EscapeTest( String name ) {
        super( name );
    }

    public void testPlotEscapes() {
        String tricky = "Axis %^50+ %s50+ one %s+ %^+";
        String tidy = "Axis   one  ";

        GrfEscape.setEscapes( true );
        if ( ! isHeadless() ) {
            Plot p2 = new Plot( new Frame( 2 ), new Rectangle( 512, 512 ),
                                new double[] { 0., 0., 512., 512. } );
            p2.set( "label(1)=" + tricky );
            assertEquals( tricky, p2.getLabel(1) );
        }
        GrfEscape.setEscapes( false );
    }

    public void testEscapes() {
        Frame f1 = new Frame( 2 );
        String raw = h2so4;
        String stripped = "H2SO4";
        f1.setTitle( raw );
        assertTrue( ! GrfEscape.getEscapes() );
        assertEquals( stripped, f1.getTitle() );
        GrfEscape.setEscapes( true );
        assertTrue( GrfEscape.getEscapes() );
        assertEquals( raw, f1.getTitle() );
        GrfEscape.setEscapes( false );
        assertEquals( stripped, f1.getTitle() );
    }

    public void testFindEscapes() {
        Object[] seq = new Object[] {
            "H",
            new GrfEscape( GrfEscape.GRF__ESSUB, 50 ),
            "2",
            new GrfEscape( GrfEscape.GRF__ESSUB, -1 ),
            "SO",
            new GrfEscape( GrfEscape.GRF__ESSUB, 50 ),
            "4",
            new GrfEscape( GrfEscape.GRF__ESSUB, -1 ),
        };
        assertArrayEquals( seq, GrfEscape.findEscapes( h2so4 ) );
    }

    public void testGetText() {
        StringBuffer sbuf = new StringBuffer();
        Object[] seq = GrfEscape.findEscapes( h2so4 );
        for ( int i = 0; i < seq.length; i++ ) {
            Object o = seq[ i ];
            if ( o instanceof String ) {
                sbuf.append( (String) o );
            }
            else if ( o instanceof GrfEscape ) {
                sbuf.append( ((GrfEscape) o).getText() );
            }
            else {
                fail();
            }
        }
        assertEquals( sbuf.toString(), h2so4 );
    }

    private void checkText( int code, int value, String rep ) {
        assertEquals( rep, new GrfEscape( code, value ).getText() );
    }

    public void testCodes() {
        checkText( GrfEscape.GRF__ESPER,  0, "%%" );
        checkText( GrfEscape.GRF__ESSUP,  1, "%^1+" );
        checkText( GrfEscape.GRF__ESSUP, -1, "%^+" );
        checkText( GrfEscape.GRF__ESSUB,  1, "%v1+" );
        checkText( GrfEscape.GRF__ESSUB, -1, "%v+" );
        checkText( GrfEscape.GRF__ESGAP,  1, "%>1+" );
        checkText( GrfEscape.GRF__ESBAC,  1, "%<1+" );
        checkText( GrfEscape.GRF__ESSIZ,  1, "%s1+" );
        checkText( GrfEscape.GRF__ESSIZ, -1, "%s+" );
        checkText( GrfEscape.GRF__ESWID,  1, "%w1+" );
        checkText( GrfEscape.GRF__ESWID, -1, "%w+" );
        checkText( GrfEscape.GRF__ESFON,  1, "%f1+" );
        checkText( GrfEscape.GRF__ESFON, -1, "%f+" );
        checkText( GrfEscape.GRF__ESCOL,  1, "%c1+" );
        checkText( GrfEscape.GRF__ESCOL, -1, "%c+" );
        checkText( GrfEscape.GRF__ESSTY,  1, "%t1+" );
        checkText( GrfEscape.GRF__ESSTY, -1, "%t+" );
        checkText( GrfEscape.GRF__ESPOP,  0, "%-" );
        checkText( GrfEscape.GRF__ESPSH,  0, "%+" );
    }

    public void testMore() {
        String tricky = "Axis %^50+ %s50+ one %s+ %^+";
        String tidy = "Axis   one  ";
        Frame frm = new Frame( 1 );

        frm.setID( tricky );
        GrfEscape.setEscapes( true );
        assertEquals( frm.getID(), tricky );
        GrfEscape.setEscapes( false );
        assertEquals( frm.getID(), tidy );

        frm.set( "ID=" + tricky );
        // frm.setID( tricky );
        GrfEscape.setEscapes( true );
        assertEquals( frm.getID(), tricky );
        GrfEscape.setEscapes( false );
        assertEquals( frm.getID(), tidy );
    }

    public void testSet() {
        GrfEscape.setEscapes( true );
        exerciseSet( "LabelOne", "LabelTwo" );
        exerciseSet( "%%%%%%%%", "label %d %s" );
        exerciseSet( "label %d %s", "  %% %%x% %% " );
    }

    public void exerciseSet( String l1, String l2 ) {
        String setting = "label(1)=" + l1 + ",label(2)=" + l2;
        if ( ! isHeadless() ) {
            Plot plot = new Plot( new Frame( 2 ), new Rectangle( 512, 512 ),
                                  new double[] { 0., 0., 512., 512. } );

            plot.set( setting );
            assertEquals( l1, plot.getLabel( 1 ) );
            assertEquals( l2, plot.getLabel( 2 ) );
        }

        Frame frm = new Frame( 2 );
        frm.set( setting );
        assertEquals( l1, frm.getLabel( 1 ) );
        assertEquals( l2, frm.getLabel( 2 ) );
    }

}
