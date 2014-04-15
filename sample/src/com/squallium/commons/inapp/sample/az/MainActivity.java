package com.squallium.commons.inapp.sample.az;

import java.util.Set;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.squallium.commons.inapp.AmazonInAppBilling;
import com.squallium.commons.inapp.IInAppBilling;
import com.squallium.commons.inapp.InAppBilling;
import com.squallium.commons.inapp.InAppBillingManager;
import com.squallium.commons.inapp.InAppBillingManager.Store;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.SKUData;
import com.squallium.commons.inapp.amazon.MySKU;
import com.squallium.commons.inapp.sample.R;

public class MainActivity extends AmazonInAppBilling {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = "SampleIAPConsumablesApp";

	private static final String ORANGE = "com.amazon.sample.iap.consumable.orange";

	// ===========================================================
	// Fields
	// ===========================================================

	protected Handler guiThreadHandler;

	protected Button buyOrangeButton;
	protected Button eatOrangeButton;

	protected TextView numOranges;
	protected TextView numOrangesConsumed;

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
	protected void setupIAPSkus() {
		InAppBillingManager.getInstance(Store.amazon).addSku(
				new MySKU(InAppType.consumable, ORANGE, 1));
	}

	@Override
	protected void setupIAPListeners() {
		setOnItemSkuAvailableListener(mOnItemSkuAvailableListener);
		setOnItemSkuUnavailableListener(mOnItemSkuUnavailableListener);
		setOnPurchaseFinishedListener(mPurchaseFinishedListener);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Click handler called when user clicks button to buy an orange consumable.
	 * This method calls
	 * {@link AmazonInAppBilling#purchasepurchase(InAppBilling.InAppType, String, IInAppBilling.OnPurchaseFinishedListener)}
	 * with the SKU for the orange consumable.
	 */
	public void onBuyOrangeClick(View view) {
		// Launch de purchase flow
		purchase(InAppType.consumable, ORANGE, mPurchaseFinishedListener);
	}

	/**
	 * Click handler called when user clicks button to eat an orange consumable.
	 */
	public void onEatOrangeClick(View view) {
		consumeItem(ORANGE, 1);
		SKUData skuData = getSKUData(ORANGE);
		updateOrangesInView(skuData.getHaveQuantity(),
				skuData.getConsumedQuantity());
	}

	/**
	 * Setup application specific things, called from onCreate()
	 */
	protected void setupApplicationSpecificOnCreate() {
		setContentView(R.layout.activity_main_az_consum);

		buyOrangeButton = (Button) findViewById(R.id.buy_orange_button);

		eatOrangeButton = (Button) findViewById(R.id.eat_orange_button);
		eatOrangeButton.setEnabled(false);

		numOranges = (TextView) findViewById(R.id.num_oranges);
		numOrangesConsumed = (TextView) findViewById(R.id.num_oranges_consumed);

		guiThreadHandler = new Handler();
	}

	/**
	 * Disable buy button for any unavailable SKUs. In this sample app, this
	 * would just disable "Buy Orange" button
	 * 
	 * @param unavailableSkus
	 */
	protected void disableButtonsForUnavailableSkus(Set<String> unavailableSkus) {
		for (String unavailableSku : unavailableSkus) {
			if (!InAppBillingManager.getInstance(Store.amazon).getAllSkus()
					.contains(unavailableSku))
				continue;

			if (ORANGE.equals(unavailableSku)) {
				Log.i(TAG,
						"disableButtonsForUnavailableSkus: disabling buyOrangeButton");
				disableBuyOrangeButton();
			}
		}
	}

	/**
	 * Disable "Buy Orange" button
	 */
	private void disableBuyOrangeButton() {
		buyOrangeButton.setEnabled(false);
	}

	/**
	 * Enable "Buy Orange" button
	 */
	private void enableBuyOrangeButton() {
		buyOrangeButton.setEnabled(true);
	}

	/**
	 * Update view with how many oranges I have.
	 * 
	 * @param haveQuantity
	 */
	protected void updateOrangesInView(final int haveQuantity) {
		Log.i(TAG, "updateOrangesInView with haveQuantity (" + haveQuantity
				+ ")");
		guiThreadHandler.post(new Runnable() {
			public void run() {
				numOranges.setText(String.valueOf(haveQuantity));

				if (haveQuantity > 0) {
					eatOrangeButton.setEnabled(true);
				}
			}
		});
	}

	/**
	 * Update view with how many oranges I have and how many I've consumed.
	 * 
	 * @param haveQuantity
	 * @param consumedQuantity
	 */
	protected void updateOrangesInView(final int haveQuantity,
			final int consumedQuantity) {
		Log.i(TAG, "updateOrangesInView with haveQuantity (" + haveQuantity
				+ ") and consumedQuantity (" + consumedQuantity + ")");
		guiThreadHandler.post(new Runnable() {
			public void run() {
				numOranges.setText(String.valueOf(haveQuantity));
				numOrangesConsumed.setText(String.valueOf(consumedQuantity));

				if (haveQuantity > 0) {
					eatOrangeButton.setEnabled(true);
				} else {
					eatOrangeButton.setEnabled(false);
				}
			}
		});
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	IInAppBilling.OnItemSkuAvailableListener mOnItemSkuAvailableListener = new OnItemSkuAvailableListener() {

		@Override
		public void onItemSkuAvailable(String sku) {
			if (ORANGE.equals(sku)) {
				enableBuyOrangeButton();
			}
		}
	};

	IInAppBilling.OnItemSkuUnavailableListener mOnItemSkuUnavailableListener = new OnItemSkuUnavailableListener() {

		@Override
		public void onItemSkuUnavailable(Set<String> unavailableSkus) {
			disableButtonsForUnavailableSkus(unavailableSkus);
		}
	};

	IInAppBilling.OnPurchaseFinishedListener mPurchaseFinishedListener = new IInAppBilling.OnPurchaseFinishedListener() {
		public void onPurchaseSuccess(InAppResult inAppResult, String sku) {
			SKUData skuData = getSKUData(sku);
			if (ORANGE.equals(skuData.getSKU())) {

				final int haveQuantity = skuData.getHaveQuantity();
				final int consumedQuantity = skuData.getConsumedQuantity();

				Log.i(TAG,
						"onGetUserIdResponseSuccessful: call updateOrangesInView, have ("
								+ haveQuantity + ") oranges and consumed ("
								+ consumedQuantity + ") oranges");
				updateOrangesInView(haveQuantity, consumedQuantity);
			}
		}

		@Override
		public void onPurchaseFailed(InAppResult arg0, String arg1, String arg2) {

		}
	};

}
