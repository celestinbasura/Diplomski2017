package com.diplomskirad.celestin.diplomskiv2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;

import static com.diplomskirad.celestin.diplomskiv2.Utils.getBitState;
import static com.diplomskirad.celestin.diplomskiv2.Utils.twoIntsToACSTransparent;

/**
 * Created by celes on 28/01/2017.
 */

public class ABBVariableSpeedDrive {


    private boolean isReadyToPowerOn;
    private boolean isReadyToRun;
    private boolean isRunning;

    private boolean isInRemoteControl;

    private boolean isFaultActive;
    private boolean isOff2Active;
    private boolean isOff3Active;
    private boolean isWarningActive;
    private boolean isSetPointDeviationActive;
    private boolean isSwitchOnInhibited;
    private boolean isSetpointLimitReachedActive;
    private boolean isExternalControlSelected;
    private boolean isExternalStartReceived;

    private float speedReferenceSetpoint;
    private float speedReferenceFeedback;
    private float instantaneousSpeed;
    private float instantaneousCurrent;
    private float instantaneousPower;

    Context applicationContext = AcsActivity.getContextOfApplication();

    int dataInOffset = 52;

    //Memory offset as defined in ABB manual for FENA 11
    //communication module

    private final int controlWordAdr = 0;
    private final int statusWordAdr = 50;
    private final int speedRefInAdr = 51;
    private final int speedRefOutAdr = 1;
    private int powerInAdr;
    private int currentInAdr;
    private int speedEstInAdr;
    private int readSpeedRef = 0;




    //Initialize all values from shared prefs

    ABBVariableSpeedDrive() {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(Constants.MY_PREFS, 0);//PreferenceManager.getDefaultSharedPreferences(applicationContext);

        Log.d("cele", "VSD object instantiated");

        currentInAdr = sharedPreferences.getInt(Postavke.ACS_CURRENT_READ,
                Constants.DEFUALT_ACS_CURRENT_READ_ADR) + dataInOffset;
        powerInAdr = sharedPreferences.getInt(Postavke.ACS_POWER_READ,
                Constants.DEFUALT_ACS_POWER_READ_ADR) + dataInOffset;
        speedEstInAdr = sharedPreferences.getInt(Postavke.ACS_SPEED_EST_READ,
                Constants.DEFUALT_ACS_SPEED_EST_READ_ADR) + dataInOffset;

    }



    boolean isReadyToPowerOn() {
        return isReadyToPowerOn;
    }

    private void setReadyToPowerOn(boolean readyToPowerOn) {
        isReadyToPowerOn = readyToPowerOn;
    }

    boolean isReadyToRun() {
        return isReadyToRun;
    }

    private void setReadyToRun(boolean readyToRun) {
        isReadyToRun = readyToRun;
    }

    boolean isRunning() {
        return isRunning;
    }

    private void setRunning(boolean running) {
        isRunning = running;
    }

    boolean isInRemoteControl() {
        return isInRemoteControl;
    }

    private void setInRemoteControl(boolean inRemoteControl) {
        isInRemoteControl = inRemoteControl;
    }

    boolean isFaultActive() {
        return isFaultActive;
    }

    private void setFaultActive(boolean faultActive) {
        isFaultActive = faultActive;
    }

    boolean isOff2Active() {
        return isOff2Active;
    }

    private void setOff2Active(boolean off2Active) {
        isOff2Active = off2Active;
    }

    boolean isOff3Active() {
        return isOff3Active;
    }

    private void setOff3Active(boolean off3Active) {
        isOff3Active = off3Active;
    }

    boolean isWarningActive() {
        return isWarningActive;
    }

    private void setWarningActive(boolean warningActive) {
        isWarningActive = warningActive;
    }

    boolean isSetPointDeviationActive() {
        return isSetPointDeviationActive;
    }

    private void setSetPointDeviationActive(boolean setPointDeviationActive) {
        isSetPointDeviationActive = setPointDeviationActive;
    }

    boolean isSwitchOnInhibited() {
        return isSwitchOnInhibited;
    }

    private void setSwitchOnInhibited(boolean switchOnInhibited) {
        isSwitchOnInhibited = switchOnInhibited;
    }

    boolean isSetpointLimitReachedActive() {
        return isSetpointLimitReachedActive;
    }

    private void setSetpointLimitReachedActive(boolean setpointLimitReachedActive) {
        isSetpointLimitReachedActive = setpointLimitReachedActive;
    }

    boolean isExternalControlSelected() {
        return isExternalControlSelected;
    }

    private void setExternalControlSelected(boolean externalControlSelected) {
        isExternalControlSelected = externalControlSelected;
    }

    boolean isExternalStartReceived() {
        return isExternalStartReceived;
    }

    private void setExternalStartReceived(boolean externalStartReceived) {
        isExternalStartReceived = externalStartReceived;
    }

    float getSpeedReferenceSetpoint() {
        return speedReferenceSetpoint;
    }

    void setSpeedReferenceSetpoint(float speedReferenceSetpoint) {
        this.speedReferenceSetpoint = speedReferenceSetpoint;
    }

    float getSpeedReferenceFeedback() {
        return speedReferenceFeedback;
    }

    private void setSpeedReferenceFeedback(float speedReferenceFeedback) {
        this.speedReferenceFeedback = speedReferenceFeedback;
    }

    float getInstantaneousSpeed() {
        return instantaneousSpeed;
    }

    private void setInstantaneousSpeed(float instantaneousSpeed) {
        this.instantaneousSpeed = instantaneousSpeed;
    }

    float getInstantaneousCurrent() {
        return instantaneousCurrent;
    }

    private void setInstantaneousCurrent(float instantaneousCurrent) {
        this.instantaneousCurrent = instantaneousCurrent;
    }

    float getInstantaneousPower() {
        return instantaneousPower;
    }

    private void setInstantaneousPower(float instantaneousPower) {
        this.instantaneousPower = instantaneousPower;
    }


    //Method to update all the values in the VSD object by using a register array from the modbus request

    void updateDriveState(ReadMultipleRegistersResponse modbusResponse){


        int statusWord = modbusResponse.getRegisterValue(statusWordAdr);

        this.setInstantaneousCurrent( twoIntsToACSTransparent(
                modbusResponse.getRegisterValue(currentInAdr),
                modbusResponse.getRegisterValue(currentInAdr + 1),
                Constants.VSD_FEEDBACK_SCALING_FACTOR));

        this.setInstantaneousPower(twoIntsToACSTransparent(
                modbusResponse.getRegisterValue(powerInAdr),
                modbusResponse.getRegisterValue(powerInAdr + 1),
                Constants.VSD_FEEDBACK_SCALING_FACTOR));

        this.setInstantaneousSpeed(twoIntsToACSTransparent(
                modbusResponse.getRegisterValue(speedEstInAdr),
                modbusResponse.getRegisterValue(speedEstInAdr + 1),
                Constants.VSD_FEEDBACK_SCALING_FACTOR));



        //Update the status bits to be used within the class
        this.setReadyToPowerOn(getBitState(0, statusWord));
        this.setReadyToRun(getBitState(1, statusWord));
        this.setRunning(getBitState(2, statusWord));
        this.setFaultActive(getBitState(3, statusWord));
        this.setOff2Active(getBitState(4, statusWord));
        this.setOff3Active(getBitState(5, statusWord));
        this.setSwitchOnInhibited(getBitState(6, statusWord));
        this.setWarningActive(getBitState(7, statusWord));
        this.setSetPointDeviationActive(getBitState(8, statusWord));
        this.setInRemoteControl(getBitState(9, statusWord));
        this.setSetpointLimitReachedActive(getBitState(10, statusWord));
        this.setExternalControlSelected(getBitState(12, statusWord));




    }
}
