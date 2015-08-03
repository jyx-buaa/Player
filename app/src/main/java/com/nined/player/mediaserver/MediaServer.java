package com.nined.player.mediaserver;

/**
 * Copyright (C) 2013 Aurélien Chabot <aurelien@chabot.fr>
 *
 * This file is part of DroidUPNP.
 *
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.MediaStore;
import android.util.Log;

import com.nined.player.R;
import com.nined.player.client.PlayerApi;
import com.nined.player.upnp.RemotePlayService;
import com.nined.player.utils.PrefUtils;

import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationError;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import fi.iki.elonen.SimpleWebServer;

//import fi.iki.elonen.SimpleWebServer;

public class MediaServer extends SimpleWebServer
{
    /*********************************/
    /**     Logging Assistant(s)    **/
    /*********************************/
    private static final String TAG = MediaServer.class.getSimpleName();
    private static final boolean SHOW_LOG = true;

    /*********************************/
    /**         Constant(s)         **/
    /*********************************/
    private static InetAddress localAddress;
    private static final int port = 8192;
    private static String DEFAULT_SERVER_NAME = "Local Server";
    /*
     *
     */
    private static final String ERROR_VERSION_NOT_FOUND = "Application name not found in package information.";
    private static final String ERROR_ID_NOT_FOUND  = "%s was not found in Media Database.";


    /*********************************/
    /**      Member Varaible(s)     **/
    /*********************************/
    private Context context;
    private UDN udn;
    private LocalDevice device;
    private LocalService localService;

    @SuppressWarnings("unchecked")
    public MediaServer(final Context context) throws ValidationException
    {
        super(null, port, Collections.EMPTY_LIST, true);
        if (SHOW_LOG) Log.i(TAG, "Creating media server !");

        localService = new AnnotationLocalServiceBinder()
                .read(ContentDirectoryService.class);
        this.context = context;

        localService.setManager(new DefaultServiceManager<>(localService, ContentDirectoryService.class));
        MediaServer.DEFAULT_SERVER_NAME = context.getString(R.string.app_name);
        this.udn = UDN.valueOf(new UUID(0,10).toString());

        try {
            MediaServer.localAddress = getLocalIpAddress(this.context);
        } catch (UnknownHostException uhe) {
            if (SHOW_LOG) Log.d(TAG, "Failed to get the local IP address on this device");
        }

        createLocalDevice();
        ContentDirectoryService contentDirectoryService = (ContentDirectoryService)localService.getManager().getImplementation();
        contentDirectoryService.setContext(context);
        contentDirectoryService.setBaseURL(getLocalAddress());
    }

    public void restart()
    {
        if (SHOW_LOG) Log.d(TAG, "Restart mediaServer");
		try {
			stop();
			createLocalDevice();
			start();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public void createLocalDevice() throws ValidationException
    {
        String version = "";
        try {
            version = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            if (SHOW_LOG) Log.e(TAG, MediaServer.ERROR_VERSION_NOT_FOUND);
        }

        DeviceDetails details = new DeviceDetails(
                PrefUtils.getString(this.context, PlayerApi.CONTENTDIRECTORY_NAME, DEFAULT_SERVER_NAME),
                new ManufacturerDetails(this.context.getString(R.string.app_name), this.context.getString(R.string.app_url)),
                new ModelDetails(this.context.getString(R.string.app_name), this.context.getString(R.string.app_url)),
                this.context.getString(R.string.app_name), version);

        List<ValidationError> l = details.validate();
        for( ValidationError v : l )
        {
            if (SHOW_LOG) Log.e(TAG, "Validation pb for property "+ v.getPropertyName());
            if (SHOW_LOG) Log.e(TAG, "Error is " + v.getMessage());
        }

        this.device = new LocalDevice(
                new DeviceIdentity(udn),
                new UDADeviceType(RemotePlayService.TYPE_MEDIA_SERVER, 1),
                details,
                localService);
    }

    /*********************************/
    /**      Getters - Setters      **/
    /*********************************/
    /**
     * @return the local device
     */
    public LocalDevice getDevice() {
        return this.device;
    }
    /**
     * @param device to set
     */
    @SuppressWarnings("unused")
    public void setDevice(LocalDevice device) {
        this.device = device;
    }
    /**
     * @return the local IP address and port
     */
    public String getLocalAddress() {
        return String.format("%s:%s", localAddress.getHostAddress(), MediaServer.port);
    }

    /*********************************/
    /**      Create File Server     **/
    /*********************************/
    /**
     *
     * @param id
     * @return created file server to access media files
     * @throws InvalidIdentificatorException
     */
    private ServerObject getFileServerObject(String id) throws InvalidIdentificatorException
    {
        try
        {
            // Remove extension
            int dot = id.lastIndexOf('.');
            if (dot >= 0)
                id = id.substring(0,dot);

            // Try to get media id
            int mediaId = Integer.parseInt(id.substring(3));
            if (SHOW_LOG) Log.v(TAG, "media of id is " + mediaId);

            MediaStore.MediaColumns mediaColumns = null;
            Uri uri = null;

            if(id.startsWith("/"+ContentDirectoryService.AUDIO_PREFIX))
            {
                if (SHOW_LOG) Log.v(TAG, "Ask for audio");
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                mediaColumns = new MediaStore.Audio.Media();
            }
            else if(id.startsWith("/"+ContentDirectoryService.VIDEO_PREFIX))
            {
                if (SHOW_LOG) Log.v(TAG, "Ask for video");
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                mediaColumns = new MediaStore.Video.Media();
            }
            else if(id.startsWith("/"+ContentDirectoryService.IMAGE_PREFIX))
            {
                if (SHOW_LOG) Log.v(TAG, "Ask for image");
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                mediaColumns = new MediaStore.Images.Media();
            }

            if(uri!=null && mediaColumns!=null)
            {
                String[] columns = new String[]{mediaColumns.DATA, mediaColumns.MIME_TYPE};
                String where = mediaColumns._ID + "=?";
                String[] whereVal = {"" + mediaId};

                String path = null;
                String mime = null;
                Cursor cursor = this.context.getContentResolver().query(uri, columns, where, whereVal, null);

                if(cursor.moveToFirst())
                {
                    path = cursor.getString(cursor.getColumnIndexOrThrow(mediaColumns.DATA));
                    mime = cursor.getString(cursor.getColumnIndexOrThrow(mediaColumns.MIME_TYPE));
                }
                cursor.close();

                if(path!=null)
                    return new ServerObject(path, mime);
            }
        }
        catch (Exception e)
        {
            if (SHOW_LOG) Log.e(TAG, "Error while parsing " + id);
            if (SHOW_LOG) Log.e(TAG, "exception", e);
        }

        throw new InvalidIdentificatorException(String.format(MediaServer.ERROR_ID_NOT_FOUND, id));
    }

    /*********************************/
    /**        NanoHTTPD Serve      **/
    /*********************************/
    /**
     * To serve
     * @param uri
     * @param method
     * @param header
     * @param params
     * @param files
     * @return
     */
    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> params,
                          Map<String, String> files)
    {
        Response response;
        String range = null;
        if (SHOW_LOG) Log.i(TAG, "Serve uri : " + uri);

        for(Entry<String, String> entry : header.entrySet()) {
            if (SHOW_LOG) Log.d(TAG, String.format("Header : key=%s , value=%s", entry.getKey(), entry.getValue()));
            if ("range".equals(entry.getKey())) {
                range = entry.getValue();
            }
        }
        for(Entry<String, String> entry : params.entrySet())
            if (SHOW_LOG) Log.d(TAG, String.format("Params : key=%s, value=%s", entry.getKey(), entry.getValue()));

        for(Entry<String, String> entry : files.entrySet())
            if (SHOW_LOG) Log.d(TAG, String.format("Files : key=%s, value=%s", entry.getKey(), entry.getValue()));

        try
        {
            try
            {
                ServerObject obj = getFileServerObject(uri);
                if (SHOW_LOG) Log.i(TAG, "Will serve " + obj.path);
                if (range==null) {
                    response = getFullResponse(obj.mime, obj.path);
                } else {
                    response = getPartialResponse(obj.mime, obj.path, range);
                }
            } catch(InvalidIdentificatorException e) {
                return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Error 404, file not found.");
            } catch(FileNotFoundException fe) {
                if (SHOW_LOG) Log.e(TAG, "File not found");
                return new Response(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Error 404, file not found.");
            }

            if( response != null )
            {
                String version = "1.0";
                try {
                    version = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    if (SHOW_LOG) Log.e(TAG, MediaServer.ERROR_VERSION_NOT_FOUND);
                }

                /**
                 * Some DLNA header options
                 */
                response.addHeader("realTimeInfo.dlna.org", "DLNA.ORG_TLAG=*");
                response.addHeader("contentFeatures.dlna.org", "");
                response.addHeader("transferMode.dlna.org", "Streaming");
                //response.addHeader("Server", "DLNADOC/1.50 UPnP/1.0 Cling/3.0 SRS Media Stream/"+version +" Android/" + Build.VERSION.RELEASE);
            }
            return response;
        }
        catch(Exception e)
        {
            if (SHOW_LOG) Log.e(TAG, "Unexpected error while serving file");
            if (SHOW_LOG) Log.e(TAG, "exception", e);
        }

        return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "INTERNAL ERROR: unexpected error.");
    }

    private Response getFullResponse(String mimeType, String filePath) throws FileNotFoundException {
        if (SHOW_LOG) Log.d(TAG, "Streaming full file into response");
        cleanUpStreams();
        FileInputStream fileInputStream = new FileInputStream(filePath);
        Response response = new Response(Response.Status.OK, mimeType, fileInputStream);
        response.addHeader("Content-Type", mimeType);
        return response;
    }

    private Response getPartialResponse(String mimeType, String filePath, String rangeHeader) throws IOException {
        if (SHOW_LOG) Log.d(TAG, "Streaming file partially into response");
        File file = new File(filePath);
        String rangeValue = rangeHeader.trim().substring("bytes=".length());
        long fileLength = file.length();
        long start, end;
        if (rangeValue.startsWith("-")) {
            end = fileLength - 1;
            start = fileLength - 1
                    - Long.parseLong(rangeValue.substring("-".length()));
        } else {
            String [] range = rangeValue.split("-");
            start = Long.parseLong(range[0]);
            end = (range.length > 1)? Long.parseLong(range[1])
                    : fileLength - 1;
        }
        if (start <= end) {
            long contentLength = end - start + 1;
            cleanUpStreams();
            FileInputStream fileInputStream = new FileInputStream(file);
            fileInputStream.skip(start);
            Response response = new Response(Response.Status.PARTIAL_CONTENT, mimeType, fileInputStream);
            response.addHeader("Content-Length", contentLength+"");
            response.addHeader("Content-Range", String.format("bytes%d-%d/%d", start, end, fileLength));
            response.addHeader("Content-Type", mimeType);
            return response;
        } else {
            return new Response(Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, rangeHeader);
        }
    }
    private void cleanUpStreams() {

    }
    /*********************************/
    /**       Private Class(s)      **/
    /*********************************/
    class ServerObject
    {
        ServerObject(String path, String mime)
        {
            this.path = path;
            this.mime = mime;
        }
        public String path;
        public String mime;
    }
    @SuppressWarnings("unused")
    public class InvalidIdentificatorException extends Exception
    {
        public InvalidIdentificatorException(){super();}
        public InvalidIdentificatorException(String message){super(message);}
    }

    /*********************************/
    /**     Get Local IP Address    **/
    /*********************************/
    private static InetAddress getLocalIpAddress(Context context) throws UnknownHostException
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
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
}