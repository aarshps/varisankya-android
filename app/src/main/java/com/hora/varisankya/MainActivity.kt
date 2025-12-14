package com.hora.varisankya

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    // Variables
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnSignIn: Button
    private lateinit var btnLogout: Button
    private lateinit var tvHelloWorld: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Connect to the XML buttons we made
        btnSignIn = findViewById(R.id.btnSignIn)
        btnLogout = findViewById(R.id.btnLogout)
        tvHelloWorld = findViewById(R.id.tvHelloWorld)

        // 2. Setup Firebase and Google Login
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("663138385072-bke7f5oflsl2cg0e5maks0ef3n6o113u.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. Check if user is ALREADY logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(true)
        }

        // 4. Handle Click: Sign In
        btnSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, 9001)
        }

        // 5. Handle Click: Logout
        btnLogout.setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut()
            updateUI(false)
            Toast.makeText(this, "Logged Out!", Toast.LENGTH_SHORT).show()
        }
    }

    // This runs when the Google Login screen closes
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 9001) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Login succeeded, now authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: " + e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login Success
                    Toast.makeText(this, "Welcome " + auth.currentUser?.displayName, Toast.LENGTH_SHORT).show()
                    updateUI(true)
                } else {
                    // Login Failed
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    updateUI(false)
                }
            }
    }

    // Show/Hide buttons based on login status
    private fun updateUI(isLoggedIn: Boolean) {
        if (isLoggedIn) {
            tvHelloWorld.visibility = View.VISIBLE
            btnLogout.visibility = View.VISIBLE
            btnSignIn.visibility = View.GONE
        } else {
            tvHelloWorld.visibility = View.GONE
            btnLogout.visibility = View.GONE
            btnSignIn.visibility = View.VISIBLE
        }
    }
}