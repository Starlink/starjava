package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * Subset query window class which requires from the user an integer.
 *
 * @author   Mark Taylor
 * @since    28 Sep 2006
 */
public abstract class IntegerSubsetQueryWindow extends SubsetQueryWindow {

    private final JTextField numField_;
    private String lastNum_;

    /**
     * Constructor.
     *
     * @param   tcModel  topcat model
     * @param   parent   parent component (for window placing)
     * @param   title   window title
     * @param   numLabel  label for the integer entry text component
     */
    @SuppressWarnings("this-escape")
    public IntegerSubsetQueryWindow( TopcatModel tcModel, Component parent,
                                     String title, String numLabel ) {
        super( tcModel, parent, title );

        /* Prepare the integer entry field. */
        numField_ = new JTextField();
        numField_.addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                configureFields();
            }
        } );
        lastNum_ = "";

        /* Place components. */
        LabelledComponentStack stack = getStack();
        stack.addLine( numLabel, numField_ );
        stack.addLine( "Subset Name", getNameField() );
        stack.addLine( "Expression", getExpressionField() );
    }

    /**
     * Invoked when the text of the numeric entry field changes.
     */
    private void configureFields() {
        String tnum = numField_.getText().trim();
	if ( tnum.length() > 0 ) {
            int inum;
            try {
                inum = Integer.parseInt( tnum );
            }
            catch ( NumberFormatException e ) {
                inum = -1;
            }
            if ( inum > 0 ) {
                lastNum_ = tnum;
                configureFields( inum );
            }
            else {
                beep();
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        numField_.setText( lastNum_ );
                    }
                } );
            }
        }
        else {
            lastNum_ = "";
            setSelectedName( null );
            getExpressionField().setText( "" );
        }
    }

    /**
     * Invoked when the value of the numeric entry field has (or may have)
     * changed to a legal value.
     * Implementations should use this value to configure fields that
     * they look after accordingly.
     *
     * @param   num  non-negative integer value
     */
    protected abstract void configureFields( int num );
}
