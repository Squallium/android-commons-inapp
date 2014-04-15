package com.squallium.commons.inapp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.squallium.commons.inapp.amazon.MySKU;

public class InAppBillingManager {

	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private static InAppBillingManager instance;

	private Store mStore;

	private Map<String, MySKU> mSkus;

	// ===========================================================
	// Constructors
	// ===========================================================

	private InAppBillingManager(Store pStore) {
		this.mStore = pStore;
	}

	public static InAppBillingManager getInstance(Store pStore) {
		if (instance == null) {
			instance = new InAppBillingManager(pStore);
		}
		return instance;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void addSku(MySKU mySKU) {
		if (mSkus == null) {
			mSkus = new HashMap<String, MySKU>();
		}
		mSkus.put(mySKU.getSku(), mySKU);
	}

	public MySKU getSku(String sku) {
		MySKU result = null;

		if (mSkus != null && mSkus.containsKey(sku)) {
			result = mSkus.get(sku);
		}

		return result;
	}

	public Set<String> getAllSkus() {
		Set<String> result = null;
		if (mSkus != null && !mSkus.isEmpty()) {
			result = mSkus.keySet();
		}
		return result;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public Store getStore() {
		return mStore;
	}

	public void setStore(Store pStore) {
		this.mStore = pStore;
	}

	public Map<String, MySKU> getSkus() {
		return mSkus;
	}

	public void setSkus(Map<String, MySKU> pSkus) {
		this.mSkus = pSkus;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public enum Store {
		google, amazon, nook
	}

	public enum ItemType {
		consumable, nonConsumable, subscription
	}

}
