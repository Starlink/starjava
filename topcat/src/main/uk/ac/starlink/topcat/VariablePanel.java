package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Panel for displaying and keeping track of user global variables.
 *
 * <p>The application-wide instance of this component is available from
 * the {@link #getInstance} method.
 *
 * @author   Mark Taylor
 * @since    3 Jul 2025
 */
public class VariablePanel extends JPanel {

    private final DefaultListModel<UserConstant<?>> varListModel_;
    private final JList<UserConstant<?>> varList_;
    private final JComponent viewpanelHolder_;
    private final CardLayout viewpanelLayout_;
    private final ActionForwarder forwarder_;
    private final Map<String,UserConstant<?>> varMap_;
    private final Map<String,UserConstant<?>> varMapView_;
    private final Map<UserConstant<?>,Specifier<?>> specifierMap_;
    private final Action addAct_;
    private final Action removeAct_;

    private static final String NO_CARD = "NONE";
    private static final boolean ALLOW_NAME_EDIT = false; // foot gun
    private static final VariablePanel instance_ = new VariablePanel();

    /**
     * Constructor.
     */
    protected VariablePanel() {
        super( new BorderLayout() );
        varListModel_ = new DefaultListModel<UserConstant<?>>();
        varMap_ = new ConcurrentHashMap<String,UserConstant<?>>();
        varMapView_ = Collections.unmodifiableMap( varMap_ );
        specifierMap_ = new HashMap<UserConstant<?>,Specifier<?>>();
        varListModel_.addListDataListener( new ListDataListener() {
            public void contentsChanged( ListDataEvent evt ) {
                updateMap();
            }
            public void intervalAdded( ListDataEvent evt ) {
                updateMap();
            }
            public void intervalRemoved( ListDataEvent evt ) {
                updateMap();
            }
            private void updateMap() {
                varMap_.clear();
                for ( int i = 0; i < varListModel_.getSize(); i++ ) {
                    UserConstant<?> konst = varListModel_.getElementAt( i );
                    varMap_.put( konst.getName(), konst );
                }
            }
        } );
        forwarder_ = new ActionForwarder();
        viewpanelLayout_ = new CardLayout();
        viewpanelHolder_ = new JPanel( viewpanelLayout_ );
        viewpanelHolder_.add( new JPanel(), NO_CARD );
        varList_ = new JList<>( varListModel_ );
        varList_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        varList_.addListSelectionListener( evt -> {
            UserConstant<?> konst = varList_.getSelectedValue();
            if ( konst == null && varList_.getModel().getSize() > 0 ) {
                varList_.setSelectedIndex( 0 );
                konst = varList_.getSelectedValue();
            }
            viewpanelLayout_.show( viewpanelHolder_, getConstLabel( konst ) );
        } );
        JPanel listBox = new JPanel( new BorderLayout() );
        listBox.add( Box.createHorizontalStrut( 64 ), BorderLayout.NORTH );
        listBox.add( varList_, BorderLayout.CENTER );
        JScrollPane scroller =
            new JScrollPane( listBox,
                             ScrollPaneConstants
                            .VERTICAL_SCROLLBAR_AS_NEEDED,
                             ScrollPaneConstants
                            .HORIZONTAL_SCROLLBAR_NEVER );
        add( scroller, BorderLayout.WEST );
        add( viewpanelHolder_, BorderLayout.CENTER );

        /* Add a couple of variables as examples. */
        addDoubleVariable( new UserConstant<Double>( Double.class, "var$x",
                                                     Double.valueOf( 0.0 ) ) );
        addIntegerVariable( new UserConstant<Integer>( Integer.class, "var$i",
                                                       Integer.valueOf( 0 ) ) );
        varList_.setSelectedIndex( 0 );

        /* Actions for manipulating the list. */
        addAct_ =
            BasicAction.create( "Add New Variable", ResourceIcon.ADD,
                                "Add a new variable to the list",
                                evt -> openAddDialogue() );
        removeAct_ =
            BasicAction.create( "Remove Selected Variable", ResourceIcon.DELETE,
                                "Delete the currently visible variable "
                              + "from the list",
                                evt -> openRemoveDialogue() );
    }

    /**
     * Adds a new floating point variable to the display.
     *
     * @param  konst   variable
     */
    public void addDoubleVariable( UserConstant<Double> konst ) {
        addVariable( konst, new UserDoubleSpecifier() );
    }

    /**
     * Adds a new integer variable to the display.
     *
     * @param  konst  variable
     */
    public void addIntegerVariable( UserConstant<Integer> konst ) {
        addVariable( konst, new UserIntSpecifier() );
    }

    /**
     * Removes the given variable from the list managed by this window.
     * No effect if the variable is not currently in the list.
     *
     * @param  konst  variable to remove
     */
    public void removeVariable( UserConstant<?> konst ) {
        varListModel_.removeElement( konst );
        specifierMap_.remove( konst );
        varMap_.remove( konst.getName() );
    }

    /**
     * Returns a map of all the variables managed by this window,
     * keyed by variable name.
     * The returned object is always the same Map object,
     * but its contents may change over time according to the state
     * of this window.
     *
     * @return  immutable (but not unchanging) name to value map
     */
    public Map<String,UserConstant<?>> getVariables() {
        return varMapView_;
    }

    /**
     * Returns the specifier component used to control a given variable.
     *
     * @param   konst  variable
     * @return   specifier
     */
    public <T> Specifier<T> getSpecifier( UserConstant<T> konst ) {
        @SuppressWarnings("unchecked")
        Specifier<T> specifier = (Specifier<T>) specifierMap_.get( konst );
        return specifier;
    }

    /**
     * Adds a listener that will be notified if any variable values change.
     *
     * @param  l  listener
     */
    public void addVariableValueListener( ActionListener l ) {
        forwarder_.addActionListener( l );
    }

    /**
     * Removes a listener previously added.
     *
     * @param  l  listener
     */
    public void removeVariableValueListener( ActionListener l ) {
        forwarder_.removeActionListener( l );
    }

    /**
     * Returns the action that adds a new variable to this panel.
     *
     * @return  add variable action
     */
    public Action getAddVariableAction() {
        return addAct_;
    }

    /**
     * Returns the action that removes the currently selected variable
     * from this panel.
     *
     * @return   remove variable action
     */
    public Action getRemoveVariableAction() {
        return removeAct_;
    }

    /**
     * Displays a modal dialogue that allows the user to add a new
     * variable to the list.
     */
    private void openAddDialogue() {
        JTextField nameField = new JTextField();
        JRadioButton intButt = new JRadioButton( "Integer" );
        JRadioButton doubleButt = new JRadioButton( "Floating point" );
        ButtonGroup buttGrp = new ButtonGroup();
        buttGrp.add( intButt );
        buttGrp.add( doubleButt );
        doubleButt.setSelected( true );
        Box typeBox = Box.createHorizontalBox();
        typeBox.add( doubleButt );
        typeBox.add( Box.createHorizontalStrut( 5 ) );
        typeBox.add( intButt );
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( "Name", nameField );
        stack.addLine( "Type", typeBox );
        JOptionPane optionPane =
                new JOptionPane( stack, JOptionPane.QUESTION_MESSAGE,
                                 JOptionPane.OK_CANCEL_OPTION ) {
            @Override
            public void selectInitialValue() {
                super.selectInitialValue();
                nameField.requestFocusInWindow();
            }
        };
        JDialog dialog = optionPane.createDialog( this, "New Variable" );
        dialog.pack();
        dialog.setLocationRelativeTo( this );
        dialog.setVisible( true );
        Integer okOption = Integer.valueOf( JOptionPane.OK_OPTION );
        if ( okOption.equals( optionPane.getValue() ) ) { 
            String name = nameField.getText();
            Class<?> clazz = intButt.isSelected() ? Integer.class
                                                  : Double.class;
            if ( isJavaIdentifier( name ) && ! varMap_.containsKey( name ) ) {
                if ( Double.class.equals( clazz ) ) {
                    addDoubleVariable( new UserConstant<Double>( Double.class,
                                                                 name, 0.0 ) );
                }
                else if ( Integer.class.equals( clazz ) ) {
                    addIntegerVariable( new UserConstant<Integer>
                                                        ( Integer.class, name,
                                                          0 ) );
                }
                else {
                    assert false;
                }
            }
        }
    }

    /**
     * Removes the currently selected variable from the list
     * following user confirmation.
     */
    private void openRemoveDialogue() {
        UserConstant<?> konst = varList_.getSelectedValue();
        if ( konst != null ) {
            if ( JOptionPane
                .showConfirmDialog( this, "Delete Variable " + konst.getName()
                                                             + "?",
                                    "Variable Deletion",
                                    JOptionPane.OK_CANCEL_OPTION ) ==
                 JOptionPane.OK_OPTION ) {
                removeVariable( konst );
            }
        }
    }

    /**
     * Adds a variable with an associated specifier to the display.
     *
     * @param  konst  variable
     * @param  specifier  specifier
     */
    private <T> void addVariable( UserConstant<T> konst,
                                  Specifier<T> specifier ) {
        specifierMap_.put( konst, specifier );
        ViewPanel<T> vpanel = new ViewPanel<>( konst, specifier );
        JTextField nameField = vpanel.nameField_;
        nameField.addActionListener( evt -> {
            String name = nameField.getText();
            if ( isJavaIdentifier( name ) ) {
                varMap_.remove( konst.getName() );
                konst.setName( name );
                varMap_.put( konst.getName(), konst );
                for ( int ik = 0; ik < varListModel_.getSize(); ik++ ) {
                    varListModel_.set( ik, varListModel_.get( ik ) );
                }
                varList_.revalidate();
            }
            else {
                nameField.setText( konst.getName() );
            }
        } );
        nameField.setEditable( ALLOW_NAME_EDIT );
        specifier.addActionListener( evt -> {
            T value = specifier.getSpecifiedValue();
            konst.setValue( value );
            vpanel.valueField_.setText( value.toString() );
            forwarder_.actionPerformed( evt );
        } );
        varListModel_.addElement( konst );
        viewpanelHolder_.add( vpanel, getConstLabel( konst ) );
        varList_.setSelectedValue( konst, true );
        specifier.setSpecifiedValue( konst.getValue() );
    }

    /**
     * Returns the application-wide instance of this class.
     *
     * @return   static instance
     */
    public static VariablePanel getInstance() {
        return instance_;
    }

    /**
     * Returns the string label identifying a given constant in the
     * CardLayout.
     *
     * @param  konst  variable
     * @return   string label uniquely identifying variable
     */
    private static String getConstLabel( UserConstant<?> konst ) {
        return konst == null ? NO_CARD : TopcatUtils.identityString( konst );
    }

    /**
     * Indicates whether a string constitutes a legal Java identifier.
     *
     * @param  txt  string
     * @return  true iff txt is syntactically a java identifier
     */
    private static boolean isJavaIdentifier( String txt ) {
        if ( txt == null ) {
            return false;
        }
        txt = txt.trim();
        if ( txt.length() == 0 ) {
            return false;
        }
        if ( ! Character.isJavaIdentifierStart( txt.charAt( 0 ) ) ) {
            return false;
        }
        for ( int i = 1; i < txt.length(); i++ ) {
            if ( ! Character.isJavaIdentifierPart( txt.charAt( i ) ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Panel displaying variable information and interaction components.
     */
    private static class ViewPanel<T> extends JPanel {

        private final JTextField nameField_;
        private final JTextField valueField_;

        /**
         * Constructor.
         *
         * @param  konst   user variable
         * @param  specifier   interaction component
         */
        ViewPanel( UserConstant<T> konst, Specifier<T> specifier ) {
            super( new BorderLayout() );
            nameField_ = new JTextField( 16 );
            nameField_.setText( konst.getName() );
            JTextField typeField = new JTextField();
            typeField.setText( konst.getContentClass().getSimpleName() );
            typeField.setEditable( false );
            typeField.setBorder( BorderFactory.createEmptyBorder() );
            valueField_ = new JTextField();
            valueField_.setText( konst.getValue().toString() );
            valueField_.setEditable( false );
            valueField_.setBorder( BorderFactory.createEmptyBorder() );
            LabelledComponentStack stack = new LabelledComponentStack();
            stack.addLine( "Name", nameField_ );
            stack.addLine( "Type", typeField );
            stack.addLine( "Value", valueField_ );
            Box linesBox = Box.createVerticalBox();
            add( linesBox, BorderLayout.NORTH );
            linesBox.add( Box.createHorizontalStrut( 500 ) );
            linesBox.add( stack );
            linesBox.add( Box.createVerticalStrut( 5 ) );
            linesBox.add( specifier.getComponent() );
            linesBox.add( Box.createVerticalGlue() );
            linesBox.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 0 ) );
        }
    }
}
