
package info.guardianproject.intheclear;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import info.guardianproject.intheclear.R;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class ShoutController implements SMSTesterConstants {
    Resources res;
    PhoneInfo pi;
    MovementTracker mt;
    Handler h;
    Context c;
    SMSConfirm smsconfirm;
    PendingIntent _sentPI, _deliveredPI;
    
    public ShoutController(Context c) {
    	this(c, null);
    }

	@SuppressLint("HandlerLeak")
	public ShoutController(Context c, Handler h) {
		this.c = c;
		
		if(h == null) {
			this.h = new Handler() {
				@Override
				public void handleMessage(Message message) {
					// TODO: handle confirmation of sent text
					// perhaps broadcast this to calling activity?
				}
			};
		} else {
			this.h = h;
		}
        
        res = this.c.getResources();
        pi = new PhoneInfo(this.c);
        mt = new MovementTracker(this.c);
        smsconfirm = new SMSConfirm();
        
        c.registerReceiver(smsconfirm, new IntentFilter(SENT));
        c.registerReceiver(smsconfirm, new IntentFilter(DELIVERED));
    }
	
	public class SMSConfirm extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().compareTo(SENT) == 0) {
                if (getResultCode() != SMS_SENT) {
                    // the attempt to send has failed.
                    exitWithResult(false, SMS_SENDING, getResultCode());
                }
                
            } else if (intent.getAction().compareTo(DELIVERED) == 0) {
                if (getResultCode() != SMS_DELIVERED) {
                    // the attempt to deliver has failed.
                    exitWithResult(false, SMS_DELIVERY, getResultCode());
                } else {
                    exitWithResult(true, SMS_DELIVERY, getResultCode());
                }
                
            }
        }
    }
	
	public void sendSMS(String recipient, String messageData) {
        _sentPI = PendingIntent.getBroadcast(this.c, 0, new Intent(SENT), 0);
        _deliveredPI = PendingIntent.getBroadcast(this.c, 0, new Intent(DELIVERED), 0);

        SmsManager sms = SmsManager.getDefault();

        ArrayList<String> splitMsg = sms.divideMessage(messageData);
        for (String msg : splitMsg) {
            try {
                sms.sendTextMessage(recipient, null, msg, _sentPI, _deliveredPI);
            } catch (IllegalArgumentException e) {
                exitWithResult(false, SMS_INITIATED, SMS_INVALID_NUMBER);
            } catch (NullPointerException e) {
                exitWithResult(false, SMS_INITIATED, SMS_INVALID_NUMBER);
            }
        }
    }

    public void exitWithResult(boolean result, int process, int status) {
        Message smsStatus = new Message();
        Map<String, Integer> msg = new HashMap<String, Integer>();
        int r = 1;
        if (result != false)
            r = -1;

        msg.put("smsResult", r);
        msg.put("process", process);
        msg.put("status", status);

        smsStatus.obj = msg;
        h.sendMessage(smsStatus);
    }

    public String buildShoutMessage(String userMessage) {
        StringBuffer sbPanicMsg = new StringBuffer();
        sbPanicMsg.append(userMessage + "\n\n");
        sbPanicMsg.append(res.getString(R.string.KEY_PANIC_MSG_TIMESTAMP) + " " + new Date().toString());
        return sbPanicMsg.toString();
    }

    public String buildShoutData() {
        String timestamp = new Date().toString();
        StringBuffer sbPanicMsg = new StringBuffer();
       
        String imei = pi.getIMEI();
        if (imei.length() > 0) {
            sbPanicMsg.append("IMEI: " + imei + "\n");
        }
        
        String imsi = pi.getIMSI();
        if (imsi.length() > 0) {
            sbPanicMsg.append("IMSI: " + imsi + "\n");
        }
        
        String cell_id = pi.getCellId(); 
        if (cell_id.length() > 0) {
            sbPanicMsg.append(res.getString(R.string.KEY_PANIC_MSG_CID) + " " + cell_id + "\n");
        }
        
        String lac = pi.getLAC(); 
        if (lac.length() > 0) {
            sbPanicMsg.append(res.getString(R.string.KEY_PANIC_MSG_LAC) + " " + lac + "\n");
        }
        
        String mcc = pi.getMCC(); 
        if (mcc.length() > 0) {
            sbPanicMsg.append(res.getString(R.string.KEY_PANIC_MSG_MCC) + " " + mcc + "\n");
        }
        
        String mnc = pi.getMNC(); 
        if (mnc.length() > 0) {
            sbPanicMsg.append(res.getString(R.string.KEY_PANIC_MSG_MNC) + " " + mnc + "\n");
        }
        
        if (MovementTracker.updateLocation() != null) {
            double[] location = MovementTracker.updateLocation();
            sbPanicMsg.append(res.getString(R.string.KEY_PANIC_MSG_LATLNG) + " " + location[0]
                    + "," + location[1] + "\n");
        }

        // I lopped off the timezone and year from the timestamp because we
        // don't want to send a txt over 140 chars
        sbPanicMsg.append(res.getString(R.string.KEY_PANIC_MSG_TIMESTAMP) + " "
                + timestamp.substring(0, timestamp.length() - 8));
        
        return sbPanicMsg.toString();
    }

    public void sendAutoSMSShout(SharedPreferences sp) {
        String recipients = sp.getString("ConfiguredFriends", "");
        String userMessage = sp.getString("DefaultPanicMsg", "");

        String shoutMsg = buildShoutMessage(userMessage);
        sendSMSShout(recipients, shoutMsg);
    }
    
    public void sendSMSShout(String recipients, String shoutMsg) {
        StringTokenizer st = new StringTokenizer(recipients, ",");
        while (st.hasMoreTokens()) {
            String recipient = st.nextToken().trim();
            sendSMS(recipient, shoutMsg + "\n\n(1/2)");
            sendSMS(recipient, buildShoutData() + "\n\n(2/2)");
        }
    }

    public void tearDownSMSReceiver() {
    	try {
    		c.unregisterReceiver(smsconfirm);
    	} catch(IllegalArgumentException e) {}
    }
}
