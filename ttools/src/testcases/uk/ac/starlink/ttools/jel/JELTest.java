package uk.ac.starlink.ttools.jel;

import gnu.jel.CompiledExpression;
import gnu.jel.Library;
import gnu.jel.CompilationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.CsvStarTable;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.ttools.QuickTable;
import uk.ac.starlink.util.ByteArrayDataSource;

public class JELTest extends TableTestCase {

    public JELTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
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

    public void testObject() throws Throwable {
        byte[] buf = new StringBuffer()
            .append( "a,b,s\n" )
            .append( "1,10,one\n" )
            .append( ",20,\n" )
            .toString().getBytes( "utf-8" );
        StarTable t2 =
            Tables.randomTable(
                new CsvStarTable( new ByteArrayDataSource( "buf", buf ) ) );
        RandomJELRowReader rdr = new RandomJELRowReader( t2 );
        rdr.setCurrentRow( 0 );
        Class[] staticLib = new Class[] { FuncLib.class };
        Class[] dynamicLib = new Class[] { rdr.getClass() };
        Library lib = new Library( staticLib, dynamicLib, new Class[ 0 ],
                                   rdr, null );
        CompiledExpression pExpr =
            JELUtils.compile( lib, t2, "triplePrim(a)" );
        CompiledExpression oExpr =
            JELUtils.compile( lib, t2, "tripleObj(Object$a)" );
 
        assertEquals( new Integer( 3 ), rdr.evaluateAtRow( pExpr, 0 ) );
        assertEquals( new Integer( 3 ), rdr.evaluateAtRow( oExpr, 0 ) );
        assertEquals( null, rdr.evaluateAtRow( pExpr, 1 ) );
        assertEquals( new Integer( Integer.MIN_VALUE ),
                      rdr.evaluateAtRow( oExpr, 1 ) );

        CompiledExpression soExpr =
            JELUtils.compile( lib, t2, "Object$s" );
        CompiledExpression sExpr =
            JELUtils.compile( lib, t2, "s" );
        assertEquals( "one", rdr.evaluateAtRow( soExpr, 0 ) );
        assertEquals( "one", rdr.evaluateAtRow( sExpr, 0 ) );
        assertEquals( null, rdr.evaluateAtRow( soExpr, 1 ) );
        assertEquals( null, rdr.evaluateAtRow( sExpr, 1 ) );
    }

    public static class FuncLib {
        public static int triplePrim( int a ) {
            return 3 * a;
        }

        public static int tripleObj( Object a ) {
            return a instanceof Number ? 3 * ((Number) a).intValue()
                                       : Integer.MIN_VALUE;
        }
    }
}
