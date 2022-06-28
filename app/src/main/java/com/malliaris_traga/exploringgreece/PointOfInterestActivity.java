package com.malliaris_traga.exploringgreece;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PointOfInterestActivity extends AppCompatActivity implements OnMapReadyCallback, TextToSpeech.OnInitListener {

    TextView textViewTitle, textViewInfo;
    ImageView imageView;
    String id,title,info,image;
    double longitude,latitude;

    TextToSpeech myTts;

    SQLiteDatabase db;

    // VOICE COMMANDS
    public static final int VOICE_REC_RESULT = 765;
    Map<String, String> voiceCommands;

    //onCreate Function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_of_interest);

        //Open SQLite Database
        db = openOrCreateDatabase("FavoritesDB",MODE_PRIVATE,null);

        //Fragment to hold the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        myTts = new TextToSpeech(this,this);

        //Get extras from activity that opened the PointOfInterestActivity
        Bundle extras = getIntent().getExtras();
        if(extras!=null){
            id = extras.getString("Key");
            title = extras.getString("Title");
            info = extras.getString("Info");
            image = extras.getString("Image");
            longitude = extras.getDouble("Longitude");
            latitude = extras.getDouble("Latitude");
        }

        textViewTitle = findViewById(R.id.textView6);
        textViewInfo = findViewById(R.id.textView7);
        imageView = findViewById(R.id.imageView2);

        textViewTitle.setText(title);
        textViewInfo.setText(info);
        Picasso.with(this).load(image).into(imageView);

        // Create voice commands
        voiceCommands = CreateVoiceCommands();
    }

    //Function that creates a menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_voice_commands, menu);
        inflater.inflate(R.menu.menu_favorite,menu);
        return true;
    }

    //Function when the menu item is clicked
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.add_favorite){
            AddToFavorites();
        }
        else if(item.getItemId() == R.id.text_to_speech){
            InfoTextToSpeech();
        }
        else if(item.getItemId() == R.id.enable_microphone){
            VoiceHandle();
        }
        else if(item.getItemId() == R.id.voice_commands){
            ShowVoiceCommandHelp();
        }
        return true;
    }

    //Function that adds the id and title into the sqlite Database
    public void AddToFavorites(){
        try {
            String insertSQL = "INSERT INTO \"Favorites\" (\"id\",\"title\") VALUES (?,?);";
            db.execSQL(insertSQL, new Object[]{id,title});
            Toast.makeText(this, getString(R.string.toast_favorite_added) ,Toast.LENGTH_LONG).show();
        }
        catch(Exception e){
            Toast.makeText(this, getString(R.string.toast_cannot_add),Toast.LENGTH_LONG).show();
        }
    }

    //Function that adds a marker on the map created and moves the camera closer to the marker
    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng location = new LatLng(latitude,longitude);
        googleMap.addMarker(new MarkerOptions().position(location).title(title));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,10));
        // Zoom in, animating the camera.
        //googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        // Zoom out to zoom level 10, animating with a duration of 2 seconds.
        //googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
    }

    //Function that takes the info text and starts the TTS procedure
    private void InfoTextToSpeech(){
        String ttsText = textViewInfo.getText().toString();
        myTts.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null, null);
    }
    //Function to handle the TTS in case the activity is destroyed
    @Override
    public void onDestroy() {
        if(myTts != null){
            myTts.stop();
            myTts.shutdown();
        }
        super.onDestroy();
    }
    //Function to initialize and set the language for TTS
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (Locale.getDefault().getLanguage().equals("en")) {
                myTts.setLanguage(Locale.ENGLISH);
            } else if (Locale.getDefault().getLanguage().equals("de")) {
                myTts.setLanguage(Locale.GERMAN);
            } else if (Locale.getDefault().getLanguage().equals("el")){
                myTts.setLanguage(new Locale("el", "GR"));
            }
            else{
                Toast.makeText(PointOfInterestActivity.this, "Language not supported", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(PointOfInterestActivity.this, "TTS Initialization failed", Toast.LENGTH_LONG).show();
        }
    }

    // VOICE COMMANDS
    // Function that creates voice commands
    private Map<String, String> CreateVoiceCommands(){
        Map<String, String> result = new HashMap<>();
        result.put("Favorite",getString(R.string.vc_favorite));
        result.put("Speak",getString(R.string.vc_speak));
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
                case "favorite":
                    AddToFavorites();
                    break;
                case "exit":
                    System.exit(0);
                    break;
                case "back":
                    finish();
                    break;
                case "speak":
                    InfoTextToSpeech();
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