package uk.ac.starlink.topcat.join;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
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
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.StarTable;
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
    private Collection matches;
    private TopcatModel tcModel;

    private final static String IDENTIFY = 
        "Mark Groups of Objects";
    private final static String ELIMINATE_0 =
        "Eliminate All Grouped Objects";
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

        /* Interrogate components for tables to operate on. */
        tcModel = tupleSelector.getTable();
        StarTable effTable = tupleSelector.getEffectiveTable();

        /* Do the matching. */
        RowMatcher matcher = 
            new RowMatcher( engine, new StarTable[] { effTable } );
        matcher.setIndicator( indicator );
        matches = matcher.findInternalMatches( false );
    }

    public void matchSuccess( Component parent ) {
        Object msg;
        String title = "Match Successful";
        int msgType = JOptionPane.INFORMATION_MESSAGE;
        if ( matches.size() == 0 ) {
            msg = "No internal matches were found";
            title = "Match Failed";
            msgType = JOptionPane.ERROR_MESSAGE;
        }

        /* Mark each row with the group it is in. */
        else if ( option.equals( IDENTIFY ) ) {
            long nrow = tcModel.getDataModel().getRowCount();
            StarTable grpTable = MatchStarTables
                                .makeInternalMatchTable( 0, matches, nrow );
            tcModel.appendColumns( grpTable );
            msg = "Grouping columns added to " + tcModel;
        }

        /* Create a new subset based on rows that are in groups. */
        else if ( option.equals( ELIMINATE_0 ) ||
                  option.equals( ELIMINATE_1 ) ) {
            int startFrom;
            String rsetName;
            String msg2;
            if ( option.equals( ELIMINATE_0 ) ) {
                startFrom = 0;
                rsetName = "matchUnique";
                msg2 = "All matched rows hidden";
            }
            else if ( option.equals( ELIMINATE_1 ) ) {
                startFrom = 1;
                rsetName = "matchUnique1";
                msg2 = "All matched rows but first in group hidden";
            }
            else {
                throw new AssertionError();
            }

            BitSet bits = new BitSet();
            for ( Iterator it = matches.iterator(); it.hasNext(); ) {
                RowLink link = (RowLink) it.next();
                int nref = link.size();
                assert nref > 1;
                for ( int i = startFrom; i < nref; i++ ) {
                    RowRef ref = link.getRef( i );
                    assert ref.getTableIndex() == 0;
                    bits.set( checkedLongToInt( ref.getRowIndex() ) );
                }
            }
            RowSubset rset = new BitsRowSubset( rsetName, bits, true );
            tcModel.addSubset( rset );
            tcModel.applySubset( rset );
            msg = new String[] {
                "New subset applied to table " + tcModel,
                msg2,
            };
        }

        /* Create a new table with elements of groups side-by-side. */
        else if ( option.equals( WIDE ) ) {
            int width = getTableWideness();
            StarTable wideTable =
                MatchStarTables
               .makeParallelMatchTable( tcModel.getApparentStarTable(), 0,
                                        matches, width, width, width );
            TopcatModel tcModel = ControlWindow.getInstance()
                                 .addTable( wideTable, "matched", true );
            msg = "New table created by match: " + tcModel;
        }

        /* Unknown option shouldn't be possible. */
        else {
            throw new AssertionError();
        }

        /* Alert the user that the matching is done. */
        JOptionPane.showMessageDialog( parent, msg, title, msgType );
    }

    private static int checkedLongToInt( long lval ) {
        return AbstractStarTable.checkedLongToInt( lval );
    }
}
