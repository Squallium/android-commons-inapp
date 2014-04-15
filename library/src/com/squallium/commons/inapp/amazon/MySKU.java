package com.squallium.commons.inapp.amazon;

import com.squallium.commons.inapp.InAppBilling.InAppType;

public class MySKU {

	private InAppType inAppType;
	private String sku;
	private int quantity;

	public MySKU(InAppType inAppType, String sku) {
		this.inAppType = inAppType;
		this.sku = sku;
		this.quantity = 0;
	}

	public MySKU(InAppType inAppType, String sku, int quantity) {
		this(inAppType, sku);
		this.quantity = quantity;
	}

	public String getSku() {
		return sku;
	}

	public int getQuantity() {
		return quantity;
	}

	public InAppType getInAppType() {
		return inAppType;
	}

	public void setInAppType(InAppType inAppType) {
		this.inAppType = inAppType;
	}
}
