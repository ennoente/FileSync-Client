package gui;

/**
 * Interface for a click listener
 * Designed and implemented for EButtons
 * 
 * @author Enno Thoma
 *
 */
public interface Clickable {
	/**
	 * Invoked when an EButton object is clicked on
	 * To implement, set {.setOnClickListener(Clickable)} with a new
	 * instance of the Clickable interface and override {onClicked()}.
	 */
	public abstract void onClicked();
}
