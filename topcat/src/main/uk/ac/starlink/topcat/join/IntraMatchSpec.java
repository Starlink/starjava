package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.join.HumanMatchEngine;
import uk.ac.starlink.table.join.LinkSet;
import uk.ac.starlink.table.join.Match1Type;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.RowLink;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TupleSelector;

/**
 * MatchSpec for matching between rows of a given table.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    20 Mar 2004
 */
public class IntraMatchSpec extends MatchSpec {

    private final TupleSelector tupleSelector_;
    private final Match1TypeSelector type1Selector_;
    private final MatchEngine engine_;
    private final Supplier<RowRunner> runnerFact_;
    private StarTable result_;
    private int matchCount_;

    /**
     * Constructs a new IntraMatchSpec.
     *
     * @param  engine the match engine defining the match type
     * @param  runnerFact   supplier for RowRunner
     */
    @SuppressWarnings("this-escape")
    public IntraMatchSpec( MatchEngine engine,
                           Supplier<RowRunner> runnerFact ) {
        engine_ = engine;
        runnerFact_ = runnerFact;
        Box main = Box.createVerticalBox();
        setLayout( new BorderLayout() );
        add( main, BorderLayout.NORTH );

        /* Set up a table/column selector panel for the sole table. */
        tupleSelector_ = new TupleSelector( engine_.getTupleInfos() );
        tupleSelector_.setBorder( AuxWindow.makeTitledBorder( "Table" ) );
        main.add( tupleSelector_ );

        /* Set up action options. */
        type1Selector_ = new Match1TypeSelector();
        type1Selector_.setBorder( AuxWindow.makeTitledBorder( "Action" ) );
        main.add( type1Selector_ );
    }

    public void checkArguments() {
        tupleSelector_.getEffectiveTable();
    }

    public void calculate( ProgressIndicator indicator )
            throws IOException, InterruptedException {
        result_ = null;

        /* Interrogate components for tables to operate on. */
        Match1Type type1 = type1Selector_.getType1();
        String type1txt = type1Selector_.getType1Text();
        StarTable effTable = tupleSelector_.getEffectiveTable();
        TopcatModel tcModel = tupleSelector_.getTable();
        StarTable appTable = tcModel.getApparentStarTable();

        /* Do the matching. */
        RowMatcher matcher =
            RowMatcher.createMatcher( engine_, new StarTable[] { effTable },
                                      runnerFact_.get() );
        matcher.setIndicator( indicator );
        LinkSet matches = matcher.findInternalMatches( false );
        matches = type1.processLinks( matches );
        matchCount_ = matches.size();

        /* Construct a result table. */
        if ( matchCount_ == 0 ) {
            result_ = null;
        }
        else {
            Collection<RowLink> links = MatchStarTables.orderLinks( matches );
            result_ = type1.createMatchTable( appTable, links );
            addMatchMetadata( result_, engine_, type1txt, tcModel );
        }
    }

    public void matchSuccess( Component parent ) {
        Object msg;
        String title = "Match Successful";
        int msgType = JOptionPane.INFORMATION_MESSAGE;
        if ( result_ == null || result_.getRowCount() == 0L ) {
            msg = "No internal matches were found";
            title = "Match Failed";
            msgType = JOptionPane.WARNING_MESSAGE;
        }
        else {
            String name = "match(" + tupleSelector_.getTable().getID() + ")";
            TopcatModel tcModel = ControlWindow.getInstance()
                                 .addTable( result_, name, true );
            msg = new String[] { 
                matchCount_ + " match groups located",
                "New table created by match: " + tcModel,
            };
            title = "Match Successful";
            msgType = JOptionPane.INFORMATION_MESSAGE;
        }

        /* Alert the user that the matching is done. */
        JOptionPane.showMessageDialog( parent, msg, title, msgType );
    }

    /**
     * Adds metadata to the output table for a match that has been performed.
     *
     * @param  table  output table
     * @param  engine  match engine
     * @param  type1txt  description of internal match type
     * @param  tcModel   input table
     */
    private static void addMatchMetadata( StarTable table, MatchEngine engine,
                                          String type1txt,
                                          TopcatModel tcModel ) {
        List<DescribedValue> params = table.getParameters();
        params.add( new DescribedValue( MATCHTYPE_INFO,
                                        "Internal match; " + type1txt ) );
        params.add( new DescribedValue( ENGINE_INFO, engine.toString() ) );
        params.addAll( Arrays.asList( HumanMatchEngine
                                     .getHumanMatchEngine( engine )
                                     .getMatchParameters() ) );
        params.add( new DescribedValue( new DefaultValueInfo( "Input table",
                                                              String.class ),
                                        tcModel.toString() ) );
    }
}
