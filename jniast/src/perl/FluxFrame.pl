#!/user/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "FluxFrame";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor (Starlink)",
);

print "public class FluxFrame extends Frame {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName with given spectral frame and value",
   descrip => "",
   params => [
      {
         name => ( $aName = "specval" ),
         type => 'double',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "specfrm" ),
         type => 'SpecFrame',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print <<'__EOT__';

    /**
     * Creates a FluxFrame with no default spectral value or frame.
     */
    public FluxFrame() {
        this( AST__BAD, null );
    }
    
__EOT__

my( @args );

@args = (
   name => ( $aName = "specVal" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

print "}\n";

