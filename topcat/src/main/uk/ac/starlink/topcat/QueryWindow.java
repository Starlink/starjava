package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
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
public abstract class QueryWindow extends AuxWindow implements WindowListener {

    private JPanel auxControls;
    private LabelledComponentStack stack;
    private Action okAction;
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

        /* React to window events. */
        addWindowListener( this );

        /* Place the components into the window. */
        Box iconBox = new Box( BoxLayout.Y_AXIS );
        iconBox.add( new JLabel( ResourceIcon.QUERY ) );
        iconBox.setBorder( blankBorder );
        stack.setBorder( blankBorder );
        auxControls.setBorder( blankBorder );
        getMainArea().add( iconBox, BorderLayout.WEST );
        getMainArea().add( stack, BorderLayout.CENTER );
        getMainArea().add( auxControls, BorderLayout.SOUTH );
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

    /*
     * WindowListener implementation
     */
    public void windowOpened( WindowEvent evt ) {
        initFocus();
        configureKeys(); 
    }
    public void windowActivated( WindowEvent evt ) {
        initFocus();
    }
    public void windowClosed( WindowEvent evt ) {}
    public void windowClosing( WindowEvent evt ) {}
    public void windowDeactivated( WindowEvent evt ) {}
    public void windowDeiconified( WindowEvent evt ) {}
    public void windowIconified( WindowEvent evt ) {}
}
