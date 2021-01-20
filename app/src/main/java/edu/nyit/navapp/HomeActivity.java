package edu.nyit.navapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    private Button rider, driver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        rider =(Button) findViewById(R.id.rider_btn);
        driver =(Button) findViewById(R.id.driver_btn);

        rider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, RiderLoginActivity.class);
                Toast.makeText(getApplicationContext(), "Logging in as Rider",Toast.LENGTH_SHORT).show();
                startActivity(intent);
                //finish(); remove the comment later if we decide on removing the activity from the stack
                return;
            }
        });
        driver.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, DriverLoginActivity.class);
                Toast.makeText(getApplicationContext(), "Logging in as Driver",Toast.LENGTH_SHORT).show();
                startActivity(intent);
                //finish(); remove the comment later if we decide on removing the activity from the stack
                return;
            }
        });
    }
}
