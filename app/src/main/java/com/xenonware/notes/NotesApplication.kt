
package com.xenonware.notes

import android.app.Application
import com.google.firebase.FirebaseApp

class NotesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
