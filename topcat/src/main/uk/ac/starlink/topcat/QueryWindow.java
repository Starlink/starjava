package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import uk.ac.starlink.table.gui.LabelledComponentStack;

/**
 * Abstract superclass for windows which are going to ask the user
 * for some input.  These are like non-modal dialogues, but share 
 * some of the TOPCAT (AuxWindow) look and feel.
 */
public abstract class QueryWindow extends AuxWindow {

    private JPanel auxControls;
    private LabelledComponentStack stack;
    private Action okAction;
    private boolean configured = false;
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
        okAction = new AbstractAction( "OK" ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( perform() ) {
                    dispose();
                }
            }
        };

        /* Set up the control panel, which will contain the OK and Cancel
         * buttons. */
        JPanel queryControls = getControlPanel();
        queryControls.add( new JButton( okAction ) );
        queryControls.add( new JButton( cancelAction ) );

        /* Set up an auxiliary control panel, for custom buttons required by 
         * subclasses. */
        auxControls = new JPanel();

        /* Set up a strut with a preferred size. */
        JComponent hstrut = new JPanel();
        hstrut.setPreferredSize( new Dimension( 400, 0 ) );

        /* Place the components into the window. */
        Box iconBox = new Box( BoxLayout.Y_AXIS );
        iconBox.add( new JLabel( ResourceIcon.QUERY ) );
        iconBox.setBorder( blankBorder );
        stack.setBorder( blankBorder );
        auxControls.setBorder( blankBorder );
        getMainArea().add( iconBox, BorderLayout.WEST );
        getMainArea().add( stack, BorderLayout.CENTER );
        getMainArea().add( auxControls, BorderLayout.SOUTH );
        getMainArea().add( hstrut, BorderLayout.NORTH );
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

    /**
     * Returns a panel which can be used by subclasses to place custom
     * controls.  The panel returned by {@link #getControlPanel} is 
     * used for the OK and Cancel controls.
     * 
     * @return   a container for custom controls
     */
    public JPanel getAuxControlPanel() {
        return auxControls;
    }

    /**
     * Give focus to the first input field in the stack.
     */
    private void initFocus() {
        Component ff = getFirstFocusableField();
        if ( ff != null ) {
            ff.requestFocusInWindow();
        }
    }

    /**
     * Returns the first focusable field in the stack of input fields.
     *
     * @return  an input component which can receive focus
     */
    private Component getFirstFocusableField() {
        Component[] fields = stack.getFields();
        for ( int i = 0; i < fields.length; i++ ) {
            if ( fields[ i ].isFocusable() ) {
                return fields[ i ];
            }
        }
        return null;
    }

    /**
     * Do configuration of keys for the input fields in the stack.
     * We configure the Enter key to close the window.
     */
    private void configureKeys() {
        Object okKey = new Object();
        KeyStroke hitEnter = KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 );
        Component[] fields = stack.getFields();
        for ( int i = 0; i < fields.length; i++ ) {
            if ( fields[ i ] instanceof JComponent ) {
                JComponent field = (JComponent) fields[ i ];
                field.getInputMap().put( hitEnter, okKey );
                field.getActionMap().put( okKey, okAction );
            }
        }
    }

    /**
     * Override the setVisible method to perform some extra actions 
     * when the window is popped up on the first or subsequent occasions.
     */
    public void setVisible( boolean isVis ) {

        /* Execute the superclass implementation. */
        super.setVisible( isVis );

        /* Take some extra actions if we are revealing the window for 
         * the first time. */
        if ( isVis && ! configured ) {
            configureKeys();
            initFocus();
        }
    }
}
