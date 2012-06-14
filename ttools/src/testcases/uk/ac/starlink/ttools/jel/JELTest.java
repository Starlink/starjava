package uk.ac.starlink.ttools.jel;

import gnu.jel.CompilationException;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.QuickTable;

public class JELTest extends TableTestCase {

    public JELTest( String name ) {
        super( name );
    }

    public void testJELTable() throws Exception {
        StarTable t1 = new QuickTable( 3, new ColumnData[] {
            col( "Name", new String[] { "Lupin", "Novena", "Delios", } ),
            col( "Level", new int[] { 6, 7, 8 } ),
            col( "Gold", new double[] { 17, 9.5, 15.25, } ),
        } );
        Tables.checkTable( t1 );

        ColumnInfo[] colInfos = new ColumnInfo[] {
            new ColumnInfo( new DefaultValueInfo( "Initial", String.class ) ),
            new ColumnInfo( new DefaultValueInfo( "FValue", Number.class ) ),
            new ColumnInfo( new DefaultValueInfo( "DValue" ) ),
        };
        String[] exprs = new String[] {
            "Name.substring( 0, 1 )",
            "Level*100 + Gold",
            "Gold+Level*100",
        };
        StarTable jt = new JELTable( t1, colInfos, exprs );

        assertArrayEquals( new String[] { "Initial", "FValue", "DValue", },
                           getColNames( jt ) );
        assertArrayEquals( new Object[] { "L", "N", "D", },
                           getColData( jt, 0 ) );
        assertArrayEquals( box( new double[] { 617., 709.5, 815.25 } ),
                           getColData( jt, 1 ) );
        assertArrayEquals( box( new double[] { 617., 709.5, 815.25 } ),
                           getColData( jt, 2 ) );
        assertEquals( String.class, jt.getColumnInfo( 0 ).getContentClass() );
        assertEquals( Double.class, jt.getColumnInfo( 1 ).getContentClass() );
        assertEquals( Double.class, jt.getColumnInfo( 2 ).getContentClass() );

        /* Now try one with mismatched colinfos. */
        try {
            new JELTable(
                t1,
                new ColumnInfo[] {
                    new ColumnInfo( new DefaultValueInfo( "L",
                                                          Integer.class ) ),
                },
                new String[] { "\"<\" + Name + \">\"", } );
            fail();
        }
        catch ( IllegalArgumentException e ) {
        }
    }

    public void testJELFunction() throws CompilationException {
        assertEquals( 30, new JELFunction( "x", "x+29" ).evaluate( 1 ) );
        assertEquals( 16, new JELFunction( "exponent", "pow(2,exponent)" )
                         .evaluate( 4 ) );
        try {
            new JELFunction( "mjd", "mjdToDate(mjd)" );
            fail();
        }
        catch ( CompilationException e ) {
        }
    }
}
