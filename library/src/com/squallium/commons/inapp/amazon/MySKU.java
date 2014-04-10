package com.squallium.commons.inapp.amazon;

import java.util.HashSet;
import java.util.Set;

public enum MySKU {

	ORANGE("com.amazon.sample.iap.consumable.orange", 1);

	private String sku;
	private int quantity;

	private MySKU(String sku, int quantity) {
		this.sku = sku;
		this.quantity = quantity;
	}

	public static MySKU valueForSKU(String sku) {
		if (ORANGE.getSku().equals(sku)) {
			return ORANGE;
		}
		return null;
	}

	public String getSku() {
		return sku;
	}

	public int getQuantity() {
		return quantity;
	}

	private static Set<String> SKUS = new HashSet<String>();
	static {
		SKUS.add(ORANGE.getSku());
	}

	public static Set<String> getAll() {
		return SKUS;
	}

}
