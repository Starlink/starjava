package uk.ac.starlink.splat.iface;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Create controls for setting the colour of a named properties of a
 * graphics component. The Color is stored in a ColourStore object
 * that provides persistence and maintains a list of listeners for
 * changes in the colour.
 *
 * @since $Date$
 * @since 14-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 */
public class ComponentColourControls
    extends JPanel
    implements ChangeListener
{
    /**
     * The component that we're controlling.
     */
    protected JComponent component = null;

    /**
     * The method that corresponds to the property we need to set.
     */
    protected Method target = null;

    /**
     * The property name (trailing part of setXXXX).
     */
    protected String property = null;

    /**
     * The description of the property.
     */
    protected String description = null;

    /**
     * Colour Icon of colour button, shows the current colour.
     */
    protected ColourIcon colourIcon = new ColourIcon( Color.black );

    /**
     * The button used to request the colour dialog.
     */
    protected JButton colourButton = new JButton();

    /**
     * Label showing the description of the property.
     */
    protected JLabel colourLabel = new JLabel();

    /**
     * ColourStore that provides change notifications and permanent
     * store.
     */
    protected ColourStore colourStore = null;

    /**
     * The original colour of the component. Kept for resetting.
     */
    protected Color[] originalColour = new Color[1];

    /**
     * Create an instance. Requires the component to colour, which
     * property to colour and a short description of the property
     * (this is shown in a label).
     *
     * @param component the component that is to be coloured.
     * @param colourStore the storage for the actual Colour. This is
     *                    also stored in the component itself.
     * @param property the component property that is to be
     *                 coloured. Must have a "setter" and a "getter"
     *                 method with this name (i.e. this property
     *                 would be "Background" for the "setBackground"
     *                 method of all JComponents).
     * @param description a short description of the property.
     *
     * @exception throws a RuntimeException if the getter or setter
     *            methods implied by the property are not accessable.
     */
    public ComponentColourControls( JComponent component,
                                    ColourStore colourStore,
                                    String property,
                                    String description )
        throws RuntimeException
    {
        initUI();
        setComponent( component );
        setColourStore( colourStore );
        setProperty( property );
        setDescription( description );
    }


    /**
     * Place the UI components.
     */
    protected void initUI()
    {
        colourButton.setIcon( colourIcon );
        colourButton.setToolTipText( "Press to select a new colour" );
        colourButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    chooseColour();
                }
            });

        setLayout( new GridBagLayout() );
        GridBagConstraints gbc = new GridBagConstraints();
        Insets labelInsets = new Insets( 10, 5, 5, 10 );
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = labelInsets;
        gbc.fill = GridBagConstraints.NONE;

        add( colourLabel, gbc );
        add( colourButton, gbc );

        //  Eat up spare horizontal space.
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        Component fillx = Box.createHorizontalStrut( 5 );
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add( fillx, gbc );

        //  Eat up all spare vertical space (pushes widgets to top).
        Component filly = Box.createVerticalStrut( 5 );
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        add( filly, gbc );
    }

    /**
     * Reset the colour to original.
     */
    public void reset()
    {
        try {
            colourStore.setColor( originalColour[0] );
        }
        catch (Exception e) {
            //  Tough, do nothing.
        }
    }

    /**
     * Set the component that we're managing.
     */
    public void setComponent( JComponent component )
    {
        this.component = component;
    }

    /**
     * Set the ColourStore used to store the colour.
     */
    public void setColourStore( ColourStore colourStore )
    {
        this.colourStore = colourStore;
        colourStore.addChangeListener( this );
    }

    /**
     * Get the ColourStore.
     */
    public ColourStore getColourStore()
    {
        return this.colourStore;
    }

    /**
     * Get the component that we're managing.
     */
    public JComponent getComponent()
    {
        return component;
    }

    /**
     * Set the name of the colour related property that we're managing.
     */
    public void setProperty( String property ) throws RuntimeException
    {
        try {

            //  Look for the setter method.
            String name = "set" + property;
            Class[] args = { Color.class }; // One arg of Color.
            target = component.getClass().getMethod( name, args );
            this.property = property;

            //  And the getter method.
            name = "get" + property;
            Method getTarget = component.getClass().getMethod( name, null );
            originalColour[0] = (Color)getTarget.invoke( component, null );

            //  Got this far so all must work (getter + setter).
            colourStore.setColor( originalColour[0] );
        }
        catch (Exception e) {
            throw new RuntimeException( e.getMessage() );
        }
    }

    /**
     * Get the name of the colour property that we're managing.
     */
    public String getProperty()
    {
        return property;
    }

    /**
     * Set the description used in the label.
     */
    public void setDescription( String description )
    {
        this.description = description;
        colourLabel.setText( description );
    }

    /**
     * Get the description used in the label.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Choose a colour for the component using a chooser dialog.
     */
    protected void chooseColour()
    {
        Color[] newColour = new Color[1];
        newColour[0] = JColorChooser.showDialog( component, "Select Colour",
                                                 colourIcon.getMainColour() );
        if ( newColour != null ) {
            colourStore.setColor( newColour[0] );
        }
    }

    /**
     * Update the component colour to match the one in store.
     */
    public void matchColour()
    {
        try {
            Color[] colourArray = new Color[1];
            colourArray[0] = colourStore.getColour();
            target.invoke( component, colourArray );
            colourIcon.setMainColour( colourArray[0] );
        }
        catch (Exception e) {
            // Do nothing.
        }
    }

//
// ChangeListener interface.
//
    /**
     * Listen for ColourStore issuing ChangeEvents.
     */
    public void stateChanged( ChangeEvent e )
    {
        matchColour();
    }
}
