
package info.guardianproject.intheclear;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import info.guardianproject.intheclear.ITCConstants.Preference;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PanicService extends Service implements PanicMessageConstants {
    public static final int UPDATE_PROGRESS = 0;
    boolean isPanicing = false;

    private NotificationManager nm;
    private SharedPreferences prefs;
    
    final Messenger panicMessageHandler = new Messenger(new PanicMessageHandler());
    private final IBinder panicService = new PanicServiceBinder();

    TimerTask shoutTimerTask;
    Timer t = new Timer();
    Handler h = new Handler();
    Messenger panicActivityMessenger = null;
    
    Intent backToPanic;
    int panicCount = 0;

    WipeService wipeService;
    ShoutController shoutController;

    ArrayList<File> selectedFolders;
    String userDisplayName, defaultPanicMsg, configuredFriends;
    
    public class PanicServiceBinder extends Binder {
    	PanicService getService() {
    		return PanicService.this;
    	}
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		//Log.d(ITCConstants.Log.PS, "binding...");
		return panicService;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return Service.START_STICKY;
	}

    @Override
    public void onCreate() {
        super.onCreate();
        
        //Log.d(ITCConstants.Log.PS, "ON CREATE CALLED!");
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        shoutController = new ShoutController(getBaseContext());
        backToPanic = new Intent(this, PanicActivity.class);
        
        alignPreferences();
        showNotification();
    }

    private void alignPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        selectedFolders = new ArrayList<File>();
        userDisplayName = prefs.getString(ITCConstants.Preference.USER_DISPLAY_NAME, "");
        configuredFriends = prefs.getString(ITCConstants.Preference.CONFIGURED_FRIENDS, "");        
    }

    protected void startPanic() {
    	Log.i(ITCConstants.Log.PS, "START PANIC INVOKED.");
    	
    	isPanicing = true;
		
    	int shoutResult = shout();		
		if (shoutResult == ITCConstants.Results.A_OK || shoutResult == ITCConstants.Results.NOT_AVAILABLE) {
			if (wipe() == ITCConstants.Results.A_OK) {
				updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_3));
			} else {
				Log.d(ITCConstants.Log.ITC, "SOMETHING WAS WRONG WITH WIPE");
			}
		} else {
			Log.d(ITCConstants.Log.ITC, "SOMETHING WAS WRONG WITH SHOUT");
		}
    }
    
    protected void stopPanic() {
    	Log.i(ITCConstants.Log.PS, "STOP PANIC INVOKED.");
    	shoutController.tearDownSMSReceiver();
    	
    	if (isPanicing) {
            if (shoutTimerTask != null) {
                shoutTimerTask.cancel();
            }
            
            isPanicing = false;
        }
    	    	
        nm.cancel(R.string.remote_service_start_id);
        stopSelf();
    }
    
    public void updatePanicUi(String message) {    	
    	Bundle bundle = new Bundle();
    	bundle.putString(ITCConstants.UPDATE_UI, message);
    	
    	Message msg = new Message();
    	msg.what = WITH_UI_UPDATE;
    	msg.setData(bundle);
    	
    	try {
    		panicActivityMessenger.send(msg);
    	} catch(RemoteException e) {
    		Log.e(ITCConstants.Log.PS, "could not send msg: " + e.toString());
    	}
    }

    private int shout() {
        if (shoutController == null) {
            return ITCConstants.Results.NOT_AVAILABLE;
        }
        
        int result = ITCConstants.Results.FAIL;
        updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_1));

        shoutTimerTask = new TimerTask() {

            @Override
            public void run() {
                h.post(new Runnable() {

                    @Override
                    public void run() {
                        if (isPanicing) {
                            // TODO: this should actually be confirmed.
                            shoutController.sendAutoSMSShout(prefs);
                            panicCount++;
                            
                            Log.d(ITCConstants.Log.ITC, "Sending panic #" + panicCount + "...");
                        }
                    }
                });
            }

        };

        t.schedule(shoutTimerTask, 0, ITCConstants.Duriation.CONTINUED_PANIC);
        result = ITCConstants.Results.A_OK;
        return result;
    }

    private int wipe() {
        int result = ITCConstants.Results.FAIL;
        updatePanicUi(getString(R.string.KEY_PANIC_PROGRESS_2));
        new PIMWiper(
                getBaseContext(),
                prefs.getBoolean(Preference.DEFAULT_WIPE_CONTACTS, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_PHOTOS, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_CALLLOG, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_SMS, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_CALENDAR, false),
                prefs.getBoolean(Preference.DEFAULT_WIPE_FOLDERS, false)).start();
        result = ITCConstants.Results.A_OK;
        return result;
    }
	
    private void showNotification() {
        backToPanic.putExtra("PanicCount", panicCount);
        backToPanic.putExtra("ReturnFrom", ITCConstants.Panic.RETURN);
        backToPanic.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pi = PendingIntent.getActivity(this, ITCConstants.Results.RETURN_FROM_PANIC,
                backToPanic, PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this)
        	.setSmallIcon(R.drawable.ic_launcher)
        	.setContentTitle(getString(R.string.KEY_PANIC_TITLE_MAIN))
        	.setContentIntent(pi);

        nm.notify(R.string.remote_service_start_id, nBuilder.build());
    }
    
    @SuppressLint("HandlerLeak")
	class PanicMessageHandler extends Handler {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		panicActivityMessenger = msg.replyTo;
    		
    		switch(msg.what) {
    			case START_PANIC:
    				startPanic();
    				break;
    			case STOP_PANIC:
    				stopPanic();
    				break;
    			default:
    				super.handleMessage(msg);
    		}
    	}
    }
}
