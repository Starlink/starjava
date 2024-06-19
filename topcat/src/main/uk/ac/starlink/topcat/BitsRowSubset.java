package uk.ac.starlink.topcat;

import java.util.BitSet;

/**
 * A RowSubset which maintains the inclusion status of each row as
 * a separate flag.
 *
 * @author    Mark Taylor (Starlink)
 */
public class BitsRowSubset extends RowSubset {

     private BitSet bits;
     private boolean invert;

     /**
      * Constructs a new row subset with a given BitSet, name and sense.
      * The <code>invert</code> argument indicates whether the sense of the
      * bit set is to be reversed prior to interpretation.
      *
      * @param   name  subset name
      * @param   bits  flag vector
      * @param   invert  whether to invert the bits from the BitSet
      */
     public BitsRowSubset( String name, BitSet bits, boolean invert ) {
         super( name );
         this.bits = bits;
         this.invert = invert;
     }

     /**
      * Constructs a new row subset with a given BitSet and name.
      * Same as <code>BitsRowSubset(name,bits,false)</code>
      *
      * @param   name  subset name
      * @param   bits  flag vector
      */
     public BitsRowSubset( String name, BitSet bits ) {
         this( name, bits, false );
     }


     /**
      * Returns the <code>BitSet</code> object used to store the inclusion
      * status flags.
      *
      * @return  flag vector
      */
     public BitSet getBitSet() {
         return bits;
     }

     /**
      * Returns the inversion sense of the inclusion flags represented by
      * this subset relative to the bit set.
      *
      * @return  true iff bitset bits are inverted to give inclusion flag
      */
     public boolean getInvert() {
         return invert;
     }

     public boolean isIncluded( long lrow ) {
         return bits.get( (int) lrow ) ^ invert;
     }
}
