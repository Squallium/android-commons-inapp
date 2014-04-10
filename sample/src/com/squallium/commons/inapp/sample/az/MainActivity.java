package com.squallium.commons.inapp.sample.az;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.GetUserIdResponse.GetUserIdRequestStatus;
import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse.ItemDataRequestStatus;
import com.amazon.inapp.purchasing.PurchaseResponse.PurchaseRequestStatus;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse.PurchaseUpdatesRequestStatus;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseData;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseDataStorage;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.SKUData;
import com.squallium.commons.inapp.amazon.AppPurchasingObserverListener;
import com.squallium.commons.inapp.amazon.MySKU;
import com.squallium.commons.inapp.sample.R;

public class MainActivity extends Activity implements
		AppPurchasingObserverListener {

	// Wrapper around SharedPreferences to save request state
	// and purchase receipt data
	private PurchaseDataStorage purchaseDataStorage;

	/**
	 * Setup IAP SDK and other UI related objects specific to this sample
	 * application.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupApplicationSpecificOnCreate();

		setupIAPOnCreate();
	}

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
	 * Calls {@link PurchasingManager#initiateGetUserIdRequest()} to get current
	 * userId and {@link PurchasingManager#initiateItemDataRequest(Set)} with
	 * the list of SKUs to verify the SKUs are valid in the Appstore.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "onResume: call initiateGetUserIdRequest");
		PurchasingManager.initiateGetUserIdRequest();

		Log.i(TAG,
				"onResume: call initiateItemDataRequest for skus: "
						+ MySKU.getAll());
		PurchasingManager.initiateItemDataRequest(MySKU.getAll());
	}

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
	 * Callback for a successful get user id response
	 * {@link GetUserIdResponseStatus#SUCCESSFUL}.
	 * 
	 * In this sample app, if the user changed from the previously stored user,
	 * this method updates the display based on purchase data stored for the
	 * user in SharedPreferences.  The orange consumable is fulfilled
	 * if a stored purchase token was found to NOT be fulfilled or if the SKU
	 * should be fulfilled.
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

				updateOrangesInView(skuData.getHaveQuantity());

				purchaseDataStorage.setPurchaseTokenFulfilled(purchaseData
						.getPurchaseToken());
				purchaseDataStorage.setRequestStateFulfilled(requestId);
			} else {
				Log.i(TAG, "onGetUserIdResponseSuccessful: for purchaseToken ("
						+ purchaseData.getPurchaseToken()
						+ ") call fulfillSKU on SKU: " + purchaseData.getSKU());
				final int haveQuantity = skuData.getHaveQuantity();
				final int consumedQuantity = skuData.getConsumedQuantity();

				Log.i(TAG,
						"onGetUserIdResponseSuccessful: call updateOrangesInView, have ("
								+ haveQuantity + ") oranges and consumed ("
								+ consumedQuantity + ") oranges");
				updateOrangesInView(haveQuantity, consumedQuantity);
			}
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
	 * Callback for item data response with unavailable SKUs. This means that
	 * these unavailable SKUs are NOT accessible in developer portal. In this
	 * sample app, we would disable the buy button for these SKUs
	 * 
	 * @param unavailableSkus
	 */
	@Override
	public void onItemDataResponseSuccessfulWithUnavailableSkus(
			Set<String> unavailableSkus) {
		disableButtonsForUnavailableSkus(unavailableSkus);
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
			if (MySKU.ORANGE.getSku().equals(sku)) {
				enableBuyOrangeButton();
			}
		}
	}

	/**
	 * Callback for failed item data response
	 * {@link ItemDataRequestStatus#FAILED}.
	 * 
	 * @param requestId
	 */
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
		SKUData skuData = purchaseDataStorage.getSKUData(sku);
		if (skuData == null)
			return;

		if (MySKU.ORANGE.getSku().equals(skuData.getSKU())) {
			updateOrangesInView(skuData.getHaveQuantity());
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
				+ ") sku ("+sku+")");
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
		Log.i(TAG, "onPurchaseResponseInvalidSKU: for userId (" + userId + ") sku ("+sku+")");
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
				+ ") sku ("+sku+")");
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

	// ///////////////////////////////////////////////////////////////////////////////////////
	// ////////////////////////// Application specific code below
	// ////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////////////////

	private static final String TAG = "SampleIAPConsumablesApp";
	protected Handler guiThreadHandler;

	protected Button buyOrangeButton;
	protected Button eatOrangeButton;

	protected TextView numOranges;
	protected TextView numOrangesConsumed;

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
}
