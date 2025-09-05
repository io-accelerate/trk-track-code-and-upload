package io.accelerate.tracking.app.sourcecode;

import io.accelerate.tracking.app.util.NoOpThread;

public class NoOpSourceCodeThread extends NoOpThread {

    public NoOpSourceCodeThread() {
        super((tick, event) -> String.format("frame no. %2d, source code recording disabled (%s)", tick, event));
    }


}
