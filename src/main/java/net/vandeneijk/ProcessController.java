package net.vandeneijk;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ProcessController implements Runnable {
    private static final DataStore S_DATA_STORE = DataStore.getInstance();

    private final Set<String> sAcceptedFileExtensions;

    private Path mExamplePath;
    private Path mSearchPath;
    private boolean mTraverseExamplePath;
    private boolean mTraverseSearchPath;
    private boolean mDuplicatesWithoutExample;
    private List<Path> mPathExampleList = new ArrayList<>();
    private List<Path> mSearchPathList = new ArrayList<>();

    public ProcessController(File examplePath, File searchPath, boolean traverseExamplePath, boolean traverseSearchPath, boolean duplicatesWithoutExample) {
        sAcceptedFileExtensions = S_DATA_STORE.getAcceptedFileExtensions();
        mExamplePath = Paths.get(examplePath.toURI());
        mSearchPath = Paths.get(searchPath.toURI());
        mTraverseExamplePath = traverseExamplePath;
        mTraverseSearchPath = traverseSearchPath;
        mDuplicatesWithoutExample = duplicatesWithoutExample;
    }

    public void run() { // TODO Build in break moments to act when Stop button is pressed.
        // --------------------------------------------------------------------
        // Traverse ExamplePath and SearchPath depending options and write results to the respective List.
        if (!mDuplicatesWithoutExample) {
            if (Files.isRegularFile(mExamplePath)) mPathExampleList.add(mExamplePath);
            else mPathExampleList = getPicPathList(mExamplePath, mTraverseExamplePath);
        }
        mSearchPathList = getPicPathList(mSearchPath, mTraverseSearchPath);



        // --------------------------------------------------------------------
        // Write Path objects inside a Picture objects to S_DATA_STORE.

        if (!mDuplicatesWithoutExample) { // TODO Build in break and multithreading.
            for (Path path : mPathExampleList) {
                try {
                    S_DATA_STORE.addToWaitingLinePicturesToArray(new Picture(path, true));
                } catch (MalformedURLException mfuEx) {
                    // May be ignored. File will be skipped though.
                }
            }

            System.out.println("!!!Calc All Done for Example!!!");
        }



        for (Path path : mSearchPathList) { // TODO Build in break and multithreading.
            try {
                S_DATA_STORE.addToWaitingLinePicturesToArray(new Picture(path, false));
            } catch (MalformedURLException mfuEx) {
                // May be ignored. File will be skipped though.
            }
        }

        System.out.println("!!!Calc All Done for Search!!!");



        // --------------------------------------------------------------------
        // Pic to Hash // TODO In testing phase.

        // java.lang.OutOfMemoryError: Java heap space
        // Possible fix, don't process all pics to array and store. Make array, then hash and store hash instead.

        Picture picToProcess;
        while ((picToProcess = S_DATA_STORE.getFromWaitingLinePicturesForProcessing()) != null) {
            System.out.println("Processing : " + picToProcess.getPath());
            new PicProcessor(picToProcess).run();
        }

        System.out.println("!!! Calc All Done for picToProcess");







    }

    private List<Path> getPicPathList(Path rootPath, boolean traversePath) {
        List<Path> picPathList = new ArrayList<>();

        try {
            Files.walkFileTree(rootPath, new HashSet<FileVisitOption>(Arrays.asList(FileVisitOption.FOLLOW_LINKS)), 100, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file , BasicFileAttributes attrs) throws IOException {
                    String[] fileNaameSplit = file.toString().split("\\.");
                    if (sAcceptedFileExtensions.contains(fileNaameSplit[fileNaameSplit.length - 1])) picPathList.add(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file , IOException e) throws IOException {
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
            e.printStackTrace(); // TODO Better exception handling.
        }

        return picPathList;
    }
}
