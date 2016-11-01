package org.wordpress.android.fluxc.example;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class MainExampleActivity extends Activity {
    private TextView mLogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ExampleApp) getApplication()).component().inject(this);
        setContentView(R.layout.activity_example);

        if (savedInstanceState == null) {
            MainFragment mf = new MainFragment();
            getFragmentManager().beginTransaction().add(R.id.fragment_container, mf).commit();
        }
        mLogView = (TextView) findViewById(R.id.log);
        mLogView.setMovementMethod(new ScrollingMovementMethod());
        prependToLog("I'll log stuff here.");
    }

    public void prependToLog(final String s) {
        String output = s + "\n" + mLogView.getText();
        mLogView.setText(output);
    }
}
