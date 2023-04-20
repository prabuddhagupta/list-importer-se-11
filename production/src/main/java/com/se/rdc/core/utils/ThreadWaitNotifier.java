package com.se.rdc.core.utils;

public class ThreadWaitNotifier {
	private Object lockObject = new Object();
	private boolean resumeSignal = false;

	public void doWait() {
		synchronized (lockObject) {
			//Changing if block into while block
			while (!resumeSignal) {
				try {
					lockObject.wait();
				} catch (InterruptedException e) {
				}
			}

			resumeSignal = false;
		}
	}

	public void doNotify() {
		synchronized (lockObject) {
			resumeSignal = true;
			lockObject.notify();
		}
	}
}