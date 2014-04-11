package com.squallium.commons.inapp;

import java.util.Set;

import android.content.Intent;

import com.squallium.commons.inapp.InAppBilling.InAppResult;
import com.squallium.commons.inapp.google.IabResult;
import com.squallium.commons.inapp.google.Purchase;

public interface IInAppBilling {

	void customOnActivityResult(int requestCode, int resultCode, Intent data);

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
		 * 
		 * @param result
		 *            The result of the purchase.
		 * @param info
		 *            The purchase information (null if purchase failed)
		 */
		public void onPurchaseSuccess(InAppResult inAppResult, String sku);

		/**
		 * Called when ocurrs any error during the purchase
		 * 
		 * @param result
		 * @param message
		 */
		public void onPurchaseFailed(InAppResult inAppResult, String sku,
				String message);
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
