package uk.ac.starlink.hdx.extension;

import uk.ac.starlink.hdx.*;
import org.w3c.dom.*;
import java.net.URI;

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
    private static HdxResourceType weatherType;

    private String cloudColour;
    private URI uri;
    
    static {
        try {
            weatherType = HdxResourceType.newHdxResourceType("weather");
            weatherType.setElementValidator(new ElementValidator() {
                public boolean validateElement(Element el) {
                    try {
                        if (HdxResourceType.match(el) != weatherType)
                            return false;
                        if (findCloudColour(el) == null)
                            return false;
                        URI uri = findDataURI(el);
                        // returned -- element is OK
                        return true;
                    } catch (HdxException ex) {
                        // findDataURI threw this
                        return false;
                    }
                }
            });
            weatherType.registerHdxResourceFactory(new HdxResourceFactory() {
                public Object getObject(Element el)
                    throws HdxException {
                    return new SimpleWeather(el);
                }
            });
            weatherType.setConstructedClass
                ("uk.ac.starlink.hdx.extension.SimpleWeather");
        } catch (HdxException ex) {
            throw new PluginException
                ("Failed to register SimpleWeather type:" + ex);
        }
    }

    public SimpleWeather() {
        cloudColour = "transparent!";
    }

    private SimpleWeather(Element el)
            throws HdxException {
        String cc = findCloudColour(el);
        if (cc == null)
            throw new HdxException("Weather element -- bad cloud information");
        else
            cloudColour = cc;
        uri = findDataURI(el);
        assert cloudColour != null;
    }

    public SimpleWeather(String colour, String uri)
            throws HdxException {
        try {
            cloudColour = colour;
            if (uri != null)
                this.uri = new URI(uri);
            //System.err.println("colour=" + this.cloudColour + "  uri=" + this.uri);
        } catch (java.net.URISyntaxException ex) {
            throw new HdxException("Error creating URI: " + ex);
        }
    }

    /**
     * Return the colour of the cloud.
     * @return a non-null string representing the cloud colour.
     */
    public String getCloudColour() {
        return cloudColour;
    }

    public URI getDataURI() {
        return uri;
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

    private static URI findDataURI(Element el) 
            throws HdxException {
        try {
            NodeList dataElements = el.getElementsByTagName("data");
            if (dataElements.getLength() != 1)
                return null;
            Element data = (Element) dataElements.item(0);
            String uri = data.getAttribute("uri");
            if (uri.length() == 0)
                return null;
            else
                return new URI(uri);
        } catch (java.net.URISyntaxException ex) {
            throw new HdxException("URI attribute invalid");
        }
    }

//     public DOMFacade getDOMFacade() {
//         return new SimpleWeatherDOMFacade();
//     }

    public HdxFacade getHdxFacade() {
        return new SimpleWeatherHdxFacade();
    }
    

    /**
     * DOMFacade for SimpleWeather.  In a normal class, if we had a
     * DOMFacade, we would use it to implement the
     * getObject method in the type initialization above.  But we
     * don't do that here so that we can test both types of
     * functionality.  That is, don't mistake this testing code for a paradigm.
     */
    protected class SimpleWeatherHdxFacade
            extends AbstractHdxFacade {
        //private Document doc;
        
        public Object synchronizeElement(Element el, Object memento) {
            // Pig-ignorant at present -- if there's anything attached
            // to this element at all, assume we've been here before,
            // and it's all correct
            if (el.hasAttributes() || el.hasChildNodes())
                return null;

            Document doc = el.getOwnerDocument();
            Element cloud = doc.createElement("cloud");
            cloud.setAttribute("colour", getCloudColour());
            el.appendChild(cloud);
            if (uri != null) {
                Element data = doc.createElement("data");
                data.setAttribute("uri", uri.toString());
                el.appendChild(data);
            }
//             System.err.println
//                     ("SimpleWeatherHdxFacade.synchronizeElement(colour="
//                      + SimpleWeather.this.cloudColour + '='
//                      + getCloudColour()
//                      + ", uri=" + SimpleWeather.this.uri + '='
//                      + getDataURI()
//                      + ") produced "
//                      + HdxDocument.NodeUtil.serializeNode(el));
            return null;
        }

//         public Element getDOM(URI base) {
//             // base not used
//             if (doc == null) {
//                 doc = (HdxDocument)HdxDOMImplementation
//                         .getInstance()
//                         .createDocument(null, "weather", null);
//                 Element top = doc.createElement("weather");
//                 doc.appendChild(top);
//                 Element cloud = doc.createElement("cloud");
//                 cloud.setAttribute("colour", getCloudColour());
//                 top.appendChild(cloud);
//                 if (uri != null) {
//                     Element data = doc.createElement("data");
//                     data.setAttribute("uri", uri.toString());
//                     top.appendChild(data);
//                 }
// //                 System.err.println("SimpleWeatherDOMFacade.getDOM(colour="
// //                                    + SimpleWeather.this.cloudColour + '='
// //                                    + getCloudColour()
// //                                    + ", uri=" + SimpleWeather.this.uri + '='
// //                                    + getDataURI()
// //                                    + ") produced "
// //                                    + HdxDocument.NodeUtil.serializeNode(doc));
//             }
//             Element de = doc.getDocumentElement();
//             assert de.getTagName().equals("weather");
//             return de;
//         }
        
        public Object getObject(Element el)
                throws HdxException {
            if (HdxResourceType.match(el) != weatherType)
                throw new HdxException
                        ("getObject was asked to realised a foreign type");
            return SimpleWeather.this;
        }
        
        public HdxResourceType getHdxResourceType() {
            return weatherType;
        }
    }


//     /**
//      * DOMFacade for SimpleWeather.  In a normal class, if we had a
//      * DOMFacade, we would use it to implement the
//      * getObject method in the type initialization above.  But we
//      * don't do that here so that we can test both types of
//      * functionality.  That is, don't mistake this testing code for a paradigm.
//      */
//     protected class SimpleWeatherDOMFacade
//             extends AbstractDOMFacade {
//         private Document doc;
        
//         public Element getDOM(URI base) {
//             // base not used
//             if (doc == null) {
//                 doc = (HdxDocument)HdxDOMImplementation
//                         .getInstance()
//                         .createDocument(null, "weather", null);
//                 Element top = doc.createElement("weather");
//                 doc.appendChild(top);
//                 Element cloud = doc.createElement("cloud");
//                 cloud.setAttribute("colour", getCloudColour());
//                 top.appendChild(cloud);
//                 if (uri != null) {
//                     Element data = doc.createElement("data");
//                     data.setAttribute("uri", uri.toString());
//                     top.appendChild(data);
//                 }
// //                 System.err.println("SimpleWeatherDOMFacade.getDOM(colour="
// //                                    + SimpleWeather.this.cloudColour + '='
// //                                    + getCloudColour()
// //                                    + ", uri=" + SimpleWeather.this.uri + '='
// //                                    + getDataURI()
// //                                    + ") produced "
// //                                    + HdxDocument.NodeUtil.serializeNode(doc));
//             }
//             Element de = doc.getDocumentElement();
//             assert de.getTagName().equals("weather");
//             return de;
//         }
        
//         public Object getObject(Element el)
//                 throws HdxException {
//             if (HdxResourceType.match(el) != weatherType)
//                 throw new HdxException
//                         ("getObject was asked to realised a foreign type");
//             return SimpleWeather.this;
//         }
        
//         public HdxResourceType getHdxResourceType() {
//             return weatherType;
//         }
//     }
}

