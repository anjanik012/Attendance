package com.example.devsocattendance;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PostAttendance {
    private static final String TAG = "PostAttendance";
    private String postLink;

    public PostAttendance(String postLink) {
        this.postLink = postLink;
    }

    public void makeAttendance() {
        ContactAttendaceApi attendaceApi = new ContactAttendaceApi();
        attendaceApi.execute(postLink);
    }

    private class ContactAttendaceApi extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                Log.d(TAG, "onPostExecute: Success");
            } else {
                Log.d(TAG, "onPostExecute: Failure");
            }
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            String arg = strings[0];
            try {
                URL url = new URL(arg);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                return (connection.getResponseCode() == 200);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "doInBackground: " + e.getMessage());
            }
            return null;
        }
    }

}
