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
import com.amazon.inapp.purchasing.PurchaseResponse.PurchaseRequestStatus;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse.PurchaseUpdatesRequestStatus;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.squallium.commons.inapp.InAppBillingManager.Store;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseData;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseDataStorage;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.SKUData;
import com.squallium.commons.inapp.amazon.AppPurchasingObserverListener;

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
	private PurchaseDataStorage purchaseDataStorage;

	private OnUserChangedListener onUserChangedListener;

	private OnItemSkuAvailableListener onItemSkuAvailableListener;

	private OnItemSkuUnavailableListener onItemSkuUnavailableListener;

	private OnPurchaseFinishedListener onPurchaseFinishedListener;

	private OnPurchaseUpdatesResponseListener onPurchaseUpdatesResponseListener;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup skus
		setupIAPSkus();

		// Setup listeners
		setupIAPListeners();

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
		Log.i(TAG, "onResume: call initiateItemDataRequest for skus: "
				+ InAppBillingManager.getInstance(Store.amazon).getAllSkus());
		PurchasingManager.initiateItemDataRequest(InAppBillingManager
				.getInstance(Store.amazon).getAllSkus());
	}

	@Override
	public void customOnActivityResult(int requestCode, int resultCode,
			Intent data) {

	}

	/**
	 * Callback for a successful get user id response
	 * {@link GetUserIdResponseStatus#SUCCESSFUL}.
	 * 
	 * In this sample app (consumable), if the user changed from the previously
	 * stored user, this method updates the display based on purchase data
	 * stored for the user in SharedPreferences. The orange consumable is
	 * fulfilled if a stored purchase token was found to NOT be fulfilled or if
	 * the SKU should be fulfilled.
	 * 
	 * In this sample app (non-consumable), if the user changed from the
	 * previously stored user, this method updates the display based on purchase
	 * data stored for the user in SharedPreferences. The level 2 entitlement is
	 * fulfilled if a stored purchase token was found to NOT be fulfilled or if
	 * the SKU should be fulfilled.
	 * 
	 * @param userId
	 *            returned from {@link GetUserIdResponse#getUserId()}.
	 * @param userChanged
	 *            - whether user changed from previously stored user.
	 */
	@Override
	public void onGetUserIdResponseSuccessful(String userId, boolean userChanged) {
		Log.i(TAG, "onGetUserIdResponseSuccessful: update display if userId ("
				+ userId + ") user changed from previous stored user ("
				+ userChanged + ")");

		if (!userChanged)
			return;

		// Callback to alert the change of the user
		if (onUserChangedListener != null) {
			onUserChangedListener.onUserChanged();
		}

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
			if (onItemSkuAvailableListener != null) {
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
		Log.i(TAG, "onItemDataResponseSuccessfulWithUnavailableSkus: for ("
				+ unavailableSkus.size() + ") unavailableSkus");
		if (onItemSkuUnavailableListener != null) {
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

		// Call the callback if exists
		if (onPurchaseFinishedListener != null) {
			onPurchaseFinishedListener.onPurchaseSuccess(null, sku);
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
		// Call the callback if exists
		if (onPurchaseFinishedListener != null) {
			onPurchaseFinishedListener.onPurchaseAlreadyEntitled(null, sku);
		}
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
		String message = "onPurchaseResponseInvalidSKU: for userId (" + userId
				+ ") sku (" + sku + ")";
		Log.i(TAG, message);
		if (onPurchaseFinishedListener != null) {
			onPurchaseFinishedListener.onPurchaseFailed(null, sku, message);
		}
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
		String message = "onPurchaseResponseFailed: for requestId ("
				+ requestId + ") sku (" + sku + ")";
		Log.i(TAG, message);
		if (onPurchaseFinishedListener != null) {
			onPurchaseFinishedListener.onPurchaseFailed(null, sku, message);
		}
	}

	/**
	 * Callback {@link PurchasingManager#initiatePurchaseUpdatesRequest} on
	 * successful purchase updates response
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
		if (onPurchaseUpdatesResponseListener != null) {
			onPurchaseUpdatesResponseListener.onPurchaseUpdatesSuccess(null,
					sku, purchaseToken);
		}
	}

	/**
	 * Callback {@link PurchasingManager#initiatePurchaseUpdatesRequest} on
	 * successful purchase updates response
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
				+ userId + ") revokedSku (" + revokedSku + ")");
		if (onPurchaseUpdatesResponseListener != null) {
			onPurchaseUpdatesResponseListener.onPurchaseUpdatesRevokedSku(null,
					revokedSku);
		}
	}

	/**
	 * Callback {@link PurchasingManager#initiatePurchaseUpdatesRequest} on
	 * failed purchase updates response
	 * {@link PurchaseUpdatesRequestStatus#FAILED}
	 * 
	 * @param requestId
	 */
	@Override
	public void onPurchaseUpdatesResponseFailed(String requestId) {
		// Not called for consumables
		Log.i(TAG, "onPurchaseUpdatesResponseFailed: for requestId ("
				+ requestId + ")");
		if (onPurchaseUpdatesResponseListener != null) {
			onPurchaseUpdatesResponseListener.onPurchaseUpdatesFailed(null,
					requestId);
		}
	}

	/**
	 * In this method you must setup the skus of the items using the
	 * InAppBillingManager
	 */
	protected abstract void setupIAPSkus();

	/**
	 * You have to setup all de listeners in this method, that will be called
	 * before setup the inapp amazon billing process
	 */
	protected abstract void setupIAPListeners();

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
	 * Method for do the purchase purchase
	 * 
	 * @param inAppType
	 * @param sku
	 * @param requestCode
	 * @param mPurchaseFinishedListener
	 * @param payload
	 */
	public void purchase(InAppType inAppType, String sku,
			IInAppBilling.OnPurchaseFinishedListener pPurchaseFinishedListener) {

		// Set the callback
		setOnPurchaseFinishedListener(pPurchaseFinishedListener);

		// Lanzamos el proceso de compra
		switch (inAppType) {
		case consumable:
		case non_consumable:
			String requestId = PurchasingManager.initiatePurchaseRequest(sku);
			PurchaseData purchaseData = purchaseDataStorage
					.newPurchaseData(requestId);
			Log.i(TAG, "purchase: requestId (" + requestId + ") requestState ("
					+ purchaseData.getRequestState() + ")");
			break;
		case subscription:
			break;
		}
	}

	/**
	 * Consume the "quantity" of item identify by the sku
	 * 
	 * @param sku
	 * @param quantity
	 */
	public void consumeItem(String sku, int quantity) {
		SKUData skuData = purchaseDataStorage.getSKUData(sku);
		Log.i(TAG, "consumeItem: consuming 1 orange");
		skuData.consume(quantity);
		purchaseDataStorage.saveSKUData(skuData);
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

	public IInAppBilling.OnPurchaseFinishedListener getOnPurchaseFinishedListener() {
		return onPurchaseFinishedListener;
	}

	public void setOnPurchaseFinishedListener(
			IInAppBilling.OnPurchaseFinishedListener onPurchaseFinishedListener) {
		this.onPurchaseFinishedListener = onPurchaseFinishedListener;
	}

	public OnUserChangedListener getOnUserChangedListener() {
		return onUserChangedListener;
	}

	public void setOnUserChangedListener(
			OnUserChangedListener onUserChangedListener) {
		this.onUserChangedListener = onUserChangedListener;
	}

	public OnPurchaseUpdatesResponseListener getOnPurchaseUpdatesResponseListener() {
		return onPurchaseUpdatesResponseListener;
	}

	public void setOnPurchaseUpdatesResponseListener(
			OnPurchaseUpdatesResponseListener onPurchaseUpdatesResponseListener) {
		this.onPurchaseUpdatesResponseListener = onPurchaseUpdatesResponseListener;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
