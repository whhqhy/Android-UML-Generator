package software.umlgenerator.data;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import software.umlgenerator.data.model.parcelables.ParcelableClass;
import software.umlgenerator.data.model.parcelables.ParcelableMethod;

/**
 * Created by mbpeele on 4/1/16.
 */
public class XposedServiceConnection implements ServiceConnection, IXposedServiceConnection {

    private Messenger messenger;
    private final List<Message> unsentMessages;

    private boolean isBound = false;

    public XposedServiceConnection() {
        unsentMessages = new CopyOnWriteArrayList<>();
    }

    @Override
    public void onServiceConnected(ComponentName component, IBinder binder) {
        messenger = new Messenger(binder);
        isBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName component) {
        messenger = null;
        isBound = false;
    }

    @Override
    public void sendBeforeClassMessage(Class clazz) {
        Message message = Message.obtain();
        message.what = CLASS_BEFORE_CALLED;
        message.setData(createMessageBundle(new ParcelableClass(clazz)));
        checkIfBound(message);
    }

    @Override
    public void sendAfterClassMessage(Class clazz) {
        Message message = Message.obtain();
        message.what = CLASS_AFTER_CALLED;
        message.setData(createMessageBundle(new ParcelableClass(clazz)));
        checkIfBound(message);
    }

    @Override
    public void sendBeforeMethodMessage(Method method) {
        Message message = Message.obtain();
        message.what = METHOD_BEFORE_CALLED;
        Bundle bundle = createMessageBundle(new ParcelableMethod(method));
        message.setData(bundle);
        checkIfBound(message);
    }

    @Override
    public void sendAfterMethodMessage(Method method) {
        Message message = Message.obtain();
        message.what = METHOD_AFTER_CALLED;
        Bundle bundle = createMessageBundle(new ParcelableMethod(method));
        message.setData(bundle);
        checkIfBound(message);
    }

    @Override
    public Bundle createMessageBundle(Parcelable parcelable) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_KEY, parcelable);
        return bundle;
    }

    @Override
    public void checkIfBound(Message message) {
        if (isBound) {
            if (unsentMessages.isEmpty()) {
                sendMessage(message);
            } else {
                synchronized (unsentMessages) {
                    for (Message message1: unsentMessages) {
                        sendMessage(message1);
                    }
                    sendMessage(message);
                    unsentMessages.clear();
                }
            }
        } else {
            unsentMessages.add(message);
        }
    }

    @Override
    public void sendMessage(Message message) {
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
