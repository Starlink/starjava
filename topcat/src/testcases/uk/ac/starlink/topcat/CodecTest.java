package uk.ac.starlink.topcat;

import java.awt.HeadlessException;
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

    private final TopcatCodec codec_;
    private final ControlWindow controlWindow_;
    private final StarTableWriter tWriter_;
    private final TableBuilder tReader_;

    public CodecTest() {
        Logger.getLogger( "uk.ac.starlink.table" ).setLevel( Level.WARNING );
        Logger.getLogger( "org.astrogrid.samp" ).setLevel( Level.WARNING );
        Logger.getLogger( "uk.ac.starlink.topcat" ).setLevel( Level.WARNING );
        codec_ = TopcatCodec.getInstance();
        ControlWindow cwin;
        try {
            cwin = ControlWindow.getInstance();
        }
        catch ( HeadlessException e ) {
            cwin = null;
        }
        controlWindow_ = cwin;
        tWriter_ = new VOTableWriter();
        tReader_ = new VOTableBuilder();
    }

    public void testCodec() throws IOException {
        if ( controlWindow_ == null ) {
            System.err.println( "headless: no codec test" );
        }
        StarTable[] demoTables = Driver.getDemoTables();
        int nt = demoTables.length;
        TopcatModel[] tcModels = new TopcatModel[ nt ];
        for ( int i = 0; i < nt; i++ ) {
            tcModels[ i ] =
                new TopcatModel( demoTables[ i ], "demo" + ( i + 1 ),
                                 controlWindow_ );
            assertEqualTopcatModels( tcModels[ i ],
                                     roundTrip( tcModels[ i ] ) );
        }

        TopcatModel tcModel = tcModels[ 1 ];
        BitSet mask = new BitSet();
        mask.set( 0, 10 );
        RowSubset tenSet = new BitsRowSubset( "Ten", mask );
        tcModel.addSubset( RowSubset.NONE );
        tcModel.addSubset( tenSet );
        tcModel.applySubset( tenSet );
        tcModel.sortBy( new SortOrder( tcModel.getColumnModel()
                                              .getColumn( 2 ) ), true );
        tcModel.getRowSendModel()
               .setSelected( ! tcModel.getRowSendModel().isSelected() );
        TableColumnModel colModel = tcModel.getColumnModel();
        colModel.moveColumn( 0, 4 );
        colModel.moveColumn( 5, 2 );
        colModel.removeColumn( colModel.getColumn( 0 ) );
        colModel.removeColumn( colModel
                              .getColumn( colModel .getColumnCount() - 1 ) );
        TopcatModel tcModel1 = roundTrip( tcModel );
        assertEqualTopcatModels( tcModel, tcModel1 );
    }

    private void assertEqualTopcatModels( TopcatModel tc0, TopcatModel tc1 )
            throws IOException {
        assertEquals( tc0.getLabel(), tc1.getLabel() );
        StarTable dm0 = tc0.getDataModel();
        StarTable dm1 = tc1.getDataModel();
        StarTable ap0 = tc0.getApparentStarTable();
        StarTable ap1 = tc1.getApparentStarTable();
        assertVOTableEquals( dm0, dm1, false );
        assertVOTableEquals( ap0, ap1, false );
        assertSubsetsEquals( tc0.getSubsets(), tc1.getSubsets(),
                             dm0.getRowCount() );
        assertEquals( tc0.getRowSendModel().isSelected(),
                      tc1.getRowSendModel().isSelected() );
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

    private TopcatModel roundTrip( TopcatModel tcModel ) throws IOException {
        StarTable oTable = codec_.encode( tcModel );
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        tWriter_.writeStarTable( oTable, bout );
        bout.close();
        String loc = "buf";
        StarTable iTable = tReader_
           .makeStarTable( new ByteArrayDataSource( loc, bout.toByteArray() ),
                           true, StoragePolicy.PREFER_MEMORY );
        return codec_.decode( iTable, loc, controlWindow_ );
    }
}
