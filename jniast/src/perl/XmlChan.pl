#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "XmlChan";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";
print "import java.io.InputStream;\n";
print "import java.io.OutputStream;\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor (Starlink)",
);

print "public class XmlChan extends Channel {\n";

print <<'__EOT__';

    /** XML namespace for elements in AstObject serialization. */
    public static final String AST__XMLNS = getAstConstantC( "AST__XMLNS" );

    /**
     * Creates a channel which reads from the given <code>InputStream</code>
     * and writes to the given <code>OutputStream</code>.
     *
     * @param   in   a stream to read AST objects from.  If <code>null</code>,
     *               then <code>System.in</code> is used.
     * @param   out  a stream to write AST objects to.  If <code>null</code>,
     *               then <code>System.out</code> is used.
     */
    public XmlChan( InputStream in, OutputStream out ) {
        super( in, out );
    }

    /**
     * This constructor does not do all the required construction to
     * create a valid XmlChan object, but is required for inheritance
     * by user subclasses of XmlChan.
     */
    protected XmlChan() {
        super();
    }

__EOT__


my( @args );

@args = (
   name => ( $aName = "xmlFormat" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "xmlIndent" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "xmlLength" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "xmlPrefix" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );


print "}\n";

