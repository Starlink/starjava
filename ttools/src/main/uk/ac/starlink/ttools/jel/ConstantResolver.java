package uk.ac.starlink.ttools.jel;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import gnu.jel.DVMap;

/**
 * DVMap implementation that can reference values from a supplied
 * Map of Constant values.
 *
 * <p>Primitives, strings and objects are currently catered for.
 *
 * @author   Mark Taylor
 * @since    10 Jul 2025
 */
public class ConstantResolver extends DVMap {

    private final Map<String,? extends Constant<?>> constMap_;
    private final Set<Constant<?>> translatedConsts_;

    /**
     * Constructor.
     * The supplied map of constants may change its content over the
     * lifetime of this object, resolution will correspond to its state
     * at resolution time.
     *
     * @param  constMap  map by name of Constant objects
     */
    public ConstantResolver( Map<String,? extends Constant<?>> constMap ) {
        constMap_ = constMap;
        translatedConsts_ = new LinkedHashSet<Constant<?>>();
    }

    /**
     * Returns this resolver's map of known constants.
     *
     * @return  map by name of Constant objects
     */
    public Map<String,? extends Constant<?>> getConstantMap() {
        return constMap_;
    }

    /**
     * Returns a collection of those constants that have been referenced
     * in expressions compiled with use of this resolver.
     *
     * @return  set of referenced constants
     */
    public Set<Constant<?>> getTranslatedConstants() {
        return translatedConsts_;
    }

    @Override
    public String getTypeName( String name ) {
        Constant<?> konst = constMap_.get( name );
        if ( konst == null ) {
            return null;
        }
        translatedConsts_.add( konst );
        Class<?> clazz = konst.getContentClass();
        if ( clazz == Double.class ) {
            return "Double";
        }
        else if ( clazz == Float.class ) {
            return "Float";
        }
        else if ( clazz == Long.class ) {
            return "Long";
        }
        else if ( clazz == Integer.class ) {
            return "Integer";
        }
        else if ( clazz == Short.class ) {
            return "Short";
        }
        else if ( clazz == Byte.class ) {
            return "Byte";
        }
        else if ( clazz == Boolean.class ) {
            return "Boolean";
        }
        else if ( clazz == Character.class ) {
            return "Character";
        }
        else if ( clazz == String.class ) {
            return "String";
        }
        else {
            return "Object";
        }
    }

    public double getDoubleProperty( String name ) {
        return ((Double) constMap_.get( name ).getValue()).doubleValue();
    }

    public float getFloatProperty( String name ) {
        return ((Float) constMap_.get( name ).getValue()).floatValue();
    }

    public long getLongProperty( String name ) {
        return ((Long) constMap_.get( name ).getValue()).longValue();
    }

    public int getIntegerProperty( String name ) {
        return ((Integer) constMap_.get( name ).getValue()).intValue();
    }

    public short getShortProperty( String name ) {
        return ((Short) constMap_.get( name ).getValue()).shortValue();
    }

    public byte getByteProperty( String name ) {
        return ((Byte) constMap_.get( name ).getValue()).byteValue();
    }

    public boolean getBooleanProperty( String name ) {
        return ((Boolean) constMap_.get( name ).getValue()).booleanValue();
    }

    public char getCharacterProperty( String name ) {
        return ((Character) constMap_.get( name ).getValue()).charValue();
    }

    public String getStringProperty( String name ) {
        return (String) constMap_.get( name ).getValue();
    }

    public Object getObjectProperty( String name ) {
        return constMap_.get( name ).getValue();
    }
}
