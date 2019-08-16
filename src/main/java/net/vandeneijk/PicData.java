/**
 * Class that contains information for a single picture.
 *
 * @author Robert van den Eijk
 */

package net.vandeneijk;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PicData {

    // Variables related to the constructor.
    private final Path mPath;
    private final URL mUrl;
    private final boolean mExamplePicture;
    private boolean mDoNotShowThumbnail;
    private final String mUniqueID;

    // Variables set and modified by setters.
    private boolean mMarkedForDeletion;
    private int mPictureWidth;
    private int mPictureHeight;
    private long mPictureFileSize;
    private double mAccuracy;
    private List<List<Byte>> mHash1024b = new ArrayList<>();



    PicData(Path path, boolean examplePicture, boolean doNotShowThumbnail) throws MalformedURLException {
        mPath = path;
        mUrl = mPath.toUri().toURL();
        mExamplePicture = examplePicture;
        mDoNotShowThumbnail = doNotShowThumbnail;
        mUniqueID = UUID.randomUUID().toString();
    }



    @Override
    public String toString() {
        return mPath.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PicData)) return false;
        PicData otherPicData = (PicData) obj;
        return mUniqueID.equals(otherPicData.mUniqueID);
    }

    @Override
    public int hashCode() {
        return mUniqueID.hashCode();
    }

    Path getPath() {
        return mPath;
    }

    URL getUrl() {
        return mUrl;
    }

    boolean isExamplePicture() {
        return mExamplePicture;
    }

    boolean isDoNotShowThumbnail() {
        return mDoNotShowThumbnail;
    }

    boolean isMarkedForDeletion() {
        return mMarkedForDeletion;
    }

    void setMarkedForDeletion(boolean markedForDeletion) {
        mMarkedForDeletion = markedForDeletion;
    }

    int getPictureWidth() {
        return mPictureWidth;
    }

    void setPictureWidth(int pictureWidth) {
        mPictureWidth = pictureWidth;
    }

    int getPictureHeight() {
        return mPictureHeight;
    }

    void setPictureHeight(int pictureHeight) {
        mPictureHeight = pictureHeight;
    }

    long getPictureFileSize() {
        return mPictureFileSize;
    }

    void setPictureFileSize(long pictureFileSize) {
        mPictureFileSize = pictureFileSize;
    }

    double getAccuracy() {
        return mAccuracy;
    }

    void setAccuracy(double accuracy) {
        mAccuracy = accuracy;
    }

    List<List<Byte>> getHash1024b() {
        return new ArrayList<>(mHash1024b);
    }

    /**
     * Returns the number of binary 1's in 1024 bit hash.
     *
     * @return
     */
    short getHash1024bAmountOfOnes() {
        short count = 0;
        if (mHash1024b.get(0) == null) return 0;
        for (Byte value : mHash1024b.get(0)) {
            count += value;
        }
        return count;
    }

    void addHash1024b(List<Byte> hashList) {
        mHash1024b.add(hashList);
    }
}