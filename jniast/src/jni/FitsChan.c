/*
*+
*  Name:
*     FitsChan.c

*  Purpose:
*     JNI implementations of native methods of FitsChan class.

*  Language:
*     ANSI C.

*  Notes:
*     Although FitsChan is a subclass of Channel, the same named methods
*     don't really do the same things - the source function is invoked
*     by astRead() in a Channel but by the constructor in a FitsChan.
*     The implementation here is therefore to a large extent written 
*     separately from the JNI interface of Channel, although many of
*     the same tricks are used.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     25-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <string.h>
#include "jni.h"
#include "ast.h"
#include "sae_par.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_FitsChan.h"


/* Typedefs. */
typedef struct {
   jobject object; 
   jmethodID sourceMethodID;
   jmethodID sinkMethodID;
} ChanInfo;


/* Static variables. */
static jfieldID ChaninfoFieldID;
static jclass NeedsChannelizingClass;
static jmethodID ChannelizeMethodID;
static jmethodID UnChannelizeMethodID;


/* Static function prototypes. */
static void initializeIDs( JNIEnv *env );
static void fillChaninfo( JNIEnv *env, jobject this );
static char *sourceWrap( const char *(* source)( void ), int * );
static void sinkWrap( void (* sink )( const char * ), const char *, int * );


/* Class methods. */

JNIEXPORT void JNICALL
          Java_uk_ac_starlink_ast_FitsChan_nativeInitializeFitsChan(
   JNIEnv *env,          /* Interface pointer */
   jclass class          /* Class object */
) {
   initializeIDs( env );
}


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;
   ChanInfo *chaninfo;
   AstPointer infopointer;

   /* Allocate space for the chaninfo structure and store it in the 
    * chaninfo instance variable of this object. */
   chaninfo = jniastMalloc( env, sizeof( ChanInfo ) );
   infopointer.ptr = chaninfo;
   if ( ! (*env)->ExceptionCheck( env ) ) {
      (*env)->SetLongField( env, this, ChaninfoFieldID, infopointer.jlong );
   
      /* Store necessary pointers in the chaninfo structure.  These are 
       * required for passing to the source and sink routines; the source 
       * routine is about to be called by the constructor function. */
      fillChaninfo( env, this );

      /* Construct the FitsChan itself, using references to the chaninfo
       * structure.  We are subverting the AST machinery here in order to
       * permit an implementation which will allow multiple simultaneously
       * active channels. */
      ASTCALL(
         pointer.FitsChan =
            astFitsChanFor( (const char *(*)()) chaninfo, sourceWrap,
                            (void (*)( const char * )) chaninfo, sinkWrap, "" );
      )
   }

   /* Store the pointer to the AST object in an instance variable. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      jniastInitObject( env, this, pointer );
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_close(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer infopointer;
   ChanInfo *chaninfo;

   /* Only do this once; if the info pointer is NULL, then close must
    * already have been called. */
   infopointer.jlong = (*env)->GetLongField( env, this, ChaninfoFieldID );
   if ( infopointer.ptr != NULL ) {
      chaninfo = (ChanInfo *) infopointer.ptr;
      (*env)->SetLongField( env, this, ChaninfoFieldID, (jlong) 0 );

      /* Annul the AST object.  This will trigger SinkWrap calls, as long
       * as there are no other AST references to this object.
       * We want to do it here since it needs to get done for correctness,
       * and if we leave it to the finalizer it might not get called
       * promptly, or ever.
       * Not sure what happens if there are other references to this object
       * when this is called.  This code was fixed when JNIAST was
       * essentially unsupported so there may be issues. */
      ASTCALL(
         astAnnul( pointer.AstObject );
      )

      /* Make sure that AstObject knows the annul has taken place, so
       * that it is not attempted again in the finalizer. */
      jniastClearObject( env, this );

      /* Delete the reference to this object in the chaninfo. */
      if ( ! (*env)->ExceptionCheck( env ) ) {
          (*env)->DeleteGlobalRef( env, chaninfo->object );
      }

      /* Free the chaninfo structure. */
      free( chaninfo );
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_delFits(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   THASTCALL( jniastList( 1, pointer.AstObject ),
      astDelFits( pointer.FitsChan );
   )
}

JNIEXPORT jstring JNICALL Java_uk_ac_starlink_ast_FitsChan_findFits(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName,        /* Card name */
   jboolean inc          /* Increment? */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *name;
   char card[ 81 ];
   int success;
   jstring jCard = NULL;

   if ( jniastCheckNotNull( env, jName ) ) {
      name = jniastGetUTF( env, jName );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         success = astFindFits( pointer.FitsChan, name, card, 
                                inc == JNI_TRUE );
      )
      jniastReleaseUTF( env, jName, name );
      if ( success ) {
         card[ 80 ] = '\0';
         jCard = (*env)->NewStringUTF( env, (const char *) card );
      }
      else {
         jCard = NULL;
      }
   }
   return jCard;
}

JNIEXPORT jboolean JNICALL Java_uk_ac_starlink_ast_FitsChan_testFits(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jName         /* Card name */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *name;
   int result;

   if ( jniastCheckNotNull( env, jName ) ) {
      name = jniastGetUTF( env, jName );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         result = astTestFits( pointer.FitsChan, name, NULL );
      )
      jniastReleaseUTF( env, jName, name );
   }
   return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_putFits(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jCard,        /* Card text */
   jboolean overwrite    /* Overwrite card? */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *card;

   if ( jniastCheckNotNull( env, jCard ) ) {
      card = jniastGetUTF( env, jCard );
      THASTCALL( jniastList( 1, pointer.AstObject ),
         astPutFits( pointer.FitsChan, card, overwrite == JNI_TRUE );
      )
      jniastReleaseUTF( env, jCard, card );
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_retainFits(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   THASTCALL( jniastList( 1, pointer.AstObject ),
      astRetainFits( pointer.FitsChan );
   )
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_purgeWCS(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   THASTCALL( jniastList( 1, pointer.AstObject ),
      astPurgeWCS( pointer.FitsChan );
   )
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_FitsChan_read(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer newpointer;
   jobject newobj = NULL;
   int needchan;

   /* Call the AST function to do the work. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      newpointer.AstObject = astRead( pointer.FitsChan );
   )

   /* Make a java object out of the AST object. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      newobj = jniastMakeObject( env, newpointer.AstObject );
   }

   /* Do any further necessary object-specific initialization. */
   if ( newobj ) {
      needchan = ( (*env)->IsInstanceOf( env, newobj, NeedsChannelizingClass )
                   == JNI_TRUE );
      if ( needchan ) {
         (*env)->CallVoidMethod( env, newobj, UnChannelizeMethodID );
      }
   }
   return newobj;
}

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_FitsChan_write(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject item          /* AstObject to write */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer itempointer;
   int needchan;
   int nwrite = 0;

   if ( jniastCheckNotNull( env, item ) ) {

      /* Get the AST object. */
      itempointer = jniastGetPointerField( env, item );

      /* See if the object requires preparation for writing to the channel. */
      needchan = ( (*env)->IsInstanceOf( env, item, NeedsChannelizingClass )
                   == JNI_TRUE );

      /* Do anything necessary to the object before it is written. */
      if ( needchan && ! (*env)->ExceptionCheck( env ) ) {
         (*env)->CallVoidMethod( env, item, ChannelizeMethodID );
      }

      /* Call the AST routine to do the work. */
      THASTCALL( jniastList( 2, pointer.AstObject, itempointer.AstObject ),
         nwrite = astWrite( pointer.FitsChan, itempointer.AstObject );
      )

      /* Reverse possible destructive effects of Channelize. */
      if ( needchan && ! (*env)->ExceptionCheck( env ) ) {
         (*env)->CallVoidMethod( env, item, UnChannelizeMethodID );
      }

      /* Return number of objects written. */
      return (jint) nwrite;
   }
   return 0;
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_putCards(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jstring jCards        /* Single string of header image */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   const char *cards;

   if ( jniastCheckNotNull( env, jCards ) ) {
       cards = jniastGetUTF( env, jCards );
       THASTCALL( jniastList( 1, pointer.AstObject ),
          astPutCards( pointer.FitsChan, cards );
       )
       jniastReleaseUTF( env, jCards, cards );
   }
}


#define MAKE_SETFITS_REAL(Xtype,Xjtype,Xappend,Xjtypesig) \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_setFits__Ljava_lang_String_2##Xjtypesig##Ljava_lang_String_2Z( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jName,        /* FITS header name */ \
   Xjtype value,         /* Value for card */ \
   jstring jComment,     /* Comment for card */ \
   jboolean overwrite    /* Overwrite flag */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *name; \
   const char *comment; \
 \
   if ( jniastCheckNotNull( env, jName ) ) { \
      name = jniastGetUTF( env, jName ); \
      comment = jComment ? jniastGetUTF( env, jComment ) : NULL; \
 \
      THASTCALL( jniastList( 1, pointer.AstObject ), \
         astSetFits##Xappend( pointer.FitsChan, name, value, \
                              comment, overwrite == JNI_TRUE ); \
      ) \
 \
      jniastReleaseUTF( env, jName, name ); \
      jniastReleaseUTF( env, jComment, comment ); \
   } \
}
MAKE_SETFITS_REAL(double,jdouble,F,D)
MAKE_SETFITS_REAL(int,jint,I,I)
MAKE_SETFITS_REAL(int,jboolean,L,Z)
#undef MAKE_SETFITS_REAL

#define MAKE_SETFITS_COMPLEX(Xtype,Xjtype,Xappend,Xjtypesig) \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_setFits__Ljava_lang_String_2##Xjtypesig##Xjtypesig##Ljava_lang_String_2Z( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jName,        /* FITS header name */ \
   Xjtype rval,          /* Real part of value */ \
   Xjtype ival,          /* Imaginary part of value */ \
   jstring jComment,     /* Comment for card */ \
   jboolean overwrite    /* Overwrite flag */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *name; \
   const char *comment; \
   Xtype value[ 2 ] = { (Xtype) rval, (Xtype) ival }; \
 \
   if ( jniastCheckNotNull( env, jName ) ) { \
      name = jniastGetUTF( env, jName ); \
      comment = jComment ? jniastGetUTF( env, jComment ) : NULL; \
 \
      THASTCALL( jniastList( 1, pointer.AstObject ), \
         astSetFits##Xappend( pointer.FitsChan, name, value, \
                              comment, overwrite == JNI_TRUE ); \
      ) \
 \
      jniastReleaseUTF( env, jName, name ); \
      jniastReleaseUTF( env, jComment, comment ); \
   } \
}
MAKE_SETFITS_COMPLEX(double,jdouble,CF,D)
MAKE_SETFITS_COMPLEX(int,jint,CI,I)
#undef MAKE_SETFITS_COMPLEX

#define MAKE_SETFITS_STRING(Xappend,Xjappend) \
JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_setFits##Xjappend( \
   JNIEnv *env,          /* Interface pointer */ \
   jobject this,         /* Instance object */ \
   jstring jName,        /* FITS header name */ \
   jstring jValue,       /* Value for card */ \
   jstring jComment,     /* Comment for card */ \
   jboolean overwrite    /* Overwrite flag */ \
) { \
   AstPointer pointer = jniastGetPointerField( env, this ); \
   const char *name; \
   const char *value; \
   const char *comment; \
 \
   if ( jniastCheckNotNull( env, jName ) && \
        jniastCheckNotNull( env, jValue ) ) { \
      name = jniastGetUTF( env, jName ); \
      value = jniastGetUTF( env, jValue ); \
      comment = jComment ? jniastGetUTF( env, jComment ) : NULL; \
 \
      THASTCALL( jniastList( 1, pointer.AstObject ), \
         astSetFits##Xappend( pointer.FitsChan, name, value, \
                              comment, overwrite == JNI_TRUE ); \
      ) \
 \
      jniastReleaseUTF( env, jName, name ); \
      jniastReleaseUTF( env, jValue, value ); \
      jniastReleaseUTF( env, jComment, comment ); \
   } \
}
MAKE_SETFITS_STRING(S,__Ljava_lang_String_2Ljava_lang_String_2Ljava_lang_String_2Z)
MAKE_SETFITS_STRING(CN,Continue)
#undef MAKE_SETFITS_STRING


/* Static functions. */

static void initializeIDs( JNIEnv *env ) {
/*
*+
*  Name:
*     intializeIDs

*  Purpose:
*     Initialize static field and method ID variables specific to FitsChan.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment.
*-
*/
   static jclass FitsChanClass = NULL;

   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Get global references to classes. */
      ( FitsChanClass = (jclass) (*env)->NewGlobalRef( env,
             (*env)->FindClass( env, PACKAGE_PATH "FitsChan" ) ) ) &&
      ( NeedsChannelizingClass = (jclass) (*env)->NewGlobalRef( env,
             (*env)->FindClass( env, PACKAGE_PATH "NeedsChannelizing" ) ) ) &&

      /* Get field IDs. */
      ( ChaninfoFieldID = (*env)->GetFieldID( env, FitsChanClass, 
                                              "chaninfo", "J" ) ) &&

      /* Get method IDs. */
      ( ChannelizeMethodID = (*env)->GetMethodID( env, NeedsChannelizingClass,
                                                  "channelize", "()V" ) ) &&
      ( UnChannelizeMethodID = (*env)->GetMethodID( env, NeedsChannelizingClass,
                                                    "unChannelize", "()V" ) ) &&
      1;
   }
}

static void fillChaninfo( JNIEnv *env, jobject this ) {
/*
*+
*  Name:
*     fillChaninfo

*  Purpose:
*     Store necessary pointers in the chaninfo structure.

*  Description:
*     This function ensures that the chaninfo structure, which can be
*     located from within the sourceWrap and sinkWrap functions, contains
*     the pointers necessary to invoke the object's source and sink methods.
*     It should be called before AST is going to do something which
*     will may result in sourceWrap or sinkWrap being called, i.e. 
*     FitsChan construction or destruction.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment
*     this = jobject
*        The FitsChan object whose source/sink methods may be invoked.
*-
*/
   ChanInfo *chaninfo;
   AstPointer infopointer;
   jclass clazz;

   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Get the class of this object. */
      clazz = (*env)->GetObjectClass( env, this );

      /* Get the location of the chaninfo structure. */
      infopointer.jlong = (*env)->GetLongField( env, this, ChaninfoFieldID );
      chaninfo = (ChanInfo *) infopointer.ptr;

      /* Fill the chaninfo structure with the required pointers. */
      /* The Global Reference here is required as things are currently 
       * done to make sure that the reference can be used from later
       * methods.  However, I believe it constitutes a memory leak;
       * the presence of this reference prevents the FitsChan from ever
       * being garbage collected. */
      chaninfo->object = (*env)->NewGlobalRef( env, this );
      chaninfo->sourceMethodID = (*env)->GetMethodID( env, clazz, "source",
                                                      "()Ljava/lang/String;" );
      chaninfo->sinkMethodID = (*env)->GetMethodID( env, clazz, "sink",
                                                    "(Ljava/lang/String;)V" );
   }
}

static char *sourceWrap( const char *(* source)( void ), int *status ) {
/*
*+
*  Name:
*     sourceWrap

*  Purpose:
*     Invoke the FitsChan's source() method and pass the result to AST.

*  Description:
*     This function is called by the AST library from within the 
*     FitsChan constructor.  It has to call the source() method of 
*     the corresponding java FitsChan object, and return the resulting
*     string.

*  Parameters:
*     source = const char *(*)()
*        A pointer to the ChanInfo structure which contains information
*        about the environment and object for which this function is
*        called.  It has to be cast to the weird form above to fool AST.
*     status 
*        Pointer to the inherited status variable.

*  Return value:
*     A new dynamically allocated (using astMalloc()) buffer holding
*     a char* representation of the String returned by the FitsChan's
*     source() method.

*  Notes:
*     There are a couple of subtleties here.  First, astRead is contracted
*     to call sourceWrap() without any useful extra information, but within
*     JNI it is impossible to do anything without having the env and
*     this pointers.  We therefore subvert the AST foreign calling
*     conventions, sneaking a pointer into the space where a reference
*     to the source() function itself (unused in the C implementation)
*     is supposed to go.  This allows us to access env and this from here.
*
*     Secondly, the sourceWrap method is contracted to return the
*     value in dynamically allocated memory, allocated using AST's
*     memory allocation functions, so we ensure that this is done.
*-
*/
   ChanInfo *chaninfo = (ChanInfo *) source;
   jstring jLine;
   const char *line;
   char *retval;
   JNIEnv *env;
   jobject this;
   jmethodID sourceMethodID;

   /* Return directly if AST status is set. */
   if ( !astOK ) return NULL;

   /* Initialise return value. */
   retval = NULL;

   /* Get the information from the chaninfo structure. */
   env = jniastGetEnv();
   this = chaninfo->object;
   sourceMethodID = chaninfo->sourceMethodID;
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Invoke this FitsChan's source method to get the line as a String. */
      jLine = (jstring) (*env)->CallObjectMethod( env, this, sourceMethodID );

      /* If the source method completed successfully, turn the String into
       * an array of chars and copy it into dynamic memory. */
      if ( ! (*env)->ExceptionCheck( env ) && jLine != NULL ) {
         line = jniastGetUTF( env, jLine );
         retval = astMalloc( strlen( line ) + 1 );
         if ( retval != NULL ) {
             strcpy( retval, line );
         }
         jniastReleaseUTF( env, jLine, line );
      }
   }

   /* Return the successfully or unsuccessfully obtained buffer. */
   return retval;
}

static void sinkWrap( void (* sink)( const char * ), const char *line, 
                      int *status ) {
/*
*+
*  Name:
*    sinkWrap

*  Purpose:
*     Invoke the FitsChan's sink(String) method on behalf of AST.

*  Description:
*     This function is called by the AST library from within the 
*     FitsChan destructor.  It has to call the sink(String) method 
*     of the corresponding java FitsChan object.

*  Parameters:
*     sink = void (*)( const char * )
*        A pointer to the ChanInfo structure which contains information
*        about the environment and object for which this function is
*        called.  It has to be cast to the weird form above to fool AST.
*     status
*        Pointer to inherited status variable.

*  Notes:
*     There is a subtlety here.  astWrite is contracted to call sinkWrap()
*     without any useful extra information, but within JNI it is
*     impossible to do anything without having the env and this pointers.
*     We therefore subvert the AST foreign calling conventions, sneaking
*     a pointer into the space where a reference to the sink() function
*     itself (unused in the C implementation) is supposed to go.
*     This allows us to access env and this from here.
*-
*/
   ChanInfo *chaninfo = (ChanInfo *) sink;
   JNIEnv *env;
   jobject this;
   jmethodID sinkMethodID;
   jstring jLine;

   /* Return directly if ast status is set. */
   if ( !astOK ) return;

   /* Get required pointers from the chaninfo structure */
   env = jniastGetEnv();
   this = chaninfo->object;
   sinkMethodID = chaninfo->sinkMethodID;
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Make a java string from the supplied argument. */
      jLine = (*env)->NewStringUTF( env, line );

      /* Pass it to the sink method of this object. */
      if ( jLine != NULL ) {
         if ( ! (*env)->ExceptionCheck( env ) ) {
            (*env)->CallVoidMethod( env, this, sinkMethodID, jLine );
         }
      }
   }
}

/* $Id$ */
