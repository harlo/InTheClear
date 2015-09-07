
package info.guardianproject.intheclear;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import info.guardianproject.utils.EndActivity;

public class ShoutActivity extends Activity implements OnClickListener, OnDismissListener {
    private SharedPreferences prefs;

    int[] screen;
    TextView configuredFriends, panicMessage, countdownReadout;
    Button sendShout, cancelCountdown;
    EditText configuredFriendsText, panicMessageText;

    String recipients, panicMsg;

    Dialog countdown;
    CountDownTimer cd = null;
    int t;

    boolean keepPanicing = true;
    boolean isEditable = false;
    volatile boolean isCountingDown = false;

    ShoutController sc;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shout);

        screen = new int[] {
                getWindowManager().getDefaultDisplay().getWidth(),
                getWindowManager().getDefaultDisplay().getHeight()
        };

        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        configuredFriends = (TextView) findViewById(R.id.configuredFriends);

        configuredFriendsText = (EditText) findViewById(R.id.configuredFriendsText);

        panicMessage = (TextView) findViewById(R.id.panicMessage);

        panicMessageText = (EditText) findViewById(R.id.panicMessageText);
        panicMessageText.setHeight((int) (screen[1] * 0.25));

        sendShout = (Button) findViewById(R.id.shoutBtn);
        sendShout.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        updatePreferences();
    }

    @Override
    public void onStart() {
        super.onStart();
        alignPreferences();
    }

    private void alignPreferences() {
        panicMsg = prefs.getString(ITCConstants.Preference.DEFAULT_PANIC_MSG, "");
        panicMessageText.setText(panicMsg);

        recipients = prefs.getString(ITCConstants.Preference.CONFIGURED_FRIENDS, "");
        configuredFriendsText.setText(recipients);

        if (recipients.compareTo("") == 0) {
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setMessage(getResources().getString(R.string.KEY_SHOUT_PREFSFAIL))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.KEY_OK),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    ShoutActivity.this.launchPreferences();
                                }
                            });
            AlertDialog a = d.create();
            a.show();
        }
    }

    private void updatePreferences() {
        Editor editor = prefs.edit();
        editor.putString(ITCConstants.Preference.DEFAULT_PANIC_MSG, panicMsg);
        editor.putString(ITCConstants.Preference.CONFIGURED_FRIENDS, recipients);
        editor.apply();
    }

    public void doCountdown() {
        countdown = new Dialog(this);
        countdown.setContentView(R.layout.countdown);
        countdown.setOnDismissListener(this);

        countdownReadout = (TextView) countdown.findViewById(R.id.countdownReadout);
        cancelCountdown = (Button) countdown.findViewById(R.id.cancelCountdown);
        cancelCountdown.setText(getResources().getString(R.string.KEY_SHOUT_COUNTDOWNCANCEL));
        cancelCountdown.setOnClickListener(this);
        countdown.show();

        t = 0;
        cd = new CountDownTimer(ITCConstants.Duriation.COUNTDOWN, ITCConstants.Duriation.COUNTDOWNINTERVAL) {
            
        	@Override
            public void onFinish() {
                recipients = configuredFriendsText.getText().toString();
                sc.sendSMSShout(recipients, panicMsg);
                countdown.dismiss();
                killActivity();
            }

            @Override
            public void onTick(long countDown) {
                String secondString =
                        getString(R.string.KEY_SHOUT_COUNTDOWNMSG) + " " + (5 - t) +
                                " " + getString(R.string.KEY_SECONDS);
                countdownReadout.setText(secondString);
                Log.d(ITCConstants.Log.ITC, secondString);
                t++;
            }
        };
        cd.start();
    }

    @Override
    public void onClick(View v) {
        if (v == sendShout) {
            sc = new ShoutController(this);
            doCountdown();
        } else if (v == cancelCountdown) {
            if (cd != null)
                cd.cancel();
            countdown.dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface d) {
        cd.cancel();
        countdown.dismiss();
    }

    private void killActivity() {
    	sc.tearDownSMSReceiver();
        
    	Intent toKill = new Intent(this, EndActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(toKill);
    }

    public void launchPreferences() {
        Intent toPrefs = new Intent(this, SettingsActivity.class);
        startActivity(toPrefs);
    }
}
