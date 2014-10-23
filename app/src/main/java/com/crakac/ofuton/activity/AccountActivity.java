package com.crakac.ofuton.activity;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.crakac.ofuton.R;
import com.crakac.ofuton.fragment.AccountListFragment;
import com.crakac.ofuton.util.AppUtil;
import com.crakac.ofuton.util.DialogManager;
import com.crakac.ofuton.util.ParallelTask;
import com.crakac.ofuton.util.TwitterUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class AccountActivity extends FinishableActionbarActivity{
	private RequestToken mRequestToken;
	private String mCallbackURL;
	private DialogManager mDialogManager;
	private AccountListFragment mFragment;
	private static final String REQUEST_TOKEN = "request_token";
	private static final String TAG = AccountActivity.class.getSimpleName();

	private void setFragment(){
		mFragment = new AccountListFragment();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.add(R.id.accountList, mFragment);
		ft.commit();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(REQUEST_TOKEN, mRequestToken);
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mRequestToken = (RequestToken) savedInstanceState.getSerializable(REQUEST_TOKEN);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_account);
		mCallbackURL = getString(R.string.twitter_callback_url);
		mDialogManager = new DialogManager(getSupportFragmentManager());

		setFragment();
	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent");
		if(intent == null
				|| intent.getData() == null
				|| !intent.getData().toString().startsWith(mCallbackURL)){
			return;
		}
        if(isDenied(intent)){
            AppUtil.showToast(R.string.canceled);
            return;
        }
		String verifier = intent.getData().getQueryParameter("oauth_verifier");
        if(verifier == null){
            AppUtil.showToast(R.string.something_wrong);
            return; //if authentication denied, query parameter is denied=hogehoge
        }
		ParallelTask<String, Void, AccessToken> task = new ParallelTask<String, Void, AccessToken>(){

			@Override
			protected void onPreExecute() {
				mDialogManager.showProgress("認証中です");
			}

			@Override
			protected AccessToken doInBackground(String... params) {
				try {
					Twitter tw = TwitterUtils.getTwitterInstanceWithoutToken();
					return tw.getOAuthAccessToken(mRequestToken, params[0]);
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(AccessToken accessToken) {
				mDialogManager.dismissProgress();
				if (accessToken != null){
					successOAuth(accessToken);
				} else {
					AppUtil.showToast("認証に失敗しました");
				}
			}
		};
		task.executeParallel(verifier);
	}
	/**
	 * Store AccessToken and user infomation
	 * @param accessToken
	 *
	 */
	private void successOAuth(AccessToken accessToken){
		ParallelTask<AccessToken, Void, Boolean> task = new ParallelTask<AccessToken, Void, Boolean>() {

			@Override
			protected void onPreExecute() {
				mDialogManager.showProgress("ユーザー情報を取得しています");
			}

			@Override
			protected Boolean doInBackground(AccessToken... params) {
				TwitterUtils.storeAccessToken(params[0]);
				return TwitterUtils.addAccount();
			}

			@Override
			protected void onPostExecute(Boolean result) {
				mDialogManager.dismissProgress();
				if(result){
					setFragment();
				} else {
					AppUtil.showToast("すでに登録されています");
				}
			}
		};
		task.executeParallel(accessToken);
	}

	public void onClickFooter() {
		ParallelTask<Void, Void, String> task = new ParallelTask<Void, Void, String>(){

			@Override
			protected void onPreExecute() {
				mDialogManager.showProgress("認証中です");
			}

			@Override
			protected String doInBackground(Void... params) {
				try {
					Twitter tw = TwitterUtils.getTwitterInstanceWithoutToken();
					mRequestToken = tw.getOAuthRequestToken(mCallbackURL);
					return mRequestToken.getAuthorizationURL();
				} catch (TwitterException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(String url) {
				mDialogManager.dismissProgress();
				if (url != null) {
					Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(intent);
				} else {
					AppUtil.showToast(R.string.something_wrong);
				}
			}
		};
		task.executeParallel();
	}

    private boolean isDenied(Intent intent){
        return intent.getData().getQueryParameter("denied") != null;
    }
}