package uk.ac.starlink.util.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.EventListenerList;
import javax.swing.text.Document;

import uk.ac.starlink.util.images.ImageHolder;

/**
 * A component for accepting a text string, but with the additional
 * capability of choosing from the complete list of characters
 * available in the current font. The font details are shown in a
 * popup window that is initialised using a button shown against the
 * text entry area.
 *
 * @since $Date$
 * @since 06-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
*/
public class SelectTextField 
    extends JPanel
    implements SelectCharactersListener 
{
    /**
     * The text entry field.
     */
    protected JTextField textField = new JTextField();

    /**
     * Button to initiate selecting special characters.
     */
    protected JButton charButton = new JButton();

    /**
     * The special character chooser.
     */
    protected SelectCharacters charChooser = null;

    /**
     * Create an instance.
     */
    @SuppressWarnings("this-escape")
    public SelectTextField() 
    {
        initUI();
    }

    /**
     * Create an instance with initial text field.
     *
     * @param text default text for the text field.
     */
    @SuppressWarnings("this-escape")
    public SelectTextField( String text ) 
    {
        initUI();
        setText( text );
    }

    /**
     * Create an instance with initial text field and using the
     * pre-configured instance of SelectCharacters.
     *
     * @param text default text for the text field.
     */
    @SuppressWarnings("this-escape")
    public SelectTextField( String text, SelectCharacters charChooser ) 
    {
        initUI();
        setText( text );
        this.charChooser = charChooser;
        charChooser.addListener( this );
    }

    /**
     * Initialise the user interface.
     */
    protected void initUI() 
    {
        //  Initialise any components.
        setLayout( new GridBagLayout() );

        ImageIcon specialIcon = new ImageIcon(
            ImageHolder.class.getResource( "special.gif" ) );
        charButton.setIcon( specialIcon );

        //  Listen to changes in the text. Passing them on to any of
        //  our listeners.
        textField.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    fireAction( e );
                }
            });

        //  Add components.
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add( textField, gbc );

        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 0;
        add( charButton, gbc );

        //  Set tooltips.
        charButton.setToolTipText( "Select from all characters in font" );

        //  Add action to respond to the button.
        charButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    chooseChars();
                }
            });
    }

    /**
     * Choose a special character phrase to append.
     */
    protected void chooseChars() 
    {
        if ( charChooser == null ) {
            charChooser = new SelectCharacters( getTextFont() );
            charChooser.addListener( this );
        } 
        else {
            charChooser.setDisplayFont( getTextFont() );
            charChooser.setVisible( true );
        }
    }

    /**
     * Accept new characters.
     */
    public void newCharacters( SelectCharactersEvent e ) 
    {
        insertText( e.getText() );
    }

    /**
     * Set the displayed text.
     */
    public void setText( String text ) 
    {
        textField.setText( text );
    }

    /**
     * Insert new text, either at the end or at the insertion point.
     */
    public void insertText( String text ) 
    {
        textField.replaceSelection( text );
    }

    /**
     * Get the displayed text.
     */
    public String getText() 
    {
        return textField.getText();
    }

    /**
     * Set the text font.
     */
    public void setTextFont( Font font ) 
    {
        textField.setFont( font );
        if ( charChooser != null ) {
            charChooser.setDisplayFont( getTextFont() );
        }
    }

    /**
     * Get the text font.
     */
    public Font getTextFont() 
    {
        return textField.getFont();
    }

    /**
     * Set the text colour.
     */
    public void setTextColour( Color colour ) 
    {
        textField.setForeground( colour );
    }

    /**
     * Get the text colour.
     */
    public Color getTextColour() 
    {
        return textField.getForeground();
    }

    /**
     * Get the document model used by the JTextField.
     */
    public Document getDocument() 
    {
        return textField.getDocument();
    }

    /**
     * Set the tooltip for the text component.
     */
    public void setToolTipText( String tip ) 
    {
        textField.setToolTipText( tip );
        super.setToolTipText( tip );
    }

    /**
     * Set the number of columns displayed by the JTextField.
     */
    public void setColumns( int columns )
    {
        textField.setColumns( columns );
    }

//
// Implement listeners interface. Just a wrapper for JTextField
// ActionListener.
//
    protected EventListenerList listeners = new EventListenerList();

    /**
     * Registers a listener who wants to be informed about changes to
     * the character string.
     *
     *  @param l the ActionListener
     */
    public void addActionListener( ActionListener l ) 
    {
        listeners.add( ActionListener.class, l );
    }
    public void removeActionListener( ActionListener l ) 
    {
        listeners.remove( ActionListener.class, l );
    }

    /**
     * Send action event to all listeners.
     */
    protected void fireAction( ActionEvent e ) 
    {
        Object[] la = listeners.getListenerList();
        for ( int i = la.length - 2; i >= 0; i -= 2 ) {
            if ( la[i] == ActionListener.class ) {
                ((ActionListener)la[i+1]).actionPerformed( e );
            }
        }
    }
}
