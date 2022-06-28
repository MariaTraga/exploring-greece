package com.malliaris_traga.exploringgreece;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesActivity extends AppCompatActivity implements LocationListener {

    FirebaseDatabase firebaseDatabase;
    List<GuestSelectionActivity.Attraction> favoriteList = new ArrayList<>();

    SQLiteDatabase db;

    LocationManager locationManager;
    double currentLongitude,currentLatitude;
    Location currentLocation;

    LinearLayout dynamicLayout;

    // VOICE COMMANDS
    public static final int VOICE_REC_RESULT = 765;
    Map<String, String> voiceCommands;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_voice_commands, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.enable_microphone){
            VoiceHandle();
        }
        else if(item.getItemId() == R.id.voice_commands){
            ShowVoiceCommandHelp();
        }
        return super.onOptionsItemSelected(item);
    }

    //onCreate Function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        dynamicLayout = findViewById(R.id.dynamicContainer2);
        db = openOrCreateDatabase("FavoritesDB",MODE_PRIVATE,null);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        currentLocation = new Location("currentLocation");

        EnableGPSUpdate();
        ReadDataFromDatabases();

        // Create voice commands
        voiceCommands = CreateVoiceCommands();
    }

    //Function that reads the data from the sqlite database and then reads the data for each id from firebase
    public void ReadDataFromDatabases(){
        //Remove any layout inside the dynamicLayout (in order to refresh the layout upon deleting a favorite)
        dynamicLayout.removeAllViews();

        //Read data from the sqlite database and if there are no entries finish the activity with result code 10
        Cursor cursor = db.rawQuery("SELECT * FROM Favorites;",null);

        if (cursor.getCount() == 0){
            setResult(10);
            finish();
        }

        //Clear the favoriteList
        favoriteList.clear();
        //While there are data in "Favorites" db call the function ReadDataFromFirebase for the selected id (i.e. column 0)
        while(cursor.moveToNext()){
            ReadDataFromFirebase(cursor.getString(0), cursor.getString(1));
        }
    }

    //Function to read data from firebase, add them to each favorite instance and set the layout that will be presented to the user
    private void ReadDataFromFirebase(String key, String title){
        firebaseDatabase = FirebaseDatabase.getInstance("https://exploringgreece-f2282-default-rtdb.firebaseio.com/");

        //Get each attraction branch as a database reference based on the key given
        DatabaseReference databaseReference = firebaseDatabase.getReference("Attractions").child(key);

        //Read data from firebase, add them on a list of attractions and create the layout for each attraction
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    GuestSelectionActivity.Attraction favorite = new GuestSelectionActivity.Attraction();
                    favorite = snapshot.getValue(GuestSelectionActivity.Attraction.class);
                    favorite.key = key;
                    favorite.favoritesDatabase = db;

                    favoriteList.add(favorite);
                    dynamicLayout.addView(favorite.CreateAttractionFavoriteLayout(FavoritesActivity.this));

                    Toast.makeText(FavoritesActivity.this, getString(R.string.toast_data_loaded), Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    db.execSQL("DELETE FROM Favorites WHERE id = ?;", new Object[]{key});
                    Toast.makeText(FavoritesActivity.this, getString(R.string.toast_favorite_not_found1) + title + getString(R.string.toast_favorite_not_found2), Toast.LENGTH_LONG).show();
                }
            }

            //Function when data can't be accessed from firebase
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FavoritesActivity.this, getString(R.string.toast_cant_access_data), Toast.LENGTH_LONG).show();
            }
        });
    }

    //Function to update the current location and calculate the distance to each attraction
    private void ManageLocationUpdates(Location location) {
        currentLongitude = location.getLongitude();
        currentLatitude = location.getLatitude();
        currentLocation.setLongitude(currentLongitude);
        currentLocation.setLatitude(currentLatitude);

        for (GuestSelectionActivity.Attraction attraction:favoriteList) {
            attraction.CalculateDistance(currentLocation);
        }
    }

    //Function that executed the ManageLocationUpdates function each time the location of the user changes
    @Override
    public void onLocationChanged(@NonNull Location location) {
        ManageLocationUpdates(location);
    }

    //Function to enable the gps location update if request is given and if not than show a message
    private void EnableGPSUpdate(){
        //If permission is not given, show message
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(FavoritesActivity.this, getString(R.string.toast_no_gps_permission),Toast.LENGTH_LONG).show();
            return;
        }
        //If permission is given, enable gps location update
        else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        }
    }

    //Stop Location Updates
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        locationManager.removeUpdates(this);
    }

    // VOICE COMMANDS
    // Function that creates voice commands
    private Map<String, String> CreateVoiceCommands(){
        Map<String, String> result = new HashMap<>();
        result.put("Back",getString(R.string.vc_back));
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
                .setTitle(getString(R.string.voice_commands))
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
                case "exit":
                    System.exit(0);
                    break;
                case "back":
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