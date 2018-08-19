package org.zimmob.zimlx.activity.homeparts;

import android.graphics.Point;

import org.zimmob.zimlx.activity.HomeActivity;
import org.zimmob.zimlx.manager.Setup;
import org.zimmob.zimlx.model.Item;
import org.zimmob.zimlx.util.IDialogListener;
import org.zimmob.zimlx.widget.Desktop;
import org.zimmob.zimlx.widget.Dock;

public class HpAppEditApplier implements IDialogListener.OnEditDialogListener {
    private HomeActivity launcher;
    private Item _item;

    public HpAppEditApplier(HomeActivity home) {
        launcher = home;
    }

    public void onEditItem(final Item item) {
        _item = item;
        Setup.eventHandler().showEditDialog(launcher, item, this);
    }

    @Override
    public void onRename(String name) {
        _item.setLabel(name);
        Setup.dataManager().saveItem(_item);
        Point point = new Point(_item.getX(), _item.getY());

        switch (_item._locationInLauncher) {
            case Item.LOCATION_DESKTOP: {
                Desktop desktop = launcher.getDesktop();
                desktop.removeItem(desktop.getCurrentPage().coordinateToChildView(point), false);
                desktop.addItemToCell(_item, _item.getX(), _item.getY());
                break;
            }
            case Item.LOCATION_DOCK: {
                Dock dock = launcher.getDock();
                launcher.getDock().removeItem(dock.coordinateToChildView(point), false);
                dock.addItemToCell(_item, _item.getX(), _item.getY());
                break;
            }
        }
    }
}
