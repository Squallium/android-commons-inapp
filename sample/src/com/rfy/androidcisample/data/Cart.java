package com.rfy.androidcisample.data;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.rfy.androidcilibrary.AbstractEntity;

public class Cart extends AbstractEntity {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = Cart.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================

	private List<Item> shoppingCart;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public void addItem(String name, String description, Double price) {
		Log.d(TAG, "Item added into cart");
		if (shoppingCart == null) {
			shoppingCart = new ArrayList<Item>();
		}
		shoppingCart.add(new Item(name, description, price));
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public List<Item> getShoppingCart() {
		return shoppingCart;
	}

	public void setShoppingCart(List<Item> shoppingCart) {
		this.shoppingCart = shoppingCart;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
