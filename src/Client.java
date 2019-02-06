import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import gui.Clickable;
import gui.EButton;
import gui.Message;
import gui.OptionPane;

public class Client {
	static final String COMMAND_LOGIN = "login";

	static int screen_width;
	static int screen_height;

	static final String USER_HOME_PATH = System.getProperty("user.home");
	static final File FILE_SYNC_DIRECTORY_ROOT = new File(USER_HOME_PATH + File.separator + "File Sync 2");

	/*
	 * Cookie file containing username and one-time valid access token
	 * used for "Remember me" - funcionality
	 */
	static final File FILE_SYNC_DIRECTORY_ENGINE = new File(FILE_SYNC_DIRECTORY_ROOT + File.separator + ".background");

	static final File FILE_SYNC_PNG_DIR = new File (FILE_SYNC_DIRECTORY_ENGINE + File.separator + "png");

	static final File FILE_SYNC_COOKIE_REMEMBER_ME_SESSION = new File(FILE_SYNC_DIRECTORY_ENGINE + File.separator + "session.cookie");

	static final File FILE_SYNC_DIRECTORY_STORAGE = new File(FILE_SYNC_DIRECTORY_ROOT + "");
	
	static final File FILE_SYNC_AUTO_SYNC_INSTRUCTION = new File (FILE_SYNC_DIRECTORY_ENGINE + File.separator + "Automatics.ini");

	static JFrame frame;
	static EButton login, register;
	static JTextField usernameField;
	static JPasswordField passwordField;
	static EButton checkRememberMe;

	static String username;
	static char[] password;
	static boolean rememberMe = true;

	static boolean enteredData = false;

	Thread connectionThread;
	ConnectionEstablisher connectionEstablisher;
	static Client client;
	
	static Message m;

	Font standard_font = new Font("Helvetica", Font.BOLD, 28);
	
	static SystemTray tray;
	static TrayIcon icon;

	public Client() {
		//		if (FILE_SYNC_DIRECTORY_ENGINE.mkdirs()) System.out.println("Created File Sync Directories!");

		System.out.println("New Client: Thread " + Thread.currentThread());
		connectionEstablisher = new ConnectionEstablisher();
		connectionThread = new Thread(connectionEstablisher);
		connectionThread.start();

		// Get width and height of screen to build proper GUI
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		screen_width = screenSize.width;
		screen_height = screenSize.height;

		System.out.println("Building GUI");
		buildGUI();
		
		// Add File Sync 2 to System Tray
		addToSystemTray();

		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) { }

			@Override
			public void windowIconified(WindowEvent e) {
				// Minimized
				frame.setVisible(false);
			}

			@Override
			public void windowDeiconified(WindowEvent e) { }

			@Override
			public void windowDeactivated(WindowEvent e) { }

			@Override
			public void windowClosing(WindowEvent e) {
				int confirm = JOptionPane.showOptionDialog(frame, "Sure to quit?", "Quitting File Sync 2", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
				if (confirm == JOptionPane.YES_OPTION) System.exit(0);
			}

			@Override
			public void windowClosed(WindowEvent e) { }

			@Override
			public void windowActivated(WindowEvent e) { }
		});
	}

	private void addToSystemTray() {
		if (SystemTray.isSupported() && tray == null && icon == null) {
			try {
				tray = SystemTray.getSystemTray();
				
				File iconFile = new File (FILE_SYNC_PNG_DIR + File.separator + "png" + ".png");
				Image image = ImageIO.read(iconFile);
				
				icon = new TrayIcon(image);
				icon.setImageAutoSize(true);
				icon.setToolTip("Open File Sync 2");
				icon.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
//						if (frame != null) frame.setState(Frame.NORMAL);
//						else if (ConnectionEstablisher.mainFrame != null) ConnectionEstablisher.mainFrame.setState(Frame.NORMAL);
						if (frame != null) {
							frame.setVisible(true);
							frame.setState(Frame.NORMAL);
						} else if (ConnectionEstablisher.mainFrame != null) {
							ConnectionEstablisher.mainFrame.setVisible(true);
							ConnectionEstablisher.mainFrame.setState(Frame.NORMAL);
						}
					}
				});
				
				tray.add(icon);
			} catch (IOException | AWTException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void buildGUI() {
		frame = new JFrame("FileSync 2");
		frame.pack();
		frame.setSize(450, 600);
		frame.setLocation((screen_width - frame.getWidth()) / 2, (screen_height - frame.getHeight()) / 2);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(null);

		frame.setVisible(true);
		
		JPanel panel = new JPanel(null);
//		panel.setSize(frame.().getSize());
//		panel.setSize(426, 536);
		panel.setSize(frame.getContentPane().getSize());
		panel.setBackground(Color.WHITE);
		
		System.out.println("Panel size: " + panel.getSize());

		JLabel lUsername = new JLabel("Username");
		lUsername.setSize(300, 50);
		lUsername.setFont(standard_font);
		lUsername.setLocation(190, 20);

		usernameField = new JTextField();
		usernameField.setSize(350, 85);
		usernameField.setLocation(25, 70);
		usernameField.setFont(standard_font);
		usernameField.setOpaque(false);

		JLabel lPassword = new JLabel("Password");
		lPassword.setSize(300, 50);
		lPassword.setFont(standard_font);
		lPassword.setLocation(190, 170);

		passwordField = new JPasswordField();
		passwordField.setSize(350, 85);
		passwordField.setLocation(25, 220);
		passwordField.setFont(standard_font);
		passwordField.setOpaque(false);
		
		final File checked_file = new File (FILE_SYNC_PNG_DIR + File.separator + "checked.png");
		
		checkRememberMe = new EButton();
		checkRememberMe.setSize (75, 75);
		checkRememberMe.setLocation(25, 320);
		try {
			checkRememberMe.setImage(ImageIO.read(checked_file), 60, 60);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		checkRememberMe.setOnClick(new Clickable() {
			
			@Override
			public void onClicked() {
				rememberMe = !rememberMe;
				if (rememberMe)
					try {
						checkRememberMe.setImage(ImageIO.read(checked_file), 60, 60);
					} catch (IOException e) {
						e.printStackTrace();
					}
				else checkRememberMe.setImage(null, 0, 0);
			}
		});

//		checkRememberMe = new EButton("Remember me");
//		checkRememberMe.setSize(250, 75);
//		checkRememberMe.setLocation(25, 320);
//		checkRememberMe.setFont(standard_font);
//		usernameField.setOpaque(false);
//		checkRememberMe.addMouseListener(new MouseListener() {
//			@Override
//			public void mouseReleased(MouseEvent e) {}
//			@Override
//			public void mousePressed(MouseEvent e) {}
//			@Override
//			public void mouseExited(MouseEvent e) {}
//			@Override
//			public void mouseEntered(MouseEvent e) {}
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				rememberMe = !rememberMe;
//				if (rememberMe) {
//					checkRememberMe.setText("Remember me", true);
//				} else {
//					checkRememberMe.setText("Don't remember me", true);
//				}
//			}
//		});

		login = new EButton("Login");
		login.setSize(160, 85);
		login.setLocation(25, 430);
		login.setClickable(false);
		login.setOpaque(false);

		register = new EButton("Register");
		register.setSize(160, 85);
		register.setLocation(215, 430);
		register.setClickable(false);
		register.setOpaque(false);
		
		m = new Message(Message.TYPE_LOADING, "Logging in", 300, 300, 500);
		m.setLocation((panel.getWidth() - m.getWidth()) / 2, (panel.getHeight() - m.getHeight()) /2);
		m.setOpaque(true);
		m.setVisible(false);
		
//		OptionPane op = new OptionPane(Message.TYPE_LOADING, "Logging in", 300, 300, 5000);
//		op.setLocation((panel.getWidth() - m.getWidth()) / 2, (panel.getHeight() - m.getHeight()) /2);
//		op.setOpaque(true);
////		op.setVisible(false);
//		op.setOnCancelClicked(new Clickable() {
//			
//			@Override
//			public void onClicked() {
//				op.setVisible(false);
//			}
//		});
		
		frame.add(panel);
		
//		panel.add(op);
		panel.add(m);

		panel.add(lUsername);
		panel.add(usernameField);	
		panel.add(lPassword);
		panel.add(passwordField);
		panel.add(checkRememberMe);
		panel.add(login);
		panel.add(register);
		
		frame.repaint();
	}

	/**
	 * Filters the typed username and password into the variables
	 * {@code username} and {@code password}, respectively
	 */
	static void filterUsernameAndPasswordIntoVariables() {
		username = usernameField.getText();
		password = passwordField.getPassword();
	}

	public static void main(String[] args) {
		// Create the FileSync engine / background directory
		FILE_SYNC_DIRECTORY_ENGINE.mkdirs();

		client = new Client();
	}

	public void stop() {
		connectionThread.interrupt();
		connectionEstablisher = null;
		connectionThread = null;
	}

}
