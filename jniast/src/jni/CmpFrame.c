/*
*+
*  Name:
*     CmpFrame.c

*  Purpose:
*     JNI implementations of native methods of CmpFrame class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor (Starlink)

*  History:
*     27-SEP-2001 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include <stdlib.h>
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_CmpFrame.h"


/* Instance methods. */

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_CmpFrame_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame1,      /* First Frame object */
   jobject jFrame2       /* Second Frame object */
) {
   AstPointer pointer;
   AstPointer frm1Pointer;
   AstPointer frm2Pointer;

   if ( jniastCheckNotNull( env, jFrame1 ) &&
        jniastCheckNotNull( env, jFrame2 ) ) {
      frm1Pointer = jniastGetPointerField( env, jFrame1 );
      frm2Pointer = jniastGetPointerField( env, jFrame2 );

      THASTCALL( jniastList( 2, frm1Pointer.AstObject, frm2Pointer.AstObject ),
         pointer.CmpFrame = astCmpFrame( frm1Pointer.Frame, frm2Pointer.Frame,
                                         "" );
      )
      jniastInitObject( env, this, pointer );
   }
}

/* $Id$ */
