package com.squallium.commons.inapp.sample.az;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.Activity;
import android.graphics.Color;
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
import com.squallium.commons.inapp.InAppBillingManager;
import com.squallium.commons.inapp.InAppBillingManager.Store;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseData;
import com.squallium.commons.inapp.amazon.AppPurchasingObserver.PurchaseDataStorage;
import com.squallium.commons.inapp.amazon.AppPurchasingObserverListener;
import com.squallium.commons.inapp.sample.R;

/**
 * Sample code for IAP entitlements
 * 
 * This is the main activity for this project that shows how to call the
 * PurchasingManager methods and how to get notified through the
 * {@link AppPurchasingObserverListener} callbacks.
 */
public class MainActivityNonCon extends Activity implements
		AppPurchasingObserverListener {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = MainActivityNonCon.class.getSimpleName();

	private static final String LEVEL2 = "com.amazon.sample.iap.entitlement.level2";

	// ===========================================================
	// Fields
	// ===========================================================

	// Wrapper around SharedPreferences to save request state
	// and purchase receipt data
	private PurchaseDataStorage purchaseDataStorage;

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

		setupIAPOnCreate();
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
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
	 * Calls {@link PurchasingManager#initiateGetUserIdRequest()} to get current
	 * userId and {@link PurchasingManager#initiateItemDataRequest(Set)} with
	 * the list of SKUs to verify the SKUs are valid in the Appstore.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "onResume: call initiateGetUserIdRequest");
		PurchasingManager.initiateGetUserIdRequest();

		Log.i(TAG, "onResume: call initiateItemDataRequest for skus: "
				+ InAppBillingManager.getInstance(Store.amazon).getAllSkus());
		PurchasingManager.initiateItemDataRequest(InAppBillingManager
				.getInstance(Store.amazon).getAllSkus());
	}

	/**
	 * Click handler called when user clicks button to buy access to level 2
	 * entitlement. This method calls
	 * {@link PurchasingManager#initiatePurchaseRequest(String)} with the SKU
	 * for the level 2 entitlement.
	 */
	public void onBuyAccessToLevel2Click(View view) {
		String requestId = PurchasingManager.initiatePurchaseRequest(LEVEL2);
		PurchaseData purchaseData = purchaseDataStorage
				.newPurchaseData(requestId);
		Log.i(TAG, "onBuyAccessToLevel2Click: requestId (" + requestId
				+ ") requestState (" + purchaseData.getRequestState() + ")");
	}

	/**
	 * Callback for a successful get user id response
	 * {@link GetUserIdResponseStatus#SUCCESSFUL}.
	 * 
	 * In this sample app, if the user changed from the previously stored user,
	 * this method updates the display based on purchase data stored for the
	 * user in SharedPreferences. The level 2 entitlement is fulfilled if a
	 * stored purchase token was found to NOT be fulfilled or if the SKU should
	 * be fulfilled.
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

		// Reset to original setting where level2 is disabled
		disableLevel2InView();

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

			String purchaseToken = purchaseData.getPurchaseToken();
			String sku = purchaseData.getSKU();
			if (!purchaseData.isPurchaseTokenFulfilled()) {
				Log.i(TAG, "onGetUserIdResponseSuccess: requestId ("
						+ requestId + ") userId (" + userId + ") sku (" + sku
						+ ") purchaseToken (" + purchaseToken
						+ ") was NOT fulfilled, fulfilling purchase now");
				onPurchaseResponseSuccess(userId, sku, purchaseToken);

				purchaseDataStorage.setPurchaseTokenFulfilled(purchaseToken);
				purchaseDataStorage.setRequestStateFulfilled(requestId);
			} else {
//				boolean shouldFulfillSKU = purchaseDataStorage
//						.shouldFulfillSKU(sku);
//				if (shouldFulfillSKU) {
//					Log.i(TAG,
//							"onGetUserIdResponseSuccess: should fulfill sku ("
//									+ sku
//									+ ") is true, so fulfilling purchasing now");
//					onPurchaseUpdatesResponseSuccess(userId, sku, purchaseToken);
//				}
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
	 * Callback for successful item data response with unavailable SKUs
	 * {@link ItemDataRequestStatus#SUCCESSFUL_WITH_UNAVAILABLE_SKUS}. This
	 * means that these unavailable SKUs are NOT accessible in developer portal.
	 * 
	 * In this sample app, we disable the buy button for these SKUs.
	 * 
	 * @param unavailableSkus
	 *            - skus that are not valid in developer portal
	 */
	@Override
	public void onItemDataResponseSuccessfulWithUnavailableSkus(
			Set<String> unavailableSkus) {
		Log.i(TAG, "onItemDataResponseSuccessfulWithUnavailableSkus: for ("
				+ unavailableSkus.size() + ") unavailableSkus");
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
			if (LEVEL2.equals(sku)) {
				enableBuyLevel2Button();
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
	 * Callback on successful purchase response
	 * {@link PurchaseRequestStatus#SUCCESSFUL}. In this sample app, we show
	 * level 2 as enabled
	 * 
	 * @param sku
	 */
	@Override
	public void onPurchaseResponseSuccess(String userId, String sku,
			String purchaseToken) {
		Log.i(TAG, "onPurchaseResponseSuccess: for userId (" + userId
				+ ") sku (" + sku + ") purchaseToken (" + purchaseToken + ")");
		enableEntitlementForSKU(sku);
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
		Log.i(TAG, "onPurchaseResponseAlreadyEntitled: for userId (" + userId
				+ ") sku (" + sku + ")");
		// For entitlements, even if already entitled, make sure to enable.
		enableEntitlementForSKU(sku);
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
	 * In this sample app, we show level 2 as enabled.
	 * 
	 * @param userId
	 * @param sku
	 * @param purchaseToken
	 */
	@Override
	public void onPurchaseUpdatesResponseSuccess(String userId, String sku,
			String purchaseToken) {
		Log.i(TAG, "onPurchaseUpdatesResponseSuccess: for userId (" + userId
				+ ") sku (" + sku + ") purchaseToken (" + purchaseToken + ")");
		enableEntitlementForSKU(sku);
	}

	/**
	 * Callback on successful purchase updates response
	 * {@link PurchaseUpdatesRequestStatus#SUCCESSFUL} for revoked SKU.
	 * 
	 * In this sample app, we revoke fulfillment if level 2 sku has been revoked
	 * by showing level 2 as disabled
	 * 
	 * @param userId
	 * @param revokedSKU
	 */
	@Override
	public void onPurchaseUpdatesResponseSuccessRevokedSku(String userId,
			String revokedSku) {
		Log.i(TAG, "onPurchaseUpdatesResponseSuccessRevokedSku: for userId ("
				+ userId + ") revokedSku (" + revokedSku + ")");
		if (!LEVEL2.equals(revokedSku))
			return;

		Log.i(TAG,
				"onPurchaseUpdatesResponseSuccessRevokedSku: disabling play level 2 button");
		disableLevel2InView();

		Log.i(TAG,
				"onPurchaseUpdatesResponseSuccessRevokedSku: fulfilledCountDown for revokedSKU ("
						+ revokedSku + ")");
	}

	/**
	 * Callback on failed purchase updates response
	 * {@link PurchaseUpdatesRequestStatus#FAILED}
	 * 
	 * @param requestId
	 */
	public void onPurchaseUpdatesResponseFailed(String requestId) {
		Log.i(TAG, "onPurchaseUpdatesResponseFailed: for requestId ("
				+ requestId + ")");
	}

	private Handler guiThreadHandler;

	// Button to buy entitlement to level 2
	private Button buyLevel2Button;

	// TextView shows whether user has been entitled to level 2
	private TextView isLevel2EnabledTextView;

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
}
