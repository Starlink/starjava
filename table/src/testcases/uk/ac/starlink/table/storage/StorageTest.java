package uk.ac.starlink.table.storage;

import java.io.IOException;
import junit.framework.AssertionFailedError;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.FormatsTest;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.TestCase;

/*
 * Note more rigorous general tests of storage classes are performed in
 * uk.ac.starlink.table package tests.  These are more specific ones.
 */
public class StorageTest extends TestCase {

    FormatsTest fTest_;

    public StorageTest( String name ) {
        super( name );
    }

    public void setUp() {
        fTest_ = new FormatsTest( "tame test" );
    }

    public void testStorage() throws IOException {
        int nrow = 100;
        ColumnStarTable t1 = ColumnStarTable.makeTableWithRows( (long) nrow );
        ColumnStarTable t2 = ColumnStarTable.makeTableWithRows( (long) nrow );
        ColumnStarTable t3 = ColumnStarTable.makeTableWithRows( (long) nrow );
        Object[] numData = {
            new byte[ nrow ],
            new short[ nrow ],
            new int[ nrow ],
            new long[ nrow ],
            new float[ nrow ],
            new double[ nrow ],
        };
        int nNum = numData.length;
        String[] strData = new String[ nrow ];
        fillCycle( strData, 
                   new String[] { "red", "green", "blue", "aquamarine", 
                                  "sky-blue-pink", "", null, } );
        for ( int i = 0; i < nNum; i++ ) {
            Object array = numData[ i ];
            fillRandom( array, -100, 100 );
            ColumnData colData = ArrayColumn
                                .makeColumn( "col" + ( i + 1 ), array );
            t1.addColumn( colData );
            t2.addColumn( colData );
            t3.addColumn( colData );
        }
        ColumnInfo varStrCol = 
           new ColumnInfo( "varStrings", String.class, null );
        ColumnInfo fixStrCol =
           new ColumnInfo( "fixStrings", String.class, null );
        fixStrCol.setElementSize( "aquamarine".length() );
        t2.addColumn( ArrayColumn.makeColumn( fixStrCol, strData ) );
        t3.addColumn( ArrayColumn.makeColumn( varStrCol, strData ) );

        DiskRowStore dst1 = (DiskRowStore) fillStore( new DiskRowStore(), t1 );
        DiskRowStore dst2 = (DiskRowStore) fillStore( new DiskRowStore(), t2 );
        DiskRowStore dst3 = (DiskRowStore) fillStore( new DiskRowStore(), t3 );
        ListRowStore mst1 = (ListRowStore) fillStore( new ListRowStore(), t1 );
        ListRowStore mst2 = (ListRowStore) fillStore( new ListRowStore(), t2 );
        ListRowStore mst3 = (ListRowStore) fillStore( new ListRowStore(), t3 );

        assertTrue( fixedRows( dst1 ) );
        assertTrue( fixedRows( dst2 ) );
        assertTrue( ! fixedRows( dst3 ) );

        StarTable dt1 = dst1.getStarTable();
        StarTable dt2 = dst2.getStarTable();
        StarTable dt3 = dst3.getStarTable();
        StarTable mt1 = mst1.getStarTable();
        StarTable mt2 = mst2.getStarTable();
        StarTable mt3 = mst3.getStarTable();
        checkTables( t1, dt1, mt1 );
        checkTables( t2, dt2, mt2 );
        checkTables( t3, dt3, mt3 );

        fTest_.assertTableEquals( t1, dt1 );
        fTest_.assertTableEquals( t1, mt1 );
        fTest_.assertTableEquals( t3, dt3 );
        fTest_.assertTableEquals( t3, mt3 );

        String err;
        try {
            fTest_.assertTableEquals( t2, dt2 );
            fTest_.assertTableEquals( t2, mt2 );
            err = "";
        }
        catch ( AssertionFailedError e ) {
            err = e.getMessage();
        }
        assertTrue( err.indexOf( "sky-blue" ) > 0 );
    }

    public void testUnserializable() throws IOException {
        ColumnStarTable table = ColumnStarTable.makeTableWithRows( 2 );
        table.addColumn( ArrayColumn.makeColumn( "objects", new Object[ 2 ] ) );
        try {
            new DiskRowStore().acceptMetadata( table );
            fail();
        }
        catch ( TableFormatException e ) {
        }
    }

    private void checkTables( StarTable tab1, StarTable tab2, StarTable tab3 )
            throws IOException {
        assertTrue( tab1.isRandom() );
        assertTrue( tab2.isRandom() );
        assertTrue( tab3.isRandom() );
        fTest_.checkStarTable( tab1 );
        fTest_.checkStarTable( tab2 );
        fTest_.checkStarTable( tab3 );
    }

    private RowStore fillStore( RowStore store, StarTable table )
            throws IOException {
        Tables.streamStarTable( table, store );
        return store;
    }

    private boolean fixedRows( DiskRowStore dstore ) {
        return dstore.getOffsets().getClass().getName().indexOf( "Fixed" ) > 0;
    }
}
