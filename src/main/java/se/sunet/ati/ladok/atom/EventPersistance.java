package se.sunet.ati.ladok.atom;

import org.apache.abdera.model.Entry;

/**
 * Ett interface som styr hur vi håller reda på vilka händelser som har setts.
 * 
 */
public interface EventPersistance {

	/**
	 * Spara en händelse
	 * 
	 * @param e händelse att spara
	 * @return Den sparade händelsen. Null om inget sparades.
	 */
	public Entry saveEntry(Entry e) throws Exception;
	
	/**
	 * Verifiera om en händelse redan har setts
	 * 
	 * @param e Händelse att verifiera
	 * 
	 * @return Returnerar TRUE om händelsen inte har setts ännu
	 */
	public boolean isUnseenEntry(Entry e);

	/**
	 *  Return the latest logged event entry
	 * 
	 * @return the latest logged event entry
	 */
	public String getLastReadEntryId();

}