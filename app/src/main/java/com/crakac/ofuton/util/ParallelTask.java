package com.crakac.ofuton.util;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

import java.util.concurrent.RejectedExecutionException;

abstract public class ParallelTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	@SuppressLint("NewApi")
    @SuppressWarnings("unchecked")
    public final void executeParallel(Params... params){
        try{
            executeParallelAnyVersion(params);
        } catch (RejectedExecutionException e){
            executeParallelAnyVersion(params);
        }
	}

    private void executeParallelAnyVersion(Params... params) {
        if (getStatus().equals(Status.RUNNING)) return;
        if (Build.VERSION.SDK_INT >= 11) {
            super.executeOnExecutor(THREAD_POOL_EXECUTOR, params);
        } else {
            super.execute(params);
        }
    }
}
