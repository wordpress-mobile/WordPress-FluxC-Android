package org.wordpress.android.fluxc.example;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class SSLWarningDialog extends DialogFragment {
    public interface Listener {
        void onClick(String username, String password, String url);
    }

    private OnClickListener mListener;
    private TextView mCertifText;
    private boolean mUrlEnabled;
    private String mCertifString;

    public void setListener(OnClickListener onClickListener) {
        mListener = onClickListener;
    }

    public void setCertifString(String certifString) {
        mCertifString = certifString;
    }

    public static SSLWarningDialog newInstance(OnClickListener onClickListener, String certifString) {
        SSLWarningDialog fragment = new SSLWarningDialog();
        fragment.setListener(onClickListener);
        fragment.setCertifString(certifString);
        return fragment;
    }

    public boolean isUrlEnabled() {
        return mUrlEnabled;
    }

    public void setUrlEnabled(boolean urlEnabled) {
        mUrlEnabled = urlEnabled;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.ssl_warning_dialog, null);
        mCertifText = (TextView) view.findViewById(R.id.text_certificate);
        mCertifText.setText(mCertifString);
        builder.setView(view)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onClick(dialog, id);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }
}
