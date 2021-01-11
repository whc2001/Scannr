package com.drobisch.partkeeprscannrapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Utils {
    private static Toast lastToast;

    public static Pair<InputStream, HttpURLConnection> doHttpConnection(String urlStr, String user, String password, String json, String method) {
        InputStream in = null;
        HttpURLConnection httpConn = null;
        String restURI = urlStr; // "http://" + urlStr + "/api/parts/1/addStock";
        boolean isGet = method.toUpperCase().equals("GET");
        int resCode = -1;

        Log.d("Utils", String.format("doHttpConnection: %s %s", method, restURI));

        try {
            URL url = new URL(restURI);
            URLConnection urlConn = url.openConnection();

            if (!(urlConn instanceof HttpURLConnection)) {
                throw new IOException("URL is not an Http URL");
            }

            String userToken= user + ":" + password;
            byte[] data = userToken.getBytes("UTF-8");
            String encode = Base64.encodeToString(data,  Base64.NO_WRAP);
            httpConn = (HttpURLConnection) urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod(method);
            httpConn.setRequestProperty("Authorization", "Basic " + encode);
            httpConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");

            httpConn.setRequestProperty("Content-length", json.getBytes().length + "");
            httpConn.setDoInput(true);
            httpConn.setDoOutput(!isGet);
            httpConn.setUseCaches(false);

            if(!isGet) {
                OutputStream outputStream = httpConn.getOutputStream();
                outputStream.write(json.getBytes("UTF-8"));
                outputStream.close();
            }

            httpConn.connect();

            resCode = httpConn.getResponseCode();

            boolean redirect = false;

            if (resCode != HttpURLConnection.HTTP_OK) {
                if (resCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || resCode == HttpURLConnection.HTTP_MOVED_PERM
                        || resCode == HttpURLConnection.HTTP_SEE_OTHER)
                    redirect = true;

            }

            if (resCode / 100 == 2) {   // 2XX Success
                in = httpConn.getInputStream();
                Log.d("Utils","Successful URL-connection" + String.valueOf(resCode));
            }
            else {
                Log.d("Utils","Error URL-connection" + String.valueOf(resCode));
                return Pair.create(null,httpConn);
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return Pair.create(in,httpConn);
    }

    public static void openMessageBox(Context context, String headline, String message)
    {
        if(!((Activity)context).hasWindowFocus())
            return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message).setTitle(headline);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int id) {}});
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void showToast(Context context, String content, int length, int gravity, int yOffset, boolean forceHideLast) {
        if(forceHideLast && lastToast != null)
            lastToast.cancel();
        lastToast = Toast.makeText(context, content, length);
        lastToast.setGravity(gravity,0,yOffset);
        lastToast.show();
    }
}
