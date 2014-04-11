package com.squallium.commons.inapp;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.GetUserIdResponse.GetUserIdRequestStatus;
import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse.ItemDataRequestStatus;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseData;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseDataStorage;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.SKUData;
import com.squallium.commons.inapp.amazon.AppPurchasingObserverListener;
import com.squallium.commons.inapp.amazon.MySKU;

public abstract class AmazonInAppBilling extends InAppBilling implements
		AppPurchasingObserverListener {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = AmazonInAppBilling.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================

	// Wrapper around SharedPreferences to save request state
	// and purchase receipt data
	protected PurchaseDataStorage purchaseDataStorage;

	private OnItemSkuAvailableListener onItemSkuAvailableListener;

	private OnItemSkuUnavailableListener onItemSkuUnavailableListener;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setaup de In-App process
		setupIAPOnCreate();
	}

	/**
	 * Calls {@link PurchasingManager#initiateGetUserIdRequest()} to get current
	 * userId and {@link PurchasingManager#initiateItemDataRequest(Set)} with
	 * the list of SKUs to verify the SKUs are valid in the Appstore.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		// Callback onGetUserIdResponse...()
		Log.i(TAG, "onResume: call initiateGetUserIdRequest");
		PurchasingManager.initiateGetUserIdRequest();

		// Callback onItemDataResponse...()
		Log.i(TAG,
				"onResume: call initiateItemDataRequest for skus: "
						+ MySKU.getAll());
		PurchasingManager.initiateItemDataRequest(MySKU.getAll());
	}

	@Override
	public void customOnActivityResult(int requestCode, int resultCode,
			Intent data) {

	}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Setup for IAP SDK called from onCreate. Sets up
	 * {@link PurchaseDataStorage} for storing purchase receipt data,
	 * {@link AppPurchasingObserver} for listening to IAP API callbacks and sets
	 * up this activity as a {@link AppPurchasingObserverListener} to listen for
	 * callbacks from the {@link AppPurchasingObserver}.
	 */
	private void setupIAPOnCreate() {
		purchaseDataStorage = new PurchaseDataStorage(this);

		AppPurchasingObserver purchasingObserver = new AppPurchasingObserver(
				this, purchaseDataStorage);
		purchasingObserver.setListener(this);

		Log.i(TAG, "onCreate: registering AppPurchasingObserver");
		PurchasingManager.registerObserver(purchasingObserver);
	}

	/**
	 * Return the SKUData from the purchaseDataStorage
	 * 
	 * @param sku
	 * @return
	 */
	protected SKUData getSKUData(String sku) {
		return purchaseDataStorage.getSKUData(sku);
	}

	/**
	 * Callback for a successful get user id response
	 * {@link GetUserIdResponseStatus#SUCCESSFUL}.
	 * 
	 * In this sample app, if the user changed from the previously stored user,
	 * this method updates the display based on purchase data stored for the
	 * user in SharedPreferences. The orange consumable is fulfilled if a stored
	 * purchase token was found to NOT be fulfilled or if the SKU should be
	 * fulfilled.
	 * 
	 * @param userId
	 *            returned from {@link GetUserIdResponse#getUserId()}.
	 * @param userChanged
	 *            - whether user changed from previously stored user.
	 */
	@Override
	public void onGetUserIdResponseSuccessful(String userId, boolean userChanged) {
		Log.i(TAG,
				"onGetUserIdResponseSuccessful: update display based on current userId");

		Set<String> requestIds = purchaseDataStorage.getAllRequestIds();
		Log.i(TAG, "onGetUserIdResponseSuccessful: (" + requestIds.size()
				+ ") saved requestIds");
		for (String requestId : requestIds) {
			PurchaseData purchaseData = purchaseDataStorage
					.getPurchaseData(requestId);
			if (purchaseData == null) {
				Log.i(TAG,
						"onGetUserIdResponseSuccessful: could NOT find purchaseData for requestId ("
								+ requestId + "), skipping");
				continue;
			}
			if (purchaseDataStorage.isRequestStateSent(requestId)) {
				Log.i(TAG,
						"onGetUserIdResponseSuccessful: have not received purchase response for requestId still in SENT status: requestId ("
								+ requestId + "), skipping");
				continue;
			}

			Log.d(TAG, "onGetUserIdResponseSuccessful: requestId (" + requestId
					+ ") " + purchaseData);
			String sku = purchaseData.getSKU();
			SKUData skuData = purchaseDataStorage.getSKUData(sku);

			if (!purchaseData.isPurchaseTokenFulfilled()) {
				Log.i(TAG, "onGetUserIdResponseSuccessful: purchaseToken ("
						+ purchaseData.getPurchaseToken()
						+ ") was NOT fulfilled, fulfilling purchase now");

				purchaseDataStorage.setPurchaseTokenFulfilled(purchaseData
						.getPurchaseToken());
				purchaseDataStorage.setRequestStateFulfilled(requestId);
			} else {
				Log.i(TAG, "onGetUserIdResponseSuccessful: for purchaseToken ("
						+ purchaseData.getPurchaseToken()
						+ ") call fulfillSKU on SKU: " + purchaseData.getSKU());
			}

			/**
			 * Call the onPurchaseSuccessMethod like a normal success purchase
			 */
			onPurchaseResponseSuccess(userId, sku,
					purchaseData.getPurchaseToken());
		}
	}

	/**
	 * Callback for a failed get user id response
	 * {@link GetUserIdRequestStatus#FAILED}
	 * 
	 * @param requestId
	 *            returned from {@link GetUserIdResponsee#getRequestId()} that
	 *            can be used to correlate with original request sent with
	 *            {@link PurchasingManager#initiateGetUserIdRequest()}.
	 */
	@Override
	public void onGetUserIdResponseFailed(String requestId) {
		Log.i(TAG, "onGetUserIdResponseFailed for requestId (" + requestId
				+ ")");
	}

	/**
	 * Callback for successful item data response
	 * {@link ItemDataRequestStatus#SUCCESSFUL} with item data
	 * 
	 * @param itemData
	 *            - map of valid SKU->Items
	 */
	@Override
	public void onItemDataResponseSuccessful(Map<String, Item> itemData) {
		for (Entry<String, Item> entry : itemData.entrySet()) {
			String sku = entry.getKey();
			Item item = entry.getValue();
			Log.i(TAG, "onItemDataResponseSuccessful: sku (" + sku + ") item ("
					+ item + ")");
			if(onItemSkuAvailableListener != null){
				onItemSkuAvailableListener.onItemSkuAvailable(sku);
			}
		}
	}

	/**
	 * Callback for item data response with unavailable SKUs. This means that
	 * these unavailable SKUs are NOT accessible in developer portal. In this
	 * sample app, we would disable the buy button for these SKUs
	 * 
	 * @param unavailableSkus
	 */
	@Override
	public void onItemDataResponseSuccessfulWithUnavailableSkus(
			Set<String> unavailableSkus) {
		if(onItemSkuUnavailableListener != null){
			onItemSkuUnavailableListener.onItemSkuUnavailable(unavailableSkus);
		}
	}

	/**
	 * Callback for failed item data response
	 * {@link ItemDataRequestStatus#FAILED}.
	 * 
	 * @param requestId
	 */
	@Override
	public void onItemDataResponseFailed(String requestId) {
		Log.i(TAG, "onItemDataResponseFailed: for requestId (" + requestId
				+ ")");
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public OnItemSkuAvailableListener getOnItemSkuAvailableListener() {
		return onItemSkuAvailableListener;
	}

	public void setOnItemSkuAvailableListener(
			OnItemSkuAvailableListener onItemSkuAvailableListener) {
		this.onItemSkuAvailableListener = onItemSkuAvailableListener;
	}

	public OnItemSkuUnavailableListener getOnItemSkuUnavailableListener() {
		return onItemSkuUnavailableListener;
	}

	public void setOnItemSkuUnavailableListener(
			OnItemSkuUnavailableListener onItemSkuUnavailableListener) {
		this.onItemSkuUnavailableListener = onItemSkuUnavailableListener;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
