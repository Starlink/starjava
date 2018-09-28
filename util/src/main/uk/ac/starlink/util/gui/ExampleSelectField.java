package uk.ac.starlink.util.gui;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Text entry component with some additional features.
 * Some example text may be provided, and this will be displayed inside
 * the component until it's used.
 * Items chosen are kept as options that can be easily selected on
 * subsequent occasions.
 *
 * This is currently implemented as an editable JComboBox.
 *
 * @author   Mark Taylor
 * @since    14 Jul 2015
 */
public class ExampleSelectField extends JPanel {

    private final String exampleText_;
    private final JComboBox comboBox_;
    private final ComboBoxEditor editor_;
    private FocusListener focusListener_;

    /**
     * Constructor.
     *
     * @param   exampleText  text for display before use; may be null
     */
    public ExampleSelectField( String exampleText ) {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        exampleText_ = exampleText;
        comboBox_ = new JComboBox();
        editor_ = comboBox_.getEditor();
        comboBox_.setEditable( true );
        comboBox_.addItem( "" );
        add( comboBox_ );
        if ( exampleText_ != null ) {
            editor_.setItem( exampleText_ );
            focusListener_ = new FocusListener() {
                public void focusGained( FocusEvent evt ) {
                    discardExample();
                }
                public void focusLost( FocusEvent evt ) {
                }
            };
            Component edComp = editor_.getEditorComponent();
            edComp.addFocusListener( focusListener_ );
            edComp.setForeground( UIManager
                                 .getColor( "TextField.inactiveForeground" ) );
        }
    }

    /**
     * Returns the currently entered text.
     *
     * @return  text
     */
    public String getText() {
        String txt = (String) editor_.getItem();
        return ( exampleText_ != null && exampleText_.equals( txt ) ) ? null 
                                                                      : txt;
    }

    /**
     * Programmatically sets the currently entered text.
     *
     * @param  txt  text
     */
    public void setText( String txt ) {
        discardExample();
        editor_.setItem( txt );
    }

    /**
     * Marks given text as a chosen value for this component.
     * This method should usually be called if the selection has been
     * used in some way.  The text will then be available for subsequent
     * re-selection.
     *
     * @param   txt  chosen text
     */
    public void chooseText( String txt ) {
        if ( txt != null ) {
            boolean hasTxt = false;
            for ( int i = 0; i < comboBox_.getItemCount(); i++ ) {
                hasTxt = hasTxt || txt.equals( comboBox_.getItemAt( i ) );
            }
            if ( ! hasTxt ) {
                comboBox_.insertItemAt( txt, 0 );
            }
        }
        comboBox_.setSelectedItem( txt );
    }

    /**
     * Adds an action listener.
     *
     * @param  l  listener to add
     */
    public void addActionListener( ActionListener l ) {
        comboBox_.addActionListener( l );
    }

    /**
     * Removes an actino listener.
     *
     * @param  l  previously-added listener
     */
    public void removeActionListener( ActionListener l ) {
        comboBox_.removeActionListener( l );
    }

    /**
     * Discards the example text, resets the foreground colour,
     * and removes listeners.  Only has any effect the first time it's called.
     */
    private void discardExample() {
        if ( exampleText_ != null &&
             exampleText_.equals( editor_.getItem() ) ) {
            editor_.setItem( null );
            Component edComp = editor_.getEditorComponent();
            edComp.removeFocusListener( focusListener_ );
            edComp.setForeground( UIManager
                                 .getColor( "TextField.foreground" ) );
            focusListener_ = null;
        }
    }
}
