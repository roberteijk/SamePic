/**
 * Class for custom Exception handling. This exception is thrown when the "stop" button is pressed in the GUI. This prevents running the batch
 * of methods as coded in ProcessController.
 *
 * @author Robert van den Eijk
 */

package net.vandeneijk;

public class ProcessingAbortedException extends Exception {}