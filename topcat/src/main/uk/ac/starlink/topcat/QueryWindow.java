package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
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
    private Action cancelAction;
    private boolean configured;
    protected Border blankBorder = 
        BorderFactory.createEmptyBorder( 5, 5, 5, 5 );

    /**
     * Constructs a new QueryWindow with OK and Cancel buttons.
     *
     * @param   title  title to put in the window heading
     * @param   parent  parent component, used for positioning the window
     */
    public QueryWindow( String title, Component parent ) {
        this( title, parent, true, true );
    }

    /**
     * Constructs a new QueryWindow.
     *
     * @param   title  title to put in the window heading
     * @param   parent  parent component, used for positioning the window
     * @param   ok      whether to include an OK button
     * @param   cancel  whether to include a Cancel button
     */
    @SuppressWarnings("this-escape")
    public QueryWindow( String title, Component parent, boolean ok,
                        boolean cancel ) {
        super( title, parent );

        /* Initialise the stack of query boxes. */
        stack = new LabelledComponentStack();

        /* Set up the action for cancelling this dialogue - just close it. */
        cancelAction = new AbstractAction( "Cancel" ) {
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
        if ( ok ) {
            queryControls.add( new JButton( okAction ) );
        }
        if ( cancel ) {
            queryControls.add( new JButton( cancelAction ) );
        }

        /* Set up an auxiliary control panel, for custom buttons required by 
         * subclasses. */
        auxControls = new JPanel( new BorderLayout() );

        /* Set up a strut with a preferred size. */
        JComponent hstrut = new JPanel();
        hstrut.setPreferredSize( new Dimension( 400, 0 ) );


        /* Place the components into the window. */
        JComponent iconBox = new Box( BoxLayout.Y_AXIS );
        iconBox.add( new JLabel( UIManager
                                .getIcon( "OptionPane.questionIcon" ) ) );
        iconBox.add( Box.createVerticalGlue() );
        iconBox.setBorder( blankBorder );
        JComponent contentBox = new JPanel( new BorderLayout() );
        stack.setBorder( blankBorder );
        contentBox.add( stack, BorderLayout.NORTH );
        contentBox.add( hstrut, BorderLayout.SOUTH );
        contentBox.add( auxControls, BorderLayout.CENTER );
        getMainArea().add( iconBox, BorderLayout.WEST );
        getMainArea().add( contentBox, BorderLayout.CENTER );

        /* Fix it so that the first field in the stack has the focus
         * when the window is displayed.  You have to jump through hoops
         * to do this because requestFocusInWindow doesn't work before
         * the component has been set visible. */
        stack.addAncestorListener( new AncestorListener() {
            public void ancestorAdded( AncestorEvent evt ) {

                /* Request focus for the first field if there is one. */
                Component[] fields = stack.getFields();
                Component field0 = fields.length > 0 ? fields[ 0 ] : null;
                if ( field0 != null ) {
                    field0.requestFocusInWindow();
                }

                /* After the first time, the behaviour is no longer required. */
                stack.removeAncestorListener( this );
            }
            public void ancestorMoved( AncestorEvent evt ) {
            }
            public void ancestorRemoved( AncestorEvent evt ) {
            }
        } );
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
     * Programatically push the OK button.
     */
    public void invokeOK() {
        okAction.actionPerformed( null );
    }

    /** 
     * Programatically push the Cancel button.
     */
    public void invokeCancel() {
        cancelAction.actionPerformed( null );
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

            /* For an editable combo box, arrange that hitting return when a
             * string is entered will trigger the OK action.  Implementing
             * this is a bit involved; if you just add the okAction to the
             * comboBox.getEditor(), the action is invoked before the 
             * combo box has its value set (at least at Sun's J2SE1.4.2 - 
             * Swing misfeature?), so we have to work round this. */
            if ( fields[ i ] instanceof JComboBox ) {
                final JComboBox<?> cbox = (JComboBox<?>) fields[ i ];
                if ( cbox.isEditable() &&
                     cbox.getEditor().getEditorComponent()
                     instanceof JTextField ) {
                    final JTextField tfield =
                       (JTextField) cbox.getEditor().getEditorComponent();
                    tfield.addActionListener( new ActionListener() {
                        public void actionPerformed( ActionEvent evt ) {
                            String text = tfield.getText();
                            if ( text != null && text.trim().length() > 0 ) {
                                cbox.setSelectedItem( text );
                                okAction.actionPerformed( evt );
                            }
                        }
                    } );
                }
            }

            /* For other kinds of fields (mostly, JTextFields) fix for an
             * Enter key to trigger OK. */
            else if ( fields[ i ] instanceof JComponent ) {
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

        /* Take some extra actions if we are revealing the window for 
         * the first time. */
        if ( isVis && ! configured ) {
            configureKeys();
            initFocus();
            pack();
            configured = true;
        }

        /* Execute the superclass implementation. */
        super.setVisible( isVis );
    }
}
