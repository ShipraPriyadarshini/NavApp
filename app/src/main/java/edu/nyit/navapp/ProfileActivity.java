package edu.nyit.navapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText fName, lName, contact;
    private Button submit_btn;
    private FirebaseAuth mAuth;
    private DatabaseReference riderRef;
    private String riderID;
    private String first, last ,conNum ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        fName = (EditText) findViewById(R.id.firstName);
        lName = (EditText) findViewById(R.id.lastName);
        contact = (EditText) findViewById(R.id.contactNo);
        submit_btn = (Button) findViewById(R.id.submit_btn);

        mAuth = FirebaseAuth.getInstance();
        riderID = mAuth.getCurrentUser().getUid();
        riderRef = FirebaseDatabase.getInstance().getReference().child("users").child("rider").child(riderID);
        getProfile();

        submit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitProfile();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        return;

    }

    private void getProfile() {
        riderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() >= 1) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("firstName")!=null){
                        first=map.get("firstName").toString();
                        fName.setText(first);
                    }
                    if(map.get("lastName")!=null){
                        last=map.get("lastName").toString();
                        lName.setText(last);
                    }
                    if(map.get("contact")!=null){
                        conNum=map.get("contact").toString();
                        contact.setText(conNum);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }

        });
    }

    private void submitProfile() {
        if(!fName.getText().toString().matches("")&&!lName.getText().toString().matches("")&&!contact.getText().toString().matches("")) {
            first = fName.getText().toString();
            last = lName.getText().toString();
            conNum = contact.getText().toString();

            Map info = new HashMap();
            info.put("firstName", first);
            info.put("lastName", last);
            info.put("contact", conNum);
            riderRef.updateChildren(info);
            finish();
        }else{
            Toast.makeText(ProfileActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        }
    }
}
