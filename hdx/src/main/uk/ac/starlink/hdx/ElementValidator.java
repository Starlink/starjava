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
     * <p>The validator should not assume that the element it is
     * validating has been constructed by an authoritative source
     * (such as the package or class which implements this
     * interface).  In particular, it should not react to invalid
     * elements by throwing unchecked exceptions or using assertions.
     * That is, this method should be usable by code which is
     * constructing its <em>own</em> DOM representing the type in question.
     *
     * @return true if the element is of the type which this validator
     * checks; the element is valid; and each of its subelements is
     * valid.
     */
    boolean validateElement(Element el);
}
