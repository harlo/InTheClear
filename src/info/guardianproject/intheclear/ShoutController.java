
package info.guardianproject.intheclear;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import info.guardianproject.intheclear.R;
import java.util.Date;
import java.util.StringTokenizer;

public class ShoutController {
    Resources res;
    PhoneInfo pi;
    SMSSender sms;
    MovementTracker mt;
    Handler h;

    @SuppressLint("HandlerLeak")
	public ShoutController(Context c) {
        h = new Handler() {
            @Override
            public void handleMessage(Message message) {
                // TODO: handle confirmation of sent text
                // perhaps broadcast this to calling activity?
            }
        };

        res = c.getResources();
        pi = new PhoneInfo(c);
        sms = new SMSSender(c, h);
        mt = new MovementTracker(c);
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
            sms.sendSMS(recipient, shoutMsg + "\n\n(1/2)");
            sms.sendSMS(recipient, buildShoutData() + "\n\n(2/2)");
        }
    }
}
