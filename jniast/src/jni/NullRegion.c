/*
*+
*  Name:
*     NullRegion.c

*  Purpose:
*     JNI implemenatations of native methods for NullRegion class.

*  Language:
*     ANSI C.

*  Authors:
*     MBT: Mark Taylor

*  History:
*     18-JUL-2005 (MBT):
*        Original version.
*-
*/

/* Header files. */
#include "jni.h"
#include "ast.h"
#include "jniast.h"
#include "uk_ac_starlink_ast_NullRegion.h"

JNIEXPORT void JNICALL Java_uk_ac_starlink_ast_NullRegion_construct(
   JNIEnv *env,          /* Interface pointer */
   jobject this,         /* Instance object */
   jobject jFrame,       /* Frame */
   jobject jUnc          /* Uncertainty region */
) {
   AstPointer pointer;
   AstFrame *frame;
   AstRegion *unc;

   unc = jUnc ? jniastGetPointerField( env, jUnc ).Region : NULL;
   if ( jniastCheckNotNull( env, jFrame ) ) {
      frame = jniastGetPointerField( env, jFrame ).Frame;
      THASTCALL( jniastList( 2, frame, unc ),
         pointer.NullRegion = astNullRegion( frame, unc, "" );
      )
      jniastInitObject( env, this, pointer );
   }
}
