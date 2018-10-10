package uk.ac.starlink.gbin;

/**
 * Parameterises the way that a GBIN object is turned into a StarTable
 * by the STIL input handler.
 *
 * @author   Mark Taylor
 * @since    13 Aug 2014
 */
public interface GbinTableProfile {

    /**
     * Indicates whether non-essential metadata is read from the GBIN file.
     * This includes amongst other things the row count and
     * a description of the data.  Because of the way GBIN reading works, 
     * reading metadata requires acquisition of a new InputStream
     * (it can't be done using the same stream that reads the data objects)
     * so it may be expensive.
     * 
     * @return   true to attempt to read metadata where possible
     */
    boolean isReadMeta();

    /**
     * Indicates whether the magic number is read from GBIN files before
     * attempting to turn them into tables.
     * It's probably a good idea to set this true, but there may be
     * GBIN variants for which the magic number testing supplied
     * gives false negatives, in which case you'd want to turn it off.
     *
     * @return  true  if you want magic number mismatch to cause read failure
     */
    boolean isTestMagic();

    /**
     * Indicates whether column names in the output table should be forced
     * to reflect the compositional hierarchy of their position in the
     * element objects.  If set true, columns will have names like
     * "Astrometry_Alpha", if false they may just be called "Alpha".
     * In case of name duplication however, the hierarchical form is
     * always used.
     *
     * @return  true to force use of hierarchical column names
     */
    boolean isHierarchicalNames();

    /**
     * Returns the separation string used to delimit parts of column
     * hierarchical names.  If the value is "_" you may see column
     * names like "Astrometry_Alpha".  An empty string is permissible too.
     *
     * @return  column hierarchical name separator string
     */
    String getNameSeparator();

    /**
     * Indicates whether object accessor method names should be
     * sorted alphabetically when producing the column sequence.
     * Otherwise <code>get*</code> method names will be used in
     * whatever sequence reflection provides them in, which
     * according to the {@link java.lang.Class#getMethods} javadocs
     * is "not sorted and are not in any particular order".
     *
     * @return   true to sort method names alphabetically
     */
    boolean isSortedMethods();

    /**
     * Returns the list of method names to ignore when
     * coming up with a list of columns for an object.
     * Any methods matching strings in the return value will not
     * be used to generate columns in an output table.
     * Only public instance methods of the form
     * <code>getXxx()</code> or <code>isXxx()</code>
     * are used in any case.
     * The restrictions determined by {@link #getIgnoreMethodDeclaringClasses}
     * also apply.
     *
     * @return   list of accessor method names to ignore
     */
    String[] getIgnoreMethodNames();

    /**
     * Returns the list of method-declaring classnames for which
     * the corresponding methods will be ignored when coming up
     * with a list of columns for an object.
     * Any method <code>m</code> for which <code>m.getDeclaringClass()</code>
     * returns a class whose name is returned by this method will not
     * be used to generate columns in an output table.
     * Only public instance methods of the form
     * <code>getXxx()</code> or <code>isXxx()</code>
     * are used in any case.
     * The restrictions determined by {@link #getIgnoreMethodNames}
     * also apply.
     *
     * @return  list of declaring classes whose methods are to be ignored
     */
    String[] getIgnoreMethodDeclaringClasses();

    /**
     * Returns an object which can represent a particular data type returned
     * from an accessor method of a GBIN object (or one of its descendants).
     * If null is returned, the object should not appear in the output
     * table at all.
     *
     * @param   clazz   return type of an accessor method
     * @return    object indicating how accessed objects should appear
     *            in the output table; null to truncate the hierarchy here
     */
    Representation<?> createRepresentation( Class<?> clazz );
}
