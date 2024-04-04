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
    private final JComboBox<String> comboBox_;
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
        comboBox_ = new JComboBox<String>();
        editor_ = comboBox_.getEditor();
        comboBox_.setEditable( true );
        comboBox_.addItem( "" );
        add( comboBox_ );

        /* Displaying the example text in the combo box before it has been
         * used is implemented by setting the example text as the editor
         * content, and then jumping through hoops when some real content
         * is inserted into the editor to get rid of it again.
         * This is pretty nasty; really the renderer should be
         * configured to show the example text when appropriate,
         * regardless of editor content, but I haven't managed to figure out
         * a way to get that working. */
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
     * Removes an action listener.
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
        if ( focusListener_ != null ) {
            Component edComp = editor_.getEditorComponent();
            edComp.removeFocusListener( focusListener_ );
            focusListener_ = null;
            edComp.setForeground( UIManager
                                 .getColor( "TextField.foreground" ) );

            /* This is pretty messy.  We can end up here because a string
             * has been cut'n'pasted into the editor field, in which case
             * it can be in the middle of the example text, like
             * "exampPASTEDle".  So try to pull out the pasted text from
             * the current contents of the editor, based on the knowledge
             * we have of the example string it got pasted into. */
            Object edItem = editor_.getItem();
            String entryText =
                  edItem instanceof String && exampleText_ != null
                ? findEmbeddedText( (String) edItem, exampleText_ )
                : null;
            editor_.setItem( entryText );
        }
    }

    /**
     * Attempts to extract a string that has been interpolated somewhere
     * within a given example string.
     *
     * @param  txt  supplied text which may consist of the knownTxt with
     *              a required string somewhere in the middle of it
     * @param  knownTxt  known text, not null
     * @return   string that was added to knownTxt to make txt,
     *           or empty string if it can't be found
     */
    private static String findEmbeddedText( String txt, String knownTxt ) {
        for ( int i = 0; i < knownTxt.length(); i++ ) {
            int j = knownTxt.length() - i;
            if ( knownTxt.substring( 0, j ).equals( txt.substring( 0, j ) ) &&
                 knownTxt.substring( j )
                         .equals( txt.substring( txt.length() - i ) ) ) {
                return txt.substring( j, txt.length() - i );
            }
        }
        return "";
    }
}
