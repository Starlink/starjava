package uk.ac.starlink.table;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.fits.BintableStarTable;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.table.storage.DiskRowStore;
import uk.ac.starlink.table.storage.ListRowStore;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableWriter;

public class FormatsTest extends TestCase {

    private static final DefaultValueInfo DRINK_INFO =
        new DefaultValueInfo( "Drink", String.class, "Favourite drink" );
    private static final DefaultValueInfo NAMES_INFO =
        new DefaultValueInfo( "Names", String[].class, "Triple of names" );
    private static final DefaultValueInfo MATRIX_INFO =
        new DefaultValueInfo( "Matrix", int[].class, "2xN matrix" );
    private static final DefaultValueInfo SIZE_INFO =
        new DefaultValueInfo( "Size", Double.class, null );

    static {
        MATRIX_INFO.setShape( new int[] { 2, -1 } );
        NAMES_INFO.setShape( new int[] { 3 } );
        NAMES_INFO.setElementSize( 24 );
        DRINK_INFO.setUCD( "ID_VERSION" );
        SIZE_INFO.setUnitString( "Area of Wales" );
    }

    private StarTable table;

    public FormatsTest( String name ) {
        super( name );
    }

    public void setUp() {
        AutoStarTable ctable = new AutoStarTable( 200 );
        table = ctable;
        ctable.setName( "Test Table" );

        List params = new ArrayList();
        params.add( new DescribedValue( NAMES_INFO,
                                        new String[] { "Test", "Table", "x" } ) );
        params.add( new DescribedValue( DRINK_INFO, "Cider" ) );
        params.add( new DescribedValue( MATRIX_INFO, 
                                        new int[] { 4, 5, } ) );
        ctable.setParameters( params );

        ctable.addColumn( new ColumnData( DRINK_INFO ) {
            public Object readValue( long irow ) {
                return "Drink " + irow;
            }
        } );
        ctable.addColumn( new ColumnData( NAMES_INFO ) {
            String[] first = { "Ichabod", "Candice", "Rowland" };
            String[] middle = { "Beauchamp", "Burbidge", "Milburn", "X" };
            String[] last = { "Percy", "Neville", "Stanley", "Fitzalan",
                              "Courtenay" };
            public Object readValue( long lrow ) {
                int irow = (int) lrow;
                return new String[] { first[ irow % first.length ],
                                      middle[ irow % middle.length ],
                                      last[ irow % last.length ], };
            }
        } );

        Class[] ptypes = { byte.class, short.class, int.class, float.class,
                           double.class, };
        for ( int i = 0; i < ptypes.length; i++ ) {
            final Class ptype = ptypes[ i ];
            ColumnInfo colinfo = new ColumnInfo( MATRIX_INFO );
            colinfo.setContentClass( Array.newInstance( ptype, 0 ).getClass() );
            colinfo.setName( ptype.getName() + "_matrix" );
            ctable.addColumn( colinfo );
            ColumnInfo colinfo2 = new ColumnInfo( colinfo );
            colinfo2.setName( ptype.getName() + "_vector" );
            final int nel = ( i + 2 ) % 4 + 2;
            colinfo2.setShape( new int[] { nel } );
            final int bs = i;
            ctable.addColumn( colinfo2 );
        }

        Class[] stypes = { Byte.class, Short.class, Integer.class,
                           Float.class, Double.class, String.class };
        for ( int i = 0; i < stypes.length; i++ ) {
            final int itype = i;
            final Class stype = stypes[ i ];
            String name = stype.getName().replaceFirst( "java.lang.", "" );
            ColumnInfo colinfo = new ColumnInfo( name + "Scalar", stype,
                                                 name + " scalar data" );
            ctable.addColumn( colinfo );
        }
    }

    public void testIdentity() throws IOException {
        checkStarTable( table );
        assertTableEquals( table, table );
    }

    public void testWrapper() throws IOException {
        assertTableEquals( table, new WrapperStarTable( table ) );
    }

    public void testStorage() throws IOException {
        exerciseRowStore( new ListRowStore() );
        exerciseRowStore( new DiskRowStore() );
    }

    private void exerciseRowStore( RowStore store ) throws IOException {
        try {
            store.getStarTable();
            fail();
        }
        catch ( IllegalStateException e ) {
        }
        Tables.streamStarTable( table, store );
        StarTable t2 = store.getStarTable();
        try {
            store.acceptRow( new Object[ 1 ] );
            fail();
        }
        catch ( IllegalStateException e ) {
        }
        assertTrue( t2.isRandom() );
        checkStarTable( t2 );
        assertTableEquals( table, t2 );
    }

    public void testOutput() throws IOException {
        int i = 0;
        StarTableFactory sfact = new StarTableFactory();
        List handlers = new StarTableOutput().getHandlers();
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            StarTableWriter handler = (StarTableWriter) it.next();
            String fmt = handler.getFormatName().toLowerCase();
            fmt.replaceAll( "^[a-zA-Z0-9]", "" );
            if ( fmt.length() > 4 ) {
                fmt = fmt.substring( 0, 4 );
            }
            File loc = getTempFile( "t" + ( ++i ) + "." + fmt );
            handler.writeStarTable( table, loc.toString() );

            if ( handler instanceof FitsTableWriter ) {
                DataSource datsrc = new FileDataSource( loc );
                StarTable st2 = sfact.makeStarTable( datsrc );
                checkStarTable( st2 );
            }
        }
        String[] knownFormats = new String[] {
            "fits",
            "fits-basic",
            "votable-tabledata",
            "votable-binary-inline",
            "votable-fits-href",
            "votable-binary-href",
            "votable-fits-inline",
            "text",
            "ascii",
            "html",
            "html-element",
            "latex",
            "latex-document",
            "mirage",
        };
        String[] gotFormats = new String[ handlers.size() ];
        for ( int j = 0; j < handlers.size(); j++ ) {
            gotFormats[ j ] = ((StarTableWriter) handlers.get( j ))
                             .getFormatName().toLowerCase();
        }
        assertArrayEquals( knownFormats, gotFormats );
    }

    public void testVOTable() throws IOException, SAXException {
        VOTableWriter vohandler = new VOTableWriter();

        assertEquals( DataFormat.TABLEDATA, vohandler.getDataFormat() );
        assertTrue( vohandler.getInline() );
        exerciseVOTableWriter( vohandler, getTempFile( "in-td.vot" ), false );

        vohandler.setDataFormat( DataFormat.FITS );
        vohandler.setInline( false );
        assertTrue( ! vohandler.getInline() );
        exerciseVOTableWriter( vohandler, getTempFile( "ex-fits.vot" ), true );

        vohandler.setDataFormat( DataFormat.FITS );
        vohandler.setInline( true );
        assertEquals( DataFormat.FITS, vohandler.getDataFormat() );
        exerciseVOTableWriter( vohandler, getTempFile( "in-fits.vot" ), true );

        vohandler.setDataFormat( DataFormat.BINARY );
        vohandler.setInline( false );
        assertEquals( DataFormat.BINARY, vohandler.getDataFormat() );
        assertTrue( ! vohandler.getInline() );
        exerciseVOTableWriter( vohandler, getTempFile( "ex-bin.vot" ), true );

        vohandler.setInline( true );
        exerciseVOTableWriter( vohandler, getTempFile( "in-bin.vot" ), true );
    }

    public void exerciseVOTableWriter( VOTableWriter writer, File loc,
                                       boolean squashNulls )
            throws IOException, SAXException {
        StarTable t1 = table;
        writer.setDoctypeDeclaration( 
            "<!DOCTYPE VOTABLE SYSTEM " +
            "'http://cdsweb.u-strasbg.fr/xml/VOTable.dtd'>" );
        writer.writeStarTable( t1, loc.toString() );
        assertValidXML( new InputSource( loc.toString() ) );
        StarTable t2 = new StarTableFactory()
                      .makeStarTable( loc.toString() );
        assertTrue( t2 instanceof VOStarTable );
        checkStarTable( t2 );

        assertVOTableEquals( t1, t2, squashNulls );

        String docDecl = writer.getDoctypeDeclaration();
        File tmpFile = File.createTempFile( "decltest", ".xml" );
        tmpFile.deleteOnExit();
        String tmpBase = tmpFile.toString();
        tmpBase = tmpBase.substring( 0, tmpBase.lastIndexOf( ".xml" ) );
        new File( tmpBase + "-data.fits" ).deleteOnExit();
        new File( tmpBase + "-data.bin" ).deleteOnExit();
        writer.setDoctypeDeclaration( 
            "<!DOCTYPE VOTABLE SYSTEM 'http://nowhere/VOTable.dtd'>" );
        writer.writeStarTable( t1, tmpFile.toString() );
        assertValidXML( new InputSource( tmpFile.toString() ) );

        tmpFile.delete();
        writer.setDoctypeDeclaration( docDecl );
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
        while ( rseq1.hasNext() ) {
            assertTrue( rseq1.hasNext() );
            assertTrue( rseq2.hasNext() );
            rseq1.next();
            rseq2.next();
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
                    cell1 = new Short( ((Number) cell1).shortValue() );
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
        assertTrue( ! rseq1.hasNext() );
        assertTrue( ! rseq2.hasNext() );
    }

    public void testFits() throws IOException {
        StarTableWriter writer = new FitsTableWriter();
        File loc = getTempFile( "t.fits" );
        StarTable t1 = table;
        writer.writeStarTable( t1, loc.toString() );
        StarTable t2 = new StarTableFactory()
                      .makeStarTable( loc.toString() );
        assertTrue( t2 instanceof BintableStarTable );
        checkStarTable( t2 );

        assertFitsTableEquals( t1, t2 );
        assertFitsTableEquals( t1, t2 );
        StarTable t3 = new StarTableFactory( false )
                      .makeStarTable( loc.toURI().toURL().toString() );
        assertTrue( t3 instanceof BintableStarTable );
        assertTrue( ! t3.isRandom() );
        checkStarTable( t3 );
        assertFitsTableEquals( t1, t3 );
        assertFitsTableEquals( t1, t3 );

        StarTable t4 = new StarTableFactory( true )
                      .makeStarTable( "file:" + loc );
        assertTrue( t4 instanceof BintableStarTable );
        assertTrue( ! t4.isRandom() );
        checkStarTable( t4 );
        assertFitsTableEquals( t1, t4 );

        // assertTableEquals( t3, t4 );

        String name = "Dobbin";
        ((BintableStarTable) t2).setName( name );
        ((BintableStarTable) t3).setName( name );
        assertTableEquals( t2, t3 );
    }

    private void assertFitsTableEquals( StarTable t1, StarTable t2 )
            throws IOException {
        int ncol = t1.getColumnCount();
        assertEquals( ncol, t2.getColumnCount() );
        long nrow = t1.getRowCount();
        assertEquals( nrow, t2.getRowCount() );

        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo c1 = t1.getColumnInfo( icol );
            ColumnInfo c2 = t2.getColumnInfo( icol );
            assertEquals( c1.getName().toUpperCase(),
                          c2.getName().toUpperCase() );
            assertEquals( c1.getUnitString(),
                          c2.getUnitString() );
            Class clazz1 = c1.getContentClass();
            Class clazz2 = c2.getContentClass();
            if ( clazz1 != byte[].class && clazz1 != Byte.class ) {
                assertEquals( clazz1, clazz2 );
            }
            int[] dims1 = c1.getShape();
            int[] dims2 = c2.getShape();
            if ( dims1 == null ) {
                assertNull( dims2 );
            }
            else {
                int ndim = dims1.length;
                assertEquals( ndim, dims2.length );
                for ( int i = 0; i < ndim - 1; i++ ) {
                    assertEquals( dims1[ i ], dims2[ i ] );
                }
            }
        }

        RowSequence rseq1 = t1.getRowSequence();
        RowSequence rseq2 = t2.getRowSequence();
        while ( rseq1.hasNext() ) {
            assertTrue( rseq2.hasNext() );
            rseq1.next();
            rseq2.next();
            long lrow = rseq1.getRowIndex();
            assertEquals( lrow, rseq2.getRowIndex() );
            Object[] row1 = rseq1.getRow();
            Object[] row2 = rseq2.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object val1 = row1[ icol ];
                Object val2 = row2[ icol ];
                if ( val1 instanceof Number && val2 instanceof Number ) {
                    Number v1 = (Number) val1;
                    Number v2 = (Number) val2;
                    assertEquals( v1.intValue(), v2.intValue() );
                }
                else if ( val1 instanceof byte[] && val2 instanceof short[] ) {
                    byte[] v1 = (byte[]) val1;
                    short[] v2 = (short[]) val2;
                    int nel = v1.length;
                    assertEquals( nel, v2.length );
                    for ( int i = 0; i < nel; i++ ) {
                        assertEquals( (int) v1[ i ], (int) v2[ i ] );
                    }
                }
                else if ( val1 == null ) {
                    // represented as some null-like value
                }
                else {
                    assertScalarOrArrayEquals( val1, val2 );
                }
            }
        }
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
        while ( rseq1.hasNext() ) {
            assertTrue( rseq1.hasNext() );
            assertTrue( rseq2.hasNext() );
            rseq1.next();
            rseq2.next();
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
        assertTrue( ! rseq1.hasNext() );
        assertTrue( ! rseq2.hasNext() );
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

    void assertValueSetEquals( List dvals1, List dvals2 ) {
        if ( dvals1 == null && dvals2 == null ) {
            return;
        }
        int nparam = dvals1.size();
        Comparator sorter = new DescribedValueComparator();
        Collections.sort( dvals1, sorter );
        Collections.sort( dvals2, sorter );
        assertEquals( nparam, dvals2.size() );
        for ( int i = 0; i < nparam; i++ ) {
            DescribedValue dv1 = (DescribedValue) dvals1.get( i );
            DescribedValue dv2 = (DescribedValue) dvals2.get( i );
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
        assertArrayEquals( v1.getShape(), v2.getShape() );
    }

    void assertValueInfoConsistent( ValueInfo v1, ValueInfo v2 ) {
        assertEquals( v1.getContentClass(), v2.getContentClass() );
        assertEquals( v1.getName(), v2.getName() );
        assertEquals( v1.getDescription(), v2.getDescription() );
        assertEquals( v1.getUCD(), v2.getUCD() );
        assertEquals( v1.getUnitString(), v2.getUnitString() );
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
        for ( RowSequence rseq = st.getRowSequence(); rseq.hasNext(); ) {
            rseq.next();
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object[] row = rseq.getRow();
                Object cell = row[ icol ];
                if ( isRandom ) {
                    assertScalarOrArrayEquals( cell, 
                                  st.getCell( rseq.getRowIndex(), icol ) );
                }
                assertScalarOrArrayEquals( cell, rseq.getCell( icol ) );
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

    static class DescribedValueComparator implements Comparator {
        public int compare( Object o1, Object o2 ) {
            return ((DescribedValue) o1).getInfo().getName()
                  .compareTo( ((DescribedValue) o2).getInfo().getName() );
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
