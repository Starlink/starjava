#!/usr/bin/perl -w

use strict;

my $package = "uk.ac.starlink.array";
my @types = qw/byte short int float double/;
my $outfile = $ARGV[ 0 ];

if ( $outfile ) {
   open( OUT, ">$outfile" ) or die( "Couldn't open '$outfile' for output\n" );
   select OUT;
}

print <<__END__;

/*
 * *************************************************************************
 * ** NOTE: Do not edit; this java source was written by ConvertWorker.pl **
 * *************************************************************************
 */

package $package;

abstract class ConvertWorker {

    abstract boolean isUnit();
    abstract void convert( Object src, int srcPos, 
                           Object dest, int destPos, int length );


    private static final byte MIN_BYTE = (byte) 0x81;
    private static final short MIN_SHORT = (short) 0x8001;
    private static final int MIN_INT = 0x80000001;
    private static final float MIN_FLOAT = -Float.MAX_VALUE;
    private static final double MIN_DOUBLE = -Double.MAX_VALUE;

    private static final byte MAX_BYTE = (byte) 0x7f;
    private static final short MAX_SHORT = (short) 0x7fff;
    private static final int MAX_INT = 0x7fffffff;
    private static final float MAX_FLOAT = Float.MAX_VALUE;
    private static final double MAX_DOUBLE = Double.MAX_VALUE;

    private static abstract class NonunitConvertWorker extends ConvertWorker {
        private final BadHandler destHandler;
        NonunitConvertWorker( BadHandler destHandler ) {
            this.destHandler = destHandler;
        }
        final boolean isUnit() {
            return false;
        }
    }


    static ConvertWorker makeWorker( Type sType, BadHandler sHandler,
                                     Type dType, BadHandler dHandler ) {
        return makeWorker( sType, sHandler, dType, dHandler, null );
    }
                                     

    static ConvertWorker makeWorker( Type sType, final BadHandler sHandler,
                                     Type dType, final BadHandler dHandler, 
                                     final Mapper mapper ) {

        if ( false ) {
            return null;
            // dummy clause
        }

__END__

    my $stype;
    my $sItype = 0;
    foreach $stype ( @types ) {
        my $sType = ucfirst( $stype );
        my $sTYPE = uc( $stype );
        my $sClass = ( $sType eq 'Int' ) ? 'Integer' : $sType;
        $sItype++;
        my $dItype = 0;
        my $dtype;

            print <<__END__;

        else if ( sType == Type.$sTYPE && dType == Type.$sTYPE && 
                  sHandler.equals( dHandler ) &&
                  mapper == null ) {
            return new ConvertWorker() {
                final boolean isUnit() {
                    return true;
                }
                final void convert( Object src, int srcPos,
                                    Object dest, int destPos, int length ) {
                    System.arraycopy( src, srcPos, dest, destPos, length );
                }
            };
        }
__END__

        foreach $dtype ( @types ) {
            my $dType = ucfirst( $dtype );
            my $dTYPE = uc( $dtype );
            my $dClass = ( $dType eq 'Int' ) ? 'Integer' : $dType;
            $dItype++;

            print <<__END__;

        else if ( sType == Type.$sTYPE && dType == Type.$dTYPE ) {
            $dtype\[\] destbad = ($dtype\[\]) Type.$dTYPE.newArray( 1 );
            dHandler.putBad( destbad, 0 );
            final $dtype dbad = destbad[ 0 ];
__END__

            if ( $sItype <= $dItype ) {
                print <<__END__;

            if ( mapper == null ) {
                return new NonunitConvertWorker( dHandler ) {
                    final void convert( Object source, int srcPos,
                                        Object destination, int destPos,
                                        int length ) {
                        $stype\[\] src = ($stype\[\]) source;
                        $dtype\[\] dest = ($dtype\[\]) destination;
                        while ( length-- > 0 ) {
                            boolean isBad = sHandler.isBad( source, srcPos );
                            dest\[ destPos \] = 
                                isBad ? dbad
                                      : ($dtype) src\[ srcPos \];
                            destPos++;
                            srcPos++;
                        }
                    }
                };
            }
__END__

            }
            else {
                print <<__END__;

            if ( mapper == null ) {
                return new NonunitConvertWorker( dHandler ) {
                    final void convert( Object source, int srcPos,
                                        Object destination, int destPos,
                                        int length ) {
                        $stype\[\] src = ($stype\[\]) source;
                        $dtype\[\] dest = ($dtype\[\]) destination;
                        while ( length-- > 0 ) {
                            $stype sval = src\[ srcPos \];
                            boolean isBad = sHandler.isBad( source, srcPos ) 
                                         || sval < MIN_$dTYPE
                                         || sval > MAX_$dTYPE;
                            dest\[ destPos \] = 
                                isBad ? dbad
                                      : ($dtype) sval;
                            destPos++;
                            srcPos++;
                        }
                    }
                };
            }

__END__

            }
            print <<__END__;

            else {
                return new NonunitConvertWorker( dHandler ) {
                    final void convert( Object source, int srcPos,
                                        Object destination, int destPos,
                                        int length ) {
                        $stype\[\] src = ($stype\[\]) source;
                        $dtype\[\] dest = ($dtype\[\]) destination;
                        while ( length-- > 0 ) {
                            if ( sHandler.isBad( source, srcPos ) ) {
                                dest\[ destPos \] = dbad;
                            }
                            else {
                                double dvald =
                                    mapper.func( (double) src\[ srcPos \] );
                                boolean isBad = Double.isNaN( dvald ) 
                                             || dvald < (double) MIN_$dTYPE
                                             || dvald > (double) MAX_$dTYPE;
                                dest\[ destPos \] = 
                                    isBad ? dbad
                                          : ($dtype) dvald;
                            }
                            destPos++;
                            srcPos++;
                        }
                    }
                };
            }

__END__

            print <<__END__;
        }

__END__

        }
    }

    print <<__END__;
        else {
            // assert false;
            return null;
        }
    }

    static interface Mapper {
        double func( double x );
    }
}
__END__

