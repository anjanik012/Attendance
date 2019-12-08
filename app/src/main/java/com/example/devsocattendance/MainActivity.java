package com.example.devsocattendance;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    EditText name, email;
    Button verifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String loginData = getString(R.string.first_run);
        SharedPreferences sp = getSharedPreferences("Login", MODE_PRIVATE);
        if (sp.contains(loginData)) {
            Intent intent = new Intent(this, Scanner.class);
            startActivity(intent);
        }

        setContentView(R.layout.activity_main);
        name = findViewById(R.id.name_entry);
        email = findViewById(R.id.email_entry);
        verifier = findViewById(R.id.verify);
        verifier.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (name.getText().toString().length() == 0 || email.getText().toString().length() == 0) {
            Toast.makeText(MainActivity.this, "Fill up the entries!!!", Toast.LENGTH_LONG).show();
        } else {
            VerifyData verifyData = new VerifyData();
            verifyData.execute(email.getText().toString());
        }
    }

    private class VerifyData extends AsyncTask<String, Void, Boolean> {
        private static final String TAG = "VerifyData";
        private ProgressDialog dialog;

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (aBoolean) {
                SharedPreferences sharedPreferences = getSharedPreferences("Login", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("Name", name.getText().toString());
                editor.putString("email", email.getText().toString());
                editor.putString("hasEntry", "HasEntry");
                editor.apply();
                Intent intent = new Intent(MainActivity.this, Scanner.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "NOT FOUND", Toast.LENGTH_LONG).show();
                name.setText("");
                email.setText("");
            }

        }

        public VerifyData() {
            dialog = new ProgressDialog(MainActivity.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Setting up the Progress Dialog.
            dialog.setMessage("Verifying...");
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            Boolean result = false;
            String link = getString(R.string.api_link);
            String formatted_url = String.format(link, strings[0]);
            Log.d(TAG, "doInBackground: link " + formatted_url);
            try {
                URL url = new URL(formatted_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                int response = httpURLConnection.getResponseCode();
                Log.d(TAG, "doInBackground: response " + response);
                if (response == 200) {
                    result = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }


}
