package com.fastchar.accepter;

import com.fastchar.core.*;
import com.fastchar.interfaces.IFastScannerAccepter;

import java.io.File;

@SuppressWarnings("unchecked")
public class FastActionScannerAccepter implements IFastScannerAccepter {
    @Override
    public boolean onScannerClass(FastEngine engine, Class<?> scannedClass) throws Exception {
        if (FastAction.class.isAssignableFrom(scannedClass)) {
            engine.getActions().add((Class<? extends FastAction>) scannedClass);
        }
        return true;
    }

    @Override
    public boolean onScannerFile(FastEngine engine, File file) throws Exception {
        return false;
    }
}
