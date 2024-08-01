package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.HumanMatchEngine;
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
    private final Supplier<RowRunner> runnerFact_;
    private final TupleSelector[] tupleSelectors_;
    private final PairModeSelector pairModeSelector_;
    private final JoinSelector joinSelector_;
    private StarTable result_;
    private RowSubset matchSubset_;
    private int pairCount_;
    private JoinType joinType_;
    private JoinFixAction[] fixActs_;

    /**
     * Constructs a new PairMatchSpec.
     *
     * @param  engine  match algorithm object
     * @param  runnerFact  supplier for RowRunner
     */
    @SuppressWarnings("this-escape")
    public PairMatchSpec( MatchEngine engine, Supplier<RowRunner> runnerFact ) {
        engine_ = engine;
        runnerFact_ = runnerFact;

        Box main = Box.createVerticalBox();
        setLayout( new BorderLayout() );
        add( main, BorderLayout.NORTH );

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
        joinType_ = joinSelector_.getJoinType();
        PairMode pairMode = pairModeSelector_.getMode();

        /* Find the matching row pairs. */
        RowRunner runner = runnerFact_.get();
        RowMatcher matcher =
            RowMatcher.createMatcher( engine_, tables, runner );
        matcher.setIndicator( indicator );
        LinkSet pairs = matcher.findPairMatches( pairMode );
        pairCount_ = pairs.size();

        /* Process these pairs according to the chosen join type. */
        LinkSet links = joinType_.processLinks( pairs, rowCounts );

        /* Get a match score column metadata object. */
        ValueInfo scoreInfo = joinType_.getUsedMatchFlag()
                            ? engine_.getMatchScoreInfo()
                            : null;

        /* Create a new table based on the matched lines. */
        boolean addGroups = pairMode.mayProduceGroups();
        fixActs_ = getDefaultFixActions( 2 );
        StarTable[] useBases = bases.clone();
        for ( int i = 0; i < 2; i++ ) {
            if ( ! joinType_.getUsedTableFlags()[ i ] ) {
                useBases[ i ] = null;
                fixActs_[ i ] = null;
            }
        }
        Collection<RowLink> orderedLinks = MatchStarTables.orderLinks( links );
        result_ = MatchStarTables.createInstance( indicator, runner )
                 .makeJoinTable( useBases, orderedLinks, addGroups,
                                 fixActs_, scoreInfo );
        addMatchMetadata( result_, engine_, pairMode, joinType_, tcModels );

        /* If it makes sense to do so, record which rows count as matches. */
        if ( joinType_.getUsedMatchFlag() ) {
            BitSet matched = new BitSet();
            int iLink = 0;
            for ( RowLink link : orderedLinks ) {
                matched.set( iLink++, link.size() == 2 );
            }
            assert iLink == orderedLinks.size();
            int nMatch = matched.cardinality();
            if ( nMatch > 0 && nMatch < orderedLinks.size() ) {
                matchSubset_ = new BitsRowSubset( "matched", matched );
            }
        }
    }

    public void matchSuccess( Component parent ) {
        if ( result_.getRowCount() == 0 ) {
            String title = "Match failed";
            int msgType = JOptionPane.ERROR_MESSAGE;
            String msg = "Result of match contains no rows";
            JOptionPane.showMessageDialog( parent, msg, title, msgType );
        }
        else if ( pairCount_ == 0 ) {
            String title = "Match failed";
            int msgType = JOptionPane.ERROR_MESSAGE;
            String msg = "No pairs were found when matching";
            JOptionPane.showMessageDialog( parent, msg, title, msgType );
        }
        else {
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
            boolean[] tflags = joinType_.getUsedTableFlags();
            Action plotAct =
                 tflags[ 0 ] && tflags[ 1 ]
               ? MatchPlotter.createPlotAction( parent, engine_,
                                                tupleSelectors_, fixActs_,
                                                tcModel )
               : null;
            String[] lines = new String[] {
                pairCount_ + " pairs found",
                "New table created by match: " + tcModel +
                " (" + result_.getRowCount() + " rows)",
            };
            showSuccessMessage( parent, lines, plotAct );
        }
    }

    public String getDescription() {
        return toString();
    }

    private static void addMatchMetadata( StarTable table, MatchEngine engine,
                                          PairMode pairMode, JoinType joinType,
                                          TopcatModel[] tcModels ) {
        List<DescribedValue> params = table.getParameters();
        String type = "Pair match; " + pairMode.getSummary();
        params.add( new DescribedValue( MATCHTYPE_INFO, type ) );
        params.add( new DescribedValue( ENGINE_INFO, engine.toString() ) );
        params.addAll( Arrays.asList( HumanMatchEngine
                                     .getHumanMatchEngine( engine )
                                     .getMatchParameters() ) );
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
        final JComboBox<JoinType> jCombo_;
        JoinSelector() {
            super( BoxLayout.X_AXIS );
            jCombo_ = new JComboBox<JoinType>( JoinType.getPairTypes() );
            jCombo_.setSelectedItem( JoinType._1AND2 );
            add( new JLabel( "Join Type: " ) );
            add( jCombo_ );
        }
        JoinType getJoinType() {
            return jCombo_.getItemAt( jCombo_.getSelectedIndex() );
        }
    }
}
