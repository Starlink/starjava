package uk.ac.starlink.hdx;     // XXX move to no package

import org.w3c.dom.*;

/**
 * Simple weather type, to exercise registration of new types.
 *
 * Example:
 * <pre>
 * &lt;weather>
 *   &lt;cloud colour="black">
 * &lt;/weather>
 * </pre>
 */
public class SimpleWeather {
    private static HdxResourceType myType;
    static {
        try {
            myType = HdxResourceType.newHdxResourceType("weather");
            myType.setElementValidator(new ElementValidator() {
                public boolean validateElement(Element el) {
                    if (HdxResourceType.match(el) != myType)
                        return false;
                    return findCloudColour(el) != null;
                }
            });
            myType.registerHdxResourceFactory(new HdxResourceFactory() {
                public Object getObject(Element el)
                    throws HdxException {
                    return new SimpleWeather(el);
                }
            });
            myType.setConstructedClass("uk.ac.starlink.hdx.SimpleWeather");
        } catch (HdxException ex) {
            throw new PluginException ("Failed to register SimpleWeather type:"
                                       + ex);
        }
    }

    private String cloudColour;

    private SimpleWeather(Element el)
            throws HdxException {
        String cc = findCloudColour(el);
        if (cc == null)
            throw new HdxException("Weather element -- bad cloud information");
        else
            cloudColour = cc;
        assert cloudColour != null;
    }

    /**
     * Return the colour of the cloud.
     *
     * @return a non-null string representing the cloud colour.
     */
    public String getCloudColour() {
        return cloudColour;
    }

    /**
     * Gets cloud colour, or null if there is not exactly 1 cloud
     * below the element.
     */
    private static String findCloudColour(Element el) {
        NodeList clouds = el.getElementsByTagName("cloud");
        if (clouds.getLength() != 1)
            return null;
        Element cloud = (Element)clouds.item(0);
        String colour = cloud.getAttribute("colour");
        if (colour.length() == 0)
            return null;
        else
            return colour;
    }
}

