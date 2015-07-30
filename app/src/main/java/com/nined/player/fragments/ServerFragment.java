package com.nined.player.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nined.player.MainActivity;
import com.nined.player.R;
import com.nined.player.mediaserver.MediaServer;
import com.nined.player.upnp.RemotePlayService;
import com.nined.player.views.FileArrayAdapter;
import com.nined.player.views.RemoteImageView;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.registry.RegistryListener;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ServerFragment extends ListFragment
        implements MainActivity.OnBackPressedListener {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = ServerFragment.class.getSimpleName();
    private static final boolean SHOW_LOG = true;

    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    private static final String CURRENT_SERVER = "current_server";
    private static final String SAVED_PATHS = "current_paths";
    private static final String LIST_STATE = "list_state";

    protected static final String SERVER = "MediaServer";
    private static final String ROOT_DIRECTORY = "0";
    @LayoutRes
    private static final int LISTITEM = R.layout.listitem_route;
    @IdRes
    private static final int ITEM_TITLE = R.id.title;
    @IdRes
    private static final int ITEM_SUBTITLE = R.id.subtitle;
    @IdRes
    private static final int ITEM_IMAGE = R.id.image;

    /*********************************/
    /**       Member Variable(s)    **/
    /*********************************/
    /**
     * Restored device id for listView
     */
    private String restoreServer;
    /**
     * Restored state for listView
     */
    private Stack<Parcelable> listState = new Stack<>();
    /**
     * Restored state for paths
     */
    private Stack<String> currentPath = new Stack<>();
    /**
     * Currently browsing server
     */
    private Device<?,?,?> currentServer;
    private MediaServer localServer;
    /**
     * List Adapter when browsing devices
     */
    private DeviceArrayAdapter deviceAdapter;
    /**
     * List Adapter when browsing files
     */
    private FileArrayAdapter fileAdapter;
    /**
     * UPnP device service
     */
    private AndroidUpnpService upnpService;
    private ServiceConnection upnpServiceConnection = createUpnpServiceConnection();
    private BroadcastReceiver wifiStateListener = createWifiStateListener();
    private TextView empty;

    /*********************************/
    /**     Lifecycle Override(s)   **/
    /*********************************/
    /**
     * Initializes listView adapters. launches CLING UPnP service, registers WiFi state change listener
     * and restores instance state if possible
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.fileAdapter = new FileArrayAdapter(getActivity());
        this.deviceAdapter = new DeviceArrayAdapter(getActivity().getApplicationContext(), ServerFragment.SERVER);
        this.setListAdapter(deviceAdapter);

        // launch and bind UPnPService
        getActivity().getApplicationContext().bindService(
                new Intent(getActivity(), AndroidUpnpServiceImpl.class),
                upnpServiceConnection, Context.BIND_AUTO_CREATE
        );

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(wifiStateListener, filter);

        this.empty = (TextView) getListView().getEmptyView();

        if (savedInstanceState!=null) {
            this.restoreServer = savedInstanceState.getString(ServerFragment.CURRENT_SERVER);
            this.currentPath.addAll(savedInstanceState.getStringArrayList(SAVED_PATHS));
            this.listState.addAll(savedInstanceState.getParcelableArrayList(ServerFragment.LIST_STATE));
        } else {
            this.listState.push(getListView().onSaveInstanceState());
        }
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ServerFragment.CURRENT_SERVER, (this.currentServer != null) ?
                this.currentServer.getIdentity().getUdn().toString() : "");
        outState.putStringArrayList(ServerFragment.SAVED_PATHS, new ArrayList<String>(this.currentPath));
        this.listState.pop();
        this.listState.push(getListView().onSaveInstanceState());
        outState.putParcelableArrayList(ServerFragment.LIST_STATE, new ArrayList<Parcelable>(this.listState));
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(this.upnpServiceConnection);
        getActivity().unregisterReceiver(wifiStateListener);
    }
    /*********************************/
    /**   ListFragment Override(s)  **/
    /*********************************/
    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        if (getListAdapter() == this.deviceAdapter) {
            browsingMode(this.deviceAdapter.getItem(position));
        } else if (getListAdapter() == this.fileAdapter) {
            if (this.fileAdapter.getItem(position) instanceof Container)
                getFiles(((Container) this.fileAdapter.getItem(position)).getId());
            else {
                List<Item> playlist = new ArrayList<Item>();
                for (int i=0; i< this.fileAdapter.getCount(); i++) {
                    if (this.fileAdapter.getItem(i) instanceof Item) {
                        playlist.add((Item) this.fileAdapter.getItem(i));
                    }
                }
                // getMainActivity().play(playlist, position);
            }
        }
    }
    /*********************************/
    /**     Member Initiation(s)    **/
    /*********************************/
    /**
     *
     * @return new UPnP service connection
     */
    private ServiceConnection createUpnpServiceConnection() {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                upnpService = (AndroidUpnpService) service;
                /*
                 * Create and add Local Server
                 */
                try {
                    if (localServer==null) {
                        Context context = getActivity().getApplicationContext();
                        localServer = new MediaServer(getLocalIpAddress(context), context);
                        localServer.start();
                    } else {
                        localServer.restart();
                    }
                } catch (UnknownHostException uhe) {
                    if (SHOW_LOG) Log.w(TAG, "Unknown host for Local Server");
                    if (SHOW_LOG) Log.e(TAG, "Exception: ", uhe);
                } catch (ValidationException ve) {
                    if (SHOW_LOG) Log.w(TAG, "Local Server Creation failed");
                    if (SHOW_LOG) Log.e(TAG, "Exception: ", ve);
                } catch (IOException e) {
                    if (SHOW_LOG) Log.w(TAG, "Starting Http server failed");
                    if (SHOW_LOG) Log.e(TAG, "Exception: ", e);
                }
                if (SHOW_LOG) Log.i(TAG, "Created local server");
                upnpService.getRegistry().addDevice(localServer.getDevice());
                if (SHOW_LOG) Log.i(TAG, "Added local device to upnpService's device list");

                upnpService.getRegistry().addListener(deviceAdapter);
                for (Device<?, ?, ?> d : upnpService.getControlPoint().getRegistry().getDevices()) {
                    ServerFragment.this.deviceAdapter.deviceAdded(d);
                }
                upnpService.getControlPoint().search();
                if (ServerFragment.this.restoreServer!=null) {
                    ServerFragment.this.currentServer = upnpService.getControlPoint().getRegistry()
                            .getDevice(new UDN(restoreServer.replace("uuid:", "")), false);
                    if (ServerFragment.this.currentServer!=null) {
                        ServerFragment.this.setListAdapter(fileAdapter);
                        getFiles(true);
                    }
                    getListView().onRestoreInstanceState(ServerFragment.this.listState.peek());
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                upnpServiceConnection = null;
            }
        };
    }

    /**
     *
     * @return wifiStateListener
     */
    private BroadcastReceiver createWifiStateListener() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (wifi.isConnected()) {
                    if (ServerFragment.this.upnpService != null) {
                        for (Device<?, ?, ?> device : ServerFragment.this.upnpService.getControlPoint()
                                .getRegistry().getDevices()) {
                            ServerFragment.this.deviceAdapter.deviceAdded(device);
                        }
                        ServerFragment.this.upnpService.getControlPoint().search();
                    }
                } else {
                    for (int i = 0; i < ServerFragment.this.deviceAdapter.getCount(); i++) {
                        Device<?, ?, ?> device = ServerFragment.this.deviceAdapter.getItem(i);
                        UDN udn = new UDN(device.getIdentity().getUdn().toString());
                        if (ServerFragment.this.upnpService
                                .getControlPoint().getRegistry().getDevice(udn, false) == null) {
                            ServerFragment.this.deviceAdapter.deviceRemoved(device);
                            if (device.equals(ServerFragment.this.currentServer)) {
                                ServerFragment.this.listState.setSize(2);
                                ServerFragment.this.currentPath.clear();
                                serverMode();
                            }
                        }
                    }
                }
            }
        };
    }
    /*********************************/
    /**       Other Function(s)     **/
    /*********************************/
    /**
     * Displays available servers in the ListView
     */
    private void serverMode() {
        setListAdapter(this.deviceAdapter);
        this.currentServer = null;
        this.empty.setText(R.string.device_list_empty);
        getListView().onRestoreInstanceState(this.listState.pop());
    }
    /**
     * Displays available files in the current media server
     */
    private void browsingMode(Device<?, ?, ?> server) {
        setListAdapter(this.fileAdapter);
        this.currentServer = server;
        getFiles(ROOT_DIRECTORY);
        this.empty.setText(R.string.folder_list_empty);
    }
    private void getFiles(String directory) {
        this.listState.push(getListView().onSaveInstanceState());
        this.currentPath.push(directory);
        getFiles(false);
    }
    /**
     * Displays the current directory on the ListView
     */
    private void getFiles(final boolean restoreListState) {
        if (this.currentServer==null) return;

        Service<?,?> service = this.currentServer.findService(
                new ServiceType(RemotePlayService.UPNP_SCHEMAS, RemotePlayService.SERVICE_CONTENT_DIRECTORY));
        this.upnpService.getControlPoint().execute(new Browse(service,
                this.currentPath.peek(), BrowseFlag.DIRECT_CHILDREN) {
            @SuppressWarnings("rawtypes")
            @Override
            public void received(ActionInvocation actionInvocation, final DIDLContent didl) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ServerFragment.this.fileAdapter.clear();
                        for (Container c : didl.getContainers()) {
                            ServerFragment.this.fileAdapter.add(c);
                        }
                        for (Item i : didl.getItems()) {
                            ServerFragment.this.fileAdapter.add(i);
                        }
                        if (restoreListState)
                            getListView().onRestoreInstanceState(ServerFragment.this.listState.pop());
                        else getListView().setSelectionFromTop(0, 0);
                    }
                });
            }

            @Override
            public void updateStatus(Status status) {
            }

            @SuppressWarnings("rawtypes")
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                if (SHOW_LOG) Log.w(TAG, "Failed to load directory contents: " + defaultMsg);
            }
        });
    }
    private static InetAddress getLocalIpAddress(Context ctx) throws UnknownHostException
    {
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        if(ipAddress!=0)
            return InetAddress.getByName(String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));

        Log.d(TAG, "No ip adress available throught wifi manager, try to get it manually");

        InetAddress inetAddress;

        inetAddress = getLocalIpAdressFromIntf("wlan0");
        if(inetAddress!=null)
        {
            Log.d(TAG, "Got an ip for interfarce wlan0");
            return inetAddress;
        }

        inetAddress = getLocalIpAdressFromIntf("usb0");
        if(inetAddress!=null)
        {
            Log.d(TAG, "Got an ip for interfarce usb0");
            return inetAddress;
        }

        return InetAddress.getByName("0.0.0.0");
    }
    private static InetAddress getLocalIpAdressFromIntf(String intfName)
    {
        try
        {
            NetworkInterface intf = NetworkInterface.getByName(intfName);
            if(intf.isUp())
            {
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address)
                        return inetAddress;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to get ip adress for interface " + intfName);
        }
        return null;
    }
    /*********************************/
    /**     Device Array Adapter    **/
    /**     w/ Registry Listener    **/
    /*********************************/
    private class DeviceArrayAdapter extends ArrayAdapter<Device<?, ?, ?>>
            implements RegistryListener {
        /*********************************/
        /**       Member Variable(s)    **/
        /*********************************/
        private Context context;
        private String deviceType;
        /*********************************/
        /**      Default Constructor    **/
        /*********************************/
        public DeviceArrayAdapter(@NonNull Context context, @NonNull String deviceType) {
            super(context, R.layout.listitem_route);
            this.context = context;
            this.deviceType = deviceType;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView!=null) holder = (ViewHolder) convertView.getTag();
            else {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(ServerFragment.LISTITEM, parent, false);
                holder = new ViewHolder(convertView);
            }
            convertView.setTag(holder);
            Device<?,?,?> device = getItem(position);
            holder.title.setText(device.getDetails().getFriendlyName());
            holder.subtitle.setText(device.getDisplayString());
            if (device.hasIcons()) {
                URI uri = device.getIcons()[0].getUri();
                if (device instanceof RemoteDevice) {
                    try {
                        RemoteDevice remoteDevice = (RemoteDevice) device;
                        uri = remoteDevice.normalizeURI(uri).toURI();
                    } catch (URISyntaxException e) {
                        if (SHOW_LOG) Log.w(TAG, "Failed to get device icon URI", e);
                    }
                    holder.image.setImageUri(uri);
                }
            }
            return convertView;
        }
        /*********************************/
        /**     Registry Listener(s)    **/
        /*********************************/
        /**
         * Adds a new device to the list if its type equals mDeviceType.
         */
        public void deviceAdded(final Device<?, ?, ?> device) {
            if (SHOW_LOG) Log.d(TAG, "deviceAdded");
            for (int i = 0; i < getCount(); i++) {
                if (getItem(i).equals(device)) {
                    return;
                }
            }
            ServerFragment.this.getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device.getType().getType().equals(DeviceArrayAdapter.this.deviceType)) {
                        add(device);
                        sort(new Comparator<Device<?, ?, ?>>() {

                            @Override
                            public int compare(Device<?, ?, ?> lhs,
                                               Device<?, ?, ?> rhs) {
                                return lhs.getDetails().getFriendlyName()
                                        .compareTo(rhs.getDetails().getFriendlyName());
                            }
                        });
                    }
                }
            });
            notifyDataSetChanged();
        }
        /**
         * Removes the device from the list (if it is an element).
         */
        public void deviceRemoved(final Device<?, ?, ?> device) {
            if (SHOW_LOG) Log.d(TAG, "deviceRemoved");
            ServerFragment.this.getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getPosition(device) != -1) {
                        remove(device);
                    }
                }
            });
            notifyDataSetChanged();
        }
        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception ex) {
            if (SHOW_LOG) Log.w(TAG, "Remote device discovery failed", ex);
        }
        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) { deviceAdded(device); }
        @Override
        public void remoteDeviceUpdated(Registry registry, RemoteDevice device) { deviceAdded(device); }
        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) { deviceRemoved(device);  }
        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) { deviceAdded(device); }
        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) { deviceRemoved(device); }
        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) { }
        @Override
        public void beforeShutdown(Registry registry) { }
        @Override
        public void afterShutdown() { }
    }
    /*********************************/
    /**     List's View Holder      **/
    /*********************************/
    static class ViewHolder {
        @Bind(ITEM_TITLE) protected TextView title;
        @Bind(ITEM_SUBTITLE) protected TextView subtitle;
        @Bind(ITEM_IMAGE) protected RemoteImageView image;
        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    /*********************************/
    /**     Back Press Listener     **/
    /*********************************/
    @Override
    public boolean onBackPressed() {
        return false;
    }

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }
}