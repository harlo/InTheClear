
package info.guardianproject.intheclear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import info.guardianproject.intheclear.ITCConstants.Preference;
import info.guardianproject.utils.EndActivity;
import java.util.ArrayList;

public class PanicActivity extends Activity implements OnClickListener, OnDismissListener, PanicMessageConstants {
	PanicService panicService;
    SharedPreferences sp;
    
    boolean oneTouchPanic, panicServicebound;
    int panicState = AT_REST;
    String currentPanicStatus;
    
    ListView listView;
    TextView shoutReadout, panicProgress, countdownReadout;
    Button controlPanic, cancelCountdown, panicControl;
    
    Dialog countdown;
    CountDownTimer cd = null;

    ProgressDialog panicStatusDialog;
        
    final Messenger panicServiceMessageHandler = new Messenger(new PanicServiceMessageHandler());
   
    private ServiceConnection panicServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			panicService = ((PanicService.PanicServiceBinder) service).getService();
			initPanic();
			//Log.i(ITCConstants.Log.ITC, "panic service attached.");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			panicService = null;
			//Log.i(ITCConstants.Log.ITC, "panic service disconnected.");			
		}
    	
    };
    
    private BroadcastReceiver killReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        	Log.d(ITCConstants.Log.ITC, "ON RECEIVE (kill receiver)");
            killActivity();
        }

    };
    IntentFilter killFilter = new IntentFilter();

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.panic);

        sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        panicControl = (Button) findViewById(R.id.panicControl);
        shoutReadout = (TextView) findViewById(R.id.shoutReadout);
        listView = (ListView) findViewById(R.id.wipeItems);

        // if this is not a cell phone, then no need to show the panic message
        ShoutController sc = new ShoutController(this);
        
        if (TextUtils.isEmpty(sc.pi.getIMEI())) {
        	Log.d(ITCConstants.Log.ITC, "no IMEI, no panic!");
        	
            shoutReadout.setVisibility(View.GONE);
            TextView shoutReadoutTitle = (TextView) findViewById(R.id.shoutReadoutTitle);
            shoutReadoutTitle.setVisibility(View.GONE);
        } else {
        	Log.d(ITCConstants.Log.ITC, "ABOUT TO PANIC!");
        	
        	String panicMsg = sp.getString(ITCConstants.Preference.DEFAULT_PANIC_MSG, "");
            shoutReadout.setText("\n\n" + panicMsg + "\n\n" + sc.buildShoutData());
        }

        panicStatusDialog = new ProgressDialog(this);
        panicStatusDialog.setButton(getResources().getString(R.string.KEY_PANIC_MENU_CANCEL), 
        		new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancelPanic();
                    }
                }
        );
        
        panicStatusDialog.setMessage(currentPanicStatus);
        panicStatusDialog.setTitle(getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
        panicStatusDialog.setCancelable(false);
        
        Intent panic_service = new Intent(this, PanicService.class);
        bindService(panic_service, panicServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        final ArrayList<WipeItem> wipeTasks = new ArrayList<WipeItem>(6);
        wipeTasks.add(0,
                new WipeItem(R.string.KEY_WIPE_WIPECONTACTS, sp, Preference.DEFAULT_WIPE_CONTACTS));
        wipeTasks.add(1,
                new WipeItem(R.string.KEY_WIPE_WIPEPHOTOS, sp, Preference.DEFAULT_WIPE_PHOTOS));
        wipeTasks.add(2,
                new WipeItem(R.string.KEY_WIPE_CALLLOG, sp, Preference.DEFAULT_WIPE_CALLLOG));
        wipeTasks.add(3,
                new WipeItem(R.string.KEY_WIPE_SMS, sp, Preference.DEFAULT_WIPE_SMS));
        wipeTasks.add(4,
                new WipeItem(R.string.KEY_WIPE_CALENDAR, sp, Preference.DEFAULT_WIPE_CALENDAR));
        wipeTasks.add(5,
                new WipeItem(R.string.KEY_WIPE_SDCARD, sp, Preference.DEFAULT_WIPE_FOLDERS));

        listView.setAdapter(new WipeItemAdapter(this, wipeTasks));
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        listView.setClickable(false);

        killFilter.addAction(this.getClass().toString());
        registerReceiver(killReceiver, killFilter);
               
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        alignPreferences();
    }
    
    protected void initPanic() {
    	boolean autoStart = false;
        
        // check to see if we're starting with the dimple intent first
        if(getIntent().hasExtra(ITCConstants.Panic.AUTO_START)) {
        	autoStart = getIntent().getExtras().getBoolean(ITCConstants.Panic.AUTO_START);
        	getIntent().removeExtra(ITCConstants.Panic.AUTO_START);
        }
        
        if(autoStart || oneTouchPanic) {
        	panicControl.setText(getString(R.string.KEY_PANIC_MENU_CANCEL));
            panicControl.setOnClickListener(this);
            doPanic(autoStart);
        } else {
        	panicControl.setText(this.getResources().getString(R.string.KEY_PANIC_BTN_PANIC));
            panicControl.setOnClickListener(this);
        }
    }

    @Override
    public void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        
        if(i.getExtras() != null) {
        	for(String key : i.getExtras().keySet()) {
        		Log.d(ITCConstants.Log.ITC, "new intent bundle key: " + key);
        	}
        } else {
        	Log.d(ITCConstants.Log.ITC, "no bundle in new intent");
        }

        if (i.hasExtra("ReturnFrom") && i.getExtras().getInt("ReturnFrom", 0) == ITCConstants.Panic.RETURN) {
            // the app is being launched from the notification tray.

        }

        if (i.hasExtra("PanicCount")) {
            Log.d(ITCConstants.Log.ITC, "Panic Count at: " + i.getExtras().getInt("PanicCount", 0));
        }
    }

    @Override
    public void onPause() {
        unregisterReceiver(killReceiver);
        super.onPause();
    }
    
    @Override
    public void onDestroy() {
    	Log.d(ITCConstants.Log.ITC, "ON DESTROY!");
    	
    	if(panicService != null) {
    		try {
				panicService.shoutController.tearDownSMSReceiver();
			} catch(IllegalArgumentException e) {
				//Log.e(ITCConstants.Log.ITC, "already teared down sms? " + e.toString());
			}
    	}
    	
    	unbindService(panicServiceConnection);
    	
    	super.onDestroy();
    }

    private void alignPreferences() {
        oneTouchPanic = false;
        String recipients = sp.getString(ITCConstants.Preference.CONFIGURED_FRIENDS, "");
        if (recipients.compareTo("") == 0) {
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setMessage(getResources().getString(R.string.KEY_SHOUT_PREFSFAIL))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.KEY_OK),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    PanicActivity.this.launchPreferences();
                                }
                            });
            AlertDialog a = d.create();
            a.show();
        } else {
            oneTouchPanic = sp.getBoolean(ITCConstants.Preference.DEFAULT_ONE_TOUCH_PANIC, false);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == panicControl && panicState == AT_REST) {
            doPanic();
        } else if (v == panicControl && panicState != AT_REST) {
            cancelPanic();
        }
    }

    @Override
    public void onDismiss(DialogInterface d) {}

    public void updateProgressWindow(String message) {
        panicStatusDialog.setMessage(message);
    }

    public void killActivity() {
        Intent toKill = new Intent(PanicActivity.this, EndActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(toKill);
    }

    public void launchPreferences() {
        Intent toPrefs = new Intent(this, SettingsActivity.class);
        startActivity(toPrefs);
    }
    
    private void doPanic() {
    	doPanic(false);
    }

    private void doPanic(boolean auto_start) {
        panicState = IN_COUNTDOWN;
        panicControl.setText(getString(R.string.KEY_PANIC_MENU_CANCEL));
        
        panicStatusDialog.show();
        
        if(auto_start) {
        	startPanic();
        	return;
        }
        
        cd = new CountDownTimer(ITCConstants.Duriation.COUNTDOWN, ITCConstants.Duriation.COUNTDOWNINTERVAL) {
            int t = 5;

            @Override
            public void onFinish() {
                startPanic();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                panicStatusDialog.setMessage(getString(R.string.KEY_PANIC_COUNTDOWNMSG) + 
                		" " + t + " " + getString(R.string.KEY_SECONDS));
                
                t--;
            }

        };

        cd.start();
    }
   
    private void startPanic() {
    	// start the panic
    	try {
			Message msg = Message.obtain(null, START_PANIC);
			msg.replyTo = panicServiceMessageHandler;
			panicService.panicMessageHandler.send(msg);
		} catch(RemoteException e) {
			Log.e(ITCConstants.Log.ITC, "whoops.\n" + e.toString());
		}
        
        // kill the activity
        killActivity();
    }

    public void cancelPanic() {
        if (cd != null && panicState == IN_COUNTDOWN) {
            // if panic hasn't started, then just kill the countdown
            cd.cancel();
        }
        
        try {
			Message msg = Message.obtain(null, STOP_PANIC);
			msg.replyTo = panicServiceMessageHandler;
			panicService.panicMessageHandler.send(msg);
		} catch(RemoteException e) {
			Log.e(ITCConstants.Log.ITC, "whoops.\n" + e.toString());
		}
        
        killActivity();
        finish();
    }
    
    @Override
    public void onBackPressed() {
    	if(panicState == AT_REST) {
    		super.onBackPressed();
    	}
    }
    
    @SuppressLint("HandlerLeak")
	private class PanicServiceMessageHandler extends Handler {
    	@Override
    	public void handleMessage(Message msg) {    		
    		switch(msg.what) {
    			case WITH_UI_UPDATE:
                    updateProgressWindow(msg.getData().getString(ITCConstants.UPDATE_UI));
    				break;
    			default:
    				super.handleMessage(msg);
    		}
    	}
    }
}
