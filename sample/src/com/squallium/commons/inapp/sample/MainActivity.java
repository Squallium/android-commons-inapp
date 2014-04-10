/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squallium.commons.inapp.sample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.squallium.commons.inapp.GoogleInAppBilling;
import com.squallium.commons.inapp.IInAppBilling;
import com.squallium.commons.inapp.google.IabResult;
import com.squallium.commons.inapp.google.Inventory;
import com.squallium.commons.inapp.google.Purchase;

/**
 * Example game using in-app billing version 3.
 * 
 * Before attempting to run this sample, please read the README file. It
 * contains important information on how to set up this project.
 * 
 * All the game-specific logic is implemented here in MainActivity, while the
 * general-purpose boilerplate that can be reused in any app is provided in the
 * classes in the util/ subdirectory. When implementing your own application,
 * you can copy over util/*.java to make use of those utility classes.
 * 
 * This game is a simple "driving" game where the player can buy gas and drive.
 * The car has a tank which stores gas. When the player purchases gas, the tank
 * fills up (1/4 tank at a time). When the player drives, the gas in the tank
 * diminishes (also 1/4 tank at a time).
 * 
 * The user can also purchase a "premium upgrade" that gives them a red car
 * instead of the standard blue one (exciting!).
 * 
 * The user can also purchase a subscription ("infinite gas") that allows them
 * to drive without using up any gas while that subscription is active.
 * 
 * It's important to note the consumption mechanics for each item.
 * 
 * PREMIUM: the item is purchased and NEVER consumed. So, after the original
 * purchase, the player will always own that item. The application knows to
 * display the red car instead of the blue one because it queries whether the
 * premium "item" is owned or not.
 * 
 * INFINITE GAS: this is a subscription, and subscriptions can't be consumed.
 * 
 * GAS: when gas is purchased, the "gas" item is then owned. We consume it when
 * we apply that item's effects to our app's world, which to us means filling up
 * 1/4 of the tank. This happens immediately after purchase! It's at this point
 * (and not when the user drives) that the "gas" item is CONSUMED. Consumption
 * should always happen when your game world was safely updated to apply the
 * effect of the purchase. So, in an example scenario:
 * 
 * BEFORE: tank at 1/2 ON PURCHASE: tank at 1/2, "gas" item is owned
 * IMMEDIATELY: "gas" is consumed, tank goes to 3/4 AFTER: tank at 3/4, "gas"
 * item NOT owned any more
 * 
 * Another important point to notice is that it may so happen that the
 * application crashed (or anything else happened) after the user purchased the
 * "gas" item, but before it was consumed. That's why, on startup, we check if
 * we own the "gas" item, and, if so, we have to apply its effects to our world
 * and consume it. This is also very important!
 * 
 * @author Bruno Oliveira (Google)
 */
public class MainActivity extends GoogleInAppBilling {

	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// Debug tag, for logging
	static final String TAG = "TrivialDrive";

	// Does the user have the premium upgrade?
	boolean mIsPremium = false;

	// Does the user have an active subscription to the infinite gas plan?
	boolean mSubscribedToInfiniteGas = false;

	// SKUs for our products: the premium upgrade (non-consumable) and gas
	// (consumable)
	static final String SKU_PREMIUM = "premium";
	static final String SKU_GAS = "gas";

	// SKU for our subscription (infinite gas)
	static final String SKU_INFINITE_GAS = "infinite_gas";

	// (arbitrary) request code for the purchase flow
	static final int RC_REQUEST = 10001;

	// Graphics for the gas gauge
	static int[] TANK_RES_IDS = { R.drawable.gas0, R.drawable.gas1,
			R.drawable.gas2, R.drawable.gas3, R.drawable.gas4 };

	// How many units (1/4 tank is our unit) fill in the tank.
	static final int TANK_MAX = 4;

	// Current amount of gas in tank, in units
	int mTank;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// load game data
		loadData();

		/*
		 * base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY (that
		 * you got from the Google Play developer console). This is not your
		 * developer public key, it's the *app-specific* public key.
		 * 
		 * Instead of just storing the entire literal string here embedded in
		 * the program, construct the key at runtime from pieces or use bit
		 * manipulation (for example, XOR with some other string) to hide the
		 * actual key. The key itself is not secret information, but we don't
		 * want to make it easy for an attacker to replace the public key with
		 * one of their own and then fake messages from the server.
		 */

	}

	@Override
	protected String getBase64EncodedPublicKey() {
		return "";
	}

	@Override
	protected void checkInventoryItems(Inventory inventory) {
		// Do we have the premium upgrade?
		Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
		mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
		Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));

		// Do we have the infinite gas plan?
		Purchase infiniteGasPurchase = inventory.getPurchase(SKU_INFINITE_GAS);
		mSubscribedToInfiniteGas = (infiniteGasPurchase != null && verifyDeveloperPayload(infiniteGasPurchase));
		Log.d(TAG, "User "
				+ (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE")
				+ " infinite gas subscription.");
		if (mSubscribedToInfiniteGas)
			mTank = TANK_MAX;

		// Check for gas delivery -- if we own gas, we should fill up the
		// tank immediately
		Purchase gasPurchase = inventory.getPurchase(SKU_GAS);
		consumeItem(gasPurchase, mConsumeItemListener);

		updateUi();
		setWaitScreen(false);
		Log.d(TAG, "Initial inventory query finished; enabling main UI.");
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// User clicked the "Buy Gas" button
	public void onBuyGasButtonClicked(View arg0) {
		Log.d(TAG, "Buy gas button clicked.");

		if (mSubscribedToInfiniteGas) {
			complain("No need! You're subscribed to infinite gas. Isn't that awesome?");
			return;
		}

		if (mTank >= TANK_MAX) {
			complain("Your tank is full. Drive around a bit!");
			return;
		}

		// launch the gas purchase UI flow.
		// We will be notified of completion via mPurchaseFinishedListener
		setWaitScreen(true);
		Log.d(TAG, "Launching purchase flow for gas.");

		/*
		 * TODO: for security, generate your payload here for verification. See
		 * the comments on verifyDeveloperPayload() for more info. Since this is
		 * a SAMPLE, we just use an empty string, but on a production app you
		 * should carefully generate this.
		 */
		String payload = "";

		purchase(InAppType.consumable, SKU_GAS, RC_REQUEST,
				mPurchaseFinishedListener, payload);
	}

	// User clicked the "Upgrade to Premium" button.
	public void onUpgradeAppButtonClicked(View arg0) {
		Log.d(TAG,
				"Upgrade button clicked; launching purchase flow for upgrade.");
		setWaitScreen(true);

		/*
		 * TODO: for security, generate your payload here for verification. See
		 * the comments on verifyDeveloperPayload() for more info. Since this is
		 * a SAMPLE, we just use an empty string, but on a production app you
		 * should carefully generate this.
		 */
		String payload = "";

		purchase(InAppType.non_consumable, SKU_PREMIUM, RC_REQUEST,
				mPurchaseFinishedListener, payload);
	}

	// "Subscribe to infinite gas" button clicked. Explain to user, then start
	// purchase
	// flow for subscription.
	public void onInfiniteGasButtonClicked(View arg0) {
		if (!isSubscriptionSupported()) {
			complain("Subscriptions not supported on your device yet. Sorry!");
			return;
		}

		/*
		 * TODO: for security, generate your payload here for verification. See
		 * the comments on verifyDeveloperPayload() for more info. Since this is
		 * a SAMPLE, we just use an empty string, but on a production app you
		 * should carefully generate this.
		 */
		String payload = "";

		setWaitScreen(true);
		Log.d(TAG, "Launching purchase flow for infinite gas subscription.");
		purchase(InAppType.subscription, SKU_INFINITE_GAS, RC_REQUEST,
				mPurchaseFinishedListener, payload);
	}

	// Callback for when a purchase is finished
	IInAppBilling.OnPurchaseFinishedListener mPurchaseFinishedListener = new IInAppBilling.OnPurchaseFinishedListener() {
		public void onPurchaseFinished(IabResult result, Purchase purchase) {
			if (result.isFailure()) {
				complain("Error purchasing: " + result);
				setWaitScreen(false);
				return;
			}
			if (!verifyDeveloperPayload(purchase)) {
				complain("Error purchasing. Authenticity verification failed.");
				setWaitScreen(false);
				return;
			}

			Log.d(TAG, "Purchase successful.");

			if (purchase.getSku().equals(SKU_GAS)) {
				// bought 1/4 tank of gas. So consume it.
				Log.d(TAG, "Purchase is gas. Starting gas consumption.");
				consumeItem(purchase, mConsumeItemListener);
			} else if (purchase.getSku().equals(SKU_PREMIUM)) {
				// bought the premium upgrade!
				Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
				alert("Thank you for upgrading to premium!");
				mIsPremium = true;
				updateUi();
				setWaitScreen(false);
			} else if (purchase.getSku().equals(SKU_INFINITE_GAS)) {
				// bought the infinite gas subscription
				Log.d(TAG, "Infinite gas subscription purchased.");
				alert("Thank you for subscribing to infinite gas!");
				mSubscribedToInfiniteGas = true;
				mTank = TANK_MAX;
				updateUi();
				setWaitScreen(false);
			}
		}
	};

	// Drive button clicked. Burn gas!
	public void onDriveButtonClicked(View arg0) {
		Log.d(TAG, "Drive button clicked.");
		if (!mSubscribedToInfiniteGas && mTank <= 0)
			alert("Oh, no! You are out of gas! Try buying some!");
		else {
			if (!mSubscribedToInfiniteGas)
				--mTank;
			saveData();
			alert("Vroooom, you drove a few miles.");
			updateUi();
			Log.d(TAG, "Vrooom. Tank is now " + mTank);
		}
	}

	// updates UI to reflect model
	public void updateUi() {
		// update the car color to reflect premium status or lack thereof
		((ImageView) findViewById(R.id.free_or_premium))
				.setImageResource(mIsPremium ? R.drawable.premium
						: R.drawable.free);

		// "Upgrade" button is only visible if the user is not premium
		findViewById(R.id.upgrade_button).setVisibility(
				mIsPremium ? View.GONE : View.VISIBLE);

		// "Get infinite gas" button is only visible if the user is not
		// subscribed yet
		findViewById(R.id.infinite_gas_button).setVisibility(
				mSubscribedToInfiniteGas ? View.GONE : View.VISIBLE);

		// update gas gauge to reflect tank status
		if (mSubscribedToInfiniteGas) {
			((ImageView) findViewById(R.id.gas_gauge))
					.setImageResource(R.drawable.gas_inf);
		} else {
			int index = mTank >= TANK_RES_IDS.length ? TANK_RES_IDS.length - 1
					: mTank;
			((ImageView) findViewById(R.id.gas_gauge))
					.setImageResource(TANK_RES_IDS[index]);
		}
	}

	// Enables or disables the "please wait" screen.
	void setWaitScreen(boolean set) {
		findViewById(R.id.screen_main).setVisibility(
				set ? View.GONE : View.VISIBLE);
		findViewById(R.id.screen_wait).setVisibility(
				set ? View.VISIBLE : View.GONE);
	}

	void saveData() {

		/*
		 * WARNING: on a real application, we recommend you save data in a
		 * secure way to prevent tampering. For simplicity in this sample, we
		 * simply store the data using a SharedPreferences.
		 */

		SharedPreferences.Editor spe = getPreferences(MODE_PRIVATE).edit();
		spe.putInt("tank", mTank);
		spe.commit();
		Log.d(TAG, "Saved data: tank = " + String.valueOf(mTank));
	}

	void loadData() {
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		mTank = sp.getInt("tank", 2);
		Log.d(TAG, "Loaded data: tank = " + String.valueOf(mTank));
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	// Called when consumption is complete
	IInAppBilling.OnConsumeItemListener mConsumeItemListener = new IInAppBilling.OnConsumeItemListener() {
		public void onConsumeItem(Purchase purchase, IabResult result) {
			// We know this is the "gas" sku because it's the only one we
			// consume,
			// so we don't check which sku was consumed. If you have more than
			// one
			// sku, you probably should check...
			if (result.isSuccess()) {
				// successfully consumed, so we apply the effects of the item in
				// our
				// game world's logic, which in our case means filling the gas
				// tank a bit
				Log.d(TAG, "Consumption successful. Provisioning.");
				mTank = mTank == TANK_MAX ? TANK_MAX : mTank + 1;
				saveData();
				alert("You filled 1/4 tank. Your tank is now "
						+ String.valueOf(mTank) + "/4 full!");
			} else {
				complain("Error while consuming: " + result);
			}
			updateUi();
			setWaitScreen(false);
		}
	};
}
