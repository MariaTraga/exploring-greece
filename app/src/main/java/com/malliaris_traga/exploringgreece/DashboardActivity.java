package com.malliaris_traga.exploringgreece;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.Image;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;

public class DashboardActivity extends AppCompatActivity {

    // Activity code
    public static final int ADD_ATTRACTION_CODE = 105;

    // Screen scale
    float scale;

    // Firebase database
    FirebaseDatabase database;
    DatabaseReference dbRef;

    // Main Attraction layout
    LinearLayout attractionLayout;

    // VOICE COMMANDS
    public static final int VOICE_REC_RESULT = 765;
    Map<String, String> voiceCommands;

    // Function for creating the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_voice_commands, menu);
        menuInflater.inflate(R.menu.menu_admin, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Function that determines which menu button is clicked and call the respective function
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.menu_admin_item_logout){
            Logout();
        }
        else if(item.getItemId() == R.id.enable_microphone){
            VoiceHandle();
        }
        else if(item.getItemId() == R.id.voice_commands){
            ShowVoiceCommandHelp();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize Global variables
        scale =  DashboardActivity.this.getResources().getDisplayMetrics().density;
        database = FirebaseDatabase.getInstance("https://exploringgreece-f2282-default-rtdb.firebaseio.com/");
        dbRef = database.getReference("Attractions");
        attractionLayout = findViewById(R.id.attractionLayout);

        // Fetch attractions and create UI
        ReadDataFromFirebase();

        // Create voice commands
        voiceCommands = CreateVoiceCommands();
    }

    // Function that signs out the admin from the dashboard back to the login screen
    private void Logout(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        FirebaseAuth.getInstance().signOut();
        startActivity(intent);
    }

    // Function that loads attractions from firebase database and creates their layouts
    private void ReadDataFromFirebase(){
        //Read data from firebase, add them on a list of attractions and create the layout for each attraction
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // clear views
                attractionLayout.removeAllViews();
                // load new views
                for(DataSnapshot ds : snapshot.getChildren()){

                    GuestSelectionActivity.Attraction attraction = ds.getValue(GuestSelectionActivity.Attraction.class);

                    attraction.key = ds.getKey();
                    attractionLayout.addView(CreateAttractionEntryUI(DashboardActivity.this, attraction));

                    // add space for better visual
                    Space space = new Space(DashboardActivity.this);
                    int heightDP = DPtoPixel(10, scale);
                    space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightDP, 1));
                    attractionLayout.addView(space);
                }
                Toast.makeText(DashboardActivity.this, getString(R.string.toast_data_loaded),Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DashboardActivity.this,getString(R.string.toast_cant_access_data),Toast.LENGTH_LONG).show();
            }
        });
    }

    // Function that is called whenever Add New button is clicked and calls the AddNewAttraction function
    public void onAddNewClick(View view){
        AddNewAttraction();
    }

    // Function that starts activity for creating a new attraction entry
    private void AddNewAttraction() {
        Intent intent = new Intent(this, PointOfInterestCreationActivity.class);
        intent.putExtra("isNew", true);
        this.startActivityForResult(intent, ADD_ATTRACTION_CODE);
    }

    // Function that starts activity for editing selected attraction
    private void EditAttraction(GuestSelectionActivity.Attraction attraction){
        Intent intent = new Intent(this, PointOfInterestCreationActivity.class);
        intent.putExtra("isNew", false);
        intent.putExtra("attractionID", attraction.key);
        intent.putExtra("title", attraction.key);
        intent.putExtra("info_en", attraction.getInfo_en());
        intent.putExtra("info_gr", attraction.getInfo_gr());
        intent.putExtra("info_de", attraction.getInfo_de());
        intent.putExtra("latitude", attraction.getLatitude());
        intent.putExtra("longitude", attraction.getLongitude());
        intent.putExtra("imageURL", attraction.getImage());
        this.startActivityForResult(intent, ADD_ATTRACTION_CODE);
    }

    // Function that deletes attraction from the firebase database
    private void DeleteAttraction(String attractionID){
        dbRef.child(attractionID).removeValue();
    }

    // Function that handles activity results (Attraction save/edit & Voice recognition)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ADD_ATTRACTION_CODE && resultCode == PointOfInterestCreationActivity.ADD_ATTRACTION_SUCCESS){
            Toast.makeText(this, getString(R.string.toast_attraction_saved_success), Toast.LENGTH_LONG).show();
        }
        // VOICE COMMANDS
        else if(requestCode == VOICE_REC_RESULT && resultCode == RESULT_OK) {
            RecognizeVoice(data);
        }
    }

    // Function that creates a linearLayout housing the contents of an attraction
    private LinearLayout CreateAttractionEntryUI(Context context, GuestSelectionActivity.Attraction attraction){

        // Result Layout
        LinearLayout attractionLayout = new LinearLayout(context);
        attractionLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,1));

        // Image
        ImageView imageView = new ImageView(this);
        int pixels = DPtoPixel(250, scale);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(pixels,pixels));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        Picasso.with(this).load(attraction.getImage())
                .into(imageView);
        // add image to attraction result layout
        attractionLayout.addView(imageView);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);

        // Info Layout
        LinearLayout infoLayout = new LinearLayout(context);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 9));
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 4);

        // title
        TextView titleText = new TextView(this);
        titleText.setText(attraction.getTitle());
        titleText.setTextSize(20);
        titleText.setTypeface(ResourcesCompat.getFont(context, R.font.autour_one));
        titleText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        titleText.setLayoutParams(params);
        // add title to info layout
        infoLayout.addView(titleText);

        // Buttons
        //LinearLayout buttonLayout = new LinearLayout(this);

        Button editButton = new Button(this);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditAttraction(attraction);
            }
        });
        editButton.setText(getString(R.string.edit));
        Button deleteButton = new Button(this);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteAttraction(attraction.key);
            }
        });
        deleteButton.setText(getString(R.string.delete));
        // set layout params
        int marginSide = DPtoPixel(25, scale);
        int marginTopBot = DPtoPixel(20, scale);
        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(marginSide,marginTopBot,marginSide,marginTopBot);
        deleteButton.setLayoutParams(params);
        editButton.setLayoutParams(params);

        // set background
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#7E1CA67A"));
        background.setCornerRadius(45);
        deleteButton.setBackground(background);
        editButton.setBackground(background);

        // add buttons to button layout
        infoLayout.addView(editButton);
        infoLayout.addView(deleteButton);

        // add button layout to info layout
        //infoLayout.addView(buttonLayout);
        // add info layout to resulting layout
        attractionLayout.addView(infoLayout);

        return attractionLayout;
    }

    // Function that converts given dpi to pixel based on the given scale
    public static int DPtoPixel(double DPI, float scale){
        return (int) (DPI * scale + 0.5f);
    }

    // VOICE COMMANDS
    // Function that creates voice commands
    private Map<String, String> CreateVoiceCommands(){
        Map<String, String> result = new HashMap<>();
        result.put("Logout",getString(R.string.vc_logout));
        result.put("Add",getString(R.string.vc_add));
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
    private void RecognizeVoice(Intent data) {
        ArrayList<String> strings = data.getStringArrayListExtra((RecognizerIntent.EXTRA_RESULTS));
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {
            builder.append(s).append("\n");
        }
        String speechToText = builder.toString().toLowerCase();

        switch (speechToText.trim()) {
            case "logout":
                Logout();
                break;
            case "exit":
                System.exit(0);
                break;
            case "add":
                AddNewAttraction();
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