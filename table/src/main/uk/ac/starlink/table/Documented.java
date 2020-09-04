package uk.ac.starlink.table;

/**
 * Mixin interface that provides extended user documentation for an object.
 *
 * @author   Mark Taylor
 * @since    4 Sep 2020
 */
public interface Documented {

    /**
     * Returns user-directed documentation in XML format.
     *
     * <p>The output should be a sequence of one or more &lt;P&gt; elements,
     * using XHTML-like XML.  Since rendering may be done in a number of
     * contexts however, use of the full range of XHTML elements is
     * discouraged.  Where possible, the content should stick to
     * simple markup such as the elements
     * P, A, UL, OL, LI, DL, DT, DD EM,
     * STRONG, I, B, CODE, TT, PRE.
     *
     * @return  XML description of this object
     */
    String getXmlDescription();
}
