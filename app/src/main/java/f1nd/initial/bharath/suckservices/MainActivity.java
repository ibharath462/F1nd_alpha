package f1nd.initial.bharath.suckservices;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
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
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.shashank.sony.fancywalkthroughlib.FancyWalkthroughCard;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "F1nd_MainActivity";
    SharedPreferences prefs = null;
    static Resources res;
    static String dbPath,dbName;
    private PieChart pieChart;
    PieData pieData1 = null;
    String arr[] = {"Crafted","by","Bharath","Asokan","feedback","to","ibharath462","@gmail",".com"};
    PieDataSet dataset1 = null;
    ArrayList<PieEntry> entries = new ArrayList<>();
    Button help,ss;
    int[] colors = { Color.rgb(189, 47, 71), Color.rgb(228, 101, 92), Color.rgb(241, 177, 79),
            Color.rgb(161, 204, 89), Color.rgb(33, 197, 163), Color.rgb(58, 158, 173), Color.rgb(92, 101, 100),Color.rgb(10, 92, 30),Color.rgb(10, 50, 70)};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        help = (Button)findViewById(R.id.help);
        ss = (Button)findViewById(R.id.ss);
        pieChart = (PieChart)findViewById(R.id.platinum);

        pieChart.setCenterText("Click word for meaning");
        for(int i=0; i<arr.length; i++){
            entries.add(new PieEntry(1, arr[i]));
        }
        dataset1 = new PieDataSet(entries, "Click word for meaning");
        pieData1 = new PieData(dataset1);
        pieChart.animateY(100, Easing.EasingOption.EaseOutCirc);
        pieChart.setHoleRadius(40);
        pieChart.setDescription(null);
        pieChart.setCenterTextColor(R.color.colorPrimary);
        pieChart.setTransparentCircleRadius(50);
        Legend l = pieChart.getLegend();
        l.setEnabled(false);
        dataset1.setColors(colors);
        pieData1.setValueTextColor(Color.rgb(255,255,255));
        pieData1.setValueTextSize(16);
        dataset1.setDrawValues(false);
        pieChart.setData(pieData1);

        //Initially saving false to SharedPreferences.. Dont close the service..
        prefs = getSharedPreferences("f1nd.initial.bharath.suck", MODE_PRIVATE);
        prefs.edit().putBoolean("needToClose", false).commit();
        prefs.edit().putBoolean("pause", false).commit();
        prefs.edit().putBoolean("isAlarmNeeded", false).commit();
        prefs.edit().putBoolean("isCbListenerNeeded", false).commit();


        if(isMyServiceRunning(MyService.class)) {
            Toast.makeText(getBaseContext(), "Service is already running", Toast.LENGTH_SHORT).show();
            ss.setText("STOP F1nd");
            ss.setBackgroundColor(Color.RED);
            ss.setTextColor(Color.WHITE);
        }
        else {
            Toast.makeText(getBaseContext(), "There is no service running, starting service..", Toast.LENGTH_SHORT).show();
            ss.setText("START F1nd");
            ss.setBackgroundColor(Color.GREEN);
            ss.setTextColor(Color.BLACK);
        }


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
            //startClipBoardHandler();
        }

        ss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isMyServiceRunning(MyService.class)) {
                    prefs.edit().putBoolean("needToClose", true).commit();
                    NotificationManager nm;
                    nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    nm.cancelAll();
                    Intent myService = new Intent(MainActivity.this, MyService.class);
                    stopService(myService);
                    Toast.makeText(getApplicationContext(), "Stopped F1nd", Toast.LENGTH_SHORT).show();
                    ss.setBackgroundColor(Color.GREEN);
                    ss.setTextColor(Color.BLACK);
                    ss.setText("start F1nd");
                }
                else {
                    ss.setBackgroundColor(Color.RED);
                    ss.setText("stop F1nd");
                    sendBroadcast(new Intent("YouWillNeverKillMe"));
                    ss.setTextColor(Color.WHITE);
                }
            }
        });

        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this,onBoarding.class);
                i.putExtra("isFromMA",true);
                startActivity(i);
                finish();
            }
        });

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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item= menu.findItem(R.id.action_settings);
        item.setVisible(false);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    public BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("com.F1nd.action.close")){
                finish();
            }
        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
