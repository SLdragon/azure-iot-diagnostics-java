// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.sdk.iot.device.diagnostic;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Device;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Property;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.PropertyCallBack;
import com.microsoft.azure.sdk.iot.device.diagnostic.ContinuousDiagnosticProvider;
import com.microsoft.azure.sdk.iot.device.diagnostic.DeviceClientWrapper;
import com.microsoft.azure.sdk.iot.device.diagnostic.IDiagnosticProvider;



import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;

/**
 * Device Twin Sample for an IoT Hub. Default protocol is to use
 * MQTT transport.
 */
public class DeviceTwin
{
    private enum LIGHTS{ ON, OFF, DISABLED }

    private enum CAMERA{ DETECTED_BURGLAR, SAFELY_WORKING }

    private static final int MAX_EVENTS_TO_REPORT = 5;

    protected static class DeviceTwinStatusCallBack implements IotHubEventCallback
    {
        public void execute(IotHubStatusCode status, Object context)
        {
            System.out.println("IoT Hub responded to device twin operation with status " + status.name());
        }
    }

    protected static class onHomeTempChange implements PropertyCallBack
    {
        @Override
        public void PropertyCall(Object propertyKey, Object propertyValue, Object context)
        {
            //if (propertyValue.equals(80))
            {
                System.out.println("Cooling down home, temp changed to " + propertyValue);
            }
        }

    }

    protected static class onCameraActivity implements PropertyCallBack
    {
        @Override
        public void PropertyCall(Object propertyKey, Object propertyValue, Object context)
        {
            System.out.println(propertyKey + " changed to " + propertyValue);
            if (propertyValue.equals(CAMERA.DETECTED_BURGLAR))
            {
                System.out.println("Triggering alarm, burglar detected");
            }
        }

    }


    /**
     * Reports properties to IotHub, receives desired property notifications from IotHub. Default protocol is to use
     * use MQTT transport.
     *
     * @param args
     * args[0] = IoT Hub connection string
     */

    public static void main(String[] args)
            throws IOException, URISyntaxException
    {
        System.out.println("Starting...");
        System.out.println("Beginning setup.");


        if (args.length != 1)
        {
            System.out.format(
                    "Expected the following argument but received: %d.\n"
                            + "The program should be called with the following args: \n"
                            + "[Device connection string] - String containing Hostname, Device Id & Device Key in the following formats: HostName=<iothub_host_name>;DeviceId=<device_id>;SharedAccessKey=<device_key>\n",
                    args.length);
            return;
        }

        String connString = args[0];

        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        System.out.println("Successfully read input parameters.");
        System.out.format("Using communication protocol %s.\n",
                protocol.name());

        DeviceClientWrapper client = new DeviceClientWrapper(connString, new ContinuousDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.Client,20));

        if (client == null)
        {
            System.out.println("Could not create an IoT Hub client.");
            return;
        }

        System.out.println("Successfully created an IoT Hub client.");

        Device homeKit = new Device()
        {
            @Override
            public void PropertyCall(String propertyKey, Object propertyValue, Object context)
            {
                System.out.println(propertyKey + " changed to " + propertyValue);
            }
        };

        try
        {
            client.open();

            System.out.println("Opened connection to IoT Hub.");

            client.startDeviceTwin(new DeviceTwinStatusCallBack(), null, homeKit, null);

            System.out.println("Starting to device Twin...");

            homeKit.setDesiredPropertyCallback(new Property("HomeTemp3(F)", null), new onHomeTempChange(), null);
            homeKit.setDesiredPropertyCallback(new Property("LivingRoomLights3", null), homeKit, null);
            homeKit.setDesiredPropertyCallback(new Property("BedroomRoomLights3", null), homeKit, null);
            homeKit.setDesiredPropertyCallback(new Property("HomeSecurityCamera3", null), new onCameraActivity(), null);

            homeKit.setReportedProp(new Property("HomeTemp4(F)", 70));
            homeKit.setReportedProp(new Property("LivingRoomLights3", LIGHTS.ON));
            homeKit.setReportedProp(new Property("BedroomRoomLights3", LIGHTS.OFF));
            client.sendReportedProperties(homeKit.getReportedProp());

            client.subscribeToDesiredProperties(homeKit.getDesiredProp());
            System.out.println("Waiting for Desired properties");
        }
        catch (Exception e)
        {
            System.out.println("On exception, shutting down \n" + " Cause: " + e.getCause() + " \n" +  e.getMessage());
            homeKit.clean();
            client.close();
            System.out.println("Shutting down...");
        }

        System.out.println("Press any key to exit...");

        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();

        homeKit.clean();
        client.close();

        System.out.println("Shutting down...");

    }
}
