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
   JNIEnv *env;
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
static char *sourceWrap( const char *( *source )() );
static void sinkWrap( void ( *sink )( const char * ), const char *line );


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer;
   ChanInfo *chaninfo;
   AstPointer infopointer;

   /* Ensure that the FitsChan field and method ID values which will
    * be used by the source and sink wrapper routines are initialized. */
   initializeIDs( env );

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

      /* Store the pointer to the AST object in an instance variable. */
   }
   if ( ! (*env)->ExceptionCheck( env ) ) {
      jniastSetPointerField( env, this, pointer );
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_destroy(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer infopointer;
   ChanInfo *chaninfo;
   int refcnt = 0;

   /* If we have already done this, don't do it again - the finalizer may
    * get called explicitly as well as by the garbage collector. */
   ASTCALL(
      refcnt = astGetI( pointer.AstObject, "RefCount" );
   )

   /* Only do what we are about to do if we haven't done it before, and
    * if the reference count of the object is 1, since the events we
    * are anticipating (astDelete being called on the object and 
    * triggering sinkWrap calls) will only happen when there are no
    * more AST references to the object. */
   infopointer.jlong = (*env)->GetLongField( env, this, ChaninfoFieldID );
   if ( infopointer.ptr != NULL && refcnt == 1 ) {

      /* Store necessary pointers in the chaninfo structure, where 
       * sinkWrap() will be able to find it.  SinkWrap is about to get 
       * called during object destruction. */
      fillChaninfo( env, this );

      /* Annul the object.  Since we know that the reference count is unity,
       * this will result in object destruction, and hence invocations
       * of sourceWrap. */
      ASTCALL(
         astAnnul( pointer.AstObject );
      )

      /* Set the pointer field to 0 so that the AstObject finalizer knows
       * not to try to annul it again. */
      pointer.jlong = 0L;
      jniastSetPointerField( env, this, pointer );

      /* Free the chaninfo structure. */
      chaninfo = (ChanInfo *) infopointer.ptr;
      free( chaninfo );
    
      /* Record that destruction has happened. */
      infopointer.ptr = NULL;
      (*env)->SetLongField( env, this, ChaninfoFieldID, infopointer.jlong );
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_delFits(
   JNIEnv *env,          /* Interface pointer */
   jobject this          /* Instance object */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   ASTCALL(
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
      ASTCALL(
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
      ASTCALL(
         astPutFits( pointer.FitsChan, card, overwrite == JNI_TRUE );
      )
      jniastReleaseUTF( env, jCard, card );
   }
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
   ASTCALL(
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

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FitsChan_write(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject item          /* AstObject to write */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer itempointer;
   int needchan;

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
      ASTCALL(
         astWrite( pointer.FitsChan, itempointer.AstObject );
      )

      /* Reverse possible destructive effects of Channelize. */
      if ( needchan && ! (*env)->ExceptionCheck( env ) ) {
         (*env)->CallVoidMethod( env, item, UnChannelizeMethodID );
      }
   }
}
   
   
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

   /* Don't bother if we have done this before. */
   if ( FitsChanClass == NULL && ! (*env)->ExceptionCheck( env ) ) {

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
      chaninfo->env = env;
      chaninfo->object = this;
      chaninfo->sourceMethodID = (*env)->GetMethodID( env, clazz, "source",
                                                      "()Ljava/lang/String;" );
      chaninfo->sinkMethodID = (*env)->GetMethodID( env, clazz, "sink",
                                                    "(Ljava/lang/String;)V" );
   }
}

static char *sourceWrap( const char *(*source)() ) {
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

   retval = NULL;

   /* Get the information from the chaninfo structure. */
   env = chaninfo->env;
   this = chaninfo->object;
   sourceMethodID = chaninfo->sourceMethodID;
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Invoke this FitsChan's source method to get the line as a String. */
      jLine = (jstring) (*env)->CallObjectMethod( env, this, sourceMethodID );

      /* Signal to AST if an exception has occurred. */
      if ( (*env)->ExceptionCheck( env ) ) {
         astSetStatus( SAI__ERROR );
      }

      /* If the source method completed successfully, turn the String into
       * an array of chars and copy it into dynamic memory. */
      else if ( jLine != NULL ) {
         line = jniastGetUTF( env, jLine );
         retval = astMalloc( strlen( line ) + 1 );
         strcpy( retval, line );
         jniastReleaseUTF( env, jLine, line );
      }
   }

   /* Return the successfully or unsuccessfully obtained buffer. */
   return retval;
}

static void sinkWrap( void (*sink)(const char *), const char *line ) {
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

   /* Get required pointers from the chaninfo structure */
   env = chaninfo->env;
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
   if ( (*env)->ExceptionCheck( env ) ) {
      astSetStatus( SAI__ERROR );
   }
}

/* $Id$ */
