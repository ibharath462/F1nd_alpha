package f1nd.initial.bharath.suckservices;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "F1nd_MainActivity";
    SharedPreferences prefs = null;
    static Resources res;
    static String dbPath,dbName;
    public static LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initially saving false to SharedPreferences.. Dont close the service..
        prefs = getSharedPreferences("f1nd.initial.bharath.suck", MODE_PRIVATE);
        prefs.edit().putBoolean("needToClose", false).commit();
        prefs.edit().putBoolean("pause", false).commit();
        prefs.edit().putBoolean("isAlarmNeeded", false).commit();
        prefs.edit().putBoolean("isCbListenerNeeded", false).commit();

        res = getResources();
        if(prefs.getBoolean("firstrun", true)){

            //Requesting permissions....
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

            //Initialising the WOD with Barbieee
            JSONObject wordOfTheDay = new JSONObject();
            try {
                wordOfTheDay.put("Brb","Be Right Back ;-)");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            prefs.edit().putBoolean("firstrun", false).commit();
            prefs.edit().putBoolean("pause", false).commit();
            prefs.edit().putString("WOD", wordOfTheDay.toString()).commit();

        }else{
            startClipBoardHandler();
        }



    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(),"Permisions granted :-)",Toast.LENGTH_SHORT).show();
                    getPermissions();
                    dbPath = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath()).getAbsolutePath();
                    dbName = "dict";
                    try {
                        copyDataBase();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    startClipBoardHandler();

                } else {
                }
                return;
            }
        }
    }

    public void getPermissions(){

        if(Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(MainActivity.this)) {

                try{
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1234);
                }catch (ActivityNotFoundException e){

                    Log.d("RestartServiceReceiver", "Exception" + e );
                }

            }


        }


    }

    public void startClipBoardHandler(){
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("com.F1nd.action.close");
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
        sendBroadcast(new Intent("YouWillNeverKillMe"));
    }

    static void copyDataBase() throws IOException{

        //Open your local db as the input stream
        InputStream myInput;
        myInput = res.openRawResource(R.raw.dict);
        // Path to the just created empty db
        String outFileName = dbPath + dbName;
        Log.d("RestartServiceReceiver", "" + outFileName);
        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);
        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }
        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("com.F1nd.action.close")){
                finish();
            }
        }
    };
}
