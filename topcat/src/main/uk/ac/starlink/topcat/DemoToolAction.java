package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * This is a template for a custom extension tool for use with the TOPCAT
 * application.
 * To use it, set the {@link ControlWindow#TOPCAT_TOOLS_PROP} system property 
 * to the full name of this class, something like
 * <pre>
 *     -Dtopcat.exttools=uk.ac.starlink.topcat.DemoToolAction
 * </pre>
 *
 * @author   Mark Taylor
 * @since    27 Sep 2011
 */
public class DemoToolAction extends AbstractAction implements TopcatToolAction {

    private Component parent_;
    private DemoWindow window_;

    /**
     * No-arg constructor.
     * This signature is essential for use as an extension tool action.
     */
    @SuppressWarnings("this-escape")
    public DemoToolAction() {
        putValue( NAME, "Demo Sum" );
        putValue( SMALL_ICON, ResourceIcon.DO_WHAT );  // 24x24 icon
        putValue( SHORT_DESCRIPTION, "Sum Column" );   // tooltip text
    }

    // from TopcatToolAction interface
    public void setParent( Component parent ) {
        parent_ = parent;
    }

    // from Action interface
    public void actionPerformed( ActionEvent evt ) {

        /* Lazily creates an instance of the window associated with this
         * action, and ensures it is displayed. */
        if ( window_ == null ) {
            window_ = new DemoWindow( parent_ );
        }
        window_.setVisible( true );
    }

    /**
     * Defines the window which is shown by a DemoToolAction.
     * Of course it is not necessary to define this within the same class
     * as the Action itself.
     */
    private static class DemoWindow extends AuxWindow {

        private final TupleSelector tupleSelector_;
        private final JLabel resultLabel_;
        private final Action calcAction_;

        /**
         * Constructor.
         *
         * @param  parent  parent component
         */
        DemoWindow( Component parent ) {
            super( "Demo", parent );

            /* Set up a selector which allows the user to choose a column
             * from a table.  The selected column is from the "apparent"
             * table, that is, if a current row subset has been selected,
             * only rows from that subset will be included.
             * Either existing column names, or algebraic expressions 
             * based on columns can be selected. */
            ValueInfo info =
                new DefaultValueInfo( "Sum", Number.class,
                                      "Value for summation" );
            tupleSelector_ = new TupleSelector( new ValueInfo[] { info } );

            /* Set up and place other elements of the GUI. */
            resultLabel_ = new JLabel();
            calcAction_ = new AbstractAction( "Calculate" ) {
                public void actionPerformed( ActionEvent evt ) {
                    calculate();
                }
            };
            Box box = Box.createVerticalBox();
            box.add( tupleSelector_ );
            JComponent rLine = Box.createHorizontalBox();
            rLine.add( new JLabel( "Sum: " ) );
            rLine.add( resultLabel_ );
            rLine.add( Box.createHorizontalGlue() );
            box.add( rLine );

            /* Place the main part of the GUI. */
            getMainArea().add( box );

            /* Place control buttons (if it's more appropriate it is OK
             * to place these directly in the main area). */
            getControlPanel().add( new JButton( calcAction_ ) );

            /* Add boilerplate actions and a reference to the help location
             * for this window. */
            addHelp( "DemoTool" );
        }

        /**
         * Invoked when the button is pressed to perform a calculation
         * and post the result.
         */
        private void calculate() {

            /* Gets the effective table consisting of the current rows
             * and single column selected by the user.  If no table
             * has been selected, a RuntimeException should be thrown with
             * a helpful message - if that happens, pass it to the user. */
            StarTable table;
            try {
                table = tupleSelector_.getEffectiveTable();
            }
            catch ( RuntimeException e ) {
                resultLabel_.setText( null );
                ErrorDialog.showError( this, "No selection", e );
                return;
            }

            /* Performs some operation on the table and updates the GUI
             * accordingly.
             * Note, this should be modified if the calculation may take
             * a significant amount of time, to avoid locking up the GUI. */
            try {
                String result = calculateResult( table );
                resultLabel_.setText( result );
            }
            catch ( IOException e ) {
                beep();
                resultLabel_.setText( null );
            }
        }

        /**
         * Performs the actual calculations.
         * Not necessarily called from the AWT Event Dispatch Thread.
         *
         * @param  table  input table
         * @return   string result of calculation
         */
        private static String calculateResult( StarTable table )
                throws IOException {
            RowSequence rseq = table.getRowSequence();
            try {
                double sum = 0;
                long nrow = 0;
                while ( rseq.next() ) {
                    nrow++;
                    Object value = rseq.getCell( 0 );
                    if ( value instanceof Number ) {
                        double dval = ((Number) value).doubleValue();
                        if ( ! Double.isNaN( dval ) ) {
                            sum += dval;
                        }
                    }
                }
                return ((float) sum) + " (" + nrow + " rows)";
            }
            finally {
                rseq.close();
            }
        }
    }
}
