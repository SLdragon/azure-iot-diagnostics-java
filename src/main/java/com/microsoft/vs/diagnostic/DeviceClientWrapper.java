package com.microsoft.vs.diagnostic;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

/**
 * Created by zhqqi on 3/23/2017.
 */
public class DeviceClientWrapper {
    private DeviceClient deviceClient;
    private IDiagnosticProvider diagnosticProvider;
    private boolean userCalledStartTwin;
    private TwinStatusCallBack _twinStatusCallback;
    private Object _twinStatusCallbackContext;
    private TwinGenericCallBack _twinGenericCallback;
    private Object _twinGenericCallbackContext;


    protected class TwinStatusCallBack implements IotHubEventCallback{
        public IotHubEventCallback userTwinStatusCallback;
        public Object userTwinStatusCallbackContext;

        public void execute(IotHubStatusCode status, Object context){
            if(this.userTwinStatusCallback != null ) {
                this.userTwinStatusCallback.execute(status,this.userTwinStatusCallbackContext);
            }
        }
    }

    protected class TwinGenericCallBack extends Device{
        public PropertyCallBack userTwinGenericCallback;
        public Object userTwinGenericCallbackContext;

        public void PropertyCall(String propertyKey, Object propertyValue, Object context){
            if(propertyKey.equals(IDiagnosticProvider.KEY_TWIN_DIAG_SAMPLE_RATE)) {
                if(diagnosticProvider.getSamplingRateSource() == IDiagnosticProvider.SamplingRateSource.Server) {
                    try {
                        // in Java SDK, integer value will be converted to float value
                        // e.g., set a => 10, here propertyValue is 10.0
                        // so make a workaround
                        Double temp = Double.parseDouble((String) propertyValue);
                        int newVal = temp.intValue();
                        if(newVal<0 || newVal>100) {
                            throw new Exception();
                        }
                        System.out.println("Sampling rate changed to " + newVal);
                        diagnosticProvider.setSamplingRatePercentage(newVal);
                    } catch (Exception e) {
                        System.out.println("Received invalid value of sampling percentage");
                        return;
                    }
                }
            }
            else if(propertyKey.equals(IDiagnosticProvider.KEY_TWIN_DIAG_ENABLE)) {
                String val = (String) propertyValue;
                if(val.equals("true")) {
                    diagnosticProvider.setServerSamplingTurnedOn(true);
                }else if(val.equals("false")) {
                    diagnosticProvider.setServerSamplingTurnedOn(false);
                }else {
                    System.out.println("Received invalid value of sampling switch");
                    return;
                }
            }
            else if(this.userTwinGenericCallback != null ) {
                this.userTwinGenericCallback.PropertyCall(propertyKey,propertyValue,this.userTwinGenericCallbackContext);
            }
        }
    }

    public DeviceClientWrapper(String connString,IDiagnosticProvider diagnosticProvider) throws URISyntaxException {
        this.deviceClient = new DeviceClient(connString,IotHubClientProtocol.MQTT);
        this.diagnosticProvider = diagnosticProvider;
        this.userCalledStartTwin = false;
        this._twinStatusCallback = new TwinStatusCallBack();
        this._twinGenericCallback = new TwinGenericCallBack();
    }

    public void open() throws IOException {
        this.deviceClient.open();
        this.deviceClient.startDeviceTwin(this._twinStatusCallback,this._twinStatusCallbackContext,this._twinGenericCallback,this._twinGenericCallbackContext);
    }

    public void close() throws IOException {
        this.deviceClient.close();
    }

    public void setOption(String optionName, Object value) {
        this.deviceClient.setOption(optionName,value);
    }

    public void sendEventAsync(Message message,
                               IotHubEventCallback callback,
                               Object callbackContext)
    {
        message = this.diagnosticProvider.Process(message);
        this.deviceClient.sendEventAsync(message,callback,callbackContext);
    }

    public DeviceClient setMessageCallback(
            MessageCallback callback,
            Object context)
    {
        return this.deviceClient.setMessageCallback(callback,context);
    }

    public void startDeviceTwin(IotHubEventCallback deviceTwinStatusCallback, Object deviceTwinStatusCallbackContext,
                                PropertyCallBack genericPropertyCallBack, Object genericPropertyCallBackContext) throws IOException
    {
        if(this.userCalledStartTwin) {
            this.deviceClient.startDeviceTwin(deviceTwinStatusCallback, deviceTwinStatusCallbackContext, genericPropertyCallBack, genericPropertyCallBackContext);
        }else{
            this.userCalledStartTwin = true;
            this._twinStatusCallback.userTwinStatusCallback = deviceTwinStatusCallback;
            this._twinStatusCallback.userTwinStatusCallbackContext = deviceTwinStatusCallbackContext;
            this._twinGenericCallback.userTwinGenericCallback = genericPropertyCallBack;
            this._twinGenericCallback.userTwinGenericCallbackContext = genericPropertyCallBackContext;
        }
    }

    public void subscribeToDesiredProperties(Map<Property, Pair<PropertyCallBack<String, Object>, Object>> onDesiredPropertyChange) throws IOException
    {
        this.deviceClient.subscribeToDesiredProperties(onDesiredPropertyChange);
    }

    public void sendReportedProperties(Set<Property> reportedProperties) throws IOException
    {
        this.deviceClient.sendReportedProperties(reportedProperties);
    }

    public void subscribeToDeviceMethod(DeviceMethodCallback deviceMethodCallback, Object deviceMethodCallbackContext, IotHubEventCallback deviceMethodStatusCallback, Object deviceMethodStatusCallbackContext) throws IOException
    {
        this.deviceClient.subscribeToDeviceMethod(deviceMethodCallback,deviceMethodCallbackContext,deviceMethodStatusCallback,deviceMethodStatusCallbackContext);
    }
}
