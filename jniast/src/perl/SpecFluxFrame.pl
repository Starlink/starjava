#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "SpecFluxFrame";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor (Starlink)",
);

print <<'__EOT__';
public class SpecFluxFrame extends CmpFrame {

    /**
     * Creates a SpecFluxFrame.
     *
     * @param  frame1  SpecFrame which will form the first axis in the 
     *         new SpecFluxFrame
     * @param  frame2  FluxFrame which will form the second axis in the
     *         new SpecFluxFrame
     */
    public SpecFluxFrame( SpecFrame frame1, FluxFrame frame2 ) {
        super( frame1, frame2 );
        construct( frame1, frame2 );
    }
    private native void construct( SpecFrame frame1, FluxFrame frame2 );
}
__EOT__

