package uk.ac.starlink.hdx;

import org.w3c.dom.Element;

/**
 * An object which provides validation services for a registered
 * {@link HdxResourceType}.  Implementations of this interface are
 * linked with types using the method {@link
 * HdxResourceType#setElementValidator setElementValidator}
 * on the object representing the type.
 */
public interface ElementValidator {
    /**
     * Checks that an element is a valid instance of a type.
     *
     * @return true if the element is of the type which this validator
     * checks; the element is valid; and each of its subelements is
     * valid.
     */
    boolean validateElement(Element el);
}
