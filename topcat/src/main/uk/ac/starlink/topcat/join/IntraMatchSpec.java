package uk.ac.starlink.topcat.join;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.RowPermutedStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.join.LinkSet;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.table.join.MatchStarTables;
import uk.ac.starlink.table.join.ProgressIndicator;
import uk.ac.starlink.table.join.RowLink;
import uk.ac.starlink.table.join.RowMatcher;
import uk.ac.starlink.table.join.RowRef;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BitsRowSubset;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * MatchSpec for matching between rows of a given table.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    20 Mar 2004
 */
public class IntraMatchSpec extends MatchSpec {

    private final TupleSelector tupleSelector;
    private final JSpinner widthSelector;
    private final MatchEngine engine;
    private StarTable result;
    private int matchCount;

    private final static Logger logger =
        Logger.getLogger( "uk.ac.starlink.topcat.join" );

    private final static String IDENTIFY = 
        "Mark Groups of Rows";
    private final static String ELIMINATE_0 =
        "Eliminate All Grouped Rows";
    private final static String ELIMINATE_1 =
        "Eliminate All But First of Each Group";
    private final static String WIDE =
        "New Table With Groups of Size ";
    private final static String[] OPTIONS = new String[] {
        IDENTIFY, ELIMINATE_0, ELIMINATE_1, WIDE,
    };
    private String option;

    /**
     * Constructs a new IntraMatchSpec.
     *
     * @param  engine the match engine defining the match type
     */
    public IntraMatchSpec( MatchEngine engine ) {
        this.engine = engine;
        Box main = Box.createVerticalBox();
        add( main );

        /* Set up a table/column selector panel for the sole table. */
        tupleSelector = new TupleSelector( engine );
        tupleSelector.setBorder( AuxWindow.makeTitledBorder( "Table" ) );
        main.add( tupleSelector );

        /* Set up a selector for group width. */
        SpinnerNumberModel widthModel = new SpinnerNumberModel();
        widthModel.setValue( new Integer( 2 ) );
        widthModel.setMinimum( new Integer( 2 ) );
        widthSelector = new JSpinner( widthModel );

        /* Set up action options. */
        JComponent actBox = Box.createVerticalBox();
        ButtonGroup buttGrp = new ButtonGroup();
        for ( int i = 0; i < OPTIONS.length; i++ ) {
            final String opt = OPTIONS[ i ];
            Action buttAct = new AbstractAction() {
                public void actionPerformed( ActionEvent evt ) {
                    option = opt;
                    widthSelector.setEnabled( opt == WIDE );
                }
            };
            JRadioButton butt = new JRadioButton( buttAct );
            buttGrp.add( butt );
            Box line = Box.createHorizontalBox();
            line.add( butt );
            line.add( new JLabel( " " + opt ) );
            if ( opt.equals( WIDE ) ) {
                line.add( widthSelector );
            }
            line.add( Box.createHorizontalGlue() );
            actBox.add( line );
            if ( i == 0 ) {
                butt.doClick();
            }
        }
        actBox.setBorder( AuxWindow.makeTitledBorder( "Action" ) );
        main.add( actBox );
    }

    public void checkArguments() {
        tupleSelector.getEffectiveTable();
    }

    /**
     * Returns the currently selected option.  This defines what the user
     * would like done with the results of the completed match.
     *
     * @return  match option, one of IDENTIFY, ELIMINATE_0, ELIMINATE_1, WIDE
     */
    private String getOption() {
        return option;
    }

    /**
     * Returns the width of the output table as a multiple of the width
     * of the input table.  This only makes sense for option==WIDE.
     *
     * @return  output table width
     */
    private int getTableWideness() {
        return ((Number) widthSelector.getValue()).intValue();
    }

    public void calculate( ProgressIndicator indicator )
            throws IOException, InterruptedException {
        result = null;

        /* Interrogate components for tables to operate on. */
        StarTable effTable = tupleSelector.getEffectiveTable();
        StarTable appTable = tupleSelector.getTable().getApparentStarTable();

        /* Do the matching. */
        RowMatcher matcher = 
            new RowMatcher( engine, new StarTable[] { effTable } );
        matcher.setIndicator( indicator );
        LinkSet matches = matcher.findInternalMatches( false );
        if ( ! matches.sort() ) {
            logger.warning( "Can't sort matches - matched table rows may be "
                          + "in an unhelpful order" );
        }
        matchCount = matches.size();

        /* Construct a result table. */
        if ( matchCount == 0 ) {
            result = null;
        }
        else {
            result = makeResultTable( appTable, matches, option );
        }
    }

    /**
     * Constructs a new StarTable from the results of a match operation
     * which has been performed.
     *
     * @param  inTable  the input (apparent) table
     * @param  matches  set of RowLinks describing matches found
     * @param  option   one of the matching options (IDENTIFY, ELIMINATE_0,
     *                  ELIMINATE_1, WIDE)
     * @return  new StarTable formed as a result of this match operation
     */
    private StarTable makeResultTable( StarTable inTable, LinkSet matches,
                                       String option ) {
        if ( option.equals( IDENTIFY ) ) {
            long nrow = inTable.getRowCount();
            StarTable grpTable = MatchStarTables
                                .makeInternalMatchTable( 0, matches, nrow );
            JoinStarTable.FixAction[] fixActs = new JoinStarTable.FixAction[] {
                JoinStarTable.FixAction.makeRenameDuplicatesAction( "_old" ),
                JoinStarTable.FixAction.NO_ACTION,
            };
            return new JoinStarTable( new StarTable[] { inTable, grpTable },
                                      fixActs );
        }
        else if ( option.equals( ELIMINATE_0 ) ||
                  option.equals( ELIMINATE_1 ) ) {
            int startFrom = option.equals( ELIMINATE_0 ) ? 0 : 1;
            BitSet bits = new BitSet();
            bits.set( 0, checkedLongToInt( inTable.getRowCount() ) );
            for ( Iterator it = matches.iterator(); it.hasNext(); ) {
                RowLink link = (RowLink) it.next();
                int nref = link.size();
                assert nref > 1;
                for ( int i = startFrom; i < nref; i++ ) {
                    RowRef ref = link.getRef( i );
                    assert ref.getTableIndex() == 0;
                    bits.clear( checkedLongToInt( ref.getRowIndex() ) );
                }
            }
            int nbit = bits.cardinality();
            long[] rowMap = new long[ nbit ];
            int i = 0;
            for ( int ipos = bits.nextSetBit( 0 ); ipos >= 0;
                  ipos = bits.nextSetBit( ipos + 1 ) ) {
                rowMap[ i++ ] = (long) ipos;
            }
            assert i == nbit;
            return new RowPermutedStarTable( inTable, rowMap );
        }
        else if ( option.equals( WIDE ) ) {
            int width = getTableWideness();
            return MatchStarTables
                  .makeParallelMatchTable( inTable, 0, matches,
                                           width, width, width,
                                           getDefaultFixActions( width ) );
        }
        else {
            throw new AssertionError();
        }
    }

    public void matchSuccess( Component parent ) {
        Object msg;
        String title = "Match Successful";
        int msgType = JOptionPane.INFORMATION_MESSAGE;
        if ( result == null || result.getRowCount() == 0L ) {
            msg = "No internal matches were found";
            title = "Match Failed";
            msgType = JOptionPane.WARNING_MESSAGE;
        }
        else {
            TopcatModel tcModel = ControlWindow.getInstance()
                                 .addTable( result, "matched", true );
            msg = new String[] { 
                matchCount + " match groups located",
                "New table created by match: " + tcModel,
            };
            title = "Match Successful";
            msgType = JOptionPane.INFORMATION_MESSAGE;
        }

        /* Alert the user that the matching is done. */
        JOptionPane.showMessageDialog( parent, msg, title, msgType );
    }

    private static int checkedLongToInt( long lval ) {
        return Tables.checkedLongToInt( lval );
    }
}
