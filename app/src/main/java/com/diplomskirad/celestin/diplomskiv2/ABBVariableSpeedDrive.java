package com.diplomskirad.celestin.diplomskiv2;

import android.content.Context;
import android.content.SharedPreferences;

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
    private boolean isExternalStartRecieved;

    private float speedReferenceSetpoint;
    private float speedReferenceFeedback;
    private float instataniousSpeed;
    private float instantaniousCurrent;
    private float instantaniousPower;

    Context applicationContext = AcsActivity.getContextOfApplication();


    //Memory offset as defined in ABB manual for FENA 11
    //communication module

    final int controlWordAdr = 0;
    final int statusWordAdr = 50;
    final int speedRefInAdr = 51;
    final int speedRefOutAdr = 1;
    final int dataInOffset = 52;
    int powerInAdr;
    int currentInAdr;
    int speedEstInAdr;
    int readSpeedRef = 0;
    float currentSpeed;




    //Initialize all values from shared prefs

    public ABBVariableSpeedDrive() {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(Constants.MY_PREFS, 0);//PreferenceManager.getDefaultSharedPreferences(applicationContext);

        currentInAdr = sharedPreferences.getInt(Postavke.ACS_CURRENT_READ, Constants.DEFUALT_ACS_CURRENT_READ_ADR) + dataInOffset;
        powerInAdr = sharedPreferences.getInt(Postavke.ACS_POWER_READ, Constants.DEFUALT_ACS_POWER_READ_ADR) + dataInOffset;
        speedEstInAdr = sharedPreferences.getInt(Postavke.ACS_SPEED_EST_READ, Constants.DEFUALT_ACS_SPEED_EST_READ_ADR);

    }



    public boolean isReadyToPowerOn() {
        return isReadyToPowerOn;
    }

    public void setReadyToPowerOn(boolean readyToPowerOn) {
        isReadyToPowerOn = readyToPowerOn;
    }

    public boolean isReadyToRun() {
        return isReadyToRun;
    }

    public void setReadyToRun(boolean readyToRun) {
        isReadyToRun = readyToRun;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public boolean isInRemoteControl() {
        return isInRemoteControl;
    }

    public void setInRemoteControl(boolean inRemoteControl) {
        isInRemoteControl = inRemoteControl;
    }

    public boolean isFaultActive() {
        return isFaultActive;
    }

    public void setFaultActive(boolean faultActive) {
        isFaultActive = faultActive;
    }

    public boolean isOff2Active() {
        return isOff2Active;
    }

    public void setOff2Active(boolean off2Active) {
        isOff2Active = off2Active;
    }

    public boolean isOff3Active() {
        return isOff3Active;
    }

    public void setOff3Active(boolean off3Active) {
        isOff3Active = off3Active;
    }

    public boolean isWarningActive() {
        return isWarningActive;
    }

    public void setWarningActive(boolean warningActive) {
        isWarningActive = warningActive;
    }

    public boolean isSetPointDeviationActive() {
        return isSetPointDeviationActive;
    }

    public void setSetPointDeviationActive(boolean setPointDeviationActive) {
        isSetPointDeviationActive = setPointDeviationActive;
    }

    public boolean isSwitchOnInhibited() {
        return isSwitchOnInhibited;
    }

    public void setSwitchOnInhibited(boolean switchOnInhibited) {
        isSwitchOnInhibited = switchOnInhibited;
    }

    public boolean isSetpointLimitReachedActive() {
        return isSetpointLimitReachedActive;
    }

    public void setSetpointLimitReachedActive(boolean setpointLimitReachedActive) {
        isSetpointLimitReachedActive = setpointLimitReachedActive;
    }

    public boolean isExternalControlSelected() {
        return isExternalControlSelected;
    }

    public void setExternalControlSelected(boolean externalControlSelected) {
        isExternalControlSelected = externalControlSelected;
    }

    public boolean isExternalStartRecieved() {
        return isExternalStartRecieved;
    }

    public void setExternalStartRecieved(boolean externalStartRecieved) {
        isExternalStartRecieved = externalStartRecieved;
    }

    public float getSpeedReferenceSetpoint() {
        return speedReferenceSetpoint;
    }

    public void setSpeedReferenceSetpoint(float speedReferenceSetpoint) {
        this.speedReferenceSetpoint = speedReferenceSetpoint;
    }

    public float getSpeedReferenceFeedback() {
        return speedReferenceFeedback;
    }

    public void setSpeedReferenceFeedback(float speedReferenceFeedback) {
        this.speedReferenceFeedback = speedReferenceFeedback;
    }

    public float getInstataniousSpeed() {
        return instataniousSpeed;
    }

    public void setInstataniousSpeed(float instataniousSpeed) {
        this.instataniousSpeed = instataniousSpeed;
    }

    public float getInstantaniousCurrent() {
        return instantaniousCurrent;
    }

    public void setInstantaniousCurrent(float instantaniousCurrent) {
        this.instantaniousCurrent = instantaniousCurrent;
    }

    public float getInstantaniousPower() {
        return instantaniousPower;
    }

    public void setInstantaniousPower(float instantaniousPower) {
        this.instantaniousPower = instantaniousPower;
    }


    //Method to update all the values in the VSD object by using a register array from the modbus request

    public void updateDriveState(ReadMultipleRegistersResponse modbusResponse){


        int statusWord = modbusResponse.getRegisterValue(statusWordAdr);

        this.setInstantaniousCurrent( twoIntsToACSTransparent(
                modbusResponse.getRegisterValue(currentInAdr),
                modbusResponse.getRegisterValue(currentInAdr + 1),
                Constants.VSD_FEEDBACK_SCALING_FACTOR));

        this.setInstantaniousPower(twoIntsToACSTransparent(
                modbusResponse.getRegisterValue(powerInAdr),
                modbusResponse.getRegisterValue(powerInAdr + 1),
                Constants.VSD_FEEDBACK_SCALING_FACTOR));

        this.setInstataniousSpeed(twoIntsToACSTransparent(
                modbusResponse.getRegisterValue(speedEstInAdr + dataInOffset),
                modbusResponse.getRegisterValue(speedEstInAdr + 1 + dataInOffset),
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
