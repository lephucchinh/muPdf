package com.artifex.mupdf.viewer

import android.annotation.SuppressLint
import android.os.Build
import android.view.View

class Stepper(protected val mPoster: View, protected val mTask: Runnable) {
    protected var mPending: Boolean = false

    @SuppressLint("NewApi")
    fun prod() {
        if (!mPending) {
            mPending = true
            mPoster.postOnAnimation {
                mPending = false
                mTask.run()
            }
        }
    }
}
