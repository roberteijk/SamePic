package net.vandeneijk;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PicProcessor implements Runnable{

    private static final DataStore S_DATA_STORE = DataStore.getInstance();

    private Picture mPicture;
    private URL mUrl;
    private BufferedImage mImage;
    private static final int[] S_PRIMES = {29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101};
    private int[][][] mPictureLayouts = new int[4][][];
    private Set<Long> foundHashesEntirePic = new HashSet<>();

    public PicProcessor(Picture picture) {
        mPicture = picture;
        mUrl = mPicture.getUrl();
    }

    public void run() {
        try {
            mImage = ImageIO.read(mUrl);
        } catch (IOException ioEx) {
            ioEx.printStackTrace(); // TODO Better exception handling.
        }

        mPicture.setPictureLayout1(convertPicToGreyScale2DArray());
        mPicture.setPictureLayout2(rotatePicOrientation(mPicture.getPictureLayout1()));
        mPicture.setPictureLayout3(rotatePicOrientation(mPicture.getPictureLayout2()));
        mPicture.setPictureLayout4(rotatePicOrientation(mPicture.getPictureLayout3()));

        calcCellValuesEntirePic();

        storeResults();
    }


    /**
     * Converts a BufferedImage to a 2D array in greyscale.
     * @return
     */
    private int[][] convertPicToGreyScale2DArray() {

        final byte[] pixels = ((DataBufferByte) mImage.getRaster().getDataBuffer()).getData();
        final int width = mImage.getWidth();
        final int height = mImage.getHeight();
        final boolean hasAlphaChannel = mImage.getAlphaRaster() != null;

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
     * This method divides the entire picture in a raster of 8x8. It calculates the average luminosity of each cell and then uses the calcHash
     * method to create a 64 bit hash from the values of the 64 cells. The special trick here is that the raster of 8x8 is actually build from 4
     * times a raster of 4x4. Each raster of 4x4 starts at a corner of the picture. This way, if the picture can't be exactly divided by 8 along each
     * axis, the skipped pictures will be in the middle forming a stripe or a cross. Reason for this is to force calculation of the same pixels when
     * the orientation of the picture is changed.
     */
    private void calcCellValuesEntirePic() {
        mPictureLayouts[0] = mPicture.getPictureLayout1();
        mPictureLayouts[1] = mPicture.getPictureLayout2();
        mPictureLayouts[2] = mPicture.getPictureLayout3();
        mPictureLayouts[3] = mPicture.getPictureLayout4();

        for (int[][] pictureLayout : mPictureLayouts) {
            int pictureWidth = pictureLayout.length;
            int pictureHeight = pictureLayout[0].length;
            int cellSizeX = pictureWidth / 8;
            int cellSizeY = pictureHeight / 8;
            int jumpPixelsX;
            int jumpPixelsY;
            List<Integer> cellValues = new ArrayList<>();



            for (int rasterPositionX = 0; rasterPositionX < 8; rasterPositionX++) {
                for (int rasterPositionY = 0; rasterPositionY < 8; rasterPositionY++) {
                    if (rasterPositionX > 3) jumpPixelsX = pictureWidth - (cellSizeX * 8);
                    else jumpPixelsX = 0;
                    if (rasterPositionY > 3) jumpPixelsY = pictureHeight - (cellSizeY * 8);
                    else jumpPixelsY = 0;

                    long cellValue = 0;

                    for (int pixelX = 0; pixelX < cellSizeX; pixelX++) {
                        for (int pixelY = 0; pixelY < cellSizeY; pixelY++) {
                            cellValue += pictureLayout[(rasterPositionX * cellSizeX) + jumpPixelsX + pixelX][(rasterPositionY * cellSizeY) + jumpPixelsY + pixelY];
                        }
                    }

                    cellValue /= cellSizeX * cellSizeY;
                    cellValues.add((int) cellValue);
                }
            }

            foundHashesEntirePic.add(calcHash(cellValues));
        }
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
                System.out.print(1 + " ");
            } else System.out.print("  ");
            if ((i + 1) % 8 == 0) System.out.println();

            if (i < 62) valueToAdd *= 2;
        }

        if(cellValues.get(63) >= averageCellValue) { // The goal is to force a long overflow on the result variable instead of adding a negative valueToAdd variable that is overflown.
            result += valueToAdd;
            result += valueToAdd;
            System.out.print(1 + " ");
        } else System.out.print("  ");

        System.out.println();
        System.out.println();
        System.out.println();
        return result;
    }

    private void storeResults() {
        S_DATA_STORE.addAllToEntirePicHashResults(foundHashesEntirePic, mPicture.getPath());
        S_DATA_STORE.addToPictureFinishedProcessing(mPicture);
    }




























    /**
     * Temp method to test processing pics.
     */
    private void tempTestWritePictureToDisk() {
        try {
            int[][] regen = mPicture.getPictureLayout1();

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


