package org.wordpress.android.fluxc.instaflux;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class ThreeEditTextDialog extends DialogFragment {
    public interface Listener {
        void onClick(String text1, String text2, String text3);
    }

    private Listener mListener;
    public String mHint1 = "";
    public String mHint2 = "";
    public String mHint3 = "";
    private EditText mEditText1;
    private EditText mEditText2;
    private EditText mEditText3;

    public void setListener(Listener onClickListener) {
        mListener = onClickListener;
    }

    public static ThreeEditTextDialog newInstance(Listener onClickListener, String text1Hint, String text2Hint,
                                                  String text3Hint) {
        ThreeEditTextDialog fragment = new ThreeEditTextDialog();
        fragment.setListener(onClickListener);
        fragment.mHint1 = text1Hint;
        fragment.mHint2 = text2Hint;
        fragment.mHint3 = text3Hint;
        return fragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.signin_dialog, null);
        mEditText1 = (EditText) view.findViewById(R.id.text1);
        mEditText2 = (EditText) view.findViewById(R.id.text2);
        mEditText3 = (EditText) view.findViewById(R.id.text3);
        mEditText1.setHint(mHint1);
        mEditText2.setHint(mHint2);
        mEditText3.setHint(mHint3);
        builder.setView(view)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onClick(mEditText1.getText().toString(), mEditText2.getText().toString(),
                                mEditText3.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }
}
