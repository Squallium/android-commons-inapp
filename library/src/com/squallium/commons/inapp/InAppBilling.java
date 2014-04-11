package com.squallium.commons.inapp;

import com.squallium.commons.inapp.google.Purchase;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;

public abstract class InAppBilling extends Activity implements IInAppBilling {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final String TAG = InAppBilling.class.getSimpleName();

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	protected void complain(String message) {
		Log.e(TAG, "**** TrivialDrive Error: " + message);
		alert("Error: " + message);
	}

	protected void alert(String message) {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setMessage(message);
		bld.setNeutralButton("OK", null);
		Log.d(TAG, "Showing alert dialog: " + message);
		bld.create().show();
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

	public enum InAppType {
		consumable, non_consumable, subscription;
	}

	public class InAppResult {
		public Purchase purchase;
	}

}
