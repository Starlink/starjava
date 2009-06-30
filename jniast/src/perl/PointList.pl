#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "PointList";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor",
);

print "public class $cName extends Region {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => FuncDescrip( $cName ),
   params => [
      {
         name => ( $aName = "frame" ),
         type => 'Frame',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "npnt" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "points" ),
         type => 'double[][]',
         descrip => q{
            An array giving the coordinates in <code>frame</code> of the
            points.  <code>points</code> is an <code>naxes</code>-element 
            array of
            <code>npnt</code>-element <code>double</code> arrays, 
            where <code>naxes</code> is the number of axes in 
            <code>frame</code>.  The value of coordinate number 
            <code>icoord</code> for point number
            <code>ipoint</code> is therefore stored at 
            <code>points[icoord][ipoint]</code>.
         },
      },
      {
         name => ( $aName = "unc" ),
         type => 'Region',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

my( @args );

@args = (
   name => ( $aName = "listSize" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );

print "}\n";

