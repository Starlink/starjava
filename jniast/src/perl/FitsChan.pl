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
      if the <code>close</code> method is explicitly called.
      The <code>close</code> method should be explicitly called to ensure
      cleanup, but the finalizer will call it if it has not previously been
      called at finalization time.
   },
);

print <<'__EOT__';

public class FitsChan extends Channel {

    /* Holds the C pointer to a data structure used by native code. */
    private long chaninfo;

    /* An iterator used by the source method. */
    private Iterator cardIt;

    /**
     * Perform initialization required for JNI code at class load time.
     */
    static {
        nativeInitializeFitsChan();
    }
    private native static void nativeInitializeFitsChan();

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
     * when the <code>close</code> method of this FitsChan is called 
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

    protected void finalize() throws Throwable {
        try {
            close();
        }
        finally {
            super.finalize();
        }
    }

    /**
     * Must be called to dispose of the object and ensure that any
     * writes are performed.
     * When this method is called, either explicitly
     * or by the finalizer, the <code>sink</code>
     * method will be invoked to write out any content of this 
     * <code>FitsChan</code>.
     */
    public native void close();

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
     * @return   number of objects written (1 on success)
     * @throws   AstException  if an error occurs in the AST library
     */
    public native int write( AstObject obj );

    /**
     * Stores a double complex value for a named keyword within this
     * FitsChan at the current card position.
     * The supplied keyword can either over-write an existing keyword
     * value, or can be inserted as a new header card.
     *
     * @param  name  FITS keyword name - may be a complete FITS header card,
     *               in which case the keyword is extracted from it.
     *               No more than 80 characters are read
     * @param  rval  real part of the value
     * @param  ival  imaginary part of the value
     * @param  comment  comment associated with the keyword.
     *                  If <tt>null</tt> or a blank string is supplied,
     *                  then any comment present in <tt>name</tt> is used.
     *                  If <code>name</code> contains no comment, then
     *                  any existing coment in the card being overwritten
     *                  is retained.  Otherwise, no comment is stored.
     * @param overwrite If true, the new card overwrites the current card,
     *                  and the <code>Card</code> attribute is incremented.
     *                  If false, then new card is inserted in front of the
     *                  current card, and the current card is left unchanged.
     *                  In either case, if the current card on entry 
     *                  points to end-of-file, the new card is appended
     *                  to the end of the list.
     */
    public native void setFits( String name, double rval, double ival,
                                String comment, boolean overwrite  );

    /**
     * Stores an integer complex value for a named keyword within this
     * FitsChan at the current card position.
     * The supplied keyword can either over-write an existing keyword
     * value, or can be inserted as a new header card.
     *
     * @param  name  FITS keyword name - may be a complete FITS header card,
     *               in which case the keyword is extracted from it.
     *               No more than 80 characters are read
     * @param  rval  real part of the value
     * @param  ival  imaginary part of the value
     * @param  comment  comment associated with the keyword.
     *                  If <tt>null</tt> or a blank string is supplied,
     *                  then any comment present in <tt>name</tt> is used.
     *                  If <code>name</code> contains no comment, then
     *                  any existing coment in the card being overwritten
     *                  is retained.  Otherwise, no comment is stored.
     * @param overwrite If true, the new card overwrites the current card,
     *                  and the <code>Card</code> attribute is incremented.
     *                  If false, then new card is inserted in front of the
     *                  current card, and the current card is left unchanged.
     *                  In either case, if the current card on entry 
     *                  points to end-of-file, the new card is appended
     *                  to the end of the list.
     */
    public native void setFits( String name, int rval, int ival, 
                                String comment, boolean overwrite );

    /**
     * Stores a double value for a named keyword within this
     * FitsChan at the current card position.
     * The supplied keyword can either over-write an existing keyword
     * value, or can be inserted as a new header card.
     *
     * @param  name  FITS keyword name - may be a complete FITS header card,
     *               in which case the keyword is extracted from it.
     *               No more than 80 characters are read
     * @param  value   value
     * @param  comment  comment associated with the keyword.
     *                  If <tt>null</tt> or a blank string is supplied,
     *                  then any comment present in <tt>name</tt> is used.
     *                  If <code>name</code> contains no comment, then
     *                  any existing coment in the card being overwritten
     *                  is retained.  Otherwise, no comment is stored.
     * @param overwrite If true, the new card overwrites the current card,
     *                  and the <code>Card</code> attribute is incremented.
     *                  If false, then new card is inserted in front of the
     *                  current card, and the current card is left unchanged.
     *                  In either case, if the current card on entry 
     *                  points to end-of-file, the new card is appended
     *                  to the end of the list.
     */
    public native void setFits( String name, double value,
                                String comment, boolean overwrite );

    /**
     * Stores an integer value for a named keyword within this
     * FitsChan at the current card position.
     * The supplied keyword can either over-write an existing keyword
     * value, or can be inserted as a new header card.
     *
     * @param  name  FITS keyword name - may be a complete FITS header card,
     *               in which case the keyword is extracted from it.
     *               No more than 80 characters are read
     * @param  value   value
     * @param  comment  comment associated with the keyword.
     *                  If <tt>null</tt> or a blank string is supplied,
     *                  then any comment present in <tt>name</tt> is used.
     *                  If <code>name</code> contains no comment, then
     *                  any existing coment in the card being overwritten
     *                  is retained.  Otherwise, no comment is stored.
     * @param overwrite If true, the new card overwrites the current card,
     *                  and the <code>Card</code> attribute is incremented.
     *                  If false, then new card is inserted in front of the
     *                  current card, and the current card is left unchanged.
     *                  In either case, if the current card on entry 
     *                  points to end-of-file, the new card is appended
     *                  to the end of the list.
     */
    public native void setFits( String name, int value,
                                String comment, boolean overwrite );

    /**
     * Stores a boolean value for a named keyword within this
     * FitsChan at the current card position.
     * The supplied keyword can either over-write an existing keyword
     * value, or can be inserted as a new header card.
     *
     * @param  name  FITS keyword name - may be a complete FITS header card,
     *               in which case the keyword is extracted from it.
     *               No more than 80 characters are read
     * @param  value   value
     * @param  comment  comment associated with the keyword.
     *                  If <tt>null</tt> or a blank string is supplied,
     *                  then any comment present in <tt>name</tt> is used.
     *                  If <code>name</code> contains no comment, then
     *                  any existing coment in the card being overwritten
     *                  is retained.  Otherwise, no comment is stored.
     * @param overwrite If true, the new card overwrites the current card,
     *                  and the <code>Card</code> attribute is incremented.
     *                  If false, then new card is inserted in front of the
     *                  current card, and the current card is left unchanged.
     *                  In either case, if the current card on entry 
     *                  points to end-of-file, the new card is appended
     *                  to the end of the list.
     */
    public native void setFits( String name, boolean value,
                                String comment, boolean overwrite );

    /**
     * Stores a String value for a named keyword within this
     * FitsChan at the current card position.
     * The supplied keyword can either over-write an existing keyword
     * value, or can be inserted as a new header card.
     *
     * @param  name  FITS keyword name - may be a complete FITS header card,
     *               in which case the keyword is extracted from it.
     *               No more than 80 characters are read
     * @param  value   value
     * @param  comment  comment associated with the keyword.
     *                  If <tt>null</tt> or a blank string is supplied,
     *                  then any comment present in <tt>name</tt> is used.
     *                  If <code>name</code> contains no comment, then
     *                  any existing coment in the card being overwritten
     *                  is retained.  Otherwise, no comment is stored.
     * @param overwrite If true, the new card overwrites the current card,
     *                  and the <code>Card</code> attribute is incremented.
     *                  If false, then new card is inserted in front of the
     *                  current card, and the current card is left unchanged.
     *                  In either case, if the current card on entry 
     *                  points to end-of-file, the new card is appended
     *                  to the end of the list.
     */
    public native void setFits( String name, String value,
                                String comment, boolean overwrite );

    /**
     * Stores a CONTINUE type value for a named keyword
     * FitsChan at the current card position.
     * These are treated like string values, but are encoded without
     * an equals sign.
     * The supplied keyword can either over-write an existing keyword
     * value, or can be inserted as a new header card.
     *
     * @param  name  FITS keyword name - may be a complete FITS header card,
     *               in which case the keyword is extracted from it.
     *               No more than 80 characters are read
     * @param  value   value
     * @param  comment  comment associated with the keyword.
     *                  If <tt>null</tt> or a blank string is supplied,
     *                  then any comment present in <tt>name</tt> is used.
     *                  If <code>name</code> contains no comment, then
     *                  any existing coment in the card being overwritten
     *                  is retained.  Otherwise, no comment is stored.
     * @param overwrite If true, the new card overwrites the current card,
     *                  and the <code>Card</code> attribute is incremented.
     *                  If false, then new card is inserted in front of the
     *                  current card, and the current card is left unchanged.
     *                  In either case, if the current card on entry 
     *                  points to end-of-file, the new card is appended
     *                  to the end of the list.
     */
    public native void setFitsContinue( String name, String value,
                                        String comment, boolean overwrite );

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
   name => ( $fName = "testFits" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'boolean', descrip => ReturnDescrip( $fName ) },
   params => [
      {
         name => ( $aName = "name" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
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
   name => ( $fName = "retainFits" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [],
   return => { type => 'void', },
);

makeNativeMethod(
   name => ( $fName = "purgeWCS" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [],
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

