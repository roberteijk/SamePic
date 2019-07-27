package net.vandeneijk;

import java.nio.file.Path;
import java.util.*;

public class DataStore {
    private static final DataStore S_INSTANCE = new DataStore();

    private Set<String> sAcceptedFileExtensions = new HashSet<>();

    private Map<Path, Picture> mWaitingLinePicturesForProcessing = new HashMap<>();
    private Map<Path, Picture> mPictureFinishedProcessing = new HashMap<>();
    private Map<Path, Long> mExamplePictureFinishedHashing = new HashMap<>();
    private Map<Long, Set<Path>> mEntirePicHashResults = new HashMap<>();

    private DataStore(){
        sAcceptedFileExtensions.add("jpg");

    }

    public static synchronized DataStore getInstance() {
        return S_INSTANCE;
    }

    public synchronized Set<String> getAcceptedFileExtensions() {
        return new HashSet<>(sAcceptedFileExtensions);
    }

    public synchronized void addToWaitingLinePicturesToArray(Picture picture) {
        mWaitingLinePicturesForProcessing.put(picture.getPath(), picture);
    }

    public synchronized Picture getFromWaitingLinePicturesForProcessing() {
        Picture picture = null;

        for (Path path : mWaitingLinePicturesForProcessing.keySet()) {
            picture = mWaitingLinePicturesForProcessing.get(path);
            mWaitingLinePicturesForProcessing.remove(path);
            break;
        }

        return picture;
    }

    public synchronized void addToPictureFinishedProcessing(Picture picture) {
        mPictureFinishedProcessing.put(picture.getPath(), picture);
    }

    public synchronized void addAllToEntirePicHashResults(Set<Long> foundHashes, Path path) {
        for (long hash : foundHashes) {
            mEntirePicHashResults.computeIfAbsent(hash, k -> new HashSet<>());
            mEntirePicHashResults.get(hash).add(path);
        }

        int count = 0;
        for (long hash : mEntirePicHashResults.keySet()) {
            Set<Path> paths = mEntirePicHashResults.get(hash);
            if (paths.contains(path)) count++;
        }

        System.out.println();
        System.out.println("From DataStore (EntirePic) : " + count);
        System.out.println("From DataStore Total (EntirePic): " + mEntirePicHashResults.size());
        System.out.println();
    }
}
