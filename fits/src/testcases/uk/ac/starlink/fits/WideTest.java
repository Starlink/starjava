package uk.ac.starlink.fits;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.LogUtils;

public class WideTest extends TestCase {

    public WideTest() {
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.SEVERE );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
    }

    // AlphaWideFits is a historical relic - this test could be removed
    public void testBase26() {
        AbstractWideFits.AlphaWideFits alphaWide =
            new AbstractWideFits.AlphaWideFits( 999 );
        assertEquals( "AAA", alphaWide.encodeInteger( 0 ) );
        assertEquals( "AAB", alphaWide.encodeInteger( 1 ) );
        assertEquals( "ABA", alphaWide.encodeInteger( 26 ) );
        assertEquals( "ZZZ", alphaWide.encodeInteger( 26 * 26 * 26 - 1 ) );
        try {
            alphaWide.encodeInteger( 26 * 26 * 26 );
            fail();
        }
        catch ( NumberFormatException e ) {
        }
        try {
            alphaWide.encodeInteger( -1 );
            fail();
        }
        catch ( NumberFormatException e ) {
        }

        for ( int i = 0; i < 26 * 26 * 26; i++ ) {
            assertEquals( i, decodeInteger( alphaWide.encodeInteger( i ) ) );
        }
    }

    public void testName() {
        assertEquals( "hierarch",
                      WideFits.DEFAULT.toString() );
        // AlphaWideFits is a historical relic - these tests could be removed
        assertEquals( "alpha",
                      AbstractWideFits.createAlphaWideFits( 999 ).toString() );
        assertEquals( "alpha23",
                      AbstractWideFits.createAlphaWideFits( 23 ).toString() );
        assertEquals( "hierarch",
                  AbstractWideFits.createHierarchWideFits( 999 ).toString() );
        assertEquals( "hierarch55",
                  AbstractWideFits.createHierarchWideFits( 55 ).toString() );
    }

    public void testReadWrite() throws IOException {
        // AlphaWideFits is a historical relic
        WideFits[] wides = {
            null,
            WideFits.DEFAULT,
            AbstractWideFits.createAlphaWideFits( 9 ),  // could be removed
            AbstractWideFits.createAlphaWideFits( 6 ),  // could be removed
            AbstractWideFits.createHierarchWideFits( 6 ),
            AbstractWideFits.createHierarchWideFits( 9 ),
        };
        for ( WideFits wide : wides ) {
            exerciseReadWrite(
                configWriter( new FitsTableWriter(), true, wide ),
                new FitsTableBuilder( wide ) );
            exerciseReadWrite(
                configWriter( new ColFitsTableWriter(), true, wide ),
                new ColFitsTableBuilder( wide ) );
            VariableFitsTableWriter vWriter = new VariableFitsTableWriter();
            vWriter.setLongIndexing( Boolean.FALSE );
            exerciseReadWrite( configWriter( vWriter, true, wide ),
                               new FitsTableBuilder( wide ) );
        }
    }

    private void exerciseReadWrite( StarTableWriter writer,
                                    TableBuilder builder )
            throws IOException {
        StarTable table1 = createIntegerTable( 21 );
        long nrow = table1.getRowCount();
        int ncol = table1.getColumnCount();
        assertTrue( nrow > 2 );
        assertTrue( ncol > 10 );

        File f = File.createTempFile( "table", ".fits" );
        f.deleteOnExit();
        OutputStream out = new FileOutputStream( f );
        writer.writeStarTable( table1, out );
        out.close();
        StarTable tout =
            builder.makeStarTable( new FileDataSource( f ), true,
                                   StoragePolicy.PREFER_MEMORY );
        tout = Tables.randomTable( tout );

        assertEquals( ncol, tout.getColumnCount() );
        assertEquals( nrow, tout.getRowCount() );
        checkIntegerTable( tout, ncol );
    }

    public void testUnwide() throws IOException {
        exerciseUnwide( 6 );
        exerciseUnwide( 9 );
    }

    private void exerciseUnwide( int iExtCol ) throws IOException {
        StarTable table1 = createIntegerTable( 23 );
        long nrow = table1.getRowCount();
        int ncol = table1.getColumnCount();
        assertTrue( nrow > 2 );
        assertTrue( ncol > 10 );

        WideFits[] wides = {
            AbstractWideFits.createAlphaWideFits( iExtCol ), // could be rm'd
            AbstractWideFits.createHierarchWideFits( iExtCol ),
        };
        for ( WideFits wide : wides ) {
            StarTableWriter writer =
                configWriter( new FitsTableWriter(), true, wide );
            File f = File.createTempFile( "table", ".fits" );
            f.deleteOnExit();
            OutputStream out = new FileOutputStream( f );
            writer.writeStarTable( table1, out );
            out.close();
            StoragePolicy storage = StoragePolicy.PREFER_MEMORY;
            DataSource datsrc = new FileDataSource( f );
            StarTable wideTable = new FitsTableBuilder( wide )
                                 .makeStarTable( datsrc, true, storage );
            StarTable thinTable = new FitsTableBuilder( (WideFits) null )
                                 .makeStarTable( datsrc, true, storage );
            assertEquals( nrow, wideTable.getRowCount() );
            assertEquals( nrow, thinTable.getRowCount() );
            assertEquals( ncol, wideTable.getColumnCount() );

            /* Check that a non-WideFits-aware reader can still read this
             * table, though it won't get the same data. */
            assertEquals( iExtCol, thinTable.getColumnCount() );
        }
    }

    private void checkIntegerTable( StarTable table, int ncol )
            throws IOException {
        Tables.checkTable( table );
        int nrow = (int) table.getRowCount();
        for ( int irow = 0; irow < nrow; irow++ ) {
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object oval = table.getCell( irow, icol );
                if ( oval instanceof Number ) {
                    assertEquals( (double) irow,
                                  ((Number) oval).doubleValue() );
                }
                else {
                    int nel = Array.getLength( oval );
                    for ( int iel = 0; iel < nel; iel++ ) {
                        assertEquals( (double) irow,
                                      ((Number) Array.get( oval, iel ))
                                               .doubleValue() );
                    }
                }
            }
        }
    }

    /**
     * Writes a FITS table with a lot of different columns.
     * Each cell value in row I is either a number with value I or a
     * numeric array in which every element has value I.
     *
     * @param  nrow  number of rows to output
     * @param  out  output stream
     */
    public static StarTable createIntegerTable( int nrow ) {

        /* Set up table. */
        ExampleTable table = new ExampleTable( nrow );

        byte[] bData1 = new byte[ nrow ];
        short[] sData1 = new short[ nrow ];
        int[] iData1 = new int[ nrow ];
        long[] lData1 = new long[ nrow ];
        float[] fData1 = new float[ nrow ];
        double[] dData1 = new double[ nrow ];

        byte[] bData2 = new byte[ nrow ];
        short[] sData2 = new short[ nrow ];
        int[] iData2 = new int[ nrow ];
        long[] lData2 = new long[ nrow ];

        byte[][] bData1s = new byte[ nrow ][];
        short[][] sData1s = new short[ nrow ][];
        int[][] iData1s = new int[ nrow ][];
        long[][] lData1s = new long[ nrow ][];
        float[][] fData1s = new float[ nrow ][];
        double[][] dData1s = new double[ nrow ][];

        byte[][] bData2s = new byte[ nrow ][];
        short[][] sData2s = new short[ nrow ][];
        int[][] iData2s = new int[ nrow ][];
        long[][] lData2s = new long[ nrow ][];

        for ( int i = 0; i < nrow; i++ ) {
            bData1[ i ] = (byte) i;
            sData1[ i ] = (short) i;
            iData1[ i ] = (int) i;
            lData1[ i ] = (long) i;
            fData1[ i ] = (float) i;
            dData1[ i ] = (double) i;

            bData2[ i ] = (byte) i;
            sData2[ i ] = (short) i;
            iData2[ i ] = (int) i;
            lData2[ i ] = (long) i;

            bData1s[ i ] = new byte[] { bData1[ i ], bData1[ i ] };
            sData1s[ i ] = new short[] { sData1[ i ], sData1[ i ] };
            iData1s[ i ] = new int[] { iData1[ i ], iData1[ i ] };
            lData1s[ i ] = new long[] { lData1[ i ], lData1[ i ] };
            fData1s[ i ] = new float[] { fData1[ i ], fData1[ i ] };
            dData1s[ i ] = new double[] { dData1[ i ], dData1[ i ] };

            bData2s[ i ] = new byte[] { bData2[ i ], bData2[ i ] };
            sData2s[ i ] = new short[] { sData2[ i ], sData2[ i ] };
            iData2s[ i ] = new int[] { iData2[ i ], iData2[ i ] };
            lData2s[ i ] = new long[] { lData2[ i ], lData2[ i ] };
        }
        int ib1 = table.addCol( bData1 );
        int is1 = table.addCol( sData1 );
        int ii1 = table.addCol( iData1 );
        int il1 = table.addCol( lData1 );
        int if1 = table.addCol( fData1 );
        int id1 = table.addCol( dData1 );

        int ib2 = table.addCol( bData2 );
        int is2 = table.addCol( sData2 );
        int ii2 = table.addCol( iData2 );
        int il2 = table.addCol( lData2 );

        int ib1s = table.addCol( bData1s );
        int is1s = table.addCol( sData1s );
        int ii1s = table.addCol( iData1s );
        int il1s = table.addCol( lData1s );
        int if1s = table.addCol( fData1s );
        int id1s = table.addCol( dData1s );

        int ib2s = table.addCol( bData2s );
        int is2s = table.addCol( sData2s );
        int ii2s = table.addCol( iData2s );
        int il2s = table.addCol( lData2s );

        return table;
    }

    private static int decodeInteger( String str ) {
        int base = 26;
        char digit0 = 'A';
        int ix = 0;
        int leng = str.length();
        if ( leng < 1 || leng > 3 ) {
            throw new NumberFormatException( "Bad index length (0-3): " + str );
        }
        int mult = 1;
        for ( int k = 0; k < leng; k++ ) {
            char c = str.charAt( leng - 1 - k );
            int j = c - digit0;
            if ( j < 0 || j >= base ) {
                throw new NumberFormatException( "Bad index: " + c );
            }
            ix += mult * j;
            mult *= base;
        }
        return ix;
    }

    private static AbstractFitsTableWriter
            configWriter( AbstractFitsTableWriter writer,
                          boolean allowSignedByte, WideFits wide ) {
        writer.setAllowSignedByte( allowSignedByte );
        writer.setWide( wide );
        return writer;
    }


    /**
     * Utility class.
     */
    private static class ExampleTable extends ColumnStarTable {
        private final int nrow_;

        ExampleTable( int nrow ) {
            nrow_ = nrow;
        }

        public int addCol( Object array ) {
            int icol = getColumnCount();
            super.addColumn( ArrayColumn.makeColumn( "c" + ( icol + 1 ),
                                                     array ) );
            return icol;
        }

        public long getRowCount() {
            return nrow_;
        }
    }
}
