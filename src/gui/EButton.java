package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

public class EButton extends JPanel implements Clickable {
	/**
	 * This is an own Button-Class I wrote
	 * (c) Enno Thoma 17.01.2017 | last change 04.02.2017
	 * For that this Button will be used for all eternity
	 * and ascend into ever glowing, lasting glory.
	 */
	private static final long serialVersionUID = 1L;

	/* These values describe the standard attributes in case none are to be
	 * specified after creating a new E-Button object.
	 * By default, the border colour will be black and will change to a dark gray when hovered over.
	 * The background colour will be white, while the text will appear in black.
	 * Standard Font will be plain Helvetic, size 24; border thickness will be 7.
	 */

	private Font EFont = new Font("Helvetica", Font.BOLD, 29);

	private String text = "";
	private String borderColor = "#000000";
	private String borderColorEntered = "#a9a9a9";
	private String borderColorExited = borderColor;
	private String textColor = "#000000";
	private String backgroundColor = "#FFFFFF";

	private Image image = null;
	private boolean drawBorders = true;
	private int img_width;
	private int img_height;
	private boolean combineImageAndText = false;
	private int margin = 15;
	//	private boolean combineImageAndText = false;

	private boolean autoScale = false;

	private int borderThickness = 7;
	private int i = 0;

	private boolean pressed = false;

	private Timer timer;

	private Clickable clickable = new Clickable() {

		@Override
		public void onClicked() {
			// TODO Auto-generated method stub

		}
	};

	private boolean isClickable = true;

	/*
	 * invoked after having passed no specified parameters into the initialization 
	 */
	public EButton() {
		this.setBackgroundColor(backgroundColor);
		setMouseListener();

	}

	/*
	 * invoked after having passed a specified [String] text into the initialization
	 */
	public EButton(String text) {
		this.text = text;
		this.setBackgroundColor(backgroundColor);
		setMouseListener();
	}

	/*
	 * invokes after having passed a specified [String] text and a specified [integer] border thickness
	 * into the initialization
	 */
	public EButton(String text, int borderThickness) {
		this.text = text;
		this.borderThickness = borderThickness;
		this.setBackgroundColor(backgroundColor);
		setMouseListener();
	}

	/*
	 * returns current border colour.
	 */
	public String getBorderColor() {
		return borderColor;
	}

	/*
	 * returns the colour appearing when hovering over
	 */
	public String getBorderColorEntered() {
		return borderColorEntered;
	}

	/*
	 * returns the colour appearing when exiting the hovering-over process
	 */
	public String getBorderColorExited() {
		return borderColorExited;
	}

	/*
	 * returns current text colour
	 */
	public String getTextColor() {
		return textColor;
	}

	/*
	 * returns current border thickness
	 */
	public int getBorderThickness() {
		return borderThickness;
	}

	/*
	 * returns current Font(non-Javadoc)
	 * @see java.awt.Component#getFont()
	 */
	public Font getFont() {
		return EFont;
	}

	/*
	 * returns current background colour
	 */
	public String getBackgroundColor() {
		return backgroundColor;
	}

	public void setMouseListener() {
		addMouseListener(new MouseListener() {
			/*
			 * when clicking the EButton (non-Javadoc)
			 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
			 */
			public void mouseClicked(MouseEvent e) {
				// *CLICK*
				clickable.onClicked();
			}

			/*
			 * when releasing the EButton (non-Javadoc)
			 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
			 */
			public void mouseReleased(MouseEvent e) {
				if (isClickable) {
					pressed = false;
					if (image != null) {
						img_width -= 10;
						img_height -= 10;
					} else {
						EFont = new Font(EFont.getName(), EFont.getStyle(), EFont.getSize() - 4);
					}
					borderColor = borderColorExited;
					repaint();
				}
			}

			/*
			 * when pressing the EButton (non-Javadoc)
			 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
			 */
			public void mousePressed(MouseEvent e) {
				if (isClickable) {
					pressed = true;
					if (image != null) {
						img_width += 10;
						img_height += 10;
					} else {
						EFont = new Font(EFont.getName(), EFont.getStyle(), EFont.getSize() + 4);
					}
					repaint();
				}
			}

			/*
			 * when exiting the EButton (non-Javadoc)
			 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
			 */
			public void mouseExited(MouseEvent e) {
				if (isClickable) {
					if(!pressed)
						borderColor = borderColorExited;
					repaint();
				}
			}

			/*
			 * when entering the EButton (non-Javadoc)
			 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
			 */
			public void mouseEntered(MouseEvent e) {
				if (isClickable) {
					borderColor = borderColorEntered;
					repaint();
				}
			}
		});
	}

	/*
	 *  Define the border colour when the mouse ENTERS the object
	 */
	public void setBorderColorEntered(String borderColorEntered) {
		this.borderColorEntered = borderColorEntered;
		repaint();
	}

	/*
	 *  Define the border colour when the mouse EXITS the object
	 */
	public void setBorderColorExited(String borderColoredExited) {
		this.borderColorExited = borderColoredExited;
		repaint();
	}

	/**
	 * Change shown text
	 * @param newText The new text to be shown
	 */
	public void setText(String newText, boolean autoScale) {
		this.text = newText;
		this.autoScale = autoScale;
		repaint();
	}

	/*
	 *  Define text colour
	 */
	public void setTextColor(String textColor) {
		this.textColor = textColor;
		repaint();
	}

	/*
	 *  Define Border-Thickness
	 */
	public void setBorderThickness(int borderThickness) {
		this.borderThickness = borderThickness;
		repaint();
	}

	/*
	 *  Set border color (accepts String -> Hex-Code)
	 */
	public void setBorderColor(String color, boolean borderColorExitedEqualsBorderColor) {
		this.borderColor = color;
		if(borderColorExitedEqualsBorderColor)
			this.borderColorExited = this.borderColor;
		repaint();
	}

	/*
	 *  Set specified Font(non-Javadoc)
	 * @see javax.swing.JComponent#setFont(java.awt.Font)
	 */
	public void setFont(Font f) {
		this.EFont = f;
		repaint();
	}

	/* 
	 * Defines the background color of the button | standard "F3FaB6"
	 * Note: argument given in String hex-code
	 */
	public void setBackgroundColor(String color) {
		this.backgroundColor = color;
		setBackground(Color.decode(color));
		repaint();
	}

	public void setClickable(boolean clickable) {
		this.isClickable  = clickable;
		if (clickable) {
			setBorderColor(borderColor, true);
			//			setBackgroundColor(backgroundColor);
			setBackgroundColor("#FFFFFF");
			//			setMouseListener();
		} else {
			setBorderColor("#808080", true);
			setBackgroundColor("#808080");
			//			removeMouseListener(getMouseListeners()[0]);
		}
		setEnabled(clickable);
	}
	
	public static Image getImageByName(String filename) {
		File file_in_dir = new File (System.getProperty("user.home") + File.separator + "File Sync 2" + File.separator + ".background" + File.separator
				+ "png" + File.separator + filename + ".png");
		try {
			return ImageIO.read(file_in_dir);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setOnClick(Clickable clickListener) {
		this.clickable = clickListener;
	}

	public void setFontSize (int size) {
		this.EFont = new Font(EFont.getFontName(), EFont.getStyle(), size);
		invalidate();
	}

	public void setImage(Image img, int width, int height) {
		this.image = img;
		this.img_width = width;
		this.img_height = height;

		invalidate();
	}

	public void combineImageAndText (Image image, int image_width, int image_height, String text, int margin) {
		combineImageAndText = true;
		this.image = image;
		this.img_width = image_width;
		this.img_height = image_height;
		this.text = text;
		this.margin = margin;
		repaint();
	}
	
	public void combineImageAndText (Image image, String text, int margin) {
		combineImageAndText = true;
		this.image = image;
		this.img_width = (int) (getWidth() * 0.7);
		this.img_height = (int) (getHeight() * 0.7);
		this.text = text;
		this.margin = margin;
		repaint();
	}

	public void setOnlyText() {
		combineImageAndText = false;
	}

	public void drawBorders(boolean drawBorders) {
		this.drawBorders = drawBorders;
		invalidate();
	}

	//	public void combineImageAndText(boolean combine) {
	//		this.combineImageAndText = combine;
	//	}

	/*
	 * Makes the border flicker for @flickTimes times in
	 * the given color @flickerColor
	 */
	public void flickerBorders(String flickerColor, int numberOfFlicks) {
		String borderColorRemind = borderColor;
		/*
		 *  First "Flicker"
		 */
		borderColor = flickerColor;
		/*
		 *  Every time the method is invoked it starts with i = 0, making it start from scratch
		 */
		i = 0;
		timer = new Timer(125, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				i++;

				if(borderColor == borderColorRemind)
					borderColor = flickerColor;
				else
					borderColor = borderColorRemind;

				if(i == numberOfFlicks)
					timer.stop();

				repaint();
			}
		});
		timer.start();
	}

	/*
	 * paints the borders and writes the text on the EButton 
	 * (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;

		//		if (super.getWidth() * 0.9 - g2d.getFontMetrics().stringWidth(text) )
		g2d.setFont(EFont);

		if (autoScale) {
			/* Write text
			 * Methods:
			 * setFont(Font f) can modify the Font used | standard: "Tahoma", Font.PLAIN, 20
			 * setTextColor(String color) can modify text color | standard: "#000000" - Color.WHITE
			 */
			EFont = new Font(EFont.getName(), EFont.getStyle(), 0);
			while (g2d.getFontMetrics().stringWidth(text) < this.getWidth() * 0.75) {
				EFont = new Font(EFont.getName(), EFont.getStyle(), EFont.getSize() + 1);
				g2d.setFont(EFont);
			}
		}

		//		if (g2d.getFontMetrics().stringWidth(text) > super.getWidth() * 0.9) {
		//			this.EFont = new Font(EFont.getName(), EFont.getStyle(), EFont.getSize() - 1);
		//			repaint();
		//		}

		/* Draw borders on buttons
		 * Methods:
		 * setBorders(String color)	can modify the border color | standard: "#000000" - Color.WHITE
		 * setBorderThickness(int thickness) can modify the border-thickness | standard: 7
		 */

		// Draw borders if not only image should be drawn
		if (drawBorders) {
			g2d.setColor(Color.decode(borderColor));
			g2d.setStroke(new BasicStroke(borderThickness));
			g2d.drawRect(0, 0, super.getWidth(), super.getHeight());
		}

		// Draw text
		g2d.setColor(Color.decode(textColor));
		//		g2d.drawString(text, (super.getWidth() - g2d.getFontMetrics().stringWidth(text)) / 2, (int) (super.getHeight() * 0.67));
		if (combineImageAndText) {
			int combined_width = img_width + g2d.getFontMetrics().stringWidth(text) + margin;
			int image_x = (getWidth() - combined_width) / 2;
			int image_y = (getHeight() - img_height) / 2;
			g2d.drawImage(image, image_x, image_y, img_width, img_height, null);
			g2d.drawString(text, image_x + img_width + margin, (super.getHeight() + EFont.getSize()) / 2);
		} else
			g2d.drawString(text, (super.getWidth() - g2d.getFontMetrics().stringWidth(text)) / 2, (super.getHeight() + EFont.getSize()) / 2);
		
		if (!combineImageAndText)
			g2d.drawImage(image, (getWidth() - img_width) / 2, (getHeight() - img_height) / 2, img_width, img_height, null);
	}

	@Override
	public void onClicked() {}
}