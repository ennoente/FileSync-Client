package gui;

public class OptionPane extends Message {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6703515468386658822L;
	EButton cancel;
	Clickable clickable;
	
	public OptionPane(int type, String message, int width, int height, int milis) {
		super(type, message, width, height, milis);
		
		ani.setSize((int) (getHeight() * 0.35), (int) (getHeight() * 0.35));
		ani.setLocation((getWidth() - ani.getWidth()) / 2, 30);
		text_y = getHeight() * 0.55f;
		
		cancel = new EButton();
		cancel.setSize((int) (width * 0.65), (int) (width * 0.25));
		cancel.setLocation((width - cancel.getWidth()) / 2, height - cancel.getHeight() - 15);
		cancel.setFontSize(23);
		cancel.combineImageAndText(EButton.getImageByName("cancel"), (int) (cancel.getHeight() * 0.7) , (int) (cancel.getHeight() * 0.7), "Cancel", 8);
		
		add (cancel);
	}
	
	public void setOnCancelClicked(Clickable actionListener) {
		cancel.setOnClick(actionListener);
	}
	
//	public OptionPane(int width, int height, String message, Clickable actionListener) {
//		setSize (width, height);
//		
//	}
	
	public void setActionListener(Clickable newActionListener) {
		this.clickable = newActionListener;
		repaint();
	}

}
