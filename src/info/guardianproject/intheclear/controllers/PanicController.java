
package info.guardianproject.intheclear.controllers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import info.guardianproject.intheclear.ITCConstants;
import info.guardianproject.intheclear.R;
import info.guardianproject.intheclear.apps.Panic;
import info.guardianproject.intheclear.data.PIMWiper;
import info.guardianproject.intheclear.data.PhoneInfo;
import info.guardianproject.intheclear.ui.WipeDisplay;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PanicController extends Service {
    private NotificationManager nm;
    SharedPreferences _sp;

    TimerTask shoutTimerTask, ui;
    Timer t = new Timer();
    Timer u = new Timer();
    final Handler h = new Handler();
    boolean isPanicing = false;

    Intent backToPanic;
    int panicCount = 0;

    WipeController wipeController;
    ShoutController shoutController;

    ArrayList<File> selectedFolders;
    ArrayList<WipeDisplay> wipeDisplayList;
    String userDisplayName, defaultPanicMsg, configuredFriends, panicData;
    boolean shouldWipePhotos, shouldWipeContacts, shouldWipeCallLog, shouldWipeSMS,
            shouldWipeCalendar, shouldWipeFolders;

    public class LocalBinder extends Binder {
        public PanicController getService() {
            return PanicController.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    String recentState;

    @Override
    public void onCreate() {
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!TextUtils.isEmpty(PhoneInfo.getIMEI()))
            shoutController = new ShoutController(getBaseContext());
        backToPanic = new Intent(this, Panic.class);
        alignPreferences();
        showNotification();
    }

    private void alignPreferences() {
        _sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        selectedFolders = new ArrayList<File>();
        defaultPanicMsg = _sp.getString(ITCConstants.Preference.DEFAULT_PANIC_MSG, "");
        userDisplayName = _sp.getString(ITCConstants.Preference.USER_DISPLAY_NAME, "");

        shouldWipeContacts = _sp.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_CONTACTS, false);
        shouldWipePhotos = _sp.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_PHOTOS, false);
        shouldWipeCallLog = _sp.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_CALLLOG, false);
        shouldWipeSMS = _sp.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_SMS, false);
        shouldWipeCalendar = _sp.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_CALENDAR, false);
        shouldWipeFolders = _sp.getBoolean(ITCConstants.Preference.DEFAULT_WIPE_FOLDERS, false);

        configuredFriends = _sp.getString(ITCConstants.Preference.CONFIGURED_FRIENDS, "");

        wipeDisplayList = new ArrayList<WipeDisplay>();

        wipeDisplayList.add(new WipeDisplay(getResources()
                .getString(R.string.KEY_WIPE_WIPECONTACTS), shouldWipeContacts, this));
        wipeDisplayList.add(new WipeDisplay(getResources().getString(R.string.KEY_WIPE_WIPEPHOTOS),
                shouldWipePhotos, this));
        wipeDisplayList.add(new WipeDisplay(getResources().getString(R.string.KEY_WIPE_CALLLOG),
                shouldWipeCallLog, this));
        wipeDisplayList.add(new WipeDisplay(getResources().getString(R.string.KEY_WIPE_SMS),
                shouldWipeSMS, this));
        wipeDisplayList.add(new WipeDisplay(getResources().getString(R.string.KEY_WIPE_CALENDAR),
                shouldWipeCalendar, this));
        wipeDisplayList.add(new WipeDisplay(getResources().getString(R.string.KEY_WIPE_SDCARD),
                shouldWipeFolders, this));
    }

    public String returnPanicData() {
        return panicData;
    }

    public ArrayList<WipeDisplay> returnWipeSettings() {
        return wipeDisplayList;
    }

    public void setPanicProgress(String progress) {
        recentState = progress;
    }

    public String getPanicProgress() {
        return recentState;
    }

    public void updatePanicUi(String message) {
        final Intent i = new Intent();
        i.putExtra(ITCConstants.UPDATE_UI, message);
        i.setAction(Panic.class.getName());

        ui = new TimerTask() {
            @Override
            public void run() {
                sendBroadcast(i);
            }

        };
        u.schedule(ui, 0);
    }

    private int shout() {
        if (shoutController == null)
            return ITCConstants.Results.NOT_AVAILABLE;
        int result = ITCConstants.Results.FAIL;
        setPanicProgress(getString(R.string.KEY_PANIC_PROGRESS_1));
        updatePanicUi(getPanicProgress());

        shoutTimerTask = new TimerTask() {

            @Override
            public void run() {
                h.post(new Runnable() {

                    @Override
                    public void run() {
                        if (isPanicing) {
                            // TODO: this should actually be confirmed.
                            shoutController.sendSMSShout(
                                    configuredFriends,
                                    defaultPanicMsg,
                                    ShoutController.buildShoutData(getResources())
                                    );
                            Log.d(ITCConstants.Log.ITC, "this is a shout going out...");
                            panicCount++;
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
        setPanicProgress(getString(R.string.KEY_PANIC_PROGRESS_2));
        updatePanicUi(getPanicProgress());
        new PIMWiper(
                getBaseContext(),
                shouldWipeContacts,
                shouldWipePhotos,
                shouldWipeCallLog,
                shouldWipeSMS,
                shouldWipeCalendar,
                shouldWipeFolders).start();
        result = ITCConstants.Results.A_OK;
        return result;
    }

    private void stopRunnables() {
        if (isPanicing) {
            if (shoutTimerTask != null)
                shoutTimerTask.cancel();
            isPanicing = false;
        }
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        return START_STICKY;
    }

    public void startPanic() {
        isPanicing = true;
        int shoutResult = shout();
        if (shoutResult == ITCConstants.Results.A_OK
                || shoutResult == ITCConstants.Results.NOT_AVAILABLE)
            if (wipe() == ITCConstants.Results.A_OK) {
                setPanicProgress(getString(R.string.KEY_PANIC_PROGRESS_3));
                updatePanicUi(getPanicProgress());
            } else {
                Log.d(ITCConstants.Log.ITC, "SOMETHING WAS WRONG WITH WIPE");
            }
        else {
            Log.d(ITCConstants.Log.ITC, "SOMETHING WAS WRONG WITH SHOUT");
        }
    }

    private void showNotification() {
        backToPanic.putExtra("PanicCount", panicCount);
        backToPanic.putExtra("ReturnFrom", ITCConstants.Panic.RETURN);
        backToPanic.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Notification n = new Notification(
                R.drawable.panic,
                getString(R.string.KEY_PANIC_TITLE_MAIN),
                System.currentTimeMillis()
                );

        PendingIntent pi = PendingIntent.getActivity(
                this,
                ITCConstants.Results.RETURN_FROM_PANIC,
                backToPanic,
                PendingIntent.FLAG_UPDATE_CURRENT
                );

        n.setLatestEventInfo(
                this,
                getString(R.string.KEY_PANIC_TITLE_MAIN),
                getString(R.string.KEY_PANIC_RETURN),
                pi
                );

        nm.notify(R.string.remote_service_start_id, n);
    }

    @Override
    public void onDestroy() {
        stopRunnables();
        Log.d(ITCConstants.Log.ITC, "goodbye service, bye bye!");
        nm.cancel(R.string.remote_service_start_id);
    }

    @Override
    public IBinder onBind(Intent i) {
        return binder;
    }

}
