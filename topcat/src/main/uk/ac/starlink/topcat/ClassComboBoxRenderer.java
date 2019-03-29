package uk.ac.starlink.topcat;

import uk.ac.starlink.util.gui.CustomComboBoxRenderer;

/**
 * Custom combo box renderer for rendering Class objects.
 *
 * @author   Mark Taylor (Starlink)
 * @since    17 Aug 2004
 */
public class ClassComboBoxRenderer extends CustomComboBoxRenderer<Class> {

    /**
     * Constructs a renderer with a given null representation.
     *
     * @param  nullTxt   representation of a null class
     */
    public ClassComboBoxRenderer( String nullTxt ) {
        super( Class.class, nullTxt );
    }

    @Override
    protected String mapValue( Class clazz ) {
        String rep = clazz.getName();
        return rep.substring( rep.lastIndexOf( '.' ) + 1 );
    }
}
