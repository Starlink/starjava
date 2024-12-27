package uk.ac.starlink.util.gui;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * TextField that presents an initial greyed-out text before use.
 * As soon as the field receives focus or the value is set,
 * the example text is lost irretrievably.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2015
 */
public class ExampleTextField extends JTextField {

    private final String exampleText_;
    private FocusListener focusListener_;
    private DocumentListener docListener_;

    /**
     * Constructor.
     *
     * @param   exampleText   initial text for field
     */
    @SuppressWarnings("this-escape")
    public ExampleTextField( String exampleText ) {
        exampleText_ = exampleText;
        super.setText( exampleText );
        focusListener_ = new FocusListener() {
            public void focusGained( FocusEvent evt ) {
                discardExample();
            }
            public void focusLost( FocusEvent evt ) {
            }
        };
        docListener_ = new DocumentListener() {
            public void insertUpdate( DocumentEvent evt ) {
                discardExample();
            }
            public void removeUpdate( DocumentEvent evt ) {
                discardExample();
            }
            public void changedUpdate( DocumentEvent evt ) {
            }
        };
        addFocusListener( focusListener_ );
        getDocument().addDocumentListener( docListener_ );
        setForeground( UIManager.getColor( "TextField.inactiveForeground" ) );
    }

    @Override
    public void setText( String txt ) {
        if ( ! exampleText_.equals( txt ) ) {
            discardExample();
        }
        super.setText( txt );
    }

    /**
     * Discards the example text, resets the foreground colour,
     * and removes listeners.  Only has any effect the first time it's called.
     */
    private void discardExample() {
        if ( exampleText_.equals( getText() ) ) {
            super.setText( null );
        }
        setForeground( UIManager.getColor( "TextField.foreground" ) );
        removeFocusListener( focusListener_ );
        getDocument().removeDocumentListener( docListener_ );
        focusListener_ = null;
        docListener_ = null;
    }
}
