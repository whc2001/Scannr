package com.drobisch.partkeeprscannrapp;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * This sample performs continuous scanning, displaying the barcode and source image whenever
 * a barcode is scanned.
 */
public class ContinuousCaptureActivity extends Activity {
    private static final String TAG = ContinuousCaptureActivity.class.getSimpleName();
    private DecoratedBarcodeView barcodeView;
    private boolean isNewTag = false;
    private String  actualCode = "";
    private String mUser;
    private String mPassword;
    private String mServer;
    private Pattern mBarcodeTemplate;
    private Integer mPartPartID = -1;
    private TextView mPartNameView;
    private TextView mPartStockView;
    private TextView mPartLocationView;



    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() != null) {
                if(actualCode.equals(result.getText()))
                {
                    actualCode = result.getText();
                } else {
                    actualCode = result.getText();

                    Log.d("barcodeResult", "New tag detected");
                    isNewTag = true;
                }
            }
            if(isNewTag == true) {
                // Utils.Net.checkInternetConenction(ContinuousCaptureActivity.this);
                isNewTag = false;
                Long partId;

                Utils.View.showToast(getApplicationContext(), actualCode, Toast.LENGTH_SHORT, Gravity.TOP, 300, true);

                // Does the barcode content match the template regex?
                String capturedPartIdStr = Utils.Text.regexMatchGroup(actualCode, mBarcodeTemplate, 1);
                if(capturedPartIdStr == null) {
                    Utils.View.openMessageBox(ContinuousCaptureActivity.this, getString(R.string.error_title), getString(R.string.error_invalid_barcode_template, actualCode));
                    return;
                }

                // Can the matched part ID be converted to number?
                try {
                    partId = Long.parseLong(capturedPartIdStr);
                } catch(NumberFormatException ex) {
                    Utils.View.openMessageBox(ContinuousCaptureActivity.this, getString(R.string.error_title), getString(R.string.error_invalid_barcode_id, actualCode));
                    return;
                }

                // Anything related to barcode is correct
                updatePartInfo(partId.intValue());
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Bundle bundle = getIntent().getExtras();

        Log.d("CaptureActivity", "User:" + bundle.getString("user"));
        Log.d("CaptureActivity", "Pwd:" + bundle.getString("password"));
        Log.d("CaptureActivity", "Server:" + bundle.getString("server"));

        mUser =  bundle.getString("user");
        mServer =  bundle.getString("server");
        mPassword =  bundle.getString("password");
        mBarcodeTemplate = Pattern.compile(bundle.getString("barcode_template"));

        mPartNameView = (TextView) findViewById(R.id.partName);
        mPartLocationView = (TextView) findViewById(R.id.partLocation);
        mPartStockView = (TextView) findViewById(R.id.partStock);

        mPartNameView.setText("");
        mPartLocationView.setText("");
        mPartStockView.setText("0");
        mPartPartID = -1;

        Button mAddStockOneButton = (Button) findViewById(R.id.addStockOne_button);
        mAddStockOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addStock(1);
            }
        });

        Button mRemoveStockOneButton = (Button) findViewById(R.id.removeStockOne_button);
        mRemoveStockOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addStock(-1);
            }
        });

        Button mAddStockMultiButton = (Button) findViewById(R.id.addStockMulti_button);
        mAddStockMultiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPartPartID != -1)
                    Utils.View.showQuantityInputDialog(ContinuousCaptureActivity.this, getString(R.string.prompt_add_multiple), new Action<Integer>() {
                        @Override
                        public void run(Integer quantity) { addStock(quantity); }
                    });
            }
        });

        Button mRemoveStockMultiButton = (Button) findViewById(R.id.removeStockMulti_button);
        mRemoveStockMultiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPartPartID != -1)
                    Utils.View.showQuantityInputDialog(ContinuousCaptureActivity.this,getString(R.string.prompt_remove_multiple), new Action<Integer>() {
                        @Override
                        public void run(Integer quantity) { addStock(-quantity); }
                    });
            }
        });

        Button mSetStockButton = (Button) findViewById(R.id.setStock_button);
        mSetStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPartPartID != -1)
                    Utils.View.showQuantityInputDialog(ContinuousCaptureActivity.this,getString(R.string.prompt_set_stock_to), new Action<Integer>() {
                        @Override
                        public void run(Integer quantity) { setStock(quantity); }
                    });
            }
        });

        Button mClearStockButton = (Button) findViewById(R.id.clearStock_button);
        mClearStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPartPartID != -1)
                    Utils.View.showConfirmDialog(ContinuousCaptureActivity.this,getString(R.string.prompt_clear_stock), new Runnable() {
                        @Override
                        public void run() { setStock(0); }
                    }, new Runnable() {
                        @Override
                        public void run() { }
                    });
            }
        });


        barcodeView = (DecoratedBarcodeView) findViewById(R.id.barcode_scanner);
        barcodeView.setStatusText("");
        barcodeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualCode = "";
            }
        });
        barcodeView.decodeContinuous(callback);
    }

    private void updatePartInfo(int partID) {
        ApiPartTask task = new ApiPartTask(mUser,mPassword,mServer,partID,"","");
        task.execute((Void) null);
    }

    private void addStock(int quantity) {
        Log.d("CaptureActivity", String.format("addStock %d", quantity));
        if(mPartPartID != -1) {
            ApiPartTask task = new ApiPartTask(mUser, mPassword, mServer, mPartPartID,"addStock", String.format("quantity=%d", quantity));
            task.execute((Void) null);
        }
    }

    private void setStock(int quantity) {
        Log.d("CaptureActivity", String.format("setStock %d", quantity));
        if(mPartPartID != -1) {
            ApiPartTask task = new ApiPartTask(mUser, mPassword, mServer, mPartPartID,"setStock", String.format("quantity=%d", quantity));
            task.execute((Void) null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
    }

    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(callback);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    public class ApiPartTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUser;
        private final String mPassword;
        private final String mServer;
        private final String mJson;
        private final String mCommand;

        private String mPartName = "";
        private Integer mPartStock = 0;
        private String mPartLocation = "";
        private Integer mPartID;
        private Boolean error = false;
        private String errorString;


        ApiPartTask(String user, String password, String server, int partID, String command, String json) {
            mUser = user;
            mPassword = password;
            mServer = server;
            mPartID = partID;
            mJson = json;
            mCommand = command;

        }

        @Override
        protected Boolean doInBackground(Void... params) {
            InputStream in = null;
            HttpURLConnection httpcon = null;
            String jsonString = "";
            Message msg = Message.obtain();
            msg.what = 1;
            String Name = "";
            Pair<InputStream, HttpURLConnection> httpResult;
            boolean isQuery = (mCommand.length() == 0);
            try {
                String restURL = mServer + "/api/parts/" + mPartID.toString();
                if(!isQuery)
                    restURL += "/" + mCommand;
                httpResult = Utils.Net.doHttpConnection(restURL,mUser,mPassword,mJson,isQuery ? "GET" : "PUT");
                in = httpResult.first;
                httpcon = httpResult.second;
                /*Bundle b = new Bundle();
                b.putString("bitmap", "test");
                msg.setData(b);
                */
                int respCode = httpcon.getResponseCode();
                if(respCode == 200 && in != null) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(in));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    jsonString = total.toString();
                    in.close();
                }
                else
                {
                    error = true;
                    if(httpcon != null) {
                        switch (respCode)
                        {
                            case 401:
                                errorString = getString(R.string.error_incorrect_password_user);
                            break;
                            case 404:
                                errorString = getString(R.string.error_part_not_exists);
                            break;
                            default:
                                errorString = getString(R.string.error_http_long, respCode);
                            break;
                        }
                    }
                    else {
                        errorString = getString(R.string.error_connection_long);
                    }
                }
            }

            catch (IOException e1) {
                e1.printStackTrace();
                error = true;
                errorString = getString(R.string.error_server_connect_failed);
            }

            if(!error) {
                try {
                    JSONObject json = new JSONObject(jsonString);
                    mPartName = (String) json.get("name") + " (ID: " + mPartID.toString() + ")";
                    JSONObject jsonStorage = json.getJSONObject("storageLocation");
                    mPartLocation = (String) jsonStorage.get("name");
                    mPartStock = json.getInt("stockLevel");
                } catch (JSONException e) {
                    e.printStackTrace();
                    errorString = getString(R.string.error_unable_parse_resp, e.getMessage());
                    error = true;
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mPartNameView.setText(mPartName);
            mPartLocationView.setText(mPartLocation);
            mPartStockView.setText(mPartStock.toString());
            mPartPartID = mPartID;
            if(error == true) {
                mPartPartID = -1;
                Utils.View.openMessageBox(ContinuousCaptureActivity.this, getString(R.string.error_title), errorString);
            }
        }

        @Override
        protected void onCancelled() {

        }
    }
}
