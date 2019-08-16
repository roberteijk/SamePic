/**
 * Class that handles the deletion of files.
 *
 * @author Robert van den Eijk
 */

package net.vandeneijk;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Delete implements Runnable {

    // Variables related to the constructor.
    private List<List<PicData>> mPicDataListList;



    Delete(List<List<PicData>> picDataListList) {
        mPicDataListList = picDataListList;
    }



    public void run() {
        long startMillis = System.currentTimeMillis();
        Gui.setDeletionActivated(true);
        List<PicData> mPicDataForDeletionList = new ArrayList<>();



        // Fills a separate List with PicData objects that represent the pictures that may be deleted.
        for (List<PicData> picDataList : mPicDataListList) {
            picDataList.removeIf(PicData::isDoNotShowThumbnail); // While iterating, it removes any artificial examples that it comes across. Those are only useful for building the GUI tree.
            picDataList.stream().filter(PicData::isMarkedForDeletion).forEach(mPicDataForDeletionList::add);
        }



        int i;

        for (i = 0; i < mPicDataForDeletionList.size(); i++) { // Iterates through the List with PicData items that represent pictures that may be deleted.
            Gui.updateStatusBar("Deleting file " + (i + 1) + " of " + mPicDataForDeletionList.size() + "   : " + mPicDataForDeletionList.get(i).getPath().toString());
            try {
                Files.deleteIfExists(mPicDataForDeletionList.get(i).getPath());
            } catch (IOException ioEx) {
                // Ignore for now. File or file system object can't be deleted.
            }
        }


        // Cleans out the mPicDataListList from objects that aren't valid because they represent a file that doesn't exist anymore.
        for (List<PicData> picDataList : mPicDataListList) {
            picDataList.removeIf(mPicDataForDeletionList::contains);
        }
        mPicDataListList.removeIf(x -> x.size() < 2); // Makes sure that luring (pseudo)examplePics are removed from mPicDataListList when there is/are no searchPic(s) to accompany it.




        Gui.setDeletionActivated(false);
        Gui.updateStatusBar("Finished deleting " + i + " items in " + ((System.currentTimeMillis() - startMillis) / 1000.0) + " seconds.");
    }
}
