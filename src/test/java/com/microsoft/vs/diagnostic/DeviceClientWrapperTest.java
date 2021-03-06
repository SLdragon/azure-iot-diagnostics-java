package com.microsoft.vs.diagnostic;

import com.microsoft.azure.sdk.iot.deps.serializer.Twin;
import com.microsoft.azure.sdk.iot.deps.serializer.TwinChangedCallback;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.DeviceClientConfig;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.Device;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceTwin;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.PropertyCallBack;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.transport.mqtt.MqttTransport;
import mockit.*;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

/**
 * Created by zhqqi on 3/30/2017.
 */
// HostName=iothub.device.com;DeviceId=java;SharedAccessKey=NMNxZNArh
public class DeviceClientWrapperTest {

    @Mocked
    IDiagnosticProvider mockDiagnosticProvider;

    final String DEVICE_CONNECTION_STRING = "HostName=iothub.device.com;DeviceId=java;SharedAccessKey=NMNxZNArh";

    // Constructor of Wrapper should new instance of Client
    // Only mqtt is allow in Wrapper
    @Test
    public void WrapperConstructor()
            throws URISyntaxException
    {
        new DeviceClientWrapper(DEVICE_CONNECTION_STRING,mockDiagnosticProvider);
        new Verifications()
        {
            {
                new DeviceClient(DEVICE_CONNECTION_STRING,IotHubClientProtocol.MQTT);
            }
        };
    }

    @Test
    // When connection open, start device twin automatically
    public void startDeviceTwinOnConnectionOpen(
            @Mocked final MqttTransport mockTransport,
            @Mocked final Twin mockTwin
    )
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };
        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING,mockDiagnosticProvider);
        wrapper.open();
        new Verifications()
        {
            {
                new Twin((TwinChangedCallback)any,(TwinChangedCallback)any);
            }
        };
    }

    @Test
    // User will receive custom desired twin update
    public void userWillReceiveCustomDesiredTwinUpdate(
            @Mocked final MqttTransport mockTransport,
            @Mocked final IotHubEventCallback mockIHCB,
            @Mocked final PropertyCallBack mockPCB
    ) throws URISyntaxException, IOException {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };

        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING,mockDiagnosticProvider);
        wrapper.open();
        wrapper.startDeviceTwin(mockIHCB,null,mockPCB,null);

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        t.updateTwin("{\"desired\":{\"custom\":\"value\"}}");
        new Verifications()
        {
            {
                mockPCB.PropertyCall("custom",(Object)"value",(Object)any);
            }
        };
    }

    @Test
    // User won't received diagnostic twin update
    public void userWillNotReceiveDiagnosticTwinUpdate(
            @Mocked final MqttTransport mockTransport,
            @Mocked final IotHubEventCallback mockIHCB,
            @Mocked final PropertyCallBack mockPCB
    ) throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };

        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING,mockDiagnosticProvider);
        wrapper.open();
        wrapper.startDeviceTwin(mockIHCB,null,mockPCB,null);

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"true\",\"diag_sample_rate\":50}}");
        new Verifications()
        {
            {
                mockPCB.PropertyCall(anyString,(Object)any,(Object)any);
                times=0;
            }
        };
    }

    @Test
    // Sampling rate will not be update when source is client
    public void samplingRateWillNotBeUpdateWhenSourceIsClient(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };


        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.Client,20) {
        });
        wrapper.open();

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"true\",\"diag_sample_rate\":50}}");
        assertEquals(p.getSamplingRatePercentage(),20);
    }

    @Test
    // Sampling rate will not be update when source is none
    public void samplingRateWillNotBeUpdateWhenSourceIsNone(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };


        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.None,20) {
        });
        wrapper.open();

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"true\",\"diag_sample_rate\":50}}");
        assertEquals(p.getSamplingRatePercentage(),20);
    }

    @Test
    // Sampling rate will be update when source is server
    public void samplingRateWillBeUpdateWhenSourceIsServer(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };


        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.Server,20) {
        });
        wrapper.open();

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"true\",\"diag_sample_rate\":50}}");
        assertEquals(p.getSamplingRatePercentage(),50);
    }

    @Test
    // Sampling switch will not be changed when source is none
    public void samplingWillNotBeChangedWhenSourceIsNone(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };

        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.None,100) {
        });
        wrapper.open();

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"true\",\"diag_sample_rate\":100}}");
        assertEquals(p.NeedSampling(),false);
    }

    @Test
    // Sampling switch will not be changed when source is Client
    public void samplingSwitchWillNotBeChangedWhenSourceIsClient(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };

        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.Client,100) {
        });
        wrapper.open();

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"false\"}}");
        assertEquals(p.NeedSampling(),true);
    }

    @Test
    // Sampling switch will defaultly turned off when source is Server
    public void samplingSwitchWillBeDefaultlyTurnedOffWhenSourceIsServer(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };

        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.Server,100) {
        });
        wrapper.open();
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        assertEquals(p.NeedSampling(),false);
    }

    @Test
    // Sampling switch will be changed when source is Server
    public void samplingSwitchWillBeChangedWhenSourceIsServer(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };

        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.Server,0) {
        });
        wrapper.open();

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"true\",\"diag_sample_rate\":100}}");
        assertEquals(p.NeedSampling(),true);
    }

    @Test
    // Sampling switch will not be changed when twin format invalid
    public void samplingWillNotBeChangedWhenTwinFormatInvalid(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };

        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.Server,0) {
        });
        wrapper.open();

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"tru1e\",\"diag_sample_rate\":100}}");
        assertEquals(p.NeedSampling(),false);
    }

    @Test
    // Sampling rate will not be changed when twin format invalid
    public void samplingRateNotBeChangedWhenTwinFormatInvalid(
            @Mocked final MqttTransport mockTransport)
            throws URISyntaxException, IOException
    {
        new NonStrictExpectations()
        {
            {
                mockTransport.isEmpty();
                result = true;
            }
        };

        DeviceClientWrapper wrapper = new DeviceClientWrapper(DEVICE_CONNECTION_STRING, new BaseDiagnosticProvider(IDiagnosticProvider.SamplingRateSource.Server,0) {
        });
        wrapper.open();

        DeviceClient dc = Deencapsulation.getField(wrapper,"deviceClient");
        DeviceTwin dt = Deencapsulation.getField(dc,"deviceTwin");
        Twin t = Deencapsulation.getField(dt,"twinObject");
        final IDiagnosticProvider p = Deencapsulation.getField(wrapper,"diagnosticProvider");
        t.updateTwin("{\"desired\":{\"diag_enable\":\"true\",\"diag_sample_rate\":101}}");
        assertEquals(p.getSamplingRatePercentage(),0);
    }

}
