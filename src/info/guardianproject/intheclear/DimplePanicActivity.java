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
				Intent i = new Intent(this, PanicActivity.class).putExtra("auto_start_panic", true);
		        startActivity(i);
			} else {
				finish();
			}
		} else {
			finish();
		}
		
	}

	@Override
	public void createPlugin(boolean longPress, int memorySize) {
		setResult(RESULT_OK, new Intent().putExtra("data", new byte[0]));
		finish();
	}

}
