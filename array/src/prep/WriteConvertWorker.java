/**
 * This <tt>main</tt> method of this class writes the source code for the 
 * {@link uk.ac.starlink.array.ConvertWorker} class.
 * It would be too error-prone to write it by hand.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 May 2004
 */
public class WriteConvertWorker {

    public static void main( String[] args ) {

        out( new String[] {
            "",
            "/*",
            " * ***************************************************************************",
            " * ** NOTE: Do not edit; this java source was written by WriteConvertWorker **",
            " * ***************************************************************************",
            " */",
            "",
        } );
        
        out( new String[] { 
            "package uk.ac.starlink.array;",
            "",
            "abstract class ConvertWorker {",
            "",
            "    abstract boolean isUnit();",
            "    abstract void convert( Object src, int srcPos,",
            "                           Object dest, int destPos, int length );",
            "",
            "    private static final byte MIN_BYTE = (byte) 0x81;",
            "    private static final short MIN_SHORT = (short) 0x8001;",
            "    private static final int MIN_INT = 0x80000001;",
            "    private static final float MIN_FLOAT = -Float.MAX_VALUE;",
            "    private static final double MIN_DOUBLE = -Double.MAX_VALUE;",
            "",
            "    private static final byte MAX_BYTE = (byte) 0x7f;",
            "    private static final short MAX_SHORT = (short) 0x7fff;",
            "    private static final int MAX_INT = 0x7fffffff;",
            "    private static final float MAX_FLOAT = Float.MAX_VALUE;",
            "    private static final double MAX_DOUBLE = Double.MAX_VALUE;",
            "",
            "    private static abstract class NonunitConvertWorker extends ConvertWorker {",
            "        private final BadHandler destHandler;",
            "        NonunitConvertWorker( BadHandler destHandler ) {",
            "            this.destHandler = destHandler;",
            "        }",
            "        final boolean isUnit() {",
            "            return false;",
            "        }",
            "    }",
            "",
            "    static ConvertWorker makeWorker( Type sType, BadHandler sHandler,",
            "                                     Type dType, BadHandler dHandler ) {",
            "        return makeWorker( sType, sHandler, dType, dHandler, null );",
            "    }",
            "",
            "    static ConvertWorker makeWorker( Type sType, final BadHandler sHandler,",
            "                                     Type dType, final BadHandler dHandler,",
            "                                     final Mapper mapper ) {",
            "        if ( false ) {",
            "            return null;",
            "            // dummy clause",
            "        }",
        } );

        TypeRep[] types = TypeRep.ALL_TYPES;
        for ( int is = 0; is < types.length; is++ ) {
            TypeRep s = types[ is ];
            out( new String[] {
                "        else if ( sType == Type." + s.type + " &&" +
                                 " dType == Type." + s.type + " &&",
                "                  sHandler.equals( dHandler ) &&",
                "                  mapper == null ) {",
                "            return new ConvertWorker() {",
                "                final boolean isUnit() {",
                "                    return true;",
                "                }",
                "                final void convert( Object src, int srcPos,",
                "                                    Object dest, int destPos, int length ) {",
                "                    System.arraycopy( src, srcPos, dest, destPos, length );",
                "                }",
                "            };",
                "        }",
            } );

            for ( int id = 0; id < types.length; id++ ) {
                TypeRep d = types[ id ];
                out( new String[] {
                    "        else if ( sType == Type." + s.type + " &&" +
                                     " dType == Type." + d.type + " ) {",
                    "            " + d.primitive + "[] destbad = " +
                               "(" + d.primitive + "[]) Type." +
                               d.type + ".newArray( 1 );",
                    "            dHandler.putBad( destbad, 0 );",
                    "            final " + d.primitive + " dbad = destbad[ 0 ];",
                } );
                if ( is <= id ) {
                    out( new String[] {
                        "            if ( mapper == null ) {",
                        "                return new NonunitConvertWorker( dHandler ) {",
                        "                    final void convert( Object source, int srcPos,",
                        "                                        Object destination, int destPos,",
                        "                                        int length ) {",
                        "                        " + s.primitive + "[] src = " +
                                               "(" + s.primitive + "[]) source;",
                        "                        " + d.primitive + "[] dest = " +
                                               "(" + d.primitive + "[]) destination;",
                        "                        while ( length-- > 0 ) {",
                        "                            boolean isBad = sHandler.isBad( source, srcPos );",
                        "                            dest[ destPos ] =",
                        "                                isBad ? dbad",
                        "                                      : (" + d.primitive + ") src[ srcPos ];",
                        "                            destPos++;",
                        "                            srcPos++;",
                        "                        }",
                        "                    }",
                        "                };",
                        "            }",
                    } );
                }
                else {
                    out( new String[] {
                        "            if ( mapper == null ) {",
                        "                return new NonunitConvertWorker( dHandler ) {",
                        "                    final void convert( Object source, int srcPos,",
                        "                                        Object destination, int destPos,",
                        "                                        int length ) {",
                        "                        " + s.primitive + "[] src = " +
                                               "(" + s.primitive + "[]) source;",
                        "                        " + d.primitive + "[] dest = " +
                                               "(" + d.primitive + "[]) destination;",
                        "                        while ( length-- > 0 ) {",
                        "                            " + s.primitive + " sval = src[ srcPos ];",
                        "                            boolean isBad = sHandler.isBad( source, srcPos )",
                        "                                         || sval < MIN_" +
                                                                     d.primitive.toUpperCase(),
                        "                                         || sval > MAX_" +
                                                                     d.primitive.toUpperCase() + ";",
                        "                            dest[ destPos ] =",
                        "                                isBad ? dbad",
                        "                                      : (" + d.primitive + ") sval;",
                        "                            destPos++;",
                        "                            srcPos++;",
                        "                        }",
                        "                    }",
                        "                };",
                        "            }",
                    } );
                }
                out( new String[] {
                    "            else {",
                    "                return new NonunitConvertWorker( dHandler ) {",
                    "                    final void convert( Object source, int srcPos,",
                    "                                        Object destination, int destPos,",
                    "                                        int length ) {",
                    "                        " + s.primitive + "[] src = " +
                                           "(" + s.primitive + "[]) source;",
                    "                        " + d.primitive + "[] dest = " +
                                           "(" + d.primitive + "[]) destination;",
                    "                        while ( length-- > 0 ) {",
                    "                            if ( sHandler.isBad( source, srcPos ) ) {",
                    "                                dest[ destPos ] = dbad;",
                    "                            }",
                    "                            else {",
                    "                                double dvald =",
                    "                                    mapper.func( (double) src[ srcPos ] );",
                    "                                boolean isBad = Double.isNaN( dvald )",
                    "                                             || dvald < (double) MIN_" +
                                                                     d.primitive.toUpperCase(),
                    "                                             || dvald > (double) MAX_" +
                                                                     d.primitive.toUpperCase() + ";",
                    "                                dest[ destPos ] =",
                    "                                    isBad ? dbad",
                    "                                          : (" + d.primitive + ") dvald;",
                    "                            }",
                    "                            destPos++;",
                    "                            srcPos++;",
                    "                        }",
                    "                    }",
                    "                };",
                    "            }",
                } );
                out( "        }" );
            }
        }

        out( new String[] {
            "        else {",
            "            // assert false;",
            "            return null;",
            "        }",
            "    }",
            "",
            "    static interface Mapper {",
            "        double func( double x );",
            "    }",
            "}",
        } );
    }

    private static void out( String[] lines ) {
        for ( int i = 0; i < lines.length; i++ ) {
            out( lines[ i ] );
        }
    }
    private static void out( String line ) {
        System.out.println( line );
    }

    private static class TypeRep {
        private final String type;
        private final String primitive;
        private final String wrapper;
        private TypeRep( String type, String wrapper, String primitive ) {
            this.type = type;
            this.wrapper = wrapper;
            this.primitive = primitive;
        }
        public static final TypeRep[] ALL_TYPES = new TypeRep[] {
            new TypeRep( "BYTE", "Byte", "byte" ),
            new TypeRep( "SHORT", "Short", "short" ),
            new TypeRep( "INT", "Integer", "int" ),
            new TypeRep( "FLOAT", "Float", "float" ),
            new TypeRep( "DOUBLE", "Double", "double" ),
        };
    }
}
