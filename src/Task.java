/*
 * Copyright Alex Chulkin (c) 2016
 */

class Task {
    static {
        Thread.setDefaultUncaughtExceptionHandler(new LocalUncaughtExceptionHandler());
    }

    private static class LocalUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e) {
            System.err.println(Helper.EXCEPTION + e.getMessage() + Helper.THROWN_IN_THREAD + t + "\n");
            e.printStackTrace();
        }
    }
}
