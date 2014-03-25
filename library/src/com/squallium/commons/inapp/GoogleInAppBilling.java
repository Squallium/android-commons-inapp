package com.squallium.commons.inapp;

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
	protected IabHelper mHelper;

	// The query inventory listener
	private IabHelper.QueryInventoryFinishedListener mGotInventoryListener;

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
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}
		});
	}

	protected abstract String getBase64EncodedPublicKey();

	protected abstract void checkInventoryItems(Inventory inventory);

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

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

}
