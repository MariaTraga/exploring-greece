package com.malliaris_traga.exploringgreece;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.security.Permission;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

public class GuestSelectionActivity extends AppCompatActivity implements LocationListener {

    FirebaseDatabase firebaseDatabase;
    List<Attraction> attractionList = new ArrayList<>();

    SQLiteDatabase db;

    LocationManager locationManager;
    double currentLongitude,currentLatitude;
    Location currentLocation;

    LinearLayout dynamicLayout;

    // VOICE COMMANDS
    public static final int VOICE_REC_RESULT = 765;
    Map<String, String> voiceCommands;

    //Class to handle each attraction instance
    public static class Attraction{
        //Variables with the same name as in the firebase database
        private String Image;
        private String Info_de;
        private String Info_en;
        private String Info_gr;
        private double Latitude;
        private double Longitude;
        private String Title;

        // Default constructor required for calls to DataSnapshot.getValue(User.class)
        public Attraction(){ }

        public Attraction(String image, String info_de, String info_en,  String info_gr, double latitude, double longitude, String title) {
            Image = image;
            Info_de = info_de;
            Info_en = info_en;
            Info_gr = info_gr;
            Latitude = latitude;
            Longitude = longitude;
            Title = title;
        }

        //Getters and Setters for the firebase data
        public String getImage() {
            return Image;
        }
        public void setImage(String imageURI) {
            Image = imageURI;
        }
        public String getInfo_de() {
            return Info_de;
        }
        public void setInfo_de(String info_de) {
            Info_de = info_de;
        }
        public String getInfo_en() {
            return Info_en;
        }
        public void setInfo_en(String info_en) {
            Info_en = info_en;
        }
        public String getInfo_gr() {
            return Info_gr;
        }
        public void setInfo_gr(String info_gr) {
            Info_gr = info_gr;
        }
        public double getLatitude() {
            return Latitude;
        }
        public void setLatitude(double latitude) {
            Latitude = latitude;
        }
        public double getLongitude() {
            return Longitude;
        }
        public void setLongitude(double longitude) {
            Longitude = longitude;
        }
        public String getTitle() {
            return Title;
        }
        public void setTitle(String title) {
            Title = title;
        }

        public static SQLiteDatabase favoritesDatabase;

        TextView distanceTextView, titleTextView;
        ImageView imageView;
        Button buttonView, buttonRemove;
        float scale;
        String key; //variable to hold id of each attraction in firebase
        float distance;
        ConstraintLayout myCLayout;

        //Functions to create each attraction/favorite component (for GuestSelectionActivity and FavoritesActivity)
        public ConstraintLayout CreateAttractionLayout(Context context) {
            //Set scale for the pixel to dp conversion
            scale = context.getResources().getDisplayMetrics().density;

            //Create horizontal layout for title and distance text
            LinearLayout layoutText = new LinearLayout(context);
            layoutText.setOrientation(LinearLayout.HORIZONTAL);

            //Set parameters to the horizontal layout
            LinearLayout.LayoutParams horizontalLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            horizontalLayoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER;
            layoutText.setLayoutParams(horizontalLayoutParams);
            layoutText.setBackgroundColor(0xAFFFFFFF);

            //Create the title text component of the Point of Interest button
            titleTextView = new TextView(context);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 5);
            titleParams.setMargins(15, 0, 0, 0);
            titleTextView.setLayoutParams(titleParams);
            titleTextView.setTextSize(30);
            titleTextView.setTypeface(ResourcesCompat.getFont(context, R.font.autour_one));
            titleTextView.setText(Title);

            //Create the distance text component of the Point of Interest button
            distanceTextView = new TextView(context);
            LinearLayout.LayoutParams distanceParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            distanceParams.setMargins(0, 0, 15, 0);
            distanceTextView.setLayoutParams(distanceParams);
            distanceTextView.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
            distanceTextView.setTextSize(18);
            distanceTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            distanceTextView.setText("...");

            //Add the text components on the horizontal layout
            layoutText.addView(titleTextView);
            layoutText.addView(distanceTextView);

            //Create layout for an image and the horizontal layout
            ConstraintLayout layoutAttraction = new ConstraintLayout(context);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 0, 0, DashboardActivity.DPtoPixel(15, scale));
            layoutAttraction.setLayoutParams(layoutParams);

            //Create the image component of the Point of Interest button
            int pixel = DashboardActivity.DPtoPixel(300, scale);
            imageView = new ImageView(context);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pixel);
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            Picasso.with(context)
                    .load(Image)
                    .into(imageView);

            //Add the image and horizontal layout components on the vertical layout
            layoutAttraction.addView(imageView);
            layoutAttraction.addView(layoutText);
            layoutAttraction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MoveToPOIActivity(context);
                }
            });
            myCLayout = layoutAttraction;
            return layoutAttraction;
        }

        public LinearLayout CreateAttractionFavoriteLayout(Context context) {
            //Set scale for the pixel to dp conversion
            scale = context.getResources().getDisplayMetrics().density;

            //Create horizontal layout for title and distance text
            LinearLayout layoutText = new LinearLayout(context);
            layoutText.setOrientation(LinearLayout.HORIZONTAL);

            //Set parameters to the horizontal layout
            LinearLayout.LayoutParams horizontalLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            layoutText.setLayoutParams(horizontalLayoutParams);

            //Create the title text component of the Point of Interest button
            titleTextView = new TextView(context);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 5);
            titleParams.setMargins(15, 0, 0, 0);
            titleTextView.setLayoutParams(titleParams);
            titleTextView.setTextSize(30);
            titleTextView.setText(Title);
            titleTextView.setTypeface(ResourcesCompat.getFont(context, R.font.autour_one));

            //Create the distance text component of the Point of Interest button
            distanceTextView = new TextView(context);
            LinearLayout.LayoutParams distanceParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            distanceParams.setMargins(0, 0, 15, 0);
            distanceTextView.setLayoutParams(distanceParams);
            distanceTextView.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
            distanceTextView.setTextSize(18);
            distanceTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            distanceTextView.setText("...");

            //Add the text components on the horizontal layout
            layoutText.addView(titleTextView);
            layoutText.addView(distanceTextView);

            //Create the vertical layout for the 2 buttons
            LinearLayout layoutButtons = new LinearLayout(context);
            layoutButtons.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutButtons.setLayoutParams(buttonLayoutParams);

            //Create the 2 buttons and their parameters
            buttonView = new Button(context);
            buttonRemove = new Button(context);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            int marginSide = DashboardActivity.DPtoPixel(15, scale);
            int marginTopBot = DashboardActivity.DPtoPixel(10, scale);
            buttonParams.setMargins(marginSide,marginTopBot,marginSide,marginTopBot);

            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.parseColor("#7E1CA67A"));
            background.setCornerRadius(45);


            //Button to view the favorite selected
            buttonView.setLayoutParams(buttonParams);
            buttonView.setBackground(background);
            buttonView.setText(context.getString(R.string.view_favorite));
            buttonView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MoveToPOIActivity(context);
                }
            });

            //Button to remove the favorite selected
            buttonRemove.setLayoutParams(buttonParams);
            buttonRemove.setBackground(background);
            buttonRemove.setText(context.getString(R.string.remove_favorite));
            buttonRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RemoveFavorite(favoritesDatabase, key);

                    //Cast the context variable into a FavoritesActivity activity in order to access the active instance of the activity
                    FavoritesActivity activity = (FavoritesActivity) context;
//                      //Execute the ReadDataFromDatabases function again upon removing a favorite (refreshing the UI)
                    activity.ReadDataFromDatabases();
                }
            });

            layoutButtons.addView(buttonView);
            layoutButtons.addView(buttonRemove);

            //Create horizontal layout for the image and the button and set parameters to the horizontal layout
            LinearLayout layoutImageButtons = new LinearLayout(context);
            layoutImageButtons.setOrientation(LinearLayout.HORIZONTAL);
            layoutImageButtons.setLayoutParams(horizontalLayoutParams);

            //Create the image component
            int pixel = DashboardActivity.DPtoPixel(150, scale);
            imageView = new ImageView(context);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(pixel, pixel);
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            Picasso.with(context)
                    .load(Image)
                    .into(imageView);

            layoutImageButtons.addView(imageView);
            layoutImageButtons.addView(layoutButtons);

            //Create vertical layout for all the components of each Point of Interest
            LinearLayout layoutAttraction = new LinearLayout(context);
            layoutAttraction.setOrientation(LinearLayout.VERTICAL);

            //Create a space for the layout
            Space space = new Space(context);
            pixel = DashboardActivity.DPtoPixel(60, scale);
            LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pixel, 0);
            space.setLayoutParams(spaceParams);

            //Add the space and the 2 horizontal layout components on the vertical layout
            layoutAttraction.addView(space);
            layoutAttraction.addView(layoutImageButtons);
            layoutAttraction.addView(layoutText);

            return layoutAttraction;

        }

        //OnClick function for all buttons created that sends the data retrieved from firebase to next activity
        private void MoveToPOIActivity(Context context){
            Intent intent = new Intent(context,PointOfInterestActivity.class);
            intent.putExtra("Key",key);
            intent.putExtra("Title",getTitle());
            intent.putExtra("Info",GetInfoBasedOnLocale());
            intent.putExtra("Image",getImage());
            intent.putExtra("Longitude",getLongitude());
            intent.putExtra("Latitude",getLatitude());
            context.startActivity(intent);
        }

        //OnClick function for all buttons created that removes the favorite from the sqlite database "Favorites" based on the id
        private void RemoveFavorite(SQLiteDatabase sqLiteDatabase, String id){
            String deleteSQL = "DELETE FROM \"Favorites\" WHERE \"id\" = ?;";
            sqLiteDatabase.execSQL(deleteSQL,new Object[]{id});
        }

        //Function to calculate and set the distance in Km from each attraction
        public void CalculateDistance(Location currLocation){
            Location attractionLocation = new Location("attractionLocation");
            attractionLocation.setLatitude(Latitude);
            attractionLocation.setLongitude(Longitude);
            // calc new distance
            if(distanceTextView != null){
                // calculate & beautify distance number to string format using java.text.DecimalFormat
                String distanceString = "";
                distance = currLocation.distanceTo(attractionLocation);
                DecimalFormat df = new DecimalFormat("#.##");
                if(distance >= 1000){
                    distanceString = df.format(distance/1000) + " Km";
                }
                else{
                    distanceString = df.format(distance) + " m";
                }
                distanceTextView.setText(distanceString);
            }
        }

        //Function to get different info text based on locality selected by the user (supported locales: English, German, Greek)
        public String GetInfoBasedOnLocale(){
            if(Locale.getDefault().getLanguage().equals("en")){return getInfo_en();}
            else if (Locale.getDefault().getLanguage().equals("de")){return  getInfo_de();}
            else if(Locale.getDefault().getLanguage().equals("el")){return  getInfo_gr();}
            else{return getInfo_en();}
        }
    }


    //onCreate Function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_selection);

        dynamicLayout = findViewById(R.id.dynamicContainer);

        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        currentLocation = new Location("currentLocation");

        RequestPermission();
        ReadDataFromFirebase();
        CreateDBSQL();

        // Create voice commands
        voiceCommands = CreateVoiceCommands();
    }

    //Function to update the current location and calculate the distance to each attraction
    private void ManageLocationUpdates(Location location) {
        currentLongitude = location.getLongitude();
        currentLatitude = location.getLatitude();
        currentLocation.setLongitude(currentLongitude);
        currentLocation.setLatitude(currentLatitude);

        for (Attraction attraction:attractionList) {
            attraction.CalculateDistance(currentLocation);
        }

        Collections.sort(attractionList, new Comparator<Attraction>() {
            @Override
            public int compare(Attraction o1, Attraction o2) {
                return (int)(o1.distance - o2.distance);
            }
        });

        dynamicLayout.removeAllViews();

        for(Attraction attraction:attractionList){
            dynamicLayout.addView(attraction.myCLayout);
        }

    }
    //Function that executed the ManageLocationUpdates function each time the location of the user changes
    @Override
    public void onLocationChanged(@NonNull Location location) {
        ManageLocationUpdates(location);
    }
    //Function to request permission from the user if not given and to enable the gps location update
    private void RequestPermission(){
        //Request permission if not given
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},100);
        }
        //If permission is given, enable gps location update
        else{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        }
    }
    //Function that receives the RequestPermission() result
    //and if permission is given executes the RequestPermission() again in order to enable the gps location update
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                RequestPermission();
            }
            else{
                Toast.makeText(this, getString(R.string.toast_location_permission_denied),Toast.LENGTH_LONG).show();
            }
        }
    }

    //Function that shows a message based on a request and result code
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 20){
            if (resultCode == 10){
                Toast.makeText(this, getString(R.string.toast_no_favorites),Toast.LENGTH_LONG).show();
            }
        }
        // VOICE COMMANDS
        else if(requestCode == VOICE_REC_RESULT && resultCode == RESULT_OK){
            ArrayList<String> strings = data.getStringArrayListExtra((RecognizerIntent.EXTRA_RESULTS));
            StringBuilder builder = new StringBuilder();
            for(String s : strings){
                builder.append(s).append("\n");
            }
            String speechToText = builder.toString().toLowerCase();

            switch (speechToText.trim()){
                case "back":
                    finish();
                    break;
                case "exit":
                    System.exit(0);
                    break;
                case "favorites":
                    GoToFavorites();
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

    //Function to read data from firebase, add them to each attraction instance and set the layout that will be presented to the user
    private void ReadDataFromFirebase(){
        //Get the firebase reference
        firebaseDatabase = FirebaseDatabase.getInstance("https://exploringgreece-f2282-default-rtdb.firebaseio.com/");
        DatabaseReference databaseReference = firebaseDatabase.getReference("Attractions");

        //Read data from firebase, add them on a list of attractions and create the layout for each attraction
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                attractionList.clear();

                //For each child/timestamp (i.e. attraction) of the "Attractions" branch in firebase
                for(DataSnapshot ds : snapshot.getChildren()){
                    //Add each value from the database to an attraction instance
                    Attraction attraction = ds.getValue(Attraction.class);
                    //Get key of specific child (i.e. timestamp of each attraction)
                    attraction.key = ds.getKey();
                    attractionList.add(attraction);
                    dynamicLayout.addView(attraction.CreateAttractionLayout(GuestSelectionActivity.this));
                }
                //Toast.makeText(GuestSelectionActivity.this,"Data loaded",Toast.LENGTH_LONG).show();
            }
            //Function when data can't be accessed from firebase
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GuestSelectionActivity.this, getString(R.string.toast_cant_access_data),Toast.LENGTH_LONG).show();
            }
        });
    }

    //Create local database for user to store favorites
    public void CreateDBSQL(){
        db = openOrCreateDatabase("FavoritesDB",MODE_PRIVATE,null);

        //db.execSQL("DROP TABLE \"Favorites\";");
        db.execSQL("CREATE TABLE IF NOT EXISTS \"Favorites\" (\n" +
                "\t\"id\"\tTEXT,\n" +
                "\t\"title\"\tTEXT\tNOT NULL,\n" +
                "\tPRIMARY KEY(\"id\")\n" +
                ");");
    }

    //Function that creates a menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_voice_commands, menu);
        inflater.inflate(R.menu.menu_user,menu);
        return true;
    }

    //Function when the menu item is clicked
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.menuItemFavorites){
            GoToFavorites();
        }
        else if(item.getItemId() == R.id.enable_microphone){
            VoiceHandle();
        }
        else if(item.getItemId() == R.id.voice_commands){
            ShowVoiceCommandHelp();
        }
        return true;
    }

    //Function to move to to favorites activity (expecting a result) with request code 20
    public void GoToFavorites(){
        Intent intent = new Intent(this,FavoritesActivity.class);
        startActivityForResult(intent, 20);
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
        result.put("Favorites",getString(R.string.vc_favorutes));
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
}