package uk.ac.starlink.topcat.join;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.table.join.LinkSet;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.RowLink;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TupleSelector;

/**
 * MatchSpec for performing matches between pairs of tables.
 *
 * @author   Mark Taylor
 * @since    8 Sep 2005
 */
public class PairMatchSpec extends MatchSpec {

    private final MatchEngine engine_;
    private final TupleSelector[] tupleSelectors_;
    private final PairModeSelector pairModeSelector_;
    private final JoinSelector joinSelector_;
    private StarTable result_;
    private RowSubset matchSubset_;
    private int pairCount_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.join" );

    /**
     * Constructs a new PairMatchSpec.
     *
     * @param  engine  match algorithm object
     */
    public PairMatchSpec( MatchEngine engine ) {
        engine_ = engine;

        Box main = Box.createVerticalBox();
        add( main );

        /* Set up table/column selector panels. */
        tupleSelectors_ = new TupleSelector[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            TupleSelector selector =
                new TupleSelector( engine.getTupleInfos() );
            selector.setBorder( AuxWindow
                               .makeTitledBorder( "Table " + ( i + 1 ) ) );
            tupleSelectors_[ i ] = selector;
            main.add( selector );
        }

        /* Construct and place box containing selections affecting 
         * output rows. */
        Box rowBox = Box.createVerticalBox();
        pairModeSelector_ = new PairModeSelector();
        rowBox.add( pairModeSelector_ );
        rowBox.add( Box.createVerticalStrut( 5 ) );
        joinSelector_ = new JoinSelector();
        rowBox.add( joinSelector_ );
        rowBox.add( Box.createVerticalStrut( 5 ) );
        rowBox.setBorder( AuxWindow.makeTitledBorder( "Output Rows" ) );
        main.add( rowBox );
    }

    public void checkArguments() {
        for ( int i = 0; i < 2; i++ ) {
            try {
                tupleSelectors_[ i ].getEffectiveTable();
            }
            catch ( IllegalStateException e ) {
                throw new IllegalStateException( e.getMessage() +
                                                 " for table " + ( i + 1 ) );
            }
        }
    }

    public void calculate( ProgressIndicator indicator )
            throws IOException, InterruptedException {

        /* Clear results which we are about to calculate, so that if the
         * calculation fails they won't be hanging over from last time. */
        result_ = null;
        matchSubset_ = null;
        pairCount_ = 0;

        /* Pick up details of the match from the user interface. */
        TopcatModel[] tcModels = new TopcatModel[ 2 ];
        StarTable[] tables = new StarTable[ 2 ];
        StarTable[] bases = new StarTable[ 2 ];
        int[] rowCounts = new int[ 2 ];
        for ( int i = 0; i < 2; i++ ) {
            tcModels[ i ] = tupleSelectors_[ i ].getTable();
            tables[ i ] = tupleSelectors_[ i ].getEffectiveTable();
            bases[ i ] = tcModels[ i ].getApparentStarTable();
            rowCounts[ i ] = Tables.checkedLongToInt( tables[ i ]
                                                     .getRowCount() );
        }
        JoinType joinType = joinSelector_.getJoinType();
        PairMode pairMode = pairModeSelector_.getMode();

        /* Find the matching row pairs. */
        RowMatcher matcher = new RowMatcher( engine_, tables );
        matcher.setIndicator( indicator );
        LinkSet pairs = matcher.findPairMatches( pairMode );
        pairCount_ = pairs.size();
        if ( ! pairs.sort() ) {
            logger_.warning( "Can't sort matches - matched table rows may be "
                           + "in an unhelpful order" );
        }

        /* Process these pairs according to the chosen join type. */
        LinkSet links = joinType.processLinks( pairs, rowCounts );

        /* Get a match score column metadata object. */
        ValueInfo scoreInfo = joinType.getUsedMatchFlag()
                            ? engine_.getMatchScoreInfo()
                            : null;

        /* Create a new table based on the matched lines. */
        boolean addGroups = pairMode.mayProduceGroups();
        JoinFixAction[] fixActs = getDefaultFixActions( 2 );
        StarTable[] useBases = (StarTable[]) bases.clone();
        for ( int i = 0; i < 2; i++ ) {
            if ( ! joinType.getUsedTableFlags()[ i ] ) {
                useBases[ i ] = null;
                fixActs[ i ] = null;
            }
        }
        result_ = MatchStarTables.makeJoinTable( useBases, links, addGroups,
                                                 fixActs, scoreInfo );
        addMatchMetadata( result_, engine_, pairMode, joinType, tcModels );

        /* If it makes sense to do so, record which rows count as matches. */
        if ( joinType.getUsedMatchFlag() ) {
            BitSet matched = new BitSet();
            int iLink = 0;
            for ( Iterator it = links.iterator(); it.hasNext(); ) {
                RowLink link = (RowLink) it.next();
                matched.set( iLink++, link.size() == 2 );
            }
            assert iLink == links.size();
            int nMatch = matched.cardinality();
            if ( nMatch > 0 && nMatch < links.size() ) {
                matchSubset_ = new BitsRowSubset( "matched", matched );
            }
        }
    }

    public void matchSuccess( Component parent ) {
        Object msg;
        String title;
        int msgType;
        if ( result_.getRowCount() == 0 ) {
            title = "Match failed";
            msgType = JOptionPane.ERROR_MESSAGE;
            msg = "Result of match contains no rows";
        }
        else if ( pairCount_ == 0 ) {
            title = "Match failed";
            msgType = JOptionPane.ERROR_MESSAGE;
            msg = "No pairs were found when matching";
        }
        else {
            title = "Match Successful";
            msgType = JOptionPane.INFORMATION_MESSAGE;
            String tname = "match(" 
                         + tupleSelectors_[ 0 ].getTable().getID()
                         + ","
                         + tupleSelectors_[ 1 ].getTable().getID()
                         + ")";
            TopcatModel tcModel = ControlWindow.getInstance()
                                 .addTable( result_, tname, true );
            if ( matchSubset_ != null ) {
                tcModel.addSubset( matchSubset_ );
            }
            msg = new String[] {
                pairCount_ + " pairs found",
                "New table created by match: " + tcModel +
                " (" + result_.getRowCount() + " rows)",
            };
        }
        JOptionPane.showMessageDialog( parent, msg, title, msgType );
    }

    public String getDescription() {
        return toString();
    }

    private static void addMatchMetadata( StarTable table, MatchEngine engine,
                                          PairMode pairMode, JoinType joinType,
                                          TopcatModel[] tcModels ) {
        List params = table.getParameters();
        String type = "Pair match; " + pairMode.getSummary();
        params.add( new DescribedValue( MATCHTYPE_INFO, type ) );
        params.add( new DescribedValue( ENGINE_INFO, engine.toString() ) );
        ValueInfo joinInfo = 
            new DefaultValueInfo( "Join Type", String.class, 
                                  "Determines which rows appear " +
                                  "in output table" );
        String joinValue = joinType.toString() + " ("
                         + joinType.getDescription() + ")";
        params.add( new DescribedValue( joinInfo, joinValue ) );
        params.add( new DescribedValue( 
                        new DefaultValueInfo( "First input table",
                                              String.class ),
                        tcModels[ 0 ].toString() ) );
        params.add( new DescribedValue( 
                        new DefaultValueInfo( "Second input table",
                                              String.class ),
                        tcModels[ 1 ].toString() ) );
    }

    private static class JoinSelector extends Box {
        final JComboBox jCombo_;
        JoinSelector() {
            super( BoxLayout.X_AXIS );
            jCombo_ = new JComboBox( JoinType.getPairTypes() );
            jCombo_.setSelectedItem( JoinType._1AND2 );
            add( new JLabel( "Join Type: " ) );
            add( jCombo_ );
        }
        JoinType getJoinType() {
            return (JoinType) jCombo_.getSelectedItem();
        }
    }
}
