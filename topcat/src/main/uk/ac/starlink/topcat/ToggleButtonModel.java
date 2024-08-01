package uk.ac.starlink.topcat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;

/**
 * Provides all information about a toggle button.  This is not only
 * it's current on/off status (selection state in swing talk), but
 * also the button's name, tooltip etc.  Swing doesn't provide a
 * model/action for this, so this class does it instead. 
 * Factory methods are provided to create Swing components that use
 * this as their model.
 *
 * @author   Mark Taylor
 * @since    3 Nov 2005
 */
public class ToggleButtonModel extends JToggleButton.ToggleButtonModel {

    private String text_;
    private Icon icon_;
    private String shortdesc_;
 
    /**
     * Constructor.
     *
     * @param   text  text to be used on buttons etc
     * @param   icon  icon to be used on buttons etc
     * @param   shortdesc  short description to be used for tool tips etc
     */
    @SuppressWarnings("this-escape")
    public ToggleButtonModel( String text, Icon icon, String shortdesc ) {
        setText( text );
        setIcon( icon );
        setDescription( shortdesc );
    }

    /**
     * Sets the text label associated with this model.
     *
     * @param  text  button name
     */
    public void setText( String text ) {
        text_ = text;
    }

    /**
     * Returns the text label associated with this model.
     *
     * @return  button name
     */
    public String getText() {
        return text_;
    }

    /**
     * Sets the icon associated with this model.
     *
     * @param  icon  button icon
     */
    public void setIcon( Icon icon ) {
        icon_ = icon;
    }

    /**
     * Returns the icon associated with this model.
     *
     * @return   button icon
     */
    public Icon getIcon() {
        return icon_;
    }

    /**
     * Sets the description (for tooltips etc) associated with this model.
     *
     * @param   shortdesc  description
     */
    public void setDescription( String shortdesc ) {
        shortdesc_ = shortdesc;
    }

    /**
     * Returns the description (for tooltips etc) associated with this model.
     *
     * @return  description
     */
    public String getDescription() {
        return shortdesc_;
    }

    /**
     * Creates and returns a normal button using this model.
     *
     * @return  button
     */
    public JToggleButton createButton() {
        JToggleButton button = new JToggleButton( text_, icon_ );
        button.setModel( this );
        button.setToolTipText( shortdesc_ );
        return button;
    }

    /**
     * Creates and returns a button suitable for use in a toolbar using
     * this model.   The button has no text.
     *
     * @return  button 
     */
    public JToggleButton createToolbarButton() {
        JToggleButton button = createButton();
        button.setText( null );
        return button;
    }

    /**
     * Creates and returns a menu item using this model.
     *
     * @return  checkbox menu item
     */
    public JCheckBoxMenuItem createMenuItem() {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem( text_, icon_ );
        menuItem.setModel( this );
        menuItem.setToolTipText( shortdesc_ );
        return menuItem;
    }

    /**
     * Creates and returns a check box using this model.
     *
     * @return  checkbox
     */
    public JCheckBox createCheckBox() {
        JCheckBox checkBox = new JCheckBox( text_ );
        checkBox.setModel( this );
        checkBox.setToolTipText( shortdesc_ );
        return checkBox;
    }

    /**
     * Creates and returns a pair of radio buttons using this model.
     * One unselects it, and the other selects it.
     *
     * @param  name0  name of the Off control
     * @param  name1  name of the On control
     * @return   array of (Off, On) controls
     */
    public JRadioButton[] createRadioButtons( String name0, String name1 ) {
        final JRadioButton butt0 = new JRadioButton( name0, ! isSelected() );
        JRadioButton butt1 = new JRadioButton( name1, isSelected() );
        butt1.setModel( this );
        butt0.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                ToggleButtonModel.this.fireActionPerformed( evt );
            }
        } );
        butt0.setToolTipText( "Off: " + shortdesc_ );
        butt1.setToolTipText( "On: " + shortdesc_ );
        ButtonGroup bgrp = new ButtonGroup();
        bgrp.add( butt0 );
        bgrp.add( butt1 );
        return new JRadioButton[] { butt0, butt1 };
    }

    /**
     * Sets the state of this model.
     * 
     * @param  state  on/off status
     */
    public void setSelected( boolean state ) {
        super.setSelected( state );
    }

    /**
     * Returns the state of this model.
     *
     * @return  on/off status
     */
    public boolean isSelected() {
        return super.isSelected();
    }

    @Override
    public void fireActionPerformed( ActionEvent evt ) {
        super.fireActionPerformed( evt );
    }
}
