package uk.ac.starlink.hdx;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Manages a collection of properties for the Hdx system.  We don't
 * have to do anything particularly elaborate here, other than manage
 * creating a set of properties, initialised from a file in a
 * well-known place, and defaulting to the system properties.
 *
 * <p>We initialise properties from the property file named by the
 * <em>system property</em> <code>Hdx.properties</code>, or if
 * that does not exist, from the <em>file</em>
 * <code>Hdx.properties</code> located in the current directory
 * (System property <code>user.dir</code>).</p>
 *
 * <p>The interface here is patterned after the property methods on
 * the <code>System</code> object.</p>
 */
public class HdxProperties {

    /**
     * The property set which holds the Hdx properties.  Lazily
     * initialised.
     */
    private static Properties hdxPropertySet;
    
    private static Logger logger = Logger.getLogger("uk.ac.starlink.hdx");

    /**
     * Constructed is private on purpose
     */
    private HdxProperties() {
        // nothing to do
    }
    

    /**
     * Determines the current Hdx properties.
     *
     * <p>The current set of system
     * properties for use by the {@link #getProperty} method is
     * returned as a <code>Properties</code> object.
     *
     * <p>If there is no current set of Hdx properties (that is, this
     * method is being invoked for the first time), a set of Hdx
     * properties is first created and initialized.  In all cases, the
     * Hdx properties are defaulted by the System properties.
     *
     * @return a <code>Properties</code> object containing all the Hdx
     * properties
     */
    public static Properties getProperties() {
        if (hdxPropertySet == null) {
            hdxPropertySet = new Properties(System.getProperties());

            // Now find the properties file
            String fn = System.getProperty
                    ("Hdx.properties",
                     System.getProperty("user.dir")+"/Hdx.properties");
            try {

                java.io.FileInputStream propfile
                        = new java.io.FileInputStream(fn);
                hdxPropertySet.load(propfile);

            } catch (java.io.FileNotFoundException e) {
                // Not a problem -- simply means there's no property file
                logger.fine("No file " + fn + " (no problem!)");
            } catch (java.io.IOException e) {
                logger.warning("IOException reading file " + fn + ": "
                               + e);
            } catch (java.lang.SecurityException e) {
                logger.warning
                        ("Security exception opening " + fn + ":" + e);
            }
        }

        assert hdxPropertySet != null;
        
        return hdxPropertySet;
    }
    
    /** 
     * Gets the Hdx property indicated by the specified key.  If
     * there is no specifically Hdx property with this name, then
     * return any System property with this name.
     *
     * <p>If there is no current set of Hdx properties, a set of
     * properties is first created and initialized in the same manner
     * as described above.</p>
     *
     * <p>Unlike the corresponding method on <code>System</code>, this
     * method throws no exceptions, and simply returns
     * <code>null</code> on any error.
     *
     * @param key the name of the Hdx property
     *
     * @return the string value of the Hdx property, or null if
     * there is no property with that key.
     */
    public static String getProperty(String key) {
        return getProperties().getProperty(key);
    }
    
    /** 
     * Gets the Hdx property indicated by the specified key.  If
     * there is no specifically Hdx property with this name, then
     * return any System property with this name.
     *
     * <p>If there is no current set of Hdx properties, a set of
     * properties is first created and initialized in the same manner as
     * described above.</p>
     *
     * <p>Unlike the corresponding method on <code>System</code>, this
     * method throws no exceptions, and simply returns
     * <code>null</code> on any error.
     *
     * @param key the name of the Hdx property
     * @param def a default value
     *
     * @return the string value of the Hdx property, or null if
     * there is no property with that key.
     */
    public static String getProperty(String key, String def) {
        return getProperties().getProperty(key, def);
    }

    /**
     * Sets the Hdx property indicated by the specified key. 
     *
     * <p>Unlike the corresponding method on <code>System</code>, this
     * method throws only {@link HdxException} on any error.
     *
     * @param key the name of the Hdx property
     * @param value the value of the Hdx property
     *
     * @return the previous value of the Hdx property, or null if it
     * did not have one.
     *
     * @throws HdxException on any error
     */
    public static String setProperty(String key, String value)
            throws HdxException {
        try {
            return (String) getProperties().setProperty(key, value);
        } catch (ClassCastException e) {
            throw new HdxException("Previous value of property " + key
                                   + " was not a string!");
        } catch (Exception e) {
            // reprocess all exceptions into HdxException
            throw new HdxException("Failed to set Hdx property: " + e);
        }
    }
}
