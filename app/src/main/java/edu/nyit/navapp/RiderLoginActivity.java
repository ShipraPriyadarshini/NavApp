package edu.nyit.navapp;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RiderLoginActivity extends AppCompatActivity {
    private EditText email, password;
    private Button login, newUser;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_login);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.isEmailVerified()) {
                    Intent intent = new Intent(RiderLoginActivity.this, RiderMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        email = (EditText) findViewById(R.id.email_et);
        password = (EditText) findViewById(R.id.password_et);
        login = (Button) findViewById(R.id.login_btn);
        newUser = (Button) findViewById(R.id.new_user_btn);
        newUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String Email = email.getText().toString();
                final String Password = password.getText().toString();
                mAuth.createUserWithEmailAndPassword(Email, Password).addOnCompleteListener(RiderLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Log.e("Rider Registration", "sign up error");
                            Toast.makeText(RiderLoginActivity.this, "sign up error", Toast.LENGTH_SHORT).show();
                        } else {
                            //
                            mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(RiderLoginActivity.this, "Registered. Please verify email", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RiderLoginActivity.this, HomeActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(RiderLoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                                    }

                                }
                            });
                            //
                            try {
                                String user_id = mAuth.getCurrentUser().getUid();
                                DatabaseReference current_user = FirebaseDatabase.getInstance().getReference().child("users").child("rider").child(user_id);
                                current_user.setValue(true);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                                Log.e("Rider Registration", "user id null exception");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String Email = email.getText().toString();
                final String Password = password.getText().toString();
                mAuth.signInWithEmailAndPassword(Email, Password).addOnCompleteListener(RiderLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Log.e("Rider Login", "sign in error");
                            Toast.makeText(RiderLoginActivity.this, "sign in error", Toast.LENGTH_SHORT).show();
                        }else{
                            if(mAuth.getCurrentUser().isEmailVerified()) {
                                Toast.makeText(RiderLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RiderLoginActivity.this, MapsActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                    }
                });

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
