package uk.ac.starlink.hds;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import uk.ac.starlink.util.Loader;

/**
 * Provides a Java interface to the Starlink 
 * <a href="http://star-www.rl.ac.uk/star/docs/sun92.htx/sun92.html">HDS</a>
 * library.  The methods provided here are all implemented using JNI
 * native code and are intended to map one-to-one to the public Fortran 
 * calls provided by HDS, using similar arguments to those calls.  
 * Other useful methods on HDS objects should be provided by 
 * other classes which extend or contain instances of <code>HDSObject</code>.
 * For this reason there is no constructor, a factory method like
 * <code>hdsOpen</code> or <code>datFind</code> should be used instead.
 * <p>
 * On the whole, the implementation of this class is such that a call
 * to one of these methods results in a call to the similarly-named
 * routine in the HDS library.  This is not always the most efficient
 * way to procure the data requested.
 * <p>
 * The arguments of the methods declared by this class are not identical
 * with those of the corresponding Fortran HDS calls, and
 * the documentation for each of the <code>HDSObject</code> methods
 * should be checked before use.  However, the following rules in 
 * general apply:
 * <ul>
 * <li>HDS calls with a LOCATOR argument turn into instance methods called on 
 *     an <code>HDSObject</code> object represented by that locator.
 *     The locator itself is kept as a (private) member of the the 
 *     <code>HDSObject</code>, and will never be referred to by Java
 *     code.
 * <li>The STATUS argument of the HDS call is omitted; if the call in
 *     question would return a non-OK STATUS then an <code>HDSException</code>
 *     is called.  The <code>getMessage</code> method of the HDSException
 *     returns the error message generated.  All methods can therefore 
 *     throw an {@link HDSException}.
 * <li>If the purpose of the HDS call is to return a locator, then 
 *     the return value of the method will be 
 *     a new <code>HDSObject</code> object.
 * <li>If the purpose of the HDS call is to return a primitive value/array
 *     of some sort, then the return value of the method will be a
 *     primitive value/array of the corresponding type.
 * <li>An array of INTEGERs (which would normally be dimensioned DAT__MXDIM
 *     or smaller in fortran) intended to represent either the shape of an
 *     HDS array component or a location within such an array is replaced
 *     by an array of <code>long</code>s.
 * <li>Arguments which are no longer required (for instance the maximum
 *     dimensionality of an array) are not used.
 * <li>All other arguments of the fortran HDS call are turned into
 *     method arguments of the corresponding type.
 * </ul>
 * Furthermore, methods are named in a predictable way from the names 
 * of the corresponding Fortran calls.
 * <p>
 * Using the above rules, we see that, for instance:
 * <pre>
 *    CALL DAT_FIND( LOC1, NAME, LOC2, STATUS )
 * </pre>
 * becomes
 * <pre>
 *    public native HDSObject datFind( String name ) throws HDSException;
 * </pre>
 * <p>
 * <b>Note:</b>
 * This class is not complete; it is being added to as calls are required.
 *
 * @author   Mark Taylor (STARLINK)
 * @version  $Id$
 */
public class HDSObject {
    private byte[] locator;

    /** Object representing the locator DAT__ROOT. */
    public static final HDSObject DAT__ROOT;

    /** 
     * Maximum number of characters in the name of an HDS component.
     * The value is determined by the underlying HDS library. 
     */
    public static final int DAT__SZNAM;

    /** 
     * maximum number of characters in the name of an HDS type string.
     * Note this does not include the size modifier in a '_CHAR*N' string.
     * The value is determined by the underlying HDS library.
     */
    public static final int DAT__SZTYP;

    /*
     * Static initializer.  This loads and initialises the shared object  
     * implementing the native code, and sets up the values of the final
     * static members.
     */
    static {
        Loader.loadLibrary( "jnihds" );
        nativeInitialize();
        DAT__ROOT = getHDSConstantLoc( "DAT__ROOT" );
        DAT__SZNAM = getHDSConstantI( "DAT__SZNAM" );
        DAT__SZTYP = getHDSConstantI( "DAT__SZTYP" );
    }

    // Ensure that there is no publicly accessible constructor.
    private HDSObject() {
    }

    protected void finalize() throws Throwable {
        try {
            if ( datValid() ) {
                datAnnul();
            }
        }
        finally {
            super.finalize();
        }
    }

    // Initialiser for shared library.
    private native static void nativeInitialize();

    /* 
     * Package-scoped methods to get constants from HDS.  These are used to set
     * up public final static members for the class.
     */
    native static int getHDSConstantI( String constName );
    native static HDSObject getHDSConstantLoc( String constname );

    /**
     * Create container file - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_HDS_NEW">HDS_NEW</a>.
     * Creates a new container file and returns a primary locator to the
     * top-level object.
     *
     * @param  file  the name of the container file to be created.  ".sdf" 
     *               will be added an extension is not specified.
     * @param  name  the name of the object to be created
     * @param  type  the HDS type of the top-level object to be created
     * @param  dims  the dimensions of the top-level object to be created
     * @return    an HDSObject referencing the object in the new container
     *               file.  It is a primary object.
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native static HDSObject
        hdsNew( String file, String name, String type, long[] dims )
        throws HDSException;

    /** 
     * Open container file - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_HDS_OPEN">HDS_OPEN</a>.
     *
     * @param   container     the container file name
     * @param   accessMode    the access mode "READ", "UPDATE" or "WRITE"
     * @return                a new <code>HDSObject</code> representing 
     *                        the newly opened container file
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native static HDSObject 
        hdsOpen( String container, String accessMode ) throws HDSException;

    /**
     * Show HDS statistics - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_HDS_SHOW">HDS_SHOW</a>.
     *
     * @param  topic  name of the topic on which to supply information.
     *                One of "DATA", "FILES" or "LOCATORS".
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native static void
        hdsShow( String topic ) throws HDSException;

    /**
     * Trace object path - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_HDS_TRACE">HDS_TRACE</a>.
     *
     * @param  results  a two-element String array.  On exit the first element
     *                  will be set to the object path name within the
     *                  container file, and the second element to the 
     *                  container file name.
     * @return  the number of path levels
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native int 
       hdsTrace( String[] results ) throws HDSException;

    /**
     * Annul locator - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_ANNUL">DAT_ANNUL</a>.
     * <p>
     * It is not generally necessary for client code to call this method 
     * (though it is permissible), since it is called by the finalizer
     * method.  The garbage collector can therefore
     * be relied upon to annul resources which are associated with 
     * unreferenced HDSObjects in due course.  Note however that there
     * is no guarantee when (or if) garbage collection will be performed,
     * so side-effects of datAnnul, such as the unmapping of mapped data,
     * must be invoked explicitly rather than leaving them to get done
     * when the automatic annul is done.
     *
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void 
        datAnnul() throws HDSException;

    /**
     * Get an <code>HDSObject</code> from a cell (element) of an array object
     * - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_CELL">DAT_CELL</a>.
     *
     * @param   position      the location within the array of the cell to 
     *                        retreive
     * @return                a new <code>HDSObject</code> representing the 
     *                        indicated element of the array
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native HDSObject
        datCell( long[] position ) throws HDSException;

    /**
     * Clone locator - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_CLONE">DAT_CLONE</a>.
     *
     * @return                a new <code>HDSObject</code> referring to the same
     *                        object as this <code>HDSObject</code> except
     *                        that the locator will always be secondary
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native HDSObject
        datClone() throws HDSException;

    /**
     * Erase component - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_ERASE">DAT_ERASE</a>.
     * Recursively erases a component.  This means that all its lower level
     * components are deleted as well.
     *
     * @param   name          the name of the component within this 
     *                        <code>HDSObject</code> 
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void 
        datErase( String name ) throws HDSException;

    /**
     * Find named component - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_FIND">DAT_FIND</a>.
     *
     * @param   name          the name of the component within this 
     *                        <code>HDSObject</code> 
     * @return                a new <code>HDSObject</code> representing the 
     *                        named component
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native HDSObject 
        datFind( String name ) throws HDSException;

    /**
     * Read a primitive as <code>String</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETx">DAT_GETx</a>.
     *
     * @param   shape        an array giving the shape of
     *                       the HDS primitive
     * @return               an <code>Object</code> giving the contents of
     *                       this primitive HDSObject.  If this object 
     *                       represents an HDS array primitive, the return
     *                       value is an array, or 
     *                       array of arrays, or ... of <code>String</code>s,
     *                       according to the dimensionality of the primitive.
     *                       If this object is a scalar primitive 
     *                       (<code>shape</code> has dimensionality of zero)
     *                       it will be the scalar value as a 
     *                       <code>String</code>.
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native Object datGetc( long[] shape ) throws HDSException;

    /**
     * Read a primitive as <code>boolean</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETx">DAT_GETx</a>.
     *
     * @param   shape         an array giving the shape of
     *                        the HDS primitive
     * @return                an <code>Object</code> giving the contents of 
     *                        this primitive HDSObject.  If this object 
     *                        represents an HDS array primitive, 
     *                        the return value is an array, or 
     *                        array of arrays, or ... of <code>boolean</code>s,
     *                        according to the dimensionality of the primitive.
     *                        If this object is a scalar primitive 
     *                        (<code>shape</code> has dimensionality of zero)
     *                        it will be an <code>Boolean</code> wrapping the
     *                        value.
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native Object datGetl( long[] shape ) throws HDSException;

    /**
     * Read a primitive as <code>integer</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETx">DAT_GETx</a>.
     *
     * @param   shape         an array giving the shape of
     *                        the HDS primitive to return
     * @return                an <code>Object</code> giving the contents of 
     *                        this primitive HDSObject.  If this object 
     *                        represents an HDS array primitive, 
     *                        the return value is an array, or 
     *                        array of arrays, or ... of <code>int</code>s,
     *                        according to the dimensionality of the primitive.
     *                        If this object is a scalar primitive 
     *                        (<code>shape</code> has dimensionality of zero)
     *                        it will be an <code>Integer</code> wrapping the
     *                        value.
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native Object datGeti( long[] shape ) throws HDSException;

    /**
     * Read a primitive as <code>float</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETx">DAT_GETx</a>.
     *
     * @param   shape         an array giving the shape of
     *                        the HDS primitive to return
     * @return                an <code>Object</code> giving the contents of 
     *                        this primitive HDSObject.  If this object 
     *                        represents an HDS array primitive, 
     *                        the return value is an array, or 
     *                        array of arrays, or ... of <code>float</code>s,
     *                        according to the dimensionality of the primitive.
     *                        If this object is a scalar primitive 
     *                        (<code>shape</code> has dimensionality of zero)
     *                        it will be an <code>Float</code> wrapping the
     *                        value.
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native Object datGetr( long[] shape ) throws HDSException;

    /**
     * Read a primitive as <code>double</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETx">DAT_GETx</a>.
     *
     * @param   shape         an array giving the shape of
     *                        the HDS primitive to return
     * @return                an <code>Object</code> giving the contents of 
     *                        this primitive HDSObject.  If this object 
     *                        represents an HDS array primitive, 
     *                        the return value is an array, or 
     *                        array of arrays, or ... of <code>double</code>s,
     *                        according to the dimensionality of the primitive.
     *                        If this object is a scalar primitive 
     *                        (<code>shape</code> has dimensionality of zero)
     *                        it will be an <code>Double</code> wrapping the
     *                        value.
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native Object datGetd( long[] shape ) throws HDSException;

    /**
     * Read a primitive as <code>String</code> type as if it were
     * vectorised (regardless of its actual shape) - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETVx">DAT_GETVx</a>.
     * The length of the returned array will be the size of the entire array
     * of this object (note: one element for each array element, not for 
     * each character).  Trailing blanks in the strings will be trimmed so
     * strings may be smaller than their declared size.  This method cannot
     * be used to read arrays with more than <code>Integer.MAX_VALUE</code>
     * elements.
     * 
     * @return                a (1-dimensional) array of <code>String</code>s,
     *                        one for each element in the primitive array.
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native String[] datGetvc() throws HDSException;

    /**
     * Read a primitive as <code>boolean</code> type as if it were 
     * vectorised (regardless of its actual shape) - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETVx">DAT_GETVx</a>.
     * The length of the returned array will be the size of the entire
     * array of this object.  This method cannot be used to read arrays
     * with more than <code>Integer.MAX_VALUE</code> elements.
     *
     * @return                a (1-dimensional) array of <code>boolean</code>s
     *                        containing all the values of the primitive array
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native boolean[] datGetvl() throws HDSException;

    /**
     * Read a primitive as <code>int</code> type as if it were 
     * vectorised (regardless of its actual shape) - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETVx">DAT_GETVx</a>.
     * The length of the returned array will be the size of the entire
     * array of this object.  This method cannot be used to read arrays
     * with more than <code>Integer.MAX_VALUE</code> elements.
     *
     * @return                a (1-dimensional) array of <code>int</code>s
     *                        containing all the values of the primitive array
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native int[] datGetvi() throws HDSException;

    /**
     * Read a primitive as <code>float</code> type as if it were 
     * vectorised (regardless of its actual shape) - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETVx">DAT_GETVx</a>.
     * The length of the returned array will be the size of the entire
     * array of this object.  This method cannot be used to read arrays
     * with more than <code>Integer.MAX_VALUE</code> elements.
     *
     * @return                a (1-dimensional) array of <code>float</code>s
     *                        containing all the values of the primitive array
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native float[] datGetvr() throws HDSException;

    /**
     * Read a primitive as <code>double</code>type as if it were 
     * vectorised (regardless of its actual shape) - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GETVx">DAT_GETVx</a>.
     * The length of the returned array will be the size of the entire
     * array of this object.  This method cannot be used to read arrays
     * with more than <code>Integer.MAX_VALUE</code> elements.
     *
     * @return                a (1-dimensional) array of <code>double</code>s
     *                        containing all the values of the primitive array
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native double[] datGetvd() throws HDSException;

    /**
     * Read scalar primitive as <code>String</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GET0x">DAT_GET0C</a>.
     * As with the underlying HDS routine, this may be used to return a 
     * representation of a scalar value of any type.
     *
     * @return                a String representation of this 
     *                        <code>HDSObject</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native String datGet0c() throws HDSException;

    /**
     * Read scalar primitive as <code>boolean</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GET0x">DAT_GET0L</a>.
     *
     * @return                a boolean representation of this 
     *                        <code>HDSObject</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native boolean datGet0l() throws HDSException;

    /**
     * Read scalar primitive as <code>int</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GET0x">DAT_GET0I</a>.
     *
     * @return                an int representation of this 
     *                        <code>HDSObject</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native int datGet0i() throws HDSException;

    /**
     * Read scalar primitive as <code>float</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GET0x">DAT_GET0R</a>.
     *
     * @return                a float representation of this 
     *                        <code>HDSObject</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native float datGet0r() throws HDSException;

    /**
     * Read scalar primitive as <code>double</code> type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_GET0x">DAT_GET0D</a>.
     *
     * @return                a String representation of this 
     *                        <code>HDSObject</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native double datGet0d() throws HDSException;

    /**
     * Index into component list - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_INDEX">DAT_INDEX</a>.
     *
     * @param   index         position in list of component to return
     * @return                a new <code>HDSObject</code> representing the
     *                        <code>index</code>'th component of this 
     *                        <code>HDSObject</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native HDSObject 
        datIndex( int index ) throws HDSException;

    /**
     * Map primitive - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_MAPV">DAT_MAPV</a>.
     * The returned object is a {@link java.nio.Buffer} of a type determined
     * by the type parameter.  Note that the unsigned HDS types (_UWORD and
     * _UBYTE) are not currently supported (an IllegalArgumentException
     * will be thrown).
     *
     * @param  type  an HDS type string giving the type with which to map 
     *               the array; one of "_BYTE", "_WORD", "_INTEGER", "_REAL",
     *               "_DOUBLE".
     * @param  mode  a string indicating access mode; one of "READ",
     *               "WRITE", "UPDATE".
     * @return       a java.nio direct Buffer of the appropriate type 
     *               (IntBuffer, FloatBuffer etc) containing the data
     * @throws   HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     * @throws   UnsupportedOperationException  if the JNI implementation 
     *           does not support mapping of a direct buffer
     */
    public Buffer datMapv( String type, String mode ) 
        throws HDSException {
        ByteBuffer bbuf = mapBuffer( type, mode );
        bbuf.order( ByteOrder.nativeOrder() );
        Buffer tbuf;
        if ( bbuf == null ) {
            throw new UnsupportedOperationException(
                "JVM implementation does not support " +
                "JNI access to direct buffers" );
        }
        if ( type.equalsIgnoreCase( "_BYTE" ) ||
             type.equalsIgnoreCase( "_UBYTE" ) ) {
            tbuf = bbuf;
        }
        else if ( type.equalsIgnoreCase( "_WORD" ) ||
                  type.equalsIgnoreCase( "_UWORD" ) ) {
            tbuf = bbuf.asShortBuffer();
        }
        else if ( type.equalsIgnoreCase( "_INTEGER" ) ) {
            tbuf = bbuf.asIntBuffer();
        }
        else if ( type.equalsIgnoreCase( "_REAL" ) ) {
            tbuf = bbuf.asFloatBuffer();
        }
        else if ( type.equalsIgnoreCase( "_DOUBLE" ) ) {
            tbuf = bbuf.asDoubleBuffer();
        }
        else {
            throw new IllegalArgumentException( 
                "Unsupported type \"" + type + "\"" );
        }
        return tbuf;
    }

    private native ByteBuffer mapBuffer( String type, String mode ) 
        throws HDSException;

    /**
     * Enquire object name - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_NAME">DAT_NAME</a>
     *
     * @return                the component name of this <code>HDSObject</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native String 
        datName() throws HDSException;

    /**
     * Enquire number of components - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_NCOMP">DAT_NCOMP</a>.
     *
     * @return                the number of components contained by this
     *                        <code>HDSObject</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native int 
        datNcomp() throws HDSException;

    /**
     * Create component - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_NEW">DAT_NEW</a>.
     *
     * @param  name  the name of the new component.
     * @param  type  the type of the new component - if it matches one of the
     *               primitive types a primitive is created, otherwise it
     *               is assumed to be a structure
     * @param  dims  component dimensions
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK) -
     *                        in particular if the named componene already
     *                        exists in this HDSObject
     */
    public native void
        datNew( String name, String type, long[] dims ) throws HDSException;

    /**
     * Locate parent structure - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PAREN">DAT_PAREN</a>.
     *
     * @return  the parent object, if one exists
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK) -
     *                        in particular if the object has no parent
     *                        because it is at the top level in an HDS 
     *                        container file.
     */
    public native HDSObject
        datParen() throws HDSException;

    /**
     * Enquire primary/secondary locator status - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PRMRY">DAT_PRMRY</a>.
     * Note there are two overloaded forms of the <code>datPrmry</code> method
     * to handle the distinct get/set semantics of the underlying routine.
     *
     * @return  <code>true</code> if the locator of this <code>HDSObject</code>
     *          is primary, <code>false</code> if it is secondary
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native boolean
        datPrmry() throws HDSException;

    /**
     * Set primary/secondary locator status - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PRMRY">DAT_PRMRY</a>.
     * Note there are two overloaded forms of the <code>datPrmry</code> method
     * to handle the distinct get/set semantics of the underlying routine.
     *
     * @param primary         <code>true</code> to set the locator of this 
     *                        <code>HDSObject</code> to primary, or 
     *                        <code>false</code> to set it to secondary
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void
        datPrmry( boolean primary ) throws HDSException;

    /**
     * Write scalar primitive - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUT0x">DAT_PUT0x</a>.
     *
     * @param  value  the value to be written
     * @throws HDSException    if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPut0c( String value ) throws HDSException;

    /**
     * Write scalar primitive - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUT0x">DAT_PUT0x</a>.
     *
     * @param  value  the value to be written
     * @throws HDSException    if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPut0l( boolean value ) throws HDSException;

    /**
     * Write scalar primitive - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUT0x">DAT_PUT0x</a>.
     *
     * @param  value  the value to be written
     * @throws HDSException    if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPut0i( int value ) throws HDSException;

    /**
     * Write scalar primitive - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUT0x">DAT_PUT0x</a>.
     *
     * @param  value  the value to be written
     * @throws HDSException    if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPut0r( float value ) throws HDSException;

    /**
     * Write scalar primitive - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUT0x">DAT_PUT0x</a>.
     *
     * @param  value  the value to be written
     * @throws HDSException    if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPut0d( double value ) throws HDSException;

    /**
     * Write a primitive as if it were vectorised (regardless of its 
     * actual shape) - see 
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUTVx">DAT_PUTVx</a>.
     * The array must be long enough to fill the entire object.
     * This method cannot be used to write arrays with more than 
     * <code>Integer.MAX_VALUE</code> elements.
     *
     * @param  value  an array containing the values to be written.
     *                Must contain enough elements to fill the entire object.
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPutvc( String[] value ) throws HDSException;

    /**
     * Write a primitive as if it were vectorised (regardless of its 
     * actual shape) - see 
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUTVx">DAT_PUTVx</a>.
     * This method cannot be used to write arrays with more than 
     * <code>Integer.MAX_VALUE</code> elements.
     *
     * @param  value  an array containing the values to be written.
     *                Must contain enough elements to fill the entire object.
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPutvl( boolean[] value ) throws HDSException;

    /**
     * Write a primitive as if it were vectorised (regardless of its 
     * actual shape) - see 
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUTVx">DAT_PUTVx</a>.
     * The array must be long enough to fill the entire object.
     * This method cannot be used to write arrays with more than 
     * <code>Integer.MAX_VALUE</code> elements.
     *
     * @param  value  an array containing the values to be written.
     *                Must contain enough elements to fill the entire object.
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPutvi( int[] value ) throws HDSException;

    /**
     * Write a primitive as if it were vectorised (regardless of its 
     * actual shape) - see 
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUTVx">DAT_PUTVx</a>.
     * The array must be long enough to fill the entire object.
     * This method cannot be used to write arrays with more than 
     * <code>Integer.MAX_VALUE</code> elements.
     *
     * @param  value  an array containing the values to be written.
     *                Must contain enough elements to fill the entire object.
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPutvr( float[] value ) throws HDSException;

    /**
     * Write a primitive as if it were vectorised (regardless of its 
     * actual shape) - see 
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_PUTVx">DAT_PUTVx</a>.
     * The array must be long enough to fill the entire object.
     * This method cannot be used to write arrays with more than 
     * <code>Integer.MAX_VALUE</code> elements.
     *
     * @param  value  an array containing the values to be written.
     *                Must contain enough elements to fill the entire object.
     * @throws HDSException   if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native void datPutvd( double[] value ) throws HDSException;

    /**
     * Obtain a reference for an HDSObject - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_REF">DAT_REF</a>.
     * @return                a String giving a name which uniquely identifies
     *                        this HDSObject.  It includes the filename,
     *                        pathname within container file, and slice
     *                        subscript information if relevant.
     */
    public native String
        datRef() throws HDSException;

    /**
     * Enquire object shape - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_SHAPE">DAT_SHAPE</a>.
     *
     * @return               a new array
     *                       representing the shape of this 
     *                       <code>HDSObject</code>
     * @throws HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native long[]
        datShape() throws HDSException;

    /**
     * Enquire object size - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_SIZE">DAT_SIZE</a>.
     *
     * @return                the size of the object.  For an array this will
     *                        be the product of the dimensions, for a 
     *                        scalar, a value of 1 is returned.
     * @throws HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native long datSize() throws HDSException;

    //  Doesn't seem to work.
    //  /**
    //   * Locate slice - see
    //   * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_SLICE">DAT_SLICE</a>.
    //   *
    //   * <p><b>N.B.: This method doesn't seem to work.</b>
    //   *
    //   * @param  lbnd  lower dimension bounds
    //   * @param  ubnd  upper dimension bounds
    //   * @return       an HDSObject representing the new slice
    //   * @throws HDSException  if an HDS error occurs (STATUS is not SAI__OK)
    //   */
    //  public native HDSObject 
    //      datSlice( long[] lbnd, long[] ubnd ) throws HDSException;

    /**
     * Enquire object state - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_STATE">DAT_STATE</a>.
     * Enquires the state of a primitive, i.e. whether its value is defined
     * or not.
     *
     * @return   is this object's data defined?
     */
    public native boolean
        datState() throws HDSException;
 
    /**
     * Enquire whether object is a structure - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_STRUC">DAT_STRUC</a>.
     *
     * @return                <code>true</code> if this object is a structure,
     *                        otherwise <code>false</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native boolean
        datStruc() throws HDSException;

    /**
     * Enquire if a component of a structure exists - see 
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_THERE">DAT_THERE</a>.
     *
     * @param   name          the name of a component whose existence is 
     *                        to be queried
     * @return                <code>true</code> if a component called
     *                        <code>name</code> is 
     *                        contained in this object, otherwise 
     *                        <code>false</code>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native boolean datThere( String name ) throws HDSException;

    /**
     * Enquire object type - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_TYPE">DAT_TYPE</a>.
     *
     * @return                a <code>String</code> giving one of the 
     *                        following HDS type strings:
     *                        <ul>
     *                        <li>"_BYTE"
     *                        <li>"_UBYTE"
     *                        <li>"_WORD"
     *                        <li>"_UWORD"
     *                        <li>"_INTEGER" 
     *                        <li>"_REAL"
     *                        <li>"_DOUBLE" 
     *                        <li>"_LOGICAL"
     *                        <li>"_CHAR"
     *                        <li>"_CHAR*N", where N is the length in characters
     *                        </ul>
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native String 
        datType() throws HDSException;

    /**
     * Unmap an object mapped by another DAT_ routine - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_UNMAP">DAT_UNMAP</a>.
     */
    public native void
        datUnmap() throws HDSException;

    /**
     * Enquire locator validity - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_VALID">DAT_VALID</a>.
     *
     * @return                <code>true</code> if this <code>HDSobject</code> 
     *                        refers to a valid HDS object, <code>false</code> 
     *                        otherwise.  It should normally be true unless
     *                        the <code>datAnnul</code> method has been 
     *                        called on it.
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native boolean
        datValid() throws HDSException;

    /**
     * Find position of primitive in HDS file - see
     * <a href="http://star-www.rl.ac.uk/cgi-bin/htxserver/sun92.htx/?xref_DAT_WHERE">DAT_WHERE</a>.
     * @return                a two-element array of <code>long</code>s.
     *                        The first element gives the number of the
     *                        file block in whch the object's data start
     *                        (HDS blocks are 512 bytes long and are 
     *                        numbered from the beginning of the file, 
     *                        starting at block 1), and the second element
     *                        gives the zero-based byte-offset of the start
     *                        of the data within the file block.
     * @throws  HDSException  if an HDS error occurs (STATUS is not SAI__OK)
     */
    public native long[]
        datWhere() throws HDSException;

}
