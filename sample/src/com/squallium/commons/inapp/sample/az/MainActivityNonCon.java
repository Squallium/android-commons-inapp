package com.squallium.commons.inapp.sample.az;

import java.util.Set;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazon.inapp.purchasing.PurchasingManager;
import com.squallium.commons.inapp.AmazonInAppBilling;
import com.squallium.commons.inapp.IInAppBilling;
import com.squallium.commons.inapp.InAppBillingManager;
import com.squallium.commons.inapp.InAppBillingManager.Store;
import com.squallium.commons.inapp.amazon.AppPurchasingObserverListener;
import com.squallium.commons.inapp.amazon.MySKU;
import com.squallium.commons.inapp.sample.R;

/**
 * Sample code for IAP entitlements
 * 
 * This is the main activity for this project that shows how to call the
 * PurchasingManager methods and how to get notified through the
 * {@link AppPurchasingObserverListener} callbacks.
 */
public class MainActivityNonCon extends AmazonInAppBilling {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = MainActivityNonCon.class.getSimpleName();

	private static final String LEVEL2 = "com.amazon.sample.iap.entitlement.level2";

	// ===========================================================
	// Fields
	// ===========================================================

	private Handler guiThreadHandler;

	// Button to buy entitlement to level 2
	private Button buyLevel2Button;

	// TextView shows whether user has been entitled to level 2
	private TextView isLevel2EnabledTextView;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	/**
	 * Setup IAP SDK and other UI related objects specific to this sample
	 * application.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupApplicationSpecificOnCreate();
	}

	@Override
	protected void setupIAPListeners() {
		InAppBillingManager.getInstance(Store.amazon).addSku(
				new MySKU(InAppType.consumable, LEVEL2));
	}

	@Override
	protected void setupIAPSkus() {
		setOnItemSkuAvailableListener(mOnItemSkuAvailableListener);
		setOnItemSkuUnavailableListener(mOnItemSkuUnavailableListener);
		setOnPurchaseFinishedListener(mOnPurchaseFinishedListener);
		setOnPurchaseUpdatesResponseListener(mOnPurchaseUpdatesResponseListener);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Setup application specific things, called from onCreate()
	 */
	private void setupApplicationSpecificOnCreate() {
		setContentView(R.layout.activity_main_az_non_consum);

		buyLevel2Button = (Button) findViewById(R.id.buy_level2_button);

		resetApplication();

		guiThreadHandler = new Handler();
	}

	private void resetApplication() {
		// Show "Level 2 Disabled" text in gray color to indicate
		// user does NOT have this entitlement initially
		isLevel2EnabledTextView = (TextView) findViewById(R.id.is_level2_enabled);
		isLevel2EnabledTextView.setText(R.string.level2_disabled);
		isLevel2EnabledTextView.setTextColor(Color.GRAY);
		isLevel2EnabledTextView.setBackgroundColor(Color.WHITE);
	}

	/**
	 * Click handler called when user clicks button to buy access to level 2
	 * entitlement. This method calls
	 * {@link PurchasingManager#initiatePurchaseRequest(String)} with the SKU
	 * for the level 2 entitlement.
	 */
	public void onBuyAccessToLevel2Click(View view) {
		// Launch the purchase flow
		purchase(InAppType.non_consumable, LEVEL2, mOnPurchaseFinishedListener);
	}

	/**
	 * Disable buy button for any unavailable SKUs. In this sample app, this
	 * would just disable "Buy Access to Level 2" button
	 * 
	 * @param unavailableSkus
	 */
	private void disableButtonsForUnavailableSkus(Set<String> unavailableSkus) {
		for (String unavailableSku : unavailableSkus) {
			if (!InAppBillingManager.getInstance(Store.amazon).getAllSkus()
					.contains(unavailableSku))
				continue;

			if (LEVEL2.equals(unavailableSku)) {
				Log.i(TAG, "disableButtonsForUnavailableSkus: unavailableSKU ("
						+ unavailableSku + "), disabling buyLevel2Button");
				disableBuyLevel2Button();
			}
		}
	}

	/**
	 * Disable "Buy Access to Level 2" button
	 */
	private void disableBuyLevel2Button() {
		buyLevel2Button.setEnabled(false);
	}

	/**
	 * Enable "Buy Access to Level 2" button
	 */
	private void enableBuyLevel2Button() {
		buyLevel2Button.setEnabled(true);
	}

	/**
	 * Enable if SKU is for Level 2
	 * 
	 * @param sku
	 */
	private void enableEntitlementForSKU(String sku) {
		if (!LEVEL2.equals(sku))
			return;
		enableLevel2InView();
	}

	/**
	 * Show Level 2 as enabled in view
	 */
	private void enableLevel2InView() {
		Log.i(TAG,
				"enableLevel2InView: enabling level 2, show by setting text color to blue and highlighting");
		guiThreadHandler.post(new Runnable() {
			public void run() {
				isLevel2EnabledTextView.setText(R.string.level2_enabled);
				isLevel2EnabledTextView.setTextColor(Color.BLUE);
				isLevel2EnabledTextView.setBackgroundColor(Color.YELLOW);
			}
		});
	}

	/**
	 * Show Level 2 as disabled in view
	 */
	protected void disableLevel2InView() {
		guiThreadHandler.post(new Runnable() {
			public void run() {
				isLevel2EnabledTextView.setText(R.string.level2_disabled);
				isLevel2EnabledTextView.setTextColor(Color.GRAY);
				isLevel2EnabledTextView.setBackgroundColor(Color.WHITE);
			}
		});
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	IInAppBilling.OnUserChangedListener mOnUserChangedListener = new OnUserChangedListener() {

		@Override
		public void onUserChanged() {
			// Reset to original setting where level2 is disabled
			disableLevel2InView();
		}
	};

	IInAppBilling.OnItemSkuAvailableListener mOnItemSkuAvailableListener = new OnItemSkuAvailableListener() {

		@Override
		public void onItemSkuAvailable(String sku) {
			if (LEVEL2.equals(sku)) {
				enableBuyLevel2Button();
			}
		}
	};

	IInAppBilling.OnItemSkuUnavailableListener mOnItemSkuUnavailableListener = new OnItemSkuUnavailableListener() {

		@Override
		public void onItemSkuUnavailable(Set<String> unavailableSkus) {
			disableButtonsForUnavailableSkus(unavailableSkus);
		}
	};

	IInAppBilling.OnPurchaseFinishedListener mOnPurchaseFinishedListener = new IInAppBilling.OnPurchaseFinishedListener() {

		@Override
		public void onPurchaseSuccess(InAppResult inAppResult, String sku) {
			enableEntitlementForSKU(sku);
		}

		@Override
		public void onPurchaseAlreadyEntitled(InAppResult inAppResult,
				String sku) {
			// For entitlements, even if already entitled, make sure to enable.
			enableEntitlementForSKU(sku);
		}

		@Override
		public void onPurchaseFailed(InAppResult inAppResult, String sku,
				String message) {

		}
	};

	IInAppBilling.OnPurchaseUpdatesResponseListener mOnPurchaseUpdatesResponseListener = new OnPurchaseUpdatesResponseListener() {

		@Override
		public void onPurchaseUpdatesSuccess(InAppResult inAppResult,
				String sku, String purchaseToken) {
			enableEntitlementForSKU(sku);
		}

		@Override
		public void onPurchaseUpdatesRevokedSku(InAppResult inAppResult,
				String revokedSku) {
			if (!LEVEL2.equals(revokedSku))
				return;

			Log.i(TAG,
					"onPurchaseUpdatesResponseSuccessRevokedSku: disabling play level 2 button");
			disableLevel2InView();

			Log.i(TAG,
					"onPurchaseUpdatesResponseSuccessRevokedSku: fulfilledCountDown for revokedSKU ("
							+ revokedSku + ")");
		}

		@Override
		public void onPurchaseUpdatesFailed(InAppResult inAppResult,
				String requestId) {

		}
	};

}
