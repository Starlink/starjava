package uk.ac.starlink.hdx;

import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.*;

/** 
 * Skeletal implementation of the {@link DOMFacade} interface.
 *
 * <p>This
 * includes trivial implementations of <code>addChildBefore</code>,
 * <code>replaceChild</code> and <code>setAttribute</code>, which act
 * as a read-only DOM.  It also implements a <code>getSource</code>
 * method in terms of the <code>getDOM</code> method.
 *
 * @author Norman Gray
 * @version $Id$
 */
public abstract class AbstractDOMFacade implements DOMFacade {
    public abstract Element getDOM(URL base);
    public abstract Object getObject(Element el) throws HdxException;

    public Source getSource(URL base) {
        return (base != null)
                ? new DOMSource(getDOM(base), base.toExternalForm())
                : new DOMSource(getDOM(base));
    }
    
    public boolean addChildBefore(Element parent,
                                  Element newChild,
                                  Element refChild) {
        return false;
    }
    
    public boolean replaceChild(Element parent,
                                Element oldChild,
                                Element newChild) {
        return false;
    }

    public boolean setAttribute(Element el, String name, String value) {
        return false;
    }
}
