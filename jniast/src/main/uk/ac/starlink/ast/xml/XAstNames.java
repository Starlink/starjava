package uk.ac.starlink.ast.xml;

/**
 * This class acts as a repository for the String constants used in the
 * XML <-> Channel conversions.  The constants defined here are not subject
 * to change, and are the only ones used for element and attribute names
 * by XAstReader and XAstWriter.
 *
 * @author   Mark Taylor (Starlink)
 */
abstract class XAstNames {

    /**
     * XML element name used to identify an attribute setting for an AST object.
     */
    public static final String ATTRIBUTE = "_attribute";

    /**
     * XML element name used to identify class membership setting for an 
     * AST object.
     */
    public static final String ISA = "_isa";

    /**
     * XML attribute name for identifying label of an object in an AST channel.
     */
    public static final String LABEL = "label";

    /**
     * XML attribute name in ATTRIBUTE element giving the name for the AST 
     * attribute.
     */
    public static final String NAME = "name";

    /**
     * XML attribute name in ATTRIBUTE element giving the value for the AST
     * attribute.
     */
    public static final String VALUE = "value";

    /**
     * XML attribute name in ISA element giving the name of the AST ancestor
     * class.
     */
    public static final String CLASS = "class";

    /**
     * XML attribute name in ATTRIBUTE element indicating quoted status of
     * VALUE attribute value.  If the QUOTED attribute has a value of
     * <tt>true</tt> (case insensitive) then a quote character 
     * ('<tt>"</tt>' = &amp;quot;) is considered to be prepended to 
     * the start and appended to the end of the value of the NAME attribute.
     */
    public static final String QUOTED = "quoted";

    /**
     * XML attribute name in ATTRIBUTE element indicating whether the 
     * attribute in question represents a default value.  If it does, 
     * then the reader should ignore it (rather than setting it, which
     * would cause it to become set rather than un-set in AST terms).
     */
    public static final String DEFAULT = "default";
}
