package com.diplomskirad.celestin.diplomskiv2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.ModbusSlaveException;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.color.holo_green_dark;
import static com.diplomskirad.celestin.diplomskiv2.Utils.acsTransparentToInt;
import static com.diplomskirad.celestin.diplomskiv2.Utils.oneIntToTransparent;

public class AcsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {


    public static Context contextOfApplication;


    Button reverse;
    Button startStop;
    TextView currentSpeedReference;
    TextView currentActualSpeed;
    TextView currentActualCurrent;
    TextView currentActualPower;
    TextView connectionState;
    TextView connLatency;
    TextView connIP;
    TextView connPort;
    TextView modbusTransaction;
    SeekBar speedReference;
    ImageButton btnWarning;
    ImageButton btnFault;
    boolean isFirstRefresh = true;


    boolean isWriting = false;

    boolean isReadyToSwitchOn = false;
    boolean isReadyToRun = false;
    boolean isReadyRef;
    boolean isFaulted;
    boolean isOffTwoInactive;
    boolean isOffThreeInactive;
    boolean isSwitchOnInhibited;
    boolean isWarningActive;
    boolean isAtSetpoint;
    boolean isRemoteActive;
    boolean isAboveLimit;
    boolean isExtRunEnabled;

    boolean isConnectedToSlave = false;
    SharedPreferences sharedPreferences;
    TCPMasterConnection conn;

    ABBVariableSpeedDrive acs880Drive;

    String AcsIP;
    int AcsPort;

    Timer tm;
    TimerTask readRegs;
    Handler handler = new Handler();
    volatile ModbusTCPTransaction trans = null; //the transaction
    ReadMultipleRegistersRequest regRequest = null;
    volatile ReadMultipleRegistersResponse regResponse = null;


    //Memory offset as defined in ABB manual for FENA 11
    //communication module

    final int controlWordAdr = 0;
    final int speedRefInAdr = 51;
    final int speedRefOutAdr = 1;
    int readSpeedRef = 0;
    float connLatencyMS = 0;
    int modbusTrans = 0;

    //default control word value for stopping the motor(required for first startup)
    int starStopWriteValue = 1150;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acs_new);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        contextOfApplication = getApplicationContext();

        acs880Drive = new ABBVariableSpeedDrive();
//getting the sharedPrerefences for IP and PORT address retrieval
        sharedPreferences = getApplicationContext().getSharedPreferences(Constants.MY_PREFS, 0); // 0 - for private mode
        AcsIP = sharedPreferences.getString(Postavke.ACS_IP, Constants.DEFUALT_ACS_IP);
        AcsPort = sharedPreferences.getInt(Postavke.ACS_PORT, Constants.DEFUALT_ACS_PORT);
        connectionState = (TextView) findViewById(R.id.txt_comm_state);
        currentSpeedReference = (TextView) findViewById(R.id.txt_speed_ref_sp);
        currentActualCurrent = (TextView) findViewById(R.id.txt_act_current);
        currentActualSpeed = (TextView) findViewById(R.id.txt_act_speed);
        currentActualPower = (TextView) findViewById(R.id.txt_act_power);
        connIP = (TextView) findViewById(R.id.txt_ip_address);
        connPort = (TextView) findViewById(R.id.txt_port);
        connLatency = (TextView) findViewById(R.id.txt_comm_latency);
        modbusTransaction = (TextView) findViewById(R.id.txt_trans_id);

        btnFault = (ImageButton) findViewById(R.id.image_fault);
        btnWarning = (ImageButton) findViewById(R.id.img_warning);



        btnFault.setVisibility(View.INVISIBLE);
        btnWarning.setVisibility(View.INVISIBLE);

        startStop = (Button) findViewById(R.id.btn_start_stop);
        // startStop.setBackgroundColor(Color.parseColor("#ff2280d7"));
        startStop.setBackgroundResource(R.drawable.button_selector);
        reverse = (Button) findViewById(R.id.btn_acs_reverziranje);


        speedReference = (SeekBar) findViewById(R.id.seek_speed_reference);

        speedReference.setOnSeekBarChangeListener(this);
        speedReference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {


                //Dialog box shows on stopSeek on the seekbar. A temp value is saved
                //so the value doesnt change during the dialog box
                final int temp = seekBar.getProgress();

                final AlertDialog.Builder promjenaBrzine = new AlertDialog.Builder(AcsActivity.this);

                promjenaBrzine.setTitle("Promjena brzine");

                //seekbar max is at 20000 so it is needed to divide by 200 to make the percentage from
                //the max speed
                promjenaBrzine.setMessage("Postaviti brzinu na " + temp / 200 + "%");

                promjenaBrzine.setPositiveButton("Potvrda", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        currentSpeedReference.setText(temp / 200 + "%");
                        if (speedRefOutAdr < 0) {
                            writeToACS(acsTransparentToInt((temp * (-1))), speedRefOutAdr);

                        } else {
                            writeToACS(temp, speedRefOutAdr);

                        }

                    }
                });

                promjenaBrzine.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                promjenaBrzine.show();
            }
        });

        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("cele", "start pressed");

                final AlertDialog.Builder stopMotor = new AlertDialog.Builder(AcsActivity.this);

                stopMotor.setTitle(startStop.getText());
                stopMotor.setMessage("Da li ste sigurni?");

                stopMotor.setPositiveButton("Potvrdi", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        writeToACS(starStopWriteValue, controlWordAdr);


                    }
                });

                stopMotor.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                stopMotor.show();
            }
        });
        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            final AlertDialog.Builder promjenaSmjera = new AlertDialog.Builder(AcsActivity.this);
            promjenaSmjera.setTitle("Reverziranje");
            promjenaSmjera.setMessage("Da li ste sigurni da zelite reverzirati smjer vrtnje?");

            promjenaSmjera.setPositiveButton("Da", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    readSpeedRef = oneIntToTransparent(regResponse.getRegisterValue(speedRefInAdr));
                    writeToACS(acsTransparentToInt((readSpeedRef * (-1))), speedRefOutAdr);
                    Log.d("cele", "Writing..." + speedRefOutAdr + "to register: " + speedRefOutAdr);

                }
            });

            promjenaSmjera.setNegativeButton("Odustani", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });

            promjenaSmjera.show();
            }
        });



        /*
        Updates the UI on a required rate (slower then the VSD data) based on local data
        Runs the updateFastGui function that hadnles all TextView and button updates

         */
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateFastGUI();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();



    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectToDevice();
            }
        }).start();
    }

    @Override
    protected void onPause() {
        Log.d("cele", "Pause disconnect.");
        closeConnection();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("cele", "Stop disconnect.");
        closeConnection();
        super.onStop();

    }

    void connectToDevice() {

        try {

            connectionState.setText("Spajanje...");
            connectionState.setTextColor(this.getResources().getColor(android.R.color.holo_green_dark));
            InetAddress address = InetAddress.getByName(AcsIP);
            conn = new TCPMasterConnection(address);
            conn.setPort(AcsPort);

            if (!conn.isConnected()) {
                Log.d("cele", "Connecting...");
                conn.connect();

            } else {
                Log.d("cele", "Already connected");
            }

            if (conn.isConnected()) {
                Log.d("cele", "Connected");

                tm = new Timer();
                readRegs = new TimerTask() {
                    @Override
                    public void run() {
                        readACSRegisters();
                    }
                };

                tm.scheduleAtFixedRate(readRegs, (long) 500, (long) 200);
                isConnectedToSlave = true;
                connectionState.setText("Spojeno");
                connectionState.setTextColor(this.getResources().getColor(android.R.color.holo_green_dark));
            }

        } catch (UnknownHostException e) {
            Log.d("cele", "No host");
            Log.d("cele", e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            Log.d("cele", "failed to Connect");
            Log.d("cele", " " + e.getLocalizedMessage());
        }
    }

    void closeConnection() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (conn != null && conn.isConnected()) {
                    tm.cancel();
                    readRegs.cancel();
                    conn.close();
                    Log.d("cele", "Connection closed to " + conn.getAddress());

                } else {
                    Log.d("cele", "Not connected");
                    return;

                }
            }
        }).start();

    }

    void readACSRegisters() {

        if (isWriting) {
            return;
        }

        regRequest = new ReadMultipleRegistersRequest(0, 75);
        float sysTime = System.currentTimeMillis();
        trans = new ModbusTCPTransaction(conn);
        trans.setRequest(regRequest);
        modbusTrans = trans.getTransactionID();

        try {

            trans.execute();


            connLatencyMS = System.currentTimeMillis() - sysTime;


        } catch (ModbusIOException e) {
            Log.d("cele", "IO error");
            closeConnection();

            e.printStackTrace();
        } catch (ModbusSlaveException e) {
            closeConnection();
            Log.d("cele", "Slave returned exception");
            e.printStackTrace();
        } catch (ModbusException e) {
            Log.d("cele", "Failed to execute request");
            closeConnection();
            e.printStackTrace();

        } catch (NullPointerException e) {

            e.printStackTrace();
        }

        try {
            if (trans.getResponse() instanceof WriteSingleRegisterResponse) {

                Log.d("cele", " response is write");
            }
            regResponse = (ReadMultipleRegistersResponse) trans.getResponse();

        } catch (ClassCastException e) {
            trans.setRequest(regRequest);
            e.printStackTrace();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {

                refreshGUI();
            }
        });
    }


    void writeToACS(final int value, final int register) {

        Log.d("cele", "Write Fun Called");
        new Thread(new Runnable() {
            @Override
            public void run() {

                SimpleRegister sr;
                sr = new SimpleRegister(value);

                Log.d("cele", "registry created at %MW" + register + " Value to write: " + value);

                WriteSingleRegisterRequest mulitpleRequest = new WriteSingleRegisterRequest(register, sr);

                if (!(conn != null && conn.isConnected())) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(getBaseContext(), "Not connected to server", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                isWriting = true;
                trans.setRequest(mulitpleRequest);
                try {
                    trans.execute();
                    trans.getResponse();
                    Log.d("cele", "executed");
                } catch (ModbusException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                isWriting = false;
                readACSRegisters();
            }


        }
        ).start();
    }


    void refreshGUI() {

        if (regResponse != null) {

            acs880Drive.updateDriveState(regResponse);
            //  Log.d("cele", "Values refreshed");

            if (isFirstRefresh) {
                readSpeedRef = oneIntToTransparent(regResponse.getRegisterValue(speedRefInAdr));
                int tempSpeedRef;

                if (readSpeedRef >= 0) {
                    tempSpeedRef = readSpeedRef;
                } else {
                    tempSpeedRef = readSpeedRef * (-1);
                }

                speedReference.setProgress(tempSpeedRef);
                isFirstRefresh = false;
            }

            currentSpeedReference.setText(speedReference.getProgress()/200 + "%");

            currentActualCurrent.setText(String.format("%.2f A",acs880Drive.getInstantaneousCurrent()));

            currentActualSpeed.setText(String.format("%.2f 1/min",acs880Drive.getInstantaneousSpeed()));

            currentActualPower.setText(String.format("%.2f kW",acs880Drive.getInstantaneousPower()));


            isReadyToSwitchOn = acs880Drive.isReadyToPowerOn();

            isReadyToRun = acs880Drive.isReadyToRun();

            isReadyRef = acs880Drive.isRunning();

            isFaulted = acs880Drive.isFaultActive();

            isOffTwoInactive = acs880Drive.isOff2Active();

            isOffThreeInactive = acs880Drive.isOff3Active();

            isSwitchOnInhibited = acs880Drive.isSwitchOnInhibited();

            isWarningActive = acs880Drive.isWarningActive();

            isAtSetpoint = acs880Drive.isSetPointDeviationActive();

            isRemoteActive = acs880Drive.isInRemoteControl();

            isAboveLimit = acs880Drive.isSetpointLimitReachedActive();

            isExtRunEnabled = acs880Drive.isExternalControlSelected();


            if (isFaulted) {
                btnFault.setVisibility(View.VISIBLE);

            } else {
                btnFault.setVisibility(View.INVISIBLE);
            }

            if (isWarningActive) {
                btnWarning.setVisibility(View.VISIBLE);
            } else {
                btnWarning.setVisibility(View.INVISIBLE);
            }

            if (isFaulted) {

                btnFault.setVisibility(View.VISIBLE);
                startStop.setText("FAULT");
                // startStop.setBackgroundColor(Color.RED);
                startStop.setBackgroundResource(R.drawable.button_red_selector);
                startStop.setClickable(false);

            } else {

                btnFault.setVisibility(View.INVISIBLE);

                if (!isRemoteActive) {

                    startStop.setText("LOKALNO");
                    // startStop.setBackgroundColor(Color.LTGRAY);
                    startStop.setBackgroundResource(R.drawable.button_selector);
                    startStop.setClickable(false);

                } else {

                    if ((!isReadyToSwitchOn || isSwitchOnInhibited) || (isReadyToRun && !isReadyRef)) {

                        startStop.setText("PRIPREMA");
                        // startStop.setBackgroundColor(Color.DKGRAY);
                        startStop.setBackgroundResource(R.drawable.button_selector);
                        startStop.setClickable(true);
                        starStopWriteValue = 1150;
                    }

                    if (isReadyToSwitchOn && !isReadyToRun && !isReadyRef) {
                        startStop.setText("START");
                        // startStop.setBackgroundColor(Color.GREEN);
                        startStop.setBackgroundResource(R.drawable.button_green_sel);
                        startStop.setClickable(true);
                        starStopWriteValue = 1151;


                    } else {

                        if (isReadyToSwitchOn && isReadyToRun && isReadyRef) {
                            startStop.setText("STOP");
                            //startStop.setBackgroundColor(Color.RED);
                            startStop.setBackgroundResource(R.drawable.button_red_selector);
                            startStop.setClickable(true);
                            starStopWriteValue = 1150;

                        }
                    }
                }
            }

        } else {
            startStop.setText("GRESKA");
            // startStop.setBackgroundColor(Color.parseColor("#ff2280d7"));
            startStop.setBackgroundResource(R.drawable.button_selector);
            Log.d("cele", "reg emtpy");
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void updateFastGUI() {

        connLatency.setText((String.format("%.1f ms", connLatencyMS)));
        modbusTransaction.setText((String.format("%1d", modbusTrans)));

        connIP.setText(AcsIP);
        connPort.setText(Integer.toString(AcsPort));

        if (conn != null && conn.isConnected()) {
            connectionState.setText("Spojeno");
            connectionState.setTextColor(this.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            connectionState.setText("Nije spojeno");
            connectionState.setTextColor(this.getResources().getColor(android.R.color.holo_red_dark));
        }


    }

    public static Context getContextOfApplication(){
        return contextOfApplication;
    }

}