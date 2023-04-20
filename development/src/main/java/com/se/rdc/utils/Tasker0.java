package com.se.rdc.utils;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

//**
// This is the original backup file
// */

//TODO: Add thread factory, custom name, configurable deamon thread per thread, for task: minimum task execution count

public class Tasker0 {
	private ExecutorService executor;
	private Phaser phaser;
	private AtomicLong taskCount = new AtomicLong(0);
	private ConcurrentLinkedQueue<Task> failedTasks;

	public Tasker0() {
		init(0);
	}

	public Tasker0(int nThreads) {
		init(nThreads);
	}

	public Tasker0(ExecutorService executorService) throws Exception {
		nullCheck(executorService, "ExecutorService");
		init(executorService);
	}

	private void init(int nThreads) {
		if (nThreads < 1) {
			//Set to maximum optimal threads
			nThreads = Runtime.getRuntime().availableProcessors() + 1;
		}

		//We are using ScheduledThreadPool so that we could schedule tasks from within default executor if needed
		init(Executors.newScheduledThreadPool(nThreads));
	}

	private void init(ExecutorService executorService) {
		phaser = new Phaser();
		//register main thread
		phaser.register();
		executor = executorService;
		failedTasks = new ConcurrentLinkedQueue<>();
	}

	public ExecutorService getExecutorService() {
		return executor;
	}

	private void registerTask() {
		if (taskCount.get() <= 0) {
			synchronized (this) {
				phaser.register();
			}
		}
		taskCount.incrementAndGet();
	}

	private void deRegisterTask() {
		taskCount.decrementAndGet();

		if (taskCount.get() <= 0) {
			synchronized (this) {
				while (phaser.getRegisteredParties() > 1) {
					phaser.arriveAndDeregister();
				}
			}
		}
	}

	public Future execute(Callable task) throws Exception {
		return execute(task, 0, null, null);
	}

	public Future execute(Callable task, int retryCount) throws Exception {
		return execute(task, retryCount, null, null);
	}

	public Future execute(Callable task, int retryCount,
			TaskStatusListener listener, Map<TASK_ATTR, Object> taskAttributes)
			throws Exception {
		nullCheck(task, "Callable/Task");

		return executor.submit(
				(Callable) new Task(task, retryCount, listener, taskAttributes));
	}

	/**
	 * Use: "schedule" method from ScheduledExecutorService
	 */
	public Future executeAfterInitialDelay(Callable task, long delay,
			TimeUnit unit, int retryCount, TaskStatusListener listener,
			Map<TASK_ATTR, Object> taskAttributes) throws Exception {

		validateScheduleExecutorType();
		nullCheck(task, "Callable/Task");

		return ((ScheduledExecutorService) executor).schedule(
				(Callable) new Task(task, retryCount, listener, taskAttributes), delay,
				unit);
	}

	/**
	 * This task will not be awaited.
	 * Use: "scheduleWithFixedDelay" method from ScheduledExecutorService
	 * All the failed scheduled tasks will become normal "non-scheduled" task when retrying
	 */
	public Future executeWithPeriodicDelay(Runnable task, long initialDelay,
			long delay, TimeUnit unit, int retryCount, TaskStatusListener listener,
			Map<TASK_ATTR, Object> taskAttributes) throws Exception {

		validateScheduleExecutorType();
		nullCheck(task, "Runnable/Task");
		if (delay < 1) {
			delay = 1;
		}

		return ((ScheduledExecutorService) executor).scheduleWithFixedDelay(
				new Task(task, retryCount, listener,
						taskAttributes).deactivateRegistration(), initialDelay, delay,
				unit);
	}

	/**
	 * This task will not be awaited.
	 * Use: "scheduleAtFixedRate" method from ScheduledExecutorService
	 * All the failed scheduled tasks will become normal "non-scheduled" task when retrying
	 * <p>
	 * Creates and executes a periodic task after the given initial delay,
	 * and subsequently with the given period; that is executions will commence after
	 * {@code initialDelay} then {@code initialDelay+period}, then
	 * {@code initialDelay + 2 * period}, and so on.
	 * <p>
	 * If any execution of this task takes longer than its period, then subsequent executions
	 * may start late, but will not concurrently execute.
	 */
	public Future executeAtFixedRate(Runnable task, long initialDelay, long delay,
			TimeUnit unit, int retryCount, TaskStatusListener listener,
			Map<TASK_ATTR, Object> taskAttributes) throws Exception {

		validateScheduleExecutorType();
		nullCheck(task, "Runnable/Task");
		if (delay < 1) {
			delay = 1;
		}

		return ((ScheduledExecutorService) executor).scheduleAtFixedRate(
				new Task(task, retryCount, listener,
						taskAttributes).deactivateRegistration(), initialDelay, delay,
				unit);
	}

	private void validateScheduleExecutorType() throws Exception {
		if (!(executor instanceof ScheduledExecutorService)) {
			throw new Exception(
					"Please use a ScheduledExecutorService for the thread executor.");
		}
	}

	private void nullCheck(Object object, String msgToken) throws Exception {
		if (object == null) {
			throw new Exception(msgToken + " must not be null.");
		}
	}

	public void runFailedTasks() {
		await();

		while (failedTasks.iterator().hasNext()) {
			for (Task task : failedTasks) {
				if (task.retryCount > 0) {
					task.retryCount--;
					if (!task.registrationDeactivated) {
						registerTask();
					}

					if (task.callableTask != null) {
						executor.submit((Callable) task);
					} else {
						executor.submit((Runnable) task);
					}

				} else {
					failedTasks.remove(task);
				}
			}
			await();
		}
	}

	/**
	 * Wait for all submitted tasks to be finished
	 */
	private void await() {
		phaser.arriveAndAwaitAdvance();
	}

  private void closeExecutor(boolean isShutDownNow) {
    if (isShutDownNow) {
    	if(executor instanceof ThreadPoolExecutor){
				((ThreadPoolExecutor)executor).setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
			}
      executor.shutdownNow();
    } else {
      executor.shutdown();
    }
  }

	/**
	 * It must be the very last operation of a Tasker
	 */
	public void shutdown() {
		closeExecutor(false);
		await();
		clearNTerminate();
	}

	public void forceShutdown(){
		closeExecutor(true);
		clearNTerminate();
	}

	private void clearNTerminate(){
		failedTasks.clear();
		phaser.forceTermination();
	}

	public enum TASK_ATTR {
		TASK_NAME
	}

	public enum TASK_STATUS {
		DONE, ERROR
	}

	public interface TaskStatusListener {
		void onFinished(TASK_STATUS status, Object output);
	}

	private class Task implements Callable, Runnable {
		private boolean isAFailedTask = false;
		private boolean registrationDeactivated = false;//TODO: implement minimum task execution count instead; necessary for periodic/rated scheduled tasks
		private int retryCount;
		private Callable callableTask;
		private Runnable runnableTask;
		private TaskStatusListener statusListener;
		private Map<TASK_ATTR, Object> taskAttributes; /*this map is not still in use, introduced it for future scalability*/

		Task(Callable task, int retryCount, TaskStatusListener listener,
				Map<TASK_ATTR, Object> taskAttributes) {
			callableTask = task;
			commonInit(retryCount, listener, taskAttributes);
		}

		public Task(Runnable task, int retryCount, TaskStatusListener listener,
				Map<TASK_ATTR, Object> taskAttributes) {
			runnableTask = task;
			commonInit(retryCount, listener, taskAttributes);
		}

		private void commonInit(int retryCount, TaskStatusListener listener,
				Map<TASK_ATTR, Object> taskAttributes) {
			this.retryCount = retryCount;
			this.statusListener = listener;
			this.taskAttributes = taskAttributes;

			//register the task
			registerTask();
		}

		/**
		 * Add task into failed tasks if the "task.call()" returns TASK_STATUS.ERROR or throws exception
		 */
		@Override public Object call() {
			Object retVal;
			TASK_STATUS status = TASK_STATUS.DONE;

			try {
				retVal = callableTask.call();

				//for pre-defined "error status" return value do retry
				if (retVal != null && retVal.equals(TASK_STATUS.ERROR)) {
					status = (TASK_STATUS) retVal;
				} else {
					retryCount = 0;
				}

			} catch (Exception e) {
				e.printStackTrace();
				retVal = e;
				status = TASK_STATUS.ERROR;
			}

			return commonTask(status, retVal);
		}

		@Override public void run() {
			TASK_STATUS status = TASK_STATUS.DONE;

			try {
				runnableTask.run();
				retryCount = 0;

			} catch (Exception e) {
				e.printStackTrace();
				status = TASK_STATUS.ERROR;
			}

			commonTask(status, null);
		}

		private Object commonTask(TASK_STATUS status, Object retVal) {
			if (retryCount > 0 && !isAFailedTask && status.equals(
					TASK_STATUS.ERROR)) {
				//execute this block once
				failedTasks.add(this);
				isAFailedTask = true;
			}

			if (statusListener != null) {
				try {
					statusListener.onFinished(status, retVal);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (!registrationDeactivated) {
				deRegisterTask();
			}

			return retVal;
		}

		private Task deactivateRegistration() {
			if (!registrationDeactivated) {
				deRegisterTask();
				registrationDeactivated = true;
			}

			return this;
		}
	}
}