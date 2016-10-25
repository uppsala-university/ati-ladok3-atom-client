package se.sunet.ati.ladok.atom;

@SuppressWarnings("serial")
public class UnexpectedClientResponseException extends Exception {
 
	private static final String MESSAGE = "Expects response code SUCCESS or CLIENT_ERROR. Got ";
	
	public UnexpectedClientResponseException(String message) {
        super(MESSAGE + message);
	}

}
