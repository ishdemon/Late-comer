package in.ishdemon.mrlate;


import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.robertlevonyan.views.customfloatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.TransitionManager;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};
    GoogleAccountCredential mCredential;
    ConstraintLayout root;
    int x_pos = -1, y_pos = -1;
    private TextView mOutputText;
    private TextView name;
    private ImageView btn_edit;
    private FloatingActionButton mCallApiButton;
    private LottieAnimationView loader;
    private LottieAnimationView ripple;
    private LottieAnimationView doneLoader;
    private String MYNAME = "";
    private List<Character> charlist = new ArrayList<>();
    private String date;
    private String day;
    private List<List<Object>> cellValue;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = getSharedPreferences("storage", MODE_PRIVATE);
        MYNAME = prefs.getString("name", "");
        if (MYNAME.equals("")) {
            ShowDialog();
        }
        root = findViewById(R.id.root);
        mCallApiButton = findViewById(R.id.fab);
        btn_edit = findViewById(R.id.btn_edit);
        name = findViewById(R.id.tv_name);
        loader = findViewById(R.id.animation_view);
        ripple = findViewById(R.id.animation_view2);
        doneLoader = findViewById(R.id.animation_view3);
        mOutputText = findViewById(R.id.tv_message);
        name.setText(MYNAME);
        for (char c = 'A'; c <= 'Z'; ++c) {
            charlist.add(c);
        }

        btn_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowDialog();
            }
        });

        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MYNAME.equals("")) {
                    mCallApiButton.setEnabled(false);
                    getResultsFromApi();
                } else ShowDialog();

            }
        });

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }


    private void ShowDialog() {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
        View mView = layoutInflaterAndroid.inflate(R.layout.userinput_dialog, null);
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(this);
        alertDialogBuilderUserInput.setView(mView);

        final EditText userInputDialogEditText = (EditText) mView.findViewById(R.id.userInputDialog);
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setNegativeButton("Send", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        // ToDo get user input here

                        if (!userInputDialogEditText.getEditableText().toString().equals("")) {
                            Log.wtf("entered", "is:" + userInputDialogEditText.getEditableText().toString());
                            SharedPreferences.Editor editor = getSharedPreferences("storage", MODE_PRIVATE).edit();
                            MYNAME = userInputDialogEditText.getEditableText().toString();
                            editor.putString("name", userInputDialogEditText.getEditableText().toString());
                            editor.apply();
                            name.setText(MYNAME);
                            mCallApiButton.setEnabled(true);
                            dialogBox.dismiss();
                        }

                    }
                });

        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.show();
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
            mCallApiButton.setEnabled(true);
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.

    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Mr.Late")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }


        private List<List<Object>> fetchSheetValues(String id, String range) throws IOException {
            ValueRange response = this.mService.spreadsheets().values()
                    .get(id, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            return values;
        }

        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         *
         * @return List of names and majors
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            String spreadsheetId = "1z9IKRI6jP7kVv_DD_k34wdpYSiJLp2qrB4fC4__-AsA";
            String range = "DEC'18!2:2";
            List<String> results = new ArrayList<String>();
            date = new SimpleDateFormat("d", Locale.getDefault()).format(new Date());
            day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());


            if (day.equals("Sunday") || day.equals("Saturday")) {
                results = null;
            } else {

                //Find Date
                List<List<Object>> values = fetchSheetValues(spreadsheetId, range);
                if (values != null) {
                    for (int i = 0; i < values.get(0).size(); i++) {
                        String num = String.valueOf(values.get(0).get(i));
                        if (date.length() == 1) {
                            if ((num.charAt(0) == date.charAt(0))) {
                                x_pos = i;
                                break;
                            }
                        } else if ((num.charAt(0) == date.charAt(0)) && (num.charAt(1) == date.charAt(1))) {
                            x_pos = i;
                            break;
                        }
                    }
                }

                //find my name
                List<List<Object>> names = fetchSheetValues(spreadsheetId, "DEC'18!A:A");
                if (names != null) {
                    for (int i = 0; i < names.size(); i++) {
                        if (names.get(i).size() != 0) {
                            String temp_name = String.valueOf(names.get(i).get(0));
                            Log.wtf("names", temp_name);
                            if (temp_name.toLowerCase().equals(MYNAME.toLowerCase())) {
                                y_pos = i;
                                break;
                            }
                        }
                    }
                    Log.wtf("Name_position", String.valueOf(y_pos));
                }


                if ((x_pos != -1) && (y_pos != -1)) {
                    String cellpos = "DEC'18!" + charlist.get(x_pos) + (y_pos + 1) + ":" + charlist.get(x_pos) + (y_pos + 1);
                    Log.wtf("position", cellpos);
                    cellValue = fetchSheetValues(spreadsheetId, cellpos);
                    if (cellValue == null) {
                        Object val = new Object();
                        String sysTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Calendar.getInstance().getTime());
                        val = sysTime;
                        List<List<Object>> data = Arrays.asList(
                                Arrays.asList(
                                        val
                                )
                                // Additional rows ...
                        );
                        ValueRange body = new ValueRange()
                                .setValues(data);
                        UpdateValuesResponse result =
                                mService.spreadsheets().values().update(spreadsheetId, cellpos, body)
                                        .setValueInputOption("RAW")
                                        .execute();
                        results.add("Great! checked-in successfully" + "\n" + "at" + " " + sysTime);
                    } else
                        results.add("Already checked-in Today" + "\n" + "at" + " " + cellValue.get(0).get(0));
                } else results = null;

            }
            return results;
        }


        @Override
        protected void onPreExecute() {
            loader.playAnimation();
            ripple.setSpeed(3f);
            ripple.playAnimation();
            mOutputText.setText("");
            TransitionManager.beginDelayedTransition(root);
            ViewGroup.LayoutParams params2 = loader.getLayoutParams();
            params2.width = 500;
            params2.height = 500;
            ripple.setRepeatMode(LottieDrawable.REVERSE);
            doneLoader.setLayoutParams(params2);
            ripple.setLayoutParams(params2);
            loader.setLayoutParams(params2);

        }

        @Override
        protected void onPostExecute(List<String> output) {
            ripple.setVisibility(View.GONE);
            ripple.cancelAnimation();
            loader.setVisibility(View.GONE);
            loader.cancelAnimation();
            doneLoader.setVisibility(View.VISIBLE);
            doneLoader.playAnimation();
            if (output == null || output.size() == 0) {
                if (y_pos == -1)
                    mOutputText.setText("Can't find your name. Try again");
                if (x_pos == -1)
                    mOutputText.append("\n Working on weekends?? Damn");

            } else {
                //output.add(0, "Data retrieved using the Google Sheets API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mCallApiButton.setEnabled(true);
            ripple.setVisibility(View.GONE);
            ripple.cancelAnimation();
            loader.setVisibility(View.GONE);
            loader.cancelAnimation();
            //mProgress.setVisibility(View.GONE);
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    Log.wtf("error", mLastError.getMessage());
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                    mCallApiButton.setEnabled(true);
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}