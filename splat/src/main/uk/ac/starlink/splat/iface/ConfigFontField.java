package uk.ac.starlink.splat.iface;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.ast.gui.SelectCharactersListener;
import uk.ac.starlink.ast.gui.SelectCharacters;
import uk.ac.starlink.ast.gui.SelectCharactersEvent;
import uk.ac.starlink.ast.gui.ColourIcon;

/**
 * A ConfigTextField contains a labbelled JTextField and controls for
 * picking a system font, for choosing amongst all the available
 * characters in the font (so that special characters can be used,
 * such as greek etc.) and for selecting a colour. The JTextField
 * contents are rendered using the selected font and colour.
 * @since $Date$
 * @since 06-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research
 *            Councils
*/
public class ConfigFontField extends JPanel
    implements SelectCharactersListener
{
    /**
     * The text entry field.
     */
    protected JTextField textField = new JTextField();

    /**
     * Label for text field description.
     */
    protected JLabel textLabel = new JLabel();

    /**
     * Button to initiate selecting a font.
     */
    protected JButton fontButton = new JButton();

    /**
     * Button to initiate selecting special characters.
     */
    protected JButton charButton = new JButton();

    /**
     * Button to initiate colour selection.
     */
    protected JButton colourButton = new JButton();

    /**
     * ColourIcon of button.
     */
    protected ColourIcon colourIcon =
        new ColourIcon( Color.black, 13, 13, Color.black, 2 );

    /**
     * Parent frame (must be set).
     */
    protected JFrame parentFrame = null;

    /**
     * The font chooser.
     */
    protected JFontChooser fontChooser = null;

    /**
     * The special character chooser.
     */
    protected SelectCharacters charChooser = null;

    /**
     * Create an instance.
     *
     * @param parentFrame the JFrame that contains this widget.
     * @param label description of the text field.
     */
    public ConfigFontField( JFrame parentFrame, String label )
    {
        this.parentFrame = parentFrame;
        initUI( label );
    }

    /**
     * Create an instance with initial text field.
     *
     * @param parentFrame the JFrame that contains this widget.
     * @param label description of the text field.
     * @param text default text for the text field.
     */
    public ConfigFontField( JFrame parentFrame, String label,
                            String text )
    {
        this.parentFrame = parentFrame;
        initUI( label );
        setText( text );
    }

    /**
     * Initialise the user interface.
     */
    protected void initUI( String label )
    {
        //  Initialise any components.
        textLabel.setText( label );
        setLayout( new GridBagLayout() );

        ImageIcon fontIcon = new ImageIcon(
            ImageHolder.class.getResource( "font.gif" ) );
        ImageIcon specialIcon = new ImageIcon(
            ImageHolder.class.getResource( "special.gif" ) );
        colourButton.setIcon( colourIcon );
        fontButton.setIcon( fontIcon );
        charButton.setIcon( specialIcon );

        //  Add components.
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        add( textLabel, gbc );

        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 4;
        gbc.gridx = 1;
        gbc.gridy = 0;
        add( textField, gbc );

        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;

        gbc.gridx = 1;
        gbc.gridy = 1;
        add( fontButton, gbc );

        gbc.gridx = 2;
        gbc.gridy = 1;
        add( charButton, gbc );

        gbc.gridx = 3;
        gbc.gridy = 1;
        add( colourButton, gbc );

        //  Set tooltips.
        fontButton.setToolTipText( "Select a new font" );
        charButton.setToolTipText( "Select special characters from font" );
        colourButton.setToolTipText( "Select a colour for the font" );

        //  Add actions to respond to the buttons.
        fontButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    chooseFont();
                }
            });
        colourButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    chooseColour();
                }
            });
        charButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    chooseChars();
                }
            });
    }

    /**
     * Choose the text font.
     */
    protected void chooseFont()
    {
        if ( fontChooser == null ) {
            fontChooser = new JFontChooser( parentFrame,
                                            "Selector New Font", true );
        }
        fontChooser.show();
        if ( fontChooser.accepted() ) {
            Font newFont = fontChooser.getSelectedFont();
            setTextFont( newFont );
        }
    }

    /**
     * Choose the text colour.
     */
    protected void chooseColour()
    {
        Color newColour = JColorChooser.showDialog(
            parentFrame, "Select Text Colour", colourIcon.getMainColour() );
        if ( newColour != null ) {
            setTextColour( newColour );
        }
    }

    /**
     * Choose a special character phase to append.
     */
    protected void chooseChars()
    {
        if ( charChooser == null ) {
            charChooser = new SelectCharacters( getTextFont() );
            charChooser.addListener( this );
        } else {
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
        colourIcon.setMainColour( colour );
        textField.setForeground( colour );
    }

    /**
     * Get the text colour.
     */
    public Color getTextColour()
    {
        return textField.getForeground();
    }
}
