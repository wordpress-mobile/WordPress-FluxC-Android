package org.wordpress.android.fluxc.instaflux;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

public class PostActivity extends AppCompatActivity {

    private EditText mTitleText;
    private EditText mContentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((InstafluxApp) getApplication()).component().inject(this);
        setContentView(R.layout.activity_post);

        mTitleText = (EditText) findViewById(R.id.edit_text_title);
        mContentText = (EditText) findViewById(R.id.edit_text_content);
        Button postButton = (Button) findViewById(R.id.button_post);
        postButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                post();
            }
        });
    }

    private void post() {
        String title = mTitleText.getText().toString();
        String content = mContentText.getText().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            ToastUtils.showToast(this, R.string.error_no_title_or_content);
            return;
        }

        AppLog.i(AppLog.T.API, "Create a new post with title: " + title + " content: " + content);
    }
}
