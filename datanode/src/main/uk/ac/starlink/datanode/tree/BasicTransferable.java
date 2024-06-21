package uk.ac.starlink.datanode.tree;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.util.DataSource;

/**
 * A basic implementation of the <code>Transferable</code> interface used
 * to implement drag'n'drop operations.  It initially supports no
 * {@link java.awt.datatransfer.DataFlavor}s, but they can be added
 * by using the various <code>add*</code> methods provided.  They should
 * be called in order of priority (most specific first).
 *
 * @author   Mark Taylor (Starlink)
 */
public class BasicTransferable implements Transferable {

    List flavorList = new ArrayList();
    Map dataMap = new HashMap();
    private static DataFlavor stringFlavor = DataFlavor.stringFlavor;
    private Class sourceStreamClass = SourceInputStream.class;

    /**
     * Adds an object which can be transferred within a single JVM.
     *
     * @param  obj  the object
     * @param  clazz  the class as which it should be declared in the MIME type
     * @param  type  a human-readable description of the type
     */
    public void addLocalObject( Object obj, Class clazz, String type ) {
        String localMimeType = DataFlavor.javaJVMLocalObjectMimeType
                             + "; class=" + clazz.getName();
        DataFlavor flavor = new DataFlavor( localMimeType, type );
        store( flavor, obj );
    }

    /**
     * Adds a {@link java.io.Serializable} object which can be serialized
     * and thus passed outside of this JVM.
     *
     * @param  obj  the serializable object
     * @param  clazz  the class as which it should be declared in the MIME type
     * @param  type  a human-readable description of the type
     */
    public void addSerializableObject( Serializable obj, Class clazz,
                                       String type ) {
        DataFlavor flavor = new DataFlavor( clazz, type );
        store( flavor, obj );
    }

    /**
     * Adds a URL.  This is currently installed as both a local and a 
     * serializable object.
     *
     * @param   url  the URL
     */
    public void addURL( URL url ) {
        addSerializableObject( url, URL.class, "URL" );
    }

    /**
     * Adds a plain text string.
     *
     * @param  text  the string
     */
    public void addString( String text ) {
        store( stringFlavor, text );
    }

    /**
     * Adds streamed data.
     *
     * @param  datsrc  the DataSource holding the data
     * @param  mimeType  the MIME type with which the data should declare
     *         itself
     */
    public void addDataSource( DataSource datsrc, String mimeType ) {
        String fullMimeType = mimeType; 
        try {
            DataFlavor flavor = new DataFlavor( fullMimeType );
            store( flavor, datsrc );
        }
        catch ( ClassNotFoundException e ) {
            throw new AssertionError( e );
        }
    }

    /**
     * Stores a supported flavor with the object on which the transfer 
     * data is based for that flavor.
     *
     * @param  flavor  the supported DataFlavor
     * @param  obj   the object from which the transfer data can be derived.
     *               How this is done depends on the flavor
     */
    private void store( DataFlavor flavor, Object obj ) {
        dataMap.put( flavor, obj );
        flavorList.add( flavor );
    }

    /*
     * The following methods implement the Transferable interface.
     */

    public DataFlavor[] getTransferDataFlavors() {
        return (DataFlavor[]) flavorList.toArray( new DataFlavor[ 0 ] );
    }

    public boolean isDataFlavorSupported( DataFlavor flavor ) {
        return dataMap.containsKey( flavor );
    }

    public Object getTransferData( DataFlavor flavor )
            throws IOException, UnsupportedFlavorException {
        String mimeType = flavor.getMimeType();
        Class clazz = flavor.getRepresentationClass();

        /* If the MIME type represents a local object, dispense the object
         * itself. */
        if ( mimeType.startsWith( DataFlavor.javaJVMLocalObjectMimeType ) ) {
            return dataMap.get( flavor );
        }

        /* If it represents a serializable object, serialize it and 
         * dispense an input stream containing its serialised form. */
        else if ( mimeType.startsWith( DataFlavor
                                      .javaSerializedObjectMimeType ) ) {
            return dataMap.get( flavor );
        }

        /* If it represents a String, use the StringSelection object's
         * implementation to return the correct object. */
        else if ( mimeType.equals( stringFlavor.getMimeType() ) ) {
            Object obj = dataMap.get( flavor );
            Transferable stringTrans = new StringSelection( (String) obj );
            assert stringTrans.isDataFlavorSupported( stringFlavor );
            return stringTrans.getTransferData( stringFlavor );
        }

        /* If it represents a DataSource, we need to supply it in an
         * InputStream. */
        else if ( clazz.equals( InputStream.class ) ) {
            return new SourceInputStream( (DataSource) dataMap.get( flavor ) );
        }

        /* We haven't put any other objects into our map. */
        else {
            throw new UnsupportedFlavorException( flavor );
        }
    }

    /**
     * Input stream used for squirting data from a DataSource to the
     * recipient of a Transferable.  All the clever stuff is done by 
     * the Swing or AWT DnD infrastructure, but what we need to provide
     * here is an InputStream subclass which has the following things:
     * <ul>
     * <li>A constructor which takes a single arg
     *     of type <code>InputStream</code>
     * <li>A <code>read(byte[],int,int)</code> method
     * </ul>
     * (see the Java Drag And Drop Specification).
     * I must admit, I don't fully understand this; I suspect it's 
     * providing some added flexibility which I don't understand.
     */
    private static class SourceInputStream extends FilterInputStream {

        public SourceInputStream( DataSource datsrc ) throws IOException {
            this( datsrc.getInputStream() );
        }

        public SourceInputStream( InputStream istrm ) {
            super( istrm );
        }

    }

}
