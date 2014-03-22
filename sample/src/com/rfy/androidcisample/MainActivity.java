package com.rfy.androidcisample;

import com.rfy.androidcisample.data.Cart;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Instance a new cart
		Cart cart = new Cart();
		cart.addItem("Orange", "Fruit", 3.99d);
		cart.addItem("T-Shirt", "Clothes", 9.99d);
		
		// Mostramos la info por pantalla
		TextView textView = (TextView) findViewById(R.id.textView1);
		textView.setText(cart.toString());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
