package com.nined.player.upnp;

import android.annotation.SuppressLint;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.media.MediaItemStatus.Builder;
import android.util.Log;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author Aekasitt Guruvanich
 * on 7/23/2015.
 */
public class RemotePlayServiceBinder extends IRemotePlayService.Stub {
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = RemotePlayServiceBinder.class.getSimpleName();
    private static final boolean SHOW_LOG = true;

    /*********************************/
    /**      Member Variable(s)     **/
    /*********************************/
    private RemotePlayService remotePlayService;
    private Device<?, ?, ?> currentRenderer;
    private SubscriptionCallback subscriptionCallback;
    private int playbackState;
    private boolean startingPlayback = false;

    /*********************************/
    /**        Constructor(s)       **/
    /*********************************/
    public RemotePlayServiceBinder(RemotePlayService remotePlayService) {
        this.remotePlayService = remotePlayService;
    }

    /*********************************/
    /**       Stub Override(s)      **/
    /*********************************/
    @Override
    public void startSearch(Messenger listener) throws RemoteException {
        this.remotePlayService.listener = listener;
    }

    @Override
    public void selectRenderer(String id) throws RemoteException {
        this.currentRenderer = this.remotePlayService.devices.get(id);
        for (RemotePlayServiceBinder binder : remotePlayService.binders.keySet()) {
            if (binder != this
                    && this.currentRenderer.equals(binder.currentRenderer)) {
                binder.unselectRenderer("");
            }
        }

        this.subscriptionCallback = new SubscriptionCallback(currentRenderer.findService(
                new ServiceType("schemas-upnp-org", "AVTransport")), 600) {
            @SuppressWarnings("rawtypes")
            @Override
            protected void established(GENASubscription genaSubscription) { }
            @SuppressWarnings("rawtypes")
            @Override
            protected void ended(GENASubscription genaSubscription, CancelReason cancelReason, UpnpResponse upnpResponse) { }
            @SuppressWarnings({"rawtypes","unchecked"})
            @Override
            protected void eventReceived(GENASubscription genaSubscription) {
                Map<String, StateVariableValue> m = genaSubscription.getCurrentValues();
                try {
                    LastChange lastChange = new LastChange(new AVTransportLastChangeParser(),
                            m.get("LastChange").toString());
                    if (RemotePlayServiceBinder.this.startingPlayback ||
                            lastChange.getEventedValue(0, AVTransportVariable.TransportState.class)==null) {return;}

                    switch(lastChange.getEventedValue(0, AVTransportVariable.TransportState.class).getValue()) {
                        case PLAYING:
                            RemotePlayServiceBinder.this.playbackState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
                            break;
                        case PAUSED_PLAYBACK:
                            RemotePlayServiceBinder.this.playbackState = MediaItemStatus.PLAYBACK_STATE_PAUSED;
                            break;
                        case STOPPED:
                            RemotePlayServiceBinder.this.playbackState = MediaItemStatus.PLAYBACK_STATE_FINISHED;
                            break;
                        case TRANSITIONING:
                            RemotePlayServiceBinder.this.playbackState = MediaItemStatus.PLAYBACK_STATE_PENDING;
                            break;
                        case NO_MEDIA_PRESENT:
                            RemotePlayServiceBinder.this.playbackState = MediaItemStatus.PLAYBACK_STATE_ERROR;
                            break;
                        default:
                    }
                } catch (Exception e) {
                    if (SHOW_LOG) Log.w(TAG, "Failed to parse UPnP event.", e);
                    RemotePlayServiceBinder.this.remotePlayService.sendError("Failed to parse UPnP event.");
                }
            }
            @SuppressWarnings("rawtypes")
            @Override
            protected void eventsMissed(GENASubscription genaSubscription, int i) { }
            @SuppressWarnings("rawtypes")
            @Override
            protected void failed(GENASubscription genaSubscription, UpnpResponse upnpResponse, Exception e, String defaultMsg) {
                if (SHOW_LOG) Log.w(TAG, "Register Subscription Callback failed: " + defaultMsg, e);
                RemotePlayServiceBinder.this.remotePlayService.sendError("Register Subscription Callback failed: " + defaultMsg);
            }
        };
    }

    /**
     * Ends selection, stops playback if possible
     * @param sessionId renderer's identifier
     * @throws RemoteException
     */
    @Override
    public void unselectRenderer(String sessionId) throws RemoteException {
        if (this.remotePlayService.devices.get(sessionId) != null) {
            stop(sessionId);
        }
        if (this.subscriptionCallback!=null) {
            this.subscriptionCallback.end();
        }
        this.currentRenderer = null;
    }

    /**
     * Sets an absolute volume. The value is assumed to be within the valid volume range
     * @param volume to be set for currentRenderer
     * @throws RemoteException
     */
    @Override
    public void setVolume(int volume) throws RemoteException {
        this.remotePlayService.upnpService.getControlPoint().execute(
                new SetVolume(this.remotePlayService.getService(this.currentRenderer, RemotePlayService.SERVICE_RENDERING), volume)
                {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMessage) {
                        super.failure(invocation, operation);
                        if (SHOW_LOG) Log.w(TAG, "Set volume failed: " + defaultMessage);
                        RemotePlayServiceBinder.this.remotePlayService.sendError("Set volume failed: " + defaultMessage);
                    }
                });
    }

    /**
     * Sets playback source and metadatam then starts playing on current renderer.
     * @param uri resource location in uri form for the renderer to find the media
     * @param metadata other data added
     * @throws RemoteException
     */
    @Override
    public void play(final String uri,final String metadata) throws RemoteException {
        this.startingPlayback = true;
        this.playbackState = MediaItemStatus.PLAYBACK_STATE_BUFFERING;
        this.remotePlayService.upnpService.getControlPoint().execute(
          new Stop(this.remotePlayService.getService(this.currentRenderer, RemotePlayService.SERVICE_AVTRANSPORT)) {
              @Override
              public void success(ActionInvocation invocation) {
                  super.success(invocation);
                  // Can't use resume here as we don't have the sessionId to call
                  RemotePlayServiceBinder.this.remotePlayService.upnpService.getControlPoint().execute(
                          new SetAVTransportURI(RemotePlayServiceBinder.this.remotePlayService.getService(
                                  RemotePlayServiceBinder.this.currentRenderer, RemotePlayService.SERVICE_AVTRANSPORT), uri, metadata) {
                              @Override
                              public void success(ActionInvocation invocation) {
                                  super.success(invocation);

                                  RemotePlayServiceBinder.this.remotePlayService.upnpService.getControlPoint().execute(
                                          new Play(RemotePlayServiceBinder.this.remotePlayService.getService(RemotePlayServiceBinder.this.currentRenderer,
                                                  RemotePlayService.SERVICE_AVTRANSPORT)) {
                                              @Override
                                              public void success(ActionInvocation invocation) {
                                                  super.success(invocation);
                                                  RemotePlayServiceBinder.this.playbackState = MediaItemStatus.PLAYBACK_STATE_PLAYING;
                                                  RemotePlayServiceBinder.this.startingPlayback = false;
                                              }
                                              @Override
                                              public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                                                  if (SHOW_LOG) Log.w(TAG, "Play failed: " + defaultMessage);
                                                  RemotePlayServiceBinder.this.remotePlayService.sendError("Play failed: " + defaultMessage);
                                                  RemotePlayServiceBinder.this.startingPlayback = false;
                                              }
                                          }
                                  );
                              }
                              @Override
                              public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                                  if (SHOW_LOG) Log.w(TAG, "Set URI failed: " + defaultMessage);
                                  RemotePlayServiceBinder.this.remotePlayService.sendError("Set URI failed: " +defaultMessage);
                                  RemotePlayServiceBinder.this.startingPlayback = false;
                              }
                          });
              }
              @Override
              public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                  if (SHOW_LOG) Log.w(TAG, "Stop playback failed: " + defaultMessage);
                  RemotePlayServiceBinder.this.remotePlayService.sendError("Stop playback failed: " + defaultMessage);
              }
          }
        );
    }

    /**
     * Pauses playback on currentRenderer
     * @param sessionId for currentRenderer
     * @throws RemoteException
     */
    @Override
    public void pause(final String sessionId) throws RemoteException {
        this.remotePlayService.upnpService.getControlPoint().execute(
                new Pause(this.remotePlayService.getService(this.remotePlayService.devices.get(sessionId), RemotePlayService.SERVICE_AVTRANSPORT)) {
                    @Override
                    public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                        if (SHOW_LOG) Log.w(TAG, "Pause failed, trying to stop: " + defaultMessage);
                        RemotePlayServiceBinder.this.remotePlayService.sendError("Pause failed, trying to stop: "+ defaultMessage);
                        try {
                            stop(sessionId);
                        } catch (RemoteException e) {
                            if (SHOW_LOG) Log.w(TAG, "Calling stop from pause failed.", e);
                            RemotePlayServiceBinder.this.remotePlayService.sendError("Calling stop from pause failed: " + e.toString());
                        }
                    }
                });

    }

    @Override
    public void resume(String sessionId) throws RemoteException {
        this.remotePlayService.upnpService.getControlPoint().execute(
            new Play(this.remotePlayService.getService(this.remotePlayService.devices.get(sessionId), RemotePlayService.SERVICE_AVTRANSPORT)) {
                @SuppressWarnings("rawtypes")
                @Override
                public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                    if (SHOW_LOG) Log.w(TAG, "Resume failed: " + defaultMessage);
                    RemotePlayServiceBinder.this.remotePlayService.sendError("Resume failed: " + defaultMessage);
                }
            });
    }

    @Override
    public void stop(String sessionId) throws RemoteException {
        this.remotePlayService.upnpService.getControlPoint().execute(
                new Stop(this.remotePlayService.getService(this.remotePlayService.devices.get(sessionId), RemotePlayService.SERVICE_AVTRANSPORT)) {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                        if (SHOW_LOG) Log.w(TAG, "Stop failed: " + defaultMessage);
                        RemotePlayServiceBinder.this.remotePlayService.sendError("Stop failed: " + defaultMessage);
                    }
                });
    }

    /**
     * Seeks to the given absolute time in seconds
     * @param sessionId for currentRenderer
     * @param itemId playlist item's identifier
     * @param milliseconds absolute time in milliseconds given
     * @throws RemoteException
     */
    @SuppressLint("SimpleDateFormat")
    @Override
    public void seek(String sessionId, String itemId, long milliseconds) throws RemoteException {
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.remotePlayService.upnpService.getControlPoint().execute(
                new Seek(this.remotePlayService.getService(this.remotePlayService.devices.get(sessionId), RemotePlayService.SERVICE_AVTRANSPORT),
                        SeekMode.REL_TIME, df.format(new Date(milliseconds))) {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                        if (SHOW_LOG) Log.w(TAG, "Seek failed: " + defaultMessage);
                        RemotePlayServiceBinder.this.remotePlayService.sendError("Seek failed: " + defaultMessage);
                    }
                }
        );
    }

    @Override
    public void getItemStatus(final String sessionId, final String itemId, final int requestHash) throws RemoteException {
        this.remotePlayService.upnpService.getControlPoint().execute(
                new GetPositionInfo(this.remotePlayService.getService(this.remotePlayService.devices.get(sessionId), RemotePlayService.SERVICE_AVTRANSPORT)) {
                    @Override
                    public void received(ActionInvocation actionInvocation, PositionInfo positionInfo) {
                        Message msg = Message.obtain(null, Provider.MSG_STATUS_INFO, 0, 0);
                        Builder status = new Builder(RemotePlayServiceBinder.this.playbackState);
                        if (positionInfo.getTrackURI() != null && positionInfo.getTrackURI().equals(itemId)) {
                            try {
                                status.setContentPosition(positionInfo.getTrackElapsedSeconds() * 1000);
                                status.setContentDuration(positionInfo.getTrackDurationSeconds() * 1000);
                                status.setTimestamp(positionInfo.getAbsCount());
                            } catch (NumberFormatException e) {
                                if (SHOW_LOG) Log.d(TAG, "Failed to read track position or duration", e);
                                RemotePlayServiceBinder.this.remotePlayService.sendError("Failed to read track position or duration: " + e.toString());
                            }
                        }
                        msg.getData().putBundle("media_item_status", status.build().asBundle());
                        msg.getData().putInt("hash", requestHash);
                        RemotePlayServiceBinder.this.remotePlayService.sendMessage(msg);
                    }

                    @Override
                    public void failure(ActionInvocation actionInvocation, UpnpResponse upnpResponse, String defaultMessage) {
                        if (SHOW_LOG) Log.w(TAG, "Get position failed: " + defaultMessage);
                        RemotePlayServiceBinder.this.remotePlayService.sendError("Get position failed: " + defaultMessage);
                    }
                }
        );
    }
    /*********************************/
    /**      Getters - Setters      **/
    /*********************************/
    public Device<?, ?, ?> getCurrentRenderer() {
        return this.currentRenderer;
    }
    public void setCurrentRenderer(Device<?, ?, ?> renderer) {
        this.currentRenderer = renderer;
    }
    public SubscriptionCallback getSubscriptionCallback() {
        return this.subscriptionCallback;
    }
}
