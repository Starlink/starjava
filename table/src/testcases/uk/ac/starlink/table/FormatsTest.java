package uk.ac.starlink.table;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.fits.BintableStarTable;
import uk.ac.starlink.fits.ColFitsTableWriter;
import uk.ac.starlink.fits.ColFitsTableBuilder;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.fits.VariableFitsTableWriter;
import uk.ac.starlink.table.storage.AdaptiveByteStore;
import uk.ac.starlink.table.storage.ByteStoreRowStore;
import uk.ac.starlink.table.storage.FileByteStore;
import uk.ac.starlink.table.storage.DiskRowStore;
import uk.ac.starlink.table.storage.ListRowStore;
import uk.ac.starlink.table.storage.MemoryByteStore;
import uk.ac.starlink.table.storage.SidewaysRowStore;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.AsciiTableWriter;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.table.formats.CsvTableWriter;
import uk.ac.starlink.table.formats.IpacTableBuilder;
import uk.ac.starlink.table.formats.IpacTableWriter;
import uk.ac.starlink.table.formats.TstTableBuilder;
import uk.ac.starlink.table.formats.TstTableWriter;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.ColFitsPlusTableBuilder;
import uk.ac.starlink.votable.ColFitsPlusTableWriter;
import uk.ac.starlink.votable.FitsPlusTableBuilder;
import uk.ac.starlink.votable.FitsPlusTableWriter;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.VOTableWriter;

public class FormatsTest extends TableCase {

    private static final DefaultValueInfo DRINK_INFO =
        new DefaultValueInfo( "Drink", String.class, "Favourite drink" );
    private static final DefaultValueInfo NAMES_INFO =
        new DefaultValueInfo( "Names", String[].class, "Triple of names" );
    private static final DefaultValueInfo MATRIX_INFO =
        new DefaultValueInfo( "Matrix", int[].class, "2xN matrix" );
    private static final DefaultValueInfo SIZE_INFO =
        new DefaultValueInfo( "Size", Double.class, null );
    private StarTableOutput sto = new StarTableOutput();
    private static final String FUNNY_UNITS = "\"'<a&b>'\"";

    static {
        MATRIX_INFO.setShape( new int[] { 2, -1 } );
        NAMES_INFO.setShape( new int[] { 3 } );
        NAMES_INFO.setElementSize( 24 );
        DRINK_INFO.setUCD( "ID_VERSION" );
        SIZE_INFO.setUnitString( "Area of Wales" );
        NAMES_INFO.setUnitString( FUNNY_UNITS );

        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.SEVERE );
        Logger.getLogger( "uk.ac.starlink.votable" ).setLevel( Level.WARNING );
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

        Class[] ptypes = { byte.class, short.class, int.class, long.class,
                           float.class, double.class, };
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

        Class[] stypes = { Byte.class, Short.class, Integer.class, Long.class,
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
        exerciseRowStore( new SidewaysRowStore() );
        exerciseRowStore( new ByteStoreRowStore( new MemoryByteStore() ) );
        exerciseRowStore( new ByteStoreRowStore( new FileByteStore() ) );
        exerciseRowStore( new ByteStoreRowStore(
                              new AdaptiveByteStore( 10 ) ) );
        exerciseRowStore( new ByteStoreRowStore(
                              new AdaptiveByteStore( 1024*1024*1024 ) ) );
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

    public void testFactory() {
        String[] defaultFormats = new String[] {
            "FITS-plus",
            "colfits-plus",
            "colfits-basic",
            "FITS",
            "VOTable",
        };
        String[] knownFormats = new String[] {
            "FITS-plus",
            "colfits-plus",
            "colfits-basic",
            "FITS",
            "VOTable",
            "ASCII",
            "CSV",
            "TST",
            "IPAC",
            "WDC",
        };
        StarTableFactory factory = new StarTableFactory();
        assertEquals( Arrays.asList( knownFormats ),
                      factory.getKnownFormats() );
        List factDefaultBuilders = new ArrayList();
        for ( Iterator it = factory.getDefaultBuilders().iterator();
              it.hasNext(); ) {
            String fname = ((TableBuilder) it.next()).getFormatName();
            factDefaultBuilders.add( fname );
        }
        assertEquals( Arrays.asList( defaultFormats ), factDefaultBuilders );
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
            handler.writeStarTable( table, loc.toString(), sto );

            if ( handler instanceof FitsTableWriter ) {
                DataSource datsrc = new FileDataSource( loc );
                StarTable st2 = sfact.makeStarTable( datsrc );
                checkStarTable( st2 );
            }
        }
        String[] knownFormats = new String[] {
            "fits",
            "fits-plus",
            "fits-basic",
            "fits-var",
            "colfits-plus",
            "colfits-basic",
            "votable-tabledata",
            "votable-binary-inline",
            "votable-fits-href",
            "votable-binary-href",
            "votable-fits-inline",
            "text",
            "ascii",
            "csv",
            "csv-noheader",
            "ipac",
            "tst",
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
            "<!DOCTYPE VOTABLE SYSTEM 'some/where/VOTable.dtd'>" );
        writer.writeStarTable( t1, loc.toString(), sto );
        assertValidXML( new InputSource( loc.toString() ) );
        StarTable t2 = new StarTableFactory()
                      .makeStarTable( loc.toString() );
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
        writer.writeStarTable( t1, tmpFile.toString(), sto );
        assertValidXML( new InputSource( tmpFile.toString() ) );

        tmpFile.delete();
        writer.setDoctypeDeclaration( docDecl );
    }

    public void testFits() throws IOException {
        StarTableWriter writer = new FitsTableWriter();
        File loc = getTempFile( "t.fits" );
        StarTable t1 = table;
        writer.writeStarTable( t1, loc.toString(), sto );
        StarTable t2 = new StarTableFactory()
                      .makeStarTable( loc.toString() );
        assertTrue( t2 instanceof BintableStarTable );
        checkStarTable( t2 );

        assertFitsTableEquals( t1, t2, false, false );
        assertFitsTableEquals( t1, t2, false, false );
        StarTable t3 = new StarTableFactory( false )
                      .makeStarTable( loc.toURI().toURL().toString() );
        assertTrue( t3 instanceof BintableStarTable );
        assertTrue( ! t3.isRandom() );
        checkStarTable( t3 );
        assertFitsTableEquals( t1, t3, false, true );
        assertFitsTableEquals( t1, t3, false, true );

        StarTable t4 = new StarTableFactory( false )
                      .makeStarTable( "file:" + loc );
        assertTrue( t4 instanceof BintableStarTable );
        assertTrue( ! t4.isRandom() );
        checkStarTable( t4 );
        assertFitsTableEquals( t1, t4, false, true );

        StarTable t5 = new StarTableFactory( true )
                      .makeStarTable( "file:" + loc );
        assertTrue( t5.isRandom() );
        checkStarTable( t5 );
        assertFitsTableEquals( t1, t5, false, false );

        // assertTableEquals( t3, t4 );

        String name = "Dobbin";
        t2.setName( name );
        t3.setName( name );
        assertEquals( "Dobbin", t2.getName() );
        assertEquals( "Dobbin", t3.getName() );
        assertTableEquals( t2, t3 );
    }

    public void testVarFits() throws IOException {
        exerciseVarFits( false );
        exerciseVarFits( true );
    }

    private void exerciseVarFits( boolean isLong ) throws IOException {
        File loc = getTempFile( "tv.fits" );
        StarTable t1 = table;
        new VariableFitsTableWriter( isLong, true )
           .writeStarTable( t1, loc.toString(), sto );
        StarTable t2 = new StarTableFactory().makeStarTable( loc.toString() );
        int ncol = t2.getColumnCount();
        int nvar = 0;
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo c1 = t1.getColumnInfo( ic );
            ColumnInfo c2 = t2.getColumnInfo( ic );
            int[] shape1 = c1.getShape();
            int[] shape2 = c2.getShape();
            String tform1 =
                (String) c1.getAuxDatumValue( BintableStarTable.TFORM_INFO,
                                              String.class );
            String tform2 =
                (String) c2.getAuxDatumValue( BintableStarTable.TFORM_INFO,
                                              String.class );
            if ( shape1 != null && shape1[ shape1.length - 1 ] < 0 ) {
                nvar++;
                assertTrue( shape2[ shape2.length - 1 ] < 0 );
                assertNull( tform1 );
                assertEquals( isLong ? 'Q' : 'P', tform2.charAt( 0 ) );
            }
        }
        assertTrue( nvar > 1 );
    }

    public void testReadWrite() throws IOException {
        exerciseReadWrite( new FitsTableWriter(),
                           new FitsTableBuilder(), "fits" );
        exerciseReadWrite( new FitsPlusTableWriter(),
                           new FitsPlusTableBuilder(), "fits" );
        exerciseReadWrite( new ColFitsTableWriter(),
                           new ColFitsTableBuilder(), "fits" );
        exerciseReadWrite( new ColFitsPlusTableWriter(),
                           new ColFitsPlusTableBuilder(), "votable" );
        exerciseReadWrite( new VariableFitsTableWriter( false, true ),
                           new FitsTableBuilder(), "fitsv" );
        exerciseReadWrite( new VariableFitsTableWriter( false, false ),
                           new FitsTableBuilder(), "fitsv" );
        exerciseReadWrite( new VariableFitsTableWriter( true, true ),
                           new FitsTableBuilder(), "fitsv" );
        exerciseReadWrite( new VariableFitsTableWriter( true, false ),
                           new FitsTableBuilder(), "fitsv" );
        exerciseReadWrite( new VOTableWriter(),
                           new VOTableBuilder(), "votable" );
        exerciseReadWrite( new AsciiTableWriter(),
                           new AsciiTableBuilder(), "text" );
        exerciseReadWrite( new CsvTableWriter( true ),
                           new CsvTableBuilder(), "text" );
        exerciseReadWrite( new IpacTableWriter(),
                           new IpacTableBuilder(), "ipac" );
        exerciseReadWrite( new TstTableWriter(),
                           new TstTableBuilder(), "text" );
                    
    }

    public void exerciseReadWrite( StarTableWriter writer,
                                   TableBuilder reader, String equalMethod )
            throws IOException {
        File loc = getTempFile( "trw." + writer.getFormatName().toLowerCase() );
        StarTable t1 = table;
        writer.writeStarTable( t1, loc.toString(), sto );
        for ( int i = 0; i < 2; i++ ) {
            final boolean isSeq;
            switch ( i ) {
                case 0:
                    isSeq = false;
                    break;
                case 1:
                    isSeq = true;
                    break;
                default:
                    throw new AssertionError();
            }
            if ( !( isSeq && ( reader instanceof ColFitsTableBuilder ||
                               reader instanceof ColFitsPlusTableBuilder ) ) ) {
                DataSource datsrc =
                    isSeq ? (DataSource) new URLDataSource( loc.toURL() )
                          : (DataSource) new FileDataSource( loc );
                StarTable t2 =
                    reader.makeStarTable( datsrc, true,
                                          StoragePolicy.PREFER_MEMORY );;
                checkStarTable( t2 );
                assertTableEqualsMethod( t1, t2, equalMethod, isSeq );
            }
        }
    }

    private void assertTableEqualsMethod( StarTable t1, StarTable t2,
                                          String equalMethod, boolean isSeq )
            throws IOException {
        if ( "fits".equals( equalMethod ) ) {
            assertFitsTableEquals( t1, t2, false, isSeq );
        }
        else if ( "fitsv".equals( equalMethod ) ) {
            assertFitsTableEquals( t1, t2, true, isSeq );
        }
        else if ( "votable".equals( equalMethod ) ) {
            assertVOTableEquals( t1, t2, false );
        }
        else if ( "text".equals( equalMethod ) ) {
            assertTextTableEquals( t1, t2 );
        }
        else if ( "ipac".equals( equalMethod ) ) {
            assertIpacTableEquals( t1, t2 );
        }
        else if ( "exact".equals( equalMethod ) ) {
            assertTableEquals( t1, t2 );
        }
        else if ( "none".equals( equalMethod ) ) {
        }
        else {
            fail();
        }
    }

    public void testMultiReadWrite() throws IOException {
        exerciseMultiReadWrite( new FitsTableWriter(),
                                new FitsTableBuilder(), "fits" );
        exerciseMultiReadWrite( new VOTableWriter(),
                                new VOTableBuilder(), "votable" );
        exerciseMultiReadWrite( new FitsPlusTableWriter(),
                                new FitsPlusTableBuilder(), "fits" );
    }

    public void exerciseMultiReadWrite( MultiStarTableWriter writer,
                                        MultiTableBuilder reader,
                                        String equalMethod )
            throws IOException {
        File loc = getTempFile( "mtrw."
                              + writer.getFormatName().toLowerCase() );
        StarTable t1 = table;
        AutoStarTable t2 = new AutoStarTable( 10 );
        t2.setName( "Other" );
        t2.addColumn( new ColumnInfo( "ci", Integer.class, null ) );
        t2.addColumn( new ColumnInfo( "cs", String.class, null ) );
        StarTable[] touts = new StarTable[] { t1, t2, t2, };

        OutputStream out =
            new BufferedOutputStream( new FileOutputStream( loc ) );
        writer.writeStarTables( Tables.arrayTableSequence( touts ), out );
        out.close();
        StarTable[] tins =
            Tables.tableArray( reader
                              .makeStarTables( new FileDataSource( loc ),
                                               StoragePolicy.PREFER_MEMORY ) );
        assertEquals( touts.length, tins.length );
        for ( int jseq = 0; jseq < 2; jseq++ ) {
            for ( int i = 0; i < touts.length; i++ ) {
                assertTableEqualsMethod( touts[ i ], tins[ i ], equalMethod,
                                         jseq == 0 );
            }
        }
    }

    public void testColFits() throws IOException {
        StarTableWriter writer = new ColFitsTableWriter();
        File loc = getTempFile( "t.colfits" );
        StarTable t1 = table;
        writer.writeStarTable( t1, loc.toString(), sto );
        StarTable t2 = new ColFitsTableBuilder()
                      .makeStarTable( new FileDataSource( loc ), false, null );
        assertEquals( "uk.ac.starlink.fits.ColFitsStarTable",
                      t2.getClass().getName() );
        checkStarTable( t2 );
        assertFitsTableEquals( t1, t2, false, false );
    }

    private void assertTextTableEquals( StarTable t1, StarTable t2 )
            throws IOException {
        assertTextTableEquals( t1, t2, false );
    }
   
    private void assertIpacTableEquals( StarTable t1, StarTable t2 )
            throws IOException {
        assertTextTableEquals( t1, t2, true );
    }

    private void assertTextTableEquals( StarTable t1, StarTable t2, 
                                        boolean trimString )
            throws IOException {
        int ncol = t1.getColumnCount();
        assertEquals( ncol, t2.getColumnCount() );
        char[] types = new char[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info1 = t1.getColumnInfo( icol );
            ColumnInfo info2 = t2.getColumnInfo( icol );
            assertEquals( info1.getName(), info2.getName() );
            if ( info1.getContentClass().getComponentType() == null ) {
                Class clazz1 = info1.getContentClass();
                Class clazz2 = info2.getContentClass();
                if ( clazz1.isAssignableFrom( Number.class ) ) {
                    assertTrue( clazz2.isAssignableFrom( Number.class ) );
                    types[ icol ] = 'n';
                }
                else if ( clazz2 == String.class ) {
                    assertEquals( String.class, clazz2 );
                    types[ icol ] = 's';
                }
                else {
                    types[ icol ] = 'o';
                }
            }
            else {
                types[ icol ] = 'a';
            }
        }

        RowSequence rseq1 = t1.getRowSequence();
        RowSequence rseq2 = t2.getRowSequence();
        try {
            while ( rseq1.next() ) {
                assertTrue( rseq2.next() );
                Object[] row1 = rseq1.getRow();
                Object[] row2 = rseq2.getRow();
                assertEquals( ncol, row1.length );
                assertEquals( ncol, row2.length );
                for ( int icol = 0; icol < ncol; icol++ ) {
                    Object val1 = row1[ icol ];
                    Object val2 = row2[ icol ];
                    if ( Tables.isBlank( val1 ) ) {
                        assertTrue( Tables.isBlank( val2 ) );
                    }
                    else {
                        assertTrue( ! Tables.isBlank( val2 ) );
                        switch ( types[ icol ] ) {
                            case 'n':
                                assertEquals( ((Number) val1).doubleValue(),
                                              ((Number) val2).doubleValue() );
                                break;
                            case 's':
                                String s1 = (String) val1;
                                String s2 = (String) val2;
                                if ( trimString ) {
                                    s1 = s1.trim();
                                    s2 = s2.trim();
                                }
                                if ( s1.length() > 24 ) {
                                    assertEquals( s1.substring( 0, 24 ),
                                                  s2.substring( 0, 24 ) );
                                }
                                else {
                                    assertEquals( s1, s2 );
                                }
                                break;
                            case 'o':
                            case 'a':
                                break;
                            default:
                                fail();
                        }
                    }
                }
            }
        }
        finally {
            rseq1.close();
            rseq2.close();
        }
    }

    private void assertFitsTableEquals( StarTable t1, StarTable t2,
                                        boolean isVar, boolean isSeq )
            throws IOException {
        int ncol = t1.getColumnCount();
        assertEquals( ncol, t2.getColumnCount() );
        long nrow = t1.getRowCount();
        assertEquals( nrow, t2.getRowCount() );
        assertEquals( t1.getName(), t2.getName() );

        boolean[] badcols = new boolean[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo c1 = t1.getColumnInfo( icol );
            ColumnInfo c2 = t2.getColumnInfo( icol );
            assertEquals( c1.getName().toUpperCase(),
                          c2.getName().toUpperCase() );
            assertEquals( c1.getUnitString(),
                          c2.getUnitString() );
            int[] dims1 = c1.getShape();
            int[] dims2 = c2.getShape();
            if ( dims1 == null ) {
                assertNull( dims2 );
            }
            else {
                if ( isVar ) {
                    if ( isSeq ) {
                        if ( dims1[ dims1.length - 1 ] < 0 ) {
                            badcols[ icol ] = true;
                        }
                    }
                    else {
                        assertTrue( ( dims1[ dims1.length - 1 ] < 0 )
                                 == ( dims2[ dims2.length - 1 ] < 0 ) );
                    }
                }
                else {
                    int ndim = dims1.length;
                    assertEquals( ndim, dims2.length );
                    for ( int i = 0; i < ndim - 1; i++ ) {
                        assertEquals( dims1[ i ], dims2[ i ] );
                    }
                }
            }
            Class clazz1 = c1.getContentClass();
            Class clazz2 = c2.getContentClass();
            if ( ! badcols[ icol ] &&
                 clazz1 != byte[].class && clazz1 != Byte.class ) {
                assertEquals( clazz1, clazz2 );
            }
        }

        RowSequence rseq1 = t1.getRowSequence();
        RowSequence rseq2 = t2.getRowSequence();
        while ( rseq1.next() ) {
            assertTrue( rseq2.next() );
            Object[] row1 = rseq1.getRow();
            Object[] row2 = rseq2.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object val1 = row1[ icol ];
                Object val2 = row2[ icol ];
                if ( ! badcols[ icol ] ) {
                    if ( val1 instanceof Number && val2 instanceof Number ) {
                        Number v1 = (Number) val1;
                        Number v2 = (Number) val2;
                        assertEquals( v1.intValue(), v2.intValue() );
                    }
                    else if ( val1 instanceof byte[] &&
                              val2 instanceof short[] ) {
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
        rseq1.close();
        rseq2.close();
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
        for ( RowSequence rseq = st.getRowSequence(); rseq.next(); ) {
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object[] row = rseq.getRow();
                Object cell = row[ icol ];
                if ( isRandom ) {
                    assertScalarOrArrayEquals( cell, st.getCell( lrow, icol ) );
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
            lrow++;
        }
    }

}
