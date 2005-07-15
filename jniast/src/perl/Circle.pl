#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "Circle";
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
         name => ( $aName = "form" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "centre" ),
         type => 'double[]',
         descrip => ArgDescrip( $aName ),
      },
      {
         name => ( $aName = "point" ),
         type => 'double[]',
         descrip => ArgDescrip( $aName ),
      },
      {
         name => ( $aName = "unc" ),
         type => 'Region',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print <<'__EOT__';

    /**
     * Create a Circle given a centre and radius.
     *
     * @param  frame   frame in which region will exist (a deep copy is taken)
     * @param  centre  Naxes-element array giving centre of circle region
     * @param  radius  radius of circle region
     * @param  unc     uncertainty associated with the circle's boundary;
     *                 may be null
     * @see    #Circle(uk.ac.starlink.ast.Frame,int,double[],double[],uk.ac.starlink.ast.Region)
     */
    public Circle( Frame frame, double[] centre, double radius, Region unc ) {
        this( frame, 1, centre, new double[] { radius }, unc );
    }

    /**
     * Create a Circle given a centre and point on the circumference.
     *
     * @param  frame   frame in which region will exist (a deep copy is taken)
     * @param  centre  Naxes-element array giving centre of circle region
     * @param  point   Naxes-element array giving a point on the circle
     *                 region's circumference
     * @param  unc     uncertainty associated with the circle's boundary;
     *                 may be null
     * @see    #Circle(uk.ac.starlink.ast.Frame,int,double[],double[],uk.ac.starlink.ast.Region)
     */
    public Circle( Frame frame, double[] centre, double[] point, Region unc ) {
        this( frame, 0, centre, point, unc );
    }
__EOT__

print "}\n";

