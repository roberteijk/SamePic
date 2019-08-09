package net.vandeneijk;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class PicProcessor {

    private static final DataStore S_DATA_STORE = DataStore.getInstance();

    private PicData mPicData;
    private URL mUrl;
    private BufferedImage mBufferedImage;
    private static final int[] S_PRIMES = {29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101};
    private int[][][] mPictureLayouts = new int[4][][];
    private Set<Long> found64bHashesEntirePic = new HashSet<>();

    private List<Integer> tempCountList = new ArrayList<>();

    public PicProcessor(PicData picData) {
        mPicData = picData;
        mUrl = mPicData.getUrl();
    }

    public void run() {
        try {
            mBufferedImage = ImageIO.read(mUrl); // TODO Almost all of the delay during processing comes from this statement. How to improve.
            if (mBufferedImage == null) return;
            if (mBufferedImage.getWidth() < 256 || mBufferedImage.getHeight() < 256) throw new SmallPictureException();
        } catch (IOException | SmallPictureException miscEx) {
            return;
        }

        mPictureLayouts[0] = convertPicToGreyScale2DArray();
        mPictureLayouts[1] = rotatePicOrientation(mPictureLayouts[0]);
        mPictureLayouts[2] = rotatePicOrientation(mPictureLayouts[1]);
        mPictureLayouts[3] = rotatePicOrientation(mPictureLayouts[2]);

        calcCellValuesEntirePic();

        int tempCountCheck = tempCountList.get(0);
        for (int i = 1; i < tempCountList.size(); i++) {
            if (tempCountCheck != tempCountList.get(i)) System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! + " + tempCountList);
        }

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
     * Rotates a 2D array by 90 degrees.
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
     * This method divides the entire picture in a raster of 32. It calculates the average luminosity of each cell and then uses the calcHash1024b and
     * calcHash methods to create a 1024 and 64 bit hash from the values of the 1024 cells. The special trick here is that the raster of 32x32 is
     * actually build from 4  times a raster of 16x16. Each raster of 16x16 starts at a corner of the picture. This way, if the picture can't be
     * exactly divided by 32 along each axis, the skipped pictures will be in the middle forming a stripe or a cross. Reason for this is to force
     * calculation of the same pixels when the orientation of the picture is changed.
     */
    private void calcCellValuesEntirePic() {
        for (int[][] pictureLayout : mPictureLayouts) {
            int pictureWidth = pictureLayout.length;
            int pictureHeight = pictureLayout[0].length;
            int cellSizeX = pictureWidth / 32;
            int cellSizeY = pictureHeight / 32;
            int jumpPixelsX;
            int jumpPixelsY;
            List<Integer> cellValues1024 = new ArrayList<>();
            List<Integer> cellValues64 = new ArrayList<>();

            for (int rasterPositionY = 0; rasterPositionY < 32; rasterPositionY++) {
                for (int rasterPositionX = 0; rasterPositionX < 32; rasterPositionX++) {
                    if (rasterPositionY > 15) jumpPixelsY = pictureHeight - (cellSizeY * 32);
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

            for (int rasterPositionY = 0; rasterPositionY < 8; rasterPositionY++) {
                for (int rasterPositionX = 0; rasterPositionX < 8; rasterPositionX++) {
                    long cell64Value = 0;

                    for (int cell1024Y = 0; cell1024Y < 4; cell1024Y++) {
                        for (int cell1024X = 0; cell1024X < 4; cell1024X++) {
                            cell64Value += cellValues1024.get((rasterPositionX * 4) + (rasterPositionY * 128) + cell1024X + (cell1024Y * 32));
                        }
                    }

                    cell64Value /= 16;
                    cellValues64.add((int) cell64Value);
                }
            }
            found64bHashesEntirePic.add(calcHash(cellValues64));
        }
    }

    private List<Byte> calcHash1024b(List<Integer> cellValues) {
        long totalCellValues = 0;
        for (int value : cellValues) totalCellValues += value;
        int averageCellValue = (int) (totalCellValues / 1024);

        int tempCount = 0;

        List<Byte> hash1024b = new ArrayList<>();
        for (Integer cellValue : cellValues) {
            if (cellValue >= averageCellValue) {
                hash1024b.add((byte) 1);
                tempCount++;
            }
            else hash1024b.add((byte) 0);
        }

        tempCountList.add(tempCount);
        return hash1024b;
    }




    private long calcHash(List<Integer> cellValues) { // TODO Wouldn't it be better with bitshift.
        long totalCellValues = 0;

        for (int value : cellValues) {
            totalCellValues += value;
        }

        int averageCellValue = (int) totalCellValues / 64;

        long result = 0;
        long valueToAdd = 1;

        for (int i = 0; i < 63; i++) {
            if(cellValues.get(i) >= averageCellValue) {
                result += valueToAdd;
            }

            if (i < 62) valueToAdd *= 2;
        }

        if(cellValues.get(63) >= averageCellValue) { // The goal is to force a long overflow on the result variable instead of adding a negative valueToAdd variable that is overflown.
            result += valueToAdd;
            result += valueToAdd;
        }

        return result;
    }

        private void storeResults() {
        mPicData.setHash64b(found64bHashesEntirePic);
        S_DATA_STORE.addAllToEntirePicHashResults(mPicData.getHash1024bAmountOfOnes(), mPicData.getPath());
        if (mPicData.isExamplePicture()) S_DATA_STORE.addToExamplePictureFinishedHashing(mPicData);
        else S_DATA_STORE.addToPictureFinishedProcessing(mPicData);
    }




























    /**
     * Temp method to test processing pics.
     */
    private void tempTestWritePictureToDisk() {
        try {
            int[][] regen = mPictureLayouts[0];

            BufferedImage img = new BufferedImage(
                    regen.length, regen[0].length, BufferedImage.TYPE_3BYTE_BGR);
            for(int x = 0; x < regen.length; x++){
                for(int y = 0; y<regen[x].length; y++){
                    img.setRGB(x, y, (int)Math.round(regen[x][y]));
                }
            }
            File imageFile = new File("D:\\UserFiles\\Robert\\OneDrive\\Coding_SymbLink\\Current\\SamePic\\src\\main\\resources\\copy.bmp");
            ImageIO.write(img, "bmp", imageFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}


