package com.squallium.commons.inapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.squallium.commons.inapp.google.IabHelper;
import com.squallium.commons.inapp.google.IabResult;
import com.squallium.commons.inapp.google.Inventory;
import com.squallium.commons.inapp.google.Purchase;

public abstract class GoogleInAppBilling extends InAppBilling {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = GoogleInAppBilling.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================

	// The helper object
	private IabHelper mHelper;

	// The query inventory listener
	private IabHelper.QueryInventoryFinishedListener mGotInventoryListener;

	private IInAppBilling.OnPurchaseFinishedListener onPurchaseFinishedListener;

	private IInAppBilling.OnConsumeItemListener onConsumeItemListener;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Incializamos el sistema de pago
		// Some sanity checks to see if the developer (that's you!) really
		// followed the
		// instructions to run this sample (don't put these checks on your app!)
		if (getBase64EncodedPublicKey() == null
				|| getBase64EncodedPublicKey().contains("CONSTRUCT_YOUR")) {
			throw new RuntimeException(
					"Please put your app's public key in MainActivity.java. See README.");
		}
		if (getPackageName().startsWith("com.example")) {
			throw new RuntimeException(
					"Please change the sample's package name! See README.");
		}

		// Create the helper, passing it our context and the public key to
		// verify signatures with
		Log.d(TAG, "Creating IAB helper.");
		mHelper = new IabHelper(this, getBase64EncodedPublicKey());

		// enable debug logging (for a production application, you should set
		// this to false).
		mHelper.enableDebugLogging(true);

		// Instanciamos el listener para recuperar el inventario
		mGotInventoryListener = new GotInventoryListener();

		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		Log.d(TAG, "Starting setup.");
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess()) {
					// Oh noes, there was a problem.
					complain("Problem setting up in-app billing: " + result);
					return;
				}

				// Have we been disposed of in the meantime? If so, quit.
				if (mHelper == null)
					return;

				// IAB is fully set up. Now, let's get an inventory of stuff we
				// own.
				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(getQuerySkuDetails(),
						mGotInventoryListener);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + ","
				+ data);
		if (mHelper == null)
			return;

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves
			super.onActivityResult(requestCode, resultCode, data);

			// call the customOnActivityResult for abstraction
			customOnActivityResult(requestCode, resultCode, data);
		} else {
			Log.d(TAG, "onActivityResult handled by IABUtil.");
		}
	}

	/***
	 * Here's where you'd perform any handling of activity results not related
	 * to in-app billing...
	 */
	@Override
	public void customOnActivityResult(int arg0, int arg1, Intent arg2) {

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// very important:
		Log.d(TAG, "Destroying helper.");
		if (mHelper != null) {
			mHelper.dispose();
			mHelper = null;
		}
	}

	/**
	 * Allow to disable de query sku details in case of your inapp items were
	 * created with an old version of the developer console
	 * 
	 * @return
	 */
	protected boolean getQuerySkuDetails() {
		return true;
	}

	protected abstract String getBase64EncodedPublicKey();

	protected abstract void checkInventoryItems(Inventory inventory);

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Method for do the purchase purchase
	 * 
	 * @param inAppType
	 * @param sku
	 * @param requestCode
	 * @param mPurchaseFinishedListener
	 * @param payload
	 */
	public void purchase(InAppType inAppType, String sku, int requestCode,
			IInAppBilling.OnPurchaseFinishedListener pPurchaseFinishedListener,
			String payload) {

		// Registramos el listener de respuesta
		setOnPurchaseFinishedListener(pPurchaseFinishedListener);

		// Lanzamos el proceso de compra
		switch (inAppType) {
		case consumable:
			mHelper.launchPurchaseFlow(this, sku, requestCode,
					mPurchaseFinishedListener, payload);
			break;
		case non_consumable:
			mHelper.launchPurchaseFlow(this, sku, requestCode,
					mPurchaseFinishedListener, payload);
			break;
		case subscription:
			mHelper.launchPurchaseFlow(this, sku, IabHelper.ITEM_TYPE_SUBS,
					requestCode, mPurchaseFinishedListener, payload);
			break;
		}
	}

	/**
	 * Checks if the subscription is available
	 * 
	 * @return
	 */
	public boolean isSubscriptionSupported() {
		boolean result = false;

		if (mHelper != null) {
			result = mHelper.subscriptionsSupported();
		}

		return result;
	}

	/**
	 * Consume item
	 * 
	 * @param pPurchase
	 * @param pConsumeFinishedListener
	 */
	public void consumeItem(Purchase pPurchase,
			IInAppBilling.OnConsumeItemListener pConsumeItemListener) {
		if (pPurchase != null && verifyDeveloperPayload(pPurchase)) {
			Log.d(TAG, "We have gas. Consuming it.");
			setOnConsumeItemListener(pConsumeItemListener);
			mHelper.consumeAsync(pPurchase, mConsumeFinishedListener);
			return;
		}
	}

	/** Verifies the developer payload of a purchase. */
	protected boolean verifyDeveloperPayload(Purchase p) {
		String payload = p.getDeveloperPayload();

		/*
		 * TODO: verify that the developer payload of the purchase is correct.
		 * It will be the same one that you sent when initiating the purchase.
		 * 
		 * WARNING: Locally generating a random string when starting a purchase
		 * and verifying it here might seem like a good approach, but this will
		 * fail in the case where the user purchases an item on one device and
		 * then uses your app on a different device, because on the other device
		 * you will not have access to the random string you originally
		 * generated.
		 * 
		 * So a good developer payload has these characteristics:
		 * 
		 * 1. If two different users purchase an item, the payload is different
		 * between them, so that one user's purchase can't be replayed to
		 * another user.
		 * 
		 * 2. The payload must be such that you can verify it even when the app
		 * wasn't the one who initiated the purchase flow (so that items
		 * purchased by the user on one device work on other devices owned by
		 * the user).
		 * 
		 * Using your own server to store and verify developer payloads across
		 * app installations is recommended.
		 */

		return true;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public IInAppBilling.OnConsumeItemListener getOnConsumeItemListener() {
		return onConsumeItemListener;
	}

	public void setOnConsumeItemListener(
			IInAppBilling.OnConsumeItemListener onConsumeItemListener) {
		this.onConsumeItemListener = onConsumeItemListener;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public IInAppBilling.OnPurchaseFinishedListener getOnPurchaseFinishedListener() {
		return onPurchaseFinishedListener;
	}

	public void setOnPurchaseFinishedListener(
			IInAppBilling.OnPurchaseFinishedListener onPurchaseFinishedListener) {
		this.onPurchaseFinishedListener = onPurchaseFinishedListener;
	}

	// Listener that's called when we finish querying the items and
	// subscriptions we own
	private class GotInventoryListener implements
			IabHelper.QueryInventoryFinishedListener {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {
			Log.d(TAG, "Query inventory finished.");

			// Have we been disposed of in the meantime? If so, quit.
			if (mHelper == null)
				return;

			// Is it a failure?
			if (result.isFailure()) {
				complain("Failed to query inventory: " + result);
				return;
			}

			Log.d(TAG, "Query inventory was successful.");

			/*
			 * Check for items we own. Notice that for each purchase, we check
			 * the developer payload to see if it's correct! See
			 * verifyDeveloperPayload().
			 */
			checkInventoryItems(inventory);
		}
	};

	// Callback for when a purchase is finished
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: "
					+ purchase);

			String message = null;

			// if we were disposed of in the meantime, quit.
			if (mHelper == null)
				return;

			// Build the generic result object
			InAppResult inAppResult = new InAppResult();
			inAppResult.purchase = purchase;

			// check posible errors
			if (result.isFailure()) {
				message = "Error purchasing: " + result;
			}
			if (purchase != null && !verifyDeveloperPayload(purchase)) {
				message = "Error purchasing. Authenticity verification failed.";
			}

			// call the listener
			if (onPurchaseFinishedListener != null) {
				if (message == null) {
					onPurchaseFinishedListener.onPurchaseSuccess(inAppResult,
							purchase != null ? purchase.getSku() : "");
				} else if (result.getResponse() == 7) {
					onPurchaseFinishedListener.onPurchaseAlreadyEntitled(
							inAppResult, purchase != null ? purchase.getSku()
									: "");
				} else {
					onPurchaseFinishedListener.onPurchaseFailed(inAppResult,
							purchase != null ? purchase.getSku() : "", message);
				}
			}

			Log.d(TAG, "End purchase finished flow.");
		}
	};

	// Called when consumption is complete
	IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
		public void onConsumeFinished(Purchase purchase, IabResult result) {
			Log.d(TAG, "Consumption finished. Purchase: " + purchase
					+ ", result: " + result);

			// if we were disposed of in the meantime, quit.
			if (mHelper == null)
				return;

			if (onConsumeItemListener != null) {
				onConsumeItemListener.onConsumeItem(purchase, result);
			}

			Log.d(TAG, "End consumption flow.");
		}
	};

}
