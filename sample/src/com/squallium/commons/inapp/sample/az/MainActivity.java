package com.squallium.commons.inapp.sample.az;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse.ItemDataRequestStatus;
import com.amazon.inapp.purchasing.PurchaseResponse.PurchaseRequestStatus;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse.PurchaseUpdatesRequestStatus;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.squallium.commons.inapp.AmazonInAppBilling;
import com.squallium.commons.inapp.IInAppBilling;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseData;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.SKUData;
import com.squallium.commons.inapp.amazon.MySKU;
import com.squallium.commons.inapp.sample.R;

public class MainActivity extends AmazonInAppBilling {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = "SampleIAPConsumablesApp";

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

	/**
	 * Callback on successful purchase of SKU. In this sample app, we show level
	 * 2 as enabled
	 * 
	 * @param sku
	 */
	@Override
	public void onPurchaseResponseSuccess(String userId, String sku,
			String purchaseToken) {
		Log.i(TAG, "onPurchaseResponseSuccess: for userId (" + userId
				+ ") sku (" + sku + ")");
		SKUData skuData = getSKUData(sku);
		if (skuData == null)
			return;

		if (MySKU.ORANGE.getSku().equals(skuData.getSKU())) {

			final int haveQuantity = skuData.getHaveQuantity();
			final int consumedQuantity = skuData.getConsumedQuantity();

			Log.i(TAG,
					"onGetUserIdResponseSuccessful: call updateOrangesInView, have ("
							+ haveQuantity + ") oranges and consumed ("
							+ consumedQuantity + ") oranges");
			updateOrangesInView(haveQuantity, consumedQuantity);
		}
	}

	/**
	 * Callback when user is already entitled
	 * {@link PurchaseRequestStatus#ALREADY_ENTITLED} to sku passed into
	 * initiatePurchaseRequest.
	 * 
	 * @param userId
	 */
	@Override
	public void onPurchaseResponseAlreadyEntitled(String userId, String sku) {
		// This will not be called for consumables
		Log.i(TAG, "onPurchaseResponseAlreadyEntitled: for userId (" + userId
				+ ") sku (" + sku + ")");
	}

	/**
	 * Callback when sku passed into
	 * {@link PurchasingManager#initiatePurchaseRequest} is not valid
	 * {@link PurchaseRequestStatus#INVALID_SKU}.
	 * 
	 * @param userId
	 * @param sku
	 */
	@Override
	public void onPurchaseResponseInvalidSKU(String userId, String sku) {
		Log.i(TAG, "onPurchaseResponseInvalidSKU: for userId (" + userId
				+ ") sku (" + sku + ")");
	}

	/**
	 * Callback on failed purchase response {@link PurchaseRequestStatus#FAILED}
	 * .
	 * 
	 * @param requestId
	 * @param sku
	 */
	@Override
	public void onPurchaseResponseFailed(String requestId, String sku) {
		Log.i(TAG, "onPurchaseResponseFailed: for requestId (" + requestId
				+ ") sku (" + sku + ")");
	}

	/**
	 * Callback on successful purchase updates response
	 * {@link PurchaseUpdatesRequestStatus#SUCCESSFUL} for each receipt.
	 * 
	 * @param userId
	 * @param sku
	 * @param purchaseToken
	 */
	@Override
	public void onPurchaseUpdatesResponseSuccess(String userId, String sku,
			String purchaseToken) {
		// Not called for consumables
		Log.i(TAG, "onPurchaseUpdatesResponseSuccess: for userId (" + userId
				+ ") sku (" + sku + ") purchaseToken (" + purchaseToken + ")");
	}

	/**
	 * Callback on successful purchase updates response
	 * {@link PurchaseUpdatesRequestStatus#SUCCESSFUL} for revoked SKU.
	 * 
	 * @param userId
	 * @param revokedSKU
	 */
	@Override
	public void onPurchaseUpdatesResponseSuccessRevokedSku(String userId,
			String revokedSku) {
		// Not called for consumables
		Log.i(TAG, "onPurchaseUpdatesResponseSuccessRevokedSku: for userId ("
				+ userId + ")");
	}

	/**
	 * Callback on failed purchase updates response
	 * {@link PurchaseUpdatesRequestStatus#FAILED}
	 * 
	 * @param requestId
	 */
	public void onPurchaseUpdatesResponseFailed(String requestId) {
		// Not called for consumables
		Log.i(TAG, "onPurchaseUpdatesResponseFailed: for requestId ("
				+ requestId + ")");
	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Click handler called when user clicks button to buy an orange consumable.
	 * This method calls
	 * {@link PurchasingManager#initiatePurchaseRequest(String)} with the SKU
	 * for the orange consumable.
	 */
	public void onBuyOrangeClick(View view) {
		String requestId = PurchasingManager
				.initiatePurchaseRequest(MySKU.ORANGE.getSku());
		PurchaseData purchaseData = purchaseDataStorage
				.newPurchaseData(requestId);
		Log.i(TAG, "onBuyOrangeClick: requestId (" + requestId
				+ ") requestState (" + purchaseData.getRequestState() + ")");
	}

	/**
	 * Click handler called when user clicks button to eat an orange consumable.
	 */
	public void onEatOrangeClick(View view) {
		String sku = MySKU.ORANGE.getSku();

		SKUData skuData = purchaseDataStorage.getSKUData(sku);
		Log.i(TAG, "onEatOrangeClick: consuming 1 orange");
		skuData.consume(1);
		purchaseDataStorage.saveSKUData(skuData);

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
			if (!MySKU.getAll().contains(unavailableSku))
				continue;

			if (MySKU.ORANGE.getSku().equals(unavailableSku)) {
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
			if (MySKU.ORANGE.getSku().equals(sku)) {
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
}
