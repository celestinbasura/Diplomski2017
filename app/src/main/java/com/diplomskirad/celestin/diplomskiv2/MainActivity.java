package com.diplomskirad.celestin.diplomskiv2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {


    Button btnSentron;
    Button btnAcs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnAcs = (Button) findViewById(R.id.btn_acs_activity);
        btnSentron = (Button) findViewById(R.id.btn_sentron_activity);

        btnSentron.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent iinent= new Intent(MainActivity.this, SentronActivity.class);
                startActivity(iinent);
            }
        });

        btnAcs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent iinent= new Intent(MainActivity.this, AcsActivity.class);
                startActivity(iinent);
            }
        });


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {

            Intent iinent= new Intent(MainActivity.this, Postavke.class);
            startActivity(iinent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
