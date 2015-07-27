package com.nined.player.upnp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.StateVariableAllowedValueRange;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Aekasitt Guruvanich
 * on 7/23/2015.
 */
public class RemotePlayService extends Service implements RegistryListener {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = RemotePlayService.class.getSimpleName();
    private static final boolean SHOW_LOG = true;

    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    public static final String SERVICE_AVTRANSPORT = "AVTransport";
    public static final String SERVICE_RENDERING = "RenderingControl";

    /*********************************/
    /**     Member Variable(s)      **/
    /*********************************/
    protected Messenger listener;
    protected Map<String, Device<?,?,?>> devices = new ConcurrentHashMap<>();
    protected AndroidUpnpService upnpService;

    private ServiceConnection upnpServiceConnection = createUpnpServiceConnection();
    private BroadcastReceiver wifiReceiver = createWifiReceiver();
    /**
     * All active binders. The HashMap value is unused
     */
    Map<RemotePlayServiceBinder, Boolean> binders =
            new WeakHashMap<>();

    /*********************************/
    /**     Service Override(s)     **/
    /*********************************/
    @Override
    public IBinder onBind(Intent intent) {
        RemotePlayServiceBinder binder = new RemotePlayServiceBinder(this);
        this.binders.put(binder, true);
        return binder;
    }

    /**
     * Binds to cling service, registers wifi state change listener
     */
    @Override
    public void onCreate() {
        super.onCreate();
        bindService(new Intent(this, AndroidUpnpServiceImpl.class),
                upnpServiceConnection, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(upnpServiceConnection);
        unregisterReceiver(wifiReceiver);
    }

    /*********************************/
    /**       UPnP Function(s)      **/
    /*********************************/
    private void deviceAdded(final Device<?, ?, ?> device) {
        if (devices.containsValue(device)) return;
        final org.fourthline.cling.model.meta.Service<?, ?> rc = getService(device, SERVICE_RENDERING);
        if (rc==null || listener==null) return;

        if (device.getType().getType().equals("MediaRenderer") && device instanceof RemoteDevice) {
            this.devices.put(device.getIdentity().getUdn().toString(), device);
            try {
                upnpService.getControlPoint().execute(new GetVolume(rc) {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                        if (SHOW_LOG) Log.w(TAG, "Failed to get current Volume: "+defaultMessage);
                        sendError("Failed to get current Volume: " + defaultMessage);
                    }
                    @SuppressWarnings("rawtypes")
                    @Override
                    public void received(ActionInvocation actionInvocation, int i) {
                        int maxVolume = 100;
                        if (rc.getStateVariable("Volume") != null) {
                            StateVariableAllowedValueRange volumeRange = rc.getStateVariable("Volume")
                                    .getTypeDetails().getAllowedValueRange();
                            maxVolume = (int) volumeRange.getMaximum();
                        }

                        Message msg = Message.obtain(null, Provider.MSG_RENDERER_ADDED, 0, 0);
                        String routeName = device.getDetails().getFriendlyName();
                        msg.getData().putParcelable("device", new Provider.Device(
                                device.getIdentity().getUdn().toString(),
                                routeName,
                                device.getDisplayString(),
                                50, // TODO currentVolume,
                                maxVolume));
                        sendMessage(msg);
                    }
                });
            } catch (IllegalArgumentException e) {
                if (SHOW_LOG) Log.e(TAG, e.getMessage());
            }

        }
    }

    /**
     * Remove the device from Provider
     * @param device to be removed
     */
    private void deviceRemoved(Device<?, ?, ?> device) {
        if (device.getType().getType().equals("MediaRenderer") && device instanceof RemoteDevice) {
            Message msg = Message.obtain(null, Provider.MSG_RENDERER_REMOVED, 0, 0);
            String udn = device.getIdentity().getUdn().toString();
            msg.getData().putString("id", udn);
            this.devices.remove(udn);
            sendMessage(msg);
        }
    }

    /**
     * If a device was updated, we just add it again (devices are stored in maps,
     * so adding the same one again just overwrites the old one.)
     * @param device that was updated
     */
    private void deviceUpdated(Device<?, ?, ?> device) {
        deviceAdded(device);
    }

    public void sendMessage(Message msg) {
        if (listener==null) {
            if (SHOW_LOG) Log.w(TAG, "Listener is not initialized on send.");
        }
        try {
            listener.send(msg);
        } catch (RemoteException e) {
            if (SHOW_LOG) Log.w(TAG, "Failed to send message");
        }
    }
    public void sendError(String error) {
        Message msg = Message.obtain(null, Provider.MSG_ERROR, 0, 0);
        msg.getData().putString("error", error);
        sendMessage(msg);
    }
    /**
     * Returns a device service by name for direct queries
     * @param device selected device
     * @param name name of service
     * @return service by name
     */
    public org.fourthline.cling.model.meta.Service<?,?> getService(
            Device<?,?,?> device, String name) {
        return device.findService(new ServiceType("schemas-upnp-org", name));
    }

    /*********************************/
    /** Member Service initiation(s)**/
    /*********************************/
    /**
     * Initiates BroadcastReceiver that listens to WiFi connection
     * @return BroadcastReceiver that registers UPnP service when connected to WiFi network
     */
    private BroadcastReceiver createWifiReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager connectivityManager = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (wifi.isConnected()) {
                    if (upnpService!=null) {
                        for (Device<?, ?, ?> d : upnpService.getControlPoint().getRegistry().getDevices()) {
                            deviceAdded(d);
                        }
                        upnpService.getControlPoint().search();
                    }
                } else {
                    if (upnpService!=null) {
                        for (Map.Entry<String, Device<?, ?, ?>> d : devices.entrySet()) {
                            if (upnpService.getControlPoint().getRegistry().getDevice(new UDN(d.getKey()), false) == null) {
                                deviceRemoved(d.getValue());
                                for (RemotePlayServiceBinder b : binders.keySet()) {
                                    if (b.getCurrentRenderer() != null && b.getCurrentRenderer().equals(d.getValue())) {
                                        b.getSubscriptionCallback().end();
                                        b.setCurrentRenderer(null);
                                        //TODO b.unselectRenderer(d.getKey());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }
    /**
     * Registers DeviceListener, adds known devices and starts search if requested.
     * @return ServiceConnection as initiation for upnpServiceConnection
     */
    private ServiceConnection createUpnpServiceConnection() {
        return new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                upnpService = (AndroidUpnpService) service;
                upnpService.getRegistry().addListener(RemotePlayService.this);
                for(Device<?, ?, ?> d: upnpService.getControlPoint().getRegistry().getDevices()) {
                    if (d instanceof LocalDevice) {
                        localDeviceAdded(upnpService.getRegistry(), (LocalDevice) d);
                    } else {
                        remoteDeviceAdded(upnpService.getRegistry(), (RemoteDevice) d);
                    }
                }
                upnpService.getControlPoint().search();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                upnpService=null;
            }
        };
    }
    /*********************************/
    /**Registry Listener Override(s)**/
    /*********************************/
    @Override
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice remoteDevice) { }

    @Override
    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice remoteDevice, Exception e) {
        if (SHOW_LOG) Log.w(TAG, "Remote device discovery failed", e);
    }

    @Override
    public void remoteDeviceAdded(Registry registry, RemoteDevice remoteDevice) {
        deviceAdded(remoteDevice);
    }

    @Override
    public void remoteDeviceUpdated(Registry registry, RemoteDevice remoteDevice) {
        deviceUpdated(remoteDevice);
    }

    @Override
    public void remoteDeviceRemoved(Registry registry, RemoteDevice remoteDevice) {
        deviceRemoved(remoteDevice);
    }

    @Override
    public void localDeviceAdded(Registry registry, LocalDevice localDevice) {
        deviceAdded(localDevice);
    }

    @Override
    public void localDeviceRemoved(Registry registry, LocalDevice localDevice) {
        deviceRemoved(localDevice);
    }

    @Override
    public void beforeShutdown(Registry registry) {  }

    @Override
    public void afterShutdown() {  }
}
