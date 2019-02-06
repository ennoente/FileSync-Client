package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Message extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1981837851451799187L;

	public static final int TYPE_LOADING = 1;
	public static final int TYPE_SUCCESSFUL = 2;

	boolean showImage = false;
	boolean animate = false;

	File loading_file = new File (System.getProperty("user.home") + File.separator + "File Sync 2" + File.separator + ".background" + File.separator 
			+ "png" + File.separator + "loading.png");
	Image image;

	File FILE_SUCCESS = new File (System.getProperty("user.home") + File.separator + "File Sync 2" + File.separator + ".background" + File.separator
			+ "png" + File.separator + "success.png");
	
	

	int type = -1;
	int rotation = 0;
	int milis = 400;
	
	int image_width, image_height;
	
	float text_y;

	Timer rotation_timer;
	Animator ani;

	String message = "";

	public Message(int type, String message, int width, int height, int milis) {
		setLayout(null);
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
		setBackground(Color.WHITE);
		setSize (width, height);
		this.type = type;
		this.message = message;
		this.milis = milis;

		if (type == TYPE_LOADING) {
			// Setup timer
			rotation_timer = new Timer();

			try {
				image = ImageIO.read(loading_file);
			} catch (IOException e) {
				e.printStackTrace();
			}

			rotation_timer.scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					rotation += 5;
					repaint();
				}
			}, 0, 16);
			
			// Make file point to correct image
			
		} else if (type == TYPE_SUCCESSFUL) {
			try {
				image = ImageIO.read(FILE_SUCCESS);
			} catch (IOException e) {
				e.printStackTrace();
			}
			rotation = 0;;
			
			Timer destroy = new Timer();
			destroy.schedule(new TimerTask() {
				
				@Override
				public void run() {
//					System.out.println("Setting invisible after " + milis + " Miliseconds");
					setVisible (false);
					cancel();
				}
			}, milis);
		}

		int image_height = (int) (getHeight() * 0.5);
		int image_width = image_height;
		
		this.image_width = image_width;
		this.image_height = image_height;

		ani = new Animator();
		ani.setSize(image_width, image_height);
		ani.setLocation((getWidth() - ani.getWidth()) / 2, 30);
		
		text_y = getHeight() * 0.85f;

		add(ani);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		//		System.out.println("Drawing");
		//		Graphics2D graphics_image = (Graphics2D) g;

//		if (type == TYPE_LOADING) {
			Graphics2D graphics_string = (Graphics2D) g;
			graphics_string.setFont(new Font ("Helvetica", Font.BOLD, 30));

			float message_x = (getWidth() - graphics_string.getFontMetrics().stringWidth(message)) / 2;
//			float message_y = getHeight() * 0.85f;
//			text_y = getHeight() * 0.85f;
			
//			if (this instanceof OptionPane) text_y = getHeight() * 0.55f;
//			else text_y = message_y;

			graphics_string.drawString(message, message_x, text_y);
//			System.out.println("Drew '" + message + "' to " + message_x + "|" + message_y);
//		}
	}

	public class Animator extends JPanel {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6640240138018372101L;

		public Animator() {
			setBackground(Color.WHITE);
		}
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D graphics_image = (Graphics2D) g;

			graphics_image.rotate(Math.toRadians(rotation), getWidth() / 2, getHeight() * 0.5);
			//			graphics_image.drawImage(image, (int) (getWidth() * 0.167), 10, image_width, image_height, null);
			graphics_image.drawImage(image, 0, 0, getWidth(), getHeight(), null);
		}
	}

}
