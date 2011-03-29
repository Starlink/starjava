package uk.ac.starlink.table.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import junit.framework.AssertionFailedError;
import uk.ac.starlink.table.ArrayColumn;
import uk.ac.starlink.table.ByteStore;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.FormatsTest;
import uk.ac.starlink.table.RowStore;
import uk.ac.starlink.table.StoragePolicy;
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

    public void testPolicies() {
        assertEquals( StoragePolicy.PREFER_MEMORY, getPolicy( "memory" ) );
        assertEquals( StoragePolicy.PREFER_DISK, getPolicy( "disk" ) );
        assertEquals( StoragePolicy.SIDEWAYS, getPolicy( "sideways" ) );
        assertEquals( StoragePolicy.DISCARD, getPolicy( "discard" ) );
        assertEquals( StoragePolicy.ADAPTIVE, getPolicy( "adaptive" ) );

        assertEquals( "StoragePolicy.PREFER_MEMORY",
                       StoragePolicy.PREFER_MEMORY.toString() );
        assertEquals( "StoragePolicy.PREFER_DISK",
                       StoragePolicy.PREFER_DISK.toString() );
        assertEquals( "StoragePolicy.SIDEWAYS",
                       StoragePolicy.SIDEWAYS.toString() );
        assertEquals( "StoragePolicy.DISCARD",
                       StoragePolicy.DISCARD.toString() );
        assertEquals( "StoragePolicy.ADAPTIVE",
                       StoragePolicy.ADAPTIVE.toString() );

        assertTrue( StoragePolicy.PREFER_MEMORY.makeRowStore()
                    instanceof ListRowStore );
        assertTrue( StoragePolicy.PREFER_DISK.makeRowStore()
                    instanceof DiskRowStore );
        assertTrue( StoragePolicy.SIDEWAYS.makeRowStore()
                    instanceof SidewaysRowStore );
        assertTrue( StoragePolicy.DISCARD.makeRowStore()
                    instanceof DiscardRowStore );
        assertTrue( StoragePolicy.ADAPTIVE.makeRowStore()
                    instanceof ByteStoreRowStore );

        assertTrue( StoragePolicy.PREFER_MEMORY.makeByteStore()
                    instanceof MemoryByteStore );
        assertTrue( StoragePolicy.PREFER_DISK.makeByteStore()
                    instanceof FileByteStore );
        assertTrue( StoragePolicy.SIDEWAYS.makeByteStore()
                    instanceof FileByteStore );
        assertTrue( StoragePolicy.DISCARD.makeByteStore()
                    instanceof DiscardByteStore );
        assertTrue( StoragePolicy.ADAPTIVE.makeByteStore()
                    instanceof AdaptiveByteStore );
    }

    public StoragePolicy getPolicy( String policyName ) {
        String prop = "startable.storage";
        StoragePolicy.setDefaultPolicy( null );
        System.setProperty( prop, policyName );
        return StoragePolicy.getDefaultPolicy();
    }

    public void testRowStorage() throws IOException {
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
        SidewaysRowStore sst1 = (SidewaysRowStore)
                                fillStore( new SidewaysRowStore(), t1 );
        SidewaysRowStore sst2 = (SidewaysRowStore)
                                fillStore( new SidewaysRowStore(), t2 );
        SidewaysRowStore sst3 = (SidewaysRowStore)
                                fillStore( new SidewaysRowStore(), t3 );

        assertTrue( fixedRows( dst1 ) );
        assertTrue( fixedRows( dst2 ) );
        assertTrue( ! fixedRows( dst3 ) );

        StarTable dt1 = dst1.getStarTable();
        StarTable dt2 = dst2.getStarTable();
        StarTable dt3 = dst3.getStarTable();
        StarTable mt1 = mst1.getStarTable();
        StarTable mt2 = mst2.getStarTable();
        StarTable mt3 = mst3.getStarTable();
        StarTable st1 = sst1.getStarTable();
        StarTable st2 = sst2.getStarTable();
        StarTable st3 = sst3.getStarTable();
        checkTables( t1, dt1, mt1, st1 );
        checkTables( t2, dt2, mt2, st2 );
        checkTables( t3, dt3, mt3, st3 );

        fTest_.assertTableEquals( t1, dt1 );
        fTest_.assertTableEquals( t1, mt1 );
        fTest_.assertTableEquals( t1, st1 );
        fTest_.assertTableEquals( t3, dt3 );
        fTest_.assertTableEquals( t3, mt3 );
        fTest_.assertTableEquals( t3, st3 );

        String err;
        try {
            fTest_.assertTableEquals( t2, mt2 );
            fTest_.assertTableEquals( t2, dt2 );
            err = "";
        }
        catch ( AssertionFailedError e ) {
            err = e.getMessage();
        }
        assertTrue( err.indexOf( "sky-blue" ) > 0 );
    }

    public void testByteStorage() throws IOException {
        testByteStore( StoragePolicy.PREFER_MEMORY.makeByteStore() );
        testByteStore( StoragePolicy.PREFER_DISK.makeByteStore() );
        testByteStore( StoragePolicy.SIDEWAYS.makeByteStore() );
        testByteStore( StoragePolicy.ADAPTIVE.makeByteStore() );
        int[] limits = new int[] { 331, 332, 333, 334,
                                   998, 999, 1000, 1001,
                                   Integer.MAX_VALUE };
        testByteStore( new AdaptiveByteStore() );
        for ( int il = 0; il < limits.length; il++ ) {
            testByteStore( new AdaptiveByteStore( limits[ il ] ) );
        }
    }

    public void testLimitByteStore() throws IOException {
        StoragePolicy policy = StoragePolicy.PREFER_MEMORY;
        testByteStore( new LimitByteStore( policy.makeByteStore(), 65536 ) );
        ByteStore lbs = new LimitByteStore( policy.makeByteStore(), 16 );
        OutputStream lout = lbs.getOutputStream();
        lout.write( new byte[ 15 ] );
        assertEquals( 15, lbs.getLength() );
        try {
            lout.write( new byte[ 100 ] );
            fail();
        }
        catch ( IOException e ) {
        }
        assertEquals( 15, lbs.getLength() );
        try {
            lout.write( new byte[ 100 ], 10, 2 );
            fail();
        }
        catch ( IOException e ) {
        }
        assertEquals( 15, lbs.getLength() );
        lout.write( 99 );
        try {
            lout.write( 100 );
            fail();
        }
        catch ( IOException e ) {
        }
    }

    private void testByteStore( ByteStore bs ) throws IOException {
        byte[] buf = new byte[ 999 ];
        for ( int i = 0; i < buf.length; i++ ) {
            buf[ i ] = (byte) ( i % 100 );
        }
        byte[] buf1 = new byte[ 333 ];
        System.arraycopy( buf, 0, buf1, 0, 333 );
        OutputStream out = bs.getOutputStream();
        out.write( buf1 );
        assertEquals( 333, bs.getLength() );
        out.write( buf, 333, 333 );
        assertEquals( 666, bs.getLength() );
        for ( int i = 0; i < 333; i++ ) {
            assertEquals( 666 + i, bs.getLength() );
            out.write( buf[ 666 + i ] );
        }
        assertEquals( 999, bs.getLength() );
        out.flush();
        assertEquals( 999, bs.getLength() );
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bs.copy( bos );
        assertArrayEquals( buf, bos.toByteArray() );

        ByteBuffer[] bbufs = bs.toByteBuffers();
        int nbyte = 0;
        for ( int ib = 0; ib < bbufs.length; ib++ ) {
            assertEquals( 0, bbufs[ ib ].position() );
            nbyte += bbufs[ ib ].limit();
        }
        assertEquals( buf.length, nbyte );

        byte[] bbufcopy = new byte[ nbyte ];
        int ibyte = 0;
        for ( int ib = 0; ib < bbufs.length; ib++ ) {
            ByteBuffer bbuf = bbufs[ ib ];
            int lim = bbuf.limit();
            bbuf.get( bbufcopy, ibyte, lim );
            ibyte += lim;
        }
        assertEquals( ibyte, nbyte );
        assertArrayEquals( buf, bbufcopy );

        bs.close();
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
        try {
            new SidewaysRowStore().acceptMetadata( table );
            fail();
        }
        catch ( TableFormatException e ) {
        }
        try {
            new ByteStoreRowStore( new MemoryByteStore() )
                                  .acceptMetadata( table );
            fail();
        }
        catch ( TableFormatException e ) {
        }
    }

    private void checkTables( StarTable tab1, StarTable tab2, StarTable tab3,
                              StarTable tab4 )
            throws IOException {
        assertTrue( tab1.isRandom() );
        assertTrue( tab2.isRandom() );
        assertTrue( tab3.isRandom() );
        assertTrue( tab4.isRandom() );
        fTest_.checkStarTable( tab1 );
        fTest_.checkStarTable( tab2 );
        fTest_.checkStarTable( tab3 );
        fTest_.checkStarTable( tab4 );
    }

    private RowStore fillStore( RowStore store, StarTable table )
            throws IOException {
        Tables.streamStarTable( table, store );
        return store;
    }

    private boolean fixedRows( DiskRowStore dstore ) {
        return dstore.getOffsets().isFixed();
    }
}
