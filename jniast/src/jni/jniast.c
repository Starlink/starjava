/*
*+
*  Name:
*     jniast.c

*  Purpose:
*     Common functions for JNI code used to harness the AST library.

*  Description:
*     These functions may be used to do useful things by modules 
*     implementing AST functionality using JNI.  All are safe to 
*     call, and where applicable return a null result, if an
*     exception is pending.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     18-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdio.h>
#include <string.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#if HAVE_PTHREADS
#include <pthread.h>
#else
#include "bdpthread.h"
#endif


/* Static variables. */
static jclass IntraMapClass = NULL;
static jclass NoClassDefFoundErrorClass;
static jclass NullPointerExceptionClass;
static jclass IllegalArgumentExceptionClass;
static jclass RuntimeExceptionClass;
static jclass OutOfMemoryErrorClass;
static jclass SystemClass;
static jfieldID AstPointerID;
static jfieldID IntraMapIntraFlagFieldID;
static jmethodID AstObjectConstructorID;
static jmethodID IllegalArgumentExceptionConstructorID;
static jmethodID SystemGcMethodID;

/* The JavaVM for this JVM. Global reference. */
static JavaVM *java_vm;


/* Static functions. */
static int ptrCmp( const void *, const void * );
static void throwTypedThrowable( JNIEnv *env, jclass throwclass, 
                                 const char *fmt, va_list ap );

/* Utility functions. */

void jniastInitialize( JNIEnv *env ) {
/*
*+
*  Name:
*     jniastInitialize

*  Purpose:
*     Perform static inialization for JNI/AST native code.

*  Description:
*     This routine should be called sometime after the dynamic library
*     containg JNI/AST code is loaded, and sometime before the
*     other native functions are used.  It generates global references
*     to classes and objects and field and method IDs which are used
*     by the native code.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*/
   jclass objclass;
   jclass classclass;


   ( ! (*env)->ExceptionCheck( env ) ) &&

   ( ! (*env)->GetJavaVM( env, &java_vm ) ) &&

   /* Get static references to classes.
    * Note that we retain a permannent global reference to each class;
    * this ensures that the class does not become unloaded in which
    * case the field and method ID values could become out of date. */
   ( DoubleArrayClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "[D" ) ) ) &&
   ( StringClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/String" ) ) ) &&
   ( SystemClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/System" ) ) ) &&
   ( NullPointerExceptionClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/NullPointerException" ) ) ) &&
   ( RuntimeExceptionClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/RuntimeException" ) ) ) &&
   ( IllegalArgumentExceptionClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/IllegalArgumentException" ) ) ) &&
   ( ErrorClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/Error" ) ) ) &&
   ( NoClassDefFoundErrorClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/NoClassDefFoundError" ) ) ) &&
   ( OutOfMemoryErrorClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/OutOfMemoryError" ) ) ) &&
   ( UnsupportedOperationExceptionClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, 
                           "java/lang/UnsupportedOperationException" ) ) ) &&
   ( DoubleClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/Double" ) ) ) &&
   ( IntegerClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, "java/lang/Integer" ) ) ) &&
   ( objclass = (*env)->FindClass( env, "java/lang/Object" ) ) &&
   ( classclass = (*env)->FindClass( env, "java/lang/Class" ) ) &&

   /* Note that some of the initializers of classes from this package
    * use classes defined above, so careful about the order in which
    * this initialization is done. */
   ( AstObjectClass = (jclass) (*env)->NewGlobalRef( env, 
        (*env)->FindClass( env, PACKAGE_PATH "AstObject" ) ) ) &&
   ( MappingClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, PACKAGE_PATH "Mapping" ) ) ) &&
   ( AstExceptionClass = (jclass) (*env)->NewGlobalRef( env,
        (*env)->FindClass( env, PACKAGE_PATH "AstException" ) ) ) &&

   /* Get field IDs. */
   ( AstPointerID = 
        (*env)->GetFieldID( env, AstObjectClass, "pointer", "J" ) ) &&

   /* Get method IDs. */
   ( ObjectHashCodeMethodID =
        (*env)->GetMethodID( env, objclass, "hashCode", 
                             "()I" ) ) &&
   ( ObjectToStringMethodID =
        (*env)->GetMethodID( env, objclass, "toString",
                             "()Ljava/lang/String;" ) ) && 
   ( ClassGetNameMethodID =
        (*env)->GetMethodID( env, classclass, "getName",
                             "()Ljava/lang/String;" ) ) &&
   ( IllegalArgumentExceptionConstructorID =
        (*env)->GetMethodID( env, IllegalArgumentExceptionClass, "<init>", 
                             "(Ljava/lang/String;)V" ) ) &&
   ( ErrorConstructorID =
        (*env)->GetMethodID( env, ErrorClass, "<init>",
                             "(Ljava/lang/String;)V" ) ) &&
   ( AstObjectConstructorID =
        (*env)->GetMethodID( env, AstObjectClass, "<init>", 
                             "()V" ) ) &&
   ( AstExceptionConstructorID =
        (*env)->GetMethodID( env, AstExceptionClass, "<init>", 
                             "(Ljava/lang/String;I)V" ) ) &&
   ( SystemGcMethodID =
        (*env)->GetStaticMethodID( env, SystemClass, "gc", 
                                   "()V" ) ) &&
   ( DoubleConstructorID =
        (*env)->GetMethodID( env, DoubleClass, "<init>",
                             "(D)V" ) ) &&
   ( IntegerConstructorID =
        (*env)->GetMethodID( env, IntegerClass, "<init>",
                             "(I)V" ) ) &&

   /* Construct the object used for synchronizing calls to the AST library. */
   ( AstLock = (jobject) (*env)->NewGlobalRef( env,
        (*env)->NewObject( env, objclass,
                           (*env)->GetMethodID( env, objclass, "<init>", 
                                                "()V" ) ) ) ) &&

   /* Construct the object used for synchronizing GRF calls. */
   ( GrfLock = (jobject) (*env)->NewGlobalRef( env,
        (*env)->NewObject( env, objclass,
                           (*env)->GetMethodID( env, objclass, "<init>", 
                                                "()V" ) ) ) ) &&

   /* Initialisations for other components. */
   ( ! jniastErrInit( env ) ) &&

   1;
}

void *jniastMalloc( JNIEnv *env, size_t size ) {
/*
*+
*  Name:
*     jniastMalloc

*  Purpose:
*     Allocates memory in a JNI context.

*  Description:
*     This function allocates memory in the same way as malloc(3), but
*     in a JNI-sensitive way.  It will not attempt the call if 
*     an exception is pending on entry.  If the call of the underlying
*     malloc fails, the System.gc() method is called, and the 
*     allocation is attempted again.  If it still fails, then NULL
*     is returned and an OutOfMemoryError is thrown.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI environment.
*     size = size_t
*        The number of bytes to allocate.

*  Return value:
*     A pointer to the allocated memory if the call was successful, or
*     NULL if it failed.  Iff the return value is NULL, a (new or old)
*     exception will be pending on exit.
*-
*/
   void *ptr = NULL;

   /* Only proceed if there is no pending exception. */
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Attempt the requested allocation and check if it worked. */
      ptr = malloc( size );
      if ( ptr == NULL ) {

         /* The allocation failed.  Call the garbage collector and try again. */
         (*env)->CallStaticVoidMethod( env, SystemClass, SystemGcMethodID );
         ptr = malloc( size );
         if ( ptr == NULL ) {

            /* We still cannot allocate the requested memory.  Throw an
             * OutOfMemoryError. */
            (*env)->ThrowNew( env, OutOfMemoryErrorClass,
                              "Out of memory during jniast native code" );
         }
      }
   }

   /* Return the successfully or unsuccessfully allocated address. */
   return ptr;
}

AstPointer jniastGetPointerField( JNIEnv *env, jobject object ) {
/*+
*  Name:
*     jniastGetPointerField

*  Purpose:
*     Get the pointer field of the given AstObject.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     object = jobject
*        Reference to the AstObject whose pointer field is required.

*  Return value:
*     An AstPointer union representing the private pointer field of object.
*-
*/
   AstPointer pointer;
   pointer.ptr = NULL;
   if ( ! (*env)->ExceptionCheck( env ) ) {
      pointer.jlong = (*env)->GetLongField( env, object, AstPointerID );
   }
   return pointer;
}

void jniastInitObject( JNIEnv *env, jobject object, AstPointer pointer ) {
/*
*+
*  Name:
*     jniastInitObject

*  Purpose:
*     Prepares a Java AstObject for use.

*  Description:
*     This method sets the pointer field of a Java AstObject to the 
*     given value.  It also releases the AST thread lock.
*
*     If the previous value referenced a valid AST object, that object
*     is annulled, so that it does not constitute a resource leak.
*     Any such previous should not therefore be in use elsewhere.
*     This will be the case if the pointer change is part of a 
*     sequence of resets for subclass constructors - the only context
*     in which pointer fields are expected to be reset.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface
*     object = jobject
*        Reference to the AstObject whose pointer field is to be set.
*        The object may be locked or unlocked on entry, and will be
*        unlocked on exit.
*     pointer = AstPointer
*        An AstPointer union giving the value of the private pointer 
*        field of object.
*-
*/
   AstPointer old;

   /* Annul the old pointer field value if any. */
   if ( ! (*env)->ExceptionCheck( env ) ) {
      old = jniastGetPointerField( env, object );
      if ( old.ptr != NULL ) {
         ASTCALL(
            astAnnul( old.AstObject );
         )

         /* This shouldn't cause a problem, but warn, rather than throw,
          * if it does (unless the new pointer is NULL, in which case
          * the caller is presumably aware of what's going on). */
         if ( (*env)->ExceptionCheck( env ) ) {
            (*env)->ExceptionClear( env );
            if ( pointer.ptr != NULL ) {
               printf( "jniast warning: trouble annulling pointer %p\n",
                       old.ptr );
            }
         }
      }
   }
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Set the new pointer field value. */
      (*env)->SetLongField( env, object, AstPointerID, pointer.jlong );

      /* Unlock ready for use by other threads. */
      ASTCALL(
         astUnlock( pointer.AstObject, 0 );
      )
   }
}

void jniastClearObject( JNIEnv *env, jobject object ) {
/*
*+
*  Name:
*     jniastClearObject

*  Purpose:
*     Clears a Java AstObject. 

*  Description:
*     Clears the AstPointer field of an AstObject.  It cannot be safely
*     used after this call.  Normally it is not necessary to make this
*     call, since this operation is performed in the AstObject finalizer,
*     but if the annul is performed manually for some reason it may
*     be necessary.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface
*     object = jobject
*        Reference to the AstObject whose pointer field is to be set.
*-
*/
   if ( ! (*env)->ExceptionCheck( env ) ) {
      (*env)->SetLongField( env, object, AstPointerID, (jlong) 0 );
   }
}

jobject jniastMakeObject( JNIEnv *env, AstObject *objptr ) {
/*+
*  Name:
*     jniastMakeObject

*  Purpose:
*     Construct a java object from a C pointer to an AST object.

*  Description:
*     Given a C pointer to a struct representing an object from the
*     AST C library, this routine constructs and returns a Java
*     object of a corresponding type.  It also releases the AST thread
*     lock.  If a java class corresponding to the actual type of the 
*     AST object cannot be found, an exception is thrown.  If the 
*     pointer passed in is NULL, a null return will be made.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     objptr = AstObject *
*        Pointer to the C struct containing the AST object.
*        The object may be locked or unlocked on entry, and will be
*        unlocked on exit.

*  Return value:
*     A new AstObject of the most specific class known which is consistent
*     with objptr;
*-
*/
#define BUFLENG (64)
   jobject newobj;
   const char *classname;
   char classbuf[ BUFLENG ];
   AstPointer pointer;
   jclass class;
   jclass intramapClass;
   jmethodID initMethodID;
   jthrowable thrown;
   int pleng;

   newobj = NULL;
   if ( objptr == NULL ) {
       return NULL;
   }
   THASTCALL( jniastList( 1, objptr ),
      classname = astGetC( objptr, "class" );
   )
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Get the java class corresponding to the class of this AST object. */
      strcpy( classbuf, PACKAGE_PATH );
      pleng = strlen( classbuf );
      strncpy( classbuf + pleng, classname, BUFLENG - pleng - 1 );
      classbuf[ BUFLENG - 1 ] = '\0';
      class = (*env)->FindClass( env, classbuf );

      /* If we failed to find it, try the AST class name with 'Ast' prepended -
       * some classes are named like this to prevent clashes with other
       * common java classes. */
      if ( class == NULL ) {
         thrown = (*env)->ExceptionOccurred( env );
         if ( (*env)->IsInstanceOf( env, thrown, 
                                    NoClassDefFoundErrorClass ) ) {
             (*env)->ExceptionClear( env );
             strcpy( classbuf, PACKAGE_PATH );
             pleng = strlen( classbuf );
             strncpy( classbuf + pleng, "Ast", BUFLENG - pleng - 1 );
             pleng = strlen( classbuf );
             strncpy( classbuf + pleng, classname, BUFLENG - pleng - 1 );
             classbuf[ BUFLENG - 1 ] = '\0';
             class = (*env)->FindClass( env, classbuf );
         }
      }

      /* If we still failed to find it, bail out (an exception will be 
       * pending) */
      if ( class != NULL ) {

         /* Allocate a new java object of this class. */
         newobj = (*env)->AllocObject( env, class );

         /* Set the pointer instance variable to hold the address of the
          * AST struct. */
         pointer.AstObject = objptr;
         jniastInitObject( env, newobj, pointer );
      }
   }
   return newobj;
#undef BUFLENG
}

void jniastLock( AstObject **ast_objs ) {
/*+
*  Name:
*     jniastLock

*  Purpose:
*     Acquire AST thread locks on a list of AstObjects.

*  Description:
*     Acquires AST thread locks on each of a given list of AST objects.
*     The locks are obtained in some determinate order.  This resource
*     ordering is important to avoid deadlock in which different threads
*     require the same resources.

*  Arguments:
*     ast_objs = AstObject **
*        Pointer to a NULL-terminated list of AstObject pointers for which
*        locks are required.  This list is sorted in place before the 
*        locks are obtained.  As a special case, if NULL is supplied,
*        no action will be taken.
*-
*/
   AstObject **pos;
   int count = 0;
   if ( ast_objs ) {
      for ( pos = ast_objs; *pos; pos++ ) {
         count++;
      }
      qsort( ast_objs, count, sizeof( AstObject * ), ptrCmp );
      for ( pos = ast_objs; *pos; pos++ ) {
         astLock( *pos, 1 );
      }
   }
}

void jniastUnlock( AstObject **ast_objs ) {
/*+
*  Name:
*     jniastUnlock

*  Purpose:
*     Relinquish AST thread locks on a list of AstObjects.

*  Description:
*     Relinquishes AST thread locks on each of a given list of AST objects.
*     The locks are relinquished in the opposite order from that in which
*     they appear in the list (I'm not sure if unlocking in order is critical
*     for deadlock avoidance?).

*  Arguments:
*     ast_objs = AstObject **
*        Pointer to NULL-terminated ordered list of AstObject pointers 
*        which should be unlocked.  As a special case, if NULL is supplied,
*        no action is taken.
*-
*/
   AstObject **pos;
   if ( ast_objs ) {
      for ( pos = ast_objs; *pos; pos++ ) {
      }
      while ( --pos >= ast_objs ) {
         astUnlock( *pos, 0 );
      }
   }
}

AstObject **jniastList( int count, ... ) {
/*+
*  Name:
*     jniastList

*  Purpose:
*     Construct array in memory of pointer objects.

*  Description:
*     Given a varargs list and its size, this function stores the supplied
*     pointer arguments in a newly malloc'd array in memory, followed
*     by a terminating NULL.  NULL values are permitted in the input list;
*     these will effectively be absent from the returned array.

*  Arguments:
*     count = int
*        The number of arguments that follow.
*     ... = AstObject *,
*        Zero or more pointers to AstObjects.  NULL pointers are permitted.

*  Return value:
*     A pointer to a newly-allocated AstObject ** array containing an
*     entry for each of the supplied pointers, followed by a terminating
*     NULL.  It is the responsibility of the caller to free() this
*     memory at a later date.
*     If count is zero, then NULL may be returned instead.
*-
*/
   va_list ap;
   int i;
   AstObject **ast_objs;
   AstObject **pos;
   AstObject *aobj;
   if ( count > 0 ) {
      ast_objs = (AstObject **) malloc( ( count + 1 ) * sizeof( AstObject * ) );
      if ( ast_objs ) {
         pos = ast_objs;
         va_start( ap, count );
         for ( i = 0; i < count; i++ ) {
            aobj = va_arg( ap, AstObject * );
            if ( aobj ) {
               *(pos++) = aobj;
            }
         }
         va_end( ap );
         *pos = NULL;
      }
      else {
         astSetStatus( AST__NOMEM );
      }
      return ast_objs;
   }
   else {
      return NULL;
   }
}

int jniastPthreadKeyCreate( JNIEnv *env, pthread_key_t *key,
                            void (*destructor)(void *) ) {
/*+
*  Name:
*     jniastPthreadKeyCreate

*  Purpose:
*     Wraps pthread_key_create() providing JNI error handling.

*  Description:
*     Invokes pthread_key_create(key,destructor), and in case of error
*     throws an exception via the supplied JNI interface.
*     See pthread_key_create(3) for more details.

*  Parameters:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     key = pthread_key_t *
*        Address into which the new key will be written.
*     destructor = void (*)( void * )
*        Will be called on the key's thread-specific value at thread
*        destruction time.

*  Return value:
*     Zero for success, non-zero for failure.
*-
*/
   int status;
   status = pthread_key_create( key, destructor );
   if ( status ) {
      jniastThrowError( env, "pthread initialization error %d", status );
   }
   return status;
}

int jniastPthreadSetSpecific( JNIEnv *env, pthread_key_t key, void *value ) {
/*+
*  Name:
*     jniastPthreadSetSpecific

*  Purpose:
*     Wraps pthread_setspecific() providing JNI error handling.

*  Description:
*     Invokes pthread_set_specific(key,value), and in case of error
*     throws an exception via the supplied JNI interface.
*     See pthread_setspecific(3) for more details.

*  Parameters:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     key = pthread_key_t
*        Key set up by earlier pthread_create_key call.
*     value = void *
*        Value to be bound to key in this thread.

*   Return value:
*      Zero for success, non-zero for failure.
*-
*/
   int status;
   status = pthread_setspecific( key, value );
   if ( status ) {
      jniastThrowError( env, "pthread_setspecific error %d", status );
   }
   return status;
}

int jniastGetNaxes( JNIEnv *env, AstFrame *frame ) {
/*
*+
*  Name:
*     jniastGetNaxes

*  Purpose:
*     Get the number of axes of a Frame.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     frame = AstFrame *
*        The frame in question.

*  Return value:
*     The number of axes of frame.  Zero if an exception is pending.
*-
*/
   int naxes = 0;
   THASTCALL( jniastList( 1, frame ),
      naxes = astGetI( frame, "Naxes" );
   )
   return naxes;
}

int jniastCheckArrayLength( JNIEnv *env, jarray jArray, int minel ) {
/*
*+
*  Name:
*     jniastCheckArrayLength

*  Purpose:
*     Ensures that an array has at least a given length.

*  Description:
*     This routine checks that the array in question has at least a given
*     number of elements.  If the array is a null pointer, then a 
*     NullPointerException is thrown.  If the array has fewer than
*     the requested number of elements, then an IllegalArgumentException
*     is thrown.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     jArray = jarray
*        The java array object.
*     minel = int
*        The minimum number of elements which jArray must have for 
*        successful completion.

*  Return value:
*     On successful return (i.e. the array has >= minel elements) 1
*     is returned.  If the array is null or has too few elements, or
*     if an exception was pending when the function was called (in
*     any of these cases an exception will be pending at the end of
*     this function) then 0 is returned.
*-
*/
   int retval = 0;
   int nel;
   if ( ! (*env)->ExceptionCheck( env ) ) {
      if ( jArray == NULL ) {
         (*env)->ThrowNew( env, NullPointerExceptionClass, 
                           "Supplied array is null" );
      }
      else {
         nel = (*env)->GetArrayLength( env, jArray );
         if ( nel < minel ) {
            jniastThrowIllegalArgumentException( env,
               "Supplied array has only %d elements (needs %d)", nel, minel );
         }
         else {
            retval = 1;
         }
      }
   }
   return retval;
}


jobject jniastCheckNotNull( JNIEnv *env, jobject jObject ) {
/*
*+
*  Name:
*     jniastCheckNotNull

*  Purpose:
*     Ensures that an object is not null.

*  Description:
*     This routine checks that the object in question is not null.
*     If it is, it throws a NullPointerException.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface
*     jObject = jobject
*        The java object.

*  Return value:
*     The object itself is returned, i.e. return will be NULL (=0) if
*     the object is null, or non-NULL (C boolean true) if it is not null.
*-
*/
   if ( ! (*env)->ExceptionCheck( env ) ) {
      if ( jObject == NULL ) {
         (*env)->ThrowNew( env, NullPointerExceptionClass, 
                           "Pointer supplied to jniast native method is null" );
      }
      return jObject;
   }
   else {
      return NULL;
   }
}


/*
*+
*  Name:
*     jniastCopyDoubleArray

*  Purpose:
*     Copy the data from a java double[] array into a fixed buffer.

*  Description:
*     Allocates a fixed sized buffer of doubles, and copies the 
*     contents of a jdouble array into it.  The point of this is that
*     sometimes you need to pass an array to AST without knowing 
*     how long it is; if it's not long enough, AST may try to dereference
*     beyond the end and dump core.  This will only happen if the user
*     supplies a java array which is too short, but nevertheless,
*     we want to defend against a core dump if we possibly can.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     jArr = jdoubleArray
*        Array whose contents are to be copied.  NULL is a legal value;
*        it will be treated the same as a zero-length array.
*     int bufsiz
*        Number of elements in the returned buffer.

*  Return value:
*     A newly-allocated buffer of doubles, of size bufsiz.  Elements
*     beyond the last one contained in the supplied input array will
*     be filled with 0.0.  This must be freed by the caller.
*-
*/
double *jniastCopyDoubleArray( JNIEnv *env, jdoubleArray jArr, int bufsiz ) {
   double *buf;
   jdouble *jbuf;
   jsize nel;
   int i;

   if ( ! (*env)->ExceptionCheck( env ) &&
        jniastCheckSameType( env, jdouble, double ) ) {
      nel = jArr ? (*env)->GetArrayLength( env, jArr )
                 : 0;
      if ( nel > bufsiz ) {
          nel = bufsiz;
      }
      buf = jniastMalloc( env, bufsiz * sizeof( double ) );
      if ( buf ) {
         for ( i = 0; i < bufsiz; i++ ) {
            buf[ i ] = 0.0;
         }
      }
      if ( nel > 0 ) {
         jbuf = jniastMalloc( env, nel * sizeof( jdouble ) );
         if ( jbuf ) {
            (*env)->GetDoubleArrayRegion( env, jArr, 0, nel, jbuf );
            for ( i = 0; i < nel; i++ ) {
               buf[ i ] = (double) jbuf[ i ];
            }
            free( jbuf );
         }
      }
   }
   return buf;
}

char *jniastEscapePercents( JNIEnv *env, const char *buf ) {
/*
*+
*  Name:
*     jniastEscapePercents

*  Purpose:
*     Escape dangerous characters in a printf-style format string.

*  Description:
*     Takes a string and returns a newly-allocated string with the same
*     content but with any '%' character replaced by the sequence "%%".

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     buf = char *
*        String to be escaped.

*  Return value:
*     A string with special characters escaped.  Note this has been 
*     allocated using jniastMalloc; it should be freed by the caller.
*-
*/
   char *buf2;
   const char *p1;
   char *p2;

   /* Allocate a buffer which is big enough (in the worst case, every 
    * character is a percent sign). */
   buf2 = jniastMalloc( env, strlen( buf ) * 2 + 1 );

   /* Transfer each character from the old buffer to the new one, adding an
    * extra percent where necessary. */
   p1 = buf;
   p2 = buf2;
   do {
      if ( *p1 == '%' ) {
         *(p2++) = '%';
      }
      *(p2++) = *(p1++);
   } while ( *(p1 - 1) );

   /* Return the altered buffer. */
   return buf2;
}


void jniastTrace( JNIEnv *env, jobject obj ) {
/*
*+
*  Name:
*     jniastTrace

*  Purpose:
*     Debugging function to trace a java object.

*  Description:
*     Intended for debugging purposes only, this function traces the
*     class name and toString() method of a java object to standard output.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     obj = jobject
*        Object to trace.
*-
*/
   jstring jClassname;
   jstring jObjstring;
   const char *classname;
   const char *objstring;

   jClassname = (*env)->GetObjectClass( env, obj );
   jObjstring = (jstring) (*env)->CallObjectMethod( env, obj, 
                                                    ObjectToStringMethodID );
   classname = jniastGetUTF( env, jClassname );
   objstring = jniastGetUTF( env, jObjstring );
   printf( "%s: %s\n", classname, objstring );
   jniastReleaseUTF( env, jClassname, classname );
   jniastReleaseUTF( env, jObjstring, objstring );
}


void jniastThrowException( JNIEnv *env, const char *fmt, ... ) {
/*
*+
*  Name:
*     jniastThrowException

*  Purpose:
*     Throws an AstException.

*  Description:
*     This routine throws an AstException with a generic status value
*     (SAI__ERROR).  It may be used to throw an exception in native
*     code which is generated by a condition outside of the calls
*     to the AST library itself.  It provides for convenience a
*     printf-like syntax.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     fmt = const char *
*        A printf-like format string.
*     ...
*        Printf-like optional extra arguments as required according to
*        fmt.
*-
*/
   va_list ap;

   va_start( ap, fmt );
   throwTypedThrowable( env, AstExceptionClass, fmt, ap );
   va_end( ap );

}

void jniastThrowIllegalArgumentException( JNIEnv *env, const char *fmt, ... ) {
/*
*+
*  Name:
*     jniastThrowIllegalArgumentException

*  Purpose:
*     Throws an IllegalArgumentException.

*  Description:
*     Throws an IllegalArgumentException, providing for a printf-like syntax.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     fmt = const char *
*        A printf-like format string.
*     ...
*        Printf-like optional extra arguments as required according to
*        fmt.
*-
*/
   va_list ap;

   va_start( ap, fmt );
   throwTypedThrowable( env, IllegalArgumentExceptionClass, fmt, ap );
   va_end( ap );
}

void jniastThrowError( JNIEnv *env, const char *fmt, ... ) {
/*
*+
*  Name:
*     jniastThrowError

*  Purpose:
*     Throws an Error.

*  Description:
*     This routine provides a single interface for throwing a throwable
*     from native JNI/AST code.  The throwable it currently throws is a
*     java.lang.Error, but this may be changed in the future.  In
*     general it should be used when the native code needs to signal
*     a serious error, particularly a programming error, not covered 
*     by one of the standard java language exceptions.  It provides for 
*     convenience a printf-like syntax.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     fmt = const char *
*        A printf-like format string.
*     ...
*        Printf-like optional extra arguments as required according to
*        fmt.
*-
*/
   va_list ap;

   va_start( ap, fmt );
   throwTypedThrowable( env, ErrorClass, fmt, ap );
   va_end( ap );
}
   

void jniastConstructStc( JNIEnv *env, jobject this, jobject jRegion,
                         jobjectArray jCoords, StcConstructor constructor ) {
/*
*+
*  Name:
*     jniastConstructStc

*  Purpose:
*     Performs JNIAST object construction on an Stc subclass.

*  Description:
*     This routine does the hard work of initializing an instance of one
*     of the Stc concrete subclasses.  Since all these classes have 
*     identical-looking constructors, the same code can be reused.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     this = jobject
*        Object reference for the Stc object to be initialized; on entry 
*        the pointer field of this has not been initialized, on exit it 
*        has been.
*     jRegion = jobject
*        Object reference for the region which is to be encapsulated
*        by the new Stc.
*     constructor = StcConstructor
*        Constructor function for the specific Stc subclass in use here.
*        This is invoked with the correct arguments to acquire the
*        AST object pointer used to initialise the this object.
*-
*/
   AstPointer pointer;
   AstRegion *region;
   AstKeyMap **coords;
   AstObject **ptrs;
   int ncoords;
   int i;
   jobject jCoord;

   if ( ! (*env)->ExceptionCheck( env ) &&
        jniastCheckNotNull( env, jRegion ) ) {
      region = jniastGetPointerField( env, jRegion ).Region;

      /* Get an array of AstKeyMap pointers from the array of java KeyMap
       * objects. */
      if ( jCoords ) {
         ncoords = (*env)->GetArrayLength( env, jCoords );
         coords = jniastMalloc( env, ncoords * sizeof( AstKeyMap * ) );
         if ( ! coords ) return;
         for ( i = 0; i < ncoords; i++ ) {
            if ( ! (*env)->ExceptionCheck( env ) ) {
               jCoord = (*env)->GetObjectArrayElement( env, jCoords, i );
               coords[ i ] = jniastGetPointerField( env, jCoord ).KeyMap;
            }
         }
      }
      else {
         ncoords = 0;
         coords = NULL;
      }

      /* Intialize the object pointer using the supplied specific
       * constructor function. */
      ptrs = jniastMalloc( env, ( ncoords + 2 ) * sizeof( AstObject * ) );
      for ( i = 0; i < ncoords; i++ ) {
         ptrs[ i ] = (AstObject *) coords[ i ];
      }
      ptrs[ ncoords ] = (AstObject *) region;
      ptrs[ ncoords + 1 ] = NULL;
      THASTCALL( ptrs,    /* ptrs will get freed by macro */
         pointer.Stc = (*constructor)( region, ncoords, coords, "" );
      )
      jniastInitObject( env, this, pointer );

      /* Tidy up. */
      if ( coords ) {
         ALWAYS(
            free( coords );
         )
      }
   }
}


static void throwTypedThrowable( JNIEnv *env, jclass throwclass, 
                                 const char *fmt, va_list ap ) {
/*
*+
*  Name:
*     throwTypedThrowable

*  Purpose:
*     Throw a throwable providing printf-like syntax.

*  Description:
*     This is a convenience routine to be called from this source file
*     only which provides printf-like variable argument syntax for
*     throwing exceptions of a given type.

*  Arguments:
*     env = JNIEnv *
*        Pointer to the JNI interface.
*     throwclass = jclass
*        The class of the exception which is to be thrown.
*     fmt = const char *
*        A printf-like format string.
*     ap = va_list
*        A pointer to the variable arguments required by fmt.
*-
*/
   char *buffer;
   int bufleng = 1024;
   int nchar;

   /* Only try to throw a new exception if we do not have one pending. */
   if ( ! (*env)->ExceptionCheck( env ) ) {

      /* Allocate a buffer and write the message into it.  In the unlikely
       * event that we have run out of memory, make do with printing the
       * format string alone. */
      buffer = jniastMalloc( env, bufleng + 1 );
      if ( buffer != NULL ) {
         nchar = vsnprintf( buffer, bufleng, fmt, ap );
         if ( nchar >= bufleng ) {
            sprintf( buffer + bufleng - 4, "..." );
         }
      }
      else {
         buffer = (char *) fmt;
      }

      /* Throw the exception. */
      if ( ! (*env)->ExceptionCheck( env ) ) {
         (*env)->ThrowNew( env, throwclass, (const char *) buffer );
      }

      /* Release the buffer. */
      if ( buffer != fmt ) free( buffer );
   }
}

static int ptrCmp( const void *p1, const void *p2 ) {
/*+
*  Name:
*     ptrCmp

*  Purpose:
*     Comparison function for use with qsort.

*  Description:
*     Compares pointer values using some arbitrary but determinate ordering
*     scheme.

*  Arguments:
*     p1 = void 
*        Pointer to first AstObject * value.
*     p2 = void *
*        Pointer to second AstObject * value.

*  Return value:
*     Positive, zero or negative integer depending on whether the first
*     value is lower than, equal to, or higher than the second.
*-
*/
   AstObject *v1;
   AstObject *v2;
   v1 = *((AstObject **) p1);
   v2 = *((AstObject **) p2);
   return v1 < v2 ? -1 
                  : ( v1 > v2 ? +1
                              : 0 );
}

/**
 *  Get the current thread environment.
 */
JNIEnv *jniastGetEnv()
{
    JNIEnv *env;
    (*java_vm)->GetEnv( java_vm, (void **) &env, JNI_VERSION_1_2 );
    return env;
}

/* $Id$ */
