/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     13-NOV-2000 (Peter W. Draper):
 *        Original version.
 *     18-FEB-2004 (Peter W. Draper):
 *        Changed to use a GridBagLayouter.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * LineControls add a series of controls for showing and changing the
 * display properties of any drawn lines (the thickness, style and
 * colour).  
 * <p>
 * This class assumes that it will be laying out its components as part of a
 * GridBagLayouter scheme. It adds the controls in a set of incrementing rows,
 * starting from the current GridBagLayouter position.
 * <p>
 * Users of this class should register a ChangeListener to be informed
 * when the line properties are changed (get the actual value using
 * the get methods).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class LineControls 
{
    /**
     * Colour button.
     */
    protected JButton colourButton = new JButton();

    /**
     * Colour Icon of colour button.
     */
    protected ColourIcon colourIcon = new ColourIcon( Color.black );

    /**
     * Style selection.
     */
    protected AstStyleBox styleBox = new AstStyleBox();

    /**
     * List of pre-selected thicknesses.
     */
    protected JComboBox thickBox = new JComboBox();

    /**
     * The GridBagLayouter. Used to get parent component access.
     */
    protected GridBagLayouter layouter = null;

    /**
     * Construct an instance using the given GridBagLayouter to arrange the
     * components. Use the given postfix to qualify the standard labels.
     */
    public LineControls( GridBagLayouter layouter, String postfix ) 
    {
        initUI( layouter, postfix );
    }

    /**
     * Reset controls to defaults.
     */
    public void reset() 
    {
        setColour( Color.black );
        setThick( 1 );
        setStyle( DefaultGrf.PLAIN );
    }

    /**
     * Initialise the user interface.
     */
    private void initUI( GridBagLayouter layouter, String postfix ) 
    {
        this.layouter = layouter;

        layouter.add( "Thickness " + postfix + ":", false );
        layouter.add( thickBox, false );
        layouter.eatLine();
        layouter.add( "Style " + postfix + ":", false );
        layouter.add( styleBox, false );
        layouter.eatLine();
        layouter.add( "Colour " + postfix + ":", false );
        layouter.add( colourButton, false );
        layouter.eatLine();

        //  Set the possible line thicknesses.
        for ( int i = 1; i < 21; i++ ) {
            thickBox.addItem( Integer.valueOf( i ) );
        }

        //  Finally set all action responses (after setting possible values).
        thickBox.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    changeThick();
                }
            });

        styleBox.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    changeStyle();
                }
            });


        //  Set ColourIcon of colour button.
        colourButton.setIcon( colourIcon );
        colourButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                chooseColour();
            }
        });

        //  Add tooltip text.
        thickBox.setToolTipText( "Select a thickness for line" );
        styleBox.setToolTipText( "Select a style for line" );
        colourButton.setToolTipText( "Select a colour for line" );
    }

    /**
     * Send event to signal a line thickness change event.
     */
    protected void changeThick() 
    {
        fireChanged();
    }

    /**
     * Set the current line thickness.
     */
    public void setThick( int thick ) 
    {
        thickBox.setSelectedItem( Integer.valueOf( thick ) );
    }

    /**
     * Return the current line thickness.
     */
    public int getThick() 
    {
        Integer thick = (Integer) thickBox.getSelectedItem();
        return thick.intValue();
    }

    /**
     * Send event to signal the line style has changed.
     */
    protected void changeStyle() 
    {
        fireChanged();
    }

    /**
     * Set the current line style (as AST integer).
     */
    public void setStyle( int style ) 
    {
        styleBox.setSelectedStyle( style );
    }

    /**
     * Return the current line style (as AST integer).
     */
    public int getStyle() 
    {
        return styleBox.getSelectedStyle();
    }

    /**
     * Create a dialog to select a new colour.
     */
    protected void chooseColour() 
    {
        Color newColour = 
            JColorChooser.showDialog( layouter.getContainer(), 
                                      "Select line colour", 
                                      colourIcon.getMainColour() );
        if ( newColour != null ) {
            colourIcon.setMainColour( newColour );
            fireChanged();
        }
    }

    /**
     * Return the current colour.
     */
    public Color getColour() 
    {
        return colourIcon.getMainColour();
    }

    /**
     * Set the current colour.
     */
    public void setColour( Color colour ) 
    {
        colourIcon.setMainColour( colour );
        colourButton.repaint();
    }

//
//  Define listeners interface.
//
    protected EventListenerList listeners = new EventListenerList();

    /**
     * Registers a listener who wants to be informed about changes.
     *
     *  @param l the ChangeListener listener.
     */
    public void addChangeListener( ChangeListener l ) 
    {
        listeners.add( ChangeListener.class, l );
    }

    /**
     * Send ChangeEvent event to all listeners.
     */
    protected void fireChanged() 
    {
        Object[] la = listeners.getListenerList();
        ChangeEvent e = null;
        for ( int i = la.length - 2; i >= 0; i -= 2 ) {
            if ( la[i] == ChangeListener.class ) {
                if ( e == null ) {
                    e = new ChangeEvent( this );
                }
                ((ChangeListener)la[i+1]).stateChanged( e );
            }
        }
    }
}
