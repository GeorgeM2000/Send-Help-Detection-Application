package com.example.sendhelpapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText phoneNumberInput;
    private ArrayList<String> phoneNumbers = new ArrayList<>();
    private byte phoneNumberCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberInput = findViewById(R.id.phone_number_input);
    }

    public void startService(View v){
        if(phoneNumberCount >= 1){
            Intent serviceIntent = new Intent(this, SendHelpService.class);
            serviceIntent.putStringArrayListExtra("inputExtra", phoneNumbers);

            // If SDK_INT Version is >= 26, use startForegroundService()
            // If SDK_INT Version is < 26, use startService()
            ContextCompat.startForegroundService(this, serviceIntent);
        }
    }

    public void stopService(View v){
        Intent serviceIntent = new Intent(this, SendHelpService.class);
        stopService(serviceIntent);
        phoneNumberCount = 0;
        phoneNumbers.clear();
    }

    public void savePhone(View v){
        String input = phoneNumberInput.getText().toString();
        if(input.matches("[0-9]+") && phoneNumberCount <= 8 && !(phoneNumbers.contains(input))) {
            phoneNumbers.add(input);
            phoneNumberInput.setText("");
            phoneNumberCount++;
        }
        else if(!input.matches("[0-9]+")){ showMessage("A Phone Number Is Required."); }
        else if(!(phoneNumberCount <= 8)) { showMessage("You Can Only Save 8 Phone Numbers."); }
        else { showMessage("Phone Number Already Exists."); }

    }

    private void showMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}