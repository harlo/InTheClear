
package info.guardianproject.intheclear;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class PhoneInfo {
    TelephonyManager tm;
    Context c;

    public PhoneInfo(Context c) {
        this.c = c;
        tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public String getMyPhoneNumber() {
        String out = "";
        
        try {
            out = tm.getLine1Number();
        } catch (NullPointerException e) {
        	Log.e(ITCConstants.Log.ITC, "error getting phone number: " + e.toString());
        }
        
        return out;
    }

    public String getOperator() {
        String out = "";
        try {
            if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE)
                out = tm.getNetworkOperator();
            return out;
        } catch (NullPointerException e) {
            return "";
        }
    }

    public String getCellId() {
        String out = "";
        try {
            if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                final GsmCellLocation gLoc = (GsmCellLocation) tm.getCellLocation();
                if (gLoc != null)
                    out = Integer.toString(gLoc.getCid());
            }
            return out;
        } catch (NullPointerException e) {
            return "";
        }
    }

    public String getLAC() {
        String out = "";
        try {
            if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                final GsmCellLocation gLoc = (GsmCellLocation) tm.getCellLocation();
                if (gLoc != null)
                    out = Integer.toString(gLoc.getLac());
            }
            return out;
        } catch (NullPointerException e) {
            return "";
        }

    }

    public String getIMSI() {
        String out = "";
        try {
            out = tm.getSubscriberId();
            return out;
        } catch (NullPointerException e) {
            return "";
        }
    }

    public String getMCC() {
        String out = "";
        try {
            out = tm.getNetworkOperator().substring(0, 3);
            return out;
        } catch (NullPointerException e) {
            return "";
        }
    }

    public String getMNC() {
        String out = "";
        try {
            out = tm.getNetworkOperator().substring(3);
            return out;
        } catch (NullPointerException e) {
            return "";
        }
    }

    public String getIMEI() {
        String out = "";
        try {
            out = tm.getDeviceId();
            return out;
        } catch (NullPointerException e) {
            return "";
        }
    }
}
