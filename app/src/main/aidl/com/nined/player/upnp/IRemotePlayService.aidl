package com.nined.player.upnp;
/**
 * @author Aekasitt Guruvanich
 * on 7/23/2015.
 */
import android.os.Messenger;

interface IRemotePlayService {
    void startSearch(in Messenger listener);
    void selectRenderer(String id);
    void unselectRenderer(String id);
    void setVolume(int volume);
    void play(String uri, String metadata);
    void pause(String sessionId);
    void resume(String sessionId);
    void stop(String sessionId);
    void seek(String sessionId, String itemId, long milliseconds);
    void getItemStatus(String sessionId, String itemId, int requestHash);
}