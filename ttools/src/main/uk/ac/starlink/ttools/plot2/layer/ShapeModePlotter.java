package uk.ac.starlink.ttools.plot2.layer;

/**
 * ModePlotter subclass with ShapeForm for form and ShapeMode for mode.
 *
 * @author   Mark Taylor
 * @since    26 Jan 2021
 */
public interface ShapeModePlotter extends ModePlotter<ShapeStyle> {
    ShapeForm getForm();
    ShapeMode getMode();
}
