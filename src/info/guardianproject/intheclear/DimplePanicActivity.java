package info.guardianproject.intheclear;

import android.content.Intent;
import android.os.Bundle;
import io.dimple.s.DimplePluginActivity;

public class DimplePanicActivity extends DimplePluginActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final Bundle extras = getIntent().getExtras();
		if(extras != null && extras.containsKey("type")) {
			if("create".equals(extras.getString("type"))) {
				createPlugin(true, extras.getInt("memory"));
			} else if("execute".equals(extras.getString("type"))) {
				launchPanic();
			} else {
				finish();
			}
		} else {
			finish();
		}
		
	}
	
	private void launchPanic() {
        Intent i = new Intent(this, PanicActivity.class);
        startActivity(i);
    }

	@Override
	public void createPlugin(boolean longPress, int memorySize) {
		setResult(RESULT_OK, new Intent().putExtra("data", new byte[0]));
		finish();
	}

}
