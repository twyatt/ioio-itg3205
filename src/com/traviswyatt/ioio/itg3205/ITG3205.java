package com.traviswyatt.ioio.itg3205;

import ioio.lib.api.IOIO;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.TwiMaster.Rate;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;

public class ITG3205 implements IOIOLooper {
	
	/**
	 * Duration to sleep thread after a register write.
	 */
	public static final long REGISTER_WRITE_DELAY = 200L;
	
	public static final Rate CLOCK_RATE = Rate.RATE_400KHz;
	
//	public static final byte ADDRESS = (byte) 0x68; // AD0 = 0 (GND)
	public static final byte ADDRESS = (byte) 0x69; // AD0 = 1 (VDD)
	
	public static final byte WHO_AM_I = (byte) 0x00;
	public static final byte SMPLRT_DIV = (byte) 0x15;
	public static final byte DLPF_FS = (byte) 0x16;
	public static final byte INT_CFG = (byte) 0x17;
	public static final byte INT_STATUS = (byte) 0x1A;
	public static final byte TEMP_OUT_H = (byte) 0x1B;
	public static final byte TEMP_OUT_L = (byte) 0x1C;
	public static final byte GYRO_XOUT_H = (byte) 0x1D;
	public static final byte GYRO_XOUT_L = (byte) 0x1E;
	public static final byte GYRO_YOUT_H = (byte) 0x1F;
	public static final byte GYRO_YOUT_L = (byte) 0x20;
	public static final byte GYRO_ZOUT_H = (byte) 0x21;
	public static final byte GYRO_ZOUT_L = (byte) 0x22;
	public static final byte PWR_MGM = (byte) 0x3E;
	
	/**
	 * Register 22 - DLPF, Full Scale
	 * 
	 * | Register | Bit 7 | Bit 6 | Bit 5 | Bit 4 | Bit 3 | Bit 2 | Bit 1 | Bit 0 |
	 * |   0x16   |           -           |     F_SEL     |        DLPF_CFG       |
	 * 
	 * | FS_SEL | Gyro Full-Scale Range |
	 * |    0   |        Reserved       |
	 * |    1   |        Reserved       |
	 * |    2   |        Reserved       |
	 * |    3   |    +/- 2000 deg/sec   |
	 * 
	 * | DLPF_CFG | Low Pass Filter Bandwidth | Internal Sample Rate |
	 * |     0    |           256Hz           |         8kHz         |
	 * |     1    |           188Hz           |         1kHz         |
	 * |     2    |            98Hz           |         1kHz         |
	 * |     3    |            42Hz           |         1kHz         |
	 * |     4    |            20Hz           |         1kHz         |
	 * |     5    |            10Hz           |         1kHz         |
	 * |     6    |             5Hz           |         1kHz         |
	 * |     7    |         Reserved          |       Reserved       |
	 */
	public static final byte FS_SEL_3 = (byte) 0x03 << 3; // +/- 2000 deg/sec
	public static final byte DLPF_CFG_0 = (byte) 0x00;
	public static final byte DLPF_CFG_1 = (byte) 0x01;
	public static final byte DLPF_CFG_2 = (byte) 0x02;
	public static final byte DLPF_CFG_3 = (byte) 0x03;
	public static final byte DLPF_CFG_4 = (byte) 0x04;
	public static final byte DLPF_CFG_5 = (byte) 0x05;
	public static final byte DLPF_CFG_6 = (byte) 0x06;
	
	/**
	 * Register 62 - Power Management
	 * 
	 * | Register |  Bit 7  | Bit 6 |  Bit 5  |  Bit 4  |  Bit 3  | Bit 2 | Bit 1 | Bit 0 |
	 * |   0x3E   | H_RESET | SLEEP | STBY_XG | STBY_YG | STBY_ZG |        CLK_SEL        |
	 * 
	 * | CLK_SEL |              Clock Source             |
	 * |    0    |          Internal oscillator          |
	 * |    1    |       PLL with X Gyro reference       |
	 * |    2    |       PLL with Y Gyro reference       |
	 * |    3    |       PLL with Z Gyro reference       |
	 * |    4    | PLL with external 32.768kHz reference |
	 * |    5    |  PLL with external 19.2MHz reference  |
	 */
	public static final byte H_RESET = (byte) (0x01 << 7);
	public static final byte CLK_SEL_0 = (byte) 0x00;
	public static final byte CLK_SEL_1 = (byte) 0x01;
	public static final byte CLK_SEL_2 = (byte) 0x02;
	public static final byte CLK_SEL_3 = (byte) 0x03;
	public static final byte CLK_SEL_4 = (byte) 0x04;
	public static final byte CLK_SEL_5 = (byte) 0x05;
	
	public static final byte WHO_AM_I_DEFAULT = (byte) 0x34 << 1; // 110 100 << 1 = 0x68
	
	private static final int READ_BUFFER_SIZE  = 10; // bytes
	private static final int WRITE_BUFFER_SIZE = 10; // bytes

	public interface ITG3205Listener {
		public void onDeviceId(byte deviceId);
		public void onData(int x, int y, int z, int temperature);
		public void onError(String message);
	}
	
	private ITG3205Listener listener;
	
	private byte deviceId;
	
	private short x;
	private short y;
	private short z;
	private short t;
	
	private int twiNum;
	private TwiMaster i2c;
	
	private byte[] readBuffer  = new byte[READ_BUFFER_SIZE];
	private byte[] writeBuffer = new byte[WRITE_BUFFER_SIZE];

	public ITG3205(int twiNum) {
		this.twiNum = twiNum;
	}
	
	public ITG3205 setListener(ITG3205Listener listener) {
		this.listener = listener;
		return this;
	}
	
	public byte readDeviceId() throws ConnectionLostException, InterruptedException {
		read(WHO_AM_I, 1, readBuffer);
		return readBuffer[0];
	}
	
	private void setupDevice() throws InterruptedException, ConnectionLostException {
		System.out.println("PWR_MGM H_RESET");
		write(PWR_MGM, H_RESET); // reset device
		
		byte id = readDeviceId();
		System.out.println("deviceId = " + ByteHelper.byteToHexString(id));
		Thread.sleep(REGISTER_WRITE_DELAY);
		
		if (id == WHO_AM_I_DEFAULT) {
			deviceId = id;
		} else {
			onError("Invalid device ID, expected " + (WHO_AM_I_DEFAULT & 0xFF) + " but got " + (id & 0xFF));
		}
		
		if (listener != null) {
			listener.onDeviceId(deviceId);
		}
		
		write(DLPF_FS, (byte) (FS_SEL_3 | DLPF_CFG_0)); // +/- 2000 deg/sec, 256Hz, 8kHz
//		write(DLPF_FS, (byte) (FS_SEL_3 | DLPF_CFG_3)); // +/- 2000 deg/sec, 42Hz, 1kHz
		
		// F_sample = F_internal / (SMPLRT_DIV + 1)
		// F_sample = 8000 / (79 + 1) = 100 Hz
		// 1000 / 100 Hz = 10 ms (thread sleep)
		write(SMPLRT_DIV, (byte) 79);
//		write(SMPLRT_DIV, (byte) 0x04);
		
		write(INT_CFG, (byte) 0x00); // disable interrupts
		
		/**
		 * It is highly recommended that the device is configured to use one of
		 * the gyros (or an external clock) as the clock reference, due to the
		 * improved stability.
		 */
		write(PWR_MGM, CLK_SEL_1);
	}
	
	protected void write(byte register, byte value) throws ConnectionLostException, InterruptedException {
		writeBuffer[0] = register;
		writeBuffer[1] = value;
		flush(2);
	}
	
	protected void write(byte register, byte[] values) throws ConnectionLostException, InterruptedException {
		writeBuffer[0] = register;
		System.arraycopy(values, 0, writeBuffer, 1, values.length);
		flush(1 + values.length);
	}
	
	/**
	 * Writes the write buffer to the I2C.
	 * 
	 * @param length Number of bytes of the buffer to write.
	 * @throws ConnectionLostException
	 * @throws InterruptedException
	 */
	protected void flush(int length) throws ConnectionLostException, InterruptedException {
		boolean tenBitAddr = false;
		int readSize = 0;
		i2c.writeRead(ADDRESS, tenBitAddr, writeBuffer, length, readBuffer, readSize);
		
		if (REGISTER_WRITE_DELAY > 0)
			Thread.sleep(REGISTER_WRITE_DELAY);
	}
	
	protected void read(byte register, int length, byte[] values) throws ConnectionLostException, InterruptedException {
		boolean tenBitAddr = false;
		writeBuffer[0] = register;
		
		i2c.writeRead(ADDRESS, tenBitAddr, writeBuffer, 1, readBuffer, length);
	}
	
	private void onError(String message) {
		if (listener != null) {
			listener.onError(message);
		}
	}
	
	/*
	 * IOIOLooper interface methods.
	 */

	@Override
	public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
		i2c = ioio.openTwiMaster(twiNum, CLOCK_RATE, false /* smbus */);
		setupDevice();
	}

	@Override
	public void loop() throws ConnectionLostException, InterruptedException {
		if (listener != null) {
			read(TEMP_OUT_H, 8, readBuffer);
			
			t = (short) (((readBuffer[0] & 0xFF) << 8) | ((readBuffer[1] & 0xFF) << 0));
			x = (short) (((readBuffer[2]) << 8) | (readBuffer[3] << 0));
			y = (short) (((readBuffer[4]) << 8) | (readBuffer[5] << 0));
			z = (short) (((readBuffer[6]) << 8) | (readBuffer[7] << 0));
			
			listener.onData(x, y, z, t);
		}
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub
	}

	@Override
	public void incompatible() {
		// TODO Auto-generated method stub
	}
	
}
