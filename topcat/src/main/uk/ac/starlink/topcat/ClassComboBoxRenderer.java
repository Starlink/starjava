package uk.ac.starlink.topcat;

/**
 * Custom combo box renderer for rendering Class objects.
 *
 * @author   Mark Taylor (Starlink)
 * @since    17 Aug 2004
 */
public class ClassComboBoxRenderer extends CustomComboBoxRenderer {

    private static ClassComboBoxRenderer instance = new ClassComboBoxRenderer();

    /**
     * Returns an instance of this singleton class.
     *
     * @return  renderer instance
     */
    public static ClassComboBoxRenderer getInstance() {
        return instance;
    }

    protected Object mapValue( Object value ) {
        if ( value instanceof Class ) {
            Class clazz = (Class) value;
            String rep = clazz.getName();
            return rep.substring( rep.lastIndexOf( '.' ) + 1 );
        }
        else {
            return value;
        }
    }
}
