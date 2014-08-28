package com.traviswyatt.ioio.itg3205.desktop;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.pc.IOIOConsoleApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.traviswyatt.ioio.itg3205.ITG3205;

public class Main extends IOIOConsoleApp {
	
	private ITG3205 itg3205;
	private boolean ledOn = false;

	// Boilerplate main(). Copy-paste this code into any IOIOapplication.
	public static void main(String[] args) throws Exception {
		new Main().go(args);
	}

	@Override
	protected void run(String[] args) throws IOException {
		int twiNum = 1; // IOIO pin 1 = SDA, pin 2 = SCL
		itg3205 = new ITG3205(twiNum, TwiMaster.Rate.RATE_400KHz);
		itg3205.setListener(new ITG3205.ITG3205Listener() {
			
			@Override
			public void onDeviceId(byte deviceId) {
				System.out.println("Device ID: " + (int) (deviceId & 0xFF));
			}
			
			@Override
			public void onData(int x, int y, int z, int temperature) {
				System.out.println("X = " + ((float) x / 14.375f) + " deg/s\tY = " + ((float) y / 14.375f) + " deg/s\tZ = " + ((float) z / 14.375f) + " deg/s\tTemperature = " + (35f + (float) (temperature + 13200) / 280f) + " C");
			}
			
			@Override
			public void onError(String message) {
				System.err.println("ITG3205 Error: " + message);
			}
			
		});
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		boolean abort = false;
		String line;
		while (!abort && (line = reader.readLine()) != null) {
			if (line.equals("t")) {
				ledOn = !ledOn;
			} else if (line.equals("n")) {
				ledOn = true;
			} else if (line.equals("f")) {
				ledOn = false;
			} else if (line.equals("q")) {
				abort = true;
			} else {
				System.out.println("Unknown input. t=toggle, n=on, f=off, q=quit.");
			}
		}
	}

	@Override
	public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
		return new IOIOLooper() {
			
			private DigitalOutput led;
			
			@Override
			public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
				led = ioio.openDigitalOutput(IOIO.LED_PIN, true);
				itg3205.setup(ioio);
			}

			@Override
			public void loop() throws ConnectionLostException, InterruptedException {
				led.write(!ledOn);
				itg3205.loop();
			}

			@Override
			public void disconnected() {
				// TODO Auto-generated method stub
			}

			@Override
			public void incompatible() {
				// TODO Auto-generated method stub
			}

			@Override
			public void incompatible(IOIO ioio) {
				// TODO Auto-generated method stub
			}
			
		};
	}
}
