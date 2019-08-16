/**
 * Class that reads in a single picture and processes it through multiple steps to get four 1024b hashes (one hash for each possible orientation).
 *
 * @author Robert van den Eijk
 */

package net.vandeneijk;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

class PicPreProcessor {

    // Variables related to the constructor.
    private PicData mPicData;
    private URL mUrl;
    private PicsProcessor mPicsProcessor;

    // Variables for temporary storage.
    private BufferedImage mBufferedImage;
    private int[][][] mPictureLayouts = new int[4][][];



    PicPreProcessor(PicData picData, PicsProcessor picsProcessor) {
        mPicData = picData;
        mUrl = mPicData.getUrl();
        mPicsProcessor = picsProcessor;
    }



    void run() {
        try {
            ImageIO.setUseCache(false); // Uses RAM memory for caching instead of disk. Should increase performance somewhat (according to internet).
            mBufferedImage = ImageIO.read(mUrl); // TODO Almost all of the delay on high core count CPU's comes from this statement. How to improve performance on modern multicore (roughly >8 threads) systems???
            if (mBufferedImage == null) return;
            if (mBufferedImage.getWidth() < 256 || mBufferedImage.getHeight() < 256) throw new SmallPictureException();
        } catch (IOException | SmallPictureException miscEx) {
            return;
        }



        // Collecting some metadata.
        mPicData.setPictureWidth(mBufferedImage.getWidth());
        mPicData.setPictureHeight(mBufferedImage.getHeight());
        try {
            mPicData.setPictureFileSize(Files.size(mPicData.getPath()));
        } catch (IOException ioEx) {
            // Ignore if thrown. Metadata file size of this picture just stays 0.
        }



        mPictureLayouts[0] = convertPicToGreyScale2DArray();
        mPictureLayouts[1] = rotatePicOrientation(mPictureLayouts[0]);
        mPictureLayouts[2] = rotatePicOrientation(mPictureLayouts[1]);
        mPictureLayouts[3] = rotatePicOrientation(mPictureLayouts[2]);

        calcCellValuesEntirePic();

        storeResults();
    }

    /**
     * Converts a BufferedImage to a 2D array in greyscale.
     * @return
     */
    private int[][] convertPicToGreyScale2DArray() {

        final byte[] pixels = ((DataBufferByte) mBufferedImage.getRaster().getDataBuffer()).getData();
        final int width = mBufferedImage.getWidth();
        final int height = mBufferedImage.getHeight();
        final boolean hasAlphaChannel = mBufferedImage.getAlphaRaster() != null;

        int[][] result = new int[width][height];

        int pixelLength = 3;
        int forLoopPixelPlus = 2;
        int valuePixelPlus = 0;

        if (hasAlphaChannel) {
            pixelLength = 4;
            forLoopPixelPlus = 3;
            valuePixelPlus = 1;
        }

        for (int pixel = 0, row = 0, col = 0; pixel + forLoopPixelPlus < pixels.length-1; pixel += pixelLength) {
            //int value0 = -16777216; // 255 alpha. Alpha channel determines transparency. Not used for this case.
            //int value0 = (((int) pixels[pixel] & 0xff) << 24); // Alpha channel determines transparency. Not used for this case.
            int value1 = ((int) pixels[pixel + valuePixelPlus] & 0xff); // blue
            int value2 = (((int) pixels[pixel + valuePixelPlus + 1] & 0xff) << 8); // green
            int value3 = (((int) pixels[pixel + valuePixelPlus + 2] & 0xff) << 16); // red
            int value = 0;
            if (value1 > value2) value = value1;
            else value = value2;
            if (value3 > value) value = value3;
            result[col][row] = value;
            col++;
            if (col == width) {
                col = 0;
                row++;
            }
        }

        return result;
    }

    /**
     * Rotates a 2D array by 90 degrees and returns the rotated 2D array.
     */
    private int[][] rotatePicOrientation(int[][] image) {
        int imageWidth = image.length;
        int imageHeight = image[0].length;
        int[][] result = new int[imageHeight][imageWidth];

        for (int i = 0; i < imageWidth; i++) {
            for (int j = 0; j < imageHeight; j++) {
                result[imageHeight - 1 - j][i] = image[i][j];
            }
        }

        return result;
    }

    /**
     * This method divides the entire picture in a raster of 32x32. It calculates the average luminosity of each raster cell and then uses the
     * calcHash1024b method to create a 1024 bit hash. The special trick here is that the raster of 32x32 is actually build from 4 times a raster
     * of 16x16. Each raster of 16x16 starts at a corner of the picture. This way, if the picture can't be exactly divided by 32 along each axis,
     * the skipped pixels will be in the middle forming a stripe or a cross. Reason for this is to force calculation of the same pixels when the
     * orientation of the picture is changed.
     */
    private void calcCellValuesEntirePic() {
        for (int[][] pictureLayout : mPictureLayouts) { // The outer for-loop is repeated for each of the 4 picture rotations.
            int pictureWidth = pictureLayout.length;
            int pictureHeight = pictureLayout[0].length;
            int cellSizeX = pictureWidth / 32;
            int cellSizeY = pictureHeight / 32;
            int jumpPixelsX;
            int jumpPixelsY;
            List<Integer> cellValues1024 = new ArrayList<>();

            for (int rasterPositionY = 0; rasterPositionY < 32; rasterPositionY++) {
                for (int rasterPositionX = 0; rasterPositionX < 32; rasterPositionX++) {
                    if (rasterPositionY > 15) jumpPixelsY = pictureHeight - (cellSizeY * 32); // jumpPixels are used to shift each raster of 16x16 to the corner as described in the JavaDo comment.
                    else jumpPixelsY = 0;
                    if (rasterPositionX > 15) jumpPixelsX = pictureWidth - (cellSizeX * 32);
                    else jumpPixelsX = 0;

                    long cell1024Value = 0;

                    for (int pixelY = 0; pixelY < cellSizeY; pixelY++) {
                        for (int pixelX = 0; pixelX < cellSizeX; pixelX++) {
                            cell1024Value += pictureLayout[(rasterPositionX * cellSizeX) + jumpPixelsX + pixelX][(rasterPositionY * cellSizeY) + jumpPixelsY + pixelY];
                        }
                    }

                    cell1024Value /= cellSizeX * cellSizeY;
                    cellValues1024.add((int) cell1024Value);
                }
            }

            mPicData.addHash1024b(calcHash1024b(cellValues1024));
        }
    }

    /**
     * Creates a 1024 bit hash of the average cell value of each of the 1024 cells. A binary 1 is noted if a cell has an average luminosity equal or
     * higher than the average luminosity of the entire picture.
     *
     * @param cellValues
     * @return
     */
    private List<Byte> calcHash1024b(List<Integer> cellValues) {
        long totalCellValues = 0;
        for (int value : cellValues) totalCellValues += value;
        int averageCellValue = (int) (totalCellValues / 1024);

        List<Byte> hash1024b = new ArrayList<>();
        for (Integer cellValue : cellValues) {
            if (cellValue >= averageCellValue) {
                hash1024b.add((byte) 1);
            }
            else hash1024b.add((byte) 0);
        }

        return hash1024b;
    }

    /**
     * Writes the results to an instance of PicsProcessor before this instance of PicPreProcessor is made eligible for garbage collection.
     */
    private void storeResults() {
        mPicsProcessor.addToPicHashOnesToPathResults(mPicData.getHash1024bAmountOfOnes(), mPicData.getPath());
        if (mPicData.isExamplePicture()) mPicsProcessor.addToExamplePicFinishedHashing(mPicData);
        else mPicsProcessor.addToSearchPicFinishedHashing(mPicData);
    }
}


