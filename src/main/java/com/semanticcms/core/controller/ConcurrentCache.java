/*
 * semanticcms-core-controller - Serves SemanticCMS content from a Servlet environment.
 * Copyright (C) 2016, 2017, 2018  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-core-controller.
 *
 * semanticcms-core-controller is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-core-controller is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-core-controller.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.controller;

import static com.semanticcms.core.controller.Cache.VERIFY_CACHE_PARENT_CHILD_RELATIONSHIPS;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.ServletException;

/**
 * A page cache that is thread safe through concurrent collections.
 *
 * Is currently still synchronized on parent-child verifications, which only occurs on put.
 */
class ConcurrentCache extends MapCache {

	private final ConcurrentMap<String, Object> concurrentAttributes;

	ConcurrentCache(SemanticCMS semanticCMS) {
		super(
			semanticCMS,
			new ConcurrentHashMap<CaptureKey, CaptureResult>(),
			VERIFY_CACHE_PARENT_CHILD_RELATIONSHIPS ? new HashMap<PageRef,Set<PageRef>>() : null,
			VERIFY_CACHE_PARENT_CHILD_RELATIONSHIPS ? new HashMap<PageRef,Set<PageRef>>() : null,
			new ConcurrentHashMap<String, Object>()
		);
		concurrentAttributes = (ConcurrentMap<String, Object>)attributes;
	}

	/**
	 * Overridden to add synchronization.
	 */
	@Override
	synchronized protected void verifyAdded(Page page) throws ServletException {
		super.verifyAdded(page);
	}

	@Override
	public <K,V> ConcurrentMap<K,V> newMap() {
		return new ConcurrentHashMap<K,V>();
	}

	@Override
	public <K,V> ConcurrentMap<K,V> newMap(int size) {
		return new ConcurrentHashMap<K,V>(size);
	}

	@Override
	public <V,E extends Exception> V getAttribute(
		String key,
		Class<V> clazz,
		Callable<? extends V,E> callable
	) throws E {
		V attribute = getAttribute(key, clazz);
		if(attribute == null) {
			attribute = callable.call();
			Object existing = concurrentAttributes.putIfAbsent(key, attribute);
			if(existing != null) attribute = clazz.cast(existing);
		}
		return attribute;
	}
}
