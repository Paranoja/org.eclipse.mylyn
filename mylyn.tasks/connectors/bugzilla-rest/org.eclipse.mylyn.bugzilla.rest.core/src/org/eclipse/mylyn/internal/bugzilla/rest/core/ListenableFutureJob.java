/*******************************************************************************
 * Copyright (c) 2014 Sam Davis and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sam Davis - initial API and implementation
 *     See git history
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.rest.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

public abstract class ListenableFutureJob<V> extends Job /* implements ListenableFuture<V> */ {

	private class Listener {

		Runnable runnable;

		Executor executor;

		public Listener(Runnable runnable, Executor executor) {
			this.runnable = runnable;
			this.executor = executor;
		}

		public void run() {
			executor.execute(runnable);
		}
	}

	List<Listener> listeners = Collections.synchronizedList(new ArrayList<Listener>());

	private volatile boolean done;

	private V resultObject;

	public ListenableFutureJob(String name) {
		super(name);
		resultObject = null;
		addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				done = true;
				synchronized (listeners) {
					for (Listener listener : listeners) {
						listener.run();
					}
				}
			}
		});
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		return this.cancel();
	}

	public V get() throws InterruptedException, ExecutionException {
		this.join();
		return resultObject;
	}

	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		long start = System.currentTimeMillis();
		while (!done && System.currentTimeMillis() - start < unit.toMillis(timeout)) {
			Thread.sleep(250);
		}
		if (!done) {
			throw new TimeoutException(Messages.ListenableFutureJob_TimeoutWaitingForResult);
		}
		return resultObject;
	}

	protected void set(V future) {
		resultObject = future;
	}

	public boolean isCancelled() {
		return getResult().getSeverity() == IStatus.CANCEL;
	}

	public boolean isDone() {
		return done;
	}

	public void addListener(Runnable listener, Executor executor) {
		listeners.add(new Listener(listener, executor));
	}
}
