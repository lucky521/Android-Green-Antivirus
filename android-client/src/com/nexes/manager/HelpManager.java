

package com.nexes.manager;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

public class HelpManager extends Activity implements OnClickListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.help_layout);
		
		String text = "绿色手机杀毒软件 1.0\n";
		
		TextView label = (TextView)findViewById(R.id.help_top_label);
		label.setText(text);
		
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}



}
