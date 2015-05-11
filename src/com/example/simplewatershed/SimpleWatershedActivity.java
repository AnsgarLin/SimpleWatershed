package com.example.simplewatershed;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;

import com.example.simplewatershed.view.imagecontainer.ImageContainer;

public class SimpleWatershedActivity extends Activity {
	private static final int IMAGE_LOAD = 100;
	/**
	 * private Load openCV library
	 */
	static {
		if (!OpenCVLoader.initDebug()) {
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_watershed);

		// This is needed to set the reference of base image in container
		((ImageContainer) findViewById(R.id.base_image_container)).initView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.stick_maker_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.menu_load:
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("image/*");
			startActivityForResult(intent, IMAGE_LOAD);
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case IMAGE_LOAD:
			if (resultCode == Activity.RESULT_OK) {
				if (data == null) {
					// PickerActivity is finished with nothing
					return;
				}
				try {
					((ImageContainer) findViewById(R.id.base_image_container)).setImage(MediaStore.Images.Media.getBitmap(this.getContentResolver(),
							data.getData()));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}
}
