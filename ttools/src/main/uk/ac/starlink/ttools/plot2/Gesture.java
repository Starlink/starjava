package uk.ac.starlink.ttools.plot2;

import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;

/**
 * Enumerates mouse gestures used to perform navigation actions.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2014
 */
public class Gesture {

    private final String name_;
    private final Icon icon_;
    private final String description_;

    /** Drag using mouse button 1. */
    public static final Gesture DRAG_1 =
        new Gesture( "Drag 1", ResourceIcon.DRAG1, "Drag with button 1" );

    /** Drag using mouse button 2. */
    public static final Gesture DRAG_2 =
        new Gesture( "Drag 2", ResourceIcon.DRAG2, "Drag with button 2" );

    /** Drag using mouse button 3. */
    public static final Gesture DRAG_3 =
        new Gesture( "Drag 3", ResourceIcon.DRAG3, "Drag with button 3" );

    /** Click mouse button 1. */
    public static final Gesture CLICK_1 =
        new Gesture( "Click 1", ResourceIcon.CLICK1, "Click button 1" );

    /** Click mouse button 2. */
    public static final Gesture CLICK_2 =
        new Gesture( "Click 2", ResourceIcon.CLICK2, "Click button 2" );

    /** Click mouse button 3. */
    public static final Gesture CLICK_3 =
        new Gesture( "Click 3", ResourceIcon.CLICK3, "Click button 3" );

    /** Rotate mouse wheel. */
    public static final Gesture WHEEL =
        new Gesture( "Wheel", ResourceIcon.MOUSE_WHEEL, "Mouse wheel" );

    /**
     * Constructor.
     *
     * @param   name  gesture name (short)
     * @param   icon  small icon (preferably 12 pixels high)
     * @param   description  gesture description
     */
    public Gesture( String name, Icon icon, String description ) {
        name_ = name;
        icon_ = icon;
        description_ = description;
    }

    /**
     * Returns a short name for this gesture.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a small icon for this gesture.
     *
     * @return  icon
     */
    public Icon getIcon() {
        return icon_;
    }

    /**
     * Returns a description for this gesture.
     *
     * @return  description, appropriate for tool tip etc
     */
    public String getDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }
}
