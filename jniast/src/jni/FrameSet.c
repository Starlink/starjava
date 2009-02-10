/*
*+
*  Name:
*     FrameSet.c

*  Purpose:
*     JNI implementations of native methods of FrameSet class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     25-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_FrameSet.h"


/* Instance methods. */


JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FrameSet_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame        /* First frame */
) {
   AstPointer pointer;
   AstPointer frame;

   if ( jniastCheckNotNull( env, jFrame ) ) {
      frame = jniastGetPointerField( env, jFrame );
      THASTCALL( jniastList( 1, frame.AstObject ),
         pointer.FrameSet = astFrameSet( frame.Frame, "" );
      )
      jniastInitObject( env, this, pointer );
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FrameSet_addFrame(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint iframe,          /* Index of added frame */
   jobject jMap,         /* Mapping */
   jobject jFrame        /* New frame to add */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer map;
   AstPointer frame;

   /* Validate arguments. */
   if ( jniastCheckNotNull( env, jMap ) &&
        jniastCheckNotNull( env, jFrame ) ) {

      /* Get C data from java data. */
      map = jniastGetPointerField( env, jMap );
      frame = jniastGetPointerField( env, jFrame );

      /* Call the C function to do the work. */
      THASTCALL( jniastList( 3, pointer.AstObject, map.AstObject,
                                frame.AstObject ),
         astAddFrame( pointer.FrameSet, (int) iframe, map.Mapping,
                      frame.Frame );
      )
   }
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_FrameSet_getFrame(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint iframe           /* Index of the frame to get */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstFrame *frame;

   /* Call the C function to do the work. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      frame = astGetFrame( pointer.FrameSet, (int) iframe );
   )

   /* Create and return a new java object. */
   return jniastMakeObject( env, (AstObject *) frame );
}

JNIEXPORT jobject JNICALL Java_uk_ac_starlink_ast_FrameSet_getMapping(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint iframe1,         /* Index of first frame */
   jint iframe2          /* Index of second frame */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstMapping *map;

   /* Call the C function to do the work. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      map = astGetMapping( pointer.FrameSet, (int) iframe1, (int) iframe2 );
   )

   /* Create and return a new java object. */
   return jniastMakeObject( env, (AstObject *) map );
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FrameSet_remapFrame(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint iframe,          /* Frame index */
   jobject jMap          /* New mapping */
) {
   AstPointer pointer = jniastGetPointerField( env, this );
   AstPointer map;

   /* Validate arguments. */
   if ( jniastCheckNotNull( env, jMap ) ) {

      /* Get C data from java data. */
      map = jniastGetPointerField( env, jMap );
      
      /* Call the C function to do the work. */
      THASTCALL( jniastList( 2, pointer.AstObject, map.AstObject ),
         astRemapFrame( pointer.FrameSet, (int) iframe, map.Mapping );
      )
   }
}

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_FrameSet_removeFrame(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jint iframe           /* Frame index */
) {
   AstPointer pointer = jniastGetPointerField( env, this );

   /* Call the C function to do the work. */
   THASTCALL( jniastList( 1, pointer.AstObject ),
      astRemoveFrame( pointer.FrameSet, (int) iframe );
   )
}

/* $Id$ */
