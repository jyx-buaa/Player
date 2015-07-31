package com.nined.player.upnp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteDescriptor;
import android.support.v7.media.MediaRouteDiscoveryRequest;
import android.support.v7.media.MediaRouteProvider;
import android.support.v7.media.MediaRouteProviderDescriptor.Builder;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.ControlRequestCallback;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 * @author Aekasitt Guruvanich, 9D Technologies
 * on 7/23/2015.
 */
final class Provider extends MediaRouteProvider {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = Provider.class.getSimpleName();
    private static final boolean SHOW_LOG = true;
    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    /**
     * Device has been added;
     * param: Device device
     */
    public static final int MSG_RENDERER_ADDED = 1;
    /**
     * Device has been removed;
     * param: int id
     */
    public static final int MSG_RENDERER_REMOVED = 2;
    /**
     * Playback status information, retrived after RemotePlayService.MSG_GET_STATUS
     */
    public static final int MSG_STATUS_INFO = 3;
    /**
     * Indicates an error in communication between RemotePlayServe and renderer/
     * param: String error
     */
    public static final int MSG_ERROR = 4;
    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private Map<String, Device> devices = new HashMap<>();

    private SparseArray<Pair<Intent, ControlRequestCallback>> requests = new SparseArray<>();

    IRemotePlayService iRemotePlayService;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
           iRemotePlayService = IRemotePlayService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iRemotePlayService = null;
        }
    };

    private static final List<IntentFilter> CONTROL_FILTERS;
    static {
        IntentFilter filter = new IntentFilter();
        filter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
        filter.addAction(MediaControlIntent.ACTION_PLAY);
        filter.addAction(MediaControlIntent.ACTION_PAUSE);
        filter.addAction(MediaControlIntent.ACTION_SEEK);
        filter.addAction(MediaControlIntent.ACTION_STOP);
        filter.addDataScheme("http");
        filter.addDataScheme("https");
        try {
            filter.addDataType("video/*");
            filter.addDataType("audio/*");
        } catch (MalformedMimeTypeException m) {
            throw new RuntimeException(m);
        }
        CONTROL_FILTERS = new ArrayList<>();
        CONTROL_FILTERS.add(filter);
    }
    final Messenger listener = new Messenger(new DeviceListener(this));

    /*********************************/
    /**      Default Constructor    **/
    /*********************************/
    public Provider(Context context) {
        super(context);
        context.bindService(
                new Intent(context,
                        RemotePlayService.class),
                connection,
                Context.BIND_AUTO_CREATE);

    }

    /*********************************/
    /**       Close and Unbind      **/
    /*********************************/
    public void close() {
        getContext().unbindService(connection);
    }


    /*********************************/
    /**MediaRouteProvider Override(s)*/
    /*********************************/
    @Override
    public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
        try {
            if (request != null && request.isActiveScan() && iRemotePlayService!= null) {
                iRemotePlayService.startSearch(listener);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Nullable
    @Override
    public RouteController onCreateRouteController(String routeId) {
        return new RouteController(routeId);
    }

    private void updateRoutes() {
        Builder builder = new Builder();
        for (Entry<String, Device> d : devices.entrySet()) {
            MediaRouteDescriptor routeDescriptor = new MediaRouteDescriptor.Builder(
                    d.getValue().id, d.getValue().name)
                    .setDescription(d.getValue().description)
                    .addControlFilters(CONTROL_FILTERS)
                    .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                    .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                    .setVolumeMax(d.getValue().volumeMax)
                    .setVolume(d.getValue().volume)
                    .build();
            builder.addRoute(routeDescriptor);
        }
        setDescriptor(builder.build());
    }

    /**
     * In case of adding more media types in the future.
     * @param filter the provider's intent filter
     * @param type unchecked and ready to fuck this app up
     */
    @SuppressWarnings("unused")
    private static void addDataTypeUnchecked(IntentFilter filter, String type){
        try {
            filter.addDataType(type);
        }catch (MalformedMimeTypeException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Requests status info via RemotePlayService,
     * stores intent and callback to access later in handle message
     * @param intent intent passed through RouteController.onControlRequest
     * @param callback callback passed through RouteController.onControlRequest
     */
    private void getItemStatus(Intent intent, ControlRequestCallback callback)
            throws RemoteException {
        if (callback == null) return;
        Pair<Intent, ControlRequestCallback> pair = new Pair<>(intent, callback);
        int r = new Random().nextInt();
        requests.put(r, pair);
        iRemotePlayService.getItemStatus(
                intent.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID),
                intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID), r);

    }

    public void handleMessage(Message msg) {
        Bundle data = msg.getData();
        switch (msg.what) {
            case MSG_RENDERER_ADDED:
                msg.getData().setClassLoader(Device.class.getClassLoader());
                Device device = data.getParcelable(RemotePlayService.MSG_DEVICE);
                this.devices.put(device.id, device);
                updateRoutes();
                break;
            case MSG_RENDERER_REMOVED:
                devices.remove(data.getString(RemotePlayService.MSG_ID));
                updateRoutes();
                break;
            case MSG_STATUS_INFO:
                Pair<Intent, ControlRequestCallback> pair = this.requests.get(data.getInt(RemotePlayService.MSG_HASH));
                Bundle status = data.getBundle(RemotePlayService.MSG_STATUS_INFO);
                if (pair.first.hasExtra(MediaControlIntent.EXTRA_SESSION_ID)) {
                    status.putString(MediaControlIntent.EXTRA_SESSION_ID,
                            pair.first.getStringExtra(MediaControlIntent.EXTRA_SESSION_ID));
                }
                if (pair.first.hasExtra(MediaControlIntent.EXTRA_ITEM_ID)) {
                    status.putString(MediaControlIntent.EXTRA_ITEM_ID,
                            pair.first.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID));
                }
                pair.second.onResult(status);
                break;
            case MSG_ERROR:
                if (SHOW_LOG) Log.e(TAG, data.getString(RemotePlayService.MSG_ERROR));
                Toast.makeText(getContext(), data.getString(RemotePlayService.MSG_ERROR), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /*********************************/
    /**    Private RouteController  **/
    /*********************************/
    private final class RouteController extends MediaRouteProvider.RouteController {
        private final String routeId;
        public RouteController(String routeId) {
            this.routeId = routeId;
        }
        @Override
        public void onRelease(){}
        @Override
        public void onSelect() {
            try {
                iRemotePlayService.selectRenderer(this.routeId);
            } catch (RemoteException selectException) {
                if (SHOW_LOG) Log.e(TAG, selectException.getMessage());
            }
        }
        @Override
        public void onUnselect() {
            try {
                iRemotePlayService.unselectRenderer(this.routeId);
            } catch (RemoteException unselectException) {
                if (SHOW_LOG) Log.e(TAG, unselectException.getMessage());
            }
        }
        @Override
        public void onSetVolume(int volume) {
            if (volume <0 || volume > devices.get(this.routeId).volumeMax) return;
            try {
                iRemotePlayService.setVolume(volume);
            } catch (RemoteException volumeException) {
                if (SHOW_LOG) Log.e(TAG, volumeException.getMessage());
            }
        }
        @Override
        public void onUpdateVolume(int delta) {
            onSetVolume(devices.get(this.routeId).volume+delta);
        }
        /**
         * Handles play, pause, resume, stop, seek, and get_status requests for this route
         * @param intent get intent for item status checking;
         * @param callback for receiving callback if necessary
         */
        @Override
        public boolean onControlRequest(Intent intent, ControlRequestCallback callback) {
            try {
                // ACTION_PLAY
                if (intent.getAction().equals(MediaControlIntent.ACTION_PLAY)) {
                    String metaData = (intent.hasExtra(MediaControlIntent.EXTRA_ITEM_METADATA))?
                                intent.getExtras().getString(MediaControlIntent.EXTRA_ITEM_METADATA) : null;
                    iRemotePlayService.play(intent.getDataString(), metaData);
                    // Store in intent extras for later
                    intent.putExtra(MediaControlIntent.EXTRA_SESSION_ID, this.routeId);
                    intent.putExtra(MediaControlIntent.EXTRA_ITEM_ID, intent.getDataString());
                    getItemStatus(intent, callback);
                    return true;
                }
                // ACTION_PAUSE
                else if (intent.getAction().equals(MediaControlIntent.ACTION_PAUSE)) {
                    iRemotePlayService.pause(this.routeId);
                    return true;
                }
                // ACTION_RESUME
                else if (intent.getAction().equals(MediaControlIntent.ACTION_RESUME)) {
                    iRemotePlayService.resume(this.routeId);
                    return true;
                }
                // ACTION_SEEK
                else if (intent.getAction().equals(MediaControlIntent.ACTION_SEEK)) {
                    iRemotePlayService.seek(this.routeId,
                            intent.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID),
                            intent.getLongExtra(MediaControlIntent.EXTRA_ITEM_CONTENT_POSITION, 0));
                    getItemStatus(intent, callback);
                    return true;
                }
                // ACTION_GET_STATUS
                else if (intent.getAction().equals(MediaControlIntent.ACTION_GET_STATUS)) {
                    getItemStatus(intent, callback);
                    return true;
                }
            } catch (RemoteException e) {
                if (SHOW_LOG) Log.w(TAG, "Failed to execute control request.");
            }
            return false;
        }
    }
    /*********************************/
    /** DeviceListener static class **/
    /*********************************/
    static private class DeviceListener extends Handler {
        private final WeakReference<Provider> service;
        DeviceListener(Provider provider) {
            service = new WeakReference<>(provider);
        }
        @Override
        public void handleMessage(Message msg) {
            if (service.get()!=null) {
                service.get().handleMessage(msg);
            }
        }
    }

    /*********************************/
    /**     Device static class     **/
    /*********************************/
    /**
     * Allows passing and storing basic information about a device.
     */
    static public class Device implements Parcelable {
        /*********************************/
        /**      Member Variable(s)     **/
        /*********************************/
        public String id;
        public String name;
        public String description;
        public int volume;
        public int volumeMax;

        /*********************************/
        /**        Constructor(s)       **/
        /*********************************/
        public Device (@NonNull String id, String name,
                       String description, int volume, int volumeMax)
        {
            this.id = id;
            this.name = name;
            this.description = description;
            this.volume = volume;
            this.volumeMax = volumeMax;
        }
        public Device(Parcel source)
        {
            this.id = source.readString();
            this.name = source.readString();
            this.description = source.readString();
            this.volume = source.readInt();
            this.volumeMax = source.readInt();
        }

        /*********************************/
        /**     Parcelable interface    **/
        /*********************************/
        public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() {
            @Override
            public Device createFromParcel(Parcel source) {
                return new Device(source);
            }
            public Device[] newArray(int size) {
                return new Device[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.id);
            dest.writeString(this.name);
            dest.writeString(this.description);
            dest.writeInt(this.volume);
            dest.writeInt(this.volumeMax);
        }
    }
}
