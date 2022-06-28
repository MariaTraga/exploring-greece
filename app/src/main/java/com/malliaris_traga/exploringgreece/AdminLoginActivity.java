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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AdminLoginActivity extends AppCompatActivity {

    // Authentication variables
    FirebaseAuth mAuth;
    EditText emailTextView, sesameTextView;

    // VOICE COMMANDS
    public static final int VOICE_REC_RESULT = 765;
    Map<String, String> voiceCommands;

    // Function for creating the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_voice_commands, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // Function that determines which menu button is clicked and call the respective function
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);
        mAuth = FirebaseAuth.getInstance();

        emailTextView = findViewById(R.id.admin_email);
        sesameTextView = findViewById(R.id.admin_password);

        // Create voice commands
        voiceCommands = CreateVoiceCommands();
    }

    // Calls login function when login button is clicked
    public void onLoginButtonClick(View view){
        Authenticate();
    }

    // Function that authenticates user and starts the dashboard activity if authentication is successful
    private void Authenticate(){
        // Get credentials from user input
        String email = emailTextView.getText().toString();
        String openSesame = sesameTextView.getText().toString();

        // Try to authenticate with the given credentials
        try {
            mAuth.signInWithEmailAndPassword(email, openSesame).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        GoToDashboard();
                    }
                    ShowLoginMessage(task.isSuccessful());
                }
            });
        }
        catch(Exception e){
            Toast.makeText(this, getString(R.string.toast_check_credentials), Toast.LENGTH_LONG).show();
        }
    }

    // Function that starts the dashboard activity
    private void GoToDashboard(){
        Intent intent = new Intent(this, DashboardActivity.class);
        startActivity(intent);
    }

    // Function that toasts success or failure message
    private void ShowLoginMessage(boolean isLoginSuccessful){
        if(isLoginSuccessful){
            Toast.makeText(this, getString(R.string.toast_login_success), Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(this, getString(R.string.toast_login_failed), Toast.LENGTH_LONG).show();
        }
    }

    // VOICE COMMANDS
    // Function that creates voice commands
    private Map<String, String> CreateVoiceCommands(){
        Map<String, String> result = new HashMap<>();
        result.put("Login",getString(R.string.vc_login));
        result.put("Email",getString(R.string.vc_email));
        result.put("Password",getString(R.string.vc_password));
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
                case "login":
                    Authenticate();
                    break;
                case "exit":
                    System.exit(0);
                    break;
                case "back":
                    finish();
                    break;
                case "email":
                    emailTextView.requestFocus();
                    break;
                case "password":
                    sesameTextView.requestFocus();
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