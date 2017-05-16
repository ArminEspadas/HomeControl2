package com.example.pch61m.homecontrol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by PCH61M on 16/05/2017.
 */

public class Cancel_Confirmation extends Activity{



    String id;
    Button eliminar;
    Button cancelar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.delete_confirmation);

        eliminar = (Button) findViewById(R.id.btn_delete_confirmation) ;
        cancelar = (Button) findViewById(R.id.btn_cancel_confirmation) ;

        eliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent_back = new Intent();
                setResult(RESULT_OK, intent_back);
                finish();
            }
        });

        cancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }




}
