package com.starskyxiii.collapsible_groups.viewer;

import com.starskyxiii.collapsible_groups.Constants;
import com.starskyxiii.collapsible_groups.group.GroupDefinition;
import com.starskyxiii.collapsible_groups.group.GroupRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The nesting of active groups, from each group's {@code extra.parent}. A parent that is missing,
 * inactive, the group itself, or part of a cycle is logged once and the group is treated as top level.
 */
public final class GroupTree {
	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	private final Map<String, GroupDefinition> active;
	private final Map<String, String> parents;
	private final Map<String, List<String>> children;

	private GroupTree(Map<String, GroupDefinition> active, Map<String, String> parents, Map<String, List<String>> children) {
		this.active = active;
		this.parents = parents;
		this.children = children;
	}

	public static GroupTree of(Collection<GroupDefinition> groups) {
		Map<String, GroupDefinition> active = new LinkedHashMap<>();
		for (GroupDefinition group : groups) {
			if (GroupRepository.isActive(group)) active.putIfAbsent(group.id(), group);
		}
		Map<String, String> declared = new LinkedHashMap<>();
		for (GroupDefinition group : active.values()) {
			String parent = group.parentId();
			if (parent == null) continue;
			if (parent.equals(group.id())) {
				warnOnce(group.id(), "names itself as its parent");
			} else if (!active.containsKey(parent)) {
				warnOnce(group.id(), "has a missing or disabled parent '" + parent + "'");
			} else {
				declared.put(group.id(), parent);
			}
		}
		Map<String, String> parents = new LinkedHashMap<>(declared);
		for (String id : declared.keySet()) {
			if (inCycle(id, declared)) {
				warnOnce(id, "is part of a parent cycle");
				parents.remove(id);
			}
		}
		Map<String, List<String>> children = new LinkedHashMap<>();
		parents.forEach((child, parent) -> children.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(child));
		children.replaceAll((parent, list) -> List.copyOf(list));
		return new GroupTree(Map.copyOf(active), Map.copyOf(parents), Map.copyOf(children));
	}

	private static boolean inCycle(String start, Map<String, String> declared) {
		Set<String> seen = new HashSet<>();
		String current = declared.get(start);
		while (current != null && seen.add(current)) {
			if (current.equals(start)) return true;
			current = declared.get(current);
		}
		return false;
	}

	private static void warnOnce(String groupId, String problem) {
		if (WARNED.add(groupId + "|" + problem)) {
			Constants.LOG.warn("Group '{}' {}; showing it as a top-level group.", groupId, problem);
		}
	}

	/** True when the group has a parent or holds nested groups. */
	public boolean nested(String groupId) {
		return parents.containsKey(groupId) || children.containsKey(groupId);
	}

	public String parent(String groupId) {
		return parents.get(groupId);
	}

	public String root(String groupId) {
		String current = groupId;
		String parent;
		while ((parent = parents.get(current)) != null) current = parent;
		return current;
	}

	public List<String> children(String groupId) {
		return children.getOrDefault(groupId, List.of());
	}

	public GroupDefinition group(String groupId) {
		return active.get(groupId);
	}
}
