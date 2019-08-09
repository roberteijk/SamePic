package net.vandeneijk;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

public class ProcessController implements Runnable {
    private static final DataStore S_DATA_STORE = DataStore.getInstance();
    private static final int S_CORE_COUNT = Runtime.getRuntime().availableProcessors();

    private final Set<String> sAcceptedFileExtensions;

    private Path mExamplePath;
    private Path mSearchPath;
    private int mAllowedDeviation;
    private boolean mTraverseExamplePath;
    private boolean mTraverseSearchPath;
    private boolean mDuplicatesWithoutExample;
    private List<Path> mPathExampleList = new ArrayList<>();
    private List<Path> mSearchPathList = new ArrayList<>();
    private ExecutorService mExecService;

    public ProcessController(File examplePath, File searchPath, int allowedDeviation, boolean traverseExamplePath, boolean traverseSearchPath, boolean duplicatesWithoutExample) {
        mExamplePath = Paths.get(examplePath.toURI());
        mSearchPath = Paths.get(searchPath.toURI());
        mAllowedDeviation = allowedDeviation;
        mTraverseExamplePath = traverseExamplePath;
        mTraverseSearchPath = traverseSearchPath;
        mDuplicatesWithoutExample = duplicatesWithoutExample; // If mBtnStartStop is pressed to stop, this variable is set to true to prevent problematic situation where mBtnStartStop is pressed twice in quick succession.

        sAcceptedFileExtensions = S_DATA_STORE.getAcceptedFileExtensions();
    }

    public void run() {

        try {
            long startMillis = System.currentTimeMillis();
            Gui.showPleaseWaitAnimation(true);
            S_DATA_STORE.cleanUpOldDataBeforeNewRun();

            mExecService = Executors.newFixedThreadPool(S_CORE_COUNT);

            Gui.updateStatusBar("Reading File Information.");
            traversePath();
            createPicDataFromPath();

            // processPicData has its own status bar updates.
            processPicData();

            Gui.updateStatusBar("Calculation Results.");
            sendResultsToGui();

            Gui.updateStatusBar("Finished in " + ((System.currentTimeMillis() - startMillis) / 1000.0) + " seconds." );
        } catch (ProcessingAbortedException paEx) {
            return; // This instance of ProcessController will stop to exist and the GUI will not be updated with partly processed results.
        } finally {
            if (mExecService != null) mExecService.shutdownNow();
            Gui.showPleaseWaitAnimation(false);
            Gui.setProcessingActivated(false);
        }
    }

    /**
     * Traverse ExamplePath and SearchPath depending options and write results to the respective List.
     *
     * TODO Multi-threading could increase performance somewhat in certain situations. However, examples and search must be on different fysical drives. How to determine for different OS?
     */
    private void traversePath() throws ProcessingAbortedException {
        if (!mDuplicatesWithoutExample) {
            if (Files.isRegularFile(mExamplePath)) mPathExampleList.add(mExamplePath);
            else mPathExampleList = getPicPathList(mExamplePath, mTraverseExamplePath);
        }
        mSearchPathList = getPicPathList(mSearchPath, mTraverseSearchPath);
    }

    /**
     * Create PicData objects from Path objects created in the traversePath method and write to S_DATA_STORE.
     *
     * TODO Multi-threading could increase performance somewhat in certain situations. However, examples and search must be on different fysical drives. How to determine for different OS?
     */
    private void createPicDataFromPath() throws ProcessingAbortedException {
        long startMillis = System.currentTimeMillis();
        if (!mDuplicatesWithoutExample) {
            for (Path path : mPathExampleList) {
                try {
                    if (!Gui.isProcessingActivated()) throw new ProcessingAbortedException();
                    if (path == null) System.out.println("Path is null : " + path); // TODO Remove, is only for testing.
                    S_DATA_STORE.addToWaitingLinePicturesToArray(new PicData(path, true, false));
                } catch (MalformedURLException mfuEx) {
                    // May be ignored. File will be skipped though.
                }
            }
        }

        for (Path path : mSearchPathList) {
            try {
                if (!Gui.isProcessingActivated()) throw new ProcessingAbortedException();
                if (path == null) System.out.println("Path is null : " + path); // TODO Remove, is only for testing.
                S_DATA_STORE.addToWaitingLinePicturesToArray(new PicData(path, false, false));
            } catch (MalformedURLException mfuEx) {
                // May be ignored. File will be skipped though.
            }
        }

        System.out.println(System.currentTimeMillis() - startMillis);
    }

    /**
     * Asks S_DATA_STORE to create 4 1024 hashes from each picture as represented by a PicData object.
     */
    private void processPicData() throws ProcessingAbortedException {
        System.out.println(S_CORE_COUNT);

        List<Callable<Boolean>> processPicDataTasks = new ArrayList<>();
        for (int i = 0; i < S_CORE_COUNT; i++) {
            processPicDataTasks.add(() -> {
                PicData picToProcess;
                while ((picToProcess = S_DATA_STORE.getFromWaitingLinePicturesForProcessing()) != null) {
                    if (!Gui.isProcessingActivated()) throw new ProcessingAbortedException();
                    Gui.updateStatusBar("Processing : " + picToProcess.getPath());
                    try {
                        Thread.sleep(25); // Improves performance of other apps and GUI at a slight cost on multicore systems without hyper-threading.
                    } catch (InterruptedException iEx) {
                        iEx.printStackTrace();
                    }
                    new PicProcessor(picToProcess).run();
                }
                return true;
            });
        }

        try {
            List<Future<Boolean>> futures = mExecService.invokeAll(processPicDataTasks);
            for (Future<Boolean> future : futures) {
                if (!future.get());
            }
        } catch (InterruptedException | ExecutionException miscEx) {
            throw new ProcessingAbortedException();
        }
    }

    /**
     * Asks S_DATA_STORE for the results and sends the results (if any) to the Gui. PicData is compared on request. Finishing this method may take
     * some time depending the workload.
     */
    private void sendResultsToGui() throws ProcessingAbortedException {
        List<List<PicData>> result;

        if (!mDuplicatesWithoutExample) result = S_DATA_STORE.getSamePicResultsFromExample(mAllowedDeviation);
        else result = S_DATA_STORE.getSamePicResultsWithoutExample();

        Gui.setTree(result, !mDuplicatesWithoutExample);
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
                    if (sAcceptedFileExtensions.contains(fileNameSplit[fileNameSplit.length - 1])) picPathList.add(file);
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
        } catch (TraversingNotAllowedException psEx) {
            throw new ProcessingAbortedException();
        } catch (IOException e) {
            throw new ProcessingAbortedException(); // TODO Inform user about IOException in a under friendly way.
        }

        System.out.println("List contains a Path as null : " + picPathList.contains(null));
        return picPathList;
    }
}
