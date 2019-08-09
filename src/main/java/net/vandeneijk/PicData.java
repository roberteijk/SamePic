package net.vandeneijk;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PicData {

    private final Path mPath;
    private final URL mUrl;
    private final boolean mExamplePicture;
    private final String mUniqueID;
    private boolean mDoNotShowThumbnail;
    private boolean mMarkedForDeletion;
    private List<Long> mHash64b;
    private List<List<Byte>> mHash1024b = new ArrayList<>();

    public PicData(Path path, boolean examplePicture, boolean doNotShowThumbnail) throws MalformedURLException {
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

    public Path getPath() {
        return mPath;
    }

    public URL getUrl() {
        return mUrl;
    }

    public boolean isExamplePicture() {
        return mExamplePicture;
    }

    public boolean isDoNotShowThumbnail() {
        return mDoNotShowThumbnail;
    }

    public boolean isMarkedForDeletion() {
        return mMarkedForDeletion;
    }

    public void setMarkedForDeletion(boolean markedForDeletion) {
        mMarkedForDeletion = markedForDeletion;
    }

    public List<Long> getHash64b() {
        return new ArrayList<>(mHash64b);
    }

    public void setHash64b(Set<Long> hash64b) {
        mHash64b = new ArrayList<>(hash64b);
    }

    public List<List<Byte>> getHash1024b() {
        return new ArrayList<>(mHash1024b);
    }

    public short getHash1024bAmountOfOnes() {
        short count = 0;
        if (mHash1024b.get(0) == null) return 0;
        for (Byte value : mHash1024b.get(0)) {
            count += value;
        }
        return count;
    }

    public void addHash1024b(List<Byte> hashList) {
        mHash1024b.add(hashList);
    }
}
