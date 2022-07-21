package uk.ac.starlink.ttools.task;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import uk.ac.starlink.table.MultiTableBuilder;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.ttools.TableTestCase;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.votable.VOTableBuilder;

public class MultiCopyTest extends TableTestCase {

    final URL multiLoc_ = getClass().getResource( "multi.vot" );

    public MultiCopyTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        LogUtils.getLogger( "uk.ac.starlink.fits" ).setLevel( Level.WARNING );
    }

    public void testMulti() throws Exception {
        String[] ofmts = new String[] { "fits", "votable", };
        StarTableFactory tfact = new StarTableFactory();
        StarTable[] inTables =
            Tables.tableArray( new VOTableBuilder()
                              .makeStarTables( new URLDataSource( multiLoc_ ),
                                               StoragePolicy.PREFER_MEMORY ) );
        for ( int i = 0; i < ofmts.length; i++ ) {
            String ofmt = ofmts[ i ];
            File ofile = File.createTempFile( "tbl", "." + ofmt );
            ofile.deleteOnExit();
            MapEnvironment env = new MapEnvironment()
                                .setValue( "in", multiLoc_.toString() )
                                .setValue( "multi", "true" )
                                .setValue( "ofmt", ofmt )
                                .setValue( "out", ofile.toString() );
            new MultiCopy().createExecutable( env ).execute();
            StarTable[] tables =
                Tables.tableArray(
                    ((MultiTableBuilder) tfact.getTableBuilder( ofmt ))
                        .makeStarTables( new FileDataSource( ofile ),
                                         StoragePolicy.PREFER_MEMORY ) );
            int ntest = tables.length;
            if ( "fits".equals( ofmt ) ) {
                ntest--;
            }
            for ( int itab = 0; itab < ntest; itab++ ) {
                assertSameData( inTables[ itab ], tables[ itab ] );
            }

            MapEnvironment envN = new MapEnvironment()
                                 .setValue( "nin", "2" )
                                 .setValue( "in1", multiLoc_.toString() )
                                 .setValue( "in2", multiLoc_.toString() + "#1" )
                                 .setValue( "icmd1", "head 1" )
                                 .setValue( "icmd2", "tail 1" )
                                 .setValue( "out", ofile.toString() );
            new MultiCopyN().createExecutable( envN ).execute();
            StarTable[] tablesN =
                Tables.tableArray(
                    ((MultiTableBuilder) tfact.getTableBuilder( ofmt ))
                        .makeStarTables( new FileDataSource( ofile ),
                                         StoragePolicy.PREFER_MEMORY ) );
            assertEquals( 1, tablesN[ 0 ].getRowCount() );
            assertEquals( 1, tablesN[ 1 ].getRowCount() );
            assertArrayEquals( inTables[ 0 ].getRow( 0 ),
                               tablesN[ 0 ].getRow( 0 ) );
            assertArrayEquals( inTables[ 1 ]
                              .getRow( inTables[ 1 ].getRowCount() - 1 ),
                               tablesN[ 1 ].getRow( 0 ) );

            assertEquals( 3, tables.length );
        }
    }
}
