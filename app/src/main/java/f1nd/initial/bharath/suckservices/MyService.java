package f1nd.initial.bharath.suckservices;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class MyService extends Service {

    ClipboardManager clipBoard ;
    String word ;
    private static final String TAG = "F1nd_cpHandler";
    SharedPreferences prefs = null;
    ClipboardManager.OnPrimaryClipChangedListener mPrimaryClipChangedListener;
    dbHandler databaseHelper;

    //Popup components....
    private WindowManager wm;
    TextView m;
    EditText w = null;
    boolean meaningFlag = false;
    boolean searchFlag = false;
    WindowManager.LayoutParams p;
    View myView;
    ListView lv;
    JSONObject sWord;
    FloatingActionButton hide;
    LayoutInflater fac;

    static boolean isPause = false;
    static boolean isSearch = false;

    //Pie chart related constants..
    private PieChart pieChart;
    PieData pieData1 = null;
    PieDataSet dataset1 = null;
    FloatingActionButton prev,next,closePie;
    static int wordCount = 0;
    String arr[] = null;
    ArrayList<PieEntry> entries = new ArrayList<>();
    static int pieCounter = 1;
    static int remaingWords = 0;
    int[] colors = { Color.rgb(189, 47, 71), Color.rgb(228, 101, 92), Color.rgb(241, 177, 79),
                Color.rgb(161, 204, 89), Color.rgb(33, 197, 163), Color.rgb(58, 158, 173), Color.rgb(92, 101, 100),Color.rgb(10, 92, 30)};

    //int[] colors = { Color.rgb(189, 183, 107), Color.rgb(189, 183, 107), Color.rgb(189, 183, 107),
    //Color.rgb(189, 183, 107), Color.rgb(189, 183, 107), Color.rgb(189, 183, 107), Color.rgb(189, 183, 107),Color.rgb(189, 183, 107)};

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("RestartServiceReceiver", "onStartCommand");
        clipBoard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        prefs = getApplicationContext().getSharedPreferences("f1nd.initial.bharath.suck", getApplicationContext().MODE_PRIVATE);
        isPause = prefs.getBoolean("pause",false);
        isSearch = false;
        if(intent != null){
            isSearch = intent.getBooleanExtra("search",false);
        }
        mPrimaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            public void onPrimaryClipChanged() {
                if (clipBoard.hasPrimaryClip())
                {
                    // Put your paste code here
                    ClipData clipData = clipBoard.getPrimaryClip();
                    word = clipData.getItemAt(0).coerceToText(getApplicationContext()).toString();
                    word = word.trim();
                    word = word.replaceAll("\\d","");
                    word = word.replaceAll("[^a-zA-Z]"," ");
                    word = word.replaceAll("^ +| +$|( )+", " ");
                    word = word.replace("\n", "").replace("\r", "");
                    if(isPause == false && isSearch == false){
                        getMeaning(null);
                    }
                    Log.d("RestartServiceReceiver", word);
                }
            }
        };
        scheduleVerseNotificationService(getApplicationContext());
        createNotification();

        boolean isCbListenerNeeded = prefs.getBoolean("isCbListenerNeeded",false);
        Log.d("RestartServiceReceiver", "isCbListener Needed ... " + isCbListenerNeeded + "  isPause.." + isPause);
        if(!isCbListenerNeeded){
            clipBoard.addPrimaryClipChangedListener(mPrimaryClipChangedListener);
            prefs.edit().putBoolean("isCbListenerNeeded", true).commit();
            Log.d("RestartServiceReceiver", "Added cbListener ");
        }

        if(isSearch){
            String wod = prefs.getString("WOD","Hi");
            JSONObject tWOD = null;
            try {
                tWOD = new JSONObject(wod);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Iterator<String> keys = tWOD.keys();
            String str_Name=keys.next();
            getMeaning(str_Name);
        }


        return START_STICKY;
    }



    public void getMeaning(String sParsed){

        if(sParsed != null){
            word = sParsed;
        }

        int wordLength = 0;
        if (!word.isEmpty())
            wordLength = word.split(" ").length;
        //Checking of we neeed a sentence parser
        if(wordLength == 1){
            //Single word...
            displayMeaning(null);
        }else{
            //Need a parser pie...
            fac=LayoutInflater.from(MyService.this);
            myView = fac.inflate(R.layout.pie, null);
            arr = word.split(" ");
            wordCount = wordLength;
            Log.d(TAG, "ML" + wordCount);
            remaingWords = wordCount % 8;
            counter(0);
            Log.d(TAG, "AFTER" + wordCount);
            prev = (FloatingActionButton)myView.findViewById(R.id.back);
            next = (FloatingActionButton)myView.findViewById(R.id.next);
            if(wordCount > 8){
                Log.d(TAG, "VISIBLEEEE" + wordCount);
                next.setVisibility(View.VISIBLE);
                prev.setVisibility(View.VISIBLE);
            }else{
                next.setVisibility(View.INVISIBLE);
                prev.setVisibility(View.INVISIBLE);
            }
            pieChart = (PieChart) myView.findViewById(R.id.platinum);
            pieChart.setCenterText("Click word for meaning");
            dataset1 = new PieDataSet(entries, "Click word for meaning");
            pieData1 = new PieData(dataset1);
            pieChart.setData(pieData1);
            setPieListeners();
            wm=(WindowManager)getSystemService(WINDOW_SERVICE);
            p=new WindowManager.LayoutParams(1000,1000, WindowManager.LayoutParams.TYPE_PHONE,  WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSPARENT);
            wm.addView(myView, p);
            animatePieChart();
        }
    }

    public void scheduleVerseNotificationService(Context mContext) {

        boolean isAlarmNeeded = prefs.getBoolean("pause",false);
        if (isAlarmNeeded) {

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(mContext,NotificationListener.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("wordOfTheDay",true);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            // reset previous pending intent
            alarmManager.cancel(pendingIntent);
            prefs.edit().putBoolean("isAlarmNeeded", true).commit();
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),5*60*1000, pendingIntent);

        }


    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void createNotification() {
        Notification.Builder notif;
        NotificationManager nm = null;
        notif = new Notification.Builder(getApplicationContext());
        notif.setSmallIcon(R.drawable.back_dialog);
        notif.setOngoing(true);


        String wod = prefs.getString("WOD","Hi");
        JSONObject tWOD = null;
        try {
            tWOD = new JSONObject(wod);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Iterator<String> keys = tWOD.keys();
        String str_Name=keys.next();
        String value = tWOD.optString(str_Name);
        value = value.replaceAll("^ +| +$|( )+", " ");
        value = value.replace("\n", "").replace("\r", "");
        notif.setContentTitle(str_Name);
        notif.setContentText("Expand to view meaning");
        Notification.Style style = new Notification.BigTextStyle().bigText(value);
        notif.setStyle(style);


        Uri path = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notif.setSound(path);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


        Intent yesReceive = new Intent();
        yesReceive.setAction("Pause");
        PendingIntent pendingIntentYes = PendingIntent.getBroadcast(this, 12345, yesReceive, PendingIntent.FLAG_UPDATE_CURRENT);
        Log.d("RestartServiceReceiver", "" + prefs.getBoolean("pause",false) + "    " + isPause);
        if(isPause == false){
            notif.addAction(0, "Pause", pendingIntentYes);
            notif.setColor(getApplicationContext().getResources().getColor(R.color.colorPrimary));
        }else{
            notif.addAction(0, "Resume", pendingIntentYes);
        }


        Intent yesReceive2 = new Intent();
        yesReceive2.setAction("Stop");
        PendingIntent pendingIntentYes2 = PendingIntent.getBroadcast(this, 12345, yesReceive2, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.addAction(0, "Stop", pendingIntentYes2);

        Intent yesReceive3 = new Intent();
        yesReceive3.setAction("Search");
        PendingIntent pendingIntentYes3 = PendingIntent.getBroadcast(this, 12345, yesReceive3, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.addAction(0, "Search", pendingIntentYes3);

        nm.notify(4, notif.getNotification());

    }

    public void counter(int tCounter){
        for(int i = tCounter ; (i < tCounter + 8 && i < wordCount);  i++){
            entries.add(new PieEntry(1, arr[i]));
            pieCounter = i;
        }
        pieCounter += 1;
        Log.d(TAG, "" + pieCounter);
    }


    public void animatePieChart(){
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
    }



    public void setPieListeners(){


        closePie = (FloatingActionButton)myView.findViewById(R.id.close);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                entries.clear();
                int tCounter = pieCounter;
                if(tCounter == wordCount){
                    tCounter = 0;
                }
                counter(tCounter);
                dataset1 = new PieDataSet(entries, "Click word for meaning");
                pieData1 = new PieData(dataset1);
                pieChart.setData(pieData1);
                animatePieChart();
            }
        });


        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                entries.clear();
                int tCounter = pieCounter;
                //The first pie check...
                if(tCounter == 8 && (tCounter + 8 > wordCount)){
                    //Do as such...
                }else if(tCounter == 8 && (tCounter + 8 < wordCount)){
                    tCounter = wordCount - (wordCount % 8);
                }else if(tCounter == wordCount){
                    if(tCounter - 8 < 8){
                        tCounter = 0;
                    }else if(tCounter % 8 != 0){
                        tCounter = tCounter - (tCounter % 8) - 8;
                    }
                }else{
                    tCounter -= 16;
                }
                counter(tCounter);
                dataset1 = new PieDataSet(entries, "Click word for meaning");
                pieData1 = new PieData(dataset1);
                pieChart.setData(pieData1);
                animatePieChart();
            }
        });

        closePie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wm.removeView(myView);
                myView = fac.inflate(R.layout.popup, null);
                arr= null;
                entries.clear();
            }
        });

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                String pieWord = null;
                Log.d(TAG, "pieWORD " + pieCounter + " " + (int)h.getX() + " " + (pieCounter - 8 + (int)h.getX()) );
                int minusFactor = 8;
                if(pieCounter == wordCount && wordCount % 8 != 0){
                    minusFactor = wordCount % 8;
                }
                if(pieCounter > 8){
                    pieWord = arr[pieCounter - minusFactor + (int)h.getX()];
                }else{
                    pieWord = arr[(int)h.getX()];
                }
                Log.d(TAG, "pieWORD " + pieWord);
                wm.removeView(myView);
                myView = fac.inflate(R.layout.popup, null);
                arr= null;
                entries.clear();
                //searchFlag = false;
                displayMeaning(pieWord);
            }

            @Override
            public void onNothingSelected() {

            }
        });
    }

    public void setListeners(){

        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "HIDINBGGGGGGGGGGGGGG" );
                lv.setVisibility(View.VISIBLE);
                m.setVisibility(View.INVISIBLE);
                w.setText("");
                meaningFlag = false;
                w.setEnabled(true);
                hide.setVisibility(View.INVISIBLE);
            }
        });

        w.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(meaningFlag == false){
                    sWord = databaseHelper.search(w.getText().toString());
                    final ArrayList<String> words = new ArrayList<String>();
                    Iterator<String> iter = sWord.keys();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        words.add(key);
                        try {
                            Object value = sWord.get(key);
                        } catch (JSONException e) {
                            // Something went wrong!
                        }
                    }
                    ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(MyService.this, android.R.layout.simple_expandable_list_item_1, words);
                    lv.setAdapter(mArrayAdapter);
                }
            }

        });

        myView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                wm.removeView(myView);
                return false;
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                try {
                    String mmmmMeaning = sWord.getString(lv.getItemAtPosition(i).toString());
                    mmmmMeaning = mmmmMeaning.replaceAll("^ +| +$|( )+", " ");
                    mmmmMeaning = mmmmMeaning.replace("\n", "").replace("\r", "");
                    m.setText(mmmmMeaning);
                    meaningFlag = true;
                    w.setEnabled(false);
                    w.setTextSize(28);
                    w.setText(lv.getItemAtPosition(i).toString());
                    Log.d(TAG, "" + sWord.getString(lv.getItemAtPosition(i).toString()));
                    m.setVisibility(View.VISIBLE);
                    lv.setVisibility(View.INVISIBLE);
                    hide.setVisibility(View.VISIBLE);

                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        });

    }


    public void popUp(){

        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        p=new WindowManager.LayoutParams(1000,1000, WindowManager.LayoutParams.TYPE_PHONE,  WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSPARENT);
        fac= LayoutInflater.from(MyService.this);
        myView = fac.inflate(R.layout.popup, null);

        //Initialising all the components..
        w = (EditText) myView.findViewById(R.id.word);
        m = (TextView)myView.findViewById(R.id.meaning);
        hide = (FloatingActionButton)myView.findViewById(R.id.mainFAB);
        lv = (ListView)myView.findViewById(R.id.lv);

        setListeners();

        hide.setVisibility(View.VISIBLE);
        lv.setVisibility(View.INVISIBLE);

    }


    public void displayMeaning(String pie){
        if(pie != null){
            word = pie;
            Log.d(TAG, "ML" + word);
        }
        databaseHelper=new dbHandler(MyService.this);
        String meaning = databaseHelper.getMeaning(word.toUpperCase());
        if(meaning != null){
            meaning = meaning.replaceAll("^ +| +$|( )+", " ");
            meaning = meaning.replace("\n", "").replace("\r", "");
            int meaningLength = 0;
            meaningLength = meaning.split("\\s+").length;
            Log.d(TAG, "ML" + meaningLength + " , SearchFlag " + searchFlag);
            //Checking if we need a popUp of make a Toast....
            if(meaningLength < 15 && isSearch == false){
                //We can toast it...
                Log.d(TAG, "Can finish in Toast");
                Toast.makeText(getApplicationContext(),"" + meaning,Toast.LENGTH_LONG).show();
            }else{
                //Need a popUp baby.....
                Log.d(TAG, "Need a popup baby....");
                popUp();
                if(isSearch){
                    isSearch = false;
                }
                m.setText(meaning);
                m.setVisibility(View.VISIBLE);
                w.setEnabled(false);
                w.setTextSize(28);
                w.setText(word);
                wm.addView(myView, p);
            }
        }
        else{
            Log.d(TAG, "Word not found hai...");
            Toast.makeText(getApplicationContext(),"Word not found",Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onDestroy() {
        boolean needToClose = prefs.getBoolean("needToClose",false);
        Log.d("RestartServiceReceiver", "" + needToClose);
        if(needToClose){
            prefs.edit().putBoolean("needToClose", false).commit();
            prefs.edit().putBoolean("isCbListenerNeeded", false).commit();
            prefs.edit().putBoolean("pause", false).commit();
            clipBoard.removePrimaryClipChangedListener(mPrimaryClipChangedListener);
            AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getApplicationContext(),NotificationListener.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("wordOfTheDay",true);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            // reset previous pending intent
            alarmManager.cancel(pendingIntent);
            Log.d("RestartServiceReceiver", "Destroyed from notification");
            super.onDestroy();
        }else{
            clipBoard.removePrimaryClipChangedListener(mPrimaryClipChangedListener);
            sendBroadcast(new Intent("YouWillNeverKillMe"));
            Log.d("RestartServiceReceiver", "Restarted from onDestroy");

        }
    }

    public void onTaskRemoved(Intent intent){
        prefs.edit().putBoolean("isCbListenerNeeded", false).commit();
        prefs.edit().putBoolean("pause", false).commit();
        clipBoard.removePrimaryClipChangedListener(mPrimaryClipChangedListener);
        sendBroadcast(new Intent("YouWillNeverKillMe"));
        Log.d("RestartServiceReceiver", "Restarted from taskRemoved");
        super.onTaskRemoved(intent);

    }
}
