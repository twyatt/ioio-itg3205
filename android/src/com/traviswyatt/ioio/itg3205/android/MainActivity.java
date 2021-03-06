package com.traviswyatt.ioio.itg3205.android;

import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import com.traviswyatt.ioio.itg3205.ITG3205;

public class MainActivity extends IOIOActivity {

	private TextView ioioStatusText;
	private TextView deviceIdText;
	private TextView xAxisText;
	private TextView yAxisText;
	private TextView zAxisText;
	private TextView temperatureText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ioioStatusText = (TextView) findViewById(R.id.ioio_status);
		deviceIdText = (TextView) findViewById(R.id.device_id);
		xAxisText = (TextView) findViewById(R.id.x_axis);
		yAxisText = (TextView) findViewById(R.id.y_axis);
		zAxisText = (TextView) findViewById(R.id.z_axis);
		temperatureText = (TextView) findViewById(R.id.temperature);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		int twiNum = 1; // IOIO pin 1 = SDA, pin 2 = SCL
		final ITG3205 itg3205 = new ITG3205(twiNum, TwiMaster.Rate.RATE_100KHz);
		itg3205.setListener(new ITG3205.ITG3205Listener() {
			@Override
			public void onDeviceId(byte deviceId) {
				updateTextView(deviceIdText, "Device ID: " + (int) (deviceId & 0xFF));
			}
			@Override
			public void onData(int x, int y, int z, int temperature) {
				updateTextView(xAxisText, "X = " + ((float) x / 14.375f) + " deg/s");
				updateTextView(yAxisText, "Y = " + ((float) y / 14.375f) + " deg/s");
				updateTextView(zAxisText, "Z = " + ((float) z / 14.375f) + " deg/s");
				updateTextView(temperatureText, "Temperature = " + (35f + (float) (temperature + 13200) / 280f) + " C");
			}
			@Override
			public void onError(String message) {
				// TODO Auto-generated method stub
			}
		});
		return new DeviceLooper(itg3205);
	}
	
	private void updateTextView(final TextView textView, final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView.setText(text);
			}
		});
	}
	
	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class DeviceLooper implements IOIOLooper {
		
		/**
		 * Duration to sleep after each loop.
		 */
		private static final long THREAD_SLEEP = 10L; // milliseconds
		
		private IOIOLooper device;

		public DeviceLooper(IOIOLooper device) {
			this.device = device;
		}
		
		@Override
		public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
			updateTextView(ioioStatusText, "IOIO Connected");
			device.setup(ioio);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException 
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			device.loop();
			Thread.sleep(THREAD_SLEEP);
		}

		@Override
		public void disconnected() {
			updateTextView(ioioStatusText, "IOIO Disconnected");
			device.disconnected();
		}

		@Override
		public void incompatible() {
			
		}

		@Override
		public void incompatible(IOIO ioio) {
			updateTextView(ioioStatusText, "IOIO Incompatible");
		}
	}

}
