import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.spec.IvParameterSpec;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.xml.bind.DatatypeConverter;

import Sendable.Directory;
import Sendable.KeySendable;
import Sendable.Sendable;
import Sendable.SendableHelper;
import Sendable.Sendable_Data;
import Sendable.Sendable_Data.FileShellSendable;
import gui.Clickable;
import gui.EButton;
import gui.IconButton;
import gui.Message;

public class ConnectionEstablisher implements Runnable {
	static boolean connectionSecure = false;

	Socket connection;

	/*
	 * For encrypting data with Session Key
	 * Algorithm: Advanced Encryption Standard (AES)
	 * Mode: Cipher Block Chaining (CBC)
	 * Padding: PKCS #7 Padding
	 */
	private static final String TRANSFORMATION_WITH_AES = "AES/CBC/PKCS5Padding";

	/*
	 * For encrypting data with server's public key
	 * Algorithm: RSA
	 * Mode: None ('ECB' is a misnomer from Java. RSA does not use a mode since it is not a block cipher
	 * Padding: Optimal Asymetric Encryption Padding (OAEP)
	 */
	private static final String TRANSFORMATION_WITH_RSA = "RSA/ECB/OAEPPadding";

	/*
	 * Contains the server's public key
	 * Encoding: X.509
	 */
	//	private static final File FILE_SERVER_PUBLIC_KEY = new File("public2048.key");
	//	private static final File FILE_SERVER_PUBLIC_KEY = new File (new File("").getAbsolutePath() + File.separator + "src" + File.separator + "public2048.key");
	//	private static final File FILE_SERVER_PUBLIC_KEY = new File ("bin" + File.separator + "public2048.key");
	//	private static File FILE_SERVER_PUBLIC_KEY; // = new File (this.getClass().getResource("/res" + File.separator + "/public2048.key").getPath());
	//	private static final File FILE_SERVER_PUBLIC_KEY = new File (new File("").getAbsolutePath() + File.separator + "bin" + File.separator + "public2048.key");
	private static File FILE_SERVER_PUBLIC_KEY = new File (ConnectionEstablisher.class.getResource("/SPK/public2048.key").getPath());

	/*
	 * The hashed bytes of the server's public key
	 * Hashing algorithm: SHA-256
	 * Encoding: X.509
	 * Prevents from using false public key
	 */
	private static final String HASHED_KEY_BYTES = "17B862A5D2092497F20A01D71922B35B178FE062E97B0AB785A54C98651F0D14";

	/*
	 * Read Sendable and KeySendable objects from connection
	 */
	static ObjectInputStream input;

	/*
	 * Send Sendable and KeySendable objects to connection
	 */
	static ObjectOutputStream output;

	/*
	 * PublicKey object holding the server's public key
	 * see the method @load
	 */
	PublicKey SERVER_PUBLIC_KEY;

	/*
	 * Key object holding the Session Key
	 * The Session Key is a 128 bit long AES key
	 */
	static Key SESSION_KEY;

	/*
	 * Encryption cipher
	 */
	Cipher ENCRYPT_CIPHER_AES;

	/*
	 * Holds the encoded bytes of the Key-object @SESSION_KEY
	 * Encoding: X.509
	 */
	byte[] BYTES_SESSION_KEY;

	UpperPanel upperPanel;

	static JFrame mainFrame;

	/*
	 * The root storage directory
	 */
	//	File STORAGE_ROOT_DIR = new File ("");
	String STORAGE_ROOT_DIR = "";

	/*
	 * Current directory or file
	 */
	//	File STORAGE_CURRENT_DIR = new File ("" + STORAGE_ROOT_DIR);
	String STORAGE_CURRENT_DIR = "" + STORAGE_ROOT_DIR;

	String STORAGE_CURRENT_FILE = STORAGE_CURRENT_DIR + "";

	JFileChooser jFileChooser;

	/*
	 * The buttons in the main panel representing all files and directory in the current dir
	 */
	IconButton[] eFiles;
	
//	Message m;


	private FileShellSendable[] currentDirFileShells;

	public ConnectionEstablisher() {
	}

	private void connect() {
		FILE_SERVER_PUBLIC_KEY = new File (System.getProperty("user.home") + File.separator + "File Sync 2" + File.separator + ".background" + File.separator + "public2048.key");
		try {
//			connection = new Socket("192.168.178.72", 6789);
			connection = new Socket ( "localhost" , 6789);
			System.out.println("Connected to Server.");
		} catch(ConnectException connectException) {
			System.out.println("Could not connect to server. Retrying...");
			connect();
		} catch (IOException ioException) {
			//			System.out.println("Could not connect to server. Retrying...");
			ioException.printStackTrace();
//			connect();
		}
	}

	@Override
	public void run() {
		try {
			connect();

			/*
			 * Setup streams
			 */

			// Output
			output = new ObjectOutputStream(connection.getOutputStream());

			// Input
			input = new ObjectInputStream(connection.getInputStream());
			System.out.println("Streams setup");

			// Generate the Session Key for secure communication
			SESSION_KEY = generateSessionKey();
			BYTES_SESSION_KEY = SESSION_KEY.getEncoded();
			System.out.println("Successfully generated Session Key in Thread " + Thread.currentThread());

			try {
				ENCRYPT_CIPHER_AES = Cipher.getInstance(TRANSFORMATION_WITH_AES);
				ENCRYPT_CIPHER_AES.init(Cipher.ENCRYPT_MODE, SESSION_KEY);
				System.out.println("Encryption Cipher setup");
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e1) {
				e1.printStackTrace();
			}


			if (fileStoresCorrectEncodedKey(FILE_SERVER_PUBLIC_KEY, HASHED_KEY_BYTES)) {
				SERVER_PUBLIC_KEY = loadPublicKeyFromFile(FILE_SERVER_PUBLIC_KEY);
				System.out.println("Server's public key loaded.");

				try {
					startHandshake();
				} catch (Exception exception) {
					System.out.println("Handshake failed!");
					exception.printStackTrace();
					connectionSecure = false;
					connection = null;
				}

				if (connectionSecure && connection != null) {
					// Search for cookie file
					System.out.println("Does cookie exist?");

					if (cookieFileExists()) {
						try {
							System.out.println("Yes. Sending cookie to server...");

							FileInputStream fis = new FileInputStream(Client.FILE_SYNC_COOKIE_REMEMBER_ME_SESSION);
							ObjectInputStream ois = new ObjectInputStream(fis);
							Sendable_Data.CookieSendable cookie = (Sendable_Data.CookieSendable) ois.readObject();
							ois.close();
							fis.close();

							// Create Sendable object
							Sendable_Data raw_data = new Sendable_Data();
							raw_data.setCookie(cookie);
							Sendable cookie_sendable = SendableHelper.createSendableFromSendable_Data(raw_data, TRANSFORMATION_WITH_AES, SESSION_KEY);

							String cookie_username = cookie.getUsername();

							// Send Sendable
							output.writeObject(cookie_sendable);

							// Listen for response
							Sendable cookie_response = null;
							while ((cookie_response = (Sendable) input.readObject()) != null) {
								System.out.println("Cookie-response from server!");

								Sendable_Data data = decryptSendable(cookie_response, TRANSFORMATION_WITH_AES, SESSION_KEY);

								// Successfully logged in
								if (data.checkAuthenticationSuccessful()) {
									System.out.println("Successfully logged in as " + cookie_username);

									// Delete old cookie file
									Client.FILE_SYNC_COOKIE_REMEMBER_ME_SESSION.delete();

									// Replace with new one
									FileOutputStream fos = new FileOutputStream(Client.FILE_SYNC_COOKIE_REMEMBER_ME_SESSION);
									ObjectOutputStream oos = new ObjectOutputStream(fos);
									oos.writeObject(data.getCookie());
									oos.flush();
									oos.close();
									fos.flush();
									fos.close();

									loggedIn(cookie_username, data);

									data = null;

									//									System.out.println("Inside my storage folder are " + data.getFileShells().length + " Files");
								} else {
									System.out.println("Authentication via Cookie was not successful. Deleting cookie");
									if (Client.FILE_SYNC_COOKIE_REMEMBER_ME_SESSION.delete()) System.out.println("Deleted cookie.");

									Client.login.setClickable(true);
									Client.register.setClickable(true);
									Client.usernameField.setEnabled(true);
									Client.passwordField.setEnabled(true);

									Client.usernameField.addActionListener(new ActionListener() {

										@Override
										public void actionPerformed(ActionEvent e) {
											Client.m.setVisible(true);
											Client.login.setClickable(false);
											Client.register.setClickable(false);
											Client.usernameField.setEnabled(false);
											Client.passwordField.setEnabled(false);
											new Thread(new LoginButtonRunnable()).start();
										}
									});

									Client.passwordField.addActionListener(new ActionListener() {

										@Override
										public void actionPerformed(ActionEvent e) {
											Client.m.setVisible(true);
											Client.login.setClickable(false);
											Client.register.setClickable(false);
											Client.usernameField.setEnabled(false);
											Client.passwordField.setEnabled(false);
											new Thread(new LoginButtonRunnable()).start();
										}
									});

									Client.login.setOnClick(new Clickable() {

										@Override
										public void onClicked() {
											Client.m.setVisible(true);
											Client.login.setClickable(false);
											Client.register.setClickable(false);
											Client.usernameField.setEnabled(false);
											Client.passwordField.setEnabled(false);
											new Thread(new LoginButtonRunnable()).start();
										}
									});

									Client.register.setOnClick(new Clickable() {

										@Override
										public void onClicked() {
											Client.m.setVisible(true);
											Client.login.setClickable(false);
											Client.register.setClickable(false);
											Client.usernameField.setEnabled(false);
											Client.passwordField.setEnabled(false);
											new Thread(new RegisterButtonRunnable()).start();
										}
									});
								}
								break;
							}
						} catch (ClassNotFoundException classNotFoundException) {
							System.out.println("Could not read cookie file.");
							classNotFoundException.printStackTrace();
						}
					} else {
						System.out.println("No cookie exists.");

						// Enable the buttons to login and register
						// These must have a secure connection underlying
						Client.login.setClickable(true);
						Client.register.setClickable(true);
						Client.usernameField.setEnabled(true);
						Client.passwordField.setEnabled(true);

						Client.usernameField.addActionListener(new ActionListener() {

							@Override
							public void actionPerformed(ActionEvent e) {
								Client.m.setVisible(true);
								Client.login.setClickable(false);
								Client.register.setClickable(false);
								Client.usernameField.setEnabled(false);
								Client.passwordField.setEnabled(false);
								new Thread(new LoginButtonRunnable()).start();
							}
						});

						Client.passwordField.addActionListener(new ActionListener() {

							@Override
							public void actionPerformed(ActionEvent e) {
								Client.m.setVisible(true);
								Client.login.setClickable(false);
								Client.register.setClickable(false);
								Client.usernameField.setEnabled(false);
								Client.passwordField.setEnabled(false);
								new Thread(new LoginButtonRunnable()).start();
							}
						});

						Client.login.setOnClick(new Clickable() {

							@Override
							public void onClicked() {
								Client.m.setVisible(true);
								Client.login.setClickable(false);
								Client.register.setClickable(false);
								Client.usernameField.setEnabled(false);
								Client.passwordField.setEnabled(false);
								new Thread(new LoginButtonRunnable()).start();
							}
						});

						Client.register.setOnClick(new Clickable() {

							@Override
							public void onClicked() {
								Client.m.setVisible(true);
								Client.login.setClickable(false);
								Client.register.setClickable(false);
								Client.usernameField.setEnabled(false);
								Client.passwordField.setEnabled(false);
								new Thread(new RegisterButtonRunnable()).start();
							}
						});
					}
				} else {
					System.out.println("CONNECTION NOT SECURE! ABORT");
					System.exit(1);
				}
			} else {
				System.out.println("Could not load Server's public key!");
				System.exit(1);
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
			//			System.out.println("Could not connect to server. Retrying...");
			//			run();
		}
	}

	Sendable_Data decryptSendable(Sendable decrypted_sendable, String transformation, Key key) {
		try {
			byte[] iv = decrypted_sendable.getIV();
			Cipher decryptionCipher = Cipher.getInstance(transformation);
			decryptionCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
			return (Sendable_Data) decrypted_sendable.getSendable_Data().getObject(decryptionCipher);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | ClassNotFoundException | IllegalBlockSizeException |
				BadPaddingException | IOException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
			return null;
		}
	}

	private boolean cookieFileExists() {
		return Client.FILE_SYNC_COOKIE_REMEMBER_ME_SESSION.exists();
	}

	private void startHandshake() throws Exception {
		/*
		 * Secure the connection with the server
		 * 
		 * 	1. Send a KeySendable object to server
		 * 	   KeySendable object contains the Session Key encrypted with the server's public key
		 *  2. Wait for server to respond with the exact KeySendable object, only encrypted with the Session Key
		 *     instead of public key
		 *  3. Check for same Session Key
		 */

		// Record start time
		long start = System.currentTimeMillis();

		// Construct new KeySendable object and fill with encoded bytes of the session key
		// (X.509 encoding)
		KeySendable keySendable = new KeySendable(BYTES_SESSION_KEY);

		// Create and initialize new Cipher to encrypt with server's public key
		// Padding: OAEP
		// See @TRANSFORMATION_WITH_RSA's declaration
		Cipher cipher = Cipher.getInstance(TRANSFORMATION_WITH_RSA);
		cipher.init(Cipher.ENCRYPT_MODE, SERVER_PUBLIC_KEY);

		// Create SealedObject width KeySendable object and initialized cipher
		SealedObject sealedKeySendable = new SealedObject(keySendable, cipher);

		// Send the sealed object to Server
		output.writeObject(sealedKeySendable);
		System.out.println("Sent KeySendable object to Server. Listening for response...");

		sealedKeySendable = null;

		SealedObject response = null;
		// Listen for response
		while ((response = (SealedObject) input.readObject()) != null) {
			System.out.println("Server responded! Decrypting...");

			//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			//			ObjectOutputStream oos = new ObjectOutputStream(baos);
			//			oos.writeObject(response);
			//			byte[] response_bytes = baos.toByteArray();
			//			
			//			System.out.println("SERVER RESPONSE SIZE: " + response_bytes.length);

			// Decrypt the sealed object from server into a KeySendable object
			KeySendable decrypted_response = (KeySendable) response.getObject(SESSION_KEY);

			// Extract encoded Session Key bytes from KeySendable object
			byte[] response_session_bytes = decrypted_response.getBytes();

			// Calculate the time it took to complete the handshaking process
			long delta = System.currentTimeMillis() - start;

			// Check if the response from the server is correct
			if (Arrays.equals(BYTES_SESSION_KEY, response_session_bytes)) {
				System.out.println("Connection secure!");
				System.out.println("Securing the connection took " + delta + " Miliseconds.");
				connectionSecure = true;
			} else {
				System.out.println("Connection not secure! Aborting.");
				connection.close();
				connectionSecure = false;
			}
			break;
		}
	}

	private Key generateSessionKey() {
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		keyGen.init(128);
		return keyGen.generateKey();
	}

	private PublicKey loadPublicKeyFromFile(File file) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException fileNotFoundException) {
			fileNotFoundException.printStackTrace();
		}
		byte[] fileBytes = new byte[(int) file.length()];
		try {
			fis.read(fileBytes);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(fileBytes);
		KeyFactory keyFactory = null;
		try {
			keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean fileStoresCorrectEncodedKey(File storageFile, String hardcodedHash) {
		try {
			FileInputStream fis = new FileInputStream(storageFile);
			byte[] fileBytes = new byte[(int) storageFile.length()];
			fis.read(fileBytes);
			fis.close();

			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			messageDigest.update(fileBytes);
			byte[] hash = messageDigest.digest();

			String hash_hex = DatatypeConverter.printHexBinary(hash);
			return hardcodedHash.equals(hash_hex);
		} catch (Exception exception) {
			exception.printStackTrace();
			return false;
		}
	}

	void sendSealedObjectToServer(SealedObject sealedSendable) {
		try {
			output.writeObject(sealedSendable);
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class LoginButtonRunnable implements Runnable {

		@Override
		public void run() {
			if (Client.usernameField.getText() != null && Client.passwordField.getPassword().length != 0) {
				Client.filterUsernameAndPasswordIntoVariables();

				/*
				 * Create a Sendable object with username and password and send
				 * it to server
				 */
				boolean login_remember_user = Client.rememberMe;

				Sendable_Data raw_login_sendable = new Sendable_Data(Client.username, Client.password, login_remember_user, new String[] { "login" });
				Sendable loginSendable = SendableHelper.createSendableFromSendable_Data(raw_login_sendable, TRANSFORMATION_WITH_AES, SESSION_KEY);

				try {
					System.out.println("Writing login data to server");
					output.writeObject(loginSendable);
					System.out.println("Done.");

					Sendable response_login_sendable = null;
					System.out.println("Listening for response");
					try {
						while ((response_login_sendable = (Sendable) input.readObject()) != null) {
							System.out.println("Server replied to login request");
							Sendable_Data raw_sendable_data = decryptSendable(response_login_sendable, TRANSFORMATION_WITH_AES, SESSION_KEY);

							if (raw_sendable_data.checkAuthenticationSuccessful()) {
								System.out.println("Successfully logged in!");

								// Save cookie, if server sent one
								if (raw_sendable_data.getCookie() != null) {
									System.out.println("Received cookie data! Saving cookie...");

									// Save cookie data to file
									FileOutputStream fos = new FileOutputStream(Client.FILE_SYNC_COOKIE_REMEMBER_ME_SESSION);
									ObjectOutputStream oos = new ObjectOutputStream(fos);
									oos.writeObject(raw_sendable_data.getCookie());
									oos.flush();
									oos.close();
									fos.flush();
									fos.close();
								}
								loggedIn(Client.username, raw_sendable_data);
							} else {
								System.out.println("Login failed!");
								Client.login.flickerBorders("#FF0000", 4);
							}
							break;
						}
					} catch (ClassNotFoundException classNotFoundException) {
						classNotFoundException.printStackTrace();
					}
				} catch (IOException ioException) {
					System.out.println("Failed to send login information to server");
					ioException.printStackTrace();
				}
			} else {
				Client.login.setClickable(true);
				Client.register.setClickable(true);
				Client.login.flickerBorders("#FF0000", 4);
			}
			return;
		}
	}

	/**
	 * The method called after having sucessfully logged in
	 * @param username
	 * @param data
	 */
	private void loggedIn(String username, Sendable_Data data) {
		STORAGE_CURRENT_DIR = STORAGE_ROOT_DIR;
		currentDirFileShells = data.getFileShells();
		disposeOldGUI();
		buildNewGUI(username, data);
	}

	MainPanel mainPanel;
	EButton eParentDir;

	private void disposeOldGUI() {
		Client.frame.dispose();
		Client.frame = null;
	}

	private void buildNewGUI(String username, Sendable_Data data) {
		mainFrame = new JFrame("File Sync 2 - " + username);
		mainFrame.setSize(1500, 800);
		mainFrame.setLocation((Client.screen_width - mainFrame.getWidth()) / 2, (Client.screen_height - mainFrame.getHeight()) / 2);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setLayout(null);
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainFrame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) { }

			@Override
			public void windowIconified(WindowEvent e) {
				// Minimized
				mainFrame.setVisible(false);
			}

			@Override
			public void windowDeiconified(WindowEvent e) { }

			@Override
			public void windowDeactivated(WindowEvent e) { }

			@Override
			public void windowClosing(WindowEvent e) {
				int confirm = JOptionPane.showOptionDialog(mainFrame, "Wirklich beenden?", "Quitting File Sync 2", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
				if (confirm == JOptionPane.YES_OPTION) System.exit(0);
			}

			@Override
			public void windowClosed(WindowEvent e) { }

			@Override
			public void windowActivated(WindowEvent e) { }
		});

		System.out.println("Created new mainFrame");
		
		mainFrame.setVisible(true);

		JPanel window = new JPanel();
		window.setSize(mainFrame.getContentPane().getSize());
		window.setLayout(null);
		
//		System.out.println("Message size: " + m.getSize() + "| Message location: " + m.getLocation());

		upperPanel = new UpperPanel();
		upperPanel.setSize(window.getWidth() - 350, 150);
		upperPanel.setLayout(null);
		upperPanel.invalidate();
		upperPanel.setBackground(Color.WHITE);

		eParentDir = new EButton();
		eParentDir.setSize(200, 80);
		eParentDir.setLocation(35, 35);
		eParentDir.setFontSize(30);
		eParentDir.setClickable(false);
		try {
			eParentDir.setImage(ImageIO.read(new File (Client.FILE_SYNC_PNG_DIR + File.separator + "back.png")), eParentDir.getHeight() - 15, eParentDir.getHeight() - 15);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		eParentDir.setOnClick(new Clickable() {

			@Override
			public void onClicked() {
				//				stopFocusOnSpecificFile();
				eParentDir.setClickable(false);

				//				System.out.println("BEFORE current dir: '" + STORAGE_CURRENT_DIR + "'");

				if (!STORAGE_CURRENT_DIR.equals(STORAGE_ROOT_DIR)) {
					// Go to parent directory
					STORAGE_CURRENT_DIR = STORAGE_CURRENT_DIR.substring(0, STORAGE_CURRENT_DIR.lastIndexOf("/"));

					//					System.out.println("New working directory: '" + STORAGE_CURRENT_DIR + "', root dir: '" + STORAGE_ROOT_DIR + "', are equal: " + STORAGE_CURRENT_DIR.equals(STORAGE_ROOT_DIR));

					// Mark back-button unclickable if now at root directory
					if (STORAGE_CURRENT_DIR.equals(STORAGE_ROOT_DIR)) eParentDir.setClickable(false);

					// Repaint upperPanel
					upperPanel.repaint();

					// Start networking thread sending new dir request to server
					new Thread (new RequestNewDir()).start();
				}
			}
		});

		mainPanel = new MainPanel();
		mainPanel.setSize(window.getWidth() - 350, 500);
		mainPanel.setLocation(0, 150);
		mainPanel.addButtons(currentDirFileShells);
		mainPanel.setLayout(null);

		mainPanel.invalidate();

		LowerPanel lowerPanel = new LowerPanel();
		lowerPanel.setSize(window.getWidth() - 350, mainFrame.getHeight() - mainPanel.getHeight() - mainPanel.getY() - 65);
		lowerPanel.setLocation(0, 650);
		lowerPanel.setLayout(null);
		lowerPanel.invalidate();
		lowerPanel.setBackground(Color.WHITE);
		lowerPanel.addFeatures();

		SidePanel sidePanel = new SidePanel();
		sidePanel.setSize(335, window.getHeight());
		sidePanel.setLocation(window.getWidth() - 350, 0);
		sidePanel.setLayout(null);
		sidePanel.invalidate();
		sidePanel.setBackground(Color.WHITE);
		sidePanel.addFeatures();

		upperPanel.add(eParentDir);
		
//		window.add(m);
//		mainPanel.add(m);

		window.add(sidePanel);
		window.add(upperPanel);
		window.add(mainPanel);
		window.add(lowerPanel);

		mainFrame.add(window);
		mainFrame.repaint();
//		mainFrame.setVisible(true);
	}

	private class LowerPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4706352762124903083L;
//		EButton download = new EButton("Download");
		EButton download = new EButton();
		EButton open = new EButton();
		EButton delete = new EButton();

		private void addFeatures() {
			// Download
			download.setSize(185, (int) (getHeight() * 0.8));
			download.setLocation(25, (int) (getHeight() * 0.1));
			download.setFontSize(23);
			download.combineImageAndText(EButton.getImageByName("download"), (int) (download.getHeight() * 0.7), (int) (download.getHeight() * 0.7), "Download", 8);
//			download.setImage(EButton.getImageByName("download"), (int) (download.getHeight() * 0.7), (int) (download.getHeight() * 0.7)); 
			download.setOnClick(new Clickable() {

				@Override
				public void onClicked() {
					if (STORAGE_CURRENT_FILE.equals(""))
						// No particular file is do be downloaded, so request whole current working directory
						new Thread (new RequestDirectory()).start();
					else
						// Request a particular file -> current working file
						new Thread (new RequestFile()).start();
				}
			});

			add(download);

			// Open
			open.setSize(download.getSize());
			open.setLocation((getWidth() - open.getWidth()) / 2, (int) (getHeight() * 0.1));
			open.setFontSize(23);
			open.combineImageAndText(EButton.getImageByName("open"), (int) (open.getHeight() * 0.7), (int) (open.getHeight() * 0.7), "Open", 8);

			add(open);

			// Delete
			delete.setSize(download.getSize());
			delete.setFontSize(23);
			delete.combineImageAndText(EButton.getImageByName("delete"), (int) (delete.getHeight() * 0.7), (int) (delete.getHeight() * 0.7), "Delete", 8);
			delete.setLocation(getWidth() - delete.getWidth() - 25, (int) (getHeight() * 0.1));

			add(delete);
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			
			
		}
	}

	private class UpperPanel extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6292930824801880510L;

		public UpperPanel() {
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Graphics2D g2d = (Graphics2D) g;
			Line2D lowerBorder = new Line2D.Float(0, getHeight(), getWidth(), getHeight());
			g2d.setStroke(new BasicStroke(11));
			g2d.draw(lowerBorder);

			// Draw current location in directory hierarchy
			g2d.setFont(new Font ("Helvetica", Font.BOLD, 33));
			//			System.out.println("Repainting. Current dir: " + STORAGE_CURRENT_DIR);
			if (STORAGE_CURRENT_DIR.equals("")) {
				//				System.out.println("Writing 'H': " + STORAGE_CURRENT_DIR.equals("") + ";" + STORAGE_CURRENT_DIR);
				g2d.drawString("Hauptordner", 400, (getHeight() + 22) / 2);
			}
			else {
				//				System.out.println("Writing dir name " + STORAGE_CURRENT_DIR);
				g2d.drawString(STORAGE_CURRENT_DIR, 400, (getHeight() + 22) / 2);
			}
		}
	}

	private class SidePanel extends JPanel {
		public SidePanel() {
			setLayout(null);
		}
		/**
		 * 
		 */
		private static final long serialVersionUID = 7645573497667245182L;

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Graphics2D g2d = (Graphics2D) g;
			Line2D lowerBorder = new Line2D.Float(0, 0, 0, getHeight());
			g2d.setStroke(new BasicStroke(11));
			g2d.draw(lowerBorder);

			// Write "My Cloud" in upper corner
			String myCloud = "My Cloud";
			g2d.setFont(new Font ("Helvetica", Font.BOLD, 31));
			int myCloud_width = g2d.getFontMetrics().stringWidth(myCloud);
			g2d.drawString(myCloud, Math.abs((myCloud_width - getWidth()) / 2), 75);
		}

		public void addFeatures() {
			EButton logout = new EButton();
			logout.setSize((int) (getWidth() * 0.8), 75);
			logout.setLocation((int) (getWidth() * 0.1), 115);
			logout.combineImageAndText(EButton.getImageByName("logout"), (int) (logout.getHeight() * 0.7), (int) (logout.getHeight() * 0.7), "Logout", 8);
			logout.setOnClick(new Clickable() {

				@Override
				public void onClicked() {
					System.out.println("___LOGGING OUT___");
					new Thread(new Logout()).start();
				}
			});
			
			EButton refresh = new EButton();
			refresh.setSize (logout.getSize());
			refresh.setLocation(logout.getX(), logout.getY() + logout.getHeight() + 35);
//			refresh.setImage(refresh_image, (int) (refresh.getHeight() * 0.9), (int) (refresh.getHeight() * 0.9));
			refresh.combineImageAndText(EButton.getImageByName("refresh"), (int) (refresh.getHeight() * 0.7), (int) (refresh.getHeight() * 0.7), "Refresh", 15);
			refresh.setOnClick(new Clickable() {
				
				@Override
				public void onClicked() {
					new Thread (new RequestNewDir()).start();
				}
			});
			
			EButton options = new EButton();
			options.setSize(refresh.getHeight(), refresh.getHeight());
			options.setLocation((getWidth() - options.getWidth()) / 2, getHeight() - options.getHeight() - options.getX() - (getWidth() - options.getWidth()) / 4);
			options.setImage(EButton.getImageByName("options"), (int) (options.getHeight() * 0.7), (int) (options.getHeight() * 0.7));
			
			EButton auto_sync = new EButton();
			auto_sync.setSize (logout.getSize());
			auto_sync.setLocation(logout.getX(), refresh.getY() + auto_sync.getHeight() + 35);
			auto_sync.combineImageAndText(EButton.getImageByName("auto-sync"), (int) (auto_sync.getHeight() * 0.7), (int) (auto_sync.getHeight() * 0.7), "Auto-Sync", 15);
			auto_sync.setOnClick(new Clickable() {
				
				@Override
				public void onClicked() {
					mainPanel.content.changeViewToAutoSyncScreen();
				}
			});
			
//			System.out.println("Options: " + getHeight() + "|" + options.getHeight() + "|" + options.getX() + "|" + options.getY());

			add (logout);
			add (refresh);
			add (options);
			add (auto_sync);
		}
	}

	private class MainPanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7059230569189254959L;

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			Graphics2D g2d = (Graphics2D) g;
			Line2D lowerBorder = new Line2D.Float(0, getHeight(), getWidth(), getHeight());
			g2d.setStroke(new BasicStroke(11));
			g2d.draw(lowerBorder);

		}

		class ContentPanel extends JPanel {
			/**
			 * 
			 */
			private static final long serialVersionUID = -8470176392621862186L;

			ContentPanel(int width, int height) {
				setSize(width, height);
				setLayout(null);

				jFileChooser = new JFileChooser();
				jFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				jFileChooser.setPreferredSize(new Dimension(getWidth() - 150, getHeight() - 50));
				jFileChooser.setVisible(false);

				ContentPanel.this.add(jFileChooser);

				addMouseListener(new MouseListener() {
					public void mouseReleased(MouseEvent e) {}
					public void mousePressed(MouseEvent e) {}
					public void mouseExited(MouseEvent e) {}
					public void mouseEntered(MouseEvent e) {}

					@Override
					public void mouseClicked(MouseEvent e) {
						stopFocusOnSpecificFile();
					}
				});
			}

			/**
			 * Updates the eFiles - buttons to correspond to the new directory
			 * Is called when entering another, different directory
			 * @param newFileShells New file shells
			 */
			void updateButtons(Sendable_Data.FileShellSendable[] newFileShells) {
				removeAllButtons();
				addButtons(newFileShells);
			}

			void removeAllButtons() {
				removeAll();
				revalidate();
				repaint();
			}

			synchronized void addButtons(Sendable_Data.FileShellSendable[] fileShells) {
				eFiles = new IconButton[fileShells.length + 1];
				setLocation (0, 0);

				int row = 0;
				int distance = 10;
				int buttons_per_row = 5;
				int width = (getWidth() - distance * (buttons_per_row + 1)) / buttons_per_row;
				int current_button_in_row = -1;

				for (int i = 0; i < fileShells.length + 1; i++) {
					current_button_in_row++;
					if (current_button_in_row == buttons_per_row) {
						// New row
						row++;
						current_button_in_row = 0;
						setSize(getWidth(), getHeight() + distance + eFiles[i-1].getHeight());
					}

					try {
						if (i == fileShells.length) {
							//							File image_plus = new File ("src" + File.separator + "png" + File.separator + "plus.png");
							File image_plus = new File (Client.FILE_SYNC_PNG_DIR + File.separator + "plus.png");
							eFiles[i] = new IconButton(ImageIO.read(image_plus), width, width, "");
						} else {
							eFiles[i] = new IconButton(null, width, width, fileShells[i].getName());
							eFiles[i].setImage(ImageIO.read(getImageFileFromExtension(fileShells[i].isDir(), fileShells[i].isDirAndContainsFiles(), fileShells[i].getName())));
							//							getImageFileFromExtension(fileShells[i].getName());
						}
						eFiles[i].setSize(width, width);
						eFiles[i].setLocation(distance + current_button_in_row * (distance + width), distance + row * (distance + eFiles[i].getHeight()));
						eFiles[i].addMouseWheelListener(new ContentPanelScrollListener(content));
						eFiles[i].setNumberInArray(i);
						add(eFiles[i]);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				for (IconButton ib : eFiles) {
					ib.imageButton.setOnClick(new Clickable() {

						@Override
						public void onClicked() {
							for (IconButton _ib : eFiles) {
								_ib.imageButton.setBorderThickness(7);
								_ib.imageButton.setBorderColor("#000000", true);
							}

							// Upload button
							if (ib.getNumberInArray() == fileShells.length) {
								jFileChooser.setVisible(true);
								jFileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

								int result = jFileChooser.showOpenDialog(MainPanel.this);
								if (result == JFileChooser.APPROVE_OPTION) {
									if (jFileChooser.getSelectedFile().isDirectory()) new Thread (new UploadDirectory()).start();
									else new Thread (new UploadFile()).start();
								}
							} else {
								if (fileShells[ib.getNumberInArray()].isDir()) {
									// Update current working directory
									//									STORAGE_CURRENT_DIR += File.separator + fileShells[ib.getNumberInArray()].getName();
									STORAGE_CURRENT_DIR += "/" + fileShells[ib.getNumberInArray()].getName();

									//								new Thread (new RequestNewDir(fileShells, ib)).start();
									new Thread (new RequestNewDir()).start();
								} else {
									// Update current working file
									STORAGE_CURRENT_FILE = fileShells[ib.getNumberInArray()].getName();
								}

//								System.out.println("Current dir: " + STORAGE_CURRENT_DIR);
								upperPanel.repaint();
							}
						}
					});
				}
			}
			
			void changeViewToAutoSyncScreen() {
				// Clear window/screen
				removeAllButtons();
				
				setLayout (null);
				
				JFileChooser jfc = new JFileChooser();
				jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				jfc.setPreferredSize(new Dimension(getWidth() - 150, getHeight() - 50));
				jfc.setCurrentDirectory(new File(System.getProperty("user.home")));
				jfc.setVisible(false);
				
//				System.out.println("Preferred size of jfc: " + jfc.getSize() + ", getSize(): " + getSize());


				ContentPanel.this.add(jfc);
				
//				JTextField location = new JTextField("root" + STORAGE_CURRENT_DIR);
				JTextField location = new JTextField();
				location.setDisabledTextColor(Color.BLACK);
				location.setEnabled(false);
				location.setSize((int) (getWidth() * 0.55), (int) (getWidth() * 0.085));
				location.setLocation(50, 175);
				location.setFont(new Font("Helvetica", Font.BOLD, (int) (location.getHeight() / 3.5)));
				
				EButton browse = new EButton();
				browse.setSize((int) (getWidth() * 0.125), (int) (location.getHeight()));
				browse.setLocation((int) (getWidth() * 0.7), location.getY() + (location.getHeight() - browse.getHeight()) / 2);
				browse.combineImageAndText(EButton.getImageByName("browse"), "Browse", 15);
				browse.setOnClick(new Clickable() {
					
					@Override
					public void onClicked() {
						jfc.setVisible(true);
						int result = jfc.showOpenDialog(ContentPanel.this);
						if (result == JFileChooser.APPROVE_OPTION) {
							File selected = jfc.getSelectedFile();
							location.setText(selected.getAbsolutePath());
						}
					}
				});
				
				EButton start = new EButton("Start");
				start.setSize(browse.getSize());
				start.setLocation((getWidth() - start.getWidth()) / 2, getHeight() - start.getHeight() - 35);
				start.setOnClick(new Clickable() {
					
					@Override
					public void onClicked() {
						addAutoSyncInstruction(jfc.getSelectedFile().getAbsolutePath(), 5000);
						System.out.println("Added instruction");
					}
				});
				
				add (location);
				add (browse);
				add (start);
			}
		}

		ContentPanel content;

		public MainPanel() {
		}

		public void updateButtons(Sendable_Data.FileShellSendable[] newFileShells) {
			content.updateButtons(newFileShells);
			content.repaint();
		}

		public void addButtons(Sendable_Data.FileShellSendable[] fileShells) {
			content = new ContentPanel(getWidth(), getHeight());
			content.addButtons(fileShells);
			content.setBackground(Color.WHITE);
			content.addMouseWheelListener(new ContentPanelScrollListener(content));

			add(content);
		}
		
		Message message;
		
		private void addLoadingMessage(String _message) {
			message = new Message(Message.TYPE_LOADING, _message, 350, 350, 500);
			message.setLocation((mainPanel.getWidth() - message.getWidth()) / 2, (mainPanel.getHeight() - message.getHeight()) /2);
			message.setOpaque(true);
			message.setVisible(true);
			
			add (message);
			setComponentZOrder(message, 0);
			repaint();
		}
		
		private void removeLoadingMessage() {
			remove (message);
			message = null;
		}
		
		private void addSuccessMessage (String _message, int milis) {
			message = new Message(Message.TYPE_SUCCESSFUL, _message, 350, 350, milis);
			message.setLocation((mainPanel.getWidth() - message.getWidth()) / 2, (mainPanel.getHeight() - message.getHeight()) /2);
			message.setOpaque(true);
			message.setVisible(true);
			
//			System.out.println("MESSGE: " + message.getSize() + "|" + message.getLocation());
			
			add (message);
			setComponentZOrder(message, 0);
			repaint();
		}
	}

	//	private File getImageFileFromExtension(boolean isDir, boolean isDirAndContainsFiles, String name) {
	//		if (isDirAndContainsFiles) return new File (Client.FILE_SYNC_PNG_DIR)
	//	}

	private File getImageFileFromExtension(boolean isDir, boolean isDirAndContainsFiles, String name) {
		//		System.out.println(Client.FILE_SYNC_PNG_DIR.getAbsolutePath() + " PNG DIR");
		//		System.out.println(name + " is dir: " + isDir + "; contains files: " + isDirAndContainsFiles);
		if (isDirAndContainsFiles) return new File (Client.FILE_SYNC_PNG_DIR + File.separator + "dir-full.png");
		else if (isDir) return new File (Client.FILE_SYNC_PNG_DIR + File.separator + "dir-empty.png");
		else if (name.contains(".")) {
			String extension = name.substring(name.lastIndexOf('.'));
			String extension_without_dot = extension.substring(1);
			//			System.out.println("EXTENSION WITHOUT DOT '" + extension_without_dot + "'");
			File f = new File (Client.FILE_SYNC_PNG_DIR + File.separator + extension_without_dot + ".png");

			if (f.exists())
				return f;
			else
				f = new File (Client.FILE_SYNC_PNG_DIR + File.separator + "unknown.png");
			return f;
		}
		return new File (Client.FILE_SYNC_PNG_DIR + File.separator + "unknown.png");
	}

	private class RegisterButtonRunnable implements Runnable {
		@Override
		public void run() {
			if (Client.usernameField.getText() != null && Client.passwordField.getPassword() != null) {
				Client.filterUsernameAndPasswordIntoVariables();

				boolean login_remember_user = Client.rememberMe;
				Sendable registerSendable = SendableHelper.createRegisterSendable(Client.username, Client.password, login_remember_user, TRANSFORMATION_WITH_AES, SESSION_KEY);
				try {
					System.out.println("Writing register Sendable to server.");
					output.writeObject(registerSendable);
					System.out.println("Done");
				} catch (IOException ioException) {
					System.out.println("Failed to send register Sendable to server.");
					ioException.printStackTrace();
				}
			} else {
				Client.login.setClickable(true);
				Client.register.setClickable(true);
				Client.register.flickerBorders("#FF0000", 4);
			}
			return;
		}
	}

	private class ContentPanelScrollListener implements MouseWheelListener {
		JPanel content;
		private ContentPanelScrollListener(JPanel content) {
			this.content = content;
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			content.setLocation(content.getX(), content.getY() - e.getWheelRotation() * 22);

			if (content.getY() <= 0 && content.getY() > - (content.getHeight() - mainPanel.getHeight())) {
			} else if (content.getY() > 0) {
				content.setLocation(content.getX(), 0);
			} else if (content.getY() < - (content.getHeight() - mainPanel.getHeight())) content.setLocation(content.getX(), - (content.getHeight() - mainPanel.getHeight()));
		}

	}

	private class Logout implements Runnable {
		@Override
		public void run() {
			Sendable_Data raw_data = new Sendable_Data();
			raw_data.fillInCommands(new String[] { "user-logout" });
			Sendable request = SendableHelper.createSendableFromSendable_Data(raw_data, TRANSFORMATION_WITH_AES, SESSION_KEY);
			try {
				output.writeObject(request);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (Client.FILE_SYNC_COOKIE_REMEMBER_ME_SESSION.exists()) Client.FILE_SYNC_COOKIE_REMEMBER_ME_SESSION.delete();
			mainFrame.dispose();
			mainFrame = null;

			try {
				connection.close();
				input.close();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Client.client.stop();
			Client.client = null;
			Client.client = new Client();
			return;
		}
	}

	private class RequestNewDir implements Runnable {
		FileShellSendable[] fileShells;
//		IconButton ib;

		@Override
		public void run() {
//			m.setVisible(true);
//			mainPanel.invalidate();
			mainPanel.addLoadingMessage("Waiting for server");
			
//			System.out.println("__");
//			System.out.println("Set m visible");
			
//			try {
//				Thread.sleep(400);
//			} catch (InterruptedException e1) {
//				e1.printStackTrace();
//			}

			Sendable request;
			Sendable_Data data = new Sendable_Data();

			String[] command = new String[1];
			String[] info = new String[1];

			// Send command to send new directory
			command[0] = "update-directory";

			// Send new directory
			info[0] = STORAGE_CURRENT_DIR;

			// Fill up Sendable_Data object
			data.fillInInfo(info);
			data.fillInCommands(command);

			// Encrypt and send
			request = SendableHelper.createSendableFromSendable_Data(data, TRANSFORMATION_WITH_AES, SESSION_KEY);
			try {
				output.writeObject(request);

				// Listen for response
				Sendable response;
				while ((response = (Sendable) input.readObject()) != null) {
					
//					m.setVisible(false);
					
					System.out.println("Server replied to 'update-directory' command");
					Sendable_Data _data = response.decrypt(TRANSFORMATION_WITH_AES, SESSION_KEY);

					this.fileShells = _data.getFileShells();
//					System.out.println("File Shells from server contain " + fileShells.length + " Files and dirs");
					mainPanel.updateButtons(fileShells);

					mainPanel.removeLoadingMessage();
//					mainPanel.addSuccessMessage("Success!", 500);

					if (!STORAGE_CURRENT_DIR.equals(STORAGE_ROOT_DIR)) eParentDir.setClickable(true);
//					System.out.println("Back-Button now has " + eParentDir.getMouseListeners().length + " MouseListeners");
					break;
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
			return;
		}
	}

	private class RequestFile implements Runnable {
		String[] command = new String[1];
		String[] info = new String[1];

		//		FileShellSendable[] fileShells;
		//		IconButton ib;

		//		public RequestFile(FileShellSendable[] fileShells, IconButton ib) {
		//			this.fileShells = fileShells;
		//			this.ib = ib;
		//		}

		@Override
		public void run() {
			// Update current file
			//			STORAGE_CURRENT_FILE = fileShells[ib.getNumberInArray()].getName();

			// Send command to send new file
			command[0] = "send-file";

			// Send file in storage file system
			//			info[0] = STORAGE_CURRENT_DIR + File.separator + STORAGE_CURRENT_FILE;
			info[0] = STORAGE_CURRENT_DIR + "/" + STORAGE_CURRENT_FILE;

			String destination_file_name = STORAGE_CURRENT_FILE;

			// Encrypt and send
			Sendable_Data data = new Sendable_Data(command, info);
			Sendable request = SendableHelper.createSendableFromSendable_Data(data, TRANSFORMATION_WITH_AES, SESSION_KEY);
			try {
				output.writeObject(request);

				Sendable response;
				Sendable_Data _data;
				while ((response = (Sendable) input.readObject()) != null) {
					System.out.println("Server replied to 'send-file' command");

					// Decrypt
					_data = response.decrypt(TRANSFORMATION_WITH_AES, SESSION_KEY);

					// Extract bytes
					byte[][] sent_files = _data.getFiles();

					// Save file
					File destination_file = new File (Client.FILE_SYNC_DIRECTORY_STORAGE + File.separator + destination_file_name);
					saveFile(destination_file, sent_files[0]);

					System.out.println("Saved File '" + destination_file.getAbsolutePath() + "'");
					break;
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
			return;
		}
	}

	private class RequestDirectory implements Runnable {

		@Override
		public void run() {
			String[] command = new String[1];
			String[] info = new String[1];

			// Command to send directory
			command[0] = "send-directory";

			// which directory should be sent
			info[0] = STORAGE_CURRENT_DIR;

			// Encrypt and send
			Sendable request = SendableHelper.createSendableFromSendable_Data(new Sendable_Data(command, info), TRANSFORMATION_WITH_AES, SESSION_KEY);
			try {
				output.writeObject(request);

				Sendable response;
				Sendable_Data _data;
				while ((response = (Sendable) input.readObject()) != null) {
					System.out.println("Server replied to 'send-directory' request.");
					_data = response.decrypt(TRANSFORMATION_WITH_AES, SESSION_KEY);

				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
			return;
		}
	}

	private class UploadDirectory implements Runnable {

		@Override
		public void run() {
			File selectedFile = jFileChooser.getSelectedFile();
			System.out.println("Selected directory: " + selectedFile.getAbsolutePath());
			
			Sendable_Data data = new Sendable_Data();

			String[] command = new String[1];
			command[0] = "save-directory";

			String[] info = new String[1];
			info[0] = STORAGE_CURRENT_DIR;

			data.fillInCommands(command);

			data.fillInInfo(info);

			//		    Sendable_Data.Directory directory_to_copy = new Sendable_Data.Directory(selectedFile.getAbsolutePath(), selectedFile.getName());
			Directory directory_to_copy = new Directory(selectedFile.getParent(), selectedFile.getName());
			data.setDirectory(directory_to_copy);

			try {
				output.writeObject(SendableHelper.createSendableFromSendable_Data(data, TRANSFORMATION_WITH_AES, SESSION_KEY));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			Sendable response;
			Sendable_Data _data;
			try {
				while ((response = (Sendable) input.readObject()) != null) {
					System.out.println("Server responded to Upload request");
					_data = response.decrypt(TRANSFORMATION_WITH_AES, SESSION_KEY);

					if (_data.getInfo()[0].equals("directory-upload-successful")) {
						System.out.println("Directory upload was successful");

						// Update UI
						currentDirFileShells = _data.getFileShells();
						mainPanel.updateButtons(currentDirFileShells);
					}
					break;
				}
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				return;
			}

			return;
		}
	}

	private class UploadFile implements Runnable {

		@Override
		public void run() {
			// Update UI
			mainPanel.addLoadingMessage("Waiting for Server");
			
			File selectedFile = jFileChooser.getSelectedFile();
			System.out.println("Selected file: " + selectedFile.getAbsolutePath());

			Sendable_Data data = new Sendable_Data();
			
//			try {
//				Thread.sleep(2000);
//			} catch (InterruptedException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}

			// Insert command
			String[] command = new String[1];
			command[0] = "save-file";
			data.fillInCommands(command);

			// Insert info
			String[] info = new String[2];
			info[0] = STORAGE_CURRENT_DIR;
			info[1] = selectedFile.getName();
			
			//			  bytes  kb     mB
			int _512_MB = 1024 * 1024 * 512;
			
			int file_length = (int) selectedFile.length();
			
			boolean multipleParts = false;
			if ((int) selectedFile.length() >= _512_MB) {
				// Make multiple parts of file and send seperately
				multipleParts = true;
//				int parts = 

				info = Arrays.copyOf(info, 4);
				info[2] = "multiple-parts";
				//info[3] = 
			}
			
			
			data.fillInInfo(info);

			// Insert file's bytes
			byte[][] bytes = new byte[1][(int) selectedFile.length()];
			try {
				FileInputStream fis = new FileInputStream(selectedFile);
				fis.read(bytes[0]);
				fis.close();

				data.fillInFiles(bytes);

				output.writeObject(SendableHelper.createSendableFromSendable_Data(data, TRANSFORMATION_WITH_AES, SESSION_KEY));

				Sendable response = null;
				while ((response = (Sendable) input.readObject()) != null) {
					Sendable_Data _data = response.decrypt(TRANSFORMATION_WITH_AES, SESSION_KEY);

					if (_data.getInfo()[0].equals("file-upload-successful")) {
						// Update UI
						currentDirFileShells = _data.getFileShells();
						mainPanel.updateButtons(currentDirFileShells);
						
						// Show Message
						mainPanel.removeLoadingMessage();
						mainPanel.addSuccessMessage("Upload successful!", 1000);
						System.out.println("File upload was successful");
					}
					break;
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				return;
			}
			return;
		}
	}

	private void saveFile(File destination, byte[] bytes) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(destination);
			fos.write(bytes);
			fos.flush();
			fos.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}


	private void stopFocusOnSpecificFile() {
		for (IconButton _ib : eFiles) {
			_ib.imageButton.setBorderThickness(7);
			_ib.imageButton.setBorderColor("#000000", true);
		}

		STORAGE_CURRENT_FILE = "";
		upperPanel.repaint();
	}
	
	private Image readWithImageIO(File source) {
		try {
			return ImageIO.read(source);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Adds a new instruction to the database
	 * @param path The absolute path of the file / directory to be automatically synchronized
	 * @param timer Timer in miliseconds
	 */
	synchronized void addAutoSyncInstruction(String path, int timer) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(Client.FILE_SYNC_AUTO_SYNC_INSTRUCTION, true));
			writer.write("<SYNC path=\"" + path + "\" timer=\"" + timer + "\" />" + System.lineSeparator());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	synchronized String[] readAutoSyncInstructions(File file) {
		String[] instructions = new String[0];
		try {
			BufferedReader reader = new BufferedReader(new FileReader(Client.FILE_SYNC_AUTO_SYNC_INSTRUCTION));
			String newLine;
			int line = 0;
			while ((newLine = reader.readLine()) != null) {
				instructions = Arrays.copyOf(instructions, instructions.length + 1);
				line++;
				instructions[line] = newLine;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instructions;
	}
	
	private class AutoSyncThread implements Runnable {
		String instruction;
		public AutoSyncThread (String instruction) {
			this.instruction = instruction;
		}
		@Override
		public void run() {
			String path;
			int timer;
			
//			inst
			
			instruction = instruction.substring(instruction.indexOf('"') + 1);
//			path = path.substring(0, path.indexOf('"'));
			path = instruction.substring(0, instruction.indexOf('"'));
			
//			instruction = instruction.substring()
			
			System.out.println("Path: " + path);
		}
	}
}
