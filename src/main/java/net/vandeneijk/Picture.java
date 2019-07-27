package net.vandeneijk;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public class Picture {

    private final Path mPath;
    private final URL mUrl;
    private final boolean mExamplePicture;
    private int[][] mPictureLayout1;
    private int[][] mPictureLayout2;
    private int[][] mPictureLayout3;
    private int[][] mPictureLayout4;

    public Picture(Path path, boolean examplePicture) throws MalformedURLException {
        mPath = path;
        mUrl = mPath.toUri().toURL();
        mExamplePicture = examplePicture;
    }

    public Path getPath() {
        return mPath;
    }

    public URL getUrl() {
        return mUrl;
    }

    private boolean isExamplePicture() {
        return mExamplePicture;
    }

    public int[][] getPictureLayout1() {
        return mPictureLayout1;
    }

    public void setPictureLayout1(int[][] pictureLayout1) {
        mPictureLayout1 = pictureLayout1;
    }

    public int[][] getPictureLayout2() {
        return mPictureLayout2;
    }

    public void setPictureLayout2(int[][] pictureLayout2) {
        mPictureLayout2 = pictureLayout2;
    }

    public int[][] getPictureLayout3() {
        return mPictureLayout3;
    }

    public void setPictureLayout3(int[][] pictureLayout3) {
        mPictureLayout3 = pictureLayout3;
    }

    public int[][] getPictureLayout4() {
        return mPictureLayout4;
    }

    public void setPictureLayout4(int[][] pictureLayout4) {
        mPictureLayout4 = pictureLayout4;
    }
}
