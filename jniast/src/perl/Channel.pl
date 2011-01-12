#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "Channel";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

print "import java.io.BufferedReader;\n";
print "import java.io.IOException;\n";
print "import java.io.InputStream;\n";
print "import java.io.InputStreamReader;\n";
print "import java.io.OutputStream;\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => q{
      This class is used for reading and writing AST objects from/to
      external media.  The <code>Channel</code> class itself reads
      from <code>System.in</code> or another <code>InputStream</code>
      and writes to <code>System.out</code> or another 
      <code>OutputStream</code>.
      To perform I/O to some other object, extend this class and
      override the <code>source</code> and <code>sink</code> methods.
   },
   author => "Mark Taylor",
);

print <<'__EOT__';

public class Channel extends AstObject {

    /* Holds the C pointer to a data structure used by native code. */
    private long chaninfo;

    private BufferedReader inreader;
    private OutputStream outstream;

    /**
     * Perform initialization required for JNI code at class load time.
     */
    static {
        nativeInitializeChannel();
    }
    private native static void nativeInitializeChannel();

    /**
     * Creates a channel which reads from the given <code>InputStream</code>
     * and writes to the given <code>OutputStream</code>.
     *
     * @param   in   a stream to read AST objects from.  If <code>null</code>,
     *               then <code>System.in</code> is used.
     * @param   out  a stream to write AST objects to.  If <code>null</code>,
     *               then <code>System.out</code> is used.
     */
    public Channel( InputStream in, OutputStream out ) {
        if ( in == null ) {
            in = System.in;
        }
        if ( out == null ) {
            out = System.out;
        }
        inreader = new BufferedReader( new InputStreamReader( in ) );
        outstream = out;
        construct();
    }

    /**
     * This constructor does not do all the required construction to
     * create a valid Channel object, but is required for inheritance
     * by user subclasses of Channel.
     */
    protected Channel() {
        construct();
    }

    /**
     * This is a dummy constructor which does nothing at all.  It is invoked
     * by the FitsChan constructor to prevent it having to use the
     * no-argument constructor which does things FitsChan does not want.
     */
    protected Channel( Channel dummy ) {
    }

    /**
     * Finalizes the object.  Certain resources allocated by the native
     * code are freed, and the finalizer of the superclass is called.
     */
    protected void finalize() throws Throwable {
        try {
            destroy();
        }
        finally {
            super.finalize();
        }
    }

    /**
     * Reads a <code>String</code> which forms one line of the textual
     * representation of an AST object from the channel's input stream.
     * If the end of the stream is reached, <code>null</code> is returned.
     * If an <code>IOException</code> occurs during the reading,
     * it is thrown.
     * <p>
     * This method is called by the <code>read</code> method.
     * To implement a channel which reads from a source other than
     * an <code>InputStream</code>, override this method.  The method
     * should return <code>null</code> when there is no more input,
     * and may throw an IOException in case of error.
     *
     * @return  a line of text read from the input stream, as a
     *          <code>String</code>.  If the end of the stream has been
     *          reached, <code>null</code> is returned.
     * @throws  IOException  if an I/O error occurs during reading
     */
    protected String source() throws IOException {
        return inreader.readLine();
    }

    /**
     * Writes a <code>String</code> which forms one line of the textual
     * representation of an AST object to the channel's output stream.
     * If an <code>IOException</code> occurs during the writing,
     * it is thrown.
     * <p>
     * This method is called by the <code>write</code> method.
     * To implement a channel which writes to a source other than
     * an <code>OutputStream</code>, override this method.  The method
     * can do anything it likes with its argument, and may throw
     * an exception in case of error.
     *
     * @param  line  a <code>String</code> which forms one line of the
     *         textual description of an AST object which is being
     *         written.
     * @throws IOException  if an I/O error occurs during writing.
     */
    protected void sink( String line ) throws IOException {
        outstream.write( line.getBytes() );
        outstream.write( (byte) '\n' );
    }

    /**
     * Performs native operations required for construction of a valid
     * Channel.
     */
    private void construct() {
        if ( this instanceof XmlChan ) {
            constructXmlChan();
        }
        else {
            constructChannel();
        }
    }

    private native void constructChannel();
    private native void constructXmlChan();
    private native void destroy();

    /**
     * Reads an AST object from this channel.  The <code>source</code>
     * method is invoked to obtain the textual representation.
     *
     * @return   the <code>AstObject</code> which has been read.
     *           <code>null</code> is returned, without error, if no
     *           further objects remain to be read on the stream
     * @throws   IOException  if such an exception was generated by the
     *                        <code>source</code> method
     * @throws   AstException  if an error occurs in the AST library
     */
    public native AstObject read() throws IOException;

    /**
     * Writes an AST object to this channel.  The <code>sink</code>
     * method is invoked to send the textual representation.
     *
     * @param    obj  an <code>AstObject</code> to be written
     * @return   number of objects written (1 on success)
     * @throws   IOException   if such an exception was generated by the
     *                         <code>sink</code> method
     * @throws   AstException  if an error occurs in the AST library
     */
    public native int write( AstObject obj ) throws IOException;

    /**
     * This method is currently unsupported for Channel and its subclasses
     * because of difficulties in its implementation, and because it is
     * probably not that useful.
     *
     * @throws   UnsupportedOperationException
     */
    public AstObject copy() {
        /* It might not be all that hard to support, but you'd need to
         * make sure that the native data structures were filled appropriately.
         * Leave implementation until someone actually wants to use it. */
        throw new UnsupportedOperationException(
            "Sorry - copy not currently supported for Channels" );
    }

__EOT__

makeNativeMethod(
   name => ( $fName = "warnings" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'KeyMap', descrip => ReturnDescrip( $fName ) },
   params => [],
);

my( @commentArgs ) = (
   name => ( $aName = "comment" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @commentArgs );
makeSetAttrib( @commentArgs );

my( @fullArgs ) = (
   name => ( $aName = "full" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @fullArgs );
makeSetAttrib( @fullArgs );

my( @reportLevelArgs ) = (
   name => ( $aName = "reportLevel" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @reportLevelArgs );
makeSetAttrib( @reportLevelArgs );

my( @skipArgs ) = (
   name => ( $aName = "skip" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @skipArgs );
makeSetAttrib( @skipArgs );

my( @strictArgs ) = (
   name => ( $aName = "strict" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @strictArgs );
makeSetAttrib( @strictArgs );

print "}\n";

