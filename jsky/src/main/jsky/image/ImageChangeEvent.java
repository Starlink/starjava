/*
 * ESO Archive
 *
 * $Id: ImageChangeEvent.java,v 1.5 2002/07/09 13:30:36 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/11/17  Created
 */

package jsky.image;

import javax.swing.event.ChangeEvent;

/**
 * Used to describe image change events such as loading a new image,
 * setting a new origin or scale.
 *
 * @version $Revision: 1.5 $
 * @author Allan Brighton
 */
public class ImageChangeEvent extends ChangeEvent {

    /** Bitmask for boolean values */
    protected int val;

    /** Bit set if a new image was (or will be) loaded */
    protected static final int NEW_IMAGE = 1 << 0;

    /** Bit set before a new image is loaded and cleared afterwards */
    protected static final int BEFORE = 1 << 1;

    /** Bit set if a new scale value was set */
    protected static final int NEW_SCALE = 1 << 2;

    /** Bit set if a new rotation angle value was set */
    protected static final int NEW_ANGLE = 1 << 3;

    /** Bit set if the origin changed */
    protected static final int NEW_ORIGIN = 1 << 4;

    /** Bit set if a new colormap was set */
    protected static final int NEW_COLORMAP = 1 << 5;

    /** Bit set if new cut levels were set */
    protected static final int NEW_CUT_LEVELS = 1 << 6;

    /** Bit set if the image file was edited (such as adding or deleting a FITS table) */
    protected static final int EDIT_STATE_CHANGED = 1 << 7;

    /** Bit set if the image data was modified so that the image display should be updated */
    protected static final int IMAGE_DATA_CHANGED = 1 << 8;


    /**
     * Create a new image change event.
     *
     * @param source the source (sending) object.
     */
    public ImageChangeEvent(Object source) {
        super(source);
    }


    /**
     * Return true if a new image was (or will be) loaded.
     * If isBefore() returns true, it is before loading the image,
     * otherwise after.
     */
    public boolean isNewImage() {
        return (val & NEW_IMAGE) != 0;
    }

    /**
     * Used with isNewImage(), returns true if a new image will be loaded,
     * and false if it was already loaded.
     */
    public boolean isBefore() {
        return (val & BEFORE) != 0;
    }

    /** Return true if the image scale (magnification) was changed */
    public boolean isNewScale() {
        return (val & NEW_SCALE) != 0;
    }

    /** Return true if the image rotation angle was changed */
    public boolean isNewAngle() {
        return (val & NEW_ANGLE) != 0;
    }

    /** Return true if the visible image origin changed (by panning) */
    public boolean isNewOrigin() {
        return (val & NEW_ORIGIN) != 0;
    }

    /** Return true if the image colormap changed */
    public boolean isNewColormap() {
        return (val & NEW_COLORMAP) != 0;
    }

    /** Return true if the image cut levels changed */
    public boolean isNewCutLevels() {
        return (val & NEW_CUT_LEVELS) != 0;
    }

    /** Return true if the edited state of the image file changed */
    public boolean isEditStateChanged() {
        return (val & EDIT_STATE_CHANGED) != 0;
    }

    /** Return true if the image data was changed */
    public boolean isImageDataChanged() {
        return (val & IMAGE_DATA_CHANGED) != 0;
    }

    public void setNewImage(boolean b) {
        if (b) val |= NEW_IMAGE; else val &= ~NEW_IMAGE;
    }

    public void setBefore(boolean b) {
        if (b) val |= BEFORE; else val &= ~BEFORE;
    }

    public void setNewScale(boolean b) {
        if (b) val |= NEW_SCALE; else val &= ~NEW_SCALE;
    }

    public void setNewAngle(boolean b) {
        if (b) val |= NEW_ANGLE; else val &= ~NEW_ANGLE;
    }

    public void setNewOrigin(boolean b) {
        if (b) val |= NEW_ORIGIN; else val &= ~NEW_ORIGIN;
    }

    public void setNewColormap(boolean b) {
        if (b) val |= NEW_COLORMAP; else val &= ~NEW_COLORMAP;
    }

    public void setNewCutLevels(boolean b) {
        if (b) val |= NEW_CUT_LEVELS; else val &= ~NEW_CUT_LEVELS;
    }

    public void setEditStateChanged(boolean b) {
        if (b) val |= EDIT_STATE_CHANGED; else val &= ~EDIT_STATE_CHANGED;
    }

    public void setImageDataChanged(boolean b) {
        if (b) val |= IMAGE_DATA_CHANGED; else val &= ~IMAGE_DATA_CHANGED;
    }

    /** Reset all fields to the default values */
    public void reset() {
        val = 0;
    }
}
