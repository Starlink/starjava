package uk.ac.starlink.splat.iface;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.ast.gui.SelectCharacters;
import uk.ac.starlink.ast.gui.SelectCharactersListener;
import uk.ac.starlink.ast.gui.SelectCharactersEvent;


/**
 * A SpecialTextField contains a JTextField and a button for
 * choosing amongst all the available characters in the font (so that
 * special characters can be used, and for selectin
 *
 * @since $Date$
 * @since 06-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research
 *            Councils
*/
public class ConfigTextField extends JPanel
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
     * Parent frame (must be set).
     */
    JFrame parentFrame = null;

    /**
     * The special character chooser.
     */
    SelectCharacters charChooser = null;

    /**
     * Create an instance.
     *
     * @param parentFrame the JFrame that contains this widget.
     */
    public ConfigTextField( JFrame parentFrame )
    {
        this.parentFrame = parentFrame;
        initUI();
    }

    /**
     * Create an instance with initial text field.
     *
     * @param parentFrame the JFrame that contains this widget.
     * @param text default text for the text field.
     */
    public ConfigTextField( JFrame parentFrame, String text )
    {
        this.parentFrame = parentFrame;
        initUI();
        setText( text );
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

        //  Add components.
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add( textField, gbc );

        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridx = 1;
        gbc.gridy = 0;
        add( charButton, gbc );

        //  Set tooltips.
        charButton.setToolTipText( "Select any character from font" );

        //  Add action to respond to the button.
        charButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    chooseChars();
                }
            });
    }

    /**
     * Choose a special character phase to append.
     */
    protected void chooseChars()
    {
        if ( charChooser == null ) {
            charChooser = new SelectCharacters( getFont() );
            charChooser.addListener( this );
        } else {
            charChooser.setDisplayFont( getFont() );
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
}
