/**
 * Class for custom Exception handling. This class is a special substitute for ProcessingAbortedException. It is thrown in a overridden method where
 * only IOExceptions are allowed to be thrown. When catched outside this method, a ProcessingAbortedException is thrown in the catch clause.
 *
 * @author Robert van den Eijk
 */

package net.vandeneijk;

import java.io.IOException;

public class TraversingNotAllowedException extends IOException {}