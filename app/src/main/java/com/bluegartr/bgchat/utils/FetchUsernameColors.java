package com.bluegartr.bgchat.utils;

import android.os.AsyncTask;

import java.io.IOException;

/**
 * Created by Daniel on 8/25/14.
 */
public class FetchUsernameColors extends AsyncTask<Void, Void, Void> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
        try {
            ChatUtils.getOverrideColors();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
    }
}
