package net.vandeneijk;

import javafx.application.Platform;
import javafx.beans.binding.When;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class DataStore {
    private static final DataStore S_INSTANCE = new DataStore();

    private Set<String> sAcceptedFileExtensions = new HashSet<>();

    private Map<Path, PicData> mWaitingLinePicForProcessing = new HashMap<>();
    private Map<Path, PicData> mExamplePicFinishedHashing = new HashMap<>();
    private Map<Path, PicData> mTestPicFinishedHashing = new HashMap<>();
    private Map<Short, Set<Path>> mPicHashResults = new HashMap<>();

    private DataStore() {
        sAcceptedFileExtensions.add("jpg");
    }

    public static synchronized DataStore getInstance() {
        return S_INSTANCE;
    }

    public Set<String> getAcceptedFileExtensions() {
        return new HashSet<>(sAcceptedFileExtensions);
    }

    public void cleanUpOldDataBeforeNewRun() {
        mWaitingLinePicForProcessing.clear();
        mExamplePicFinishedHashing.clear();
        mTestPicFinishedHashing.clear();
        mPicHashResults.clear();
    }

    public void addToWaitingLinePicturesToArray(PicData picData) {
        mWaitingLinePicForProcessing.put(picData.getPath(), picData);
    }

    public synchronized PicData getFromWaitingLinePicturesForProcessing() {
        PicData picData = null;

        for (Path path : mWaitingLinePicForProcessing.keySet()) {
            picData = mWaitingLinePicForProcessing.get(path);
            mWaitingLinePicForProcessing.remove(path);
            break;
        }

        return picData;
    }

    public void addToPictureFinishedProcessing(PicData picData) {
        mTestPicFinishedHashing.put(picData.getPath(), picData);
    }

    public void addToExamplePictureFinishedHashing(PicData picData) {
        mExamplePicFinishedHashing.put(picData.getPath(), picData);
    }
    /*
    public void addAllToEntirePicHashResults(short hash1024bAmountOfOnes, Set<Long> foundHashes, Path path) {
        mEntirePicHashResults.computeIfAbsent(hash1024bAmountOfOnes, x -> new HashMap<>());

        for (long hash : foundHashes) {
            mEntirePicHashResults.get(hash1024bAmountOfOnes).computeIfAbsent(hash, k -> new HashSet<>());
            mEntirePicHashResults.get(hash1024bAmountOfOnes).get(hash).add(path);
        }
    }
    */

    public void addAllToEntirePicHashResults(short hash1024bAmountOfOnes, Path path) {
        mPicHashResults.computeIfAbsent(hash1024bAmountOfOnes, x -> new HashSet<>());
        mPicHashResults.get(hash1024bAmountOfOnes).add(path);
    }

    /**
     * This method iterates through a Map of example pictures which all finished the hashing related methods. First, for every iteration a List
     * subResult is created and filled with the examplePic of that iteration. Then a Set called filteredSet is created and filled with Path objects
     * from all entries in the mPicHashResults that have that same number of ones in the 1024 bit hash. This is a quick and dirty pre-filter to spare
     * some CPU resources. After that, the resulting Path objects are first checked against the mTestPicFinishedHashing in another for-loop. This to
     * ensure no examplePic can ne tested against a testPic. Then the examplePic and testPic are tested in a dedicated method called calcIfSameEnough.
     * If a true returns, testPic is added to subResult. After this for-loop, another test is done to check if subResult is bigger than 1 (index 0 is
     * reserved for examplePic). If so, subResult is added to result and the next iteration for a new examplePic is started.
     *
     * @param allowedDeviation
     * @return
     */
    public List<List<PicData>> getSamePicResultsFromExample(int allowedDeviation) throws ProcessingAbortedException {
        List<List<PicData>> result = new ArrayList<>();

        for (PicData examplePic : mExamplePicFinishedHashing.values()) { // Loops through all the example pictures.
            if (!Gui.isProcessingActivated()) throw new ProcessingAbortedException();
            List<PicData> subResult = new ArrayList<>(); // Creates an ArrayList for storing pics that match within specification.
            subResult.add(examplePic); // Adds examplePic to index position 0.

            Set<Path> filteredSet = new HashSet<>(); // Results of phase 1 of the filtering process are stored here.

            // Used to calc a valid deviation in the amount of 1's a 1024 bit hash is allowed to have.
            int examplePictureHash1024bAmountOfOnes = examplePic.getHash1024bAmountOfOnes();
            int minAmountOfOnes = Math.max(examplePictureHash1024bAmountOfOnes - allowedDeviation, 0);
            int maxAmountOfOnes = Math.min(examplePictureHash1024bAmountOfOnes + allowedDeviation, 1023);

            // Adds all paths with a valid deviation to filteredSet.
            for (int i = minAmountOfOnes; i <= maxAmountOfOnes; i++) {
                if (mPicHashResults.get((short) i) != null) filteredSet.addAll(mPicHashResults.get((short) i));
            }

            // Checks all paths in filteredSet against current examplePic. If within specification, the path will be added to result.
            for (Path path : filteredSet) {
                PicData testPic = mTestPicFinishedHashing.get(path);
                if (testPic == null) continue;
                if (calcIfSameEnough(examplePic, testPic, allowedDeviation)) {
                    subResult.add(testPic);
                }
            }

            if (subResult.size() > 1) result.add(subResult); // Adds subResult to result List if subResult contains more than only examplePic.

        }

        return result;
    }


    /**
     * This method tests all the hashes of all PicData object against each other. Because there is no examplePic, specifying a deviation is not
     * allowed to prevent endless strings of pictures where the middle pictures bridge the difference between the outer pictures. In other words,
     * pictures must match exactly per 1024 bit hash.
     * <p>
     * For each iteration of the outer for-loop, Path objects with the same amount of ones in the 1024 bit hash are matched to a PicData object and
     * added to a new filterList object. Then, the while-loop tests if the size of filterList is greater than 1. This has 2 reasons. The initial
     * filterList can be of size one. Or the filterList has shrunk to the size of one. In both cases there are not enough objects to test against
     * each other (1024 bit hash wise). If the size of filterList is greater than 1, the first PicData object is tested against all other PicData
     * objects in filterList. Matches are registered index wise and removed before the next iteration of the while loop. All matched PicData objects
     * are also stored in a separate list, called matchedPicData. If the size of matchPositions is not 0, all index positions that created a match
     * are remove from filterList. Also, matchedPicData is added to result. Index position 0 is always removed after the if statement.
     *
     * @return
     */
    public List<List<PicData>> getSamePicResultsWithoutExample() throws ProcessingAbortedException {
        List<List<PicData>> result = new ArrayList<>();

        for (Set<Path> pathSet : mPicHashResults.values()) { // The content of each pathSet has the same amount of ones in the 1024b hash.
            List<PicData> filterList = new ArrayList<>(); // Creates an ArrayList for storing PicData objects that have the same amount of ones in the 1024b hash.

            // This loop matches PicData to the paths found in pathSet.
            for (Path path : pathSet) {
                filterList.add(mTestPicFinishedHashing.get(path));
            }

            if (filterList.contains(null)) System.out.println("!!!filterList contain null  : " + filterList.contains(null));

            while (filterList.size() > 1) {
                if (!Gui.isProcessingActivated()) throw new ProcessingAbortedException();
                List<Integer> matchPositions = new ArrayList<>(); // Creates an ArrayList that stores the index positions of PicData that matches PicData of index 0 of filterList.
                List<PicData> matchedPicData = new ArrayList<>(); // Creates an ArrayList for storing matching PicData.
                PicData pseudoExamplePic = filterList.get(0); // pseudoExamplePic is a temporary examplePic.
                matchedPicData.add(pseudoExamplePic); // pseudoExamplePic is added at index position 0. Matches, if found, are added later.


                // Tests each PicData object in filterList against pseudoExamplePic through the calcIfSameEnough method.
                for (int i = 1; i < filterList.size(); i++) {
                    PicData testPic = filterList.get(i);
                    if (calcIfSameEnough(pseudoExamplePic, testPic, 0)) {
                        matchPositions.add(i);
                        matchedPicData.add(testPic);
                    }
                }


                // When the size of matchPositions is not 0, matches are found and action is taken.
                if (matchPositions.size() != 0) {
                    result.add(matchedPicData);
                    filterList.removeIf(x -> matchedPicData.contains(x));
                }

                // Index position 0 (pseudoExamplePic is always removed from filterList. The new PicData object at index position 0 will be used as pseudoExamplePic in the next iteration.
                filterList.remove(pseudoExamplePic);
            }
        }

        return result;
    }


    /**
     * This methods checks the 1024 bit hash of examplePic against testPic. It takes in account the allowed deviation/mismatch between thee two. For
     * each 90 degree rotation a PicData object has a 1024 bit hash (which totals to 4 per PicData object). So this method iterates through all the
     * hashes in a nested for-loop to find the best match. The best match is tested against the allowedDeviation and when within spec, a true is
     * returned.
     *
     * @param examplePic
     * @param testPic
     * @param allowedDeviation
     * @return
     */
    private boolean calcIfSameEnough(PicData examplePic, PicData testPic, int allowedDeviation) {
        List<List<Byte>> examplePicHashes = examplePic.getHash1024b();
        //if (testPic == null) return false; // TODO Try to find out why testPic is sometimes null and sometimes not null on the same set of data on subsequent runs.
        List<List<Byte>> testPicHashes = testPic.getHash1024b();
        int bestCount = 0;
        for (List listExamplePic : examplePicHashes) {
            for (List listTestPic : testPicHashes) {
                int count = 0;
                for (int i = 0; i <= 1023; i++) {
                    if (listExamplePic.get(i) == listTestPic.get(i)) count++;
                }
                if (count > bestCount) bestCount = count;
            }
        }

        if (1024 - bestCount > allowedDeviation) return false;
        else return true;
    }
}
