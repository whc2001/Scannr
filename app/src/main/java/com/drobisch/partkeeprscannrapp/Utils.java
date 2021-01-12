package com.drobisch.partkeeprscannrapp;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.SingleLineTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class Utils {
    public static class Net {
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

                String userToken = user + ":" + password;
                byte[] data = userToken.getBytes("UTF-8");
                String encode = Base64.encodeToString(data, Base64.NO_WRAP);
                httpConn = (HttpURLConnection) urlConn;
                httpConn.setAllowUserInteraction(false);
                httpConn.setInstanceFollowRedirects(true);
                httpConn.setRequestMethod(method);
                httpConn.setRequestProperty("Authorization", "Basic " + encode);
                httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                httpConn.setRequestProperty("Content-length", json.getBytes().length + "");
                httpConn.setDoInput(true);
                httpConn.setDoOutput(!isGet);
                httpConn.setUseCaches(false);

                if (!isGet) {
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
                    Log.d("Utils", "Successful URL-connection" + String.valueOf(resCode));
                } else {
                    Log.d("Utils", "Error URL-connection" + String.valueOf(resCode));
                    return Pair.create(null, httpConn);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Pair.create(in, httpConn);
        }

        public static boolean checkInternetConenction(Context context) {
            // get Connectivity Manager object to check connection
            ConnectivityManager connec =(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            // Check for network connections
            if ( connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||

                    connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                    connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                    connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED ) {
                return true;
            }else if (
                    connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                            connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED  ) {
                return false;
            }
            return false;
        }
    }

    public static class View {
        private static Toast lastToast;

        public static void openMessageBox(Context context, String headline, String message) {
            if (!((Activity) context).hasWindowFocus())
                return;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(message).setTitle(headline);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        public static void showToast(Context context, String content, int length, int gravity, int yOffset, boolean forceHideLast) {
            if (forceHideLast && lastToast != null)
                lastToast.cancel();
            lastToast = Toast.makeText(context, content, length);
            lastToast.setGravity(gravity, 0, yOffset);
            lastToast.show();
        }

        public static void showQuantityInputDialog(final Context context, String actionName, final Action<Integer> onConfirm) {
            if (!((Activity) context).hasWindowFocus())
                return;

            final InputMethodManager ime = ((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE));
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(String.format("%s: %s", actionName, context.getString(R.string.prompt_input_quantity)));

            final EditText txtQuantity = new EditText(context);
            txtQuantity.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            // Prevent text from being obscured to dots, as we used VARIATION_PASSWORD above to restrict non-numeric value.
            txtQuantity.setTransformationMethod(SingleLineTransformationMethod.getInstance());
            builder.setView(txtQuantity);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    String quantityStr = txtQuantity.getText().toString();
                    if(quantityStr.length() > 0) {
                        Integer quantity = Integer.parseInt(quantityStr);
                        onConfirm.run(quantity);
                    }
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            final AlertDialog alert = builder.create();

            // Enter key on keyboard triggers OK button
            alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                        alert.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                        return true;
                    }
                    return false;
                }
            });

            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    ime.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                }
            });

            alert.show();
            txtQuantity.requestFocus();
            ime.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }

        public static void showConfirmDialog(Context context, String text, final Runnable onConfirm, final Runnable onCancel) {
            if (!((Activity) context).hasWindowFocus())
                return;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(text);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    onConfirm.run();
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    onCancel.run();
                }
            });

            builder.show();
        }
    }
}
