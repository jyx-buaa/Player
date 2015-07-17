/* 
**
** Copyright 2014, Jules White
**
** 
*/

package com.nined.player.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.Callable;


public class CallableTask<T> extends AsyncTask<Void,Double,T> {
	/**
	 * Logging Assistants
	 */
    private static final String TAG = CallableTask.class.getSimpleName();
    private static final boolean SHOW_LOG = false;

    public static <V> void invoke(Callable<V> call, TaskCallback<V> callback){
        new CallableTask<V>(call, callback).execute();
    }

    private Callable<T> callable_;

    private TaskCallback<T> callback_;
    
    private Exception error_;

    public CallableTask(Callable<T> callable, TaskCallback<T> callback) {
        callable_ = callable;
        callback_ = callback;
    }

    @Override
    protected T doInBackground(Void... ts) {
        T result = null;
        try{
            result = callable_.call();
        } catch (Exception e){
            if (SHOW_LOG) Log.e(TAG, "Error invoking callable in AsyncTask callable: "+callable_, e);
            error_ = e;
        }
        return result;
    }

    @Override
    protected void onPostExecute(T r) {
    	if(error_ != null){
    		callback_.error(error_);
    	}
    	else {
    		callback_.success(r);
    	}
    }
}

