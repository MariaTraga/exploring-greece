package com.malliaris_traga.exploringgreece;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PointOfInterestCreationActivity extends AppCompatActivity {

    public static final int ADD_ATTRACTION_SUCCESS = 106;
    public static final int ADD_ATTRACTION_FAILURE = 107;

    // VOICE COMMANDS
    public static final int VOICE_REC_RESULT = 765;
    Map<String, String> voiceCommands;

    // Firebase database variables
    FirebaseDatabase database;
    DatabaseReference dbRef;

    // Attraction variables
    boolean isNew;
    String attractionKey;
    EditText title, info_gr, info_en, info_de, latitude, longitude, imageURL;

    //Function that creates a menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_voice_commands, menu);
        return true;
    }

    //Function when the menu item is clicked
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.enable_microphone){
            VoiceHandle();
        }
        else if(item.getItemId() == R.id.voice_commands){
            ShowVoiceCommandHelp();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_of_interest_creation);

        // Initialize variables
        database = FirebaseDatabase.getInstance("https://exploringgreece-f2282-default-rtdb.firebaseio.com/");
        dbRef = database.getReference("Attractions");

        title = findViewById(R.id.editText_Title);
        info_gr = findViewById(R.id.editText_info_gr);
        info_en = findViewById(R.id.editText_info_en);
        info_de = findViewById(R.id.editText_info_de);
        latitude = findViewById(R.id.editText_Latitude);
        longitude = findViewById(R.id.editText_Longitude);
        imageURL = findViewById(R.id.editText_ImageURL);

        // Check if mode is Edit or Create
        isNew = this.getIntent().getBooleanExtra("isNew", false);
        if(!isNew){ // If mode is Edit then load previous attraction values
            attractionKey = this.getIntent().getStringExtra("attractionID");
            title.setText(this.getIntent().getStringExtra("title"));
            info_gr.setText(this.getIntent().getStringExtra("info_gr"));
            info_en.setText(this.getIntent().getStringExtra("info_en"));
            info_de.setText(this.getIntent().getStringExtra("info_de"));
            latitude.setText(String.valueOf(this.getIntent().getDoubleExtra("latitude", 0)));
            longitude.setText(String.valueOf(this.getIntent().getDoubleExtra("longitude", 0)));
            imageURL.setText(this.getIntent().getStringExtra("imageURL"));
        }

        // Create voice commands
        voiceCommands = CreateVoiceCommands();
    }

    // Save button click
    public void onSaveClick(View view){
        // Check all input fields
        if(!IsUserInputValid()){
            return;
        }
        // Call save method
        SaveAttraction();
    }

    // Cancel button click
    public void onCancelClick(View view){
        // Set result and return to previous activity
        this.setResult(ADD_ATTRACTION_FAILURE);
        finish();
    }

    // Function that checks if any of the input fields is empty
    private boolean IsUserInputValid(){
        return title.getText() != null &&
                info_gr.getText() != null &&
                info_en.getText() != null &&
                info_de.getText() != null &&
                latitude.getText() != null &&
                longitude.getText() != null &&
                imageURL.getText() != null;
    }

    // Function that saves current values to a New Attraction / updates values of Existing Attraction Entry on Firebase Database
    private void SaveAttraction() {
        // Create Attraction instance and set data
        GuestSelectionActivity.Attraction newAttraction = new GuestSelectionActivity.Attraction();
        newAttraction.setTitle(title.getText().toString());
        newAttraction.setImage(imageURL.getText().toString());
        newAttraction.setInfo_gr(info_gr.getText().toString());
        newAttraction.setInfo_en(info_en.getText().toString());
        newAttraction.setInfo_de(info_de.getText().toString());
        newAttraction.setLatitude(Double.parseDouble(latitude.getText().toString()));
        newAttraction.setLongitude(Double.parseDouble(latitude.getText().toString()));

        // Save as new
        if(isNew){
            dbRef.push().setValue(newAttraction).addOnCompleteListener(
                    new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            SetSaveResult(task.isSuccessful());
                        }
                    }
            );
        }
        // Update existing
        else{
            dbRef.child(attractionKey).setValue(newAttraction).addOnCompleteListener(
                    new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            SetSaveResult(task.isSuccessful());
                        }
                    }
            );
        }
    }

    // Function that sets result as successful and finishes activity or prints message and returns
    private void SetSaveResult(boolean success) {
        if (success) {
            this.setResult(ADD_ATTRACTION_SUCCESS);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.toast_save_failed), Toast.LENGTH_LONG).show();
        }
    }

    // VOICE COMMANDS
    // Function that creates voice commands
    private Map<String, String> CreateVoiceCommands(){
        Map<String, String> result = new HashMap<>();
        result.put("Save",getString(R.string.vc_save));
        result.put("Cancel",getString(R.string.vc_cancel));
        result.put("Exit",getString(R.string.vc_exit));
        result.put("Help",getString(R.string.vc_help));
        return result;
    }

    // Function that shows available voice commands inside alert box
    private void ShowVoiceCommandHelp(){
        if(voiceCommands == null){
            Toast.makeText(this, getString(R.string.toast_no_voice_commands), Toast.LENGTH_LONG).show();
            return;
        }

        StringBuilder  stringBuilder = new StringBuilder();

        for(Map.Entry<String, String> entry : voiceCommands.entrySet()){
            stringBuilder.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n---------------------\n");
        }

        String message = stringBuilder.toString();

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Voice Commands")
                .setMessage(message)
                .show();
    }

    // Function that starts voice recognition
    private void VoiceHandle(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        startActivityForResult(intent,VOICE_REC_RESULT);
    }

    // Function that handles the voice recognition result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == VOICE_REC_RESULT && resultCode == RESULT_OK){
            ArrayList<String> strings = data.getStringArrayListExtra((RecognizerIntent.EXTRA_RESULTS));
            StringBuilder builder = new StringBuilder();
            for(String s : strings){
                builder.append(s).append("\n");
            }
            String speechToText = builder.toString().toLowerCase();

            switch (speechToText.trim()){
                case "save":
                    SaveAttraction();
                    break;
                case "exit":
                    System.exit(0);
                    break;
                case "cancel":
                    finish();
                    break;
                case "help":
                    ShowVoiceCommandHelp();
                    break;
                default:
                    Toast.makeText(this, getString(R.string.toast_voice_command_not_found), Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}