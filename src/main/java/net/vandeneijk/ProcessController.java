/**
 * Class that starts and keeps check on various 'under the hood' processes as started from the Gui class.
 *
 * @author Robert van den Eijk
 */

package net.vandeneijk;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

public class ProcessController implements Runnable {

    // Final class variables.
    private static final int S_CORE_COUNT = Runtime.getRuntime().availableProcessors();

    // Variables related to the constructor.
    private Path mExamplePath;
    private Path mSearchPath;
    private int mAllowedDeviation;
    private boolean mTraverseExamplePath;
    private boolean mTraverseSearchPath;
    private boolean mUseExamplePath;
    private static final Set<String> sAcceptedFileExtensions = new HashSet<>();

    // Variables filled and initialized by method calls and statements in this class.
    private PicsProcessor mPicsProcessor = new PicsProcessor();
    private List<Path> mPathExampleList = new ArrayList<>();
    private List<Path> mSearchPathList = new ArrayList<>();
    private Map<Path, PicData> mWaitingLinePicForProcessing = new HashMap<>();
    private ExecutorService mExecService;
    private int mCountProcessing = 0;



    ProcessController(File examplePath, File searchPath, int allowedDeviation, boolean traverseExamplePath, boolean traverseSearchPath, boolean useExamplePath) {
        mExamplePath = Paths.get(examplePath.toURI());
        mSearchPath = Paths.get(searchPath.toURI());
        mAllowedDeviation = allowedDeviation;
        mTraverseExamplePath = traverseExamplePath;
        mTraverseSearchPath = traverseSearchPath;
        mUseExamplePath = useExamplePath;

        sAcceptedFileExtensions.add("jpg");
        sAcceptedFileExtensions.add("jpeg");
        sAcceptedFileExtensions.add("png");
        sAcceptedFileExtensions.add("bmp");
    }



    public void run() {
        try {
            long startMillis = System.currentTimeMillis();
            Gui.showPleaseWaitAnimation(true);

            mExecService = Executors.newFixedThreadPool(S_CORE_COUNT);



            Gui.updateStatusBar("Reading File Information.");
            traversePath();
            createPicDataFromPath();

            // preProcessPicData has its own status bar updates.
            preProcessPicData();

            // sendResultsToGui has its own status bar updates.
            sendResultsToGui();

            Gui.updateStatusBar("Finished in " + ((System.currentTimeMillis() - startMillis) / 1000.0) + " seconds." );
        } catch (ProcessingAbortedException paEx) { // This instance of ProcessController will stop to exist and the GUI will NOT be updated with partly processed results.
            Gui.updateStatusBar("Processing aborted!");
        } finally {
            if (mExecService != null) mExecService.shutdownNow();
            Gui.showPleaseWaitAnimation(false);
            Gui.setProcessingActivated(false);
        }
    }

    private synchronized PicData getFromWaitingLinePicForProcessing() {
        PicData picData = null;

        for (Path path : mWaitingLinePicForProcessing.keySet()) {
            picData = mWaitingLinePicForProcessing.get(path);
            mWaitingLinePicForProcessing.remove(path);
            break;
        }

        return picData;
    }

    /**
     * Traverses ExamplePath and SearchPath depending options and writes results to their respective List.
     */
    private void traversePath() throws ProcessingAbortedException {
        if (mUseExamplePath) {
            if (Files.isRegularFile(mExamplePath)) mPathExampleList.add(mExamplePath);
            else mPathExampleList = getPicPathList(mExamplePath, mTraverseExamplePath);
        }
        mSearchPathList = getPicPathList(mSearchPath, mTraverseSearchPath);
    }

    /**
     * Creates PicData objects from Path objects created in the traversePath method and writes to mWaitingLinePicForProcessing.
     */
    private void createPicDataFromPath() throws ProcessingAbortedException {
        if (mUseExamplePath) {
            for (Path path : mPathExampleList) {
                try {
                    if (!Gui.isProcessingActivated()) throw new ProcessingAbortedException();
                    mWaitingLinePicForProcessing.put(path, new PicData(path, true, false));
                } catch (MalformedURLException mfuEx) {
                    // May be ignored. File will be skipped though.
                }
            }
        }

        for (Path path : mSearchPathList) {
            try {
                if (!Gui.isProcessingActivated()) throw new ProcessingAbortedException();
                mWaitingLinePicForProcessing.put(path, new PicData(path, false, false));
            } catch (MalformedURLException mfuEx) {
                // May be ignored. File will be skipped though.
            }
        }
    }

    /**
     * Creates multiple Callables depending S_CORE_COUNT. Each Callable will process work from mWaitingLinePicForProcessing as long as new work is
     * available. The PicPreProcessor class called from within does the heavy lifting.
     */
    private void preProcessPicData() throws ProcessingAbortedException {
        List<Callable<Boolean>> processPicDataTasks = new ArrayList<>();
        final int amountProcessing = mWaitingLinePicForProcessing.size();

        for (int i = 0; i < S_CORE_COUNT; i++) {
            processPicDataTasks.add(() -> {
                PicData picToProcess;
                while ((picToProcess = getFromWaitingLinePicForProcessing()) != null) {
                    if (!Gui.isProcessingActivated()) throw new ProcessingAbortedException();
                    Gui.updateStatusBar("Pre-processing picture " + ++mCountProcessing + " of " + amountProcessing + ": " + picToProcess.getPath());
                    try {
                        Thread.sleep(25); // Improves performance of other apps and GUI at a slight cost on multicore systems without hyper-threading.
                    } catch (InterruptedException iEx) {
                        // Ignore for now.
                    }
                    new PicPreProcessor(picToProcess, mPicsProcessor).run();
                }
                return true;
            });
        }

        try {
            List<Future<Boolean>> futures = mExecService.invokeAll(processPicDataTasks); // Waits here until all Callables have returned a result or thrown an exception.
            for (Future<Boolean> future : futures) {
                future.get(); // If an exception was thrown in the Callable, it will be rethrown because of this statement and catched in the catch clause below.
            }
        } catch (InterruptedException | ExecutionException miscEx) {
            throw new ProcessingAbortedException();
        }
    }

    /**
     * Asks mPicsProcessor for the results and sends the results (if any) to the Gui. mPicsProcessor does some heavy lifting by comparing all the
     * 1024 bit hashes from PicData objects created by PicPreProcessor.
     */
    private void sendResultsToGui() throws ProcessingAbortedException {
        List<List<PicData>> result;

        if (mUseExamplePath) result = mPicsProcessor.getSamePicResultsFromExample(mAllowedDeviation);
        else result = mPicsProcessor.getSamePicResultsWithoutExample();

        Gui.setTree(result, mUseExamplePath);
    }

    /**
     * This method traverses a directory and depending settings also subdirectories to search for files with certain file extensions. When found,
     * these are added as Path object to picPathList.
     *
     * @param rootPath
     * @param traversePath
     * @return
     */
    private List<Path> getPicPathList(Path rootPath, boolean traversePath) throws ProcessingAbortedException{
        List<Path> picPathList = new ArrayList<>();

        try {
            Files.walkFileTree(rootPath, new HashSet<FileVisitOption>(Arrays.asList(FileVisitOption.FOLLOW_LINKS)), 100, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Gui.isProcessingActivated()) throw new TraversingNotAllowedException();
                    String[] fileNameSplit = file.toString().split("\\.");
                    if (sAcceptedFileExtensions.contains(fileNameSplit[fileNameSplit.length - 1].toLowerCase())) picPathList.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!traversePath) {
                        if (rootPath.equals(dir)) return FileVisitResult.CONTINUE;
                        else return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new ProcessingAbortedException();
        }

        return picPathList;
    }
}