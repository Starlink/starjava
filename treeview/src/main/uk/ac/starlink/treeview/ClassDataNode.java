package uk.ac.starlink.treeview;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.swing.JComponent;
import uk.ac.starlink.util.DataSource;

/**
 * Data node representing a Java class.
 * <p>
 * This class is based on using ClassLoaders, but it doesn't really work
 * for several reasons in many cases.  I suspect that the only way to
 * do it really is to write (or rip off) a custom class file parser.
 *
 * @deprecated - badly broken
 * @author   Mark Taylor (Starlink)
 */
public class ClassDataNode extends DefaultDataNode {

    private Class clazz;
    private JComponent fullView;

    private static ClassMaker classMaker = new ClassMaker();

    public ClassDataNode( DataSource datsrc ) throws NoSuchDataException {
        try {
            byte[] magic = new byte[ 4 ];
            datsrc.getMagic( magic );
            if ( ! isMagic( magic ) ) {
                throw new NoSuchDataException( "Not a class file" );
            }
            ByteArrayOutputStream ostrm = new ByteArrayOutputStream();
            InputStream istrm = datsrc.getInputStream();
            int bufsiz = 512;
            byte[] buf = new byte[ bufsiz ];
            for ( int n; ( n = istrm.read( buf ) ) > 0; ) {
                ostrm.write( buf, 0, n );
            }
            istrm.close();
            ostrm.close();
            clazz = makeClass( ostrm.toByteArray() );
            setLabel( clazz.getName() );
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
    }

    public ClassDataNode( Class clazz ) {
        this.clazz = clazz;
        setLabel( clazz.getName() );
    }

    public String getName() {
        return clazz.getName();
    }

    public String getNodeTLA() {
        return "CLS";
    }

    public String getNodeType() {
        return "Java class";
    }

    public boolean allowsChildren() {
        return clazz.getClasses().length + clazz.getInterfaces().length > 0;
    }

    protected DataNode[] getChildren() {
        Class[] members = clazz.getClasses();
        int nmem = members.length;
        DataNode[] children = new DataNode[ nmem ];
        DataNodeFactory childMaker = getChildMaker();
        for ( int i = 0; i < nmem; i++ ) {
            try {
                children[ i ] = childMaker.makeDataNode( this, members[ i ] );
            }
            catch ( NoSuchDataException e ) {
                children[ i ] = childMaker.makeErrorDataNode( this, e );
            }
        }
        return children;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();

            dv.addSeparator();
            dv.addKeyedItem( "Type", 
                             clazz.isInterface() ? "interface" : "class" );

            dv.addSubHead( "Inheritance" );
            for ( Class parent = clazz.getSuperclass(); parent != null; 
                  parent = parent.getSuperclass() ) {
                dv.addText( "extends " + parent.getName() );
            }

            Class[] ifs = clazz.getInterfaces();
            if ( ifs.length > 0 ) {
                dv.addSubHead( "Interfaces" );
                for ( int i = 0; i < ifs.length; i++ ) {
                    dv.addText( "implements " + ifs[ i ].getName() );
                }
            }

            dv.addSubHead( "Modifiers" );
            dv.addText( Modifier.toString( clazz.getModifiers() ) );

            dv.addSubHead( "Methods" );
            Method[] methods = clazz.getMethods();
            for ( int i = 0; i < methods.length; i++ ) {
                dv.addText( methods[ i ].toString() );
            }
        }
        return fullView;
    }

    private static Class makeClass( byte[] buf ) throws NoSuchDataException {
        try {
            return classMaker.makeClass( buf );
        }
        catch ( LinkageError e ) {
            throw new NoSuchDataException( e );
        }
    }

    public static boolean isMagic( byte[] magic ) {
        return ( magic.length >= 4 )
            && magic[ 0 ] == (byte) 0xca
            && magic[ 1 ] == (byte) 0xfe
            && magic[ 2 ] == (byte) 0xba
            && magic[ 3 ] == (byte) 0xbe;
    }

    private static class ClassMaker extends ClassLoader {
        Class makeClass( byte[] buf ) {
            return defineClass( null, buf, 0, buf.length );
        }
    }
}
