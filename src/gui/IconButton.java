package gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class IconButton extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9210357135717224689L;
	Image image;
	String text;
	String[] single_words = new String[0];

	int img_width, img_height;

	int words_in_text = 1;
	int[] space_chars_at = new int[0];
	
	int numberInArray;
	
	public EButton imageButton;
	
	public String getText() {
		return this.text;
	}


	public IconButton(Image image, int width, int height, String text) {
		setLayout(null);
		setSize(width, height);
		this.image = image;
		this.text = text;

		img_width = (int) (width * 0.67);
		//		img_height = (int) (height * 0.67);
		img_height = img_width;

		imageButton = new EButton();
		imageButton.setSize(img_width, img_height);
		imageButton.drawBorders(true);
		imageButton.setImage(image, (int) (img_width * 0.8), (int) (img_height * 0.8));
		imageButton.setLocation((getWidth() - imageButton.getWidth()) / 2, 0);
		imageButton.addMouseListener(new IconMouseListener());
		imageButton.drawBorders(false);

		add(imageButton);
		
//		setBorder(BorderFactory.createLineBorder(Color.black));
		setBackground(Color.WHITE);

		filterOutSingleWords();
	}
	
	public void setNumberInArray(int numberInArray) {
		this.numberInArray = numberInArray;
	}
	
	public int getNumberInArray() {
		return this.numberInArray;
	}
	
	public void setImage(Image image) {
		this.image = image;
		imageButton.setImage(image, (int) (img_width * 0.8), (int) (img_height * 0.8));
		invalidate();
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		
		g2d.setFont(new Font("Helvetica", Font.BOLD, 21));

		int row = 1;
		int distance = 35;
		float row_0_y = (float) getHeight() * 0.67f;
		
		String composite = "";
		int current_length = 0;
		
		for (int i = 0; i < single_words.length; i++) {
			composite += single_words[i];
			current_length = g2d.getFontMetrics().stringWidth(composite);
			if (current_length >= img_width) {
				g2d.drawString(composite, Math.abs((current_length - getWidth()) / 2), row_0_y + distance * row);
				row++;
				composite = "";
			}
		}
		
		if (!composite.equals("")) g2d.drawString(composite, Math.abs((current_length - getWidth()) / 2), row_0_y + distance * row);
	}

	private void filterOutSingleWords() {
		int i = 0;
		int k = 0;
		for (char c : text.toCharArray()) {
			i++;
			if (c == ' ' || c == '-' || c == '_' || c == '.') {
//				System.out.println("Space right now at i value " + i);
				single_words = Arrays.copyOf(single_words, single_words.length + 1);
				single_words[single_words.length - 1] = text.substring(k, i);
				k = i;
			}
		}
		if (k < text.length()) {
			single_words = Arrays.copyOf(single_words, single_words.length + 1);
			single_words[single_words.length - 1] = text.substring(k, text.length());
		}
	}
	
	private class IconMouseListener implements MouseListener {
		boolean pressed = false;

		@Override
		public void mouseClicked(MouseEvent e) {
			imageButton.drawBorders(true);
			imageButton.setBorderThickness(15);
			imageButton.setBorderColor("#00FF00", true);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			pressed = true;
			
			imageButton.setSize((int) (imageButton.getWidth() * 1.1), (int) (imageButton.getHeight() * 1.1));
			imageButton.setLocation((getWidth() - imageButton.getWidth()) / 2, 0);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			pressed = false;
			
			imageButton.setSize(img_width, img_height);
			imageButton.setLocation((getWidth() - imageButton.getWidth()) / 2, 0);
			
			imageButton.drawBorders(false);
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			imageButton.drawBorders(true);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (!pressed) imageButton.drawBorders(false);
		}
		
	}

}
