#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "FitsChan";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";
print "import java.util.Iterator;\n";
print "import java.util.NoSuchElementException;\n";
print "import java.io.*;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
   extra => q{
      <h4>Usage</h4>
      The FitsChan implemented by this class can be used as a buffer for
      FITS header cards, but it neither reads from nor writes to an
      external source.  To make a more useful object, subclass this
      class and override the <code>source</code> and/or <code>sink</code>
      methods.  Note that output is only guaranteed to get written
      if the <code>finalize</code> method is explicitly called.
   },
);

print <<'__EOT__';

public class FitsChan extends Channel {

    /* Holds the C pointer to a data structure used by native code. */
    private long chaninfo;

    /* An iterator used by the source method. */
    private Iterator cardIt;

    /**
     * Creates a new FitsChan whose initial contents will be a sequence of
     * FITS header cards obtained from an Iterator.
     *
     * @param   cardIt  an Iterator which should supply Strings giving the
     *                  channel's initial content.  Only the first 80
     *                  characters of each supplied string are significant.
     *                  This parameter may be supplied null in the case of
     *                  no initial content.
     */
    public FitsChan( Iterator cardIt ) {
        if ( cardIt == null ) {
            cardIt = new Iterator() {
                public boolean hasNext() {
                    return false;
                }
                public Object next() {
                    throw new NoSuchElementException();
                }
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        this.cardIt = cardIt;

        /* Perform native initialisation. */
        construct();
    }

    /**
     * Creates a new FitsChan which can be used as a buffer for
     * FITS objects, but will not read or write header cards.
     */
    public FitsChan() {
        this( null );
    }
    private native void construct();

    /**
     * Returns an iterator over the header cards currently in this FitsChan.
     * Each object returned from the Iterator's <code>next</code> method
     * will be an 80-character String.  The Iterator's <code>remove</code>
     * method can be used to delete cards from the underlying channel.
     * The iterator should not be used while the FitsChan is being 
     * written to.
     * <p>
     * This method is a convenience wrapper which uses <code>findFits</code>
     * and <code>delFits</code> to do the work.
     *
     * @return  an Iterator which retrieves each line from the FitsChan in turn
     */
    public Iterator iterator() {
        return new Iterator() {
            private int icard = 1;
            private int removable = -1;
            public boolean hasNext() {
                return icard <= getNcard();
            }
            public Object next() {
                if ( ! hasNext() ) {
                    throw new NoSuchElementException();
                }
                else {
                    int ic = getCard();
                    setCard( icard );
                    removable = icard++;
                    String line = findFits( "%f", false );
                    setCard( ic );
                    return line;
                }
            }
            public void remove() {
                if ( removable > 0 ) {
                    int ic = getCard();
                    setCard( removable );
                    delFits();
                    icard--;
                    if ( ic > removable ) { 
                        ic--;
                    }
                    setCard( ic );
                    removable = 0;
                }
                else {
                    throw new IllegalStateException();
                }
            }
        };
    }

    /**
     * Disposes of a line of output.  This method is invoked repeatedly 
     * when the <code>finalize</code> method of this FitsChan is called 
     * (either explicitly or under control of the garbage collector)
     * to dispose of each FITS header card currently in the channel.
     *
     * The <code>FitsChan</code> implementation simply discards each
     * line, but it may be overridden by subclasses to output the header
     * cards in a useful way.  The method may throw an IOException in case
     * of error.
     *
     * @param  line  an 80-character string giving the contents of one
     *               FITS header card.  This method discards it.
     * @throws IOException  if a write error is encountered
     */
    protected void sink( String line ) throws IOException {
    }

    /**
     * Obtains a line of input.  This method is invoked repeatedly during 
     * construction of this FitsChan to obtain its initial contents.
     * On each call it returns the text of the next FITS header card
     * if there is one, and <code>null</code> if there are no more.
     *
     * The <code>FitsChan</code> implementation uses the Iterator 
     * supplied in the constructor to obtain the lines of text.
     * It is declared final to prevent subclasses overriding it -
     * algthough in principle it could work, this practice would be
     * fraught with difficulty since the method is called by the 
     * FitsChan constructor before the subclassed object has been 
     * properly constucted, so it is likely to be in an inconsistent state.
     */
    final protected String source() {
        return cardIt.hasNext() ? (String) cardIt.next() : null;
    }

    /**
     * Finalizes the object.  When this method is called, either explicitly
     * or under control of the garbage collector, the <code>sink</code>
     * method will be invoked to write out any content of this 
     * <code>FitsChan</code>
     */
    public void finalize() throws Throwable {
        destroy();
        super.finalize();
    }
    private native void destroy();

    /**
     * Reads an AST object from this FitsChan.
     *
     * @throws  AstException  if an error occurs in the AST library
     */
    public native AstObject read();

    /**
     * Writes an AST object to this channel.  
     *
     * @param    obj  an <code>AstObject</code> to be written
     * @throws   AstException  if an error occurs in the AST library
     */
    public native void write( AstObject obj );

__EOT__

makeNativeMethod(
   name => ( $fName = "delFits" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [],
   return => { type => 'void' },
);

makeNativeMethod(
   name => ( $fName = "findFits" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      { 
         name => ( $aName = "name" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      { 
         name => ( $aName = 'inc' ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { 
      type => 'String',
      descrip => ArgDescrip( $fName, "card" ),
   },
);

makeNativeMethod(
   name => ( $fName = "putFits" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      { 
         name => ( $aName = "card" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "overwrite" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void', },
);

makeNativeMethod(
   name => ( $fName = "putCards" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      {
         name => ( $aName = "cards" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

my( @cardArgs ) = (
   name => ( $aName = "card" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @cardArgs );
makeSetAttrib( @cardArgs );

my( @carLinArgs ) = (
   name => ( $aName = "carLin" ),
   type => "boolean",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @carLinArgs );
makeSetAttrib( @carLinArgs );

my( @CDMatrixArgs ) = (
   name => ( $aName = "CDMatrix" ),
   type => "boolean",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @CDMatrixArgs );
makeSetAttrib( @CDMatrixArgs );

my( @defB1950Args ) = (
   name => ( $aName = "defB1950" ),
   type => "boolean",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @defB1950Args );
makeSetAttrib( @defB1950Args );

my( @encodingArgs ) = (
   name => ( $aName = "encoding" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @encodingArgs );
makeSetAttrib( @encodingArgs );

my( @fitsdigitsArgs ) = (
   name => ( $aName = "fitsDigits" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @fitsdigitsArgs );
makeSetAttrib( @fitsdigitsArgs );

my( @ncardArgs ) = (
   name => ( $aName = "ncard" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @ncardArgs );

my( @warningsArgs ) = (
   name => ( $aName = "warnings" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @warningsArgs );
makeSetAttrib( @warningsArgs );

my( @allwarningsArgs ) = (
   name => ( $aName = "allWarnings" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @allwarningsArgs );

my( @iwcArgs ) = (
   name => ( $aName = "iwc" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @iwcArgs );
makeSetAttrib( @iwcArgs );

print "}\n";

