package uk.ac.starlink.votable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import junit.framework.TestCase;
import uk.ac.starlink.fits.ColFitsTableBuilder;
import uk.ac.starlink.fits.FitsTableBuilder;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.votable.FitsPlusTableBuilder;

public class FitsPlusTest extends TestCase {

    ColumnStarTable table_;
    String longdesc =
        "My goodness this is a long-winded description for a column without " +
        "any very interesting content";

    ColumnInfo COL1_INFO = new ColumnInfo( "col1", Integer.class, longdesc );
    ValueInfo AUTHOR_INFO = 
        new DefaultValueInfo( "Author", String.class, "Writer of the table" );
    DescribedValue authorParam = new DescribedValue( AUTHOR_INFO, "Beauchamp" );

    public FitsPlusTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void setUp() {
        table_ = ColumnStarTable.makeTableWithRows( 2 );
        ArrayColumn acol = ArrayColumn.makeColumn( COL1_INFO, 2 );
        table_.addColumn( acol );
        acol.storeValue( 0, Integer.valueOf( 23 ) );
        acol.storeValue( 1, Integer.valueOf( 32 ) );
        table_.getParameters().add( authorParam );
    }

    public void testFitsPlus() throws IOException {
        UnifiedFitsTableWriter basicWriter = new UnifiedFitsTableWriter();
        basicWriter.setPrimaryType( UnifiedFitsTableWriter.BASIC_PRIMARY_TYPE );
        exerciseFormats( new UnifiedFitsTableWriter(), basicWriter,
                         new FitsPlusTableBuilder(), new FitsTableBuilder() );
    }

    public void testColFitsPlus() throws IOException {
        UnifiedFitsTableWriter colfitsPlusWriter = new UnifiedFitsTableWriter();
        colfitsPlusWriter.setColfits( true );
        UnifiedFitsTableWriter colfitsWriter = new UnifiedFitsTableWriter();
        colfitsWriter.setColfits( true );
        colfitsWriter.setPrimaryType(UnifiedFitsTableWriter.BASIC_PRIMARY_TYPE);
        exerciseFormats( colfitsPlusWriter, colfitsWriter,
                         new ColFitsPlusTableBuilder(),
                         new ColFitsTableBuilder() );
    }

    public void exerciseFormats( StarTableWriter fpOut, StarTableWriter fOut,
                                 TableBuilder fpIn, TableBuilder fIn )
            throws IOException {
        {
            StarTable t2 = copyTable( table_, fpOut, fpIn );
            assertEquals( table_.getCell( 0, 0 ), t2.getCell( 0, 0 ) );
            assertEquals( authorParam.getValue(),
                t2.getParameterByName( AUTHOR_INFO.getName() ).getValue() );
            assertEquals( COL1_INFO.getDescription(),
                          t2.getColumnInfo( 0 ).getDescription() );
        }

        {
            StarTable t3 = copyTable( table_, fOut, fIn );
            assertEquals( table_.getCell( 0, 0 ), t3.getCell( 0, 0 ) );
            assertNull( t3.getParameterByName( AUTHOR_INFO.getName() ) );
            assertTrue( COL1_INFO.getDescription()
                       .startsWith( t3.getColumnInfo( 0 ).getDescription() ) );
            assertTrue( ! COL1_INFO.getDescription()
                         .equals( t3.getColumnInfo( 0 ).getDescription() ) );
        }

        {
            StarTable t4 = copyTable( table_, fpOut, fIn );
            assertEquals( table_.getCell( 0, 0 ), t4.getCell( 0, 0 ) );
            assertNull( t4.getParameterByName( AUTHOR_INFO.getName() ) );
            assertTrue( COL1_INFO.getDescription()
                       .startsWith( t4.getColumnInfo( 0 ).getDescription() ) );
            assertTrue( ! COL1_INFO.getDescription()
                         .equals( t4.getColumnInfo( 0 ).getDescription() ) );
        }
    }

    private StarTable copyTable( StarTable table, 
                                 StarTableWriter outie, TableBuilder innie ) 
           throws IOException {
        File tmp = File.createTempFile( "table", ".tmp" );
        tmp.deleteOnExit();
        outie.writeStarTable( table, new FileOutputStream( tmp ) );
        return innie.makeStarTable( new FileDataSource( tmp ), 
                                    true, StoragePolicy.PREFER_MEMORY );
    }
}
