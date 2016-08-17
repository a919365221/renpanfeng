package com.stock;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by 907703 on 2016/3/20.
 */
public class ExitAskFragment extends DialogFragment implements View.OnClickListener{
    Button confirm ;
    Button cancel;
    View.OnClickListener onClickListener;
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.MyTryUseDialogFragment);
    }

    @Override
    public void onAttach(Activity activity) {
        if(activity instanceof View.OnClickListener){
            onClickListener = (View.OnClickListener)activity;
        }
        super.onAttach(activity);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.exit_ask_layout,container);
         confirm  = (Button)v.findViewById(R.id.confirm);
        cancel = (Button)v.findViewById(R.id.cancel);
        confirm.setOnClickListener(this);
        cancel.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {

        onClickListener.onClick(v);
        switch (v.getId()){
            case R.id.confirm:
                dismiss();

                break;
            case R.id.cancel:
                dismiss();

                break;
        }
    }
}
