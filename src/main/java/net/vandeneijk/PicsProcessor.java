/**
 * Class that is a store for the results from PicPreProcessor instances. This class uses these result to compare pictures against each
 * other on request.
 *
 * @author Robert van den Eijk
 */

package net.vandeneijk;

import java.nio.file.Path;
import java.util.*;

class PicsProcessor {

    // Variables related to processing PicData.
    private Map<Path, PicData> mExamplePicFinishedHashing = new HashMap<>();
    private Map<Path, PicData> mSearchPicFinishedHashing = new HashMap<>();
    private Map<Short, Set<Path>> mPicHashOnesToPathResults = new HashMap<>();



    void addToSearchPicFinishedHashing(PicData picData) {
        mSearchPicFinishedHashing.put(picData.getPath(), picData);
    }

    void addToExamplePicFinishedHashing(PicData picData) {
        mExamplePicFinishedHashing.put(picData.getPath(), picData);
    }

    void addToPicHashOnesToPathResults(short hash1024bAmountOfOnes, Path path) {
        mPicHashOnesToPathResults.computeIfAbsent(hash1024bAmountOfOnes, x -> new HashSet<>());
        mPicHashOnesToPathResults.get(hash1024bAmountOfOnes).add(path);
    }

    /**
     * This method checks pictures from mSearchPicFinishedHashing against mExamplePicFinishedHashing. It does this by comparing each of the 4 1024 bit
     * hashes. A certain deviation between the hashes may be allowed depending allowedDeviation.
     *
     * @param allowedDeviation
     * @return
     */
    List<List<PicData>> getSamePicResultsFromExample(int allowedDeviation) throws ProcessingAbortedException { // TODO Make multithreaded.
        List<List<PicData>> result = new ArrayList<>();
        int count = 0;
        int amountExample = mExamplePicFinishedHashing.size();
        int amountSearch = mSearchPicFinishedHashing.size();

        for (PicData examplePic : mExamplePicFinishedHashing.values()) { // Loops through all the example pictures.
            Gui.updateStatusBar("Processing results for example picture " + ++count + " of " + amountExample + " against " + amountSearch + " pictures in the search directory.");
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
                if (mPicHashOnesToPathResults.get((short) i) != null) filteredSet.addAll(mPicHashOnesToPathResults.get((short) i));
            }

            // Checks all paths in filteredSet against current examplePic. If within specification, the path will be added to subResult.
            for (Path path : filteredSet) {
                PicData testPic = mSearchPicFinishedHashing.get(path);
                if (testPic == null) continue;
                if (calcIfSameEnough(examplePic, testPic, allowedDeviation)) {
                    subResult.add(testPic);
                }
            }

            if (subResult.size() > 1) {
                // Temporary removes examplePic from the subResult for easy sorting without examplePic and then adds it to index 0 again afterward.
                subResult.remove(0);
                subResult.sort(Comparator.comparing(o -> o.getPath().toString()));
                subResult.add(0, examplePic);

                result.add(subResult); // Adds subResult to result List if subResult contains something else besides examplePic.
            }
        }

        result.sort(Comparator.comparing(o -> o.get(0).getPath().toString())); // Sorts each List of result by the path of index 0 of each List.

        return result;
    }

    /**
     * This method searches for exact matches in mSearchPicFinishedHashing. No deviation in the 4 1024 bit hashes is allowed.
     *
     * @return
     */
    List<List<PicData>> getSamePicResultsWithoutExample() throws ProcessingAbortedException { // TODO Make multithreaded.
        List<List<PicData>> result = new ArrayList<>();
        int count = 1;
        int amount = mSearchPicFinishedHashing.size();

        for (Set<Path> pathSet : mPicHashOnesToPathResults.values()) { // The content of each pathSet has the same amount of ones in the 1024 bit hash.
            Gui.updateStatusBar("Processing results for picture " + count + " of " + amount + ".");
            List<PicData> filterList = new ArrayList<>(); // Creates an ArrayList for storing PicData objects that have the same amount of ones in the 1024 bit hash.

            // This for-loop matches PicData to the paths found in pathSet. Because of add, null will be added if no match is found. After the for-loop, all null entries will be removed.
            for (Path path : pathSet) {
                filterList.add(mSearchPicFinishedHashing.get(path));
            }
            filterList.removeIf(Objects::isNull);

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
                    count += matchPositions.size();
                    matchedPicData.sort(Comparator.comparing(o -> o.getPath().toString()));
                    result.add(matchedPicData);
                    filterList.removeIf(matchedPicData::contains);
                }

                // Index position 0 (pseudoExamplePic is always removed from filterList. The new PicData object at index position 0 will be used as pseudoExamplePic in the next iteration.
                filterList.remove(pseudoExamplePic);
                count++;
            }
        }

        result.sort(Comparator.comparing(o -> o.get(0).getPath().toString())); // Sorts each List of result by the path of index 0 of each List.

        return result;
    }

    /**
     * This methods checks the 1024 bit hashes of (pseudeo)examplePic against testPic. It takes in account the allowed deviation/mismatch between the
     * two. For each 90 degree rotation a PicData object has a 1024 bit hash (which totals to 4 per PicData object). So this method iterates through
     * all the hashes in a nested for-loop to find the best match. The best match is tested against the allowedDeviation and when within spec, a true
     * is returned.
     *
     * @param examplePic
     * @param testPic
     * @param allowedDeviation
     * @return
     */
    private boolean calcIfSameEnough(PicData examplePic, PicData testPic, int allowedDeviation) {
        List<List<Byte>> examplePicHashes = examplePic.getHash1024b();
        List<List<Byte>> testPicHashes = testPic.getHash1024b();
        int bestCount = 0;
        for (List listExamplePic : examplePicHashes) {
            INNER_ENHANCED_FOR_LOOP: for (List listTestPic : testPicHashes) {
                int count = 0;
                for (int i = 0; i <= 1023; i++) {
                    if (listExamplePic.get(i) == listTestPic.get(i)) count++; // Counts the amount of matched bits.
                    if (i - count > allowedDeviation) continue INNER_ENHANCED_FOR_LOOP; // Continues to the next testPic orientation if allowedDeviation is exceeded early. For efficiency.
                }
                if (count > bestCount) bestCount = count;
            }
        }

        if (1024 - bestCount > allowedDeviation) return false;
        else {
            examplePic.setAccuracy(100.0);
            testPic.setAccuracy((100.0 / 1024) * bestCount);
            return true;
        }
    }
}