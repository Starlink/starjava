package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.ByteArrayDataSource;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.votable.VOTableBuilder;
import uk.ac.starlink.votable.VOTableWriter;

public class CodecTest extends TableCase {

    private final TopcatCodec[] allCodecs_;
    private final ControlWindow controlWindow_;
    private final StarTableWriter tWriter_;
    private final TableBuilder tReader_;

    public CodecTest() {
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        Logger.getLogger( "org.astrogrid.samp" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.topcat" ).setLevel( Level.WARNING );
        controlWindow_ = null;
        tWriter_ = new VOTableWriter();
        tReader_ = new VOTableBuilder();
        allCodecs_ = TopcatUtils.SESSION_DECODERS.clone();
    }

    public void testCodecs() throws IOException, CompilationException {
        for ( TopcatCodec codec : allCodecs_ ) {
            exerciseCodec( codec );
        }
    }

    private void exerciseCodec( TopcatCodec codec )
            throws IOException, CompilationException {
        StarTable[] demoTables = Driver.getDemoTables();
        int nt = demoTables.length;
        TopcatModel[] tcModels = new TopcatModel[ nt ];
        for ( int i = 0; i < nt; i++ ) {
            tcModels[ i ] =
                TopcatModel
               .createRawTopcatModel( demoTables[ i ], "demo" + ( i + 1 ),
                                      controlWindow_ );
            assertEqualTopcatModels( tcModels[ i ],
                                     roundTrip( codec, tcModels[ i ] ) );
        }

        TopcatModel tcModel = tcModels[ 1 ];
        long nrow = tcModel.getDataModel().getRowCount();
        BitSet mask = new BitSet();
        mask.set( 0, 10 );
        RowSubset tenSet = new BitsRowSubset( "Ten", mask );
        RowSubset notTenSet = new InverseRowSubset( tenSet );
        RowSubset quarterSet =
            new SyntheticRowSubset( "Quarter", tcModel, "$0 % 4 == 0" );
        RowSubset notQuarterSet = new InverseRowSubset( quarterSet );
        tcModel.addSubset( RowSubset.NONE );
        tcModel.addSubset( tenSet );
        tcModel.addSubset( notTenSet );
        tcModel.addSubset( quarterSet );
        tcModel.addSubset( notQuarterSet );
        assertEquals( 0, countSubset( RowSubset.NONE, nrow ) );
        assertEquals( 10, countSubset( tenSet, nrow ) );
        assertEquals( nrow - 10, countSubset( notTenSet, nrow ) );
        assertEquals( ( nrow + 1 ) / 4, countSubset( quarterSet, nrow ) );
        assertEquals( ( nrow + 1 ) * 3 / 4, countSubset( notQuarterSet, nrow ));

        RowSubset removed = (RowSubset) tcModel.getSubsets().remove( 3 );
        assertEquals( notTenSet, removed );
        tcModel.applySubset( tenSet );
        tcModel.sortBy( new SortOrder( tcModel.getColumnModel()
                                              .getColumn( 2 ) ), true );
        ColumnData addcol1 =
            new SyntheticColumn( tcModel,
                                 new ColumnInfo( "ix", Integer.class, null ),
                                 "(int)$0", Integer.class );
        tcModel.appendColumn( addcol1 );
        ColumnData addcol2 =
            new SyntheticColumn( tcModel,
                                 new ColumnInfo( "isOne", Boolean.class, null ),
                                 "ix==1", null );

        TableColumnModel colModel = tcModel.getColumnModel();
        colModel.moveColumn( 0, 4 );
        colModel.moveColumn( 5, 2 );
        colModel.removeColumn( colModel.getColumn( 0 ) );
        colModel.removeColumn( colModel
                              .getColumn( colModel .getColumnCount() - 1 ) );
        TopcatModel tcModel1 = roundTrip( codec, tcModel );
        assertEqualTopcatModels( tcModel, tcModel1 );
    }

    private void assertEqualTopcatModels( TopcatModel tc0, TopcatModel tc1 )
            throws IOException {
        assertEquals( tc0.getLabel(), tc1.getLabel() );
        assertEquals( tc0.getSelectedSubset().getName(),
                      tc1.getSelectedSubset().getName() );
        StarTable dm0 = tc0.getDataModel();
        StarTable dm1 = tc1.getDataModel();
        StarTable ap0 = tc0.getApparentStarTable();
        StarTable ap1 = tc1.getApparentStarTable();
        assertVOTableEquals( dm0, dm1, false );
        assertVOTableEquals( ap0, ap1, false );
        assertSubsetsEquals( tc0.getSubsets(), tc1.getSubsets(),
                             dm0.getRowCount() );
    }

    private void assertSubsetsEquals( List<RowSubset> subsets0,
                                      List<RowSubset> subsets1,
                                      long nrow ) {
        int nset = subsets0.size();
        assertEquals( nset, subsets1.size() );
        for ( int iset = 0; iset < nset; iset++ ) {
            RowSubset set0 = subsets0.get( iset );
            RowSubset set1 = subsets1.get( iset );
            assertEquals( set0.getName(), set1.getName() );
            for ( long irow = 0; irow < nrow; irow++ ) {
                assertTrue( set0.isIncluded( irow ) ==
                            set1.isIncluded( irow ) );
            }
        }
    }

    private TopcatModel roundTrip( TopcatCodec codec, TopcatModel tcModel )
                                   throws IOException {
        StarTable oTable = codec.encode( tcModel );
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        tWriter_.writeStarTable( oTable, bout );
        bout.close();
        String loc = "buf";
        StarTable iTable = tReader_
           .makeStarTable( new ByteArrayDataSource( loc, bout.toByteArray() ),
                           true, StoragePolicy.PREFER_MEMORY );
        for ( TopcatCodec aCodec : allCodecs_ ) {
            assertEquals( aCodec.isEncoded( iTable ), aCodec.equals( codec ) );
        }
        return codec.decode( iTable, loc, controlWindow_ );
    }

    private int countSubset( RowSubset rset, long nrow ) {
        int n = 0;
        for ( long i = 0; i < nrow; i++ ) {
            if ( rset.isIncluded( i ) ) {
                n++;
            }
        }
        return n;
    }
}
