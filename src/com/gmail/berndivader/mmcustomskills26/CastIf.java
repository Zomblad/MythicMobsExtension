package com.gmail.berndivader.mmcustomskills26;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.regex.Pattern;

import com.gmail.berndivader.jboolexpr.BooleanExpression;
import com.gmail.berndivader.jboolexpr.MalformedBooleanException;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.AbstractEntity;
import io.lumine.xikage.mythicmobs.adapters.AbstractLocation;
import io.lumine.xikage.mythicmobs.io.MythicLineConfig;
import io.lumine.xikage.mythicmobs.skills.AbstractSkill;
import io.lumine.xikage.mythicmobs.skills.INoTargetSkill;
import io.lumine.xikage.mythicmobs.skills.ITargetedEntitySkill;
import io.lumine.xikage.mythicmobs.skills.ITargetedLocationSkill;
import io.lumine.xikage.mythicmobs.skills.Skill;
import io.lumine.xikage.mythicmobs.skills.SkillCondition;
import io.lumine.xikage.mythicmobs.skills.SkillManager;
import io.lumine.xikage.mythicmobs.skills.SkillMechanic;
import io.lumine.xikage.mythicmobs.skills.SkillMetadata;
import io.lumine.xikage.mythicmobs.skills.SkillString;
import io.lumine.xikage.mythicmobs.skills.SkillTargeter;
import io.lumine.xikage.mythicmobs.skills.conditions.InvalidCondition;
import io.lumine.xikage.mythicmobs.skills.targeters.IEntitySelector;
import io.lumine.xikage.mythicmobs.skills.targeters.ILocationSelector;

public class CastIf extends SkillMechanic implements INoTargetSkill, ITargetedEntitySkill, ITargetedLocationSkill {

	protected static MythicMobs mythicmobs;
	protected static SkillManager skillmanager;
	protected String meetAction, elseAction;
	protected Optional<String> 
		meetTargeter=Optional.empty(),
		elseTargeter=Optional.empty();
	protected String cConditionLine, tConditionLine;
	protected HashMap<Integer, String> 
		tConditionLines = new HashMap<>(),
		cConditionLines = new HashMap<>();
	protected HashMap<Integer, SkillCondition> 
		targetConditions = new HashMap<>(),
		casterConditions = new HashMap<>();
	protected Optional<Skill> 
		meetSkill = Optional.empty(),
		elseSkill = Optional.empty();

	public CastIf(String skill, MythicLineConfig mlc) {
		super(skill, mlc);
		this.ASYNC_SAFE=false;
		CastIf.mythicmobs = Main.getPlugin().getMythicMobs();
		CastIf.skillmanager = CastIf.mythicmobs.getSkillManager();
		String ms = mlc.getString(new String[] { "conditions", "c" });
		this.parseConditionLines(ms, false);
		ms = mlc.getString(new String[] { "targetconditions", "tc" });
		this.parseConditionLines(ms, true);
		this.meetAction = mlc.getString(new String[] { "meet" });
		this.elseAction = mlc.getString(new String[] { "else" });
		this.meetTargeter = Optional.ofNullable(mlc.getString("meettargeter"));
		this.elseTargeter = Optional.ofNullable(mlc.getString("elsetargeter"));
		if (this.meetAction != null) {
			this.meetSkill = CastIf.skillmanager.getSkill(this.meetAction);
		}
		if (this.elseAction != null) {
			this.elseSkill = CastIf.skillmanager.getSkill(this.elseAction);
		}
		if (this.cConditionLines != null && !this.cConditionLines.isEmpty()) {
			this.casterConditions = this.getConditions(this.cConditionLines);
		}
		if (this.tConditionLines != null && !this.tConditionLines.isEmpty()) {
			this.targetConditions = this.getConditions(this.tConditionLines);
		}
		if (this.meetTargeter.isPresent()) {
			String mt = this.meetTargeter.get();
			mt=mt.substring(1, mt.length()-1);
			mt=SkillString.parseMessageSpecialChars(mt);
			this.meetTargeter = Optional.of(mt);
		}
		if (this.elseTargeter.isPresent()) {
			String mt = this.elseTargeter.get();
			mt=mt.substring(1, mt.length()-1);
			mt=SkillString.parseMessageSpecialChars(mt);
			this.elseTargeter = Optional.of(mt);
		}
	}

	@Override
	public boolean cast(SkillMetadata data) {
		if (this.handleConditions(data)) {
			if (this.meetSkill.isPresent() && this.meetSkill.get().isUsable(data)) {
				if (this.meetTargeter.isPresent()) renewTargets(this.meetTargeter.get(),data);
				this.meetSkill.get().execute(data);
			}
		} else {
			if (this.elseSkill.isPresent() && this.elseSkill.get().isUsable(data)) {
				if (this.elseTargeter.isPresent()) renewTargets(this.elseTargeter.get(),data);
				this.elseSkill.get().execute(data);
			}
		}
		return true;
	}

	private static void renewTargets(String ts, SkillMetadata data) {
		Optional<SkillTargeter> maybeTargeter = Optional.of(AbstractSkill.parseSkillTargeter(ts));
		if (maybeTargeter.isPresent()) {
			SkillTargeter st = maybeTargeter.get();
			if (st instanceof IEntitySelector) {
	            ((IEntitySelector)st).filter(data, false);
	            data.setEntityTargets(((IEntitySelector)st).getEntities(data));
			} else if (st instanceof ILocationSelector) {
	            ((ILocationSelector)st).filter(data);
	            data.setLocationTargets(((ILocationSelector)st).getLocations(data));
			}
		}
	}

	@Override
	public boolean castAtLocation(SkillMetadata data, AbstractLocation location) {
		SkillMetadata sdata = data.deepClone();
		HashSet<AbstractLocation> targets = new HashSet<>();
		targets.add(location);
		sdata.setLocationTargets(targets);
		return this.cast(sdata);
	}

	@Override
	public boolean castAtEntity(SkillMetadata data, AbstractEntity entity) {
		SkillMetadata sdata = data.deepClone();
		HashSet<AbstractEntity> targets = new HashSet<>();
		targets.add(entity);
		sdata.setEntityTargets(targets);
		return this.cast(sdata);
	}

	private boolean handleConditions(SkillMetadata data) {
		boolean meet = true;
		if (!this.casterConditions.isEmpty()) {
			meet = this.checkConditions(data, this.casterConditions, false);
		}
		if (!this.targetConditions.isEmpty() && meet) {
			meet = this.checkConditions(data, this.targetConditions, true);
		}
		return meet;
	}

	private boolean checkConditions(SkillMetadata data, HashMap<Integer, SkillCondition> conditions, boolean isTarget) {
		String cline = isTarget ? this.tConditionLine : this.cConditionLine;
		for (int a = 0; a < conditions.size(); a++) {
			SkillMetadata sdata = null;
			sdata = data.deepClone();
			SkillCondition condition = conditions.get(a);
			if (isTarget) {
				cline = cline.replaceFirst(Pattern.quote(this.tConditionLines.get(a)),
						Boolean.toString(condition.evaluateTargets(sdata)));
			} else {
				cline = cline.replaceFirst(Pattern.quote(this.cConditionLines.get(a)),
						Boolean.toString(condition.evaluateCaster(sdata)));
			}
		}
		BooleanExpression be = null;
		try {
			be = BooleanExpression.readLR(cline);
		} catch (MalformedBooleanException e) {
			e.printStackTrace();
		}
		return be.booleanValue();
	}

	private HashMap<Integer, SkillCondition> getConditions(HashMap<Integer, String> conditionList) {
		HashMap<Integer, SkillCondition> conditions = new HashMap<Integer, SkillCondition>();
		for (int a = 0; a < conditionList.size(); a++) {
			SkillCondition sc;
			String s = conditionList.get(a);
			if (s.startsWith(" "))
				s = s.substring(1);
			if ((sc = SkillCondition.getCondition(s)) instanceof InvalidCondition)
				continue;
			conditions.put(a, sc);
		}
		return conditions;
	}

	private void parseConditionLines(String ms, boolean istarget) {
		if (ms != null && (ms.startsWith("\"") && ms.endsWith("\""))) {
			ms = ms.substring(1, ms.length() - 1);
			ms = SkillString.parseMessageSpecialChars(ms);
			if (istarget) {
				this.tConditionLine = ms;
			} else {
				this.cConditionLine = ms;
			}
			ms = ms.replaceAll("\\(", "").replaceAll("\\)", "");
			String[] parse = ms.split("\\&\\&|\\|\\|");
			if (parse != null && parse.length > 0) {
				for (int a = 0; a < Arrays.asList(parse).size(); a++) {
					String p = Arrays.asList(parse).get(a);
					if (istarget) {
						this.tConditionLines.put(a, p);
					} else {
						this.cConditionLines.put(a, p);
					}
				}
			}
		}
	}
}
