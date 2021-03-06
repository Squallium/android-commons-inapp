package com.squallium.commons.inapp;

import java.util.Set;

import android.content.Intent;

import com.amazon.inapp.purchasing.PurchasingManager;
import com.squallium.commons.inapp.InAppBilling.InAppResult;
import com.squallium.commons.inapp.google.IabResult;
import com.squallium.commons.inapp.google.Purchase;

public interface IInAppBilling {

	void customOnActivityResult(int requestCode, int resultCode, Intent data);

	/**
	 * Callback that notifies when the user changed
	 */
	public interface OnUserChangedListener {
		public void onUserChanged();
	}

	/**
	 * Callback that notifies when a item sku is available
	 */
	public interface OnItemSkuAvailableListener {
		/**
		 * 
		 * @param sku
		 */
		public void onItemSkuAvailable(String sku);
	}

	/**
	 * Callback that notifies when a item sku is unavailable
	 */
	public interface OnItemSkuUnavailableListener {
		/**
		 * 
		 * @param unavailableSkus
		 */
		public void onItemSkuUnavailable(Set<String> unavailableSkus);
	}

	/**
	 * Callback that notifies when a purchase is finished.
	 */
	public interface OnPurchaseFinishedListener {
		/**
		 * Called to notify that an in-app purchase finished. If the purchase
		 * was successful, then the sku parameter specifies which item was
		 * purchased. If the purchase failed, the sku and extraData parameters
		 * may or may not be null, depending on how far the purchase process
		 * went.
		 */
		public void onPurchaseSuccess(InAppResult inAppResult, String sku);

		/**
		 * Called when the item is already entitle
		 */
		public void onPurchaseAlreadyEntitled(InAppResult inAppResult,
				String sku);

		/**
		 * Called when ocurrs any error during the purchase
		 */
		public void onPurchaseFailed(InAppResult inAppResult, String sku,
				String message);
	}

	/**
	 * Callback for posibles resposes of
	 * {@link PurchasingManager#initiatePurchaseUpdatesRequest}
	 */
	public interface OnPurchaseUpdatesResponseListener {

		/**
		 * Called when a purchase update is success
		 */
		public void onPurchaseUpdatesSuccess(InAppResult inAppResult,
				String sku, String purchaseToken);

		/**
		 * Called when a purchase update is success but the sku was revoked
		 */
		public void onPurchaseUpdatesRevokedSku(InAppResult inAppResult,
				String revokedSku);

		/**
		 * Called when a purchase updates failed
		 */
		public void onPurchaseUpdatesFailed(InAppResult inAppResult,
				String requestId);
	}

	/**
	 * Callback that notifies when a consumption operation finishes.
	 */
	public interface OnConsumeItemListener {
		/**
		 * Called to notify that a consumption has finished.
		 * 
		 * @param purchase
		 *            The purchase that was (or was to be) consumed.
		 * @param result
		 *            The result of the consumption operation.
		 */
		public void onConsumeItem(Purchase purchase, IabResult result);
	}

}
