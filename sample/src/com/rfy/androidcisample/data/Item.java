package com.rfy.androidcisample.data;

import android.util.Log;

import com.rfy.androidcilibrary.AbstractEntity;

public class Item extends AbstractEntity {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = Item.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================

	private String name;

	private String description;

	private Double price;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Item(String name, String description, Double price) {
		super();
		Log.d(TAG, "New item created");
		this.name = name;
		this.description = description;
		this.price = price;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
