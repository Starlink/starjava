/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     03-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util.gui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * BasicFontChooser is a dialog for choosing one of the available
 * fonts. The size and style can also be selected. Usage follows that
 * of JDialog (i.e. use the .show() method to reveal, dialogs can be
 * modal or non-modal).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class BasicFontChooser
    extends JDialog
{
    /**
     * Whether selected font is accepted.
     */
    protected boolean accepted = false;

    /**
     * The dialog contentpane.
     */
    protected JPanel contentPane;

    /**
     * Label for font selector.
     */
    protected JLabel fontLabel = new JLabel();

    /**
     * Label for size selector.
     */
    protected JLabel sizeLabel = new JLabel();

    /**
     * Label for style selector.
     */
    protected JLabel styleLabel = new JLabel();

    /**
     * List of possible styles.
     */
    protected JComboBox<String> styleBox = new JComboBox<String>();

    /**
     * List of pre-selected sizes.
     */
    protected JComboBox<Integer> sizeBox = new JComboBox<Integer>();

    /**
     * List of all available fonts.
     */
    protected JComboBox<String> fontBox = new JComboBox<String>();

    /**
     * Display of the currently selected font.
     */
    protected JTextField fontDisplay = new JTextField();

    /**
     * Accept and exit button.
     */
    protected JButton okButton = new JButton();

    /**
     * Cancel and exit button.
     */
    protected JButton cancelButton = new JButton();

    /**
     * Selected font name
     */
    protected String currentFont = "Lucida Sans";

    /**
     * Selected font size.
     */
    protected int currentSize = 12;

    /**
     * Selected font style.
     */
    protected int currentStyle = Font.PLAIN;

    /**
     * Construct an instance with default configuration.
     */
    public BasicFontChooser()
    {
        this( "Font selection dialog" );
    }

    /**
     * Construct an instance using the given window title.
     */
    public BasicFontChooser( String title )
    {
        this( null, title, false );
    }

    /**
     * Construct an instance, setting the parent, window title and
     * whether the dialog is modal.
     */
    public BasicFontChooser( Frame owner, String title, boolean modal )
    {
        super( owner, title, modal );
        startup();
    }

    /**
     * Start common initialisation sequence.
     */
    protected void startup()
    {
        enableEvents( AWTEvent.WINDOW_EVENT_MASK );
        try {
            initUI();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        accepted = false;

        //  Set the initial font.
        fontBox.setSelectedItem( currentFont );
        sizeBox.setSelectedItem( Integer.valueOf( currentSize ) );
        styleBox.setSelectedItem( "PLAIN" );
    }

    /**
     * Initialise the user interface.
     */
    private void initUI() throws Exception
    {
        //  Get the dialog content pane and set the layout manager.
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new GridBagLayout() );

        //  Set dialog size.
        setSize( new Dimension( 500, 150 ) );

        fontLabel.setHorizontalAlignment( SwingConstants.CENTER );
        fontLabel.setText( "Font" );

        sizeLabel.setHorizontalAlignment( SwingConstants.CENTER );
        sizeLabel.setText( "Size" );

        styleLabel.setHorizontalAlignment( SwingConstants.CENTER );
        styleLabel.setText( "Style" );

        fontDisplay.setToolTipText( "Selected font" );
        fontDisplay.setHorizontalAlignment( SwingConstants.CENTER );
        fontDisplay.setText( "the quick brown fox" );

        //  Add all components to the content pane.
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets( 2, 0, 0, 2 );
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        contentPane.add( fontLabel, gbc );

        gbc.gridx = 1;
        contentPane.add( sizeLabel, gbc );

        gbc.gridx = 2;
        contentPane.add( styleLabel, gbc );

        gbc.gridx = 0;
        gbc.gridy = 1;
        contentPane.add( fontBox, gbc );

        gbc.gridx = 1;
        contentPane.add( sizeBox, gbc );

        gbc.gridx = 2;
        contentPane.add( styleBox, gbc );

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        contentPane.add( fontDisplay, gbc );

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        contentPane.add( okButton, gbc );

        gbc.gridx = 2;
        gbc.gridy = 3;
        contentPane.add( cancelButton, gbc );

        //  Set the fonts that we will use.
        addFonts();

        //  Set the possible styles.
        styleBox.addItem( "PLAIN" );
        styleBox.addItem( "BOLD" );
        styleBox.addItem( "ITALIC" );
        styleBox.addItem( "BOLD & ITALIC" );

        //  And a quick set of sizes.
        for ( int i = 8; i <= 32; i++ ) {
            sizeBox.addItem( Integer.valueOf( i ) );
        }

        //  Finally set all action responses (after setting possible values).
        fontBox.addItemListener( new ItemListener() {
                public void itemStateChanged( ItemEvent e ) {
                    setFontName();
                }
            });

        sizeBox.setEditable( true );
        sizeBox.addItemListener( new ItemListener() {
                public void itemStateChanged( ItemEvent e ) {
                    setSize();
                }
            });

        styleBox.addItemListener( new ItemListener() {
                public void itemStateChanged( ItemEvent e ) {
                    setStyle();
                }
            });

        //  Set various close window buttons.
        okButton.setText( "OK" );
        okButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    closeWindow( true );
                }
            });
        cancelButton.setText( "Cancel" );
        cancelButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    closeWindow( false );
                }
            });
    }

    /**
     * Add all the available fonts.
     */
    protected void addFonts()
    {
        GraphicsEnvironment gEnv =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        String envfonts[] = gEnv.getAvailableFontFamilyNames();
        for ( int i = 1; i < envfonts.length; i++ ) {
            fontBox.addItem( envfonts[i] );
        }
    }

    /**
     * Update the display to reflect the new font, size or style.
     */
    protected void updateDisplay()
    {
        Font newFont = new Font( currentFont,
                                 currentStyle,
                                 currentSize );
        fontDisplay.setFont( newFont );
    }


    /**
     * Set a new default font name from the value in the font name
     * combobox.
     */
    protected void setFontName()
    {
        Object fontObj = fontBox.getSelectedItem();
        currentFont = fontObj.toString();
        updateDisplay();
    }

    /**
     * Set a new default font size from the value in the size
     * combobox.
     */
    protected void setSize()
    {
        Object sizeObj = sizeBox.getSelectedItem();
        if ( sizeObj instanceof Integer ) {
            currentSize = ((Integer) sizeObj).intValue();
        } else {
            //  Not an Integer so get string and convert.
            currentSize = Integer.parseInt( sizeObj.toString() );
        }
        updateDisplay();
    }

    /**
     * Set the font style from the value in the style combobox.
     */
    protected void setStyle()
    {
        String newStyle = styleBox.getSelectedItem().toString();
        if ( newStyle.equals( "PLAIN" ) ) {
            currentStyle = Font.PLAIN;
        } else if ( newStyle.equals( "BOLD" ) ) {
            currentStyle = Font.BOLD;
        } else if ( newStyle.equals( "ITALIC" ) ) {
            currentStyle = Font.ITALIC;
        } else {
            currentStyle = Font.BOLD | Font.ITALIC;
        }
        updateDisplay();
    }

    /**
     * Get the selected font.
     */
    public Font getSelectedFont()
    {
        return new Font( currentFont, currentStyle, currentSize );
    }

    /**
     * Return the exit status of the dialog. The selected font should
     * only be used if this returns true.
     */
    public boolean accepted()
    {
        return accepted;
    }

    /**
     * Close the window. If argument is true then it is OK to return
     * selected font.
     */
    protected void closeWindow( boolean accepted )
    {
        this.accepted = accepted;
        setVisible( false );
    }
}
