package net.buddat.kangabot;

/**
 * KangaBot-specific exceptions.
 */
class KangaBotException extends Exception {
	/**
	 * Constructs a new exception with the specified message
	 * @param message	Detail message indicating the cause of the exception
	 */
	KangaBotException(String message) {
        super(message);
    }

	/**
	 * Constructs a new exception with a specified message and an exception
	 * that was the underlying cause.
	 * @param message Detail message indicating the problem encountered
	 * @param e Exception encapsulates the underlying cause of the problem
	 */
	KangaBotException(String message, Throwable e) {
        super(message, e);
    }
}
