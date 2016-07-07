package uk.ac.starlink.gbin;

import java.util.List;
import java.util.Map;

/**
 * Represents table metadata as extracted by
 * <code>gaia.cu9.tools.documentationexport.MetadataReader</code>.
 * An instance of this class corresponds to the extracted metadata
 * for a table with a given table name, that name being known to
 * the MetadataReader class.
 * The method signatures are taken from MetadataReader,
 * except that the initial <code>tableName</code> argument is removed
 * from them all.
 *
 * <p>I <em>think</em> that the term "parameter" in method names and
 * formal arguments in these signatures indicates column names.
 *
 * @author   Mark Taylor
 * @since    7 Jul 2016
 */
public interface GaiaTableMetadata {

    /**
     * Returns a table description string.
     *
     * @return  table description
     */
    String getTableDescription();

    /**
     * Returns a map of parameter name to data type for this table.
     *
     * @return  data type map
     */
    Map<String,String> getParametersWithTypes();

    /**
     * Returns a description of a named parameter of this table.
     *
     * @param  parameterName  column name?
     * @return   description
     */
    String getParameterDescription( String parameterName );

    /**
     * Returns a detailed description of a named parameter of this table.
     *
     * @param  parameterName  column name?
     * @return   description
     */
    String getParameterDetailedDescription( String parameterName );

    /**
     * Returns a list of UCD items for a named parameter of this table.
     * The returned list if of type
     * <code>gaia.cu9.tools.documentationexport.xmlparser.XmlUcd</code>;
     * the toString method of these can be used to turn them into
     * strings.  I <em>think</em> those strings are UCD atoms that can be
     * concatenated with semicolons.
     *
     * @param  parameterName  column name?
     * @return  UCD atom list?
     */
    List<?> getUcds(String parameterName);
}
