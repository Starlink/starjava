package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * Abstract superclass for windows which are going to ask the user
 * for some input.  These are like non-modal dialogues, but share 
 * some of the TOPCAT (AuxWindow) look and feel.
 */
public abstract class QueryWindow extends AuxWindow {

    private JPanel controls;
    private LabelledComponentStack stack;
    protected Border blankBorder = 
        BorderFactory.createEmptyBorder( 5, 5, 5, 5 );

    /**
     * Constructs a new QueryWindow.
     *
     * @param   title  title to put in the window heading
     * @param   parent  parent component, used for positioning the window
     */
    public QueryWindow( String title, Component parent ) {
        super( title, parent );

        /* Initialise the stack of query boxes. */
        stack = new LabelledComponentStack();

        /* Set up the action for cancelling this dialogue - just close it. */
        Action cancelAction = new AbstractAction( "Cancel" ) {
            public void actionPerformed( ActionEvent evt ) {
                dispose();
            }
        };

        /* Set up the action for completing this dialogue - invoke the 
         * perform method until it returns true. */
        Action okAction = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( perform() ) {
                    dispose();
                }
            }
        };

        /* Set up the control panel. */
        controls = new JPanel();
        controls.add( new JButton( okAction ) );
        controls.add( new JButton( cancelAction ) );

        /* Place the components into the window. */
        Box iconBox = new Box( BoxLayout.Y_AXIS );
        iconBox.add( new JLabel( ResourceIcon.QUERY ) );
        iconBox.setBorder( blankBorder );
        stack.setBorder( blankBorder );
        controls.setBorder( blankBorder );
        getContentPane().add( iconBox, BorderLayout.WEST );
        getContentPane().add( stack, BorderLayout.CENTER );
        getContentPane().add( controls, BorderLayout.SOUTH );
    }

    /**
     * This method will be invoked when the OK button is pushed or the user
     * otherwise indicates that he has filled in the form.
     *
     * @return   true if the action is complete in some sense. 
     *           The window will be disposed if true is returned, otherwise
     *           it will remain posted.
     */
    protected abstract boolean perform();

    /**
     * Returns a stack of components suitable for adding new query boxes to.
     * 
     * @return  the stack
     */
    protected LabelledComponentStack getStack() {
        return stack;
    }
}
