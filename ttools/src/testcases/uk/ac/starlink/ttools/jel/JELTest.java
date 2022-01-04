package uk.ac.starlink.ttools.jel;

import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import gnu.jel.CompilationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ConstantColumn;
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
        ColumnInfo multInfo =
            new ColumnInfo( "*Count*", Integer.class, "Number of persons" );
        multInfo.setUnitString( "persons" );
        multInfo.setUCD( "meta.number" );
        StarTable t1 = new QuickTable( 3, new ColumnData[] {
            col( "Name", new String[] { "Lupin", "Novena", "Delios", } ),
            col( "Level", new int[] { 6, 7, 8 } ),
            col( "Gold", new double[] { 17, 9.5, 15.25, } ),
            new ConstantColumn( multInfo, new Integer( 1 ) ),
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

        /* Test metadata propagation. */
        StarTable ct = JELTable.createJELTable( t1, new String[] {
            "*COUNT*",
            "$4",
            "ucd$meta_number",
            "100 - $4",
        } );
        assertEquals( 4, ct.getColumnCount() );
        assertArrayEquals( box( new int[] { 1, 1, 1, 99 } ), ct.getRow( 0 ) );
        for ( int ic = 0; ic < 3; ic++ ) {
            ColumnInfo info = ct.getColumnInfo( ic );
            assertEquals( multInfo.getName(), info.getName() );
            assertEquals( multInfo.getDescription(), info.getDescription() );
            assertEquals( multInfo.getUnitString(), info.getUnitString() );
            assertEquals( multInfo.getUCD(), info.getUCD() );
            assertEquals( Integer.class, info.getContentClass() );
        }
        assertEquals( Integer.class, ct.getColumnInfo( 3 ).getContentClass() );
        Tables.checkTable( ct );

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

    public void testJELArrayFunction() throws CompilationException {
        assertArrayEquals(
            new int[] { 30, 300, 3000 },
            JELArrayFunction.evaluate( "i", "x", "3*x",
                                       new int[] { 10, 100, 1000 } ) );
        assertArrayEquals(
            new float[] { 1f, 32f, 1024f, 0.5f },
            JELArrayFunction.evaluate( "i", "x", "(float)pow(2,x)",
                                       new double[] { 0, 5, 10, -1 } ) );
        assertArrayEquals(
            new boolean[] { true, false, true },
            JELArrayFunction.evaluate( "i", "flag", "!flag",
                                       new boolean[] { false, true, false } ) );
        assertArrayEquals(
            new int[] { 3, 6, 0, 9, },
            JELArrayFunction.evaluate( "i", "s", "s.length()",
                   new String[] { "dog", "rabbit", null, "crocodile", } ) );
        assertArrayEquals(
            new double[] { 3., 6., Double.NaN, 9., },
            JELArrayFunction.evaluate( "i", "s", "(double)s.length()",
                   new String[] { "dog", "rabbit", null, "crocodile", } ) );
        assertArrayEquals(
            new String[] { null, "ap", "or", null },
            JELArrayFunction.evaluate( "i", "fruit", "fruit.substring(0, 2)",
                   new String[] { null, "apple", "orange", "p", } ) );

        assertArrayEquals(
            new int[] { 1, 11, 102, 1003 },
            JELArrayFunction.evaluate( "i", "$", "i+$",
                   new int[] { 1, 10, 100, 1000 } ) );

        assertEquals( int[].class,
                      new JELArrayFunction<short[],Object>( "ix", "ival",
                                                            "(int)ival",
                                                            short[].class,
                                                            Object.class )
                     .evaluate( new short[ 0 ] ).getClass() );
        assertEquals( int[].class,
                      new JELArrayFunction<short[],int[]>( "ix", "ival",
                                                           "(int)ival",
                                                           short[].class,
                                                           int[].class )
                     .evaluate( new short[ 0 ] ).getClass() );
        assertEquals( long[].class,
                      new JELArrayFunction<short[],long[]>( "ix", "ival",
                                                            "(int)ival",
                                                            short[].class,
                                                            long[].class )
                     .evaluate( new short[ 0 ] ).getClass() );
        try {
            new JELArrayFunction<short[],boolean[]>( "ix", "ival", "(int)ival",
                                                     short[].class,
                                                     boolean[].class );
            fail();
        }
        catch ( CompilationException e ) {
            // OK
        }
    }

    public void testObject() throws Throwable {
        testRandom( false );
        testRandom( true );
    }

    private void testRandom( boolean isThreadsafe ) throws Throwable {
        byte[] buf = new StringBuffer()
            .append( "a,b,s\n" )
            .append( "1,10,one\n" )
            .append( ",20,\n" )
            .toString().getBytes( "utf-8" );
        StarTable t2 =
            Tables.randomTable(
                new CsvStarTable( new ByteArrayDataSource( "buf", buf ) ) );
        RandomJELRowReader rdr =
            isThreadsafe ? RandomJELRowReader.createConcurrentReader( t2 )
                         : RandomJELRowReader.createAccessReader( t2 );
        Class[] staticLib = new Class[] { FuncLib.class };
        Class[] dynamicLib = new Class[] { rdr.getClass() };
        Library lib = JELUtils.createLibrary( staticLib, dynamicLib, rdr );
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

    public void testStringComparison() throws Throwable {

        // This tests for presence of a bug in JEL versions 0.9.8 to 2.1.2,
        // which yielded a NullPointerException when a String was compared
        // (==) against a null value.  Fixed in JEL 2.1.3.
        Library lib = new Library( new Class[] { FuncLib.class },
                                   null, null, null, null );
        assertTrue( Evaluator
                   .compile( "copyText(\"abc\", false)==\"abc\"", lib )
                   .evaluate_boolean( null ) );
        assertFalse( Evaluator
                    .compile( "copyText(\"abc\", true)==\"abc\"", lib )
                    .evaluate_boolean( null ) );
    }

    public static class FuncLib {
        public static int triplePrim( int a ) {
            return 3 * a;
        }

        public static int tripleObj( Object a ) {
            return a instanceof Number ? 3 * ((Number) a).intValue()
                                       : Integer.MIN_VALUE;
        }

        public static String copyText( String txt, boolean isNull ) {
            return isNull ? null : txt;
        }
    }
}
