package uk.ac.starlink.splat.iface;

import java.awt.Color;
import java.awt.GridBagConstraints;
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

/**
 * LineControls add a series of controls for showing and changing the
 * display properties of any drawn lines (the thickness, style and
 * colour).  
 * <p>
 * This class assumes that it will be laying out its components in a
 * GridBagLayout container of somekind. It adds the controls in a set
 * of incrementing rows, starting from given values.
 * <p>
 * Users of this class should register a ChangeListener to be informed
 * when the line properties are changed (get the actual value using
 * the get methods).
 *
 * @since $Date$
 * @since 13-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research
 * Councils 
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
     * Parent component.
     */
    protected JComponent parent = null;

    /**
     * Construct an instance filling component starting at 0,0.
     */
    public LineControls( JComponent parent ) 
    {
        initUI( parent, 0, 0 );
    }

    /**
     * Construct an instance filling component starting at given row
     * and column (column is fixed, row increments).
     */
    public LineControls( JComponent parent, int row, int column ) 
    {
        initUI( parent, row, column );
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
    private void initUI( JComponent parent, int row, int column ) 
    {
        this.parent = parent;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;

        //  Add components.
        gbc.gridx = column;
        gbc.gridy = row++;
        parent.add( thickBox, gbc );
        gbc.gridx = column;
        gbc.gridy = row++;
        parent.add( styleBox, gbc );
        gbc.gridx = column;
        gbc.gridy = row;
        parent.add( colourButton, gbc );

        //  Set the possible line thicknesses.
        for ( int i = 1; i < 21; i++ ) {
            thickBox.addItem( new Integer( i ) );
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
        thickBox.setSelectedItem( new Integer( thick ) );
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
        Color newColour = JColorChooser.showDialog( 
            parent, "Select Line Colour", colourIcon.getMainColour() );
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
