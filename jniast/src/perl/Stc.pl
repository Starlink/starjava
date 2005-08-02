#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "Stc";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor",
);

print "public abstract class Stc extends Region {\n";

print <<'__EOT__';

    private static final String STCNAME = getAstConstantC( "AST__STCNAME" );
    private static final String STCVALUE = getAstConstantC( "AST__STCVALUE" );
    private static final String STCERROR = getAstConstantC( "AST__STCERROR" );
    private static final String STCRES = getAstConstantC( "AST__STCRES" );
    private static final String STCSIZE = getAstConstantC( "AST__STCSIZE" );
    private static final String STCPIXSZ = getAstConstantC( "AST__STCPIXSZ" );

    /**
     * Package-private default constructor for abstract class.
     */
    Stc() {
    }

    /**
     * Returns the number of AstroCoords elements stored within 
     * this <code>Stc</code> when it was constructed. 
     *
     * @return   number of coords stored
     */
    public native int getStcNCoord();

    /**
     * Returns one of the AstroCoords elements stored within this 
     * <code>Stc</code> when it was constructed.
     * If the coordinate system represented by this <code>Stc</code> 
     * has been changed since it was created (for instance, by 
     * changing its System attribute), then the sizes and positions 
     * in the returned <code>AstroCoords</code> object 
     * will reflect the change in coordinate system.
     *
     * @param   i  index of coords to retrieve.  The first index is 1.
     * @return  <code>i</code>'th coord stored
     */
    public AstroCoords getStcCoord( int i ) {
        return keyMapToAstroCoords( getStcCoordKeyMap( i ) );
    }
    private native KeyMap getStcCoordKeyMap( int i );

    /**
     * Converts an AstroCoords object to an equivalent KeyMap object.
     *
     * @param  astroCoords  AstroCoords object
     * @return   KeyMap object
     */
    static KeyMap astroCoordsToKeyMap( AstroCoords astroCoords ) {
        KeyMap kmap = new KeyMap();
        String[] axisNames = astroCoords.getName();
        if ( axisNames != null && axisNames.length > 0 ) {
            kmap.mapPut1C( STCNAME, axisNames, "STC axis names" );
        }
        Region value = astroCoords.getValue();
        if ( value != null ) {
            kmap.mapPut0A( STCVALUE, value, "STC value" );
        }
        Region error = astroCoords.getError();
        if ( error != null ) {
            kmap.mapPut0A( STCERROR, error, "STC error" );
        }
        Region res = astroCoords.getResolution();
        if ( res != null ) {
            kmap.mapPut0A( STCRES, res, "STC resolution" );
        }
        Region size = astroCoords.getSize();
        if ( size != null ) {
            kmap.mapPut0A( STCSIZE, size, "STC size" );
        }
        Region pixSize = astroCoords.getPixSize();
        if ( pixSize != null ) {
            kmap.mapPut0A( STCPIXSZ, pixSize, "STC pixel size" );
        }
        return kmap;
    }

    /**
     * Converts a KeyMap object to an equivalent AstroCoords object.
     *
     * @param   keyMap   KeyMap object
     * @return  AstroCoords object
     */
    static AstroCoords keyMapToAstroCoords( KeyMap keyMap ) {
        AstroCoords ac = new AstroCoords();
        if ( keyMap.mapType( STCNAME ) == KeyMap.AST__STRINGTYPE ) {
            ac.setName( keyMap.mapGet1C( STCNAME, 100 ) );
        }
        if ( keyMap.mapType( STCVALUE ) == KeyMap.AST__OBJECTTYPE ) {
            AstObject value = keyMap.mapGet0A( STCVALUE );
            if ( value instanceof Region ) {
                ac.setValue( (Region) value );
            }
        }
        if ( keyMap.mapType( STCERROR ) == KeyMap.AST__OBJECTTYPE ) {
            AstObject error = keyMap.mapGet0A( STCERROR );
            if ( error instanceof Region ) {
                ac.setError( (Region) error );
            }
        }
        if ( keyMap.mapType( STCRES ) == KeyMap.AST__OBJECTTYPE ) {
            AstObject res = keyMap.mapGet0A( STCRES );
            if ( res instanceof Region ) {
                ac.setResolution( (Region) res );
            }
        }
        if ( keyMap.mapType( STCSIZE ) == KeyMap.AST__OBJECTTYPE ) {
            AstObject size = keyMap.mapGet0A( STCSIZE );
            if ( size instanceof Region ) {
                ac.setSize( (Region) size );
            }
        }
        if ( keyMap.mapType( STCPIXSZ ) == KeyMap.AST__OBJECTTYPE ) {
            AstObject pixsz = keyMap.mapGet0A( STCPIXSZ );
            if ( pixsz instanceof Region ) {
                ac.setPixSize( (Region) pixsz );
            }
        }
        return ac;
    }

    /**
     * Converts an array of AstroCoords objects to an equivalent array
     * of KeyMap objects.
     *
     * @param  coords  array of AstroCoords objects
     * @return  array of KeyMap objects
     */
    static KeyMap[] astroCoordsToKeyMaps( AstroCoords[] coords ) {
        if ( coords == null ) {
            return null;
        }
        else {
            KeyMap[] kmaps = new KeyMap[ coords.length ];
            for ( int i = 0; i < coords.length; i++ ) {
                kmaps[ i ] = astroCoordsToKeyMap( coords[ i ] );
            }
            return kmaps;
        }
    }
__EOT__

makeNativeMethod(
   name => ( $fName = "getStcRegion" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'Region', descrip => ReturnDescrip( $fName ), },
   params => [],
);

print "}\n";

