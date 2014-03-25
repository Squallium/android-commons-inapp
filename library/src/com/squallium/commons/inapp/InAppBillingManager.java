package com.squallium.commons.inapp;

public class InAppBillingManager {

	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private static InAppBillingManager instance;

	private Store mStore;

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

	// ===========================================================
	// Getter & Setter
	// ===========================================================

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
