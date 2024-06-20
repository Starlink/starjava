package uk.ac.starlink.table;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.AssertionFailedError;
import uk.ac.starlink.util.TestCase;

public class TableCase extends TestCase {

    public TableCase() {
    }

    public TableCase( String name ) {
        super( name );
    }

    public void assertVOTableEquals( StarTable t1, StarTable t2,
                                     boolean squashNulls ) throws IOException {
        int ncol = t1.getColumnCount();
        assertEquals( ncol, t2.getColumnCount() );
        assertValueSetEquals( t1.getParameters(), t2.getParameters() );
        assertEquals( t1.getName(), t2.getName() );

        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo cinfo1 = t1.getColumnInfo( icol );
            ColumnInfo cinfo2 = t2.getColumnInfo( icol );
            Class clazz1 = cinfo1.getContentClass();
            Class clazz2 = cinfo2.getContentClass();
            if ( clazz1 == byte[].class && clazz2 == short[].class ||
                 clazz1 == Byte.class && clazz2 == Short.class ) {
                // ok
            }
            else {
                assertValueInfoConsistent( cinfo1, cinfo2 );
            }
        }

        RowSequence rseq1 = t1.getRowSequence();
        RowSequence rseq2 = t2.getRowSequence();
        while ( rseq1.next() ) {
            assertTrue( rseq2.next() );
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object cell1 = rseq1.getCell( icol );
                Object cell2 = rseq2.getCell( icol );
                if ( cell1 instanceof byte[] && cell2 instanceof short[] ) {
                    int nel = ((short[]) cell2).length;
                    short[] c1x = new short[ nel ];
                    for ( int i = 0; i < nel; i++ ) {
                        c1x[ i ] = ((byte[]) cell1)[ i ];
                    }
                    cell1 = c1x;
                }
                else if ( cell1 instanceof Byte && cell2 instanceof Short ) {
                    cell1 = Short.valueOf( ((Number) cell1).shortValue() );
                }
                else if ( cell1 == null &&
                          cell2 != null &&
                          cell2.getClass().getComponentType() != null ) {
                    cell2 = null;
                }
                try {
                    assertScalarOrArrayEquals( cell1, cell2 );
                }
                catch ( AssertionFailedError e ) {
                    if ( squashNulls && cell1 == null ) {
                        // ok
                    }
                    else {
                        throw e;
                    }
                }
            }
        }
        assertTrue( ! rseq1.next() );
        assertTrue( ! rseq2.next() );
        rseq1.close();
        rseq2.close();
    }

    public void assertTableEquals( StarTable t1, StarTable t2 )
            throws IOException {
        int ncol = t1.getColumnCount();
        assertEquals( ncol, t2.getColumnCount() );
        assertValueSetEquals( t1.getParameters(), t2.getParameters() );
        assertEquals( t1.getName(), t2.getName() );
        assertEquals( t1.getURL() + "", t2.getURL() + "" );
        for ( int i = 0; i < ncol; i++ ) {
            assertColumnInfoEquals( t1.getColumnInfo( i ),
                                    t2.getColumnInfo( i ) );
        }
        assertRowSequenceEquals( t1, t2 );
    }

    void assertRowSequenceEquals( StarTable t1, StarTable t2 )
            throws IOException {
        int ncol = t1.getColumnCount();
        assertEquals( ncol, t2.getColumnCount() );
        RowSequence rseq1 = t1.getRowSequence();
        RowSequence rseq2 = t2.getRowSequence();
        int irow = 0;
        while ( rseq1.next() ) {
            assertTrue( "irow: " + irow, rseq2.next() );
            Object[] row1 = rseq1.getRow();
            Object[] row2 = rseq2.getRow();
            for ( int i = 0; i < ncol; i++ ) {
                assertScalarOrArrayEquals( row1[ i ], row2[ i ] );
            }
            for ( int i = ncol - 1; i >= 0; i-- ) {
                assertScalarOrArrayEquals( row1[ i ], rseq1.getCell( i ) );
                assertScalarOrArrayEquals( row1[ i ], rseq2.getCell( i ) );
            }
            irow++;
        }
        assertTrue( ! rseq1.next() );
        assertTrue( ! rseq2.next() );
        rseq1.close();
        rseq2.close();
    }

    void assertScalarOrArrayEquals( Object o1, Object o2 ) {
        o1 = blankToNull( o1 );
        o2 = blankToNull( o2 );
        if ( o1 != null && o1.getClass().isArray() ) {
            assertArrayEquals( o1, o2 );
        }
        else {
            assertEquals( o1, o2 );
        }
    }

    void assertValueSetEquals( List<DescribedValue> dvals1,
                               List<DescribedValue> dvals2 ) {
        if ( dvals1 == null && dvals2 == null ) {
            return;
        }
        int nparam = dvals1.size();
        Comparator<DescribedValue> sorter = new DescribedValueComparator();
        Collections.sort( dvals1, sorter );
        Collections.sort( dvals2, sorter );
        assertEquals( dvals1 + "\t" + dvals2, nparam, dvals2.size() );
        for ( int i = 0; i < nparam; i++ ) {
            DescribedValue dv1 = dvals1.get( i );
            DescribedValue dv2 = dvals2.get( i );
            assertScalarOrArrayEquals( dv1.getValue(), dv2.getValue() );
            assertValueInfoEquals( dv1.getInfo(), dv2.getInfo() );
        }
    }

    void assertColumnInfoEquals( ColumnInfo c1, ColumnInfo c2 ) {
        assertValueInfoEquals( c1, c2 );
        assertValueSetEquals( c1.getAuxData(), c2.getAuxData() );
    }

    void assertValueInfoEquals( ValueInfo v1, ValueInfo v2 ) {
        assertEquals( v1.getContentClass(), v2.getContentClass() );
        assertEquals( v1.getName(), v2.getName() );
        assertEquals( v1.getDescription(), v2.getDescription() );
        assertEquals( v1.getUCD(), v2.getUCD() );
        assertEquals( v1.getUnitString(), v2.getUnitString() );
        assertEquals( v1.isArray(), v2.isArray() );
        int[] shape1 = v1.getShape();
        int[] shape2 = v2.getShape();
        if ( shape1 == null && shape2 == null ) {
            // ok
        }
        else {
            int ndim = shape1.length;
            assertEquals( ndim, shape2.length );
            for ( int i = 0; i < ndim - 1; i++ ) {
                assertEquals( shape1[ i ], shape2[ i ] );
            }
            if ( ndim > 0 ) {
                int ld1 = shape1[ ndim - 1 ];
                int ld2 = shape2[ ndim - 1 ];
                assertTrue( ld1 == ld2 || ld1 < 0 || ld2 < 0 );
            }
        }
    }

    void assertValueInfoConsistent( ValueInfo v1, ValueInfo v2 ) {
        assertEquals( v1.getContentClass(), v2.getContentClass() );
        assertEquals( v1.getName(), v2.getName() );
        assertEquals( v1.getDescription(), v2.getDescription() );
        assertEquals( v1.getUCD(), v2.getUCD() );
        assertEquals( v1.getUnitString(), v2.getUnitString() );
        assertEquals( v1.getUtype(), v2.getUtype() );
        assertEquals( v1.isArray(), v2.isArray() );
        int[] s1 = v1.getShape();
        int[] s2 = v2.getShape();

        /* Last element of dims can be -1 if unknown. */
        if ( s1 == null ) {
            assertTrue( s2 == null );
        }
        else {
            int ndim = s1.length;
            assertEquals( ndim, s2.length );
            for ( int i = 0; i < ndim - 1; i++ ) {
                assertEquals( s1[ i ], s2[ i ] );
            }
            if ( ndim > 0 ) {
                int last1 = s1[ ndim - 1 ];
                int last2 = s2[ ndim - 1 ];
                assertTrue( last1 == last2 || last1 * last2 < 0 );
            }
        }
    }
        

    /**
     * Checks table invariants.  Any StarTable should be able to run
     * through these tests without errors.
     */
    public void checkStarTable( StarTable st ) throws IOException {
        Tables.checkTable( st );
        int ncol = st.getColumnCount();
        boolean isRandom = st.isRandom();
        int[] nels = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colinfo = st.getColumnInfo( icol );
            int[] dims = colinfo.getShape();
            if ( dims != null ) {
                int ndim = dims.length;
                assertTrue( dims.length > 0 );
                assertTrue( colinfo.getContentClass().isArray() );
                int nel = 1;
                for ( int i = 0; i < ndim; i++ ) {
                    nel *= dims[ i ];
                    assertTrue( dims[ i ] != 0 );
                    if ( i < ndim - 1 ) {
                        assertTrue( dims[ i ] > 0 );
                    }
                }
                nels[ icol ] = nel;
            }
        }
        long lrow = 0;
        RowSequence rseq1 = st.getRowSequence();
        RowSequence rseq2 = st.getRowSequence();
        RowSplittable rsplit = st.getRowSplittable();
        RowAccess racc = isRandom ? st.getRowAccess() : null;
        while ( rseq1.next() ) {
            assertTrue( rseq2.next() );
            assertTrue( rsplit.next() );
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object[] row = rseq1.getRow();
                assertArrayEquals( row, rseq2.getRow() );
                assertArrayEquals( row, rsplit.getRow() );
                Object cell = row[ icol ];
                if ( isRandom ) {
                    assertScalarOrArrayEquals( cell, st.getCell( lrow, icol ) );
                    racc.setRowIndex( lrow );
                    assertScalarOrArrayEquals( cell, racc.getCell( icol ) );
                    assertArrayEquals( row, racc.getRow() );
                }
                assertScalarOrArrayEquals( cell, rseq1.getCell( icol ) );
                assertScalarOrArrayEquals( cell, rseq2.getCell( icol ) );
                assertScalarOrArrayEquals( cell, rsplit.getCell( icol ) );
                if ( cell != null && cell.getClass().isArray() ) {
                    int nel = Array.getLength( cell );
                    if ( nels[ icol ] < 0 ) {
                        assertTrue( nel % nels[ icol ] == 0 );
                    }
                    else {
                        assertEquals( nels[ icol ], nel );
                    }
                }
                if ( cell != null ) {
                    Class c1 = st.getColumnInfo( icol ).getContentClass();
                    Class c2 = cell.getClass();
                    assertTrue( "Matching " + c2 + " with " + c1,
                                c1.isAssignableFrom( c2 ) );
                }
            }
            lrow++;
        }
        rseq1.close();
        rseq2.close();
        rsplit.close();
        if ( racc != null ) {
            racc.close();
        }
    }

    static Object blankToNull( Object o ) {
        return Tables.isBlank( o ) ? null : o;
    }
             
    static class ValueInfoComparator implements Comparator {
        public int compare( Object o1, Object o2 ) {
            return ((ValueInfo) o1).getName()
                  .compareTo( ((ValueInfo) o2).getName() );
        }
    }

    static class DescribedValueComparator
            implements Comparator<DescribedValue> {
        public int compare( DescribedValue dval1, DescribedValue dval2 ) {
            return dval1.getInfo().getName()
                  .compareTo( dval2.getInfo().getName() );
        }
    }

    File getTempFile( String suffix ) throws IOException {
        File f;
        File markDir = new File( "/mbt/scratch/table" );
        if ( markDir.exists() && markDir.canWrite() ) {
            f = new File( markDir, suffix );
        }
        else {
            f = File.createTempFile( "table", suffix );
            f.deleteOnExit();
        }
        return f;
    }

}
