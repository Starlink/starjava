package uk.ac.starlink.table;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.AssertionFailedError;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.ecsv.EcsvTableBuilder;
import uk.ac.starlink.ecsv.EcsvTableWriter;
import uk.ac.starlink.feather.FeatherTableBuilder;
import uk.ac.starlink.feather.FeatherTableWriter;
import uk.ac.starlink.fits.AbstractFitsTableWriter;
import uk.ac.starlink.fits.AbstractWideFits;
import uk.ac.starlink.fits.BintableStarTable;
import uk.ac.starlink.fits.ColFitsTableWriter;
import uk.ac.starlink.fits.ColFitsTableBuilder;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.fits.FitsTableWriter;
import uk.ac.starlink.fits.HealpixFitsTableWriter;
import uk.ac.starlink.fits.VariableFitsTableWriter;
import uk.ac.starlink.fits.WideFits;
import uk.ac.starlink.parquet.ParquetTableBuilder;
import uk.ac.starlink.parquet.ParquetTableWriter;
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
import uk.ac.starlink.table.gui.TableSaveChooser;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.IntList;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.ColFitsPlusTableBuilder;
import uk.ac.starlink.votable.ColFitsPlusTableWriter;
import uk.ac.starlink.votable.FitsPlusTableBuilder;
import uk.ac.starlink.votable.FitsPlusTableWriter;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.VOTableVersion;
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
    private static final DescribedValue UBYTE_AUXDATUM =
        new DescribedValue( Tables.UBYTE_FLAG_INFO, Boolean.TRUE );
    private static final WideFits[] wides_ = {
          null,
          WideFits.DEFAULT,
          AbstractWideFits.createHierarchWideFits( 9 ),
    };

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
        Logger.getLogger( "uk.ac.starlink.feather" ).setLevel( Level.SEVERE );
        Logger.getLogger( "uk.ac.starlink.ecsv" ).setLevel( Level.SEVERE );
        Logger.getLogger( "uk.ac.starlink.parquet" ).setLevel( Level.WARNING );
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
                                        new String[] { "Test", "Table", "x" }));
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

        Class[] ptypes = { byte.class, short.class, short.class, int.class,
                           long.class, float.class, double.class, };
        for ( int i = 0; i < ptypes.length; i++ ) {
            final Class ptype = ptypes[ i ];
            String pname = ptype.getName();
            ColumnInfo colinfo = new ColumnInfo( MATRIX_INFO );
            if ( i == 1 ) {
                assertEquals( short.class, ptype );
                pname = "ubyte";
                colinfo.setAuxDatum( UBYTE_AUXDATUM );
            }
            colinfo.setContentClass( Array.newInstance( ptype, 0 ).getClass() );
            colinfo.setName( pname + "_matrix" );
            ctable.addColumn( colinfo );
            ColumnInfo colinfo2 = new ColumnInfo( colinfo );
            colinfo2.setName( pname + "_vector" );
            final int nel = ( i + 2 ) % 4 + 2;
            colinfo2.setShape( new int[] { nel } );
            final int bs = i;
            ctable.addColumn( colinfo2 );
        }

        Class[] stypes = { Byte.class, Short.class, Short.class, Integer.class,
                           Long.class, Float.class, Double.class,
                           String.class };
        for ( int i = 0; i < stypes.length; i++ ) {
            final int itype = i;
            final Class stype = stypes[ i ];
            String name = stype.getName().replaceFirst( "java.lang.", "" );
            ColumnInfo colinfo = new ColumnInfo( name + "Scalar", stype,
                                                 name + " scalar data" );
            if ( i == 1 ) {
                assertEquals( Short.class, stype );
                colinfo.setAuxDatum( UBYTE_AUXDATUM );
                colinfo.setName( "ubyteScalar" );
                colinfo.setDescription( "Unsigned byte scalar data" );
            }
            ctable.addColumn( colinfo );
        }
    }

    public void testTransferable() {
        assertNotNull( new StarTableOutput().getTransferWriter() );
    }

    public void testIdentity() throws IOException {
        checkStarTable( table );
        assertTableEquals( table, table );
    }

    public void testWrapper() throws IOException {
        assertTableEquals( table, new WrapperStarTable( table ) );
    }

    public void testHandlerNames() throws TableFormatException {
        StarTableOutput tout = new StarTableOutput();
        String[] fnames = new String[] {
            "fits-basic", "fits-plus", "fits-var", "fits-healpix",
            "colfits-basic", "colfits-plus",
            "votable", "ecsv", "feather", "text", "ascii", "csv",
            "ipac", "tst", "html", "latex", "mirage",
        };
        for ( String fname : fnames ) {
            assertNotNull( tout.getHandler( fname ) );
            assertNotNull( tout.getHandler( fname + "()" ) );
        }

        StarTableFactory tfact = new StarTableFactory();
        String[] bnames = new String[] {
            "fits", "colfits-basic", "colfits-plus",
            "votable", "cdf", "ecsv", "parquet", "feather",
        };
        for ( String bname : bnames ) {
            assertNotNull( tfact.getTableBuilder( bname ) );
            assertNotNull( tfact.getTableBuilder( bname + "()" ) );
        }
    }

    public void testLegacyOutputHandlers() throws TableFormatException {
        StarTableOutput sto = new StarTableOutput();
        for ( String hname :
              StarTableOutput.createLegacyHandlerMap().keySet() ) {
            assertNotNull( hname, sto.getHandler( hname ) );
        }
    }

    public void testGuiOutputHandlers() throws TableFormatException {
        StarTableOutput sto = new StarTableOutput();
        for ( String hname : TableSaveChooser.getExtraWriterNames( false ) ) {
            assertNotNull( hname, sto.getHandler( hname ) );
        }
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
            "CDF",
            "ECSV",
            "PDS4",
            "MRT",
            "parquet",
            "feather",
            "GBIN",
        };
        String[] knownFormats = new String[] {
            "FITS-plus",
            "colfits-plus",
            "colfits-basic",
            "FITS",
            "VOTable",
            "CDF",
            "ECSV",
            "PDS4",
            "MRT",
            "parquet",
            "feather",
            "GBIN",
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
            boolean isGeneric = ! ( handler instanceof HealpixFitsTableWriter );
            if ( isGeneric ) {
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
        }
        String[] knownFormats = new String[] {
            "fits-plus",
            "fits-basic",
            "fits-var",
            "fits-healpix",
            "colfits-plus",
            "colfits-basic",
            "votable",
            "ecsv",
            "parquet",
            "feather",
            "text",
            "ascii",
            "csv",
            "ipac",
            "tst",
            "html",
            "latex",
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
        for ( VOTableVersion vers :
              VOTableVersion.getKnownVersions().values() ) {
            exerciseVOTableVersion( vers );
        }

        StarTableOutput sto = new StarTableOutput();
        StarTableFactory tfact = new StarTableFactory( false );
        StarTable t1 = table;
        for ( String config :
              new String[] {
                  "format=BINARY2,encoding=UTF-16",
              } ) {
            VOTableWriter writer =
                (VOTableWriter) sto.getHandler( "votable(" + config + ")" );
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            writer.writeStarTable( t1, bout );
            StarTable t2 =
                tfact
               .makeStarTable( new ByteArrayInputStream( bout.toByteArray() ),
                               new VOTableBuilder() );
            assertVOTableEquals( t1, t2, false );
        }
    }

    public void exerciseVOTableVersion( VOTableVersion vers )
            throws IOException, SAXException {
        VOTableWriter vohandler =
            new VOTableWriter( DataFormat.TABLEDATA, true, vers );

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

        if ( vers.allowBinary2() ) {
            vohandler.setDataFormat( DataFormat.BINARY2 );
            vohandler.setInline( true );
            assertEquals( DataFormat.BINARY2, vohandler.getDataFormat() );
            assertTrue( vohandler.getInline() );
            exerciseVOTableWriter( vohandler, getTempFile( "in-bin2.vot" ),
                                   false );
        }
    }

    public void exerciseVOTableWriter( VOTableWriter writer, File loc,
                                       boolean squashNulls )
            throws IOException, SAXException {
        StarTable t1 = table;
        writer.writeStarTable( t1, loc.toString(), sto );
        StarTable t2 = new StarTableFactory()
                      .makeStarTable( loc.toString() );
        checkStarTable( t2 );
        assertVOTableEquals( t1, t2, squashNulls );
    }

    public void testFits() throws IOException {
        for ( WideFits wide : wides_ ) {
            exerciseFits( wide );
        }
    }

    private void exerciseFits( WideFits wide ) throws IOException {
        FitsTableWriter writer = new FitsTableWriter();
        writer.setAllowSignedByte( true );
        writer.setWide( wide );
        File loc = getTempFile( "t.fits" );
        StarTable t1 = table;
        writer.writeStarTable( t1, loc.toString(), sto );
        StarTable t2 = new FitsTableBuilder( wide )
                      .makeStarTable( new FileDataSource( loc ), true,
                                      StoragePolicy.PREFER_MEMORY );
        assertTrue( t2 instanceof BintableStarTable );
        checkStarTable( t2 );

        assertFitsTableEquals( t1, t2, false, false );
        assertFitsTableEquals( t1, t2, false, false );
        if ( wide == null || wide == WideFits.DEFAULT ) {
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
    }

    public void testVarFits() throws IOException {
        for ( WideFits wide : wides_ ) {
            exerciseVarFits( false, wide );
            exerciseVarFits( true, wide );
        }
    }

    private void exerciseVarFits( boolean isLong, WideFits wide )
            throws IOException {
        File loc = getTempFile( "tv.fits" );
        StarTable t1 = table;
        VariableFitsTableWriter vWriter =
            createVariableFitsTableWriter( isLong );
        vWriter.setAllowSignedByte( true );
        vWriter.setWide( wide );
        vWriter.writeStarTable( t1, loc.toString(), sto );
        StarTable t2 = new FitsTableBuilder( wide )
                      .makeStarTable( new FileDataSource( loc ), true,
                                      StoragePolicy.PREFER_MEMORY );
        int ncol = t2.getColumnCount();
        int nvar = 0;
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo c1 = t1.getColumnInfo( ic );
            ColumnInfo c2 = t2.getColumnInfo( ic );
            int[] shape1 = c1.getShape();
            int[] shape2 = c2.getShape();
            String tform1 = c1.getAuxDatumValue( BintableStarTable.TFORM_INFO,
                                                 String.class );
            String tform2 = c2.getAuxDatumValue( BintableStarTable.TFORM_INFO,
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

        exerciseFitsReadWrite( () -> new FitsTableWriter(),
                               wide -> new FitsTableBuilder( wide ), "fits" );
        exerciseFitsReadWrite( () -> new ColFitsTableWriter(),
                               wide -> new ColFitsTableBuilder( wide ), "fits");
        exerciseFitsReadWrite( () -> createVariableFitsTableWriter( false ),
                               wide -> new FitsTableBuilder( wide ), "fitsv" );
        exerciseFitsReadWrite( () -> createVariableFitsTableWriter( true ),
                               wide -> new FitsTableBuilder( wide ), "fitsv" );

        exerciseReadWrite( new FitsPlusTableWriter(),
                           new FitsPlusTableBuilder( null ), "votable" );
        exerciseReadWrite( new ColFitsPlusTableWriter(),
                           new ColFitsPlusTableBuilder( null ), "votable" );

        exerciseReadWrite( new VOTableWriter(),
                           new VOTableBuilder(), "votable" );
        exerciseReadWrite( EcsvTableWriter.SPACE_WRITER,
                           new EcsvTableBuilder(), "ecsv" );
        exerciseReadWrite( EcsvTableWriter.COMMA_WRITER,
                           new EcsvTableBuilder(), "ecsv" );

        ParquetTableBuilder parquetBuilder = new ParquetTableBuilder();
        ParquetTableWriter parquetWriter = new ParquetTableWriter();
        parquetBuilder.setCacheCols( Boolean.FALSE );
        parquetWriter.setGroupArray( false );
        exerciseReadWrite( parquetWriter, parquetBuilder, "parquet");
        parquetBuilder.setCacheCols( Boolean.TRUE );
        parquetWriter.setGroupArray( true );
        exerciseReadWrite( parquetWriter, parquetBuilder, "parquet");

        exerciseReadWrite(
            new FeatherTableWriter( false, StoragePolicy.PREFER_MEMORY ),
            new FeatherTableBuilder(), "feather" );
        exerciseReadWrite(
            new FeatherTableWriter( true, StoragePolicy.PREFER_MEMORY ),
            new FeatherTableBuilder(), "feather" );
        exerciseReadWrite( new AsciiTableWriter(),
                           new AsciiTableBuilder(), "text" );
        exerciseReadWrite( new CsvTableWriter( true ),
                           new CsvTableBuilder(), "text" );
        exerciseReadWrite( new IpacTableWriter(),
                           new IpacTableBuilder(), "ipac" );
        exerciseReadWrite( new TstTableWriter(),
                           new TstTableBuilder(), "text" );
    }

    private void exerciseFitsReadWrite(
                Supplier<AbstractFitsTableWriter> writerSupplier,
                Function<WideFits,TableBuilder> builderFunction,
                String equalMethod )
            throws IOException {
        for ( WideFits wide : wides_ ) {
            TableBuilder builder = builderFunction.apply( wide );
            for ( int ib = 0; ib < 2; ib++ ) {
                boolean allowSignedByte = ib == 0;
                AbstractFitsTableWriter writer = writerSupplier.get();
                writer.setWide( wide );
                writer.setAllowSignedByte( allowSignedByte );
                exerciseReadWrite( writer, builder, equalMethod );
            }
        }
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
            DataSource datsrc =
                isSeq ? (DataSource) new URLDataSource( loc.toURI().toURL() )
                      : (DataSource) new FileDataSource( loc );
            StarTable t2 =
                reader.makeStarTable( datsrc, true,
                                      StoragePolicy.PREFER_MEMORY );;
            checkStarTable( t2 );
            assertTableEqualsMethod( t1, t2, equalMethod, isSeq );
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
        else if ( "ecsv".equals( equalMethod ) ) {
            assertEcsvTableEquals( t1, t2 );
        }
        else if ( "parquet".equals( equalMethod ) ) {
            assertParquetTableEquals( t1, t2 );
        }
        else if ( "feather".equals( equalMethod ) ) {
            assertFeatherTableEquals( t1, t2 );
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
        for ( WideFits wide : wides_ ) {
            exerciseColFits( wide );
        }
    }

    private void exerciseColFits( WideFits wide ) throws IOException {
        ColFitsTableWriter writer = new ColFitsTableWriter();
        writer.setWide( wide );
        File loc = getTempFile( "t.colfits" );
        StarTable t1 = table;
        writer.writeStarTable( t1, loc.toString(), sto );
        StarTable t2 = new ColFitsTableBuilder( wide )
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

    private void assertEcsvTableEquals( StarTable t1, StarTable t2 )
            throws IOException {
        StarTable t1a = new MetaCopyStarTable( t1 );
        int ncol = t1a.getColumnCount();
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo cinfo = t1a.getColumnInfo( ic );
            int[] shape = cinfo.getShape();
            if ( shape != null &&
                 shape.length > 1 &&
                 shape[ shape.length - 1 ] < 0 ) {
                cinfo.setShape( new int[] { -1 } );
            }
        }
        for ( int ic = 0; ic < ncol; ic++ ) {
            assertValueInfoEquals( t1a.getColumnInfo( ic ),
                                   t2.getColumnInfo( ic ) );
        }
        assertRowSequenceEquals( t1a, t2 );
    }

    private void assertParquetTableEquals( StarTable t1, StarTable t2 )
            throws IOException {
        int nc = t1.getColumnCount();
        assertEquals( nc, t2.getColumnCount() );
        for ( int ic = 0; ic < nc; ic++ ) {
            ColumnInfo cinfo1 = t1.getColumnInfo( ic );
            ColumnInfo cinfo2 = t2.getColumnInfo( ic );
            assertEquals( cinfo1.getName(), cinfo2.getName() );
            assertEquals( cinfo1.getContentClass(), cinfo2.getContentClass() );
        }
        assertRowSequenceEquals( t1, t2 );
    }

    private void assertFeatherTableEquals( StarTable t1, StarTable t2 )
            throws IOException {
        IntList icols = new IntList();
        int nc = t1.getColumnCount();
        for ( int ic = 0; ic < nc; ic++ ) {
            Class<?> clazz = t1.getColumnInfo( ic ).getContentClass();
            if ( clazz.equals( byte[].class ) ||
                 clazz.getComponentType() == null ) { 
                icols.add( ic );
            }
        }
        StarTable t1a = new ColumnPermutedStarTable( t1, icols.toIntArray() );
        int ncol = t1a.getColumnCount();
        assertEquals( ncol, t2.getColumnCount() );
        assertEquals( t1a.getName(), t2.getName() );
        assertEquals( t1a.getURL() + "", t2.getURL() + "" );
        for ( int ic = 0; ic < ncol; ic++ ) {
            assertValueInfoEquals( t1a.getColumnInfo( ic ),
                                   t2.getColumnInfo( ic ) );
        }
        assertRowSequenceEquals( t1a, t2 );
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
        Object[] rrow = null;
        for ( RowSequence rseq = st.getRowSequence(); rseq.next(); ) {
            if ( isRandom ) {
                rrow = st.getRow( lrow );
            }
            Object[] row = rseq.getRow();
            for ( int icol = 0; icol < ncol; icol++ ) {
                Object cell = row[ icol ];
                if ( isRandom ) {
                    assertScalarOrArrayEquals( cell, st.getCell( lrow, icol ) );
                    for ( int ic = 0; ic < ncol; ic++ ) {
                        assertScalarOrArrayEquals( cell, rrow[ icol ] );
                    }
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

    private static VariableFitsTableWriter
            createVariableFitsTableWriter( boolean longIndexing ) {
        VariableFitsTableWriter vWriter = new VariableFitsTableWriter();
        vWriter.setStoragePolicy( StoragePolicy.PREFER_MEMORY );
        vWriter.setLongIndexing( Boolean.valueOf( longIndexing ) );
        return vWriter;
    }

    /**
     * Adjusts a table so that, as far as possible, any variable-length
     * multi-dimensional arrays are described in the column metadata
     * as fixed-length multi-dimensional arrays.
     * This is a utility function to assist with testing formats like ECSV
     * that don't cope with variable-length multi-dimensional array-valued
     * columns.
     *
     * @param   table   input table
     * @return   output table
     */
    private static StarTable fixArraySizes( StarTable table )
            throws IOException {
        table = new MetaCopyStarTable( table );
        int ncol = table.getColumnCount();
        int[] nels = new int[ ncol ];
        RowSequence rseq = table.getRowSequence();
        ColumnInfo[] cinfos = Tables.getColumnInfos( table );
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();
            for ( int ic = 0; ic < ncol; ic++ ) {
                if ( cinfos[ ic ].getShape() != null ) {
                    Object cell = row[ ic ];
                    if ( cell != null ) {
                        int nel = Array.getLength( cell );
                        if ( nels[ ic ] == -1 ) {
                            // no good
                        }
                        else if ( nels[ ic ] == 0 ) {
                            nels[ ic ] = nel;
                        }
                        else if ( nel != nels[ ic ] ) {
                            nels[ ic ] = -1;
                        }
                    }
                }
            }
        }
        rseq.close();
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo cinfo = cinfos[ ic ];
            int[] shape0 = cinfo.getShape();
            if ( shape0 != null &&
                 shape0.length > 1 &&
                 shape0[ shape0.length - 1 ] < 0 ) {
                int nd0 = shape0.length;
                int[] shape1;
                if ( nels[ ic ] >= 1 ) {
                    shape1 = new int[ nd0 ];
                    int nel = 1;
                    for ( int id = 0; id < nd0 - 1; id++ ) {
                        nel *= shape0[ id ];
                        shape1[ id ] = shape0[ id ];
                    }
                    shape1[ nd0 - 1 ] = nels[ ic ] / nel;
                }
                else {
                    shape1 = new int[] { -1 };
                }
                cinfo.setShape( shape1 );
            }
        }
        return table;
    }
}
