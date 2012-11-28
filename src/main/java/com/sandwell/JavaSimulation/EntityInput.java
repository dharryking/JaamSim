/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.sandwell.JavaSimulation;

import java.util.ArrayList;
import java.util.Collections;

public class EntityInput<T extends Entity> extends Input<T> {

	private Class<T> entClass;
	private ArrayList<T> invalidEntities;

	public EntityInput(Class<T> aClass, String key, String cat, T def) {
		super(key, cat, def);
		entClass = aClass;
		invalidEntities = null;
	}

	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCount(input, 0, 1);
		if (input.size() == 0) {
			value = null;
		}
		else {
			T tmp = Input.parseEntity(input.get(0), entClass);
			if (!isValid(tmp))
				throw new InputErrorException("%s is not a valid entity", tmp.getInputName());

			value = tmp;
		}

		this.updateEditingFlags();
	}

	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<String>();
		for (T each: Simulation.getClonesOf(entClass)) {
			if (each.testFlag(Entity.FLAG_GENERATED))
				continue;

			if (!isValid(each))
				continue;

			list.add(each.getInputName());
		}
		Collections.sort(list);
		return list;
	}

	public void setInvalidEntities(T... list) {
		if (list == null) {
			invalidEntities = null;
			return;
		}

		invalidEntities = new ArrayList<T>(list.length);
		for (T each: list)
			invalidEntities.add(each);
	}

	private boolean isValid(T ent) {
		if (invalidEntities == null)
			return true;

		return !invalidEntities.contains(ent);
	}
}