package com.nined.player.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.widget.ListView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.appearance.simple.AlphaInAnimationAdapter;
import com.nhaarman.listviewanimations.util.Insertable;
import com.nined.player.MainActivity;
import com.nined.player.R;
import com.nined.player.upnp.RemotePlayService;
import com.nined.player.views.FileArrayAdapter;
import com.nined.player.views.RemoteImageView;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
    private static final boolean SHOW_LOG = false;

    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    private static final String CURRENT_SERVER = "current_server";
    private static final String SAVED_PATHS = "current_paths";
    private static final String LIST_STATE = "list_state";
    private static final String ROOT_DIRECTORY = "0";
    @LayoutRes
    private static final int LAYOUT = R.layout.fragment_server;
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
    /**
     * List Adapter when browsing devices
     */
    private DeviceArrayAdapter deviceAdapter;
    private AlphaInAnimationAdapter deviceAnimationAdapter;
    /**
     * List Adapter when browsing files
     */
    private FileArrayAdapter fileAdapter;
    private AlphaInAnimationAdapter fileAnimationAdapter;
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
        this.fileAnimationAdapter = new AlphaInAnimationAdapter(fileAdapter);
        this.fileAnimationAdapter.setAbsListView(getListView());
        this.deviceAdapter = new DeviceArrayAdapter(getActivity().getApplicationContext(), RemotePlayService.TYPE_MEDIA_SERVER);
        this.deviceAnimationAdapter = new AlphaInAnimationAdapter(deviceAdapter);
        this.deviceAnimationAdapter.setAbsListView(getListView());
        this.setListAdapter(deviceAnimationAdapter);

        // launch and bind UPnPService
        if (SHOW_LOG) Log.d(TAG, "Attempt to bind service to AndroidUpnpServiceImpl");
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(LAYOUT, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ServerFragment.CURRENT_SERVER, (this.currentServer != null) ?
                this.currentServer.getIdentity().getUdn().toString() : "");
        outState.putStringArrayList(ServerFragment.SAVED_PATHS, new ArrayList<>(this.currentPath));
        this.listState.pop();
        this.listState.push(getListView().onSaveInstanceState());
        outState.putParcelableArrayList(ServerFragment.LIST_STATE, new ArrayList<>(this.listState));
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
        if (getListAdapter() == this.deviceAnimationAdapter) {
            this.deviceAnimationAdapter.notifyDataSetChanged();
            browsingMode(this.deviceAdapter.getItem(position));
        } else if (getListAdapter() == this.fileAnimationAdapter) {
            if (this.fileAdapter.getItem(position) instanceof Container)
                getFiles((this.fileAdapter.getItem(position)).getId());
            else {
                //TODO
                List<Item> playlist = new ArrayList<>();
                for (int i=0; i< this.fileAdapter.getCount(); i++) {
                    if (this.fileAdapter.getItem(i) instanceof Item) {
                        playlist.add((Item) this.fileAdapter.getItem(i));
                    }
                }
                // Play
                MainActivity main = getMainActivity();
                if (main!=null) main.play(playlist, position);
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
                if (SHOW_LOG) Log.d(TAG, "Service bound successfully");
                ServerFragment.this.upnpService = (AndroidUpnpService) service;
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
                if (SHOW_LOG) Log.w(TAG, "Unsuccessful service binding");
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
        setListAdapter(this.deviceAnimationAdapter);
        this.currentServer = null;
        this.empty.setText(R.string.device_list_empty);
        getListView().onRestoreInstanceState(this.listState.pop());
    }
    /**
     * Displays available files in the current media server
     */
    private void browsingMode(Device<?, ?, ?> server) {
        setListAdapter(this.fileAnimationAdapter);
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
        if (SHOW_LOG) Log.d(TAG, "getFiles");
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

    /*********************************/
    /**     Device Array Adapter    **/
    /**     w/ Registry Listener    **/
    /*********************************/
    private class DeviceArrayAdapter extends ArrayAdapter<Device<?, ?, ?>>
            implements RegistryListener, Insertable<Device<?,?,?>> {
        /*********************************/
        /**       Member Variable(s)    **/
        /*********************************/
        private Context context;
        private String deviceType;
        /*********************************/
        /**      Default Constructor    **/
        /*********************************/
        public DeviceArrayAdapter(@NonNull Context context, @NonNull String deviceType) {
            super();
            this.context = context;
            this.deviceType = deviceType;
        }

        /*********************************/
        /**   ArrayAdapter Override(s)  **/
        /*********************************/
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
            if (getActivity()==null) return;
            for (int i = 0; i < getCount(); i++) {
                if (getItem(i).equals(device)) {
                    return;
                }
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device.getType().getType().equals(DeviceArrayAdapter.this.deviceType)) {
                        add(device);
                        //TODO
                        /*sort(new Comparator<Device<?, ?, ?>>() {

                            @Override
                            public int compare(Device<?, ?, ?> lhs,
                                               Device<?, ?, ?> rhs) {
                                return lhs.getDetails().getFriendlyName()
                                        .compareTo(rhs.getDetails().getFriendlyName());
                            }
                        });*/
                    }
                }
            });
        }
        /**
         * Removes the device from the list (if it is an element).
         */
        public void deviceRemoved(final Device<?, ?, ?> device) {
            if (SHOW_LOG) Log.d(TAG, "deviceRemoved");
            if (ServerFragment.this.getMainActivity()==null) return;
            ServerFragment.this.getMainActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (contains(device)) {
                        remove(device);
                    }
                }
            });
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
    /**   Back Pressed Listener     **/
    /*********************************/
    @Override
    public boolean onBackPressed() {
        if (getListAdapter() == this.deviceAnimationAdapter) return false;
        this.currentPath.pop();
        if (currentPath.isEmpty()) serverMode();
        else {
            getFiles(true);
        }
        return true;
    }

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }
}
