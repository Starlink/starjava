package uk.ac.starlink.fits;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.util.BufferedDataOutputStream;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.formats.TextTableWriter;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

/**
 * Fairly rigorous test for the BINTABLE column reading code in ColumnReader.
 */
public class ColumnReaderTest extends TestCase {

    int icol_;

    public ColumnReaderTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    /**
     * Writes a table then reads it back in again and tests the contents.
     */
    public void testWriteRead() throws IOException, HeaderCardException {
        File f = File.createTempFile( "table", ".fits" );
        f.deleteOnExit();
        OutputStream out = new FileOutputStream( f );
        writeIntegersBintable( 8, out );
        out.close();
        StarTable inTable = new FitsTableBuilder()
                           .makeStarTable( new FileDataSource( f ), true,
                                           StoragePolicy.PREFER_MEMORY );
        f.delete();
        // new TextTableWriter().writeStarTable( inTable, System.out );

        checkIntegersTable( inTable );
    }

    /**
     * Reads a table written by an earlier incarnation of this program,
     * and tests its contents.
     *
     * <p>This is effectively a regression test to prove that it's not 
     * changes in the writing code that allow the Write/Read test to pass.
     * This is a good idea, since the writing code is somewhat tricky
     * largely because of signed/unsigned integer type definitions in
     * the FITS standard.
     */
    public void testRead() throws IOException {
        URL url = ColumnReaderTest.class.getResource( "0-9.fits" );
        StarTable table = new FitsTableBuilder()
                         .makeStarTable( new URLDataSource( url ), true,
                                         StoragePolicy.PREFER_MEMORY );
        table = StoragePolicy.PREFER_MEMORY.randomTable( table );
        checkIntegersTable( table );
    }

    public void testTformX() throws IOException {
        // Tests a 75-bit vector column (TFORM1='75X')
        URL url = ColumnReaderTest.class.getResource( "testFlags.fits" );
        StarTable table = new FitsTableBuilder()
                         .makeStarTable( new URLDataSource( url ), true,
                                         StoragePolicy.PREFER_MEMORY );
        int nflag = 75;
        RowSequence rseq = table.getRowSequence();
        for ( int irow = 0; rseq.next(); irow++ ) {
            boolean[] flags = (boolean[]) rseq.getCell( 0 );
            assertEquals( flags.length, nflag );
            for ( int ic = 0; ic < nflag; ic++ ) {
                boolean flag = flags[ ic ];
                if ( ic == irow ) {
                    assertTrue( flag );
                }
                else {
                    assertFalse( flag );
                }
            }
        }
        rseq.close();
    }

    public void testTformX1() throws IOException {
        // Tests a number of 1-bit vector columns (TFORMn='X' or '1X').
        // The 1-element case is handled differently in code,
        // so it's useful to have a separate test.
        // It doesn't prove much to have lots of columns like this,
        // but that's the test file I have to hand.
        URL url = ColumnReaderTest.class.getResource( "testFlags2.fits" );
        StarTable table = new FitsTableBuilder()
                         .makeStarTable( new URLDataSource( url ), true,
                                         StoragePolicy.PREFER_MEMORY );
        int nflag = 75;
        RowSequence rseq = table.getRowSequence();
        for ( int irow = 0; rseq.next(); irow++ ) {
            for ( int ic = 0; ic < nflag; ic++ ) {
                boolean flag = ((Boolean) rseq.getCell( ic )).booleanValue();
                if ( ic == irow ) {
                    assertTrue( flag );
                }
                else {
                    assertFalse( flag  );
                }
            }
        }
        rseq.close();
    }

    /**
     * Tests the contents of a table.
     * It succeeds only if every element in row I is either a Number with
     * the value I or a numeric array in which every value has value I.
     *
     * @param  table  specially prepared input table
     */
    private void checkIntegersTable( StarTable table ) throws IOException {
        Tables.checkTable( table );
        int nrow = (int) table.getRowCount();
        int ncol = table.getColumnCount();
        assertTrue( nrow > 2 );
        assertTrue( ncol > 10 );
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            DescribedValue longOff =
               info.getAuxDatum( BintableStarTable.LONGOFF_INFO );
            if ( longOff != null ) {
                Class<?> clazz = info.getContentClass();
                assert String.class.equals( clazz )
                    || String[].class.equals( clazz );
                assert ! new BigInteger( (String) longOff.getValue() )
                        .equals( BigInteger.ZERO );
            }
            boolean isString = longOff != null;
            for ( int irow = 0; irow < nrow; irow++ ) {
                Object oval = table.getCell( irow, icol );
                if ( ! oval.getClass().isArray() ) {
                    assertEqualValue( irow, oval, isString );
                }
                else {
                    int nel = Array.getLength( oval );
                    for ( int iel = 0; iel < nel; iel++ ) {
                        assertEqualValue( irow, Array.get( oval, iel ),
                                          isString );
                    }
                }
            }
        }
    }

    private void assertEqualValue( int irow, Object oval, boolean isString ) {
        double num = isString ? Double.parseDouble( (String) oval )
                              : ((Number) oval).doubleValue();
        assertEquals( (double) irow, num );
    }

    /**
     * Writes a specially prepared FITS table which exercises as much of
     * the TZERO/TSCAL code in ColumnReader as possible.
     * Each cell value in row I is either a number with value I or a
     * numeric array in which every element has value I.
     *
     * @param  nrow  number of rows to output
     * @param  out  output stream
     */
    public static void writeIntegersBintable( int nrow, OutputStream out )
            throws IOException, HeaderCardException {

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

        byte[] bData3 = new byte[ nrow ];
        short[] sData3 = new short[ nrow ];
        int[] iData3 = new int[ nrow ];
        long[] lData3 = new long[ nrow ];
        float[] fData3 = new float[ nrow ];
        double[] dData3 = new double[ nrow ];

        byte[] bData4 = new byte[ nrow ];
        short[] sData4 = new short[ nrow ];
        int[] iData4 = new int[ nrow ];
        long[] lData4 = new long[ nrow ];
        float[] fData4 = new float[ nrow ];
        double[] dData4 = new double[ nrow ];

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

        byte[][] bData3s = new byte[ nrow ][];
        short[][] sData3s = new short[ nrow ][];
        int[][] iData3s = new int[ nrow ][];
        long[][] lData3s = new long[ nrow ][];
        float[][] fData3s = new float[ nrow ][];
        double[][] dData3s = new double[ nrow ][];

        byte[][] bData4s = new byte[ nrow ][];
        short[][] sData4s = new short[ nrow ][];
        int[][] iData4s = new int[ nrow ][];
        long[][] lData4s = new long[ nrow ][];
        float[][] fData4s = new float[ nrow ][];
        double[][] dData4s = new double[ nrow ][];

        int xoff = 23;
        double xscale = 4.0;
        for ( int i = 0; i < nrow; i++ ) {
            bData1[ i ] = (byte) i;
            sData1[ i ] = (short) i;
            iData1[ i ] = (int) i;
            lData1[ i ] = (long) i;
            fData1[ i ] = (float) i;
            dData1[ i ] = (double) i;

            bData2[ i ] = (byte) ( Byte.MIN_VALUE + i );
            sData2[ i ] = (short) ( Short.MIN_VALUE + i );
            iData2[ i ] = (int) ( Integer.MIN_VALUE + i );
            lData2[ i ] = (long) ( Long.MIN_VALUE + i );

            bData3[ i ] = (byte) ( i - xoff );
            sData3[ i ] = (short) ( i - xoff );
            iData3[ i ] = (int) ( i - xoff );
            lData3[ i ] = (long) ( i - xoff );
            fData3[ i ] = (float) ( i - xoff );
            dData3[ i ] = (double) ( i - xoff );

            bData4[ i ] = (byte) ( Byte.MIN_VALUE + i * xscale );
            sData4[ i ] = (short) ( i * xscale );
            iData4[ i ] = (int) ( i * xscale );
            lData4[ i ] = (long) ( i * xscale );
            fData4[ i ] = (float) ( i * xscale );
            dData4[ i ] = (double) ( i * xscale );

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

            bData3s[ i ] = new byte[] { bData3[ i ], bData3[ i ] };
            sData3s[ i ] = new short[] { sData3[ i ], sData3[ i ] };
            iData3s[ i ] = new int[] { iData3[ i ], iData3[ i ] };
            lData3s[ i ] = new long[] { lData3[ i ], iData3[ i ] };
            fData3s[ i ] = new float[] { fData3[ i ], fData3[ i ] };
            dData3s[ i ] = new double[] { dData3[ i ], dData3[ i ] };

            bData4s[ i ] = new byte[] { bData4[ i ], bData4[ i ] };
            sData4s[ i ] = new short[] { sData4[ i ], sData4[ i ] };
            iData4s[ i ] = new int[] { iData4[ i ], iData4[ i ] };
            lData4s[ i ] = new long[] { lData4[ i ], lData4[ i ] };
            fData4s[ i ] = new float[] { fData4[ i ], fData4[ i ] };
            dData4s[ i ] = new double[] { dData4[ i ], dData4[ i ] }; 
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

        int ib3 = table.addCol( bData3 );
        int is3 = table.addCol( sData3 );
        int ii3 = table.addCol( iData3 );
        int il3 = table.addCol( lData3 );
        int if3 = table.addCol( fData3 );
        int id3 = table.addCol( dData3 );

        int ib4 = table.addCol( bData4 );
        int is4 = table.addCol( sData4 );
        int ii4 = table.addCol( iData4 );
        int il4 = table.addCol( lData4 );
        int if4 = table.addCol( fData4 );
        int id4 = table.addCol( dData4 );

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

        int ib3s = table.addCol( bData3s );
        int is3s = table.addCol( sData3s );
        int ii3s = table.addCol( iData3s );
        int il3s = table.addCol( lData3s );
        int if3s = table.addCol( fData3s );
        int id3s = table.addCol( dData3s );

        int ib4s = table.addCol( bData4s );
        int is4s = table.addCol( sData4s );
        int ii4s = table.addCol( iData4s );
        int il4s = table.addCol( lData4s );
        int if4s = table.addCol( fData4s );
        int id4s = table.addCol( dData4s );

        /* Write to a FITS file. */
        BufferedDataOutputStream fout = new BufferedDataOutputStream( out );
        FitsTableSerializerConfig config = new FitsTableSerializerConfig() {
            public WideFits getWide() {
                return null;
            }
            public boolean allowSignedByte() {
                return true;
            }
            public boolean allowZeroLengthString() {
                return true;
            }
            public byte getPadCharacter() {
                return (byte) '\0';
            }
        };
        FitsTableSerializer ser =
            new StandardFitsTableSerializer( config, table );
        FitsConstants.writeEmptyPrimary( fout );
        Header hdr = ser.getHeader();

        hdr.removeCard( "TZERO" + ( ib2 + 1 ) );
        hdr.addValue( "TZERO" + ( is2 + 1 ), "32768", "unsigned offset" );
        hdr.addValue( "TZERO" + ( ii2 + 1 ), "2147483648", "unsigned offset" );
        hdr.addValue( "TZERO" + ( il2 + 1 ), "9223372036854775808",
                                             "unsigned offset" );

        hdr.addValue( "TZERO" + ( ib3 + 1 ), -128 + xoff, "offset" );
        hdr.addValue( "TZERO" + ( is3 + 1 ), xoff, "offset" );
        hdr.addValue( "TZERO" + ( ii3 + 1 ), xoff, "offset" );
        hdr.addValue( "TZERO" + ( il3 + 1 ), xoff, "offset" );
        hdr.addValue( "TZERO" + ( if3 + 1 ), xoff, "offset" );
        hdr.addValue( "TZERO" + ( id3 + 1 ), xoff, "offset" );
       
        hdr.removeCard( "TZERO" + ( ib4 + 1 ) );
        hdr.addValue( "TSCAL" + ( ib4 + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( is4 + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( ii4 + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( il4 + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( if4 + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( id4 + 1 ), 1.0 / xscale, "scaling" );

        hdr.removeCard( "TZERO" + ( ib2s + 1 ) );
        hdr.addValue( "TZERO" + ( is2s + 1 ), "32768", "unsigned offset" );
        hdr.addValue( "TZERO" + ( ii2s + 1 ), "2147483648", "unsigned offset" );
        hdr.addValue( "TZERO" + ( il2s + 1 ), "9223372036854775808",
                                              "unsigned offset" );

        hdr.addValue( "TZERO" + ( ib3s + 1 ), -128 + xoff, "offset" );
        hdr.addValue( "TZERO" + ( is3s + 1 ), xoff, "offset" );
        hdr.addValue( "TZERO" + ( ii3s + 1 ), xoff, "offset" );
        hdr.addValue( "TZERO" + ( il3s + 1 ), xoff, "offset" );
        hdr.addValue( "TZERO" + ( if3s + 1 ), xoff, "offset" );
        hdr.addValue( "TZERO" + ( id3s + 1 ), xoff, "offset" );

        hdr.removeCard( "TZERO" + ( ib4s + 1 ) );
        hdr.addValue( "TSCAL" + ( ib4s + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( is4s + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( ii4s + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( il4s + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( if4s + 1 ), 1.0 / xscale, "scaling" );
        hdr.addValue( "TSCAL" + ( id4s + 1 ), 1.0 / xscale, "scaling" );

        FitsConstants.writeHeader( fout, hdr );
        ser.writeData( fout );
        fout.flush();
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

    /**
     * Writes an example integer FITS file into the current directory.
     */
    public static void main( String[] args )
            throws IOException, HeaderCardException {
        int nrow = 10;
        File f = new File( "0-" + ( nrow - 1 ) + ".fits" );
        OutputStream out = new FileOutputStream( f );
        writeIntegersBintable( nrow, out );
        out.close();
        System.out.println( nrow + " rows written to " + f );
    }
}
