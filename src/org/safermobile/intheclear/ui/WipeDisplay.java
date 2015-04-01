
package org.safermobile.intheclear.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.safermobile.intheclear.R;

public class WipeDisplay {
    String _displayText;
    Drawable _displayIcon, selectedIcon, unselectedIcon;

    public WipeDisplay(String displayText, boolean selected, Context c) {
        _displayText = displayText;
        selectedIcon = c.getResources().getDrawable(R.drawable.tick);
        unselectedIcon = c.getResources().getDrawable(R.drawable.cross);
        if (selected)
            _displayIcon = selectedIcon;
        else
            _displayIcon = unselectedIcon;
    }
}
