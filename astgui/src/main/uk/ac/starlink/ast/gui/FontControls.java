/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-NOV-2000 (Peter W. Draper):
 *        Original version.
 *     18-FEB-2004 (Peter W. Draper):
 *        Modified to use GridBagLayouter.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.event.EventListenerList;

import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * FontControls add a series of controls for selecting from the
 * available families of fonts and assigning a size and style.
 * <p>
 * This class assumes that it will be laying out its components using
 * the standard GridBagLayouter.
 * <p>
 * Users of this class should implement the FontChangedListener
 * interface to be informed when the selected font is updated.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class FontControls 
{
    /**
     * List of possible styles.
     */
    protected JComboBox styleBox = new JComboBox();

    /**
     * List of pre-selected sizes.
     */
    protected JComboBox sizeBox = new JComboBox();

    /**
     * List of all available font families.
     */
    protected JComboBox fontBox = new JComboBox();

    /**
     * Selected font name
     */
    protected String currentFont;

    /**
     * Selected font size.
     */
    protected int currentSize;

    /**
     * Selected font style.
     */
    protected int currentStyle;

    /**
     * Styles as formatted string.
     */
    protected static final String[] styleStrings = {
        "PLAIN", "BOLD", "ITALIC", "BOLD & ITALIC"
    };

    /**
     * Styles as indexed array.
     */
    protected static final int[] styleInts = {
        Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC,
    };

    /**
     * Construct an instance. Add the postfix to the standard labels
     * (set to "" for none).
     */
    public FontControls( GridBagLayouter layouter, String postfix ) 
    {
        initUI( layouter, postfix );
    }

    /**
     * Set/reset interface to default values.
     */
    public void setDefaults() 
    {
        currentFont = "Lucida Sans";
        currentSize = 12;
        currentStyle = Font.PLAIN;
        fontBox.setSelectedItem( currentFont );
        sizeBox.setSelectedItem( Integer.valueOf( currentSize ) );
        styleBox.setSelectedItem( styleStrings[styleInts[currentStyle]] );
    }

    /**
     * Initialise the user interface.
     */
    private void initUI( GridBagLayouter layouter, String postfix )
    {
        layouter.add( "Font:", false );
        layouter.add( fontBox, false );
        layouter.eatLine();

        layouter.add( "Style:", false );
        layouter.add( styleBox, false );
        layouter.eatLine();

        layouter.add( "Size:", false );
        layouter.add( sizeBox, false );
        layouter.eatLine();

        //  Set the fonts that we will use.
        addFonts();

        //  Set the possible styles.
        for ( int i = 0; i < styleStrings.length; i++ ) {
            styleBox.addItem( styleStrings[i] );
        }

        //  And a quick set of sizes.
        for ( int i = 8; i <= 32; i++ ) {
            sizeBox.addItem( Integer.valueOf( i ) );
        }

        //  Set the initial font selection.
        setDefaults();

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

        //  Add tooltip text.
        fontBox.setToolTipText( "Select a font from all known" );
        sizeBox.setToolTipText( "Set the font size" );
        styleBox.setToolTipText( "Select a font style" );
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
     * Set a new default font name from the value in the font name
     * combobox.
     */
    protected void setFontName() 
    {
        Object fontObj = fontBox.getSelectedItem();
        currentFont = fontObj.toString();
        fireChanged();
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
        fireChanged();
    }

    /**
     * Set the font style from the value in the style combobox.
     */
    protected void setStyle() 
    {
        String newStyle = styleBox.getSelectedItem().toString();
        for ( int i = 0; i < styleStrings.length; i++ ) {
            if ( newStyle.equals( styleStrings[i] ) ) {
                currentStyle = styleInts[i];
                break;
            }
        }
        fireChanged();
    }

    /**
     * Get the selected font.
     */
    public Font getSelectedFont() 
    {
        return new Font( currentFont, currentStyle, currentSize );
    }

    /**
     * Set the control to match a given font. If null then the default
     * font is used.
     */
    public void setFont( Font font ) 
    {
        if ( font != null ) {
            currentFont = font.getFamily();
            fontBox.setSelectedItem( currentFont );
            currentStyle = font.getStyle();
            styleBox.setSelectedItem( styleStrings[styleInts[currentStyle]] );
            currentSize = font.getSize();
            sizeBox.setSelectedItem( Integer.valueOf( currentSize ) );
        }
        else {
            setDefaults();
        }
    }

//
//  Define listeners interface.
//
    protected EventListenerList listeners = new EventListenerList();

    /**
     * Registers a listener who wants to be informed about font
     * changes.
     *
     *  @param l the FontChangedListener listener.
     */
    public void addListener( FontChangedListener l ) 
    {
        listeners.add( FontChangedListener.class, l );
    }

    /**
     * Send FontChangedEvent event to all listeners.
     *
     */
    protected void fireChanged() 
    {
        Object[] la = listeners.getListenerList();
        FontChangedEvent e = null;
        for ( int i = la.length - 2; i >= 0; i -= 2 ) {
            if ( la[i] == FontChangedListener.class ) {
                if ( e == null ) {
                    e = new FontChangedEvent( this, getSelectedFont() );
                }
                ((FontChangedListener)la[i+1]).fontChanged( e );
            }
        }
    }
}
