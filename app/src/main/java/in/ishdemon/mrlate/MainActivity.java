package in.ishdemon.mrlate;


import android.Manifest;
import android.accounts.AccountManager;
import android.animation.Animator;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
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
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AddConditionalFormatRuleRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.BooleanCondition;
import com.google.api.services.sheets.v4.model.BooleanRule;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.ConditionValue;
import com.google.api.services.sheets.v4.model.ConditionalFormatRule;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.robertlevonyan.views.customfloatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import de.hdodenhof.circleimageview.CircleImageView;
import picker.ugurtekbas.com.Picker.Picker;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS, PeopleServiceScopes.CONTACTS_READONLY, "https://www.googleapis.com/auth/plus.login"};
    Color orange = new Color().setRed(1f).setBlue(0f).setGreen(0.55f).setAlpha(1f);
    Color red = new Color().setRed(1f).setBlue(0f).setGreen(0f).setAlpha(1f);
    Color green = new Color().setRed(0.24f).setBlue(0.44f).setGreen(0.70f).setAlpha(1f);
    int selectedcolor = android.graphics.Color.parseColor("#3CB371");
    Color rulecolor = green;
    GoogleAccountCredential mCredential;
    ConstraintLayout root;
    int x_pos = -1, y_pos = -1;
    private TextView mOutputText;
    private TextView name;
    private CircleImageView profilepic;
    private ImageView btn_edit;
    private Picker timepicker;
    private FloatingActionButton mCallApiButton;
    private LottieAnimationView loader;
    private LottieAnimationView ripple;
    private LottieAnimationView doneLoader;
    private String MYNAME = "";
    private String MYURL = "";
    private List<Character> charlist = new ArrayList<>();
    private String date;
    private String day;
    private List<List<Object>> cellValue;
    private PeopleService peopleService;
    private CircleImageView redbtn;
    private CircleImageView greenbtn;
    private CircleImageView orangebtn;
    private String year;
    private String month;

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
        View divider = findViewById(R.id.lineView);
        redbtn = findViewById(R.id.red_btn);
        greenbtn = findViewById(R.id.green_btn);
        orangebtn = findViewById(R.id.orange_btn);
        profilepic = findViewById(R.id.imageView);
        root = findViewById(R.id.root);
        timepicker = findViewById(R.id.amPicker);
        mCallApiButton = findViewById(R.id.fab);
        btn_edit = findViewById(R.id.btn_edit);
        name = findViewById(R.id.tv_name);
        loader = findViewById(R.id.animation_view);
        doneLoader = findViewById(R.id.animation_view3);
        mOutputText = findViewById(R.id.tv_message);
        timepicker.setHourFormat(false);
        name.setText(MYNAME);
        divider.animate().scaleX(1).setStartDelay(500);
        animateButtons();
        for (char c = 'A'; c <= 'Z'; ++c) {
            charlist.add(c);
        }

        redbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedcolor = android.graphics.Color.parseColor("#FF0000");
                timepicker.setTextColor(selectedcolor);
                rulecolor = red;
            }
        });
        greenbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedcolor = android.graphics.Color.parseColor("#3CB371");
                timepicker.setTextColor(selectedcolor);
                rulecolor = green;
            }
        });
        orangebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedcolor = android.graphics.Color.parseColor("#FF8C00");
                timepicker.setTextColor(selectedcolor);
                rulecolor = orange;
            }
        });
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
        peopleService = new PeopleService.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), mCredential).build();
        MYURL = prefs.getString("avatar", "");
        if (!MYURL.equals(""))
            GlideApp.with(MainActivity.this).load(MYURL).transition(DrawableTransitionOptions.withCrossFade()).apply(new RequestOptions().placeholder(R.drawable.ic_user)).into(profilepic);
    }

    private void animateButtons() {
        orangebtn.animate().scaleX(1).scaleY(1).setStartDelay(200).setDuration(100).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                greenbtn.animate().scaleX(1).scaleY(1).setDuration(100).setStartDelay(200).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        redbtn.animate().scaleX(1).scaleY(1).setDuration(100).setStartDelay(200);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
    }

    private void animateHideButtons() {
        redbtn.animate().scaleX(0).scaleY(0).setDuration(200).setStartDelay(200).setListener(null);
        greenbtn.animate().scaleX(0).scaleY(0).setDuration(200).setStartDelay(400).setListener(null);
        orangebtn.animate().scaleX(0).scaleY(0).setDuration(200).setStartDelay(600).setListener(null);
    }


    private void ShowDialog() {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
        View mView = layoutInflaterAndroid.inflate(R.layout.userinput_dialog, null);
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(this);
        alertDialogBuilderUserInput.setView(mView);

        final EditText userInputDialogEditText = (EditText) mView.findViewById(R.id.userInputDialog);
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        // ToDo get user input here

                        if (!userInputDialogEditText.getEditableText().toString().equals("")) {
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
            //GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
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
                        new loadprofile().execute();
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

    private class loadprofile extends AsyncTask<Void, Void, Void> {

        private String url;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Person profile = peopleService.people().get("people/me")
                        .setPersonFields("coverPhotos")
                        .execute();
                SharedPreferences.Editor editor = getSharedPreferences("storage", MODE_PRIVATE).edit();
                editor.putString("avatar", profile.getCoverPhotos().get(0).getUrl()).apply();
                url = profile.getCoverPhotos().get(0).getUrl();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            GlideApp.with(MainActivity.this).load(url).into(profilepic);
        }
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

        private List<Sheet> fetchSheets(String id) throws IOException {
            Spreadsheet spreadsheet = this.mService.spreadsheets().get(id).execute();
            return spreadsheet.getSheets();
        }

        private List<List<Object>> fetchSheetValues(String id, String range) throws IOException {
            ValueRange response = this.mService.spreadsheets().values()
                    .get(id, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            return values;
        }

        private void Addrule(String spreadsheetId, int x, int y)
                throws IOException {
            List<String> results = new ArrayList<String>();
            // [START sheets_conditional_formatting]
            List<GridRange> ranges = Collections.singletonList(new GridRange()
                    .setSheetId(0)
                    .setStartRowIndex(y - 1)
                    .setEndRowIndex(y)
                    .setStartColumnIndex(x)
                    .setEndColumnIndex(x + 1)
            );
            String rule = "=EQ($" + charlist.get(x) + y + ",$" + charlist.get(x) + y + ")";
            Log.wtf("rule", rule);
            List<Request> requests = Arrays.asList(
                    new Request().setAddConditionalFormatRule(new AddConditionalFormatRuleRequest()
                            .setRule(new ConditionalFormatRule()
                                    .setRanges(ranges)
                                    .setBooleanRule(new BooleanRule()
                                            .setCondition(new BooleanCondition()
                                                    .setType("CUSTOM_FORMULA")
                                                    .setValues(Collections.singletonList(
                                                            new ConditionValue().setUserEnteredValue(rule)
                                                    ))
                                            )
                                            .setFormat(new CellFormat().setTextFormat(
                                                    new TextFormat().setForegroundColor(
                                                            rulecolor)
                                            ))
                                    )
                            )
                            .setIndex(0)
                    )
            );

            BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest()
                    .setRequests(requests);
            BatchUpdateSpreadsheetResponse result = this.mService.spreadsheets()
                    .batchUpdate(spreadsheetId, body)
                    .execute();
            results.add(String.valueOf(result.getReplies().size()));
        }

        /**
         *
         * @return List of names and Dates
         * @throws IOException
         */


        private List<String> getDataFromApi() throws IOException {
            //String spreadsheetId = "1z9IKRI6jP7kVv_DD_k34wdpYSiJLp2qrB4fC4__-AsA";
            String spreadsheetId = "1X4UFOcMr9nI1EyNlskZ6Ywn8ZeKZg5psDubicC69QVc";
            List<Sheet> sheets = fetchSheets(spreadsheetId);
            String LATEST_SHEET = sheets.get(sheets.size() - 1).getProperties().getTitle();
            String Daterange = LATEST_SHEET + "!" + "2:2";
            String Namerange = LATEST_SHEET + "!" + "A:A";
            List<String> results = new ArrayList<String>();
            date = new SimpleDateFormat("d", Locale.getDefault()).format(new Date());
            day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());

            if (day.equals("Sunday") || day.equals("Saturday")) {
                results = null;
            } else {

                //Find Date
                List<List<Object>> values = fetchSheetValues(spreadsheetId, Daterange);
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
                List<List<Object>> names = fetchSheetValues(spreadsheetId, Namerange);
                if (names != null) {
                    for (int i = 0; i < names.size(); i++) {
                        if (names.get(i).size() != 0) {
                            String temp_name = String.valueOf(names.get(i).get(0));
                            if (temp_name.toLowerCase().equals(MYNAME.toLowerCase())) {
                                y_pos = i + 1;
                                break;
                            }
                        }
                    }
                    Log.wtf("Name_position", String.valueOf(y_pos));
                }


                if ((x_pos != -1) && (y_pos != -1)) {
                    String cellpos = LATEST_SHEET + "!" + charlist.get(x_pos) + (y_pos) + ":" + charlist.get(x_pos) + (y_pos);
                    Log.wtf("position", cellpos);
                    cellValue = fetchSheetValues(spreadsheetId, cellpos);
                    if (cellValue == null) {
                        String sysTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timepicker.getTime());
                        List<List<Object>> data = Arrays.asList(
                                Arrays.asList(
                                        (Object) sysTime
                                )
                                // Additional rows ...
                        );
                        ValueRange body = new ValueRange()
                                .setValues(data);

                        mService.spreadsheets().values().update(spreadsheetId, cellpos, body)
                                .setValueInputOption("RAW")
                                .execute();
                        Addrule(spreadsheetId, x_pos, y_pos);
                        results.add("Great! checked-in successfully" + "\n" + "at" + " " + sysTime);
                    } else
                        results.add("Already checked-in Today" + "\n" + "at" + " " + cellValue.get(0).get(0));
                } else results = null;

            }
            return results;
        }


        @Override
        protected void onPreExecute() {
            animateHideButtons();
            timepicker.setClockColor(android.graphics.Color.BLACK);
            timepicker.setDialColor(android.graphics.Color.BLACK);
            loader.playAnimation();
            timepicker.disableTouch(true);
            timepicker.setTextColor(selectedcolor);
            mOutputText.setText("");

        }

        @Override
        protected void onPostExecute(List<String> output) {
            timepicker.setVisibility(View.GONE);
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
            animateButtons();
            mCallApiButton.setEnabled(true);
            timepicker.disableTouch(false);
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
                    mOutputText.setText(mLastError.getMessage());
                    mCallApiButton.setEnabled(true);
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }

    }
}