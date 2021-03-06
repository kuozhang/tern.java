/**
 *  Copyright (c) 2013-2014 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package tern.server;

import java.util.ArrayList;
import java.util.List;

import tern.ITernFileSynchronizer;
import tern.ITernProject;
import tern.server.protocol.completions.ITernCompletionCollector;
import tern.utils.StringUtils;

public abstract class AbstractTernServer implements ITernServer {

	private final ITernProject project;

	private final List<ITernServerListener> listeners;

	private boolean dataAsJsonString;
	private boolean dispose;
	private boolean loadingLocalPlugins;

	public AbstractTernServer(ITernProject project) {
		this.project = project;
		this.listeners = new ArrayList<ITernServerListener>();
		final ITernFileSynchronizer fileSynchronizer = getFileSynchronizer();
		if (fileSynchronizer != null) {
			this.addServerListener(new TernServerAdapter() {
				@Override
				public void onStop(ITernServer server) {
					fileSynchronizer.cleanIndexedFiles();
				}
			});
		}
	}

	public boolean isDataAsJsonString() {
		return dataAsJsonString;
	}

	public void setDataAsJsonString(boolean dataAsJsonString) {
		this.dataAsJsonString = dataAsJsonString;
	}

	@Override
	public void addServerListener(ITernServerListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeServerListener(ITernServerListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	protected void fireStartServer() {
		synchronized (listeners) {
			for (ITernServerListener listener : listeners) {
				listener.onStart(this);
			}
		}
	}

	protected void fireEndServer() {
		synchronized (listeners) {
			for (ITernServerListener listener : listeners) {
				listener.onStop(this);
			}
		}
	}

	@Override
	public final void dispose() {
		if (!isDisposed()) {
			this.dispose = true;
			doDispose();
			fireEndServer();
		}
	}

	@Override
	public boolean isDisposed() {
		return dispose;
	}

	protected abstract void doDispose();

	protected void addProposal(Object completion, int start, int end,
			ITernCompletionCollector collector) {
		String name = getText(completion, "name");
		String displayName = getText(completion, "displayName");
		String type = getText(completion, "type");
		String doc = getText(completion, "doc");
		String url = getText(completion, "url");
		String origin = getText(completion, "origin");
		collector.addProposal(name, displayName, type, doc, url, origin, start,
				end, completion, this);
	}

	public abstract String getText(Object value);

	public String getText(Object value, String name) {
		return getText(getValue(value, name));
	}

	public abstract Object getValue(Object value, String name);

	@Override
	public ITernFileSynchronizer getFileSynchronizer() {
		if (project != null) {
			return project.getFileSynchronizer();
		}
		return null;
	}

	public ITernProject getProject() {
		return project;
	}

	@Override
	public void addFile(String name, String text) {
		addFile(name, text, null);
	}

	@Override
	public void setLoadingLocalPlugins(boolean loadingLocalPlugins) {
		this.loadingLocalPlugins = loadingLocalPlugins;
	}

	@Override
	public boolean isLoadingLocalPlugins() {
		return loadingLocalPlugins;
	}
}
