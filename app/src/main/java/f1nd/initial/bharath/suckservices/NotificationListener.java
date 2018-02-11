package f1nd.initial.bharath.suckservices;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

public class NotificationListener extends BroadcastReceiver {

    SharedPreferences prefs = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        String action = intent.getAction();
        prefs = context.getSharedPreferences("f1nd.initial.bharath.suck", context.MODE_PRIVATE);

        boolean isWordOfDay = intent.getBooleanExtra("wordOfTheDay",false);
        if(isWordOfDay){
            Toast.makeText(context, "Word of the day changed.....", Toast.LENGTH_SHORT).show();
            Log.d("RestartServiceReceiver", "Word of the day changed.....");

            JSONObject wordOfTheDay = new JSONObject();
            final dbHandler d = new dbHandler(context);
            wordOfTheDay = d.getWordOfTheDay();
            prefs.edit().putString("WOD", wordOfTheDay.toString()).commit();
            context.sendBroadcast(new Intent("YouWillNeverKillMe"));

        }
        if ("Pause".equals(action)) {
            if(prefs.getBoolean("pause",false) == false){
                Toast.makeText(context, "Paused F1nd", Toast.LENGTH_SHORT).show();
                prefs.edit().putBoolean("pause", true).commit();
            }else{
                Toast.makeText(context, "Resumed F1nd", Toast.LENGTH_SHORT).show();
                prefs.edit().putBoolean("pause", false).commit();
            }
            context.sendBroadcast(new Intent("YouWillNeverKillMe"));
        }
        else  if ("Stop".equals(action)) {
            prefs.edit().putBoolean("needToClose", true).commit();
            NotificationManager nm;
            nm = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
            nm.cancelAll();
            Intent myService = new Intent(context, MyService.class);
            context.stopService(myService);
            Toast.makeText(context, "Stopped F1nd", Toast.LENGTH_SHORT).show();

        }
        else  if ("Search".equals(action)) {
            Toast.makeText(context, "Searched", Toast.LENGTH_SHORT).show();
            Intent myIntent = new Intent(context, MyService.class);
            myIntent.putExtra("search",true);
            context.startService(myIntent);
        }
    }
}
