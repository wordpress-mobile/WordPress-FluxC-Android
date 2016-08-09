package org.wordpress.android.fluxc.instaflux;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainInstafluxActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button signInBtn = (Button) findViewById(R.id.sign_in_button);
        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSignInDialog();
            }
        });
    }

    private void showSignInDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment newFragment = ThreeEditTextDialog.newInstance(new ThreeEditTextDialog.Listener() {
            @Override
            public void onClick(String username, String password, String url) {
                // sign in action
            }
        }, "Username", "Password", "XMLRPC Url");
        newFragment.show(ft, "dialog");
    }
}
